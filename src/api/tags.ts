import { http } from './http'
import type { Tag } from '@/types/api'

export const tagApi = {
  list: () => http.get<Tag[]>('/tags').then((r) => r.data),
}
