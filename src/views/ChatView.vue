<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import ChatMessage from '@/components/chat/ChatMessage.vue'
import ChatInput from '@/components/chat/ChatInput.vue'
import ModelSelector from '@/components/chat/ModelSelector.vue'
import ChatHistorySidebar from '@/components/chat/ChatHistorySidebar.vue'

defineOptions({ name: 'ChatView' })

const route = useRoute()
const router = useRouter()
const chat = useChatStore()

const listEl = ref<HTMLElement>()

const activeSessionTitle = computed(() => chat.sessions.find((s) => s.id === chat.activeSessionId)?.title ?? '')

function isNearBottom(): boolean {
  const el = listEl.value
  if (!el) return true
  return el.scrollHeight - el.scrollTop - el.clientHeight < 80
}

function scrollToBottom(): void {
  void nextTick(() => {
    if (listEl.value) listEl.value.scrollTop = listEl.value.scrollHeight
  })
}

// 流式生成时跟随最新 token；用户回看历史时不强制下拉
const lastMessageText = computed(() => chat.messages[chat.messages.length - 1]?.content ?? "")

watch(lastMessageText, () => {
  if (isNearBottom()) scrollToBottom()
})

async function handleSend(question: string): Promise<void> {
  await chat.send(question)
  scrollToBottom()
}

async function handleRetry(): Promise<void> {
  await chat.retry()
  scrollToBottom()
}

const handledAutoAsk = ref(false)

onMounted(async () => {
  await Promise.all([chat.loadModels(), chat.loadSessions()]).catch(() => {})
  const sessionId = Number(route.params.sessionId)
  if (sessionId) {
    await chat.openSession(sessionId).catch(() => {})
  }
  scrollToBottom()

  // 首页「智能问答」跳转带 q 参数 → 自动发起提问
  const q = route.query.q
  if (typeof q === 'string' && q.trim() && !handledAutoAsk.value) {
    handledAutoAsk.value = true
    chat.newChat()
    await handleSend(q)
    void router.replace({ name: 'chat', params: { sessionId: chat.activeSessionId ?? undefined } })
  }
})

// 流式完成拿到新会话 id 后同步到 URL
watch(
  () => chat.activeSessionId,
  (id) => {
    const current = Number(route.params.sessionId)
    if (id && id !== current) {
      void router.replace({ name: 'chat', params: { sessionId: id } })
    }
  },
)

watch(
  () => route.params.sessionId,
  async (id) => {
    const sessionId = Number(id)
    if (sessionId && sessionId !== chat.activeSessionId) {
      await chat.openSession(sessionId).catch(() => {})
      scrollToBottom()
    }
  },
)
</script>

<template>
  <div class="chat-page">
    <ChatHistorySidebar />

    <section class="chat-main">
      <header class="chat-header">
        <div class="chat-title">
          <h2>{{ activeSessionTitle || '智能问答' }}</h2>
          <span v-if="chat.isStreaming" class="streaming-tag">生成中…</span>
        </div>
        <ModelSelector />
      </header>

      <div ref="listEl" class="message-list">
        <div v-if="!chat.messages.length" class="chat-empty">
          <div class="chat-empty-icon">💬</div>
          <p>描述客户遇到的问题，或输入一个产品问题</p>
          <p class="hint">答案会附上引用来源与可信度，可点击跳转知识详情</p>
        </div>

        <ChatMessage
          v-for="msg in chat.messages"
          :key="msg.id"
          :message="msg"
          :streaming="msg.isStreaming"
          @retry="handleRetry"
        />
      </div>

      <ChatInput
        class="composer"
        :sending="chat.sending"
        :streaming="chat.isStreaming"
        @send="handleSend"
        @stop="chat.stopStreaming"
      />
    </section>
  </div>
</template>

<style scoped>
.chat-page {
  display: grid;
  grid-template-columns: 280px 1fr;
  gap: 20px;
  height: calc(100vh - 40px);
  max-width: 1240px;
  margin: 0 auto;
}

.chat-main {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-width: 0;
}

.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 18px;
  border-bottom: 1px solid var(--border-light);
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.chat-title h2 {
  font-size: 1rem;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.streaming-tag {
  padding: 2px 10px;
  border-radius: 99px;
  background: var(--primary-light);
  color: var(--primary);
  font-size: 0.6875rem;
  font-weight: 600;
  flex-shrink: 0;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.chat-empty {
  text-align: center;
  margin: auto;
  color: var(--text-secondary);
}

.chat-empty-icon {
  font-size: 2.75rem;
  margin-bottom: 12px;
}

.chat-empty .hint {
  color: var(--text-tertiary);
  font-size: 0.8125rem;
  margin-top: 6px;
}

@media (max-width: 900px) {
  .chat-page {
    grid-template-columns: 1fr;
  }
}
</style>
