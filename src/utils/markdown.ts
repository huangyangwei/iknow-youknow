import MarkdownIt from 'markdown-it'
import { sanitizeHtml } from './sanitize'

const markdown = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true,
})

/** 渲染 Markdown → 经 DOMPurify 白名单过滤后的安全 HTML（XSS 防护） */
export function renderMarkdown(source: string): string {
  return sanitizeHtml(markdown.render(source ?? ''))
}
