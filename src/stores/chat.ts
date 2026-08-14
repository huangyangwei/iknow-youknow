import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { chatApi } from '@/api/chat'
import { ApiError } from '@/api/http'
import type { ChatMessage, ChatSession, ModelOption } from '@/types/api'

export const MODELS: ModelOption[] = [
  { key: 'claude', name: 'Claude Opus 5', desc: '最深度推理，适合复杂问题', dot: 'claude' },
  { key: 'gpt', name: 'GPT-4o', desc: '多模态能力强，响应快速', dot: 'gpt' },
  { key: 'gemini', name: 'Gemini 2.5 Pro', desc: '超长上下文，推理均衡', dot: 'gemini' },
  { key: 'deepseek', name: 'DeepSeek V3', desc: '高性价比，中文理解优秀', dot: 'deepseek' },
]

export const useChatStore = defineStore('chat', () => {
  const sessions = ref<ChatSession[]>([])
  const activeSessionId = ref<number | null>(null)
  const messages = ref<ChatMessage[]>([])
  const modelKey = ref<string>('claude')
  const isStreaming = ref(false)
  const sending = ref(false)

  const activeModel = computed<ModelOption>(() => MODELS.find((m) => m.key === modelKey.value) ?? MODELS[0])
  const latestAnswer = computed<ChatMessage | null>(() => {
    for (let i = messages.value.length - 1; i >= 0; i--) {
      if (messages.value[i].role === 'assistant') return messages.value[i]
    }
    return null
  })

  function setModel(key: string): void {
    if (MODELS.some((m) => m.key === key)) modelKey.value = key
  }

  async function loadSessions(): Promise<void> {
    sessions.value = await chatApi.sessions()
  }

  async function openSession(id: number): Promise<void> {
    activeSessionId.value = id
    messages.value = await chatApi.messages(id)
  }

  function newChat(): void {
    activeSessionId.value = null
    messages.value = []
  }

  async function send(question: string): Promise<void> {
    const trimmed = question.trim()
    if (!trimmed || sending.value) return
    sending.value = true
    messages.value.push({ id: `u-${Date.now()}`, role: 'user', content: trimmed })
    try {
      const answer = await chatApi.ask({ sessionId: activeSessionId.value, question: trimmed, model: modelKey.value })
      if (!activeSessionId.value && answer.sessionId) activeSessionId.value = answer.sessionId
      messages.value.push(answer)
      await loadSessions()
    } catch (error) {
      const message = error instanceof ApiError ? error.message : '问答服务异常，请稍后重试'
      messages.value.push({ id: `e-${Date.now()}`, role: 'assistant', content: message })
    } finally {
      sending.value = false
    }
  }

  return {
    sessions,
    activeSessionId,
    messages,
    modelKey,
    activeModel,
    latestAnswer,
    isStreaming,
    sending,
    setModel,
    loadSessions,
    openSession,
    newChat,
    send,
  }
})
