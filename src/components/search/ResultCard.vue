<script setup lang="ts">
import { useRouter } from 'vue-router'
import { highlightText, formatDate } from '@/utils/format'
import type { SearchResult } from '@/types/api'

defineOptions({ name: 'ResultCard' })

const props = defineProps<{
  result: SearchResult
  keyword?: string
}>()

const router = useRouter()

function open() {
  void router.push({ name: 'knowledge-detail', params: { id: props.result.id } })
}
</script>

<template>
  <article class="result-card" @click="open">
    <div class="result-head">
      <h3 class="result-title" v-html="highlightText(result.title, keyword ?? '')"></h3>
      <span v-if="result.status === 'archived'" class="status-tag archived">已归档</span>
    </div>
    <p class="result-excerpt" v-html="highlightText(result.excerpt, keyword ?? '')"></p>
    <div class="result-meta">
      <span v-if="result.categoryPath" class="meta-item">📁 {{ result.categoryPath }}</span>
      <span v-if="result.score != null" class="meta-item">
        相关度
        <span class="score-bar"><span class="score-fill" :style="{ width: `${Math.round(result.score * 100)}%` }"></span></span>
        {{ Math.round(result.score * 100) }}%
      </span>
      <span v-if="result.viewCount != null" class="meta-item">👁 {{ result.viewCount }}</span>
      <span class="meta-item">更新于 {{ formatDate(result.updatedAt) }}</span>
    </div>
    <div v-if="result.tags?.length" class="result-tags">
      <span v-for="tag in result.tags.slice(0, 4)" :key="tag" class="tag-chip"># {{ tag }}</span>
    </div>
  </article>
</template>

<style scoped>
.result-card {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 20px 24px;
  cursor: pointer;
  transition: all var(--transition-base);
  margin-bottom: 12px;
}

.result-card:hover {
  border-color: var(--primary);
  box-shadow: var(--shadow-sm);
  transform: translateY(-1px);
}

.result-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.result-title {
  font-size: 1.0625rem;
  font-weight: 600;
  color: var(--text-primary);
  margin: 0;
  line-height: 1.5;
}

.result-title :deep(mark) {
  background: var(--primary-light);
  color: var(--primary);
  border-radius: 3px;
  padding: 0 2px;
}

.status-tag {
  font-size: 0.6875rem;
  padding: 2px 8px;
  border-radius: 99px;
  background: var(--surface-2);
  color: var(--text-tertiary);
  flex-shrink: 0;
}

.result-excerpt {
  color: var(--text-secondary);
  font-size: 0.875rem;
  line-height: 1.7;
  margin: 8px 0 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.result-excerpt :deep(mark) {
  background: var(--primary-light);
  color: var(--primary);
  border-radius: 3px;
  padding: 0 2px;
}

.result-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 0.75rem;
  color: var(--text-tertiary);
  align-items: center;
}

.meta-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.score-bar {
  width: 44px;
  height: 4px;
  border-radius: 2px;
  background: var(--surface-2);
  overflow: hidden;
  display: inline-block;
}

.score-fill {
  display: block;
  height: 100%;
  background: var(--primary);
  border-radius: 2px;
}

.result-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.tag-chip {
  font-size: 0.6875rem;
  color: var(--text-tertiary);
  background: var(--surface-1);
  border: 1px solid var(--border-light);
  border-radius: 99px;
  padding: 2px 10px;
}
</style>
