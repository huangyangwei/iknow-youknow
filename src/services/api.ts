import { useSessionStore } from '../stores/session'
import type { ApiPage, SearchResult } from '../types/api'

const baseUrl = '/api/v1'
export async function apiFetch<T>(path: string, init: RequestInit = {}): Promise<T> {
  const session = useSessionStore()
  const response = await fetch(`${baseUrl}${path}`, { ...init, headers: { 'Content-Type': 'application/json', ...(session.token ? { Authorization: `Bearer ${session.token}` } : {}), ...init.headers } })
  if (!response.ok) throw new Error((await response.json().catch(() => null))?.message ?? `Request failed: ${response.status}`)
  return response.status === 204 ? undefined as T : response.json() as Promise<T>
}
export function searchKnowledge(query: string, filters: Record<string, string>) { const params = new URLSearchParams({ q: query, ...filters }); return apiFetch<ApiPage<SearchResult>>(`/search?${params}`) }
