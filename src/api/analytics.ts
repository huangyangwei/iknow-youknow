import { http } from './http'
import type { AnalyticsOverview, CategoryDistribution, HotSearchItem, QueryTrendPoint } from '@/types/api'

export const analyticsApi = {
  overview: (range = 'week') => http.get<AnalyticsOverview>('/analytics/overview', { params: { range } }).then((r) => r.data),
  queryTrend: (range = 'week') => http.get<QueryTrendPoint[]>('/analytics/query-trend', { params: { range } }).then((r) => r.data),
  categoryDistribution: () => http.get<CategoryDistribution[]>('/analytics/category-distribution').then((r) => r.data),
  hotSearch: (range = 'week') => http.get<HotSearchItem[]>('/analytics/hot-search', { params: { range } }).then((r) => r.data),
}
