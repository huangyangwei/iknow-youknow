import type { Citation } from '../types/api'

export interface StreamHandlers { onToken: (token: string) => void; onCitation: (citation: Citation) => void; onDone: (cursor?: string) => void; onError: (message: string) => void }
export async function streamAnswer(question: string, model: string, cursor: string | undefined, signal: AbortSignal, handlers: StreamHandlers) {
  const response = await fetch('/api/v1/chat/stream', { method: 'POST', headers: { Accept: 'text/event-stream', 'Content-Type': 'application/json' }, body: JSON.stringify({ question, model, cursor }), signal })
  if (!response.ok || !response.body) throw new Error('无法建立问答连接')
  const reader = response.body.getReader(); const decoder = new TextDecoder(); let buffer = ''
  while (true) { const { value, done } = await reader.read(); if (done) break; buffer += decoder.decode(value, { stream: true }); const events = buffer.split('\n\n'); buffer = events.pop() ?? ''; events.forEach(raw => { const type = raw.match(/^event: (.+)$/m)?.[1]; const data = raw.match(/^data: (.+)$/m)?.[1]; if (!type || !data) return; try { const payload = JSON.parse(data); if (type === 'token') handlers.onToken(payload.token); if (type === 'citation') handlers.onCitation(payload); if (type === 'done') handlers.onDone(payload.cursor); if (type === 'error') handlers.onError(payload.message) } catch { handlers.onError('流式响应格式无效') } }) }
}
