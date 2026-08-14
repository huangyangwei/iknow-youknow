<script setup lang="ts">
import { computed } from 'vue'
import { sanitizeHtml } from '@/utils/sanitize'

defineOptions({ name: 'ArticleContent' })

const props = defineProps<{
  html?: string
  plainText?: string
}>()

const sanitized = computed(() => sanitizeHtml(props.html ?? ''))
</script>

<template>
  <div class="article-content">
    <!-- 富文本：经过 DOMPurify 白名单清洗，过滤脚本/事件属性（XSS 防护） -->
    <div v-if="sanitized" class="rich" v-html="sanitized"></div>
    <pre v-else-if="plainText" class="plain">{{ plainText }}</pre>
    <div v-else class="empty">暂无内容</div>
  </div>
</template>

<style scoped>
.article-content .rich {
  line-height: 1.85;
  color: var(--text-primary);
  font-size: 0.9375rem;
  word-break: break-word;
}

.article-content :deep(h1),
.article-content :deep(h2),
.article-content :deep(h3) {
  font-family: var(--font-heading);
  margin: 1.4em 0 0.6em;
  line-height: 1.4;
}

.article-content :deep(h2) {
  font-size: 1.25rem;
  padding-bottom: 0.4em;
  border-bottom: 1px solid var(--border-light);
}

.article-content :deep(p) {
  margin: 0.8em 0;
}

.article-content :deep(ul),
.article-content :deep(ol) {
  padding-left: 1.4em;
  margin: 0.8em 0;
}

.article-content :deep(li) {
  margin: 0.3em 0;
}

.article-content :deep(a) {
  color: var(--primary);
  text-decoration: none;
}

.article-content :deep(a:hover) {
  text-decoration: underline;
}

.article-content :deep(blockquote) {
  margin: 1em 0;
  padding: 0.6em 1em;
  border-left: 3px solid var(--primary);
  background: var(--surface-1);
  color: var(--text-secondary);
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
}

.article-content :deep(code) {
  background: var(--surface-2);
  padding: 2px 6px;
  border-radius: 4px;
  font-size: 0.85em;
  font-family: var(--font-mono, ui-monospace, SFMono-Regular, Menlo, Consolas, monospace);
}

.article-content :deep(pre) {
  background: #0f172a;
  color: #e2e8f0;
  padding: 16px;
  border-radius: var(--radius-sm);
  overflow-x: auto;
  font-size: 0.8125rem;
}

.article-content :deep(pre code) {
  background: none;
  padding: 0;
  color: inherit;
}

.article-content :deep(img) {
  max-width: 100%;
  height: auto;
  border-radius: var(--radius-sm);
}

.article-content :deep(table) {
  width: 100%;
  border-collapse: collapse;
  margin: 1em 0;
  font-size: 0.875rem;
}

.article-content :deep(th),
.article-content :deep(td) {
  border: 1px solid var(--border);
  padding: 8px 12px;
  text-align: left;
}

.article-content :deep(th) {
  background: var(--surface-1);
  font-weight: 600;
}

.article-content .plain {
  white-space: pre-wrap;
  word-break: break-word;
  font-family: var(--font-body);
  margin: 0;
  line-height: 1.85;
}

.article-content .empty {
  color: var(--text-tertiary);
  text-align: center;
  padding: 48px 0;
}
</style>
