/**
 * 轻量 token 持有者：解耦 axios 拦截器与 Pinia store，
 * 避免 http ↔ store 循环依赖（由 authStore 在登录/登出时同步）。
 */
let accessToken: string | null = null

export const tokenStore = {
  get: (): string | null => accessToken,
  set: (token: string | null): void => {
    accessToken = token
  },
}
