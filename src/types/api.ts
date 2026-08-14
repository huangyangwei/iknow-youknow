/** 后端统一响应体：{ code: 0, message: "ok", data, traceId } */
export interface ApiResult<T> {
  code: number
  message: string
  data: T
  traceId?: string
}

export interface ApiPage<T> {
  items: T[]
  total: number
  page?: number
  size?: number
}

export type RoleCode = 'ADMIN' | 'EDITOR' | 'MEMBER'

export interface UserInfo {
  id: number
  username: string
  email: string
  nickname: string
  roles: string[]
  avatar?: string
  createdAt?: string
}

export interface LoginPayload {
  provider?: string
  email: string
  password: string
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresIn: number
  user: UserInfo
}

export interface Category {
  id: number
  parentId: number
  name: string
  productLine?: string
  sort?: number
  level?: number
  children?: Category[]
}

export interface Tag {
  id: number
  name: string
  knowledgeCount?: number
}

export type KnowledgeStatus = 'draft' | 'published' | 'archived' | 'pending_publish'
export type KnowledgeType = '操作指南' | '故障排查' | '接口文档' | '政策说明' | 'FAQ'

export interface KnowledgeItem {
  id: number
  title: string
  htmlContent?: string
  plainText?: string
  summary?: string
  categoryId?: number
  categoryName?: string
  categoryPath?: string
  productLine?: string
  moduleName?: string
  knowledgeType?: KnowledgeType | string
  status: KnowledgeStatus
  versionNo?: number
  tags: Tag[]
  viewCount?: number
  likeCount?: number
  publishTime?: string
  scheduledPublishTime?: string
  createdAt?: string
  updatedAt: string
  createdBy?: number
  updatedBy?: number
  excerpt?: string
}

export interface KnowledgeVersion {
  id: number
  knowledgeId: number
  versionNo: number
  title?: string
  changeNote?: string
  createdByName?: string
  createdAt: string
  isCurrent?: boolean
}

export interface SearchParams {
  keyword: string
  categoryId?: number | null
  tagId?: number | null
  knowledgeType?: string | null
  timeFrom?: string | null
  timeTo?: string | null
  page: number
  size: number
  sort: 'relevance' | 'updatedAt' | 'viewCount'
}

export interface SearchResult {
  id: number
  title: string
  excerpt: string
  score?: number
  categoryPath?: string
  updatedAt: string
  viewCount?: number
  tags: string[]
  status?: KnowledgeStatus
}

export type ConfidenceLevel = 'high' | 'medium' | 'low'

export interface ChatSource {
  knowledgeId: number
  title: string
  url?: string
  categoryPath?: string
  chunkText?: string
}

export interface ChatMessage {
  id: string
  sessionId?: number
  role: 'user' | 'assistant'
  content: string
  model?: string
  confidence?: ConfidenceLevel
  sources?: ChatSource[]
  createdAt?: string
  isStreaming?: boolean
}

export interface ChatSession {
  id: number
  title: string
  createdAt?: string
  updatedAt?: string
}

export interface ModelOption {
  key: string
  name: string
  desc: string
  dot: string
}

export type FeedbackType = 'like' | 'dislike' | 'correction' | 'suggestion'
export type FeedbackStatus = 'pending' | 'processing' | 'resolved'

export interface FeedbackItem {
  id: number
  type: FeedbackType
  sourceType?: 'knowledge' | 'answer'
  sourceId?: number
  sourceTitle?: string
  question?: string
  content?: string
  status: FeedbackStatus
  createdByName?: string
  createdByRole?: string
  categoryPath?: string
  createdAt: string
}

export interface AnalyticsOverview {
  knowledgeTotal: number
  knowledgeNewThisWeek: number
  queryTotal: number
  queryChangePercent: number
  adoptionRate: number
  adoptionChangePercent: number
  noResultRate: number
  noResultChangePercent: number
  updatedAt: string
}

export interface QueryTrendPoint {
  date: string
  count: number
}

export interface CategoryDistribution {
  name: string
  value: number
}

export interface HotSearchItem {
  rank: number
  keyword: string
  count: number
}

export interface FeedbackStats {
  pending: number
  monthlyTotal: number
  avgHandleDays: number
}
