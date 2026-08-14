<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref } from 'vue'
import * as echarts from 'echarts'
import { analyticsApi } from '@/api/analytics'
import { MOCK_CATEGORY_DISTRIBUTION, MOCK_OVERVIEW, MOCK_QUERY_TREND } from '@/mock/data'
import type { AnalyticsOverview, CategoryDistribution, HotSearchItem, QueryTrendPoint } from '@/types/api'

defineOptions({ name: 'DashboardView' })

const overview = ref<AnalyticsOverview | null>(null)
const trend = ref<QueryTrendPoint[]>([])
const distribution = ref<CategoryDistribution[]>([])
const hotSearches = ref<HotSearchItem[]>([])

const trendRef = ref<HTMLDivElement>()
const pieRef = ref<HTMLDivElement>()
let trendChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

function renderTrend() {
  if (!trendRef.value) return
  trendChart ??= echarts.init(trendRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: trend.value.map((t) => t.date), boundaryGap: false },
    yAxis: { type: 'value' },
    series: [
      {
        name: '问答次数',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 7,
        data: trend.value.map((t) => t.count),
        lineStyle: { width: 3, color: '#2563eb' },
        itemStyle: { color: '#2563eb' },
        areaStyle: { color: 'rgba(37, 99, 235, 0.08)' },
      },
    ],
  })
}

function renderPie() {
  if (!pieRef.value) return
  pieChart ??= echarts.init(pieRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0, icon: 'circle' },
    series: [
      {
        name: '分类占比',
        type: 'pie',
        radius: ['42%', '68%'],
        center: ['50%', '44%'],
        itemStyle: { borderRadius: 6, borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b} {d}%' },
        data: distribution.value.map((d) => ({ name: d.name, value: d.value })),
      },
    ],
  })
}

function resize() {
  trendChart?.resize()
  pieChart?.resize()
}

onMounted(async () => {
  window.addEventListener('resize', resize)
  try {
    const [over, tr, dist, hot] = await Promise.all([
      analyticsApi.overview(),
      analyticsApi.queryTrend(),
      analyticsApi.categoryDistribution(),
      analyticsApi.hotSearch(),
    ])
    overview.value = over
    trend.value = tr
    distribution.value = dist
    hotSearches.value = hot
  } catch {
    overview.value = MOCK_OVERVIEW
    trend.value = MOCK_QUERY_TREND
    distribution.value = MOCK_CATEGORY_DISTRIBUTION
    hotSearches.value = []
  }
  renderTrend()
  renderPie()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  trendChart?.dispose()
  pieChart?.dispose()
  trendChart = null
  pieChart = null
})

const cards = [
  { label: '知识总量', value: () => overview.value?.knowledgeTotal ?? 0, suffix: '', note: () => `本周新增 ${overview.value?.knowledgeNewThisWeek ?? 0}` },
  { label: '累计问答', value: () => overview.value?.queryTotal ?? 0, suffix: '', note: () => `环比 ${overview.value && overview.value.queryChangePercent >= 0 ? '+' : ''}${overview.value?.queryChangePercent ?? 0}%` },
  { label: '答案采纳率', value: () => overview.value?.adoptionRate ?? 0, suffix: '%', note: () => `环比 +${overview.value?.adoptionChangePercent ?? 0}%` },
  { label: '无结果率', value: () => overview.value?.noResultRate ?? 0, suffix: '%', note: () => `环比 ${overview.value && overview.value.noResultChangePercent >= 0 ? '+' : ''}${overview.value?.noResultChangePercent ?? 0}%` },
]
</script>

<template>
  <div class="dashboard">
    <header class="page-head">
      <h1>数据仪表盘</h1>
      <p class="muted">知识库问答系统运行概览</p>
    </header>

    <div class="stat-grid">
      <div v-for="c in cards" :key="c.label" class="stat-card">
        <div class="stat-value">{{ c.value() }}<span class="suffix">{{ c.suffix }}</span></div>
        <div class="stat-label">{{ c.label }}</div>
        <div class="stat-note">{{ c.note() }}</div>
      </div>
    </div>

    <div class="chart-grid">
      <div class="card chart-card">
        <h2>近 7 日问答趋势</h2>
        <div ref="trendRef" class="chart"></div>
      </div>
      <div class="card chart-card">
        <h2>分类分布</h2>
        <div ref="pieRef" class="chart"></div>
      </div>
    </div>

    <div v-if="hotSearches.length" class="card hot-card">
      <h2>热搜排行 Top 10</h2>
      <ol class="hot-list">
        <li v-for="h in hotSearches" :key="h.rank">
          <span class="rank" :class="{ top: h.rank <= 3 }">{{ h.rank }}</span>
          <span class="keyword">{{ h.keyword }}</span>
          <span class="count">{{ h.count }} 次</span>
        </li>
      </ol>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 1100px;
  margin: 0 auto;
}

.page-head {
  margin-bottom: 20px;
}

.page-head h1 {
  font-family: var(--font-heading);
  font-size: 1.375rem;
  margin-bottom: 4px;
}

.muted {
  color: var(--text-secondary);
  font-size: 0.875rem;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(180px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 18px 20px;
}

.stat-value {
  font-family: var(--font-heading);
  font-size: 1.625rem;
  font-weight: 700;
  color: var(--text-primary);
}

.suffix {
  font-size: 0.875rem;
  color: var(--text-tertiary);
  font-weight: 600;
  margin-left: 2px;
}

.stat-label {
  font-size: 0.8125rem;
  color: var(--text-secondary);
  margin-top: 4px;
}

.stat-note {
  font-size: 0.6875rem;
  color: var(--text-tertiary);
  margin-top: 4px;
}

.chart-grid {
  display: grid;
  grid-template-columns: 3fr 2fr;
  gap: 16px;
  margin-bottom: 20px;
}

.card {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 20px;
}

.chart-card h2,
.hot-card h2 {
  font-family: var(--font-heading);
  font-size: 1rem;
  margin-bottom: 12px;
}

.chart {
  height: 300px;
}

.hot-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.hot-list li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  background: var(--surface-1);
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
}

.count {
  font-size: 0.75rem;
  color: var(--text-tertiary);
}

@media (max-width: 860px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
