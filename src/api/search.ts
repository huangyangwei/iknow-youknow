import { http } from './http'
import type { ApiPage, SearchParams, SearchResult } from '@/types/api'

export const searchApi = {
  search: (params: SearchParams) =>
    http.get<ApiPage<SearchResult>>('/search', { params: { ...params, categoryId: params.categoryId || undefined, tagId: params.tagId || undefined, knowledgeType: params.knowledgeType || undefined } }).then((r) => r.data),
}
