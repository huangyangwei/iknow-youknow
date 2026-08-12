<script setup lang="ts">
import { computed } from 'vue'; import { RouterLink, RouterView, useRouter } from 'vue-router'; import { useSessionStore } from '../stores/session'
const session = useSessionStore(); const router = useRouter()
const navigation = computed(() => [{ label:'问答', to:'/' },{ label:'搜索',to:'/search' },{ label:'知识管理',to:'/manage/knowledge',permission:'knowledge:write' },{ label:'反馈管理',to:'/manage/feedback',permission:'feedback:read' },{ label:'分析',to:'/analytics',permission:'analytics:read' },{ label:'用户管理',to:'/admin/users',permission:'users:write' }].filter(item => !item.permission || session.hasPermission(item.permission)))
function logout(){session.signOut();router.push('/login')}
;</script>
<template><div class="shell"><aside class="sidebar"><div class="brand">iKnow · youKnow</div><el-menu default-active="/" background-color="#101828" text-color="#d0d5dd" active-text-color="#fff" router><el-menu-item v-for="item in navigation" :key="item.to" :index="item.to">{{ item.label }}</el-menu-item></el-menu><el-button class="logout" text @click="logout">退出登录</el-button></aside><main class="content"><Suspense><RouterView /><template #fallback><el-skeleton :rows="8" animated /></template></Suspense></main></div></template>
<style scoped>.logout{position:absolute;bottom:20px;color:#fff}</style>
