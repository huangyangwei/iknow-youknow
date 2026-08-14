import axios, { type AxiosInstance, type InternalAxiosRequestConfig, type AxiosResponse } from 'axios'
import { tokenStore } from './token'
import type { ApiResult } from '@/types/api'

/** 业务错误（后端统一错误码，见 ResultCode） */
export class ApiError extends Error {
  code: number

  constructor(code: number, message: string) {
    super(message)
    this.name = 'ApiError'
    this.code = code
  }
}

export const http: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 20000,
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = tokenStore.get()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

http.interceptors.response.use(
  (response: AxiosResponse<ApiResult<unknown> | unknown>) => {
    const body = response.data as ApiResult<unknown>
    if (body && typeof body === 'object' && 'code' in body) {
      if (body.code !== 0) {
        return Promise.reject(new ApiError(body.code, body.message || '请求失败'))
      }
      // 解包统一响应体：调用方直接拿到 data
      response.data = body.data
    }
    return response
  },
  (error) => {
    const status: number | undefined = error.response?.status
    const body = error.response?.data as ApiResult<unknown> | undefined
    if (status === 401) {
      tokenStore.set(null)
      window.dispatchEvent(new CustomEvent('auth:unauthorized'))
      return Promise.reject(new ApiError(body?.code ?? 2001, body?.message ?? '未登录或登录已过期'))
    }
    const message = body?.message ?? (status ? `请求失败（${status}）` : '网络异常，请稍后重试')
    return Promise.reject(new ApiError(body?.code ?? (status ?? -1), message))
  },
)

