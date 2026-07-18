import {
  isActive,
  mergeAttributes,
  Node,
  ToolboxItem,
  VueNodeViewRenderer,
  type Editor,
  type EditorState,
  type ExtensionOptions,
  type Range,
} from '@halo-dev/richtext-editor'
import { markRaw } from 'vue'
import RiCalendar2Line from '~icons/ri/calendar-2-line'
import RiDeleteBinLine from '~icons/ri/delete-bin-line'
import ActivityCalendarView from './ActivityCalendarView.vue'

declare module '@halo-dev/richtext-editor' {
  interface Commands<ReturnType> {
    activityCalendar: {
      addActivityCalendar: () => ReturnType
    }
  }
}

const ActivityCalendar = Node.create<ExtensionOptions>({
  name: 'activityCalendar',
  group: 'block',
  atom: true,
  draggable: true,

  addAttributes() {
    return {
      title: {
        default: '全站创作活跃度',
        parseHTML: element => element.getAttribute('data-title') || '全站创作活跃度',
      },
      color: {
        default: '#216e39',
        parseHTML: element => element.getAttribute('data-color') || '#216e39',
      },
      years: {
        default: 5,
        parseHTML: element => Number(element.getAttribute('data-years')) || 5,
      },
      showUsers: {
        default: true,
        parseHTML: element => element.getAttribute('data-show-users') !== 'false',
      },
    }
  },

  addOptions() {
    return {
      getCommandMenuItems() {
        return {
          priority: 82,
          icon: markRaw(RiCalendar2Line),
          title: '全站创作活跃日历',
          keywords: ['活跃', '日历', 'activity', 'calendar', 'github'],
          command: ({ editor, range }: { editor: Editor; range: Range }) => {
            editor.chain().focus().deleteRange(range).addActivityCalendar().run()
          },
        }
      },
      getToolboxItems({ editor }: { editor: Editor }) {
        return {
          priority: 52,
          component: markRaw(ToolboxItem),
          props: {
            editor,
            icon: markRaw(RiCalendar2Line),
            title: '全站创作活跃日历',
            description: '展示所有作者每天的创作活跃度',
            action: () => editor.chain().focus().addActivityCalendar().run(),
          },
        }
      },
      getBubbleMenu() {
        return {
          pluginKey: 'activityCalendarBubbleMenu',
          shouldShow: ({ state }: { state: EditorState }): boolean =>
            isActive(state, 'activityCalendar'),
          items: [
            {
              priority: 100,
              props: {
                icon: markRaw(RiDeleteBinLine),
                title: '删除',
                action: ({ editor }: { editor: Editor }) =>
                  editor.chain().focus().deleteSelection().run(),
              },
            },
          ],
        }
      },
    }
  },

  addCommands() {
    return {
      addActivityCalendar:
        () =>
        ({ commands }) =>
          commands.insertContent({ type: this.name }),
    }
  },

  parseHTML() {
    return [{ tag: 'div.halo-activity-calendar' }]
  },

  renderHTML({ HTMLAttributes }) {
    return [
      'div',
      mergeAttributes(HTMLAttributes, {
        class: 'halo-activity-calendar',
        'data-title': HTMLAttributes.title,
        'data-color': HTMLAttributes.color,
        'data-years': HTMLAttributes.years,
        'data-show-users': String(HTMLAttributes.showUsers),
      }),
      '正在加载创作活跃度…',
    ]
  },

  addNodeView() {
    return VueNodeViewRenderer(ActivityCalendarView)
  },
})

export default ActivityCalendar
