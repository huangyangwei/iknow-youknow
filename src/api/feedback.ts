import { http } from './http'
import type { ApiPage, FeedbackCreatePayload, FeedbackHandlePayload, FeedbackItem, FeedbackStatus, FeedbackType } from '@/types/api'

export interface FeedbackQuery {
  status?: FeedbackStatus | ''
  type?: FeedbackType | ''
  page?: number
  size?: number
}

export const feedbackApi = {
  list: (params: FeedbackQuery = {}) =>
    http
      .get<ApiPage<FeedbackItem>>('/feedback', {
        params: { ...params, status: params.status || undefined, type: params.type || undefined },
      })
      .then((r) => r.data),
  create: (payload: FeedbackCreatePayload) => http.post<FeedbackItem>('/feedback', payload).then((r) => r.data),
  handle: (id: number, payload: FeedbackHandlePayload) => http.put<FeedbackItem>(`/feedback/${id}/handle`, payload).then((r) => r.data),
}
