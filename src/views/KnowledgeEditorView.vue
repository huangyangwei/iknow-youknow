<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { knowledgeApi, type KnowledgeSavePayload } from '@/api/knowledge'
import { ApiError } from '@/api/http'
import { useKnowledgeStore } from '@/stores/knowledge'
import RichTextEditor from '@/components/editor/RichTextEditor.vue'
import TagInput from '@/components/editor/TagInput.vue'
import VersionHistory from '@/components/editor/VersionHistory.vue'
import PublishPanel from '@/components/editor/PublishPanel.vue'
import type { Category, KnowledgeStatus, KnowledgeVersion } from '@/types/api'

defineOptions({ name: 'KnowledgeEditorView' })

const route = useRoute()
const router = useRouter()
const knowledge = useKnowledgeStore()

const isEdit = computed(() => Boolean(route.params.id))
const loading = ref(false)
const saving = ref(false)
const error = ref('')

const form = ref({
  title: '',
  summary: '',
  categoryId: null as number | null,
  knowledgeType: '',
  tags: [] as string[],
  htmlContent: '',
  plainText: '',
  status: 'draft' as KnowledgeStatus,
  versionNo: 1,
})

const versions = ref<KnowledgeVersion[]>([])
const showHistory = ref(false)

const KNOWLEDGE_TYPES = ['操作指南', '故障排查', '接口文档', '政策说明', 'FAQ'] as const

const flatCategories = computed(() => {
  const list: Category[] = []
  const walk = (items: Category[]) => {
    for (const c of items) {
      list.push(c)
      if (c.children?.length) walk(c.children)
    }
  }
  walk(knowledge.categories)
  return list
})

const tagSuggestions = computed(() => knowledge.tags.map((t) => t.name))

async function load() {
  const id = Number(route.params.id)
  if (!id) return
  loading.value = true
  error.value = ''
  try {
    const detail = await knowledgeApi.detail(id)
    form.value = {
      title: detail.title,
      summary: detail.summary ?? '',
      categoryId: detail.categoryId ?? null,
      knowledgeType: detail.knowledgeType ?? '',
      tags: detail.tags.map((t) => t.name),
      htmlContent: detail.htmlContent ?? '',
      plainText: detail.plainText ?? '',
      status: detail.status,
      versionNo: detail.versionNo ?? 1,
    }
    versions.value = await knowledgeApi.versions(id).catch(() => [])
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await knowledge.loadOptions().catch(() => {})
  await load()
})

function buildPayload(status: KnowledgeStatus): KnowledgeSavePayload {
  return {
    title: form.value.title,
    summary: form.value.summary,
    categoryId: form.value.categoryId ?? undefined,
    knowledgeType: form.value.knowledgeType || undefined,
    tags: form.value.tags,
    htmlContent: form.value.htmlContent,
    plainText: form.value.htmlContent.replace(/<[^>]+>/g, ' ').replace(/\s+/g, ' ').trim(),
    status,
  }
}

async function save(status: KnowledgeStatus) {
  if (!form.value.title.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  saving.value = true
  try {
    const id = Number(route.params.id)
    if (id) {
      await knowledgeApi.update(id, buildPayload(status))
      ElMessage.success('已保存')
    } else {
      const created = await knowledgeApi.create(buildPayload(status))
      void router.replace({ name: 'knowledge-editor', params: { id: created.id } })
      ElMessage.success('已创建')
    }
  } catch (e) {
    ElMessage.error(e instanceof ApiError ? e.message : '保存失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

async function publish() {
  if (!form.value.title.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  saving.value = true
  try {
    const id = Number(route.params.id)
    const targetId = id || (await knowledgeApi.create(buildPayload('draft'))).id
    await knowledgeApi.publish(targetId)
    if (!id) void router.replace({ name: 'knowledge-editor', params: { id: targetId } })
    ElMessage.success('发布成功')
  } catch (e) {
    ElMessage.error(e instanceof ApiError ? e.message : '发布失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

async function publishScheduled(scheduledAt: string) {
  if (!form.value.title.trim()) {
    ElMessage.warning('请填写标题')
    return
  }
  saving.value = true
  try {
    const id = Number(route.params.id)
    const targetId = id || (await knowledgeApi.create(buildPayload('draft'))).id
    await knowledgeApi.publish(targetId, scheduledAt)
    if (!id) void router.replace({ name: 'knowledge-editor', params: { id: targetId } })
    ElMessage.success('已安排定时发布')
  } catch (e) {
    ElMessage.error(e instanceof ApiError ? e.message : '操作失败，请稍后重试')
  } finally {
    saving.value = false
  }
}

async function rollback(versionNo: number) {
  const id = Number(route.params.id)
  if (!id) return
  try {
    await knowledgeApi.rollback(id, versionNo)
    ElMessage.success('已回滚')
    await load()
  } catch (e) {
    ElMessage.error(e instanceof ApiError ? e.message : '回滚失败')
  }
}

function back() {
  void router.push({ name: 'knowledge-mgmt' })
}
</script>

<template>
  <div class="editor-page">
    <button type="button" class="back-link" @click="back">← 返回知识管理</button>

    <div v-if="loading" class="editor-loading">
      <el-skeleton :rows="10" animated />
    </div>

    <div v-else-if="error" class="empty">
      <div class="empty-icon">⚠️</div>
      <p>{{ error }}</p>
      <button type="button" class="btn-secondary" @click="load">重试</button>
    </div>

    <div v-else class="editor-layout">
      <div class="editor-main">
        <div class="card field-group">
          <label class="field">
            <span>标题 <em>*</em></span>
            <input v-model="form.title" type="text" placeholder="请输入知识标题" />
          </label>

          <label class="field">
            <span>摘要</span>
            <textarea v-model="form.summary" rows="2" placeholder="一句话描述本文内容，用于搜索摘要展示"></textarea>
          </label>

          <div class="field-row">
            <div class="field">
              <span>分类</span>
              <el-select v-model="form.categoryId" placeholder="选择分类" clearable style="width: 100%">
                <el-option
                  v-for="c in flatCategories"
                  :key="c.id"
                  :label="c.name"
                  :value="c.id"
                />
              </el-select>
            </div>
            <div class="field">
              <span>知识类型</span>
              <el-select v-model="form.knowledgeType" placeholder="选择类型" clearable style="width: 100%">
                <el-option v-for="t in KNOWLEDGE_TYPES" :key="t" :label="t" :value="t" />
              </el-select>
            </div>
          </div>

          <div class="field">
            <span>标签</span>
            <TagInput v-model="form.tags" :suggestions="tagSuggestions" />
          </div>
        </div>

        <div class="card field-group">
          <div class="field">
            <span>正文 <em>*</em></span>
            <RichTextEditor v-model="form.htmlContent" />
          </div>
        </div>
      </div>

      <aside class="editor-side">
        <PublishPanel
          :status="form.status"
          :saving="saving"
          @save-draft="save('draft')"
          @publish="publish"
          @publish-scheduled="publishScheduled"
        />

        <div v-if="isEdit" class="card history-card">
          <button type="button" class="history-toggle" @click="showHistory = !showHistory">
            版本历史（{{ versions.length }}）
            <span>{{ showHistory ? '▲' : '▼' }}</span>
          </button>
          <div v-if="showHistory" class="history-body">
            <VersionHistory :versions="versions" :current-version-no="form.versionNo" @rollback="rollback" />
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.editor-page {
  max-width: 1180px;
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

.editor-layout {
  display: grid;
  grid-template-columns: 1fr 280px;
  gap: 24px;
  align-items: start;
}

.editor-main {
  display: flex;
  flex-direction: column;
  gap: 16px;
  min-width: 0;
}

.card {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  padding: 20px;
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field span {
  display: block;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.field em {
  color: var(--danger);
  font-style: normal;
}

.field input,
.field textarea {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  font-size: 0.9375rem;
  font-family: var(--font-body);
  color: var(--text-primary);
  background: #fff;
  transition: all var(--transition-fast);
  box-sizing: border-box;
  resize: vertical;
}

.field input:focus,
.field textarea:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 4px var(--primary-light);
}

.field-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.editor-side {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.history-toggle {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  border: none;
  background: none;
  font-family: var(--font-body);
  font-size: 0.875rem;
  font-weight: 600;
  color: var(--text-primary);
  cursor: pointer;
  padding: 0;
}

.history-body {
  margin-top: 12px;
}

.editor-loading {
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

@media (max-width: 900px) {
  .editor-layout {
    grid-template-columns: 1fr;
  }
  .editor-side {
    order: 2;
  }
}
</style>
