/**
 * 轻量 token 持有者：解耦 axios 拦截器与 Pinia store，
 * 避免 http ↔ store 循环依赖（由 authStore 在登录/登出时同步）。
 */
let accessToken: string | null = null
const STORAGE_KEY = 'iknow.token'
const MOCK_TOKEN_PREFIX = 'mock-token-'

export function normalizeAccessToken(token: string | null): string | null {
  if (!token) return null
  if (import.meta.env.VITE_USE_MOCK === 'false' && token.startsWith(MOCK_TOKEN_PREFIX)) return null
  return token
}

function readPersistedToken(): string | null {
  if (typeof window === 'undefined') return null

  const value = window.localStorage.getItem(STORAGE_KEY)
  if (!value) return null

  try {
    const parsed = JSON.parse(value) as unknown
    return typeof parsed === 'string' ? normalizeAccessToken(parsed) : null
  } catch {
    return normalizeAccessToken(value)
  }
}

export const tokenStore = {
  get: (): string | null => {
    if (!accessToken) accessToken = readPersistedToken()
    return accessToken
  },
  set: (token: string | null): void => {
    accessToken = normalizeAccessToken(token)
  },
}
