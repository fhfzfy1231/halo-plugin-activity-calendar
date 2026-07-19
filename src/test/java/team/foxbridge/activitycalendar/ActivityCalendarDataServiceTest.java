package team.foxbridge.activitycalendar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ActivityCalendarDataServiceTest {

    @Test
    void aggregatesOnlyRequestedYear() {
        ActivityRecord.Spec inYear = spec("2026-02-24", 800);
        ActivityRecord.Spec anotherYear = spec("2025-02-24", 200);

        Map<String, Object> result = ActivityCalendarDataService.aggregateSpecs(2026,
            List.of(inYear, anotherYear), new ActivitySettings());

        assertEquals(2026, result.get("year"));
        assertEquals(800L, result.get("totalScore"));
        assertEquals(1, ((List<?>) result.get("days")).size());
    }

    @Test
    void escapesJsonThatCouldCloseTheScriptElement() {
        String escaped = ActivityCalendarHeadProcessor.escapeEmbeddedJson(
            "{\"name\":\"</script>&\"}");

        assertEquals("{\"name\":\"\\u003c/script\\u003e\\u0026\"}", escaped);
    }

    @Test
    void headProcessorDoesNotRequireAnObjectMapperBean() {
        boolean requiresObjectMapper = Arrays.stream(
                ActivityCalendarHeadProcessor.class.getDeclaredConstructors())
            .flatMap(constructor -> Arrays.stream(constructor.getParameterTypes()))
            .anyMatch(ObjectMapper.class::equals);

        assertFalse(requiresObjectMapper);
    }

    private static ActivityRecord.Spec spec(String date, long score) {
        ActivityRecord.Spec spec = new ActivityRecord.Spec();
        spec.setDate(date);
        spec.setUsername("author");
        spec.setDisplayName("Author");
        spec.setScore(score);
        return spec;
    }
}
