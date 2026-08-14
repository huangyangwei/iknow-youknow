<script setup lang="ts">
import { computed } from 'vue'
import type { Category, SearchParams } from '@/types/api'

defineOptions({ name: 'FilterPanel' })

const props = defineProps<{
  categories: Category[]
  tags: string[]
  modelValue: SearchParams
}>()

const emit = defineEmits<{
  'update:modelValue': [value: SearchParams]
  apply: []
}>()

const KNOWLEDGE_TYPES = ['操作指南', '故障排查', '接口文档', '政策说明', 'FAQ'] as const

const flatCategories = computed(() => {
  const list: Category[] = []
  const walk = (items: Category[]) => {
    for (const c of items) {
      list.push(c)
      if (c.children?.length) walk(c.children)
    }
  }
  walk(props.categories)
  return list
})

// 标签按列表序号对齐 tagId（1 起），与 mock/后端约定保持一致
const tagNameById = computed<Record<number, string>>(() => {
  const map: Record<number, string> = {}
  props.tags.forEach((t, i) => {
    map[i + 1] = t
  })
  return map
})

const sortOptions = [
  { value: 'relevance', label: '相关度' },
  { value: 'updatedAt', label: '最近更新' },
  { value: 'viewCount', label: '浏览量' },
]

function set(patch: Partial<SearchParams>) {
  emit('update:modelValue', { ...props.modelValue, ...patch })
}

function reset() {
  emit('update:modelValue', {
    ...props.modelValue,
    categoryId: null,
    tagId: null,
    knowledgeType: null,
    timeFrom: null,
    timeTo: null,
  })
}
</script>

<template>
  <aside class="filter-panel">
    <div class="filter-head">
      <h3>筛选</h3>
      <button type="button" class="reset-btn" @click="reset">重置</button>
    </div>

    <div class="filter-group">
      <div class="filter-title">分类</div>
      <button
        type="button"
        class="filter-option"
        :class="{ active: modelValue.categoryId == null }"
        @click="set({ categoryId: null })"
      >
        全部分类
      </button>
      <button
        v-for="c in flatCategories"
        :key="c.id"
        type="button"
        class="filter-option"
        :class="{ active: modelValue.categoryId === c.id }"
        @click="set({ categoryId: c.id })"
      >
        {{ c.name }}
      </button>
    </div>

    <div class="filter-group">
      <div class="filter-title">知识类型</div>
      <button
        v-for="t in KNOWLEDGE_TYPES"
        :key="t"
        type="button"
        class="filter-option"
        :class="{ active: modelValue.knowledgeType === t }"
        @click="set({ knowledgeType: modelValue.knowledgeType === t ? null : t })"
      >
        {{ t }}
      </button>
    </div>

    <div class="filter-group">
      <div class="filter-title">标签</div>
      <button
        v-for="t in tags"
        :key="t"
        type="button"
        class="filter-option"
        :class="{ active: tagNameById[modelValue.tagId ?? -1] === t }"
        @click="set({ tagId: tagNameById[modelValue.tagId ?? -1] === t ? null : tags.indexOf(t) + 1 })"
      >
        {{ t }}
      </button>
    </div>

    <div class="filter-group">
      <div class="filter-title">排序</div>
      <button
        v-for="s in sortOptions"
        :key="s.value"
        type="button"
        class="filter-option"
        :class="{ active: modelValue.sort === s.value }"
        @click="set({ sort: s.value as SearchParams['sort'] })"
      >
        {{ s.label }}
      </button>
    </div>

    <button type="button" class="apply-btn" @click="emit('apply')">应用筛选</button>
  </aside>
</template>

<style scoped>
.filter-panel {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 20px;
  align-self: flex-start;
  position: sticky;
  top: 32px;
}

.filter-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.filter-head h3 {
  font-size: 1rem;
  font-weight: 600;
  margin: 0;
}

.reset-btn {
  border: none;
  background: none;
  color: var(--text-tertiary);
  font-size: 0.75rem;
  cursor: pointer;
  font-family: var(--font-body);
}

.reset-btn:hover {
  color: var(--primary);
}

.filter-group {
  margin-bottom: 20px;
}

.filter-title {
  font-size: 0.6875rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--text-tertiary);
  margin-bottom: 8px;
}

.filter-option {
  display: block;
  width: 100%;
  text-align: left;
  padding: 7px 10px;
  border: none;
  background: none;
  border-radius: var(--radius-sm);
  font-size: 0.8125rem;
  color: var(--text-secondary);
  cursor: pointer;
  font-family: var(--font-body);
  transition: all var(--transition-fast);
}

.filter-option:hover {
  background: var(--surface-1);
  color: var(--text-primary);
}

.filter-option.active {
  background: var(--primary-subtle);
  color: var(--primary);
  font-weight: 600;
}

.apply-btn {
  width: 100%;
  padding: 9px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--primary);
  color: #fff;
  font-size: 0.875rem;
  font-weight: 600;
  cursor: pointer;
  font-family: var(--font-body);
  transition: background var(--transition-fast);
}

.apply-btn:hover {
  background: var(--primary-hover);
}
</style>
