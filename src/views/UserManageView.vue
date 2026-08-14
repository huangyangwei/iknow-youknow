<script setup lang="ts">
import { MOCK_USERS } from '@/mock/data'
import { ROLE_LABELS } from '@/utils/roles'
import type { UserInfo } from '@/types/api'

defineOptions({ name: 'UserManageView' })

interface Row extends UserInfo {
  password?: string
}

const users: Row[] = MOCK_USERS.map(({ password: _pw, ...info }) => info)

const roleText = (roles: string[]) => roles.map((r) => ROLE_LABELS[r] ?? r).join('、') || '—'
</script>

<template>
  <div class="users">
    <header class="page-head">
      <h1>用户管理</h1>
      <p class="muted">管理账号、角色与访问权限</p>
    </header>

    <div class="table-wrap">
      <el-table :data="users">
        <el-table-column label="用户" min-width="160">
          <template #default="{ row }">
            <div class="user-cell">
              <span class="avatar">{{ row.nickname?.slice(0, 1) || row.username?.slice(0, 1) || '?' }}</span>
              <div>
                <div class="name">{{ row.nickname || row.username }}</div>
                <div class="sub">{{ row.email }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="登录名" width="140" />
        <el-table-column label="角色" width="160">
          <template #default="{ row }">
            <el-tag v-for="r in row.roles" :key="r" size="small" class="role-tag">
              {{ ROLE_LABELS[r] ?? r }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default>
            <span class="active">启用</span>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <p class="hint">用户管理接口（邀请、禁用、角色变更）将在后续迭代接入。</p>
  </div>
</template>

<style scoped>
.users {
  max-width: 1000px;
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

.table-wrap {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  background: var(--primary-light);
  color: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.8125rem;
  flex-shrink: 0;
}

.name {
  font-weight: 500;
  color: var(--text-primary);
}

.sub {
  font-size: 0.75rem;
  color: var(--text-tertiary);
}

.role-tag {
  margin-right: 4px;
}

.active {
  color: var(--success, #15803d);
  font-size: 0.8125rem;
}

.hint {
  margin-top: 16px;
  font-size: 0.75rem;
  color: var(--text-tertiary);
}
</style>
