import { http, ApiError } from './http'
import { tokenStore } from './token'
import { safeJson } from '@/utils/safeJson'
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

export interface StreamStart {
  sessionId?: string
}

export interface StreamDone {
  sessionId?: string
  cursor?: string
  answer?: string
  model?: string
  modelName?: string
  confidence?: ConfidenceLevel
  confidenceScore?: number
  sources?: ChatSource[]
}

export interface StreamHandlers {
  onStart?: (event: StreamStart) => void
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
    const parsed = safeJson.parse(value) as unknown
    return Array.isArray(parsed) ? (parsed as ChatSource[]) : undefined
  } catch {
    return undefined
  }
}

function isConfidenceLevel(value: unknown): value is ConfidenceLevel {
  return value === 'high' || value === 'medium' || value === 'low'
}

function normalizeMessage(message: ChatMessage): ChatMessage {
  return {
    ...message,
    id: toId(message.id) ?? '',
    sessionId: toId(message.sessionId),
    sources: normalizeSources(message.sources),
  }
}

interface ParsedFrame {
  event?: string
  data: string
}

function parseFrame(frame: string): ParsedFrame | undefined {
  const data: string[] = []
  let event: string | undefined

  for (const rawLine of frame.split(/\r?\n/)) {
    const line = rawLine.trimEnd()
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
      continue
    }
    if (line.startsWith('data:')) {
      data.push(line.slice(5).trimStart())
    }
  }

  if (!data.length) return undefined
  return { event, data: data.join('\n') }
}

function readTextField(payload: unknown, fields: string[]): string {
  if (typeof payload === 'string') return payload
  if (payload == null || typeof payload !== 'object') return ''

  const record = payload as Record<string, unknown>
  for (const field of fields) {
    const value = record[field]
    if (typeof value === 'string') return value
  }
  return ''
}

function normalizeMeta(payload: unknown): StreamMeta {
  if (payload == null || typeof payload !== 'object') return {}
  const record = payload as Record<string, unknown>
  return {
    model: typeof record.modelName === 'string' ? record.modelName : typeof record.model === 'string' ? record.model : undefined,
    confidence: isConfidenceLevel(record.confidence) ? record.confidence : undefined,
  }
}

function normalizeDone(payload: unknown): StreamDone {
  if (payload == null || typeof payload !== 'object') return {}

  const record = payload as Record<string, unknown>
  return {
    sessionId: toId(record.sessionId),
    cursor: typeof record.cursor === 'string' ? record.cursor : undefined,
    answer: typeof record.answer === 'string' ? record.answer : undefined,
    model: typeof record.model === 'string' ? record.model : undefined,
    modelName: typeof record.modelName === 'string' ? record.modelName : undefined,
    confidence: isConfidenceLevel(record.confidence) ? record.confidence : undefined,
    confidenceScore: typeof record.confidenceScore === 'number' ? record.confidenceScore : undefined,
    sources: normalizeSources(record.sources),
  }
}

/**
 * 解析单个 SSE frame（`event:` / `data:`），分发到 handlers；fatal 表示该帧终止流。
 * 兼容无 `event:` 行的帧：优先取 data JSON 内的 `type` 字段，缺省按 token 处理，
 * 避免后端以 `data:` 单行帧推送时事件被静默丢弃。
 */
function dispatchFrame(frame: string, handlers: StreamHandlers, fatal: () => void): void {
  const parsedFrame = parseFrame(frame)
  if (!parsedFrame) return

  let payload: unknown
  try {
    payload = safeJson.parse(parsedFrame.data)
  } catch {
    // 纯文本数据帧：兼容后端直接推送 token 文本的实现
    if (parsedFrame.data.trim()) handlers.onToken(parsedFrame.data)
    return
  }

  const payloadType = typeof payload === 'object' && payload != null ? (payload as { type?: string }).type : undefined
  const eventType = parsedFrame.event && parsedFrame.event !== 'message' ? parsedFrame.event : undefined
  const type = payloadType || eventType || 'token'

  switch (type) {
    case 'token':
      handlers.onToken(readTextField(payload, ['token', 'content', 'delta']))
      break
    case 'delta':
      handlers.onToken(readTextField(payload, ['content', 'delta', 'token']))
      break
    case 'start':
      handlers.onStart?.({ sessionId: toId((payload as { sessionId?: unknown }).sessionId) })
      break
    case 'citation':
      handlers.onCitation(payload as ChatSource)
      break
    case 'meta':
      handlers.onMeta(normalizeMeta(payload))
      break
    case 'done':
      handlers.onDone(normalizeDone(payload))
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
      const body = safeJson.parse(await response.text())
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
    const frames = buffer.split(/\r?\n\r?\n/)
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
