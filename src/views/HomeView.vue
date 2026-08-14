<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import ModeTabs from '@/components/ModeTabs.vue'
import SearchHero from '@/components/SearchHero.vue'
import { useChatStore } from '@/stores/chat'
import { analyticsApi } from '@/api/analytics'
import { MOCK_HOT_SEARCH, MOCK_OVERVIEW } from '@/mock/data'
import type { AnalyticsOverview, HotSearchItem } from '@/types/api'

defineOptions({ name: 'HomeView' })

const router = useRouter()
const chat = useChatStore()

const mode = ref<'search' | 'qa'>('search')
const modelKey = ref('claude')
const overview = ref<AnalyticsOverview | null>(null)
const hotSearches = ref<HotSearchItem[]>([])

function goSearch(keyword: string) {
  void router.push({ name: 'search', query: { keyword } })
}

function goAsk(question: string) {
  chat.newChat()
  chat.setModel(modelKey.value)
  void router.push({ name: 'chat', query: { q: question } })
}

function selectModel(key: string) {
  modelKey.value = key
}

onMounted(async () => {
  // 数据仪表盘接口：Mock 已就绪时直接可用；后端未就绪时兜底到静态数据
  try {
    const [over, hot] = await Promise.all([analyticsApi.overview(), analyticsApi.hotSearch()])
    overview.value = over
    hotSearches.value = hot
  } catch {
    overview.value = MOCK_OVERVIEW
    hotSearches.value = MOCK_HOT_SEARCH
  }
})

const stats = [
  { label: '知识条目', value: () => overview.value?.knowledgeTotal ?? 0, suffix: '', note: () => `本周新增 ${overview.value?.knowledgeNewThisWeek ?? 0}` },
  { label: '累计问答', value: () => overview.value?.queryTotal ?? 0, suffix: '', note: () => `环比 ${overview.value && overview.value.queryChangePercent >= 0 ? '+' : ''}${overview.value?.queryChangePercent ?? 0}%` },
  { label: '答案采纳率', value: () => overview.value?.adoptionRate ?? 0, suffix: '%', note: () => `环比 +${overview.value?.adoptionChangePercent ?? 0}%` },
  { label: '无结果率', value: () => overview.value?.noResultRate ?? 0, suffix: '%', note: () => `环比 ${overview.value && overview.value.noResultChangePercent >= 0 ? '+' : ''}${overview.value?.noResultChangePercent ?? 0}%` },
]
</script>

<template>
  <div class="home">
    <ModeTabs v-model="mode" />

    <SearchHero
      :mode="mode"
      :model-key="modelKey"
      :hot-questions="hotSearches.slice(0, 8).map((h) => h.keyword)"
      @search="goSearch"
      @ask="goAsk"
      @update:model-key="selectModel"
    />

    <section class="stats-row">
      <div v-for="s in stats" :key="s.label" class="stat-card">
        <div class="stat-value">
          {{ s.value() }}<span class="stat-suffix">{{ s.suffix }}</span>
        </div>
        <div class="stat-label">{{ s.label }}</div>
        <div class="stat-note">{{ s.note() }}</div>
      </div>
    </section>

    <section v-if="hotSearches.length" class="hot-ranking">
      <h2>热搜排行</h2>
      <div class="hot-list">
        <button
          v-for="h in hotSearches"
          :key="h.rank"
          type="button"
          class="hot-item"
          @click="goSearch(h.keyword)"
        >
          <span class="rank" :class="{ top: h.rank <= 3 }">{{ h.rank }}</span>
          <span class="keyword">{{ h.keyword }}</span>
          <span class="count">{{ h.count }} 次</span>
        </button>
      </div>
    </section>
  </div>
</template>

<style scoped>
.home {
  max-width: 960px;
  margin: 0 auto;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-top: 40px;
}

.stat-card {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 20px;
  box-shadow: var(--shadow-xs);
}

.stat-value {
  font-family: var(--font-heading);
  font-size: 1.75rem;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.stat-suffix {
  font-size: 0.9375rem;
  font-weight: 600;
  color: var(--text-tertiary);
  margin-left: 2px;
}

.stat-label {
  font-size: 0.8125rem;
  color: var(--text-secondary);
  margin-top: 6px;
}

.stat-note {
  font-size: 0.6875rem;
  color: var(--text-tertiary);
  margin-top: 4px;
}

.hot-ranking {
  margin-top: 40px;
}

.hot-ranking h2 {
  font-family: var(--font-heading);
  font-size: 1.125rem;
  margin-bottom: 16px;
}

.hot-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 8px;
}

.hot-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-family: var(--font-body);
  text-align: left;
}

.hot-item:hover {
  border-color: var(--primary);
  background: var(--primary-subtle);
}

.rank {
  width: 22px;
  height: 22px;
  border-radius: 6px;
  background: var(--surface-2);
  color: var(--text-tertiary);
  font-size: 0.75rem;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.rank.top {
  background: var(--primary);
  color: #fff;
}

.keyword {
  flex: 1;
  font-size: 0.875rem;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.count {
  font-size: 0.6875rem;
  color: var(--text-tertiary);
  flex-shrink: 0;
}
</style>
