import { http } from './http'
import type { AnalyticsOverview, CategoryDistribution, HotSearchItem, QueryTrendPoint } from '@/types/api'

export type AnalyticsRange = 'today' | 'week' | 'month' | 'quarter'

export const analyticsApi = {
  overview: (range: AnalyticsRange = 'week') => http.get<AnalyticsOverview>('/analytics/overview', { params: { range } }).then((r) => r.data),
  feedbackTrend: (range: AnalyticsRange = 'week') => http.get<QueryTrendPoint[]>('/analytics/feedback-trend', { params: { range } }).then((r) => r.data),
  categoryDistribution: () => http.get<CategoryDistribution[]>('/analytics/category-distribution').then((r) => r.data),
  hotSearch: (range: AnalyticsRange = 'week') => http.get<HotSearchItem[]>('/analytics/hot-search', { params: { range } }).then((r) => r.data),
}
