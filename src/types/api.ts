/** 后端统一响应体：{ code: 0, message: "ok", data, traceId } */
export interface ApiResult<T> {
  code: number
  message: string
  data: T
  traceId?: string
}

export interface ApiPage<T> {
  records: T[]
  total: number
  page?: number
  size?: number
  pages?: number
}

export type RoleCode = 'ADMIN' | 'EDITOR' | 'MEMBER'

export interface UserInfo {
  id: string
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
  id: string
  parentId: string
  name: string
  productLine?: string
  sort?: number
  level?: number
  children?: Category[]
}

export interface Tag {
  id: string
  name: string
  knowledgeCount?: number
}

export type KnowledgeStatus = 'draft' | 'published' | 'archived' | 'pending_publish'
export type KnowledgeType = '操作指南' | '故障排查' | '接口文档' | '政策说明' | 'FAQ'

export interface KnowledgeItem {
  id: string
  title: string
  htmlContent?: string
  plainText?: string
  summary?: string
  categoryId?: string
  categoryName?: string
  categoryPath?: string
  productLine?: string
  moduleName?: string
  knowledgeType?: KnowledgeType | string
  status: KnowledgeStatus
  versionNo?: string
  tags: Tag[]
  viewCount?: number
  likeCount?: number
  publishTime?: string
  scheduledPublishTime?: string
  createdAt?: string
  updatedAt: string
  createdBy?: string
  updatedBy?: string
  excerpt?: string
}

export interface KnowledgeVersion {
  id: string
  knowledgeId: string
  versionNo: string
  title?: string
  changeNote?: string
  createdByName?: string
  createdAt: string
  isCurrent?: boolean
}

export interface SearchParams {
  keyword: string
  categoryId?: string | null
  tagId?: string | null
  knowledgeType?: string | null
  timeFrom?: string | null
  timeTo?: string | null
  page: number
  size: number
  sort: 'relevance' | 'updatedAt' | 'viewCount'
}

export interface SearchResult {
  id: string
  title: string
  summary?: string
  categoryId?: string
  categoryName?: string
  knowledgeType?: string
  publishTime?: string
  viewCount?: number
  likeCount?: number
  rank?: number
  tags: string[]
}

export type ConfidenceLevel = 'high' | 'medium' | 'low'

export interface ChatSource {
  knowledgeId: string
  title: string
  url?: string
  categoryPath?: string
  chunkText?: string
}

export interface ChatMessage {
  id: string
  sessionId?: string
  role: 'user' | 'assistant'
  content: string
  model?: string
  confidence?: ConfidenceLevel
  sources?: ChatSource[]
  createdAt?: string
  isStreaming?: boolean
  /** 流式中断/失败后是否可重试 */
  retryable?: boolean
}

export interface ChatSession {
  id: string
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

/** GET /api/models 返回的可用模型信息 */
export interface ModelInfo {
  key: string
  name: string
  description?: string
}

/** 多轮追问上下文：随 ask 一起发送的最近会话消息 */
export interface ChatContextMessage {
  role: 'user' | 'assistant'
  content: string
}

export type FeedbackType = 'like' | 'dislike' | 'correction' | 'suggestion'
export type FeedbackStatus = 'pending' | 'processing' | 'resolved'

export interface FeedbackItem {
  id: string
  type: FeedbackType
  sourceType?: 'knowledge' | 'answer'
  sourceId?: string
  sessionId?: string
  question?: string
  content?: string
  status: FeedbackStatus
  handlerId?: string
  handleNote?: string
  handledAt?: string
  createdBy?: string
  createdAt: string
  updatedAt?: string
}

/** POST /api/feedback 请求体 */
export interface FeedbackCreatePayload {
  type: FeedbackType
  sourceType: 'knowledge' | 'answer'
  sourceId?: string
  sourceTitle?: string
  sessionId?: string
  question?: string
  content?: string
}

/** PUT /api/feedback/{id}/handle 请求体 */
export interface FeedbackHandlePayload {
  status: FeedbackStatus
  handleNote?: string
}

export interface AnalyticsOverview {
  knowledgeCount: number
  categoryCount?: number
  feedbackCount?: number
  pendingFeedbackCount?: number
  queryCount: number
  searchCount?: number
  qaCount?: number
  noResultRate: number
  adoptionRate: number
  likeCount?: number
  dislikeCount?: number
}

export interface QueryTrendPoint {
  date: string
  count: number
}

export interface CategoryDistribution {
  categoryId?: string
  categoryName: string
  count: number
}

export interface HotSearchItem {
  rank: string
  keyword: string
  count: string
}

export interface FeedbackStatsData {
  pending: string
  processing: string
  resolved: string
  monthlyTotal: string
  avgHandleDays: string
}
