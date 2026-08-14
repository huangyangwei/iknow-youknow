<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

interface NavItem {
  label: string
  to: string
  icon: string
  permission?: string
}

interface NavSection {
  title: string
  items: NavItem[]
}

const auth = useAuthStore()
const route = useRoute()
const router = useRouter()

const querySection: NavSection = {
  title: '查询',
  items: [
    { label: '首页', to: '/home', icon: '🏠' },
    { label: '智能问答', to: '/chat', icon: '💬' },
  ],
}

const adminSection = computed<NavSection>(() => ({
  title: '管理',
  items: [
    { label: '知识管理', to: '/knowledge-mgmt', icon: '📚', permission: 'knowledge:manage' },
    { label: '反馈管理', to: '/feedback-mgmt', icon: '📋', permission: 'feedback:manage' },
    { label: '数据仪表盘', to: '/dashboard', icon: '📊', permission: 'analytics:read' },
  ].filter((item) => !item.permission || auth.hasPermission(item.permission!)),
}))

const systemSection = computed<NavSection>(() => ({
  title: '系统',
  items: [
    { label: '用户管理', to: '/user-mgmt', icon: '👥', permission: 'user:manage' },
  ].filter((item) => !item.permission || auth.hasPermission(item.permission!)),
}))

const sections = computed(() => {
  const list: NavSection[] = [querySection]
  if (adminSection.value.items.length) list.push(adminSection.value)
  if (systemSection.value.items.length) list.push(systemSection.value)
  return list
})

const isActive = (to: string) => {
  if (to === '/home') return route.path === '/home' || route.path === '/'
  return route.path.startsWith(to)
}

function logout() {
  auth.signOut()
  void router.push('/login')
}
</script>

<template>
  <div class="shell">
    <aside class="sidebar">
      <div class="sidebar-logo">
        <div class="icon">K</div>
        <span>KnowledgeHub</span>
      </div>

      <nav class="sidebar-nav">
        <div v-for="section in sections" :key="section.title" class="nav-section">
          <div class="nav-section-title">{{ section.title }}</div>
          <RouterLink
            v-for="item in section.items"
            :key="item.to"
            :to="item.to"
            class="nav-item"
            :class="{ active: isActive(item.to) }"
          >
            <span>{{ item.icon }}</span>
            {{ item.label }}
          </RouterLink>
        </div>
      </nav>

      <div class="sidebar-footer">
        <div class="user-chip" @click="router.push('/home')">
          <div class="user-avatar">{{ auth.avatarText }}</div>
          <div class="user-info">
            <div class="user-name">{{ auth.displayName }}</div>
            <div class="user-role">{{ auth.roleText }}</div>
          </div>
        </div>
        <button class="logout-btn" title="退出登录" @click="logout">退出登录</button>
      </div>
    </aside>

    <main class="main">
      <RouterView v-slot="{ Component }">
        <Suspense :timeout="0">
          <component :is="Component" />
          <template #fallback>
            <div class="page-loading">
              <el-skeleton :rows="8" animated />
            </div>
          </template>
        </Suspense>
      </RouterView>
    </main>
  </div>
</template>

<style scoped>
.shell {
  min-height: 100vh;
  display: flex;
}

.sidebar {
  width: var(--sidebar-width);
  background: var(--surface-1);
  border-right: 1px solid var(--border);
  display: flex;
  flex-direction: column;
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  z-index: 100;
  padding: 24px 16px;
}

.sidebar-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 32px;
  padding: 0 8px;
}

.sidebar-logo .icon {
  width: 32px;
  height: 32px;
  background: var(--primary);
  border-radius: var(--radius-sm);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  font-family: var(--font-heading);
}

.sidebar-logo span {
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 1.1rem;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}

.sidebar-nav {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  overflow-y: auto;
}

.nav-section {
  margin-bottom: 16px;
}

.nav-section-title {
  font-size: 0.675rem;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--text-tertiary);
  padding: 0 8px;
  margin-bottom: 6px;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: var(--radius-sm);
  font-size: 0.875rem;
  font-weight: 500;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all var(--transition-fast);
  text-decoration: none;
  width: 100%;
}

.nav-item:hover {
  background: var(--surface-2);
  color: var(--text-primary);
}

.nav-item.active {
  background: var(--primary-subtle);
  color: var(--primary);
  font-weight: 600;
}

.sidebar-footer {
  border-top: 1px solid var(--border-light);
  padding-top: 16px;
}

.user-chip {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 8px;
  border-radius: var(--radius-md);
  cursor: pointer;
  transition: background var(--transition-fast);
}

.user-chip:hover {
  background: var(--surface-2);
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--primary-light);
  color: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-heading);
  font-weight: 700;
  font-size: 0.8rem;
  flex-shrink: 0;
}

.user-info {
  min-width: 0;
}

.user-name {
  font-size: 0.8125rem;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  font-size: 0.6875rem;
  color: var(--text-tertiary);
}

.logout-btn {
  margin-top: 8px;
  width: 100%;
  padding: 8px 12px;
  border: none;
  border-radius: var(--radius-sm);
  background: none;
  color: var(--text-tertiary);
  font-size: 0.8125rem;
  cursor: pointer;
  font-family: var(--font-body);
  transition: all var(--transition-fast);
  text-align: left;
}

.logout-btn:hover {
  background: var(--danger-light);
  color: var(--danger);
}

.main {
  flex: 1;
  margin-left: var(--sidebar-width);
  padding: 32px 40px 40px;
  max-width: 1280px;
  width: calc(100% - var(--sidebar-width));
}

.page-loading {
  padding: 24px;
}

@media (max-width: 768px) {
  .sidebar {
    display: none;
  }
  .main {
    margin-left: 0;
    width: 100%;
    padding: 18px;
  }
}
</style>
