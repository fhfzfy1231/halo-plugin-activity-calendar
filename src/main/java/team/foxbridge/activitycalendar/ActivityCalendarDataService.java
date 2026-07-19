package team.foxbridge.activitycalendar;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/** Builds the shared calendar response used by both the endpoint and public page bootstrap. */
@Component
public class ActivityCalendarDataService {

    static final String PLUGIN_VERSION = "2.1.3";
    static final String API_VERSION = "v1alpha2";
    static final String BUILD_SIGNATURE = "hac-2.1.3-213";
    private static final Duration PUBLIC_CACHE_TTL = Duration.ofSeconds(15);

    private final ActivityTracker tracker;
    private volatile CachedPayload publicCache;

    public ActivityCalendarDataService(ActivityTracker tracker) {
        this.tracker = tracker;
    }

    Mono<Map<String, Object>> calendar(int year) {
        return tracker.settings().flatMap(settings -> tracker.baselineForYear(year, settings)
            .collectList()
            .map(baseline -> {
                List<ActivityRecord.Spec> specs = new ArrayList<>(baseline);
                specs.addAll(tracker.runtimeForYear(year));
                return aggregateSpecs(year, specs, settings);
            }));
    }

    Mono<Map<String, Object>> publicCalendars(int newestYear, int yearCount) {
        int safeCount = Math.max(1, Math.min(10, yearCount));
        int oldestYear = newestYear - safeCount + 1;
        CachedPayload cached = publicCache;
        Instant now = Instant.now();
        if (cached != null && cached.newestYear() == newestYear
            && cached.yearCount() == safeCount && now.isBefore(cached.expiresAt())) {
            return cached.payload();
        }
        return replacePublicCache(newestYear, oldestYear, safeCount, now);
    }

    private synchronized Mono<Map<String, Object>> replacePublicCache(int newestYear,
        int oldestYear, int yearCount, Instant now) {
        CachedPayload cached = publicCache;
        if (cached != null && cached.newestYear() == newestYear
            && cached.yearCount() == yearCount && now.isBefore(cached.expiresAt())) {
            return cached.payload();
        }

        Mono<Map<String, Object>> payload = buildPublicCalendars(oldestYear, newestYear)
            .doOnError(error -> clearFailedCache(newestYear, yearCount))
            .cache();
        publicCache = new CachedPayload(newestYear, yearCount,
            now.plus(PUBLIC_CACHE_TTL), payload);
        return payload;
    }

    private synchronized void clearFailedCache(int newestYear, int yearCount) {
        CachedPayload cached = publicCache;
        if (cached != null && cached.newestYear() == newestYear
            && cached.yearCount() == yearCount) {
            publicCache = null;
        }
    }

    private Mono<Map<String, Object>> buildPublicCalendars(int oldestYear, int newestYear) {
        return tracker.settings().flatMap(settings ->
            tracker.baselineForYearRange(oldestYear, newestYear, settings)
                .collectList()
                .map(baseline -> {
                    List<ActivityRecord.Spec> specs = new ArrayList<>(baseline);
                    specs.addAll(tracker.runtimeForYearRange(oldestYear, newestYear));

                    Map<String, Object> years = new LinkedHashMap<>();
                    for (int year = newestYear; year >= oldestYear; year--) {
                        years.put(String.valueOf(year), aggregateSpecs(year, specs, settings));
                    }

                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("pluginVersion", PLUGIN_VERSION);
                    result.put("apiVersion", API_VERSION);
                    result.put("buildSignature", BUILD_SIGNATURE);
                    result.put("generatedAt", Instant.now().toString());
                    result.put("years", years);
                    return result;
                }));
    }

    static Map<String, Object> aggregateSpecs(int year, List<ActivityRecord.Spec> specs,
        ActivitySettings settings) {
        Map<String, DaySummary> days = new TreeMap<>();
        long totalScore = 0;
        String prefix = year + "-";
        for (ActivityRecord.Spec spec : specs) {
            if (spec == null || spec.getDate() == null || !spec.getDate().startsWith(prefix)) {
                continue;
            }
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

    private static int level(long score, ActivitySettings settings) {
        if (score < settings.getLevel1()) return 0;
        if (score < settings.getLevel2()) return 1;
        if (score < settings.getLevel3()) return 2;
        if (score < settings.getLevel4()) return 3;
        return 4;
    }

    private record CachedPayload(int newestYear, int yearCount, Instant expiresAt,
                                 Mono<Map<String, Object>> payload) { }

    private static class DaySummary {
        private long score;
        private final List<Map<String, Object>> users = new ArrayList<>();
    }
}
