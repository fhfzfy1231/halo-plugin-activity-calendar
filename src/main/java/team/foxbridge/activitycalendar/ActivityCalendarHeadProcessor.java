package team.foxbridge.activitycalendar;

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

    public ActivityCalendarHeadProcessor(PluginContext pluginContext) {
        this.pluginContext = pluginContext;
    }

    @Override
    public Mono<Void> process(ITemplateContext context, IModel model,
        IElementModelStructureHandler structureHandler) {
        String version = pluginContext.getVersion();
        String root = "/plugins/activity-calendar/assets/static/";
        String html = "\\n<!-- Activity Calendar start -->\\n"
            + "<link rel=\\"stylesheet\\" href=\\"" + root + "activity-calendar.css?v=" + version + "\\">\\n"
            + "<script defer src=\\"" + root + "activity-calendar.js?v=" + version + "\\"></script>\\n"
            + "<!-- Activity Calendar end -->\\n";
        model.add(context.getModelFactory().createText(html));
        return Mono.empty();
    }
}
