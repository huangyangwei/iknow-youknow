<script setup lang="ts">
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useChatStore } from '@/stores/chat'

defineOptions({ name: 'ChatHistorySidebar' })

const chat = useChatStore()
const router = useRouter()

function newChat(): void {
  chat.newChat()
  void router.push({ name: 'chat' })
}

async function openSession(id: number): Promise<void> {
  if (chat.isStreaming) {
    ElMessage.warning('当前有回答正在生成，请先停止')
    return
  }
  try {
    await chat.openSession(id)
    void router.replace({ name: 'chat', params: { sessionId: id } })
  } catch {
    ElMessage.error('加载会话失败')
  }
}

async function removeSession(event: MouseEvent, id: number): Promise<void> {
  event.stopPropagation()
  try {
    await ElMessageBox.confirm('删除后不可恢复，确定删除该会话？', '删除会话', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await chat.deleteSession(id)
    ElMessage.success('会话已删除')
  } catch {
    ElMessage.error('删除失败')
  }
}

function formatTime(value?: string): string {
  if (!value) return ''
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
</script>

<template>
  <aside class="chat-sidebar">
    <div class="sidebar-header">
      <h3>📝 历史对话</h3>
      <button type="button" class="new-chat-btn" @click="newChat">+ 新对话</button>
    </div>

    <div class="session-list">
      <div
        v-for="s in chat.sessions"
        :key="s.id"
        class="session-item"
        :class="{ active: chat.activeSessionId === s.id }"
        @click="openSession(s.id)"
      >
        <span class="session-title">{{ s.title }}</span>
        <span class="session-time">{{ formatTime(s.updatedAt) }}</span>
        <button type="button" class="session-delete" title="删除会话" @click="removeSession($event, s.id)">✕</button>
      </div>
      <div v-if="!chat.sessions.length" class="session-empty">暂无历史对话</div>
    </div>
  </aside>
</template>

<style scoped>
.chat-sidebar {
  background: #fff;
  border: 1px solid var(--border-light);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.sidebar-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 14px 14px 10px;
}

.sidebar-header h3 {
  font-size: 0.875rem;
  font-weight: 600;
  margin: 0;
  color: var(--text-primary);
}

.new-chat-btn {
  padding: 5px 12px;
  border: none;
  border-radius: 99px;
  background: var(--primary);
  color: #fff;
  font-size: 0.75rem;
  font-weight: 600;
  cursor: pointer;
  font-family: var(--font-body);
  transition: background var(--transition-fast);
  white-space: nowrap;
}

.new-chat-btn:hover {
  background: var(--primary-hover);
}

.session-list {
  flex: 1;
  overflow-y: auto;
  padding: 0 8px 8px;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.session-item {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  padding: 10px 28px 10px 12px;
  border: none;
  border-radius: var(--radius-sm);
  background: none;
  cursor: pointer;
  font-family: var(--font-body);
  text-align: left;
  transition: background var(--transition-fast);
}

.session-item:hover {
  background: var(--surface-1);
}

.session-item.active {
  background: var(--primary-subtle);
}

.session-title {
  font-size: 0.8125rem;
  color: var(--text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  width: 100%;
}

.session-time {
  font-size: 0.6875rem;
  color: var(--text-tertiary);
}

.session-delete {
  position: absolute;
  right: 8px;
  top: 50%;
  transform: translateY(-50%);
  width: 18px;
  height: 18px;
  border: none;
  border-radius: 50%;
  background: transparent;
  color: var(--text-tertiary);
  font-size: 0.625rem;
  cursor: pointer;
  line-height: 1;
  display: none;
  align-items: center;
  justify-content: center;
  font-family: var(--font-body);
}

.session-item:hover .session-delete {
  display: flex;
}

.session-delete:hover {
  background: var(--danger-subtle, #fee2e2);
  color: var(--danger, #dc2626);
}

.session-empty {
  padding: 24px 12px;
  font-size: 0.75rem;
  color: var(--text-tertiary);
  text-align: center;
}

@media (max-width: 900px) {
  .chat-sidebar {
    display: none;
  }
}
</style>
