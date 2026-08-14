<script setup lang="ts">
import '@wangeditor/editor/dist/css/style.css'
import { onBeforeUnmount, ref, shallowRef, watch } from 'vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import type { IDomEditor, IEditorConfig, IToolbarConfig } from '@wangeditor/editor'

defineOptions({ name: 'RichTextEditor' })

const props = defineProps<{
  modelValue: string
  readonly?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string]
}>()

const editorRef = shallowRef<IDomEditor>()
const valueHtml = ref(props.modelValue)

// 视频菜单常需要额外 CDN 配置，先屏蔽避免空配置时报错
const toolbarConfig: Partial<IToolbarConfig> = {
  excludeKeys: ['group-video', 'fullScreen'],
}

const editorConfig: Partial<IEditorConfig> = {
  placeholder: '请输入正文内容…（支持标题、列表、表格、代码块等）',
}

watch(
  () => props.modelValue,
  (v) => {
    if (v !== valueHtml.value) valueHtml.value = v
  },
)

watch(valueHtml, (v) => {
  emit('update:modelValue', v)
})

function handleCreated(editor: IDomEditor) {
  editorRef.value = editor
}

onBeforeUnmount(() => {
  editorRef.value?.destroy()
})
</script>

<template>
  <div class="rich-editor" :class="{ readonly }">
    <Toolbar
      :editor="editorRef"
      :default-config="toolbarConfig"
      :mode="'default'"
      class="toolbar"
    />
    <Editor
      v-model="valueHtml"
      :default-config="editorConfig"
      :mode="'default'"
      :readonly="readonly ?? false"
      class="editor-body"
      @on-created="handleCreated"
    />
  </div>
</template>

<style scoped>
.rich-editor {
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  background: #fff;
  overflow: hidden;
}

.rich-editor :deep(.w-e-toolbar) {
  border-bottom: 1px solid var(--border-light);
  flex-wrap: wrap;
}

.rich-editor :deep(.w-e-text-container) {
  min-height: 320px;
}

.rich-editor :deep(.w-e-text-container [data-slate-editor]) {
  padding: 16px 18px;
  line-height: 1.85;
  min-height: 320px;
}

.rich-editor.readonly :deep(.w-e-toolbar) {
  display: none;
}
</style>
