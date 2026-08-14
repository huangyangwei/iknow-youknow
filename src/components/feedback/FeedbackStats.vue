<script setup lang="ts">
import type { FeedbackStatsData } from '@/types/api'

defineOptions({ name: 'FeedbackStats' })

const props = defineProps<{
  stats: FeedbackStatsData
}>()

const cards = [
  { label: '待处理反馈', value: () => String(props.stats.pending), note: '等待管理员处理', tone: 'warn' },
  { label: '本月总反馈', value: () => String(props.stats.monthlyTotal), note: '当月提交的反馈量', tone: 'default' },
  { label: '平均处理时长', value: () => (props.stats.avgHandleDays > 0 ? props.stats.avgHandleDays.toFixed(1) : '0'), suffix: ' 天', note: '已解决反馈的平均处理时间', tone: 'default' },
]
</script>

<template>
  <div class="feedback-stats">
    <div v-for="c in cards" :key="c.label" class="stat-card" :class="c.tone">
      <div class="stat-value">{{ c.value() }}<span v-if="c.suffix" class="stat-suffix">{{ c.suffix }}</span></div>
      <div class="stat-label">{{ c.label }}</div>
      <div class="stat-note">{{ c.note }}</div>
    </div>
  </div>
</template>

<style scoped>
.feedback-stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.stat-card {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 16px 20px;
}

.stat-card.warn {
  border-color: var(--warning-subtle, #fde68a);
  background: linear-gradient(180deg, var(--warning-subtle, #fffbeb), #fff 70%);
}

.stat-value {
  font-family: var(--font-heading);
  font-size: 1.5rem;
  font-weight: 700;
  color: var(--text-primary);
}

.stat-suffix {
  font-size: 0.8125rem;
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
</style>
