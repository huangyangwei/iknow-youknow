import { http } from './http'
import { tokenStore } from './token'
import type { ChatContextMessage, ChatMessage, ChatSession, ChatSource, ConfidenceLevel, ModelInfo } from '@/types/api'

export interface AskPayload {
  sessionId?: number | null
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
  sessionId?: number
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
  sessions: () => http.get<ChatSession[]>('/chat/sessions').then((r) => r.data),
  messages: (sessionId: number) => http.get<ChatMessage[]>(`/chat/sessions/${sessionId}/messages`).then((r) => r.data),
  deleteSession: (sessionId: number) => http.delete<boolean>(`/chat/sessions/${sessionId}`).then((r) => r.data),
  models: () => http.get<ModelInfo[]>('/models').then((r) => r.data),
}

/** 解析单个 SSE frame（`event:` / `data:`），分发到 handlers；fatal 表示该帧终止流 */
function dispatchFrame(frame: string, handlers: StreamHandlers, fatal: () => void): void {
  const event = frame.match(/^event:\s*(.+)$/m)?.[1]
  const dataLine = frame.match(/^data:\s*(.+)$/m)?.[1]
  if (!event || !dataLine) return

  let payload: unknown
  try {
    payload = JSON.parse(dataLine)
  } catch {
    handlers.onError('流式响应格式无效')
    fatal()
    return
  }

  switch (event) {
    case 'token':
      handlers.onToken((payload as { token?: string }).token ?? '')
      break
    case 'citation':
      handlers.onCitation(payload as ChatSource)
      break
    case 'meta':
      handlers.onMeta(payload as StreamMeta)
      break
    case 'done':
      handlers.onDone(payload as StreamDone)
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
  if (!response.ok || !response.body) throw new Error('无法建立问答连接')

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
