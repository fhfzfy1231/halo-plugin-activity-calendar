package team.foxbridge.activitycalendar;

import org.springframework.stereotype.Component;
import run.halo.app.extension.Scheme;
import run.halo.app.extension.SchemeManager;
import run.halo.app.plugin.BasePlugin;
import run.halo.app.plugin.PluginContext;

/**
 * <p>Plugin main class to manage the lifecycle of the plugin.</p>
 * <p>This class must be public and have a public constructor.</p>
 * <p>Only one main class extending {@link BasePlugin} is allowed per plugin.</p>
 *
 * @author Akagi_Zen
 * @since 1.0.0
 */
@Component
public class ActivityCalendarPlugin extends BasePlugin {

    private final SchemeManager schemeManager;
    private final ActivityTracker activityTracker;

    public ActivityCalendarPlugin(PluginContext pluginContext, SchemeManager schemeManager,
        ActivityTracker activityTracker) {
        super(pluginContext);
        this.schemeManager = schemeManager;
        this.activityTracker = activityTracker;
    }

    @Override
    public void start() {
        schemeManager.unregister(Scheme.buildFromType(ActivityRecord.class));
        schemeManager.register(ActivityRecord.class);
        activityTracker.startTracking();
    }

    @Override
    public void stop() {
        activityTracker.stopTracking();
        schemeManager.unregister(Scheme.buildFromType(ActivityRecord.class));
    }
}
