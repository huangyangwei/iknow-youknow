<script setup lang="ts">
import { computed } from 'vue'
import type { Category, KnowledgeStatus } from '@/types/api'

defineOptions({ name: 'KnowledgeToolbar' })

const props = defineProps<{
  keyword: string
  categoryId: number | null
  status: KnowledgeStatus | ''
  categories: Category[]
  canCreate: boolean
}>()

const emit = defineEmits<{
  'update:keyword': [value: string]
  'update:categoryId': [value: number | null]
  'update:status': [value: KnowledgeStatus | '']
  search: []
  create: []
}>()

const STATUS_OPTIONS: { value: KnowledgeStatus | ''; label: string }[] = [
  { value: '', label: '全部状态' },
  { value: 'draft', label: '草稿' },
  { value: 'pending_publish', label: '待发布' },
  { value: 'published', label: '已发布' },
  { value: 'archived', label: '已归档' },
]

const flatCategories = computed(() => {
  const list: { id: number; name: string }[] = []
  const walk = (items: Category[], prefix = '') => {
    for (const c of items) {
      list.push({ id: c.id, name: prefix ? `${prefix} / ${c.name}` : c.name })
      if (c.children?.length) walk(c.children, prefix ? `${prefix} / ${c.name}` : c.name)
    }
  }
  walk(props.categories)
  return list
})
</script>

<template>
  <div class="toolbar">
    <div class="search-field">
      <input
        :value="keyword"
        type="text"
        placeholder="搜索标题 / 摘要…"
        @input="emit('update:keyword', ($event.target as HTMLInputElement).value)"
        @keydown.enter="emit('search')"
      />
      <button type="button" class="btn-search" title="搜索" @click="emit('search')">→</button>
    </div>

    <el-select
      :model-value="categoryId"
      class="filter-select"
      placeholder="全部分类"
      clearable
      @update:model-value="emit('update:categoryId', ($event ?? null) as number | null)"
    >
      <el-option
        v-for="c in flatCategories"
        :key="c.id"
        :label="c.name"
        :value="c.id"
      />
    </el-select>

    <el-select
      :model-value="status"
      class="filter-select"
      placeholder="全部状态"
      @update:model-value="emit('update:status', ($event ?? '') as KnowledgeStatus | '')"
    >
      <el-option v-for="s in STATUS_OPTIONS" :key="s.value" :label="s.label" :value="s.value" />
    </el-select>

    <div class="spacer"></div>

    <button v-if="canCreate" type="button" class="btn-primary create-btn" @click="emit('create')">
      + 新建知识
    </button>
  </div>
</template>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.search-field {
  display: flex;
  align-items: center;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  padding: 0 4px 0 12px;
  min-width: 240px;
  flex: 1;
  max-width: 380px;
}

.search-field input {
  flex: 1;
  border: none;
  outline: none;
  padding: 8px 0;
  font-size: 0.875rem;
  font-family: var(--font-body);
  color: var(--text-primary);
  background: transparent;
}

.btn-search {
  width: 30px;
  height: 30px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--primary);
  color: #fff;
  cursor: pointer;
}

.filter-select {
  width: 150px;
}

.spacer {
  flex: 1;
}

.create-btn {
  padding: 9px 16px;
  font-size: 0.875rem;
}
</style>
