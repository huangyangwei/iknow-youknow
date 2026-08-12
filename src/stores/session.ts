import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { useStorage } from '@vueuse/core'

export const useSessionStore = defineStore('session', () => {
  const token = useStorage<string | null>('session-token', null)
  const permissions = useStorage<string[]>('session-permissions', [])
  const model = useStorage('preferred-model', 'default')
  const isAuthenticated = computed(() => Boolean(token.value))
  function setSession(nextToken: string, nextPermissions: string[]) { token.value = nextToken; permissions.value = nextPermissions }
  function hasPermission(permission: string) { return permissions.value.includes(permission) }
  function signOut() { token.value = null; permissions.value = [] }
  return { token, permissions, model, isAuthenticated, setSession, hasPermission, signOut }
})
