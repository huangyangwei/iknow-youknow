<script setup lang="ts">
import { formatDateTime } from '@/utils/format'
import type { KnowledgeVersion } from '@/types/api'

defineOptions({ name: 'VersionHistory' })

defineProps<{
  versions: KnowledgeVersion[]
  currentVersionNo?: number
}>()

const emit = defineEmits<{
  rollback: [versionNo: number]
}>()
</script>

<template>
  <div class="version-history">
    <div v-if="!versions.length" class="empty">暂无历史版本</div>
    <div v-for="v in versions" :key="v.id" class="version-item">
      <div class="version-line">
        <div class="version-head">
          <span class="version-no">v{{ v.versionNo }}</span>
          <span v-if="v.isCurrent || v.versionNo === currentVersionNo" class="current">当前</span>
          <span v-else class="author">{{ v.createdByName || '—' }}</span>
        </div>
        <span class="time">{{ formatDateTime(v.createdAt) }}</span>
      </div>
      <div v-if="v.changeNote" class="note">{{ v.changeNote }}</div>
      <div class="actions">
        <button
          v-if="!(v.isCurrent || v.versionNo === currentVersionNo)"
          type="button"
          class="rollback-btn"
          @click="emit('rollback', v.versionNo)"
        >
          回滚到此版本
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.version-history {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.version-item {
  padding: 12px 14px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  background: var(--surface-1);
}

.version-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.version-head {
  display: flex;
  align-items: center;
  gap: 8px;
}

.version-no {
  font-weight: 600;
  font-size: 0.875rem;
  color: var(--text-primary);
}

.current {
  font-size: 0.6875rem;
  padding: 1px 8px;
  border-radius: 99px;
  background: var(--success-subtle, #dcfce7);
  color: var(--success, #15803d);
  font-weight: 600;
}

.author {
  font-size: 0.75rem;
  color: var(--text-tertiary);
}

.time {
  font-size: 0.75rem;
  color: var(--text-tertiary);
}

.note {
  font-size: 0.8125rem;
  color: var(--text-secondary);
  margin-top: 6px;
}

.actions {
  margin-top: 8px;
}

.rollback-btn {
  border: none;
  background: none;
  color: var(--primary);
  font-size: 0.75rem;
  cursor: pointer;
  font-family: var(--font-body);
  padding: 0;
}

.rollback-btn:hover {
  text-decoration: underline;
}

.empty {
  color: var(--text-tertiary);
  font-size: 0.875rem;
  text-align: center;
  padding: 24px 0;
}
</style>
