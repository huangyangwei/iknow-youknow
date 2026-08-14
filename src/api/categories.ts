import { http } from './http'
import type { Category } from '@/types/api'

export const categoryApi = {
  list: () => http.get<Category[]>('/categories').then((r) => r.data),
}
