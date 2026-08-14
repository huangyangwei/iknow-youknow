import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import './style.css'
import App from './App.vue'
import { router } from './router'
import { useAuthStore } from '@/stores/auth'
import { setupMocks } from '@/mock/setup'

setupMocks()

const app = createApp(App)

app.use(createPinia())
// 应用启动时把持久化 token 同步到内存 tokenStore，供 axios 拦截器使用（刷新不丢会话）
useAuthStore().syncToken()
app.use(router)
app.use(ElementPlus)
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 401（令牌过期/未登录）全局处理：登出并回到登录页
window.addEventListener('auth:unauthorized', () => {
  const auth = useAuthStore()
  if (auth.isAuthenticated) {
    auth.signOut()
  }
  if (router.currentRoute.value.name !== 'login') {
    void router.push({ name: 'login', query: { redirect: router.currentRoute.value.fullPath } })
  }
})

app.mount('#app')
