<script setup lang="ts">
import { ref } from 'vue'

defineOptions({ name: 'TagInput' })

const props = defineProps<{
  modelValue: string[]
  suggestions?: string[]
}>()

const emit = defineEmits<{
  'update:modelValue': [value: string[]]
}>()

const input = ref('')

function add() {
  const tag = input.value.trim()
  if (!tag) return
  if (!props.modelValue.includes(tag)) {
    emit('update:modelValue', [...props.modelValue, tag])
  }
  input.value = ''
}

function remove(tag: string) {
  emit('update:modelValue', props.modelValue.filter((t) => t !== tag))
}

function addSuggestion(tag: string) {
  if (!props.modelValue.includes(tag)) {
    emit('update:modelValue', [...props.modelValue, tag])
  }
}

const remaining = () => (props.suggestions ?? []).filter((s) => !props.modelValue.includes(s)).slice(0, 6)
</script>

<template>
  <div class="tag-input">
    <div class="tags">
      <span v-for="tag in modelValue" :key="tag" class="tag">
        # {{ tag }}
        <button type="button" class="tag-remove" title="移除" @click="remove(tag)">×</button>
      </span>
      <input
        v-model="input"
        type="text"
        placeholder="输入标签后回车"
        @keydown.enter.prevent="add"
        @blur="add"
      />
    </div>
    <div v-if="remaining().length" class="suggestions">
      <button
        v-for="s in remaining()"
        :key="s"
        type="button"
        class="suggestion"
        @click="addSuggestion(s)"
      >
        + {{ s }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.tag-input {
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #fff;
  padding: 8px;
}

.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  align-items: center;
}

.tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 0.75rem;
  color: var(--text-secondary);
  background: var(--surface-1);
  border: 1px solid var(--border-light);
  border-radius: 99px;
  padding: 3px 6px 3px 10px;
}

.tag-remove {
  border: none;
  background: none;
  color: var(--text-tertiary);
  cursor: pointer;
  font-size: 0.9375rem;
  line-height: 1;
  padding: 0 2px;
}

.tag-remove:hover {
  color: var(--danger);
}

.tags input {
  flex: 1;
  min-width: 140px;
  border: none;
  outline: none;
  padding: 4px 6px;
  font-size: 0.875rem;
  font-family: var(--font-body);
  color: var(--text-primary);
  background: transparent;
}

.suggestions {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px dashed var(--border-light);
}

.suggestion {
  border: 1px dashed var(--border);
  background: var(--surface-1);
  color: var(--text-secondary);
  font-size: 0.75rem;
  border-radius: 99px;
  padding: 3px 10px;
  cursor: pointer;
  font-family: var(--font-body);
  transition: all var(--transition-fast);
}

.suggestion:hover {
  border-color: var(--primary);
  color: var(--primary);
  background: var(--primary-subtle);
}
</style>
