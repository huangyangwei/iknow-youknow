<script setup lang="ts">
defineOptions({ name: 'TagChip' })

defineProps<{
  name: string
  count?: number
}>()
</script>

<template>
  <span class="tag-chip">
    <span class="hash">#</span>{{ name }}<span v-if="count != null" class="count">{{ count }}</span>
  </span>
</template>

<style scoped>
.tag-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 0.75rem;
  color: var(--text-secondary);
  background: var(--surface-1);
  border: 1px solid var(--border-light);
  border-radius: 99px;
  padding: 3px 10px;
  white-space: nowrap;
}

.hash {
  color: var(--primary);
  font-weight: 600;
}

.count {
  color: var(--text-tertiary);
  font-size: 0.6875rem;
}
</style>
