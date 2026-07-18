package team.foxbridge.activitycalendar;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Year;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.ITemplateContext;
import org.thymeleaf.model.IModel;
import org.thymeleaf.processor.element.IElementModelStructureHandler;
import reactor.core.publisher.Mono;
import run.halo.app.plugin.PluginContext;
import run.halo.app.theme.dialect.TemplateHeadProcessor;

@Component
public class ActivityCalendarHeadProcessor implements TemplateHeadProcessor {

    private final PluginContext pluginContext;
    private final ActivityCalendarDataService dataService;
    private final ObjectMapper objectMapper;

    public ActivityCalendarHeadProcessor(PluginContext pluginContext,
        ActivityCalendarDataService dataService, ObjectMapper objectMapper) {
        this.pluginContext = pluginContext;
        this.dataService = dataService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> process(ITemplateContext context, IModel model,
        IElementModelStructureHandler structureHandler) {
        return dataService.publicCalendars(Year.now().getValue(), 10)
            .map(this::serializeForHtml)
            .onErrorReturn(emptyPayload())
            .doOnNext(json -> {
                String version = pluginContext.getVersion();
                String root = "/plugins/activity-calendar/assets/static/";
                String html = "\n<!-- Activity Calendar start -->\n"
                    + "<link rel=\"stylesheet\" href=\"" + root
                    + "activity-calendar.css?v=" + version + "\">\n"
                    + "<script type=\"application/json\" id=\"halo-activity-calendar-data\">"
                    + json + "</script>\n"
                    + "<script defer src=\"" + root + "activity-calendar.js?v=" + version
                    + "\"></script>\n"
                    + "<!-- Activity Calendar end -->\n";
                model.add(context.getModelFactory().createText(html));
            })
            .then();
    }

    String serializeForHtml(Map<String, Object> payload) {
        try {
            return escapeEmbeddedJson(objectMapper.writeValueAsString(payload));
        } catch (JsonProcessingException error) {
            return emptyPayload();
        }
    }

    static String escapeEmbeddedJson(String json) {
        return json.replace("&", "\\u0026")
            .replace("<", "\\u003c")
            .replace(">", "\\u003e")
            .replace("\u2028", "\\u2028")
            .replace("\u2029", "\\u2029");
    }

    private static String emptyPayload() {
        return "{\"pluginVersion\":\"2.1.2\",\"apiVersion\":\"v1alpha2\","
            + "\"buildSignature\":\"hac-2.1.2-212\",\"years\":{}}";
    }
}
