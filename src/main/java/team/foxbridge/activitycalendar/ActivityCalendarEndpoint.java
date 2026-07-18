package team.foxbridge.activitycalendar;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Year;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.http.CacheControl;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import run.halo.app.core.extension.endpoint.CustomEndpoint;
import run.halo.app.core.extension.content.Post;
import run.halo.app.core.extension.content.SinglePage;
import run.halo.app.extension.GroupVersion;
import run.halo.app.extension.ReactiveExtensionClient;

@Component
public class ActivityCalendarEndpoint implements CustomEndpoint {

    private final ReactiveExtensionClient client;
    private final ActivityTracker tracker;

    public ActivityCalendarEndpoint(ReactiveExtensionClient client, ActivityTracker tracker) {
        this.client = client;
        this.tracker = tracker;
    }

    @Override
    public RouterFunction<ServerResponse> endpoint() {
        return RouterFunctions.route()
            .GET("calendar", this::calendar)
            .GET("calendar/debug", this::debug)
            .POST("calendar/rebuild-history", this::rebuildHistory)
            .build();
    }

    private Mono<ServerResponse> calendar(ServerRequest request) {
        int current = Year.now().getValue();
        int year = request.queryParam("year").map(this::parseYear).orElse(current);
        String prefix = year + "-";
        Comparator<ActivityRecord> order = Comparator
            .comparing(record -> record.getSpec().getDate());

        return tracker.baselineForYear(year)
            .collectList()
            .map(baseline -> {
                List<ActivityRecord.Spec> specs = new ArrayList<>(baseline);
                specs.addAll(tracker.runtimeForYear(year));
                return aggregateSpecs(year, specs);
            })
            .flatMap(body -> ServerResponse.ok()
                .cacheControl(CacheControl.noStore())
                .bodyValue(body))
            .onErrorResume(error -> ServerResponse.ok()
                .cacheControl(CacheControl.noStore())
                .bodyValue(Map.of(
                    "year", year,
                    "totalScore", 0,
                    "days", List.of(),
                    "message", "Activity calendar scan failed: " + error.getClass().getSimpleName())));
    }

    private Map<String, Object> aggregateSpecs(int year, List<ActivityRecord.Spec> specs) {
        Map<String, DaySummary> days = new TreeMap<>();
        long totalScore = 0;
        for (ActivityRecord.Spec spec : specs) {
            if (spec == null || spec.getDate() == null) continue;
            DaySummary day = days.computeIfAbsent(spec.getDate(), ignored -> new DaySummary());
            day.score += spec.getScore();
            totalScore += spec.getScore();
            Map<String, Object> user = new LinkedHashMap<>();
            user.put("username", spec.getUsername());
            user.put("displayName", spec.getDisplayName());
            user.put("addedWords", spec.getAddedWords());
            user.put("modifiedWords", spec.getModifiedWords());
            user.put("publishedCount", spec.getPublishedCount());
            user.put("republishedCount", spec.getRepublishedCount());
            user.put("score", spec.getScore());
            day.users.add(user);
        }
        return finishAggregate(year, totalScore, days);
    }

    private Mono<ServerResponse> debug(ServerRequest request) {
        int current = Year.now().getValue();
        int year = request.queryParam("year").map(this::parseYear).orElse(current);
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("pluginVersion", "2.0.0-debug.4");
        report.put("year", year);
        report.put("status", "running");

        return diagnosticCountPosts(report)
            .then(diagnosticCountPages(report))
            .then(diagnosticContent(year, report))
            .then(diagnosticBaselineSpec(year, report))
            .then(diagnosticBaseline(year, report))
            .then(Mono.fromSupplier(() -> {
                report.put("status", "completed");
                return report;
            }))
            .onErrorResume(error -> {
                report.put("status", "failed");
                report.put("errorClass", error.getClass().getName());
                report.put("errorMessage", String.valueOf(error.getMessage()));
                report.put("rootCause", rootCause(error));
                report.put("stackTrace", stackTrace(error));
                return Mono.just(report);
            })
            .flatMap(body -> ServerResponse.ok()
                .cacheControl(CacheControl.noStore())
                .bodyValue(body));
    }

    private Mono<Void> diagnosticCountPosts(Map<String, Object> report) {
        report.put("phase", "list-posts");
        return client.list(Post.class, post -> true,
                Comparator.comparing(post -> post.getMetadata().getName()))
            .collectList()
            .doOnNext(posts -> {
                report.put("postCount", posts.size());
                List<Map<String, Object>> samples = new ArrayList<>();
                posts.stream().limit(10).forEach(post -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", post.getMetadata() == null ? null : post.getMetadata().getName());
                    item.put("hasSpec", post.getSpec() != null);
                    if (post.getSpec() != null) {
                        item.put("owner", post.getSpec().getOwner());
                        item.put("publish", post.getSpec().getPublish());
                        item.put("publishTime", String.valueOf(post.getSpec().getPublishTime()));
                        item.put("headSnapshot", post.getSpec().getHeadSnapshot());
                        item.put("baseSnapshot", post.getSpec().getBaseSnapshot());
                        item.put("releaseSnapshot", post.getSpec().getReleaseSnapshot());
                    }
                    samples.add(item);
                });
                report.put("postSamples", samples);
            })
            .then();
    }

    private Mono<Void> diagnosticCountPages(Map<String, Object> report) {
        report.put("phase", "list-pages");
        return client.list(SinglePage.class, page -> true,
                Comparator.comparing(page -> page.getMetadata().getName()))
            .collectList()
            .doOnNext(pages -> report.put("pageCount", pages.size()))
            .then();
    }


    private Mono<Void> diagnosticContent(int year, Map<String, Object> report) {
        report.put("phase", "content-and-authors");
        return tracker.diagnosticContentForYear(year, 20)
            .collectList()
            .doOnNext(items -> {
                report.put("contentDiagnosticCount", items.size());
                report.put("contentDiagnostics", items);
                long success = items.stream()
                    .filter(item -> Boolean.TRUE.equals(item.get("contentSuccess")))
                    .count();
                long failed = items.size() - success;
                report.put("contentSuccessCount", success);
                report.put("contentFailureCount", failed);
                report.put("snapshotReadFailureItems", items.stream()
                    .filter(item -> "failed".equals(item.get("step2SnapshotRead")))
                    .count());
                report.put("multiAuthorCandidateItems", items.stream()
                    .filter(item -> ((Number) item.getOrDefault("authorCandidateCount", 0)).intValue() > 1)
                    .count());
                report.put("activityCandidateCount", items.stream()
                    .filter(item -> Boolean.TRUE.equals(item.get("activityWouldGenerate")))
                    .count());
            })
            .then();
    }

    private Mono<Void> diagnosticBaselineSpec(int year, Map<String,Object> report) {
        report.put("phase", "baseline-spec-generation");
        return tracker.diagnosticBaselineForYear(year, 20)
            .collectList()
            .doOnNext(list -> report.put("baselineSpecDiagnostics", list))
            .then();
    }
    private Mono<Void> diagnosticBaseline(int year, Map<String, Object> report) {
        report.put("phase", "baseline-for-year");
        return tracker.baselineForYear(year)
            .collectList()
            .doOnNext(specs -> {
                report.put("baselineRecordCount", specs.size());
                long score = specs.stream().mapToLong(ActivityRecord.Spec::getScore).sum();
                report.put("baselineTotalScore", score);
                List<Map<String, Object>> samples = new ArrayList<>();
                specs.stream().limit(10).forEach(spec -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("date", spec.getDate());
                    item.put("username", spec.getUsername());
                    item.put("displayName", spec.getDisplayName());
                    item.put("addedWords", spec.getAddedWords());
                    item.put("publishedCount", spec.getPublishedCount());
                    item.put("score", spec.getScore());
                    samples.add(item);
                });
                report.put("baselineSamples", samples);
            })
            .then();
    }

    private String rootCause(Throwable error) {
        Throwable root = error;
        while (root.getCause() != null && root.getCause() != root) {
            root = root.getCause();
        }
        return root.getClass().getName() + ": " + String.valueOf(root.getMessage());
    }

    private String stackTrace(Throwable error) {
        StringWriter writer = new StringWriter();
        error.printStackTrace(new PrintWriter(writer));
        String trace = writer.toString();
        return trace.length() > 24000 ? trace.substring(0, 24000) : trace;
    }

    private Mono<ServerResponse> rebuildHistory(ServerRequest request) {
        return tracker.rebuildHistory()
            .then(ServerResponse.ok().bodyValue(Map.of(
                "success", true,
                "message", "Historical activity rebuild completed"
            )));
    }

    private Map<String, Object> finishAggregate(int year, long totalScore, Map<String, DaySummary> days) {
        ActivitySettings settings = tracker.settings();
        List<Map<String, Object>> dayList = new ArrayList<>();
        days.forEach((date, summary) -> {
            summary.users.sort(Comparator.comparingLong(user ->
                -((Number) user.get("score")).longValue()));
            Map<String, Object> day = new LinkedHashMap<>();
            day.put("date", date);
            day.put("score", summary.score);
            day.put("level", level(summary.score, settings));
            day.put("users", summary.users);
            dayList.add(day);
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("year", year);
        result.put("totalScore", totalScore);
        result.put("days", dayList);
        return result;
    }

    private int level(long score, ActivitySettings settings) {
        if (score < settings.getLevel1()) return 0;
        if (score < settings.getLevel2()) return 1;
        if (score < settings.getLevel3()) return 2;
        if (score < settings.getLevel4()) return 3;
        return 4;
    }

    private int parseYear(String value) {
        try {
            int year = Integer.parseInt(value);
            return year >= 2000 && year <= 2100 ? year : Year.now().getValue();
        } catch (NumberFormatException e) {
            return Year.now().getValue();
        }
    }

    @Override
    public GroupVersion groupVersion() {
        return GroupVersion.parseAPIVersion("api.activity.foxbridge.team/v1alpha1");
    }

    private static class DaySummary {
        private long score;
        private final List<Map<String, Object>> users = new ArrayList<>();
    }
}
