<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { MODELS } from '@/stores/chat'

defineOptions({ name: 'SearchHero' })

const props = defineProps<{
  mode: 'search' | 'qa'
  modelKey?: string
  hotQuestions?: string[]
}>()

const emit = defineEmits<{
  search: [keyword: string]
  ask: [question: string]
  'update:modelKey': [key: string]
}>()

const activeModel = computed(() => MODELS.find((m) => m.key === (props.modelKey ?? 'claude')) ?? MODELS[0])

const keyword = ref('')
const question = ref('')

watch(
  () => props.mode,
  () => {
    keyword.value = ''
    question.value = ''
  },
)

function submitSearch() {
  const kw = keyword.value.trim()
  if (kw) emit('search', kw)
}

function submitAsk() {
  const q = question.value.trim()
  if (q) emit('ask', q)
}

const hot = ref(props.hotQuestions?.length ? props.hotQuestions : [
  '支付回调失败怎么排查？',
  '用户账号如何注销？',
  'API 接口认证方式',
  '订单退款流程说明',
  '数据导出格式说明',
  '如何配置企业微信集成？',
])
</script>

<template>
  <div class="search-hero">
    <h1>搜知识，问答案</h1>
    <p>快速查找产品文档，或直接提问获取精准答案</p>

    <div class="search-box-wrapper">
      <div v-if="mode === 'search'" class="search-box">
        <input
          v-model="keyword"
          type="text"
          placeholder="搜索产品知识… 例如：支付回调配置、账号注销流程"
          @keydown.enter="submitSearch"
        />
        <button class="btn-search" title="搜索" @click="submitSearch">→</button>
      </div>

      <div v-else class="search-box">
        <span class="model-badge">
          <span class="mini-dot" :class="activeModel?.dot"></span>
          {{ activeModel?.name }}
        </span>
        <input
          v-model="question"
          type="text"
          placeholder="描述客户遇到的问题… 例如：客户支付成功但订单状态未更新"
          @keydown.enter="submitAsk"
        />
        <button class="btn-search" title="提问" @click="submitAsk">→</button>
      </div>

      <div v-if="mode === 'qa'" class="model-chips">
        <button
          v-for="model in MODELS"
          :key="model.key"
          type="button"
          class="model-chip"
          :class="{ active: model.key === props.modelKey }"
          @click="emit('update:modelKey', model.key)"
        >
          {{ model.name }}
        </button>
      </div>
    </div>

    <div class="hot-questions">
      <h3>常见问题</h3>
      <div class="hot-tags">
        <button
          v-for="q in hot"
          :key="q"
          type="button"
          class="hot-tag"
          @click="mode === 'search' ? (keyword = q, submitSearch()) : (question = q, submitAsk())"
        >
          {{ q }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.search-hero {
  text-align: center;
  padding: 48px 0 32px;
}

.search-hero h1 {
  font-size: clamp(1.75rem, 3vw, 2.5rem);
  margin-bottom: 8px;
}

.search-hero > p {
  color: var(--text-secondary);
  font-size: 1.0625rem;
  margin-bottom: 28px;
}

.search-box-wrapper {
  max-width: 680px;
  margin: 0 auto;
}

.search-box {
  display: flex;
  align-items: center;
  background: #fff;
  border: 1.5px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 12px 16px;
  box-shadow: var(--shadow-sm);
  transition: all var(--transition-base);
}

.search-box:focus-within {
  border-color: var(--primary);
  box-shadow: var(--shadow-md), 0 0 0 4px var(--primary-light);
}

.search-box input {
  flex: 1;
  border: none;
  outline: none;
  font-size: 1rem;
  font-family: var(--font-body);
  color: var(--text-primary);
  background: transparent;
}

.search-box input::placeholder {
  color: var(--text-placeholder);
}

.search-box .btn-search {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  background: var(--primary);
  border: none;
  color: #fff;
  cursor: pointer;
  font-size: 1.1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background var(--transition-fast);
  flex-shrink: 0;
}

.search-box .btn-search:hover {
  background: var(--primary-hover);
}

.model-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px;
  border-radius: 99px;
  font-size: 0.6875rem;
  font-weight: 600;
  background: var(--surface-2);
  color: var(--text-tertiary);
  margin-right: 10px;
  flex-shrink: 0;
}

.mini-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.model-chips {
  display: flex;
  gap: 6px;
  margin-top: 10px;
  justify-content: center;
  flex-wrap: wrap;
}

.model-chip {
  padding: 4px 12px;
  border-radius: 99px;
  border: 1px solid var(--border);
  background: #fff;
  color: var(--text-secondary);
  font-size: 0.75rem;
  cursor: pointer;
  font-family: var(--font-body);
  transition: all var(--transition-fast);
}

.model-chip.active {
  border-color: var(--primary);
  background: var(--primary-subtle);
  color: var(--primary);
  font-weight: 600;
}

.hot-questions {
  margin-top: 32px;
  max-width: 680px;
  margin-left: auto;
  margin-right: auto;
}

.hot-questions h3 {
  font-size: 0.8125rem;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-tertiary);
  margin-bottom: 12px;
}

.hot-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  justify-content: center;
}

.hot-tag {
  padding: 6px 16px;
  background: var(--surface-2);
  border-radius: 99px;
  font-size: 0.8125rem;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  border: none;
  font-family: var(--font-body);
}

.hot-tag:hover {
  background: var(--primary-subtle);
  color: var(--primary);
}
</style>
