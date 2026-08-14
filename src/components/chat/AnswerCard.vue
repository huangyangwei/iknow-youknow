<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import { renderMarkdown } from '@/utils/markdown'
import type { ChatMessage } from '@/types/api'

defineOptions({ name: 'AnswerCard' })

const props = defineProps<{
  message: ChatMessage
  streaming?: boolean
}>()

const emit = defineEmits<{
  retry: []
}>()

const router = useRouter()
const chat = useChatStore()

const html = computed(() => renderMarkdown(props.message.content))

const confidenceText = computed(() => {
  switch (props.message.confidence) {
    case 'high':
      return '高可信度'
    case 'medium':
      return '中可信度'
    case 'low':
      return '低可信度'
    default:
      return ''
  }
})

const modelDotClass = computed(() => chat.models.find((m) => m.name === props.message.model)?.dot ?? '')

const liked = ref(false)
const disliked = ref(false)

watch(
  () => props.message.id,
  () => {
    liked.value = false
    disliked.value = false
  },
)

function toggleLike(): void {
  if (disliked.value) disliked.value = false
  liked.value = !liked.value
  ElMessage.success(liked.value ? '已记录「有帮助」' : '已取消')
}

function toggleDislike(): void {
  if (liked.value) liked.value = false
  disliked.value = !disliked.value
  ElMessage.success(disliked.value ? '已记录「无帮助」' : '已取消')
}

function submitFeedback(kind: 'correction' | 'suggestion'): void {
  ElMessage.success(kind === 'correction' ? '纠错建议已提交' : '补充建议已提交')
}

function openSource(knowledgeId: number): void {
  void router.push({ name: 'knowledge-detail', params: { id: knowledgeId } })
}
</script>

<template>
  <div class="answer-card">
    <div v-if="message.confidence || message.model" class="answer-meta">
      <span v-if="message.confidence" class="confidence-badge" :class="message.confidence">
        <span class="confidence-dot"></span>
        {{ confidenceText }}
      </span>
      <span v-if="message.model" class="model-badge">
        <span class="model-dot" :class="modelDotClass"></span>
        {{ message.model }}
      </span>
    </div>

    <div class="answer-content">
      <template v-if="streaming && !message.content">
        <span class="streaming-thinking">正在思考</span>
        <span class="cursor-blink">▋</span>
      </template>
      <div v-else class="answer-body" v-html="html"></div>
    </div>

    <div v-if="message.sources?.length" class="sources">
      <h4 class="sources-title">📖 引用来源</h4>
      <button
        v-for="(src, i) in message.sources"
        :key="i"
        type="button"
        class="source-link"
        @click="openSource(src.knowledgeId)"
      >
        <span class="source-icon">📄</span>
        <span class="source-text">
          <span class="source-title">{{ src.title }}</span>
          <span v-if="src.categoryPath" class="source-path">{{ src.categoryPath }}</span>
        </span>
      </button>
    </div>

    <div v-if="streaming" class="streaming-indicator">
      <span class="streaming-dots"></span>
      正在生成…
    </div>

    <div class="answer-actions">
      <button type="button" class="btn-action" :class="{ liked }" @click="toggleLike">👍 有帮助</button>
      <button type="button" class="btn-action" :class="{ disliked }" @click="toggleDislike">👎 无帮助</button>
      <button type="button" class="btn-action" @click="submitFeedback('correction')">✏️ 纠错</button>
      <button type="button" class="btn-action" @click="submitFeedback('suggestion')">💡 补充建议</button>
      <button v-if="message.retryable" type="button" class="btn-action retry" @click="emit('retry')">🔄 重试</button>
    </div>
  </div>
</template>

<style scoped>
.answer-card {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.answer-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.confidence-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 99px;
  font-size: 0.6875rem;
  font-weight: 600;
}

.confidence-badge.high {
  background: var(--success-subtle, #dcfce7);
  color: var(--success, #15803d);
}

.confidence-badge.medium {
  background: var(--warning-subtle, #fef3c7);
  color: var(--warning, #b45309);
}

.confidence-badge.low {
  background: var(--surface-2);
  color: var(--text-tertiary);
}

.confidence-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.model-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 99px;
  font-size: 0.6875rem;
  font-weight: 600;
  background: var(--surface-2);
  color: var(--text-secondary);
}

.answer-content {
  font-size: 0.9375rem;
  line-height: 1.7;
  color: var(--text-primary);
  word-break: break-word;
}

.answer-body :deep(p) {
  margin: 0 0 10px;
}

.answer-body :deep(p:last-child) {
  margin-bottom: 0;
}

.answer-body :deep(ol),
.answer-body :deep(ul) {
  margin: 10px 0;
  padding-left: 22px;
}

.answer-body :deep(li) {
  margin-bottom: 4px;
}

.answer-body :deep(strong) {
  font-weight: 700;
}

.streaming-thinking {
  color: var(--text-tertiary);
  font-size: 0.875rem;
}

.cursor-blink {
  color: var(--primary);
  animation: blink 1s step-start infinite;
}

@keyframes blink {
  50% {
    opacity: 0;
  }
}

.sources {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 12px;
  border-top: 1px dashed var(--border);
}

.sources-title {
  font-size: 0.75rem;
  font-weight: 600;
  color: var(--text-tertiary);
  margin: 0;
}

.source-link {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  width: 100%;
  text-align: left;
  padding: 8px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: var(--surface-1);
  cursor: pointer;
  font-family: var(--font-body);
  transition: all var(--transition-fast);
}

.source-link:hover {
  border-color: var(--primary);
  background: var(--primary-subtle);
}

.source-icon {
  flex-shrink: 0;
  font-size: 0.8125rem;
}

.source-text {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.source-title {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--primary);
}

.source-path {
  font-size: 0.6875rem;
  color: var(--text-tertiary);
}

.streaming-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.6875rem;
  color: var(--text-tertiary);
}

.streaming-dots {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--primary);
  animation: pulse 1.2s ease-in-out infinite;
}

@keyframes pulse {
  0%,
  100% {
    opacity: 0.3;
    transform: scale(0.85);
  }
  50% {
    opacity: 1;
    transform: scale(1);
  }
}

.answer-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding-top: 2px;
}

.btn-action {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 12px;
  border: 1px solid var(--border);
  border-radius: 99px;
  background: #fff;
  font-size: 0.75rem;
  color: var(--text-secondary);
  cursor: pointer;
  font-family: var(--font-body);
  transition: all var(--transition-fast);
}

.btn-action:hover {
  color: var(--primary);
  border-color: var(--primary);
  background: var(--primary-subtle);
}

.btn-action.liked {
  border-color: var(--success, #15803d);
  background: var(--success-subtle, #dcfce7);
  color: var(--success, #15803d);
}

.btn-action.disliked {
  border-color: var(--danger, #dc2626);
  background: var(--danger-subtle, #fee2e2);
  color: var(--danger, #dc2626);
}

.btn-action.retry {
  border-color: var(--warning, #b45309);
  color: var(--warning, #b45309);
  font-weight: 600;
}
</style>
