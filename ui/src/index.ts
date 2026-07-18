import { definePlugin } from '@halo-dev/ui-shared'

export default definePlugin({
  components: {},
  routes: [],
  extensionPoints: {
    'default:editor:extension:create': async () => {
      const { default: ActivityCalendarExtension } = await import('./editor/activity-calendar')
      return [ActivityCalendarExtension]
    },
  },
})
