import { http, ApiError } from './http'
import { tokenStore } from './token'
import type { ApiPage, ChatContextMessage, ChatMessage, ChatSession, ChatSource, ConfidenceLevel, ModelInfo } from '@/types/api'

export interface AskPayload {
  sessionId?: string | null
  question: string
  model: string
  /** 多轮追问上下文：最近几轮消息（不含本次提问） */
  messages?: ChatContextMessage[]
}

export interface StreamMeta {
  model?: string
  confidence?: ConfidenceLevel
}

export interface StreamDone {
  sessionId?: string
  cursor?: string
}

export interface StreamHandlers {
  onToken: (token: string) => void
  onCitation: (citation: ChatSource) => void
  onMeta: (meta: StreamMeta) => void
  onDone: (result: StreamDone) => void
  onError: (message: string) => void
}

export const chatApi = {
  sessions: () => http.get<ApiPage<ChatSession>>('/chat/sessions', { params: { page: 1, size: 50 } }).then((r) => r.data.records.map(normalizeSession)),
  messages: (sessionId: string) => http.get<ChatMessage[]>(`/chat/sessions/${sessionId}/messages`).then((r) => r.data.map(normalizeMessage)),
  deleteSession: (sessionId: string) => http.delete<boolean>(`/chat/sessions/${sessionId}`).then((r) => r.data),
  models: () => http.get<ModelInfo[]>('/models').then((r) => r.data),
}

function toId(value: unknown): string | undefined {
  if (value == null) return undefined
  return String(value)
}

function normalizeSession(session: ChatSession): ChatSession {
  return { ...session, id: toId(session.id) ?? '' }
}

function normalizeSources(value: unknown): ChatSource[] | undefined {
  if (Array.isArray(value)) return value as ChatSource[]
  if (typeof value !== 'string' || !value) return undefined
  try {
    const parsed = JSON.parse(value) as unknown
    return Array.isArray(parsed) ? (parsed as ChatSource[]) : undefined
  } catch {
    return undefined
  }
}

function normalizeMessage(message: ChatMessage): ChatMessage {
  return {
    ...message,
    id: toId(message.id) ?? '',
    sessionId: toId(message.sessionId),
    sources: normalizeSources(message.sources),
  }
}

/**
 * 解析单个 SSE frame（`event:` / `data:`），分发到 handlers；fatal 表示该帧终止流。
 * 兼容无 `event:` 行的帧：优先取 data JSON 内的 `type` 字段，缺省按 token 处理，
 * 避免后端以 `data:` 单行帧推送时事件被静默丢弃。
 */
function dispatchFrame(frame: string, handlers: StreamHandlers, fatal: () => void): void {
  const dataLine = frame.match(/^data:\s*(.+)$/m)?.[1]
  if (dataLine == null) return

  const event = frame.match(/^event:\s*(.+)$/m)?.[1] ?? ''

  let payload: unknown
  try {
    payload = JSON.parse(dataLine)
  } catch {
    // 纯文本数据帧：兼容后端直接推送 token 文本的实现
    if (dataLine.trim()) handlers.onToken(dataLine)
    return
  }

  const type = event || (payload as { type?: string }).type || 'token'

  switch (type) {
    case 'token':
      handlers.onToken((payload as { token?: string }).token ?? '')
      break
    case 'delta':
      handlers.onToken((payload as { content?: string }).content ?? '')
      break
    case 'start':
      // SSE start 事件携带 sessionId，与 done 中的一致，此处可忽略或预存
      break
    case 'citation':
      handlers.onCitation(payload as ChatSource)
      break
    case 'meta':
      handlers.onMeta(payload as StreamMeta)
      break
    case 'done':
      handlers.onDone({ ...(payload as StreamDone), sessionId: toId((payload as StreamDone).sessionId) })
      break
    case 'error':
      handlers.onError((payload as { message?: string }).message ?? '服务异常')
      fatal()
      break
  }
}

/**
 * SSE 流式问答（`POST /api/chat/ask`）。
 * - Mock 模式（VITE_USE_MOCK=true）：本地模拟流，打字机效果，不请求后端；
 * - 真实模式：`fetch` 读 `ReadableStream` 逐块解析 `text/event-stream`。
 */
export async function streamAnswer(payload: AskPayload, signal: AbortSignal, handlers: StreamHandlers): Promise<void> {
  const mockEnabled = import.meta.env.VITE_USE_MOCK !== 'false'
  if (mockEnabled) {
    const { streamMockAnswer } = await import('@/mock/ask')
    await streamMockAnswer(payload, signal, handlers)
    return
  }

  const token = tokenStore.get()
  const response = await fetch('/api/chat/ask', {
    method: 'POST',
    headers: {
      Accept: 'text/event-stream',
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify(payload),
    signal,
  })
  if (!response.ok) {
    // 尝试读取后端错误 JSON（如 401 未登录），降级为 HTTP 状态文本
    let message = `请求失败（${response.status}）`
    try {
      const body = await response.json()
      message = body?.message ?? message
    } catch { /* 非 JSON 响应 */ }
    if (response.status === 401) {
      tokenStore.set(null)
      window.dispatchEvent(new CustomEvent('auth:unauthorized'))
      throw new ApiError(2001, message)
    }
    throw new ApiError(response.status, message)
  }
  if (!response.body) throw new Error('无法建立问答连接')

  const reader = response.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  let stopped = false

  while (true) {
    const { value, done } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    const frames = buffer.split('\n\n')
    buffer = frames.pop() ?? ''
    for (const frame of frames) {
      dispatchFrame(frame, handlers, () => {
        stopped = true
      })
      if (stopped) break
    }
    if (stopped || signal.aborted) break
  }
  if (buffer.trim()) dispatchFrame(buffer, handlers, () => undefined)
}
