import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import AppShell from '@/layouts/AppShell.vue'
import AuthLayout from '@/layouts/AuthLayout.vue'

declare module 'vue-router' {
  interface RouteMeta {
    title?: string
    /** 需要的权限点；未配置则仅需登录 */
    permission?: string
  }
}

const appRoutes: RouteRecordRaw[] = [
  {
    path: '/',
    component: AppShell,
    children: [
      { path: '', redirect: { name: 'home' } },
      { path: 'home', name: 'home', component: () => import('@/views/HomeView.vue'), meta: { title: '首页' } },
      { path: 'search', name: 'search', component: () => import('@/views/SearchResultsView.vue'), meta: { title: '搜索结果' } },
      { path: 'chat/:sessionId?', name: 'chat', component: () => import('@/views/ChatView.vue'), meta: { title: '智能问答' } },
      { path: 'knowledge/:id', name: 'knowledge-detail', component: () => import('@/views/KnowledgeDetailView.vue'), meta: { title: '知识详情' } },
      {
        path: 'knowledge-mgmt',
        name: 'knowledge-mgmt',
        component: () => import('@/views/KnowledgeManageView.vue'),
        meta: { title: '知识管理', permission: 'knowledge:manage' },
      },
      {
        path: 'knowledge-editor/:id?',
        name: 'knowledge-editor',
        component: () => import('@/views/KnowledgeEditorView.vue'),
        meta: { title: '知识编辑器', permission: 'knowledge:manage' },
      },
      {
        path: 'feedback-mgmt',
        name: 'feedback-mgmt',
        component: () => import('@/views/FeedbackView.vue'),
        meta: { title: '反馈管理', permission: 'feedback:manage' },
      },
      {
        path: 'dashboard',
        name: 'dashboard',
        component: () => import('@/views/DashboardView.vue'),
        meta: { title: '数据仪表盘', permission: 'analytics:read' },
      },
      {
        path: 'user-mgmt',
        name: 'user-mgmt',
        component: () => import('@/views/UserManageView.vue'),
        meta: { title: '用户管理', permission: 'user:manage' },
      },
    ],
  },
  {
    path: '/login',
    component: AuthLayout,
    children: [{ path: '', name: 'login', component: () => import('@/views/LoginView.vue'), meta: { title: '登录' } }],
  },
  { path: '/:pathMatch(.*)*', redirect: '/home' },
]

export const router = createRouter({
  history: createWebHistory(),
  routes: appRoutes,
  scrollBehavior: () => ({ top: 0 }),
})

router.beforeEach((to) => {
  const auth = useAuthStore()

  if (to.name === 'login') {
    if (auth.isAuthenticated) return { name: 'home' }
    return true
  }

  if (!auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }

  const permission = to.meta.permission
  if (permission && !auth.hasPermission(permission)) {
    return { name: 'home' }
  }

  return true
})

router.afterEach((to) => {
  const base = 'KnowledgeHub'
  document.title = to.meta.title ? `${to.meta.title} · ${base}` : base
})
