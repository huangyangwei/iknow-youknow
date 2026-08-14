<script setup lang="ts">
import { onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { analyticsApi, type AnalyticsRange } from '@/api/analytics'
import type { AnalyticsOverview, CategoryDistribution, HotSearchItem, QueryTrendPoint } from '@/types/api'

defineOptions({ name: 'DashboardView' })

const overview = ref<AnalyticsOverview | null>(null)
const trend = ref<QueryTrendPoint[]>([])
const distribution = ref<CategoryDistribution[]>([])
const hotSearches = ref<HotSearchItem[]>([])
const range = ref<AnalyticsRange>('week')
const loading = ref(false)
const error = ref('')

const trendRef = ref<HTMLDivElement>()
const pieRef = ref<HTMLDivElement>()
let trendChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

const RANGE_OPTIONS: { value: AnalyticsRange; label: string }[] = [
  { value: 'today', label: '今日' },
  { value: 'week', label: '本周' },
  { value: 'month', label: '本月' },
  { value: 'quarter', label: '近三月' },
]

function renderTrend(): void {
  if (!trendRef.value) return
  trendChart ??= echarts.init(trendRef.value)
  trendChart.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: { type: 'category', data: trend.value.map((t) => t.date), boundaryGap: false },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      {
        name: '反馈数',
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

function renderPie(): void {
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

function resize(): void {
  trendChart?.resize()
  pieChart?.resize()
}

async function load(): Promise<void> {
  loading.value = true
  error.value = ''
  try {
    const [over, tr, dist, hot] = await Promise.all([
      analyticsApi.overview(range.value),
      analyticsApi.feedbackTrend(range.value),
      analyticsApi.categoryDistribution(),
      analyticsApi.hotSearch(range.value),
    ])
    overview.value = over
    trend.value = tr
    distribution.value = dist
    hotSearches.value = hot
  } catch (e) {
    error.value = e instanceof Error ? e.message : '加载失败，请稍后重试'
    ElMessage.error('仪表盘数据加载失败')
  } finally {
    loading.value = false
    renderTrend()
    renderPie()
  }
}

watch(range, () => {
  void load()
})

onMounted(() => {
  window.addEventListener('resize', resize)
  void load()
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  trendChart?.dispose()
  pieChart?.dispose()
  trendChart = null
  pieChart = null
})

const cards = [
  { label: '知识总量', value: () => overview.value?.knowledgeTotal ?? 0, suffix: '', note: () => `本周新增 ${overview.value?.knowledgeNewThisWeek ?? 0} 篇` },
  { label: '查询次数', value: () => overview.value?.queryTotal ?? 0, suffix: '', note: () => `环比 ${overview.value && overview.value.queryChangePercent >= 0 ? '+' : ''}${overview.value?.queryChangePercent ?? 0}%` },
  { label: '答案采纳率', value: () => overview.value?.adoptionRate ?? 0, suffix: '%', note: () => `环比 +${overview.value?.adoptionChangePercent ?? 0}%` },
  { label: '无结果率', value: () => overview.value?.noResultRate ?? 0, suffix: '%', note: () => `环比 ${overview.value && overview.value.noResultChangePercent >= 0 ? '+' : ''}${overview.value?.noResultChangePercent ?? 0}%` },
]
</script>

<template>
  <div class="dashboard" v-loading="loading">
    <header class="page-head">
      <h1>数据仪表盘</h1>
      <p class="muted">知识库运营数据概览</p>
    </header>

    <div class="range-bar">
      <el-radio-group v-model="range">
        <el-radio-button v-for="opt in RANGE_OPTIONS" :key="opt.value" :value="opt.value">
          {{ opt.label }}
        </el-radio-button>
      </el-radio-group>
      <span v-if="overview?.updatedAt" class="updated-at">数据更新于 {{ overview.updatedAt }}</span>
    </div>

    <div v-if="error" class="error-banner">
      <span>⚠️ {{ error }}</span>
      <el-button size="small" @click="load">重试</el-button>
    </div>

    <div class="stat-grid">
      <div v-for="c in cards" :key="c.label" class="stat-card">
        <div class="stat-value">{{ c.value() }}<span class="suffix">{{ c.suffix }}</span></div>
        <div class="stat-label">{{ c.label }}</div>
        <div class="stat-note">{{ c.note() }}</div>
      </div>
    </div>

    <div class="chart-grid">
      <div class="card chart-card">
        <h2>📈 反馈趋势</h2>
        <div ref="trendRef" class="chart"></div>
      </div>
      <div class="card chart-card">
        <h2>🏷️ 分类分布</h2>
        <div ref="pieRef" class="chart"></div>
      </div>
    </div>

    <div class="card hot-card">
      <h2>🔍 热搜排行 Top 10</h2>
      <ol v-if="hotSearches.length" class="hot-list">
        <li v-for="h in hotSearches" :key="h.rank">
          <span class="rank" :class="{ top: h.rank <= 3 }">{{ h.rank }}</span>
          <span class="keyword">{{ h.keyword }}</span>
          <span class="count">{{ h.count }} 次</span>
        </li>
      </ol>
      <div v-else class="hot-empty">暂无搜索数据</div>
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

.range-bar {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.updated-at {
  font-size: 0.75rem;
  color: var(--text-tertiary);
}

.error-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  background: var(--danger-subtle, #fee2e2);
  color: var(--danger, #dc2626);
  border-radius: var(--radius-md);
  padding: 12px 16px;
  margin-bottom: 16px;
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

.hot-empty {
  color: var(--text-tertiary);
  font-size: 0.875rem;
  padding: 16px 0;
  text-align: center;
}

@media (max-width: 860px) {
  .chart-grid {
    grid-template-columns: 1fr;
  }
}
</style>
