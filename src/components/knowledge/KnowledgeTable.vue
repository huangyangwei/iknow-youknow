<script setup lang="ts">
import StatusBadge from './StatusBadge.vue'
import { formatCount, formatDate } from '@/utils/format'
import type { KnowledgeItem } from '@/types/api'

defineOptions({ name: 'KnowledgeTable' })

const props = defineProps<{
  items: KnowledgeItem[]
  loading: boolean
  canEdit: boolean
  canDelete: boolean
  selectedKeys: number[]
}>()

const emit = defineEmits<{
  view: [item: KnowledgeItem]
  edit: [item: KnowledgeItem]
  publish: [item: KnowledgeItem]
  archive: [item: KnowledgeItem]
  delete: [item: KnowledgeItem]
  'update:selectedKeys': [keys: number[]]
}>()

function onSelectionChange(rows: KnowledgeItem[]) {
  emit('update:selectedKeys', rows.map((r) => r.id))
}
</script>

<template>
  <div class="table-wrap">
    <el-table
      :data="items"
      v-loading="loading"
      :row-key="(r: KnowledgeItem) => r.id"
      @selection-change="onSelectionChange"
    >
      <el-table-column type="selection" width="44" :reserve-selection="true" />
      <el-table-column label="标题" min-width="260">
        <template #default="{ row }">
          <div class="title-cell">
            <div class="title">{{ row.title }}</div>
            <div v-if="row.summary" class="summary">{{ row.summary }}</div>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="分类" min-width="140" prop="categoryPath">
        <template #default="{ row }">
          <span class="cell-muted">{{ row.categoryPath || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="类型" width="100" prop="knowledgeType">
        <template #default="{ row }">
          <span v-if="row.knowledgeType" class="type-tag">{{ row.knowledgeType }}</span>
          <span v-else class="cell-muted">—</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <StatusBadge :status="row.status" />
        </template>
      </el-table-column>
      <el-table-column label="版本" width="70" prop="versionNo">
        <template #default="{ row }">v{{ row.versionNo ?? 1 }}</template>
      </el-table-column>
      <el-table-column label="阅读" width="80" align="right" prop="viewCount">
        <template #default="{ row }">{{ formatCount(row.viewCount) }}</template>
      </el-table-column>
      <el-table-column label="更新时间" width="110" prop="updatedAt">
        <template #default="{ row }">
          <span class="cell-muted">{{ formatDate(row.updatedAt) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="190" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" size="small" @click="emit('view', row)">查看</el-button>
          <el-button v-if="canEdit" text size="small" @click="emit('edit', row)">编辑</el-button>
          <el-dropdown v-if="canEdit" trigger="click" class="more">
            <el-button text size="small">⋯</el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item v-if="row.status !== 'published'" @click="emit('publish', row)">
                  发布
                </el-dropdown-item>
                <el-dropdown-item v-if="row.status !== 'archived'" @click="emit('archive', row)">
                  归档
                </el-dropdown-item>
                <el-dropdown-item divided :disabled="!canDelete" @click="emit('delete', row)">
                  删除
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>
  </div>
</template>

<style scoped>
.table-wrap {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.title-cell .title {
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 2px;
}

.title-cell .summary {
  font-size: 0.75rem;
  color: var(--text-tertiary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 320px;
}

.cell-muted {
  color: var(--text-tertiary);
}

.type-tag {
  display: inline-block;
  font-size: 0.6875rem;
  padding: 2px 8px;
  border-radius: 99px;
  background: var(--surface-2);
  color: var(--text-secondary);
}
</style>
