<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { feedbackApi } from '@/api/feedback'
import { formatDateTime } from '@/utils/format'
import type { FeedbackItem, FeedbackStatus } from '@/types/api'

defineOptions({ name: 'FeedbackView' })

const items = ref<FeedbackItem[]>([])
const total = ref(0)
const loading = ref(false)

const query = reactive<{ status: FeedbackStatus | ''; page: number; size: number }>({
  status: '',
  page: 1,
  size: 10,
})

async function load() {
  loading.value = true
  try {
    const res = await feedbackApi.list({ ...query })
    items.value = res.items
    total.value = res.total
  } finally {
    loading.value = false
  }
}

onMounted(load)

const TYPE_LABELS: Record<string, string> = {
  like: '有帮助',
  dislike: '没帮助',
  correction: '内容纠错',
  suggestion: '建议补充',
}

const STATUS_LABELS: Record<FeedbackStatus, string> = {
  pending: '待处理',
  processing: '处理中',
  resolved: '已解决',
}

function statusClass(status: FeedbackStatus) {
  return status === 'pending' ? 'warn' : status === 'processing' ? 'info' : 'ok'
}

function statusLabel(status: FeedbackStatus) {
  return STATUS_LABELS[status] ?? String(status)
}
</script>

<template>
  <div class="feedback">
    <header class="page-head">
      <h1>反馈管理</h1>
      <p class="muted">跟踪问答质量与用户建议</p>
    </header>

    <div class="toolbar">
      <el-radio-group v-model="query.status" @change="query.page = 1; load()">
        <el-radio-button value="">全部</el-radio-button>
        <el-radio-button value="pending">待处理</el-radio-button>
        <el-radio-button value="processing">处理中</el-radio-button>
        <el-radio-button value="resolved">已解决</el-radio-button>
      </el-radio-group>
      <div class="spacer"></div>
      <span class="count">共 {{ total }} 条</span>
    </div>

    <div class="table-wrap">
      <el-table :data="items" v-loading="loading">
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <span class="type-tag">{{ TYPE_LABELS[row.type] ?? row.type }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="sourceTitle" label="来源" min-width="200">
          <template #default="{ row }">
            {{ row.sourceTitle || row.question || '—' }}
          </template>
        </el-table-column>
        <el-table-column prop="content" label="内容" min-width="240">
          <template #default="{ row }">{{ row.content || '—' }}</template>
        </el-table-column>
        <el-table-column prop="categoryPath" label="分类" min-width="140">
          <template #default="{ row }">
            <span class="muted">{{ row.categoryPath || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdByName" label="提交人" width="100">
          <template #default="{ row }">{{ row.createdByName || '—' }}</template>
        </el-table-column>
        <el-table-column prop="createdAt" label="提交时间" width="140">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <span class="status" :class="statusClass(row.status)">
              {{ statusLabel(row.status) }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<style scoped>
.feedback {
  max-width: 1200px;
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

.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.spacer {
  flex: 1;
}

.count {
  font-size: 0.8125rem;
  color: var(--text-tertiary);
}

.table-wrap {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.type-tag {
  display: inline-block;
  font-size: 0.6875rem;
  padding: 2px 8px;
  border-radius: 99px;
  background: var(--surface-2);
  color: var(--text-secondary);
}

.status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.6875rem;
  font-weight: 600;
}

.status::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.status.warn {
  color: var(--warning, #b45309);
}
.status.warn::before {
  background: var(--warning, #d97706);
}

.status.info {
  color: var(--primary);
}
.status.info::before {
  background: var(--primary);
}

.status.ok {
  color: var(--success, #15803d);
}
.status.ok::before {
  background: var(--success, #16a34a);
}
</style>
