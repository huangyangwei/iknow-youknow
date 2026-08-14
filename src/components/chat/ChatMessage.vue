<script setup lang="ts">
import AnswerCard from './AnswerCard.vue'
import type { ChatMessage as ChatMessageData } from '@/types/api'

defineOptions({ name: 'ChatMessage' })

defineProps<{
  message: ChatMessageData
  streaming?: boolean
}>()

const emit = defineEmits<{
  retry: []
}>()
</script>

<template>
  <div class="chat-message" :class="message.role">
    <div class="chat-avatar" :class="message.role">{{ message.role === 'user' ? '我' : 'AI' }}</div>
    <div class="chat-bubble" :class="message.role">
      <template v-if="message.role === 'user'">
        <span class="user-text">{{ message.content }}</span>
      </template>
      <AnswerCard v-else :message="message" :streaming="streaming" @retry="emit('retry')" />
    </div>
  </div>
</template>

<style scoped>
.chat-message {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.chat-message.user {
  flex-direction: row-reverse;
}

.chat-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 0.8125rem;
  font-weight: 600;
  flex-shrink: 0;
  color: #fff;
}

.chat-avatar.user {
  background: var(--primary);
}

.chat-avatar.assistant {
  background: var(--text-tertiary);
}

.chat-bubble {
  max-width: min(680px, 82%);
  padding: 12px 16px;
  border-radius: var(--radius-md);
  font-size: 0.9063rem;
  line-height: 1.75;
  word-break: break-word;
}

.chat-bubble.user {
  background: var(--primary);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.chat-bubble.assistant {
  background: var(--surface-1);
  border: 1px solid var(--border-light);
  border-bottom-left-radius: 4px;
}

.user-text {
  white-space: pre-wrap;
}
</style>
