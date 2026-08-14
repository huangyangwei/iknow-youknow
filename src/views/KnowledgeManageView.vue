<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useKnowledgeStore } from '@/stores/knowledge'
import { knowledgeApi } from '@/api/knowledge'
import { ApiError } from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import Toolbar from '@/components/knowledge/Toolbar.vue'
import KnowledgeTable from '@/components/knowledge/KnowledgeTable.vue'
import AppPagination from '@/components/search/Pagination.vue'
import type { KnowledgeItem } from '@/types/api'

defineOptions({ name: 'KnowledgeManageView' })

const router = useRouter()
const knowledge = useKnowledgeStore()
const auth = useAuthStore()

const selectedKeys = ref<number[]>([])

const canCreate = auth.hasPermission('knowledge:create') || auth.hasPermission('knowledge:manage')
const canEdit = auth.hasPermission('knowledge:update') || auth.hasPermission('knowledge:manage')
const canDelete = auth.hasPermission('knowledge:delete')

onMounted(async () => {
  await knowledge.loadOptions().catch(() => {})
  await knowledge.fetchList()
})

function onSearch() {
  knowledge.query.page = 1
  void knowledge.fetchList()
}

function onCreate() {
  void router.push({ name: 'knowledge-editor' })
}

function onView(item: KnowledgeItem) {
  void router.push({ name: 'knowledge-detail', params: { id: item.id } })
}

function onEdit(item: KnowledgeItem) {
  void router.push({ name: 'knowledge-editor', params: { id: item.id } })
}

async function onPublish(item: KnowledgeItem) {
  try {
    await ElMessageBox.confirm(`确认发布「${item.title}」？`, '发布确认', {
      type: 'warning',
      confirmButtonText: '发布',
      cancelButtonText: '取消',
    })
    await knowledgeApi.publish(item.id)
    ElMessage.success('已发布')
    await knowledge.fetchList()
  } catch {
    // 用户取消
  }
}

async function onArchive(item: KnowledgeItem) {
  try {
    await ElMessageBox.confirm(`归档后不再对用户展示「${item.title}」，确认归档？`, '归档确认', {
      type: 'warning',
      confirmButtonText: '归档',
      cancelButtonText: '取消',
    })
    await knowledgeApi.update(item.id, { status: 'archived' })
    ElMessage.success('已归档')
    await knowledge.fetchList()
  } catch {
    // 用户取消
  }
}

async function onDelete(item: KnowledgeItem) {
  try {
    await ElMessageBox.confirm(`删除后不可恢复，确认删除「${item.title}」？`, '删除确认', {
      type: 'error',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
    await knowledgeApi.remove(item.id)
    ElMessage.success('已删除')
    await knowledge.fetchList()
  } catch {
    // 用户取消
  }
}
</script>

<template>
  <div class="mgmt">
    <header class="page-head">
      <div>
        <h1>知识管理</h1>
        <p class="muted">维护企业知识库的文档、分类与发布状态</p>
      </div>
    </header>

    <Toolbar
      :keyword="knowledge.query.keyword"
      :category-id="knowledge.query.categoryId"
      :status="knowledge.query.status"
      :categories="knowledge.categories"
      :can-create="canCreate"
      @update:keyword="knowledge.query.keyword = $event"
      @update:category-id="knowledge.query.categoryId = $event"
      @update:status="knowledge.query.status = $event"
      @search="onSearch"
      @create="onCreate"
    />

    <KnowledgeTable
      :items="knowledge.items"
      :loading="knowledge.loading"
      :can-edit="canEdit"
      :can-delete="canDelete"
      v-model:selected-keys="selectedKeys"
      @view="onView"
      @edit="onEdit"
      @publish="onPublish"
      @archive="onArchive"
      @delete="onDelete"
    />

    <AppPagination
      :page="knowledge.query.page"
      :size="knowledge.query.size"
      :total="knowledge.total"
      @change="(p) => { knowledge.query.page = p; knowledge.fetchList() }"
    />
  </div>
</template>

<style scoped>
.mgmt {
  max-width: 1200px;
  margin: 0 auto;
}

.page-head {
  margin-bottom: 20px;
}

.page-head h1 {
  font-family: var(--font-heading);
  font-size: 1.375rem;
  margin-bottom: 4px;
}

.muted {
  color: var(--text-secondary);
  font-size: 0.875rem;
}
</style>
