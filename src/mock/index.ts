import MockAdapter from 'axios-mock-adapter'
import { http } from '@/api/http'
import type { KnowledgeItem } from '@/types/api'
import {
  MOCK_CATEGORIES,
  MOCK_CATEGORY_DISTRIBUTION,
  MOCK_CHAT_MESSAGES,
  MOCK_CHAT_SESSIONS,
  MOCK_HOT_SEARCH,
  MOCK_KNOWLEDGE,
  MOCK_OVERVIEW,
  MOCK_QUERY_TREND,
  MOCK_TAGS,
  MOCK_USERS,
  MOCK_VERSIONS,
  mockAnswer,
  searchMockKnowledge,
} from './data'

/** 从 Bearer token 反解邮箱（mock token 形如 mock-token-<email>） */
function emailFromAuthorization(authorization?: string): string | null {
  if (!authorization) return null
  const token = authorization.replace(/^Bearer\s+/i, '')
  if (!token.startsWith('mock-token-')) return null
  return token.slice('mock-token-'.length)
}

type MockReply = [number, Record<string, unknown>]

function ok<T>(data: T, message = 'ok'): MockReply {
  return [200, { code: 0, message, data }]
}

function fail(status: number, code: number, message: string): MockReply {
  return [status, { code, message }]
}

/** 从路径末尾取第 N 段（1 起）作为 ID */
function atIndex(url: string, fromEnd: number): number {
  const parts = url.split('/').filter(Boolean)
  return Number(parts[parts.length - fromEnd])
}

export function setupMock(): void {
  const mock = new MockAdapter(http, { delayResponse: 200 })

  // ===== 认证 =====
  mock.onPost('/api/auth/login').reply((config) => {
    const body = JSON.parse(config.data ?? '{}') as { email?: string; password?: string }
    const email = body.email?.trim().toLowerCase()
    const user = MOCK_USERS.find((u) => u.email.toLowerCase() === email && u.password === body.password)
    if (!user) return fail(400, 2004, '用户名或密码错误')
    const { password: _pw, ...info } = user
    return ok({
      accessToken: `mock-token-${user.email}`,
      tokenType: 'Bearer',
      expiresIn: 7200,
      user: info,
    })
  })

  mock.onGet('/api/auth/me').reply((config) => {
    const email = emailFromAuthorization(config.headers?.Authorization as string | undefined)
    const user = MOCK_USERS.find((u) => u.email === email)
    if (!user) return fail(401, 2001, '未登录或登录已过期')
    const { password: _pw, ...info } = user
    return ok(info)
  })

  // ===== 搜索 =====
  mock.onGet('/api/search').reply((config) => {
    const params = config.params ?? {}
    const keyword = (params.keyword ?? '') as string
    const page = Number(params.page ?? 1)
    const size = Number(params.size ?? 10)
    let results = searchMockKnowledge(keyword)
    if (params.categoryId) results = results.filter((r) => String(r.id) !== '__never__')
    const start = (page - 1) * size
    return ok({ items: results.slice(start, start + size), total: results.length, page, size })
  })

  // ===== 知识 CRUD =====
  mock.onGet('/api/knowledge').reply((config) => {
    const params = config.params ?? {}
    const page = Number(params.page ?? 1)
    const size = Number(params.size ?? 10)
    const keyword = ((params.keyword ?? '') as string).trim().toLowerCase()
    let list = [...MOCK_KNOWLEDGE]
    if (keyword) list = list.filter((k) => k.title.toLowerCase().includes(keyword))
    if (params.categoryId) list = list.filter((k) => k.categoryId === Number(params.categoryId) || k.productLine === MOCK_CATEGORIES.find((c) => c.id === Number(params.categoryId))?.name)
    if (params.status) list = list.filter((k) => k.status === params.status)
    const start = (page - 1) * size
    return ok({ items: list.slice(start, start + size), total: list.length, page, size })
  })

  mock.onGet(/\/api\/knowledge\/\d+\/versions$/).reply((config) => {
    const id = atIndex(config.url ?? '', 2)
    const versions = MOCK_VERSIONS.filter((v) => v.knowledgeId === id)
    return ok(versions)
  })

  mock.onGet(/\/api\/knowledge\/\d+$/).reply((config) => {
    const id = atIndex(config.url ?? '', 1)
    const item = MOCK_KNOWLEDGE.find((k) => k.id === id)
    if (!item) return fail(404, 3000, '知识条目不存在')
    return ok(item)
  })

  mock.onPost('/api/knowledge').reply((config) => {
    const body = JSON.parse(config.data ?? '{}') as { title?: string; htmlContent?: string; summary?: string; categoryId?: number; knowledgeType?: string; status?: KnowledgeItem['status']; tags?: string[] }
    const item: KnowledgeItem = {
      id: Math.max(...MOCK_KNOWLEDGE.map((k) => k.id)) + 1,
      title: body.title ?? '未命名知识',
      htmlContent: body.htmlContent ?? '',
      summary: body.summary ?? '',
      categoryId: body.categoryId,
      categoryName: MOCK_CATEGORIES.find((c) => c.id === body.categoryId)?.name,
      productLine: MOCK_CATEGORIES.find((c) => c.id === body.categoryId)?.productLine,
      knowledgeType: body.knowledgeType,
      status: body.status ?? 'draft',
      versionNo: 1,
      tags: (body.tags ?? []).map((name) => ({ id: 0, name })),
      viewCount: 0,
      likeCount: 0,
      updatedAt: new Date().toISOString(),
      createdAt: new Date().toISOString(),
    }
    MOCK_KNOWLEDGE.unshift(item)
    return ok(item)
  })

  mock.onPut(/\/api\/knowledge\/\d+$/).reply((config) => {
    const id = atIndex(config.url ?? '', 1)
    const item = MOCK_KNOWLEDGE.find((k) => k.id === id)
    if (!item) return fail(404, 3000, '知识条目不存在')
    const body = JSON.parse(config.data ?? '{}') as { title?: string; htmlContent?: string; summary?: string; categoryId?: number; knowledgeType?: string; status?: KnowledgeItem['status']; tags?: string[] }
    const patch: Partial<KnowledgeItem> = { ...body, tags: (body.tags ?? []).map((name) => ({ id: 0, name })) }
    Object.assign(item, patch, { id: item.id, updatedAt: new Date().toISOString() })
    return ok(item)
  })

  mock.onDelete(/\/api\/knowledge\/\d+$/).reply((config) => {
    const id = atIndex(config.url ?? '', 1)
    const idx = MOCK_KNOWLEDGE.findIndex((k) => k.id === id)
    if (idx === -1) return fail(404, 3000, '知识条目不存在')
    MOCK_KNOWLEDGE.splice(idx, 1)
    return ok(true)
  })

  mock.onPost(/\/api\/knowledge\/\d+\/publish$/).reply((config) => {
    const id = atIndex(config.url ?? '', 2)
    const item = MOCK_KNOWLEDGE.find((k) => k.id === id)
    if (!item) return fail(404, 3000, '知识条目不存在')
    item.status = 'published'
    item.publishTime = new Date().toISOString()
    item.updatedAt = new Date().toISOString()
    return ok(true)
  })

  mock.onPost(/\/api\/knowledge\/\d+\/rollback$/).reply((config) => {
    const id = atIndex(config.url ?? '', 2)
    const body = JSON.parse(config.data ?? '{}') as { versionNo?: number }
    const item = MOCK_KNOWLEDGE.find((k) => k.id === id)
    if (!item) return fail(404, 3000, '知识条目不存在')
    item.versionNo = body.versionNo ?? (item.versionNo ?? 1) + 1
    item.updatedAt = new Date().toISOString()
    return ok(true)
  })

  // ===== 分类 / 标签 =====
  mock.onGet('/api/categories').reply(() => ok(MOCK_CATEGORIES))
  mock.onGet('/api/tags').reply(() => ok(MOCK_TAGS))

  // ===== 问答 =====
  mock.onGet('/api/chat/sessions').reply(() => ok(MOCK_CHAT_SESSIONS))

  mock.onGet(/\/api\/chat\/sessions\/\d+\/messages$/).reply((config) => {
    const id = atIndex(config.url ?? '', 2)
    return ok(MOCK_CHAT_MESSAGES[id] ?? [])
  })

  mock.onPost('/api/chat/ask').reply((config) => {
    const body = JSON.parse(config.data ?? '{}') as { sessionId?: number; question: string; model: string }
    const modelName = body.model
    const answer = mockAnswer(body.question, modelName)
    answer.sessionId = body.sessionId ?? 1
    return ok(answer)
  })

  // ===== 数据仪表盘 =====
  mock.onGet('/api/analytics/overview').reply(() => ok(MOCK_OVERVIEW))
  mock.onGet('/api/analytics/query-trend').reply(() => ok(MOCK_QUERY_TREND))
  mock.onGet('/api/analytics/category-distribution').reply(() => ok(MOCK_CATEGORY_DISTRIBUTION))
  mock.onGet('/api/analytics/hot-search').reply(() => ok(MOCK_HOT_SEARCH))

  // ===== 反馈（占位，供 P3 使用） =====
  mock.onGet('/api/feedback').reply(() => ok({ items: [], total: 0, page: 1, size: 10 }))
}
