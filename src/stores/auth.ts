import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { useStorage, type RemovableRef } from '@vueuse/core'
import { authApi } from '@/api/auth'
import { tokenStore } from '@/api/token'
import type { LoginPayload, UserInfo } from '@/types/api'
import { hasPermission as checkPermission, isAdminRole, roleLabel } from '@/utils/roles'

const userSerializer = {
  read: (v: string): UserInfo | null => (v ? (JSON.parse(v) as UserInfo) : null),
  write: (v: UserInfo | null): string => JSON.stringify(v),
}

export const useAuthStore = defineStore('auth', () => {
  const token = useStorage<string | null>('iknow.token', null) as RemovableRef<string | null>
  const user = useStorage<UserInfo | null>('iknow.user', null, undefined, { serializer: userSerializer })

  const isAuthenticated = computed(() => Boolean(token.value))
  const roles = computed<string[]>(() => user.value?.roles ?? [])
  const displayName = computed(() => user.value?.nickname || user.value?.username || '')
  const avatarText = computed(() => (displayName.value || '用').slice(0, 1))
  const roleText = computed(() => roleLabel(roles.value))
  const isAdmin = computed(() => isAdminRole(roles.value))

  function syncToken(): void {
    tokenStore.set(token.value)
  }

  function hasPermission(permission: string): boolean {
    return checkPermission(roles.value, permission)
  }

  async function login(payload: LoginPayload): Promise<void> {
    const res = await authApi.login(payload)
    token.value = res.accessToken
    user.value = res.user
    syncToken()
  }

  async function fetchMe(): Promise<void> {
    const me = await authApi.me()
    user.value = me
  }

  function signOut(): void {
    token.value = null
    user.value = null
    tokenStore.set(null)
  }

  return {
    token,
    user,
    isAuthenticated,
    roles,
    displayName,
    avatarText,
    roleText,
    isAdmin,
    hasPermission,
    login,
    fetchMe,
    signOut,
  }
})
