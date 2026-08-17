import type { AskPayload, StreamDone, StreamHandlers } from '@/api/chat'
import { MOCK_CHAT_MESSAGES, MOCK_CHAT_SESSIONS, mockAnswer } from './data'

/** 每个 token 块的字符数（越小打字机效果越细腻） */
const CHUNK_SIZE = 3
/** token 间隔毫秒 */
const CHUNK_DELAY = 24
/** 引用来源事件间隔毫秒 */
const CITATION_DELAY = 14

function sleep(ms: number): Promise<void> {
  return new Promise((resolve) => setTimeout(resolve, ms))
}

/**
 * Mock SSE：模拟后端按 `meta → token* → citation* → done` 顺序推送事件，
 * 并就地维护会话/消息内存数据，保证历史会话侧栏与多轮追问可演示。
 */
export async function streamMockAnswer(payload: AskPayload, signal: AbortSignal, handlers: StreamHandlers): Promise<void> {
  const answer = mockAnswer(payload.question, payload.model)

  let sessionId = payload.sessionId ?? null
  if (sessionId === null) {
    sessionId = Math.max(0, ...MOCK_CHAT_SESSIONS.map((s) => s.id)) + 1
    MOCK_CHAT_SESSIONS.unshift({ id: sessionId, title: payload.question.slice(0, 20), updatedAt: new Date().toISOString() })
    MOCK_CHAT_MESSAGES[sessionId] = []
  }
  const createdAt = new Date().toISOString()
  MOCK_CHAT_MESSAGES[sessionId].push({ id: `mu-${Date.now()}`, sessionId, role: 'user', content: payload.question, createdAt })
  answer.sessionId = sessionId
  MOCK_CHAT_MESSAGES[sessionId].push(answer)

  handlers.onMeta({ model: answer.model, confidence: answer.confidence })

  const text = answer.content
  for (let i = 0; i < text.length; i += CHUNK_SIZE) {
    if (signal.aborted) return
    handlers.onToken(text.slice(i, i + CHUNK_SIZE))
    await sleep(CHUNK_DELAY)
  }

  for (const source of answer.sources ?? []) {
    if (signal.aborted) return
    handlers.onCitation(source)
    await sleep(CITATION_DELAY)
  }

  if (signal.aborted) return
  const done: StreamDone = { sessionId }
  handlers.onDone(done)
}
