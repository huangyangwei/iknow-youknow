<script setup lang="ts">
import { computed } from 'vue'
import type { FeedbackStatus, FeedbackType } from '@/types/api'

defineOptions({ name: 'FeedbackTabs' })

const props = defineProps<{
  status: FeedbackStatus | ''
  type: FeedbackType | ''
  counts: { pending: number; processing: number; resolved: number }
  total: number
}>()

const emit = defineEmits<{
  'update:status': [value: FeedbackStatus | '']
  'update:type': [value: FeedbackType | '']
  change: []
}>()

const TYPE_OPTIONS: { value: FeedbackType; label: string }[] = [
  { value: 'like', label: '有帮助' },
  { value: 'dislike', label: '没帮助' },
  { value: 'correction', label: '内容纠错' },
  { value: 'suggestion', label: '建议补充' },
]

const STATUS_TABS = computed<{ value: FeedbackStatus | ''; label: string; count: number }[]>(() => [
  { value: '', label: '全部', count: props.total },
  { value: 'pending', label: '待处理', count: props.counts.pending },
  { value: 'processing', label: '处理中', count: props.counts.processing },
  { value: 'resolved', label: '已解决', count: props.counts.resolved },
])

function onStatusChange(value: FeedbackStatus | ''): void {
  emit('update:status', value)
  emit('change')
}

function onTypeChange(value: FeedbackType | ''): void {
  emit('update:type', value)
  emit('change')
}
</script>

<template>
  <div class="feedback-tabs">
    <el-radio-group :model-value="status" @update:model-value="onStatusChange">
      <el-radio-button v-for="tab in STATUS_TABS" :key="String(tab.value)" :value="tab.value">
        {{ tab.label }}<span v-if="tab.count > 0" class="tab-count">({{ tab.count }})</span>
      </el-radio-button>
    </el-radio-group>

    <div class="spacer"></div>

    <el-select
      :model-value="type"
      placeholder="全部类型"
      clearable
      style="width: 150px"
      @update:model-value="onTypeChange"
    >
      <el-option v-for="opt in TYPE_OPTIONS" :key="opt.value" :value="opt.value" :label="opt.label" />
    </el-select>
  </div>
</template>

<style scoped>
.feedback-tabs {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.spacer {
  flex: 1;
}

.tab-count {
  font-weight: 400;
  margin-left: 2px;
  opacity: 0.75;
}
</style>
