package team.foxbridge.activitycalendar;

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
