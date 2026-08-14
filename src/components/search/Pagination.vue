<script setup lang="ts">
import { computed } from 'vue'

defineOptions({ name: 'AppPagination' })

const props = defineProps<{
  page: number
  size: number
  total: number
}>()

const emit = defineEmits<{
  change: [page: number]
}>()

const pageCount = computed(() => Math.max(1, Math.ceil(props.total / props.size)))

const pages = computed<(number | '...')[]>(() => {
  const total = pageCount.value
  const current = props.page
  const list: (number | '...')[] = []
  if (total <= 7) {
    for (let i = 1; i <= total; i++) list.push(i)
    return list
  }
  list.push(1)
  if (current > 3) list.push('...')
  for (let i = Math.max(2, current - 1); i <= Math.min(total - 1, current + 1); i++) list.push(i)
  if (current < total - 2) list.push('...')
  list.push(total)
  return list
})

function go(p: number) {
  if (p < 1 || p > pageCount.value || p === props.page) return
  emit('change', p)
}
</script>

<template>
  <nav v-if="pageCount > 1" class="pagination">
    <button type="button" class="page-btn" :disabled="page <= 1" @click="go(page - 1)">‹</button>
    <template v-for="(p, i) in pages" :key="`${p}-${i}`">
      <button
        v-if="p !== '...'"
        type="button"
        class="page-btn"
        :class="{ active: p === page }"
        @click="go(p)"
      >
        {{ p }}
      </button>
      <span v-else class="ellipsis">…</span>
    </template>
    <button type="button" class="page-btn" :disabled="page >= pageCount" @click="go(page + 1)">›</button>
  </nav>
</template>

<style scoped>
.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 24px;
}

.page-btn {
  min-width: 34px;
  height: 34px;
  padding: 0 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #fff;
  color: var(--text-secondary);
  font-size: 0.875rem;
  cursor: pointer;
  font-family: var(--font-body);
  transition: all var(--transition-fast);
}

.page-btn:hover:not(:disabled):not(.active) {
  border-color: var(--primary);
  color: var(--primary);
}

.page-btn.active {
  background: var(--primary);
  border-color: var(--primary);
  color: #fff;
  font-weight: 600;
}

.page-btn:disabled {
  opacity: 0.45;
  cursor: not-allowed;
}

.ellipsis {
  color: var(--text-tertiary);
  padding: 0 4px;
}
</style>
