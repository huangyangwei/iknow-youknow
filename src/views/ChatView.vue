<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { MODELS, useChatStore } from '@/stores/chat'

defineOptions({ name: 'ChatView' })

const route = useRoute()
const router = useRouter()
const chat = useChatStore()

const input = ref('')
const listEl = ref<HTMLElement>()

function scrollToBottom() {
  void nextTick(() => {
    if (listEl.value) listEl.value.scrollTop = listEl.value.scrollHeight
  })
}

async function send() {
  const question = input.value.trim()
  if (!question || chat.sending) return
  input.value = ''
  scrollToBottom()
  await chat.send(question)
  scrollToBottom()
}

async function openSession(id: number) {
  await chat.openSession(id)
  void router.replace({ name: 'chat', params: { sessionId: id } })
  scrollToBottom()
}

function newChat() {
  chat.newChat()
  void router.replace({ name: 'chat' })
}

function selectModel(key: string) {
  chat.setModel(key)
}

const handledAutoAsk = ref(false)

onMounted(async () => {
  await chat.loadSessions().catch(() => {})
  const sessionId = Number(route.params.sessionId)
  if (sessionId) {
    await chat.openSession(sessionId).catch(() => {})
    scrollToBottom()
  }
  // 首页「智能问答」跳转带 q 参数 → 自动发起提问
  const q = route.query.q
  if (typeof q === 'string' && q.trim() && !handledAutoAsk.value) {
    handledAutoAsk.value = true
    chat.newChat()
    input.value = q
    await send()
  }
})

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

function formatTime(value?: string) {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function feedback(kind: 'good' | 'bad') {
  ElMessage.success(kind === 'good' ? '感谢反馈，已记录「有帮助」' : '已记录「需改进」，我们将持续优化')
}
</script>

<template>
  <div class="chat-page">
    <aside class="chat-sidebar">
      <button type="button" class="new-chat-btn" @click="newChat">
        + 新对话
      </button>
      <div class="session-list">
        <button
          v-for="s in chat.sessions"
          :key="s.id"
          type="button"
          class="session-item"
          :class="{ active: chat.activeSessionId === s.id }"
          @click="openSession(s.id)"
        >
          <span class="session-title">{{ s.title }}</span>
          <span class="session-time">{{ formatTime(s.updatedAt) }}</span>
        </button>
        <div v-if="!chat.sessions.length" class="session-empty">暂无历史对话</div>
      </div>
    </aside>

    <section class="chat-main">
      <div class="chat-toolbar">
        <span class="model-label">模型：</span>
        <div class="model-chips">
          <button
            v-for="m in MODELS"
            :key="m.key"
            type="button"
            class="model-chip"
            :class="{ active: chat.modelKey === m.key }"
            @click="selectModel(m.key)"
          >
            {{ m.name }}
          </button>
        </div>
      </div>

      <div ref="listEl" class="message-list">
        <div v-if="!chat.messages.length" class="chat-empty">
          <div class="chat-empty-icon">💬</div>
          <p>描述客户遇到的问题，或输入一个产品问题</p>
          <p class="hint">答案会附上引用来源，可点击跳转知识详情</p>
        </div>

        <div v-for="msg in chat.messages" :key="msg.id" class="message" :class="msg.role">
          <div class="bubble">
            <div v-if="msg.role === 'assistant'" class="msg-meta">
              <span class="model-dot" :class="chat.activeModel.dot"></span>
              {{ chat.activeModel.name }}
              <span v-if="msg.confidence" class="confidence" :class="msg.confidence">
                {{ msg.confidence === 'high' ? '高置信' : msg.confidence === 'medium' ? '中置信' : '低置信' }}
              </span>
            </div>
            <div class="msg-content">{{ msg.content }}</div>
            <div v-if="msg.sources?.length" class="sources">
              <span class="sources-label">引用来源</span>
              <button
                v-for="src in msg.sources"
                :key="src.knowledgeId"
                type="button"
                class="source-link"
                @click="router.push({ name: 'knowledge-detail', params: { id: src.knowledgeId } })"
              >
                📄 {{ src.title }}
              </button>
            </div>
            <div v-if="msg.role === 'assistant'" class="msg-actions">
              <button type="button" class="mini-btn" @click="feedback('good')">👍 有帮助</button>
              <button type="button" class="mini-btn" @click="feedback('bad')">👎 需改进</button>
            </div>
          </div>
        </div>
      </div>

      <div class="composer">
        <textarea
          v-model="input"
          rows="2"
          placeholder="输入问题，Enter 发送，Shift+Enter 换行"
          @keydown.enter.exact.prevent="send"
        ></textarea>
        <button
          type="button"
          class="send-btn"
          :disabled="chat.sending || !input.trim()"
          @click="send"
        >
          {{ chat.sending ? '生成中…' : '发送' }}
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.chat-page {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 20px;
  height: calc(100vh - 64px);
  max-width: 1200px;
  margin: 0 auto;
}

.chat-sidebar {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.new-chat-btn {
  margin: 12px;
  padding: 9px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--primary);
  color: #fff;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  font-family: var(--font-body);
}

.new-chat-btn:hover {
  background: var(--primary-hover);
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.session-item {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: 10px 12px;
  border: none;
  border-radius: var(--radius-sm);
  background: none;
  cursor: pointer;
  font-family: var(--font-body);
  text-align: left;
  transition: background var(--transition-fast);
}

.session-item:hover {
  background: var(--surface-1);
}

.session-item.active {
  background: var(--primary-subtle);
}

.session-title {
  font-size: 0.8125rem;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
}

.session-time {
  font-size: 0.6875rem;
  color: var(--text-tertiary);
}

.session-empty {
  padding: 24px 12px;
  font-size: 0.75rem;
  color: var(--text-tertiary);
  text-align: center;
}

.chat-main {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.chat-toolbar {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--border-light);
  flex-wrap: wrap;
}

.model-label {
  font-size: 0.75rem;
  color: var(--text-tertiary);
}

.model-chips {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.model-chip {
  padding: 4px 12px;
  border-radius: 99px;
  border: 1px solid var(--border);
  background: #fff;
  color: var(--text-secondary);
  font-size: 0.75rem;
  cursor: pointer;
  font-family: var(--font-body);
  transition: all var(--transition-fast);
}

.model-chip.active {
  border-color: var(--primary);
  background: var(--primary-subtle);
  color: var(--primary);
  font-weight: 600;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
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

.message {
  display: flex;
}

.message.user {
  justify-content: flex-end;
}

.message.assistant {
  justify-content: flex-start;
}

.bubble {
  max-width: min(720px, 88%);
  padding: 12px 16px;
  border-radius: var(--radius-md);
  font-size: 0.9063rem;
  line-height: 1.75;
  word-break: break-word;
}

.message.user .bubble {
  background: var(--primary);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.message.assistant .bubble {
  background: var(--surface-1);
  border: 1px solid var(--border-light);
  border-bottom-left-radius: 4px;
}

.msg-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 0.6875rem;
  color: var(--text-tertiary);
  margin-bottom: 6px;
}

.model-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.confidence {
  padding: 1px 8px;
  border-radius: 99px;
  font-weight: 600;
}

.confidence.high {
  background: var(--success-subtle, #dcfce7);
  color: var(--success, #15803d);
}

.confidence.medium {
  background: var(--warning-subtle, #fef3c7);
  color: var(--warning, #b45309);
}

.confidence.low {
  background: var(--surface-2);
  color: var(--text-tertiary);
}

.sources {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.sources-label {
  font-size: 0.6875rem;
  color: var(--text-tertiary);
  width: 100%;
}

.source-link {
  border: 1px solid var(--border);
  background: #fff;
  border-radius: 99px;
  padding: 3px 12px;
  font-size: 0.75rem;
  color: var(--primary);
  cursor: pointer;
  font-family: var(--font-body);
  transition: all var(--transition-fast);
}

.source-link:hover {
  border-color: var(--primary);
  background: var(--primary-subtle);
}

.msg-actions {
  display: flex;
  gap: 6px;
  margin-top: 10px;
}

.mini-btn {
  border: 1px solid var(--border);
  background: #fff;
  border-radius: 99px;
  padding: 3px 10px;
  font-size: 0.6875rem;
  color: var(--text-tertiary);
  cursor: pointer;
  font-family: var(--font-body);
}

.mini-btn:hover {
  color: var(--primary);
  border-color: var(--primary);
}

.composer {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  padding: 12px 16px;
  border-top: 1px solid var(--border-light);
}

.composer textarea {
  flex: 1;
  resize: none;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 10px 12px;
  font-size: 0.875rem;
  font-family: var(--font-body);
  line-height: 1.5;
  color: var(--text-primary);
  background: #fff;
  transition: border-color var(--transition-fast);
}

.composer textarea:focus {
  outline: none;
  border-color: var(--primary);
}

.send-btn {
  padding: 10px 20px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--primary);
  color: #fff;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  font-family: var(--font-body);
}

.send-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 900px) {
  .chat-page {
    grid-template-columns: 1fr;
  }
  .chat-sidebar {
    display: none;
  }
}
</style>
