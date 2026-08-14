import { http } from './http'
import type { ApiPage, KnowledgeItem, KnowledgeStatus, KnowledgeVersion } from '@/types/api'

export interface KnowledgeQuery {
  keyword?: string
  categoryId?: number | null
  status?: KnowledgeStatus | ''
  page?: number
  size?: number
}

/** 编辑器提交载荷：标签以名称数组传输，由后端解析/落库 */
export interface KnowledgeSavePayload {
  title?: string
  summary?: string
  categoryId?: number
  knowledgeType?: string
  tags?: string[]
  htmlContent?: string
  plainText?: string
  status?: KnowledgeStatus
}

export const knowledgeApi = {
  list: (params: KnowledgeQuery = {}) =>
    http.get<ApiPage<KnowledgeItem>>('/knowledge', { params: { ...params, categoryId: params.categoryId || undefined, status: params.status || undefined } }).then((r) => r.data),

  detail: (id: number) => http.get<KnowledgeItem>(`/knowledge/${id}`).then((r) => r.data),

  create: (data: KnowledgeSavePayload) => http.post<KnowledgeItem>('/knowledge', data).then((r) => r.data),

  update: (id: number, data: KnowledgeSavePayload) => http.put<KnowledgeItem>(`/knowledge/${id}`, data).then((r) => r.data),

  remove: (id: number) => http.delete<boolean>(`/knowledge/${id}`).then((r) => r.data),

  publish: (id: number, scheduledAt?: string) =>
    http.post<boolean>(`/knowledge/${id}/publish`, { scheduledAt }).then((r) => r.data),

  rollback: (id: number, versionNo: number) =>
    http.post<boolean>(`/knowledge/${id}/rollback`, { versionNo }).then((r) => r.data),

  versions: (id: number) => http.get<KnowledgeVersion[]>(`/knowledge/${id}/versions`).then((r) => r.data),
}
