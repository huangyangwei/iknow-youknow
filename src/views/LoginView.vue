<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ApiError } from '@/api/http'

defineOptions({ name: 'LoginView' })

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const email = ref('admin@iknow.com')
const password = ref('123456')
const isLoading = ref(false)
const error = ref('')

async function submit() {
  if (!email.value.trim() || !password.value) {
    error.value = '请输入账号和密码'
    return
  }
  isLoading.value = true
  error.value = ''
  try {
    await auth.login({ email: email.value.trim(), password: password.value })
    const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/home'
    await router.push(redirect)
  } catch (e) {
    error.value = e instanceof ApiError ? e.message : '登录失败，请稍后重试'
  } finally {
    isLoading.value = false
  }
}

const demoAccounts = [
  { email: 'admin@iknow.com', role: '后端研发（全权限）' },
  { email: 'editor@iknow.com', role: '知识管理员（知识管理）' },
  { email: 'member@iknow.com', role: '一线运营（仅查询）' },
]

function fill(acc: string) {
  email.value = acc
  password.value = '123456'
}
</script>

<template>
  <div class="login">
    <h1>欢迎回来</h1>
    <p class="subtitle">登录知识库问答系统</p>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" class="login-alert" />

    <form @submit.prevent="submit">
      <label class="field">
        <span>邮箱 / 账号</span>
        <input v-model="email" type="email" autocomplete="username" placeholder="name@company.com" />
      </label>
      <label class="field">
        <span>密码</span>
        <input v-model="password" type="password" autocomplete="current-password" placeholder="••••••" />
      </label>
      <button type="submit" class="btn-primary login-btn" :disabled="isLoading">
        {{ isLoading ? '登录中…' : '登 录' }}
      </button>
    </form>

    <div class="demo-accounts">
      <div class="demo-title">演示账号（密码均为 123456）</div>
      <button
        v-for="acc in demoAccounts"
        :key="acc.email"
        type="button"
        class="demo-account"
        @click="fill(acc.email)"
      >
        <span class="demo-email">{{ acc.email }}</span>
        <span class="demo-role">{{ acc.role }}</span>
      </button>
    </div>
  </div>
</template>

<style scoped>
.login h1 {
  font-family: var(--font-heading);
  font-size: 1.5rem;
  margin-bottom: 4px;
}

.subtitle {
  color: var(--text-secondary);
  font-size: 0.875rem;
  margin-bottom: 24px;
}

.login-alert {
  margin-bottom: 16px;
}

.field {
  display: block;
  margin-bottom: 16px;
}

.field span {
  display: block;
  font-size: 0.8125rem;
  font-weight: 500;
  color: var(--text-secondary);
  margin-bottom: 6px;
}

.field input {
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
}

.field input:focus {
  outline: none;
  border-color: var(--primary);
  box-shadow: 0 0 0 4px var(--primary-light);
}

.login-btn {
  width: 100%;
  padding: 11px;
  margin-top: 4px;
  font-size: 0.9375rem;
}

.login-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.demo-accounts {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px dashed var(--border);
}

.demo-title {
  font-size: 0.75rem;
  color: var(--text-tertiary);
  margin-bottom: 10px;
}

.demo-account {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
  padding: 8px 12px;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-sm);
  background: var(--surface-1);
  cursor: pointer;
  transition: all var(--transition-fast);
  font-family: var(--font-body);
  margin-bottom: 8px;
  text-align: left;
}

.demo-account:hover {
  border-color: var(--primary);
  background: var(--primary-subtle);
}

.demo-email {
  font-size: 0.8125rem;
  color: var(--text-primary);
  font-weight: 500;
}

.demo-role {
  font-size: 0.6875rem;
  color: var(--text-tertiary);
}
</style>
