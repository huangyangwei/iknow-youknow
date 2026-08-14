import { http } from './http'
import type { LoginPayload, LoginResponse, UserInfo } from '@/types/api'

export const authApi = {
  login: (payload: LoginPayload) => http.post<LoginResponse>('/auth/login', payload).then((r) => r.data),
  me: () => http.get<UserInfo>('/auth/me').then((r) => r.data),
}
