export function formatDate(value?: string | number | Date, fallback = '—'): string {
  if (value == null || value === '') return fallback
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return fallback
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
}

export function formatDateTime(value?: string | number | Date, fallback = '—'): string {
  if (value == null || value === '') return fallback
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return fallback
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${formatDate(date)} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

export function formatCount(value?: number): string {
  if (value == null) return '0'
  return value >= 1000 ? `${(value / 1000).toFixed(1).replace(/\.0$/, '')}k` : String(value)
}

/** 先对原文做 HTML 转义，再包 <mark>，防止标题/摘录中的富文本触发存储型 XSS */
export function highlightText(text: string, keyword: string): string {
  const kw = keyword.trim()
  if (!kw) return escapeHtml(text)
  const escapedKw = escapeHtml(kw).replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return escapeHtml(text).replace(new RegExp(`(${escapedKw})`, 'gi'), '<mark>$1</mark>')
}

function escapeHtml(value: string): string {
  return value
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;')
}
