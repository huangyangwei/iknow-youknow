<script setup lang="ts">
import { ref } from 'vue'
import type { KnowledgeStatus } from '@/types/api'

defineOptions({ name: 'PublishPanel' })

const props = defineProps<{
  status: KnowledgeStatus
  saving?: boolean
}>()

const emit = defineEmits<{
  'save-draft': []
  publish: []
  'publish-scheduled': [scheduledAt: string]
}>()

const scheduledAt = ref<string>('')
const showScheduled = ref(false)

function publishScheduled() {
  if (!scheduledAt.value) return
  emit('publish-scheduled', new Date(scheduledAt.value).toISOString())
}
</script>

<template>
  <div class="publish-panel">
    <div class="head">
      <div class="status-line">
        <span class="label">当前状态</span>
        <span class="status-text">{{ status === 'published' ? '已发布' : status === 'draft' ? '草稿' : status === 'pending_publish' ? '待发布' : '已归档' }}</span>
      </div>
    </div>

    <button type="button" class="btn-primary publish-btn" :disabled="saving" @click="emit('publish')">
      {{ saving ? '处理中…' : '发布' }}
    </button>

    <button type="button" class="btn-secondary" :disabled="saving" @click="emit('save-draft')">
      保存草稿
    </button>

    <button type="button" class="link-btn" @click="showScheduled = !showScheduled">
      {{ showScheduled ? '收起定时发布' : '定时发布…' }}
    </button>

    <div v-if="showScheduled" class="scheduled">
      <el-date-picker
        v-model="scheduledAt"
        type="datetime"
        placeholder="选择发布时间"
        :disabled-date="(d: Date) => d.getTime() < Date.now()"
        style="width: 100%"
      />
      <button type="button" class="btn-secondary" :disabled="!scheduledAt" @click="publishScheduled">
        确认定时发布
      </button>
    </div>
  </div>
</template>

<style scoped>
.publish-panel {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  position: sticky;
  top: 32px;
}

.head {
  margin-bottom: 4px;
}

.status-line {
  display: flex;
  align-items: center;
  gap: 8px;
}

.label {
  font-size: 0.75rem;
  color: var(--text-tertiary);
}

.status-text {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--text-primary);
}

.publish-btn,
.btn-secondary {
  width: 100%;
  padding: 10px;
  font-size: 0.875rem;
}

.link-btn {
  border: none;
  background: none;
  color: var(--text-tertiary);
  font-size: 0.75rem;
  cursor: pointer;
  font-family: var(--font-body);
  padding: 0;
  text-align: left;
}

.link-btn:hover {
  color: var(--primary);
}

.scheduled {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding-top: 8px;
  border-top: 1px dashed var(--border-light);
}
</style>
