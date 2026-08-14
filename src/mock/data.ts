import type {
  AnalyticsOverview,
  Category,
  CategoryDistribution,
  ChatMessage,
  ChatSession,
  ConfidenceLevel,
  HotSearchItem,
  KnowledgeItem,
  KnowledgeVersion,
  ModelInfo,
  QueryTrendPoint,
  SearchResult,
  Tag,
  UserInfo,
} from '@/types/api'

export interface MockUser extends UserInfo {
  password: string
}

export const MOCK_USERS: MockUser[] = [
  { id: 1, username: 'wangdawei', email: 'admin@iknow.com', password: '123456', nickname: '王大伟', roles: ['ADMIN'], avatar: '王' },
  { id: 2, username: 'zhangxiaomei', email: 'editor@iknow.com', password: '123456', nickname: '张小美', roles: ['EDITOR'], avatar: '张' },
  { id: 3, username: 'lixiaohua', email: 'member@iknow.com', password: '123456', nickname: '李小华', roles: ['MEMBER'], avatar: '李' },
]

export const MOCK_CATEGORIES: Category[] = [
  { id: 1, parentId: 0, name: '支付平台', productLine: '支付平台', level: 1, sort: 1 },
  { id: 11, parentId: 1, name: '支付接入', productLine: '支付平台', level: 2, sort: 1 },
  { id: 12, parentId: 1, name: '故障排查', productLine: '支付平台', level: 2, sort: 2 },
  { id: 13, parentId: 1, name: '渠道接入', productLine: '支付平台', level: 2, sort: 3 },
  { id: 14, parentId: 1, name: '系统机制', productLine: '支付平台', level: 2, sort: 4 },
  { id: 15, parentId: 1, name: '开发规范', productLine: '支付平台', level: 2, sort: 5 },
  { id: 2, parentId: 0, name: '账户系统', productLine: '账户系统', level: 1, sort: 2 },
  { id: 21, parentId: 2, name: '账户管理', productLine: '账户系统', level: 2, sort: 1 },
  { id: 22, parentId: 2, name: '认证安全', productLine: '账户系统', level: 2, sort: 2 },
  { id: 3, parentId: 0, name: '订单中心', productLine: '订单中心', level: 1, sort: 3 },
  { id: 31, parentId: 3, name: '数据导出', productLine: '订单中心', level: 2, sort: 1 },
  { id: 32, parentId: 3, name: '退款流程', productLine: '订单中心', level: 2, sort: 2 },
  { id: 4, parentId: 0, name: '通知服务', productLine: '通知服务', level: 1, sort: 4 },
  { id: 5, parentId: 0, name: '开放平台', productLine: '开放平台', level: 1, sort: 5 },
  { id: 51, parentId: 5, name: 'API 文档', productLine: '开放平台', level: 2, sort: 1 },
]

export const MOCK_TAGS: Tag[] = [
  { id: 1, name: '支付', knowledgeCount: 15 },
  { id: 2, name: '回调', knowledgeCount: 8 },
  { id: 3, name: '排查', knowledgeCount: 6 },
  { id: 4, name: '微信', knowledgeCount: 5 },
  { id: 5, name: 'API', knowledgeCount: 12 },
  { id: 6, name: '鉴权', knowledgeCount: 7 },
  { id: 7, name: '注销', knowledgeCount: 3 },
  { id: 8, name: '导出', knowledgeCount: 4 },
  { id: 9, name: '退款', knowledgeCount: 6 },
  { id: 10, name: '企业微信', knowledgeCount: 3 },
  { id: 11, name: '订单', knowledgeCount: 9 },
  { id: 12, name: '限流', knowledgeCount: 2 },
  { id: 13, name: 'Webhook', knowledgeCount: 4 },
  { id: 14, name: 'SSO', knowledgeCount: 5 },
]

const baseDetailHtml = `
  <p>当支付回调未正常触发或订单状态未正确更新时，可按照以下步骤进行问题排查。本文适用于所有支付渠道（微信支付、支付宝、银联等）。</p>
  <h2>一、检查回调日志</h2>
  <p>登录支付管理后台，进入「回调日志」页面，按订单号或商户号筛选相关记录：</p>
  <ul>
    <li>如果没有找到任何回调记录 → 说明支付渠道未发起回调，需联系渠道确认</li>
    <li>如果有记录但状态码不是 200 → 说明回调已到达但服务端返回异常，需查看错误信息</li>
    <li>如果状态码为 200 但订单未更新 → 可能是业务处理逻辑异常</li>
  </ul>
  <h2>二、验证回调签名</h2>
  <p>签名验证失败是回调处理失败最常见的原因。检查以下配置：</p>
  <pre><code>// 验证签名示例
const crypto = require('crypto');
const sign = crypto
  .createHmac('sha256', apiKey)
  .update(payload)
  .digest('hex');
if (sign !== receivedSign) {
  throw new Error('签名验证失败');
}</code></pre>
  <h2>三、检查网络可达性</h2>
  <p>确认回调 URL 可从公网正常访问。常见网络问题包括：</p>
  <ul>
    <li>防火墙或安全组未开放对应端口</li>
    <li>负载均衡器健康检查未通过</li>
    <li>DNS 解析异常</li>
    <li>SSL 证书过期或配置错误</li>
  </ul>
  <h2>四、查看重试记录</h2>
  <p>系统在回调失败时会自动重试，最多 5 次，重试间隔为：1分钟 → 5分钟 → 30分钟 → 2小时 → 6小时。如果 5 次全部失败，系统会生成告警通知。</p>
  <blockquote>提示：如果需要批量处理回调失败的订单，可使用管理后台的「补偿任务」功能，一键触发重新回调。</blockquote>
`

export const MOCK_KNOWLEDGE: KnowledgeItem[] = [
  {
    id: 1,
    title: '支付回调常见问题排查',
    htmlContent: baseDetailHtml,
    summary: '本文介绍支付回调未触发或订单状态未更新时的排查步骤，覆盖回调日志、签名验证、网络可达性与重试记录。',
    categoryId: 12,
    categoryName: '故障排查',
    categoryPath: '支付平台 › 故障排查',
    productLine: '支付平台',
    moduleName: '故障排查',
    knowledgeType: '故障排查',
    status: 'published',
    versionNo: 3,
    tags: [MOCK_TAGS[2], MOCK_TAGS[1], MOCK_TAGS[0]],
    viewCount: 876,
    likeCount: 12,
    publishTime: '2026-07-15T10:30:00+08:00',
    updatedAt: '2026-07-15T10:30:00+08:00',
    createdAt: '2026-05-01T09:00:00+08:00',
  },
  {
    id: 2,
    title: '支付回调配置指南',
    htmlContent: '<p>本文介绍如何配置支付回调地址，包括支付成功、退款、取消等场景的回调处理逻辑。</p><h2>配置步骤</h2><ol><li>登录商户平台</li><li>进入「产品中心 → 支付设置」</li><li>填写回调 URL 并保存</li></ol>',
    summary: '本文介绍如何配置支付回调地址，包括支付成功、退款、取消等场景的回调处理逻辑。',
    categoryId: 11,
    categoryName: '支付接入',
    categoryPath: '支付平台 › 支付接入',
    productLine: '支付平台',
    moduleName: '支付接入',
    knowledgeType: '操作指南',
    status: 'published',
    versionNo: 2,
    tags: [MOCK_TAGS[0], MOCK_TAGS[1]],
    viewCount: 1234,
    likeCount: 20,
    publishTime: '2026-07-28T09:00:00+08:00',
    updatedAt: '2026-07-28T09:00:00+08:00',
    createdAt: '2026-06-01T09:00:00+08:00',
  },
  {
    id: 3,
    title: '微信支付回调通知说明',
    htmlContent: '<p>微信支付结果通知通过 HTTPS POST 发送到商户配置的回调 URL，需验证签名。</p><h2>通知机制</h2><p>支付成功后，微信支付会异步发送通知，商户需返回成功应答，否则会多次重试。</p>',
    summary: '微信支付结果通知通过 HTTPS POST 发送到商户配置的回调 URL，需验证签名。',
    categoryId: 13,
    categoryName: '渠道接入',
    categoryPath: '支付平台 › 渠道接入',
    productLine: '支付平台',
    moduleName: '渠道接入',
    knowledgeType: '接口文档',
    status: 'published',
    versionNo: 1,
    tags: [MOCK_TAGS[3], MOCK_TAGS[0], MOCK_TAGS[1]],
    viewCount: 2101,
    likeCount: 31,
    publishTime: '2026-06-30T14:00:00+08:00',
    updatedAt: '2026-06-30T14:00:00+08:00',
    createdAt: '2026-06-10T09:00:00+08:00',
  },
  {
    id: 4,
    title: '回调重试机制说明',
    htmlContent: '<p>当回调失败时，系统会自动重试最多 5 次，重试间隔逐渐递增。本文说明重试策略。</p><h2>重试间隔</h2><p>1分钟 → 5分钟 → 30分钟 → 2小时 → 6小时，5 次全部失败后生成告警。</p>',
    summary: '当回调失败时，系统会自动重试最多 5 次，重试间隔逐渐递增。本文说明重试策略。',
    categoryId: 14,
    categoryName: '系统机制',
    categoryPath: '支付平台 › 系统机制',
    productLine: '支付平台',
    moduleName: '系统机制',
    knowledgeType: '政策说明',
    status: 'published',
    versionNo: 1,
    tags: [MOCK_TAGS[1], MOCK_TAGS[0]],
    viewCount: 654,
    likeCount: 5,
    publishTime: '2026-05-12T11:00:00+08:00',
    updatedAt: '2026-05-12T11:00:00+08:00',
    createdAt: '2026-05-12T11:00:00+08:00',
  },
  {
    id: 5,
    title: '支付结果异步通知规范',
    htmlContent: '<p>本文档定义了支付异步通知的通用规范，包括数据结构、签名算法、回调超时设置。</p><h2>数据结构</h2><p>通知报文包含订单号、支付金额、支付时间、签名等字段。</p>',
    summary: '本文档定义了支付异步通知的通用规范，包括数据结构、签名算法、回调超时设置。',
    categoryId: 15,
    categoryName: '开发规范',
    categoryPath: '支付平台 › 开发规范',
    productLine: '支付平台',
    moduleName: '开发规范',
    knowledgeType: '接口文档',
    status: 'published',
    versionNo: 1,
    tags: [MOCK_TAGS[5], MOCK_TAGS[0], MOCK_TAGS[1]],
    viewCount: 432,
    likeCount: 3,
    publishTime: '2026-04-18T16:00:00+08:00',
    updatedAt: '2026-04-18T16:00:00+08:00',
    createdAt: '2026-04-18T16:00:00+08:00',
  },
  {
    id: 6,
    title: '账户注销流程说明',
    htmlContent: '<p>本文说明用户账号注销的完整流程、条件与注意事项。</p><h2>注销流程</h2><ol><li>用户发起注销申请</li><li>系统校验账号状态</li><li>进入 7 天冷静期</li><li>注销完成</li></ol>',
    summary: '本文说明用户账号注销的完整流程、条件与注意事项。',
    categoryId: 21,
    categoryName: '账户管理',
    categoryPath: '账户系统 › 账户管理',
    productLine: '账户系统',
    moduleName: '账户管理',
    knowledgeType: '操作指南',
    status: 'draft',
    versionNo: 1,
    tags: [MOCK_TAGS[6]],
    viewCount: 0,
    likeCount: 0,
    updatedAt: '2026-08-02T10:00:00+08:00',
    createdAt: '2026-08-02T10:00:00+08:00',
  },
  {
    id: 7,
    title: 'SSO 单点登录集成指南',
    htmlContent: '<p>本文介绍企业 SSO 单点登录的集成方式，支持 OIDC 与 SAML 协议。</p><h2>集成步骤</h2><p>配置 IdP 信息、回调地址与密钥后即可启用。</p>',
    summary: '本文介绍企业 SSO 单点登录的集成方式，支持 OIDC 与 SAML 协议。',
    categoryId: 22,
    categoryName: '认证安全',
    categoryPath: '账户系统 › 认证安全',
    productLine: '账户系统',
    moduleName: '认证安全',
    knowledgeType: '接口文档',
    status: 'published',
    versionNo: 2,
    tags: [MOCK_TAGS[13], MOCK_TAGS[5]],
    viewCount: 328,
    likeCount: 4,
    publishTime: '2026-07-05T09:30:00+08:00',
    updatedAt: '2026-07-05T09:30:00+08:00',
    createdAt: '2026-06-20T09:30:00+08:00',
  },
  {
    id: 8,
    title: '订单批量导出方法',
    htmlContent: '<p>本文说明订单数据的批量导出方法，支持 CSV 与 Excel 格式。</p><h2>导出方式</h2><p>在订单列表页筛选条件后点击导出，后台异步生成文件并通知下载。</p>',
    summary: '本文说明订单数据的批量导出方法，支持 CSV 与 Excel 格式。',
    categoryId: 31,
    categoryName: '数据导出',
    categoryPath: '订单中心 › 数据导出',
    productLine: '订单中心',
    moduleName: '数据导出',
    knowledgeType: '操作指南',
    status: 'published',
    versionNo: 1,
    tags: [MOCK_TAGS[7], MOCK_TAGS[10]],
    viewCount: 512,
    likeCount: 8,
    publishTime: '2026-07-01T10:00:00+08:00',
    updatedAt: '2026-07-01T10:00:00+08:00',
    createdAt: '2026-07-01T10:00:00+08:00',
  },
  {
    id: 9,
    title: '退款流程说明',
    htmlContent: '<p>本文说明订单退款的整体流程、状态流转与处理时限。</p><h2>流程</h2><p>退款申请 → 审核 → 原路退回 → 完成。支持部分退款与全额退款。</p>',
    summary: '本文说明订单退款的整体流程、状态流转与处理时限。',
    categoryId: 32,
    categoryName: '退款流程',
    categoryPath: '订单中心 › 退款流程',
    productLine: '订单中心',
    moduleName: '退款流程',
    knowledgeType: '操作指南',
    status: 'published',
    versionNo: 1,
    tags: [MOCK_TAGS[8], MOCK_TAGS[10]],
    viewCount: 689,
    likeCount: 11,
    publishTime: '2026-07-18T15:00:00+08:00',
    updatedAt: '2026-07-18T15:00:00+08:00',
    createdAt: '2026-07-18T15:00:00+08:00',
  },
  {
    id: 10,
    title: '企业微信集成配置',
    htmlContent: '<p>本文介绍企业微信的集成配置，包括自建应用、通讯录同步与消息推送。</p><h2>配置步骤</h2><p>在企业微信管理后台创建自建应用，配置 CorpID 与 Secret，再在系统后台填入即可。</p>',
    summary: '本文介绍企业微信的集成配置，包括自建应用、通讯录同步与消息推送。',
    categoryId: 4,
    categoryName: '通知服务',
    categoryPath: '通知服务',
    productLine: '通知服务',
    moduleName: '通知服务',
    knowledgeType: '操作指南',
    status: 'published',
    versionNo: 2,
    tags: [MOCK_TAGS[9], MOCK_TAGS[12]],
    viewCount: 476,
    likeCount: 6,
    publishTime: '2026-07-22T09:00:00+08:00',
    updatedAt: '2026-07-22T09:00:00+08:00',
    createdAt: '2026-07-10T09:00:00+08:00',
  },
  {
    id: 11,
    title: 'API 接口认证方式说明',
    htmlContent: '<p>本文说明开放平台 API 的认证方式，支持 Basic Auth 与 Bearer Token。</p><h2>v1 Basic Auth</h2><p>使用 ClientId + ClientSecret 通过 Basic Auth 头访问。</p><h2>v2 Bearer Token</h2><p>先获取 access_token，再通过 Authorization: Bearer 头访问。</p>',
    summary: '本文说明开放平台 API 的认证方式，支持 Basic Auth 与 Bearer Token。',
    categoryId: 51,
    categoryName: 'API 文档',
    categoryPath: '开放平台 › API 文档',
    productLine: '开放平台',
    moduleName: 'API 文档',
    knowledgeType: '接口文档',
    status: 'published',
    versionNo: 1,
    tags: [MOCK_TAGS[4], MOCK_TAGS[5]],
    viewCount: 934,
    likeCount: 15,
    publishTime: '2026-07-10T11:00:00+08:00',
    updatedAt: '2026-07-10T11:00:00+08:00',
    createdAt: '2026-07-10T11:00:00+08:00',
  },
  {
    id: 12,
    title: '接口限流策略说明',
    htmlContent: '<p>本文说明开放平台接口的限流策略，采用令牌桶算法。</p><h2>限流规则</h2><p>默认 QPS 100，超出后返回 429 与 Retry-After 头。</p>',
    summary: '本文说明开放平台接口的限流策略，采用令牌桶算法。',
    categoryId: 51,
    categoryName: 'API 文档',
    categoryPath: '开放平台 › API 文档',
    productLine: '开放平台',
    moduleName: 'API 文档',
    knowledgeType: '政策说明',
    status: 'archived',
    versionNo: 1,
    tags: [MOCK_TAGS[11], MOCK_TAGS[4]],
    viewCount: 0,
    likeCount: 0,
    publishTime: '2026-03-01T09:00:00+08:00',
    updatedAt: '2026-03-01T09:00:00+08:00',
    createdAt: '2026-03-01T09:00:00+08:00',
  },
]

export const MOCK_VERSIONS: KnowledgeVersion[] = [
  { id: 31, knowledgeId: 1, versionNo: 3, title: '支付回调常见问题排查', changeNote: '补充重试记录与告警说明', createdByName: '张小美', createdAt: '2026-07-15T10:30:00+08:00', isCurrent: true },
  { id: 21, knowledgeId: 1, versionNo: 2, title: '支付回调常见问题排查', changeNote: '新增网络可达性检查', createdByName: '张小美', createdAt: '2026-06-20T10:15:00+08:00' },
  { id: 11, knowledgeId: 1, versionNo: 1, title: '支付回调常见问题排查', changeNote: '初版发布', createdByName: '王大伟', createdAt: '2026-05-01T09:00:00+08:00' },
]

export const MOCK_CHAT_SESSIONS: ChatSession[] = [
  { id: 1, title: '支付回调失败排查', updatedAt: '2026-08-14T10:00:00+08:00' },
  { id: 2, title: '用户账号注销流程', updatedAt: '2026-08-13T16:20:00+08:00' },
  { id: 3, title: 'API 鉴权失败处理', updatedAt: '2026-08-12T09:10:00+08:00' },
  { id: 4, title: '订单批量导出方法', updatedAt: '2026-08-10T14:45:00+08:00' },
  { id: 5, title: '企业微信集成配置', updatedAt: '2026-08-08T11:30:00+08:00' },
]

export const MOCK_MODELS: ModelInfo[] = [
  { key: 'claude', name: 'Claude Opus 5', desc: '最深度推理，适合复杂问题', dot: 'claude' },
  { key: 'gpt', name: 'GPT-4o', desc: '多模态能力强，响应快速', dot: 'gpt' },
  { key: 'gemini', name: 'Gemini 2.5 Pro', desc: '超长上下文，推理均衡', dot: 'gemini' },
  { key: 'deepseek', name: 'DeepSeek V3', desc: '高性价比，中文理解优秀', dot: 'deepseek' },
]

export const MODEL_NAMES: Record<string, string> = {
  claude: 'Claude Opus 5',
  gpt: 'GPT-4o',
  gemini: 'Gemini 2.5 Pro',
  deepseek: 'DeepSeek V3',
}

/** 根据提问生成模拟答案（Markdown 内容，供 SSE 流式打字机效果演示） */
export const mockAnswer = (question: string, model: string): ChatMessage => {
  const modelName = MODEL_NAMES[model] ?? model
  const confidences: ConfidenceLevel[] = ['high', 'medium', 'low']
  const confidence = confidences[question.length % 3]
  const q = question.trim() || '该问题'
  return {
    id: `a-${Date.now()}`,
    sessionId: 1,
    role: 'assistant',
    content:
      `根据知识库内容，关于「${q}」的排查结论如下：\n\n` +
      `**核心结论**：该问题通常由配置不正确或外部依赖异常导致，建议按以下步骤排查。\n\n` +
      `1. **检查配置**：进入对应管理后台核对相关配置项，确认与文档要求一致。\n` +
      `2. **查看日志**：定位到最近一次失败记录，关注错误码与时间点，判断是否与变更相关。\n` +
      `3. **验证连通性**：确认回调/请求地址可从公网访问，未被防火墙或安全组拦截。\n` +
      `4. **关注重试**：系统最多重试 5 次，若全部失败需检查服务端错误日志与告警。`,
    model: modelName,
    confidence,
    sources: [
      { knowledgeId: 1, title: '支付回调常见问题排查', categoryPath: '支付平台 › 故障排查' },
      { knowledgeId: 2, title: '支付回调配置指南', categoryPath: '支付平台 › 支付接入' },
      { knowledgeId: 4, title: '回调重试机制说明', categoryPath: '支付平台 › 系统机制' },
    ],
    createdAt: new Date().toISOString(),
  }
}

export const MOCK_CHAT_MESSAGES: Record<number, ChatMessage[]> = {
  1: [
    { id: 'm1', sessionId: 1, role: 'user', content: '客户反馈支付已经扣款成功，但我们的系统订单状态还是"待支付"，回调地址确认配置正确，请问怎么排查？', createdAt: '2026-08-14T09:58:00+08:00' },
    mockAnswer('客户反馈支付已经扣款成功', 'Claude Opus 5'),
  ],
  2: [
    { id: 'm2', sessionId: 2, role: 'user', content: '用户账号如何注销？', createdAt: '2026-08-13T16:18:00+08:00' },
    { id: 'm3', sessionId: 2, role: 'assistant', content: '账号注销流程：用户发起申请 → 系统校验 → 进入 7 天冷静期 → 注销完成。注销后数据保留 30 天，期间可申诉恢复。', model: 'GPT-4o', confidence: 'medium', createdAt: '2026-08-13T16:19:00+08:00' },
  ],
}

export function searchMockKnowledge(keyword: string): SearchResult[] {
  const kw = keyword.trim().toLowerCase()
  if (!kw) return []
  return MOCK_KNOWLEDGE.filter((k) => k.status === 'published' && (k.title.toLowerCase().includes(kw) || (k.summary ?? '').toLowerCase().includes(kw)))
    .map((k) => ({
      id: k.id,
      title: k.title,
      excerpt: k.summary ?? '',
      categoryPath: k.categoryPath,
      updatedAt: k.updatedAt,
      viewCount: k.viewCount,
      tags: k.tags.map((t) => t.name),
      status: k.status,
    }))
}

export const MOCK_OVERVIEW: AnalyticsOverview = {
  knowledgeTotal: 485,
  knowledgeNewThisWeek: 12,
  queryTotal: 1247,
  queryChangePercent: 18,
  adoptionRate: 92.6,
  adoptionChangePercent: 2.1,
  noResultRate: 8.3,
  noResultChangePercent: -1.2,
  updatedAt: '2026-08-14T10:30:00+08:00',
}

export const MOCK_QUERY_TREND: QueryTrendPoint[] = [
  { date: '08-08', count: 132 },
  { date: '08-09', count: 168 },
  { date: '08-10', count: 145 },
  { date: '08-11', count: 201 },
  { date: '08-12', count: 176 },
  { date: '08-13', count: 224 },
  { date: '08-14', count: 189 },
]

export const MOCK_CATEGORY_DISTRIBUTION: CategoryDistribution[] = [
  { name: '支付平台', value: 35 },
  { name: '账户系统', value: 20 },
  { name: '订单中心', value: 15 },
  { name: '通知服务', value: 12 },
  { name: '其他', value: 18 },
]

export const MOCK_HOT_SEARCH: HotSearchItem[] = [
  { rank: 1, keyword: '支付回调', count: 156 },
  { rank: 2, keyword: 'API鉴权', count: 134 },
  { rank: 3, keyword: '账号注销', count: 112 },
  { rank: 4, keyword: '退款流程', count: 98 },
  { rank: 5, keyword: '数据导出', count: 87 },
  { rank: 6, keyword: '企业微信', count: 76 },
  { rank: 7, keyword: '订单状态', count: 65 },
  { rank: 8, keyword: '限流策略', count: 54 },
  { rank: 9, keyword: 'Webhook配置', count: 48 },
  { rank: 10, keyword: 'SSO单点登录', count: 42 },
]
