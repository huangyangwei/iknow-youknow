<script setup lang="ts">
defineOptions({ name: 'ModeTabs' })

const props = defineProps<{
  modelValue: 'search' | 'qa'
}>()

const emit = defineEmits<{
  'update:modelValue': [value: 'search' | 'qa']
}>()
</script>

<template>
  <div class="search-mode-tabs">
    <button
      type="button"
      class="search-mode-tab"
      :class="{ active: props.modelValue === 'search' }"
      @click="emit('update:modelValue', 'search')"
    >
      🔍 搜索文档
    </button>
    <button
      type="button"
      class="search-mode-tab"
      :class="{ active: props.modelValue === 'qa' }"
      @click="emit('update:modelValue', 'qa')"
    >
      💬 智能问答
    </button>
  </div>
</template>

<style scoped>
.search-mode-tabs {
  display: inline-flex;
  background: var(--surface-2);
  border-radius: 99px;
  padding: 4px;
  margin-bottom: 24px;
  gap: 2px;
}

.search-mode-tab {
  padding: 8px 20px;
  border-radius: 99px;
  border: none;
  background: none;
  font-family: var(--font-body);
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
}

.search-mode-tab.active {
  background: #fff;
  color: var(--text-primary);
  box-shadow: var(--shadow-xs);
  font-weight: 600;
}
</style>
