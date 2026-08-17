<script setup lang="ts">
import { formatDateTime } from '@/utils/format'
import type { FeedbackItem, FeedbackStatus, FeedbackType } from '@/types/api'

defineOptions({ name: 'FeedbackList' })

defineProps<{
  items: FeedbackItem[]
  total: number
  page: number
  size: number
  loading: boolean
}>()

const emit = defineEmits<{
  handle: [item: FeedbackItem]
  'update:page': [page: number]
  'update:size': [size: number]
}>()

const TYPE_LABELS: Record<FeedbackType, string> = {
  like: '有帮助',
  dislike: '没帮助',
  correction: '内容纠错',
  suggestion: '建议补充',
}

const TYPE_CLASSES: Record<FeedbackType, string> = {
  like: 'like',
  dislike: 'dislike',
  correction: 'correction',
  suggestion: 'suggestion',
}

const STATUS_LABELS: Record<FeedbackStatus, string> = {
  pending: '待处理',
  processing: '处理中',
  resolved: '已解决',
}

function statusClass(status: FeedbackStatus): string {
  return status === 'pending' ? 'warn' : status === 'processing' ? 'info' : 'ok'
}

function statusLabel(row: FeedbackItem): string {
  return STATUS_LABELS[row.status] ?? row.status
}

function sourceText(row: FeedbackItem): string {
  if (row.sourceTitle) return row.sourceTitle
  if (row.question) return row.question
  return '—'
}
</script>

<template>
  <div class="feedback-list">
    <div class="table-wrap">
      <el-table :data="items" v-loading="loading" row-key="id">
        <el-table-column label="类型" width="104">
          <template #default="{ row }">
            <span class="type-tag" :class="TYPE_CLASSES[row.type as FeedbackType]">
              {{ TYPE_LABELS[row.type as FeedbackType] ?? row.type }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="来源" min-width="180">
          <template #default="{ row }">
            <div class="source-cell">
              <span class="source-title">{{ sourceText(row) }}</span>
              <span v-if="row.sourceType === 'answer'" class="source-kind">问答答案</span>
              <span v-else-if="row.sourceType === 'knowledge'" class="source-kind">知识内容</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="内容" min-width="220">
          <template #default="{ row }">
            <span class="content-cell">{{ row.content || row.question || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="分类" min-width="130">
          <template #default="{ row }">
            <span class="muted">{{ row.categoryPath || '—' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="createdByName" label="提交人" width="96">
          <template #default="{ row }">{{ row.createdByName || '—' }}</template>
        </el-table-column>
        <el-table-column label="提交时间" width="140">
          <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="96">
          <template #default="{ row }">
            <span class="status" :class="statusClass(row.status)">
              {{ statusLabel(row) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="96" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status !== 'resolved'" link type="primary" size="small" @click="emit('handle', row)">
              处理
            </el-button>
            <el-button v-else link type="info" size="small" @click="emit('handle', row)">
              详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="pagination-bar">
      <el-pagination
        :current-page="page"
        :page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @update:current-page="(p: number) => emit('update:page', p)"
        @update:page-size="(s: number) => emit('update:size', s)"
      />
    </div>
  </div>
</template>

<style scoped>
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

.type-tag.like {
  background: var(--success-subtle, #dcfce7);
  color: var(--success, #15803d);
}

.type-tag.dislike {
  background: var(--danger-subtle, #fee2e2);
  color: var(--danger, #dc2626);
}

.type-tag.correction {
  background: var(--warning-subtle, #fef3c7);
  color: var(--warning, #b45309);
}

.type-tag.suggestion {
  background: var(--primary-subtle, #dbeafe);
  color: var(--primary, #2563eb);
}

.source-cell {
  display: flex;
  flex-direction: column;
  gap: 2px;
  min-width: 0;
}

.source-title {
  font-weight: 500;
  color: var(--text-primary);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-kind {
  font-size: 0.6875rem;
  color: var(--text-tertiary);
}

.content-cell {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  color: var(--text-secondary);
  font-size: 0.8125rem;
  line-height: 1.5;
}

.muted {
  color: var(--text-tertiary);
  font-size: 0.8125rem;
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

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
