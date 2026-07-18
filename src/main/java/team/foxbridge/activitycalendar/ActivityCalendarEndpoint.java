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
            .build();
    }

    private Mono<ServerResponse> calendar(ServerRequest request) {
        int current = Year.now().getValue();
        int year = request.queryParam("year").map(this::parseYear).orElse(current);
        String prefix = year + "-";
        Comparator<ActivityRecord> order = Comparator
            .comparing(record -> record.getSpec().getDate());

        return client.list(ActivityRecord.class,
                record -> record.getSpec() != null
                    && record.getSpec().getDate() != null
                    && record.getSpec().getDate().startsWith(prefix), order)
            .collectList()
            .map(records -> aggregate(year, records))
            .flatMap(body -> ServerResponse.ok()
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofSeconds(60)).cachePublic())
                .bodyValue(body));
    }

    private Map<String, Object> aggregate(int year, List<ActivityRecord> records) {
        Map<String, DaySummary> days = new TreeMap<>();
        long totalScore = 0;
        for (ActivityRecord record : records) {
            ActivityRecord.Spec spec = record.getSpec();
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
