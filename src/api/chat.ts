import { http } from './http'
import type { ChatMessage, ChatSession } from '@/types/api'

export interface AskPayload {
  sessionId?: number | null
  question: string
  model: string
}

export const chatApi = {
  ask: (payload: AskPayload) => http.post<ChatMessage>('/chat/ask', payload).then((r) => r.data),
  sessions: () => http.get<ChatSession[]>('/chat/sessions').then((r) => r.data),
  messages: (sessionId: number) => http.get<ChatMessage[]>(`/chat/sessions/${sessionId}/messages`).then((r) => r.data),
}
