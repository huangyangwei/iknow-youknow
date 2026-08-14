<script setup lang="ts">
import type { KnowledgeStatus } from '@/types/api'

defineOptions({ name: 'StatusBadge' })

const props = defineProps<{
  status: KnowledgeStatus
}>()

const STATUS_META: Record<KnowledgeStatus, { label: string; className: string }> = {
  draft: { label: '草稿', className: 'draft' },
  pending_publish: { label: '待发布', className: 'pending' },
  published: { label: '已发布', className: 'published' },
  archived: { label: '已归档', className: 'archived' },
}

const meta = () => STATUS_META[props.status] ?? { label: props.status, className: 'draft' }
</script>

<template>
  <span class="status-badge" :class="meta().className">{{ meta().label }}</span>
</template>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 2px 10px;
  border-radius: 99px;
  font-size: 0.6875rem;
  font-weight: 600;
  white-space: nowrap;
}

.status-badge::before {
  content: '';
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.draft {
  background: var(--surface-2);
  color: var(--text-secondary);
}
.draft::before {
  background: var(--text-tertiary);
}

.pending {
  background: var(--warning-subtle, #fef3c7);
  color: var(--warning, #b45309);
}
.pending::before {
  background: var(--warning, #d97706);
}

.published {
  background: var(--success-subtle, #dcfce7);
  color: var(--success, #15803d);
}
.published::before {
  background: var(--success, #16a34a);
}

.archived {
  background: var(--surface-2);
  color: var(--text-tertiary);
}
.archived::before {
  background: var(--text-tertiary);
}
</style>
