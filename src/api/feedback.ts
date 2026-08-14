import { http } from './http'
import type { ApiPage, FeedbackItem, FeedbackStatus } from '@/types/api'

export interface FeedbackQuery {
  status?: FeedbackStatus | ''
  page?: number
  size?: number
}

export const feedbackApi = {
  list: (params: FeedbackQuery = {}) =>
    http.get<ApiPage<FeedbackItem>>('/feedback', { params: { ...params, status: params.status || undefined } }).then((r) => r.data),
}
