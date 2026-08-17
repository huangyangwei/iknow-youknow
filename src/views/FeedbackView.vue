<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { feedbackApi } from '@/api/feedback'
import FeedbackList from '@/components/feedback/FeedbackList.vue'
import FeedbackStats from '@/components/feedback/FeedbackStats.vue'
import FeedbackTabs from '@/components/feedback/FeedbackTabs.vue'
import { formatDateTime } from '@/utils/format'
import type { FeedbackItem, FeedbackStatsData, FeedbackStatus, FeedbackType } from '@/types/api'

defineOptions({ name: 'FeedbackView' })

const items = ref<FeedbackItem[]>([])
const total = ref(0)
const loading = ref(false)
const stats = ref<FeedbackStatsData>({ pending: 0, processing: 0, resolved: 0, monthlyTotal: 0, avgHandleDays: 0 })

const query = reactive<{ status: FeedbackStatus | ''; type: FeedbackType | ''; page: number; size: number }>({
  status: '',
  type: '',
  page: 1,
  size: 10,
})

async function load(): Promise<void> {
  loading.value = true
  try {
    const res = await feedbackApi.list({ ...query })
    items.value = res.items
    total.value = res.total
  } finally {
    loading.value = false
  }
}

/** 汇总统计：从全量列表推导待处理/处理中/已解决计数、本月总量与平均处理时长（契约无独立统计接口） */
async function loadStats(): Promise<void> {
  try {
    const res = await feedbackApi.list({ status: '', type: '', page: 1, size: 1000 })
    const now = new Date()
    const monthStart = new Date(now.getFullYear(), now.getMonth(), 1)
    const pending = res.items.filter((f) => f.status === 'pending').length
    const processing = res.items.filter((f) => f.status === 'processing').length
    const resolved = res.items.filter((f) => f.status === 'resolved').length
    const monthTotal = res.items.filter((f) => new Date(f.createdAt) >= monthStart).length
    const handled = res.items.filter((f) => f.status === 'resolved' && f.handledAt)
    const avgHandleDays = handled.length
      ? handled.reduce((sum, f) => sum + (new Date(f.handledAt!).getTime() - new Date(f.createdAt).getTime()) / 86_400_000, 0) / handled.length
      : 0
    stats.value = { pending, processing, resolved, monthlyTotal: monthTotal, avgHandleDays }
  } catch {
    // 统计失败时保持默认值，列表仍可正常展示
  }
}

function onFilterChange(): void {
  query.page = 1
  void load()
}

function onPageChange(page: number): void {
  query.page = page
  void load()
}

function onSizeChange(size: number): void {
  query.size = size
  query.page = 1
  void load()
}

// ===== 处理反馈 =====
const dialogVisible = ref(false)
const currentItem = ref<FeedbackItem | null>(null)
const handleStatus = ref<FeedbackStatus>('processing')
const handleNote = ref('')
const handling = ref(false)

function openHandle(item: FeedbackItem): void {
  currentItem.value = item
  handleStatus.value = item.status === 'resolved' ? 'resolved' : 'processing'
  handleNote.value = item.handleNote ?? ''
  dialogVisible.value = true
}

async function submitHandle(): Promise<void> {
  const item = currentItem.value
  if (!item) return
  handling.value = true
  try {
    await feedbackApi.handle(item.id, { status: handleStatus.value, handleNote: handleNote.value.trim() })
    ElMessage.success(handleStatus.value === 'resolved' ? '反馈已标记为已解决' : '反馈已标记为处理中')
    dialogVisible.value = false
    await Promise.all([load(), loadStats()])
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '处理失败，请稍后重试')
  } finally {
    handling.value = false
  }
}

const TYPE_LABELS: Record<FeedbackType, string> = {
  like: '有帮助',
  dislike: '没帮助',
  correction: '内容纠错',
  suggestion: '建议补充',
}

onMounted(() => {
  void load()
  void loadStats()
})
</script>

<template>
  <div class="feedback">
    <header class="page-head">
      <h1>反馈管理</h1>
      <p class="muted">处理运营人员提交的纠错、补充建议和评价反馈</p>
    </header>

    <FeedbackStats :stats="stats" />

    <FeedbackTabs
      v-model:status="query.status"
      v-model:type="query.type"
      :counts="{ pending: stats.pending, processing: stats.processing, resolved: stats.resolved }"
      :total="total"
      @change="onFilterChange"
    />

    <FeedbackList
      :items="items"
      :total="total"
      :page="query.page"
      :size="query.size"
      :loading="loading"
      @handle="openHandle"
      @update:page="onPageChange"
      @update:size="onSizeChange"
    />

    <el-dialog
      v-model="dialogVisible"
      :title="currentItem?.status === 'resolved' ? '反馈详情' : '处理反馈'"
      width="min(560px, calc(100vw - 40px))"
      append-to-body
    >
      <div v-if="currentItem" class="detail-block">
        <div class="detail-meta">
          <span class="type-tag">{{ TYPE_LABELS[currentItem.type] ?? currentItem.type }}</span>
          <span class="meta-text">{{ currentItem.sourceTitle || currentItem.question || '—' }}</span>
        </div>
        <div v-if="currentItem.content" class="detail-content">「{{ currentItem.content }}」</div>
        <div class="detail-foot-meta">
          <span>👤 {{ currentItem.createdByName || '—' }}</span>
          <span>🕐 {{ formatDateTime(currentItem.createdAt) }}</span>
          <span v-if="currentItem.categoryPath">📁 {{ currentItem.categoryPath }}</span>
        </div>

        <div v-if="currentItem.status === 'resolved'" class="handle-result">
          <div class="handle-line">
            <span class="handle-label">处理结果：</span>
            <span class="handle-note">{{ currentItem.handleNote || '—' }}</span>
          </div>
          <div class="handle-meta">
            <span v-if="currentItem.handlerName">处理人：{{ currentItem.handlerName }}</span>
            <span v-if="currentItem.handledAt">处理时间：{{ formatDateTime(currentItem.handledAt) }}</span>
          </div>
        </div>
      </div>

      <template v-if="currentItem && currentItem.status !== 'resolved'">
        <el-radio-group v-model="handleStatus" class="status-radio">
          <el-radio-button value="processing">处理中</el-radio-button>
          <el-radio-button value="resolved">已解决</el-radio-button>
        </el-radio-group>
        <el-input
          v-model="handleNote"
          type="textarea"
          :rows="4"
          class="note-input"
          placeholder="填写处理意见（如：已更新知识内容 / 已核实无需修改）…"
        />
      </template>

      <template v-if="currentItem && currentItem.status === 'resolved'" #footer>
        <el-button @click="dialogVisible = false">关闭</el-button>
      </template>
      <template v-else #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="handling" @click="submitHandle">
          {{ handleStatus === 'resolved' ? '标记已解决' : '标记处理中' }}
        </el-button>
      </template>
    </el-dialog>
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

.detail-block {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-meta {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.type-tag {
  font-size: 0.6875rem;
  padding: 2px 10px;
  border-radius: 99px;
  background: var(--surface-2);
  color: var(--text-secondary);
  font-weight: 600;
}

.meta-text {
  font-weight: 600;
  font-size: 0.9375rem;
  color: var(--text-primary);
}

.detail-content {
  background: var(--surface-1);
  border-radius: var(--radius-sm);
  padding: 10px 14px;
  font-size: 0.875rem;
  color: var(--text-secondary);
  line-height: 1.6;
}

.detail-foot-meta {
  display: flex;
  gap: 14px;
  flex-wrap: wrap;
  font-size: 0.75rem;
  color: var(--text-tertiary);
}

.handle-result {
  margin-top: 4px;
  padding-top: 12px;
  border-top: 1px dashed var(--border);
}

.handle-line {
  font-size: 0.875rem;
  color: var(--text-secondary);
}

.handle-label {
  font-weight: 600;
  color: var(--text-primary);
}

.handle-note {
  line-height: 1.6;
}

.handle-meta {
  margin-top: 6px;
  display: flex;
  gap: 14px;
  font-size: 0.75rem;
  color: var(--text-tertiary);
}

.status-radio {
  margin-top: 16px;
}

.note-input {
  margin-top: 12px;
}
</style>
