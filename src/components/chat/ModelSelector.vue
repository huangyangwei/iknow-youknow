<script setup lang="ts">
import { ref } from 'vue'
import { onClickOutside } from '@vueuse/core'
import { useChatStore } from '@/stores/chat'

defineOptions({ name: 'ModelSelector' })

const chat = useChatStore()
const open = ref(false)
const rootEl = ref<HTMLElement>()

onClickOutside(rootEl, () => {
  open.value = false
})

function selectModel(key: string): void {
  chat.setModel(key)
  open.value = false
}
</script>

<template>
  <div ref="rootEl" class="model-selector">
    <button type="button" class="model-selector-trigger" :class="{ open }" @click="open = !open">
      <span class="model-dot" :class="chat.activeModel.dot"></span>
      <span class="model-selector-name">{{ chat.activeModel.name }}</span>
      <span class="model-selector-chevron">▾</span>
    </button>

    <Transition name="dropdown">
      <div v-if="open" class="model-dropdown">
        <button
          v-for="m in chat.models"
          :key="m.key"
          type="button"
          class="model-option"
          :class="{ active: chat.modelKey === m.key }"
          @click="selectModel(m.key)"
        >
          <span class="model-dot" :class="m.dot"></span>
          <span class="model-info">
            <span class="model-name">{{ m.name }}</span>
            <span class="model-desc">{{ m.desc }}</span>
          </span>
          <span class="model-check">✓</span>
        </button>
      </div>
    </Transition>
  </div>
</template>

<style scoped>
.model-selector {
  position: relative;
}

.model-selector-trigger {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border: 1px solid var(--border);
  border-radius: 99px;
  background: #fff;
  font-size: 0.8125rem;
  color: var(--text-primary);
  cursor: pointer;
  font-family: var(--font-body);
  transition: all var(--transition-fast);
}

.model-selector-trigger:hover,
.model-selector-trigger.open {
  border-color: var(--primary);
  background: var(--primary-subtle);
}

.model-selector-name {
  font-weight: 600;
  white-space: nowrap;
}

.model-selector-chevron {
  color: var(--text-tertiary);
  font-size: 0.6875rem;
  transition: transform var(--transition-fast);
}

.model-selector-trigger.open .model-selector-chevron {
  transform: rotate(180deg);
}

.model-dropdown {
  position: absolute;
  right: 0;
  top: calc(100% + 6px);
  z-index: 50;
  min-width: 240px;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  padding: 6px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.model-option {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 9px 10px;
  border: none;
  border-radius: var(--radius-sm);
  background: none;
  cursor: pointer;
  font-family: var(--font-body);
  text-align: left;
  transition: background var(--transition-fast);
}

.model-option:hover {
  background: var(--surface-1);
}

.model-option.active {
  background: var(--primary-subtle);
}

.model-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.model-name {
  font-size: 0.8125rem;
  font-weight: 600;
  color: var(--text-primary);
}

.model-desc {
  font-size: 0.6875rem;
  color: var(--text-tertiary);
}

.model-check {
  color: var(--primary);
  font-size: 0.8125rem;
  opacity: 0;
}

.model-option.active .model-check {
  opacity: 1;
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: opacity var(--transition-fast), transform var(--transition-fast);
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
