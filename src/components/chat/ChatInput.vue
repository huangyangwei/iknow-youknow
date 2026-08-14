<script setup lang="ts">
import { nextTick, ref, watch } from 'vue'

defineOptions({ name: 'ChatInput' })

const props = defineProps<{
  sending?: boolean
  streaming?: boolean
}>()

const emit = defineEmits<{
  send: [question: string]
  stop: []
}>()

const input = ref('')
const textareaEl = ref<HTMLTextAreaElement>()

function autoResize(): void {
  if (!textareaEl.value) return
  textareaEl.value.style.height = 'auto'
  textareaEl.value.style.height = `${Math.min(textareaEl.value.scrollHeight, 140)}px`
}

function submit(): void {
  const question = input.value.trim()
  if (!question || props.sending || props.streaming) return
  input.value = ''
  void nextTick(autoResize)
  emit('send', question)
}

function onKeydown(event: KeyboardEvent): void {
  if (event.key === 'Enter' && !event.shiftKey) {
    event.preventDefault()
    submit()
  }
}

watch(input, autoResize)
</script>

<template>
  <div class="chat-input-wrapper">
    <div class="chat-input-box">
      <textarea
        ref="textareaEl"
        v-model="input"
        rows="1"
        placeholder="输入问题，Enter 发送，Shift+Enter 换行"
        @keydown="onKeydown"
      ></textarea>
      <button v-if="streaming" type="button" class="btn-stop" @click="emit('stop')">■ 停止</button>
      <button v-else type="button" class="btn-send" :disabled="sending || !input.trim()" @click="submit">↑</button>
    </div>
    <p v-if="streaming" class="chat-input-hint">正在生成回答，点击「停止」可中断；停止后可继续输入或点击重试</p>
  </div>
</template>

<style scoped>
.chat-input-wrapper {
  padding: 12px 16px;
  border-top: 1px solid var(--border-light);
  background: #fff;
}

.chat-input-box {
  display: flex;
  align-items: flex-end;
  gap: 10px;
  background: var(--surface-1);
  border: 1px solid var(--border);
  border-radius: var(--radius-lg);
  padding: 10px 10px 10px 16px;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.chat-input-box:focus-within {
  border-color: var(--primary);
  box-shadow: 0 0 0 3px var(--primary-light);
}

.chat-input-box textarea {
  flex: 1;
  resize: none;
  border: none;
  outline: none;
  background: transparent;
  font-size: 0.9063rem;
  font-family: var(--font-body);
  line-height: 1.6;
  color: var(--text-primary);
  max-height: 140px;
}

.chat-input-box textarea::placeholder {
  color: var(--text-placeholder);
}

.btn-send,
.btn-stop {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  border: none;
  cursor: pointer;
  font-size: 1rem;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-body);
  transition: background var(--transition-fast);
  flex-shrink: 0;
}

.btn-send {
  background: var(--primary);
  color: #fff;
}

.btn-send:hover:not(:disabled) {
  background: var(--primary-hover);
}

.btn-send:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-stop {
  background: var(--danger, #dc2626);
  color: #fff;
  font-size: 0.8125rem;
  font-weight: 600;
}

.btn-stop:hover {
  background: var(--danger-hover, #b91c1c);
}

.chat-input-hint {
  margin: 6px 4px 0;
  font-size: 0.6875rem;
  color: var(--text-tertiary);
}
</style>
