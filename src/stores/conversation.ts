import { ref } from 'vue'
import { defineStore } from 'pinia'
import type { ChatMessage } from '../types/api'

export const useConversationStore = defineStore('conversation', () => {
  const messages = ref<ChatMessage[]>([])
  const cursor = ref<string | undefined>()
  const isStreaming = ref(false)
  function reset() { messages.value = []; cursor.value = undefined; isStreaming.value = false }
  return { messages, cursor, isStreaming, reset }
})
