import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { chatApi, streamAnswer } from '@/api/chat'
import { ApiError } from '@/api/http'
import type { ChatContextMessage, ChatMessage, ChatSession, ModelOption } from '@/types/api'

export const MODELS: ModelOption[] = [
  { key: 'claude', name: 'Claude Opus 5', desc: '最深度推理，适合复杂问题', dot: 'claude' },
  { key: 'gpt', name: 'GPT-4o', desc: '多模态能力强，响应快速', dot: 'gpt' },
  { key: 'gemini', name: 'Gemini 2.5 Pro', desc: '超长上下文，推理均衡', dot: 'gemini' },
  { key: 'deepseek', name: 'DeepSeek V3', desc: '高性价比，中文理解优秀', dot: 'deepseek' },
]

/** 多轮追问携带的上下文轮数（用户+助手各计一轮） */
const CONTEXT_TURNS = 8

function isAbortError(error: unknown): boolean {
  return error instanceof DOMException && error.name === 'AbortError'
}

export const useChatStore = defineStore('chat', () => {
  const sessions = ref<ChatSession[]>([])
  const activeSessionId = ref<number | null>(null)
  const messages = ref<ChatMessage[]>([])
  const models = ref<ModelOption[]>(MODELS)
  const modelKey = ref<string>('claude')
  const isStreaming = ref(false)
  const sending = ref(false)

  let abortController: AbortController | null = null

  const activeModel = computed<ModelOption>(() => models.value.find((m) => m.key === modelKey.value) ?? models.value[0] ?? MODELS[0])

  const latestAnswer = computed<ChatMessage | null>(() => {
    for (let i = messages.value.length - 1; i >= 0; i--) {
      if (messages.value[i].role === 'assistant' && !messages.value[i].isStreaming) return messages.value[i]
    }
    return null
  })

  function setModel(key: string): void {
    if (models.value.some((m) => m.key === key)) modelKey.value = key
  }

  async function loadModels(): Promise<void> {
    try {
      const list = await chatApi.models()
      if (Array.isArray(list) && list.length) {
        models.value = list.map((m) => ({ key: m.key, name: m.name, desc: m.desc ?? '', dot: m.dot ?? m.key }))
        if (!models.value.some((m) => m.key === modelKey.value)) modelKey.value = models.value[0].key
      }
    } catch {
      // 后端未就绪时保持默认模型列表
    }
  }

  async function loadSessions(): Promise<void> {
    sessions.value = await chatApi.sessions()
  }

  async function openSession(id: number): Promise<void> {
    activeSessionId.value = id
    messages.value = await chatApi.messages(id)
  }

  function stopStreaming(): void {
    if (abortController) {
      abortController.abort()
      abortController = null
    }
    if (!isStreaming.value) return
    const last = messages.value[messages.value.length - 1]
    if (last?.role === 'assistant' && last.isStreaming) {
      last.isStreaming = false
      if (!last.content) last.content = '已停止生成'
    }
    isStreaming.value = false
  }

  function newChat(): void {
    stopStreaming()
    activeSessionId.value = null
    messages.value = []
  }

  async function deleteSession(id: number): Promise<void> {
    await chatApi.deleteSession(id)
    if (activeSessionId.value === id) {
      activeSessionId.value = null
      messages.value = []
    }
    sessions.value = sessions.value.filter((s) => s.id !== id)
  }

  /** 组装多轮追问上下文：取 [start, endExclusive) 之间的历史消息 */
  function buildContext(endExclusive: number): ChatContextMessage[] {
    const start = Math.max(0, endExclusive - CONTEXT_TURNS * 2)
    return messages.value
      .slice(start, endExclusive)
      .filter((m) => m.role === 'user' || (m.role === 'assistant' && !m.isStreaming))
      .map((m) => ({ role: m.role, content: m.content }))
  }

  async function runStream(question: string, context: ChatContextMessage[]): Promise<void> {
    messages.value.push({
      id: `a-${Date.now()}`,
      role: 'assistant',
      content: '',
      model: activeModel.value.name,
      isStreaming: true,
      createdAt: new Date().toISOString(),
    })

    const assistant = messages.value[messages.value.length - 1]
    const controller = new AbortController()
    abortController = controller
    isStreaming.value = true

    let completed = false
    let failed = false

    try {
      await streamAnswer(
        { sessionId: activeSessionId.value, question, model: modelKey.value, messages: context },
        controller.signal,
        {
          onToken: (token) => {
            assistant.content += token
          },
          onCitation: (citation) => {
            assistant.sources = assistant.sources ?? []
            if (!assistant.sources.some((s) => s.knowledgeId === citation.knowledgeId && s.title === citation.title)) {
              assistant.sources.push(citation)
            }
          },
          onMeta: (meta) => {
            if (meta.model) assistant.model = meta.model
            if (meta.confidence) assistant.confidence = meta.confidence
          },
          onDone: (result) => {
            completed = true
            if (result.sessionId) {
              if (!activeSessionId.value) activeSessionId.value = result.sessionId
              assistant.sessionId = result.sessionId
            }
          },
          onError: (message) => {
            failed = true
            if (!assistant.content) assistant.content = message
          },
        },
      )
    } catch (error) {
      if (!isAbortError(error)) {
        failed = true
        if (!assistant.content) {
          assistant.content = error instanceof ApiError ? error.message : '问答服务异常，请稍后重试'
        }
      }
    } finally {
      assistant.isStreaming = false
      if (failed && !completed) assistant.retryable = true
      isStreaming.value = false
      sending.value = false
      abortController = null
      await loadSessions().catch(() => {})
    }
  }

  async function send(question: string): Promise<void> {
    const trimmed = question.trim()
    if (!trimmed || sending.value || isStreaming.value) return
    const context = buildContext(messages.value.length)
    sending.value = true
    messages.value.push({ id: `u-${Date.now()}`, role: 'user', content: trimmed, createdAt: new Date().toISOString() })
    await runStream(trimmed, context)
  }

  /** 断线/失败后重试：移除末尾失败的答案，重发同一问题 */
  async function retry(): Promise<void> {
    if (sending.value || isStreaming.value) return
    const lastAssistantIndex = messages.value.length - 1
    const lastAssistant = messages.value[lastAssistantIndex]
    if (!lastAssistant || lastAssistant.role !== 'assistant') return

    let question = ''
    let lastUserIndex = -1
    for (let i = lastAssistantIndex - 1; i >= 0; i--) {
      if (messages.value[i].role === 'user') {
        question = messages.value[i].content
        lastUserIndex = i
        break
      }
    }
    if (!question) return

    const context = buildContext(lastUserIndex)
    messages.value.splice(lastAssistantIndex, 1)
    sending.value = true
    await runStream(question, context)
  }

  return {
    sessions,
    activeSessionId,
    messages,
    models,
    modelKey,
    activeModel,
    latestAnswer,
    isStreaming,
    sending,
    setModel,
    loadModels,
    loadSessions,
    openSession,
    newChat,
    deleteSession,
    stopStreaming,
    send,
    retry,
  }
})
