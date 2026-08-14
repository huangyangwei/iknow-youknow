import { reactive, ref } from 'vue'
import { defineStore } from 'pinia'
import { categoryApi } from '@/api/categories'
import { knowledgeApi } from '@/api/knowledge'
import { tagApi } from '@/api/tags'
import type { Category, KnowledgeItem, KnowledgeStatus, Tag } from '@/types/api'

export interface KnowledgeListQuery {
  keyword: string
  categoryId: number | null
  status: KnowledgeStatus | ''
  page: number
  size: number
}

export const useKnowledgeStore = defineStore('knowledge', () => {
  const items = ref<KnowledgeItem[]>([])
  const total = ref(0)
  const loading = ref(false)
  const categories = ref<Category[]>([])
  const tags = ref<Tag[]>([])

  const query = reactive<KnowledgeListQuery>({
    keyword: '',
    categoryId: null,
    status: '',
    page: 1,
    size: 10,
  })

  async function fetchList(): Promise<void> {
    loading.value = true
    try {
      const res = await knowledgeApi.list({
        keyword: query.keyword || undefined,
        categoryId: query.categoryId,
        status: query.status,
        page: query.page,
        size: query.size,
      })
      items.value = res.items
      total.value = res.total
    } finally {
      loading.value = false
    }
  }

  async function loadOptions(): Promise<void> {
    const [catRes, tagRes] = await Promise.all([categoryApi.list(), tagApi.list()])
    categories.value = catRes
    tags.value = tagRes
  }

  function resetQuery(): void {
    query.keyword = ''
    query.categoryId = null
    query.status = ''
    query.page = 1
  }

  return {
    items,
    total,
    loading,
    categories,
    tags,
    query,
    fetchList,
    loadOptions,
    resetQuery,
  }
})
