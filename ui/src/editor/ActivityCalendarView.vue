<script setup lang="ts">
import { NodeViewWrapper, nodeViewProps } from '@halo-dev/richtext-editor'
import { computed } from 'vue'

const props = defineProps(nodeViewProps)

const title = computed({
  get: () => props.node.attrs.title as string,
  set: value => props.updateAttributes({ title: value || '全站创作活跃度' }),
})
const color = computed({
  get: () => props.node.attrs.color as string,
  set: value => props.updateAttributes({ color: value }),
})
const years = computed({
  get: () => Number(props.node.attrs.years) || 5,
  set: value => props.updateAttributes({ years: Math.max(1, Math.min(10, Number(value) || 5)) }),
})
const showUsers = computed({
  get: () => props.node.attrs.showUsers !== false,
  set: value => props.updateAttributes({ showUsers: value }),
})

const levels = Array.from({ length: 105 }, (_, index) => {
  if ([18, 19, 42, 43, 44, 76, 91, 92].includes(index)) return 4
  if ([17, 31, 45, 59, 77, 90].includes(index)) return 3
  if ([16, 30, 58, 75, 93].includes(index)) return 2
  if ([15, 29, 60, 74].includes(index)) return 1
  return 0
})
</script>

<template>
  <NodeViewWrapper class="hac-editor" :style="{ '--preview-color': color }">
    <div class="hac-editor__toolbar" contenteditable="false">
      <label>
        标题
        <input v-model="title" type="text" />
      </label>
      <label>
        主色
        <input v-model="color" type="color" />
      </label>
      <label>
        年份数
        <input v-model.number="years" type="number" min="1" max="10" />
      </label>
      <label class="hac-editor__check">
        <input v-model="showUsers" type="checkbox" />
        显示作者明细
      </label>
    </div>
    <div class="hac-editor__preview" contenteditable="false">
      <div class="hac-editor__head">
        <strong>{{ title }}</strong>
        <span>编辑器预览</span>
      </div>
      <div class="hac-editor__months">1月　 2月　 3月　 4月　 5月　 6月　 7月</div>
      <div class="hac-editor__grid">
        <i v-for="(level, index) in levels" :key="index" :data-level="level"></i>
      </div>
      <p>发布页面后将显示真实的全站活跃数据。</p>
    </div>
  </NodeViewWrapper>
</template>

<style scoped>
.hac-editor { border: 1px solid #d8dee4; border-radius: 8px; overflow: hidden; margin: 12px 0; background: #fff; }
.hac-editor__toolbar { display: flex; flex-wrap: wrap; gap: 10px 16px; align-items: end; padding: 10px 12px; background: #f6f8fa; border-bottom: 1px solid #d8dee4; }
.hac-editor__toolbar label { display: grid; gap: 4px; color: #57606a; font-size: 12px; }
.hac-editor__toolbar input[type="text"], .hac-editor__toolbar input[type="number"] { height: 30px; box-sizing: border-box; border: 1px solid #d0d7de; border-radius: 5px; padding: 4px 8px; background: #fff; }
.hac-editor__toolbar input[type="text"] { width: 200px; }
.hac-editor__toolbar input[type="number"] { width: 70px; }
.hac-editor__toolbar input[type="color"] { width: 44px; height: 30px; padding: 2px; border: 1px solid #d0d7de; border-radius: 5px; background: #fff; }
.hac-editor__toolbar .hac-editor__check { display: flex; align-items: center; gap: 6px; height: 30px; }
.hac-editor__preview { padding: 14px; overflow: hidden; }
.hac-editor__head { display: flex; justify-content: space-between; margin-bottom: 10px; color: #24292f; }
.hac-editor__head span { color: #8c959f; font-size: 12px; }
.hac-editor__months { color: #57606a; font-size: 10px; white-space: nowrap; margin: 0 0 5px 24px; }
.hac-editor__grid { display: grid; grid-template-rows: repeat(7, 10px); grid-auto-flow: column; grid-auto-columns: 10px; gap: 3px; width: max-content; }
.hac-editor__grid i { width: 10px; height: 10px; border-radius: 2px; background: #ebedf0; }
.hac-editor__grid i[data-level="1"] { background: color-mix(in srgb, var(--preview-color) 28%, white); }
.hac-editor__grid i[data-level="2"] { background: color-mix(in srgb, var(--preview-color) 48%, white); }
.hac-editor__grid i[data-level="3"] { background: color-mix(in srgb, var(--preview-color) 72%, white); }
.hac-editor__grid i[data-level="4"] { background: var(--preview-color); }
.hac-editor__preview p { margin: 10px 0 0; color: #8c959f; font-size: 12px; }
</style>
