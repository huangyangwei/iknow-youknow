<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { knowledgeApi } from '@/api/knowledge'
import { ApiError } from '@/api/http'
import ArticleContent from '@/components/knowledge/ArticleContent.vue'
import TagChip from '@/components/knowledge/TagChip.vue'
import FeedbackActions from '@/components/knowledge/FeedbackActions.vue'
import { formatCount, formatDate } from '@/utils/format'
import type { FeedbackType, KnowledgeItem } from '@/types/api'

defineOptions({ name: 'KnowledgeDetailView' })

const route = useRoute()
const router = useRouter()

const item = ref<KnowledgeItem | null>(null)
const loading = ref(true)
const error = ref('')

async function load() {
  const id = Number(route.params.id)
  if (!id) {
    error.value = '无效的知识条目'
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    item.value = await knowledgeApi.detail(id)
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

watch(() => route.params.id, load, { immediate: true })

onMounted(load)

function handleFeedback(type: FeedbackType) {
  const map: Record<FeedbackType, string> = {
    like: '感谢反馈，已记录「有帮助」',
    dislike: '已记录「没帮助」，我们将持续优化',
    correction: '纠错反馈已提交，感谢支持',
    suggestion: '补充建议已提交，感谢支持',
  }
  ElMessage.success(map[type])
}
</script>

<template>
  <div class="detail">
    <button type="button" class="back-link" @click="router.back()">← 返回</button>

    <div v-if="loading" class="detail-loading">
      <el-skeleton :rows="8" animated />
    </div>

    <div v-else-if="error" class="empty">
      <div class="empty-icon">⚠️</div>
      <p>{{ error }}</p>
      <button type="button" class="btn-secondary" @click="load">重试</button>
    </div>

    <template v-else-if="item">
      <div class="detail-head">
        <div class="meta-line">
          <span v-if="item.categoryPath" class="category">📁 {{ item.categoryPath }}</span>
          <span v-if="item.knowledgeType" class="type-badge">{{ item.knowledgeType }}</span>
        </div>

        <h1>{{ item.title }}</h1>

        <div v-if="item.summary" class="summary">{{ item.summary }}</div>

        <div class="meta-row">
          <span class="meta-item">版本 v{{ item.versionNo ?? 1 }}</span>
          <span class="meta-item">更新于 {{ formatDate(item.updatedAt) }}</span>
          <span class="meta-item">阅读 {{ formatCount(item.viewCount) }}</span>
        </div>

        <div v-if="item.tags?.length" class="tag-row">
          <TagChip v-for="tag in item.tags" :key="tag.id" :name="tag.name" />
        </div>
      </div>

      <div class="detail-body">
        <ArticleContent :html="item.htmlContent" :plain-text="item.plainText" />
      </div>

      <div class="detail-foot">
        <FeedbackActions
          :like-count="item.likeCount"
          :view-count="item.viewCount"
          @feedback="handleFeedback"
        />
      </div>
    </template>
  </div>
</template>

<style scoped>
.detail {
  max-width: 860px;
  margin: 0 auto;
}

.back-link {
  border: none;
  background: none;
  color: var(--text-tertiary);
  font-size: 0.8125rem;
  cursor: pointer;
  font-family: var(--font-body);
  padding: 0 0 12px;
}

.back-link:hover {
  color: var(--primary);
}

.detail-head {
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-light);
}

.meta-line {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}

.category {
  font-size: 0.75rem;
  color: var(--text-tertiary);
}

.type-badge {
  font-size: 0.6875rem;
  padding: 2px 10px;
  border-radius: 99px;
  background: var(--accent-subtle, var(--surface-2));
  color: var(--accent, var(--text-secondary));
  font-weight: 600;
}

.detail-head h1 {
  font-family: var(--font-heading);
  font-size: 1.625rem;
  letter-spacing: -0.02em;
  line-height: 1.4;
  margin-bottom: 12px;
}

.summary {
  color: var(--text-secondary);
  font-size: 0.9375rem;
  line-height: 1.7;
  background: var(--surface-1);
  border-left: 3px solid var(--primary);
  padding: 12px 16px;
  border-radius: 0 var(--radius-sm) var(--radius-sm) 0;
  margin-bottom: 16px;
}

.meta-row {
  display: flex;
  gap: 18px;
  flex-wrap: wrap;
  font-size: 0.75rem;
  color: var(--text-tertiary);
  margin-bottom: 16px;
}

.tag-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.detail-body {
  padding: 24px 0;
}

.detail-foot {
  padding-top: 20px;
  border-top: 1px solid var(--border-light);
}

.detail-loading {
  padding: 24px 0;
}

.empty {
  text-align: center;
  padding: 64px 0;
  color: var(--text-secondary);
}

.empty-icon {
  font-size: 2.5rem;
  margin-bottom: 12px;
}
</style>
