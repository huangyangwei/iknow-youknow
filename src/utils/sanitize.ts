import DOMPurify from 'dompurify'

/**
 * 富文本渲染白名单：仅放行 wangEditor 输出的结构化标签，
 * 过滤 script/on* 事件/iframe 等危险内容（XSS 防护）。
 */
const ALLOWED_TAGS = [
  'p', 'br', 'hr',
  'h1', 'h2', 'h3', 'h4', 'h5', 'h6',
  'strong', 'b', 'em', 'i', 'u', 's', 'span', 'sub', 'sup', 'mark', 'small',
  'ul', 'ol', 'li',
  'blockquote', 'pre', 'code',
  'a', 'img',
  'table', 'thead', 'tbody', 'tr', 'th', 'td',
  'div', 'section', 'video', 'audio',
]

const ALLOWED_ATTR = [
  'href', 'target', 'rel', 'title', 'alt', 'src',
  'class', 'colspan', 'rowspan', 'width', 'height',
  'controls', 'loop', 'muted', 'poster',
]

const ALLOWED_URI_REGEXP = /^(?:https?|mailto|tel|data):/i

const CONFIG = {
  ALLOWED_TAGS,
  ALLOWED_ATTR,
  ALLOW_DATA_ATTR: false,
  ALLOWED_URI_REGEXP,
  ALLOW_UNKNOWN_PROTOCOLS: false,
  FORBID_TAGS: ['style', 'script', 'iframe', 'object', 'embed', 'form', 'input', 'textarea'],
  FORBID_ATTR: ['style', 'onerror', 'onclick', 'onload', 'onmouseover'],
}

export function sanitizeHtml(html: string): string {
  if (!html) return ''
  return DOMPurify.sanitize(html, CONFIG)
}
