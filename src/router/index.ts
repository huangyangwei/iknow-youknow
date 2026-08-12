import { createRouter, createWebHistory } from 'vue-router'
import { useSessionStore } from '../stores/session'
import AppShell from '../layouts/AppShell.vue'
import AuthLayout from '../layouts/AuthLayout.vue'

const routes = [
  { path: '/login', component: AuthLayout, children: [{ path: '', name: 'login', component: () => import('../views/LoginView.vue') }] },
  { path: '/', component: AppShell, children: [
    { path: '', name: 'home', component: () => import('../views/SearchHomeView.vue') },
    { path: 'search', name: 'search', component: () => import('../views/SearchResultsView.vue') },
    { path: 'chat', name: 'chat', component: () => import('../views/ChatView.vue') },
    { path: 'knowledge/:id', name: 'knowledge-detail', component: () => import('../views/KnowledgeDetailView.vue') },
    { path: 'manage/knowledge', name: 'knowledge', component: () => import('../views/KnowledgeManageView.vue'), meta: { permission: 'knowledge:write' } },
    { path: 'manage/knowledge/:id?', name: 'knowledge-editor', component: () => import('../views/KnowledgeEditorView.vue'), meta: { permission: 'knowledge:write' } },
    { path: 'manage/feedback', name: 'feedback', component: () => import('../views/FeedbackView.vue'), meta: { permission: 'feedback:read' } },
    { path: 'analytics', name: 'analytics', component: () => import('../views/AnalyticsView.vue'), meta: { permission: 'analytics:read' } },
    { path: 'admin/users', name: 'users', component: () => import('../views/UserManageView.vue'), meta: { permission: 'users:write' } }
  ] },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]
export const router = createRouter({ history: createWebHistory(), routes })
router.beforeEach(to => { const session = useSessionStore(); if (to.name !== 'login' && !session.isAuthenticated) return { name: 'login', query: { redirect: to.fullPath } }; const permission = to.meta.permission as string | undefined; if (permission && !session.hasPermission(permission)) return { name: 'home' } })
