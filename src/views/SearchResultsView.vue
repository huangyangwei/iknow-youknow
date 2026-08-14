<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { searchApi } from '@/api/search'
import { useKnowledgeStore } from '@/stores/knowledge'
import ResultCard from '@/components/search/ResultCard.vue'
import FilterPanel from '@/components/search/FilterPanel.vue'
import AppPagination from '@/components/search/Pagination.vue'
import type { SearchParams, SearchResult } from '@/types/api'

defineOptions({ name: 'SearchResultsView' })

const route = useRoute()
const knowledge = useKnowledgeStore()

const params = reactive<SearchParams>({
  keyword: '',
  categoryId: null,
  tagId: null,
  knowledgeType: null,
  timeFrom: null,
  timeTo: null,
  page: 1,
  size: 10,
  sort: 'relevance',
})

const items = ref<SearchResult[]>([])
const total = ref(0)
const loading = ref(false)
const error = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await searchApi.search({ ...params })
    items.value = res.items
    total.value = res.total
  } catch {
    error.value = '搜索服务暂不可用，请稍后重试'
    items.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

watch(
  () => route.query.keyword,
  (kw) => {
    if (typeof kw === 'string') {
      params.keyword = kw
      params.page = 1
      void load()
    }
  },
)

onMounted(async () => {
  const kw = route.query.keyword
  params.keyword = typeof kw === 'string' ? kw : ''
  await knowledge.loadOptions().catch(() => {})
  void load()
})

const tagNames = () => knowledge.tags.map((t) => t.name)
</script>

<template>
  <div class="search-results">
    <header class="results-head">
      <h1>
        搜索结果
        <span v-if="params.keyword" class="kw">「{{ params.keyword }}」</span>
      </h1>
      <p class="summary">
        找到 <strong>{{ total }}</strong> 条相关知识
      </p>
    </header>

    <div class="results-layout">
      <FilterPanel
        :categories="knowledge.categories"
        :tags="tagNames()"
        :model-value="params"
        @update:model-value="Object.assign(params, $event)"
        @apply="params.page = 1; load()"
      />

      <div class="results-main">
        <div v-if="loading" class="skeleton-list">
          <el-skeleton v-for="i in 5" :key="i" :rows="3" animated class="sk" />
        </div>

        <div v-else-if="error" class="empty">
          <div class="empty-icon">⚠️</div>
          <p>{{ error }}</p>
          <button type="button" class="btn-secondary" @click="load">重试</button>
        </div>

        <div v-else-if="!items.length" class="empty">
          <div class="empty-icon">🔍</div>
          <p>没有找到与「{{ params.keyword }}」相关的知识</p>
          <p class="empty-hint">试试更换关键词，或调整左侧筛选条件</p>
        </div>

        <template v-else>
          <ResultCard v-for="r in items" :key="r.id" :result="r" :keyword="params.keyword" />

          <AppPagination
            :page="params.page"
            :size="params.size"
            :total="total"
            @change="(p) => { params.page = p; load() }"
          />
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.search-results {
  max-width: 1100px;
  margin: 0 auto;
}

.results-head {
  margin-bottom: 24px;
}

.results-head h1 {
  font-family: var(--font-heading);
  font-size: 1.375rem;
  margin-bottom: 4px;
}

.results-head .kw {
  color: var(--primary);
}

.summary {
  color: var(--text-secondary);
  font-size: 0.875rem;
}

.summary strong {
  color: var(--primary);
  font-weight: 600;
}

.results-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 24px;
  align-items: start;
}

.results-main {
  min-width: 0;
}

.skeleton-list .sk {
  margin-bottom: 12px;
  border-radius: var(--radius-md);
  padding: 8px;
}

.empty {
  text-align: center;
  padding: 64px 0;
  color: var(--text-secondary);
}

.empty-icon {
  font-size: 2.5rem;
  margin-bottom: 12px;
}

.empty-hint {
  color: var(--text-tertiary);
  font-size: 0.8125rem;
  margin-top: 6px;
}

@media (max-width: 860px) {
  .results-layout {
    grid-template-columns: 1fr;
  }
}
</style>
