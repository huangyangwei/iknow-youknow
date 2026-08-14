<script setup lang="ts">
import { ref, watch } from 'vue'
import type { FeedbackType } from '@/types/api'

defineOptions({ name: 'FeedbackActions' })

const props = defineProps<{
  likeCount?: number
  liked?: boolean
  disliked?: boolean
  viewCount?: number
}>()

const emit = defineEmits<{
  feedback: [type: FeedbackType, content?: string]
}>()

const liked = ref(props.liked ?? false)
const disliked = ref(props.disliked ?? false)
const showDialog = ref(false)
const feedbackType = ref<FeedbackType>('correction')
const feedbackContent = ref('')

watch(
  () => [props.liked, props.disliked],
  ([l, d]) => {
    liked.value = l ?? false
    disliked.value = d ?? false
  },
)

function toggleLike() {
  if (liked.value) {
    liked.value = false
    return
  }
  if (disliked.value) disliked.value = false
  liked.value = true
  emit('feedback', 'like')
}

function toggleDislike() {
  if (disliked.value) {
    disliked.value = false
    return
  }
  if (liked.value) liked.value = false
  disliked.value = true
  emit('feedback', 'dislike')
}

function open(type: FeedbackType) {
  feedbackType.value = type
  feedbackContent.value = ''
  showDialog.value = true
}

function submitFeedback() {
  emit('feedback', feedbackType.value, feedbackContent.value.trim() || undefined)
  showDialog.value = false
}

const options: { type: FeedbackType; label: string }[] = [
  { type: 'correction', label: '✏️ 内容纠错' },
  { type: 'suggestion', label: '💡 建议补充' },
]
</script>

<template>
  <div class="feedback-actions">
    <button type="button" class="action-btn like" :class="{ active: liked }" @click="toggleLike">
      <span class="icon">👍</span>
      有帮助 <span v-if="likeCount != null">{{ likeCount }}</span>
    </button>
    <button type="button" class="action-btn dislike" :class="{ active: disliked }" @click="toggleDislike">
      <span class="icon">👎</span>
      没帮助
    </button>
    <span class="divider"></span>
    <button v-for="opt in options" :key="opt.type" type="button" class="action-btn" @click="open(opt.type)">
      {{ opt.label }}
    </button>

    <el-dialog
      v-model="showDialog"
      :title="feedbackType === 'correction' ? '内容纠错' : '建议补充'"
      width="min(480px, calc(100vw - 40px))"
      append-to-body
    >
      <el-input
        v-model="feedbackContent"
        type="textarea"
        :rows="4"
        :placeholder="feedbackType === 'correction' ? '请描述错误内容或给出修正建议…' : '请描述需要补充的内容…'"
      />
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" :disabled="!feedbackContent.trim()" @click="submitFeedback">
          提交反馈
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.feedback-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}

.action-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 7px 14px;
  border: 1px solid var(--border);
  border-radius: 99px;
  background: #fff;
  font-size: 0.8125rem;
  color: var(--text-secondary);
  cursor: pointer;
  font-family: var(--font-body);
  transition: all var(--transition-fast);
}

.action-btn:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.action-btn.like.active {
  border-color: var(--success, #15803d);
  background: var(--success-subtle, #dcfce7);
  color: var(--success, #15803d);
  font-weight: 600;
}

.action-btn.dislike.active {
  border-color: var(--danger, #dc2626);
  background: var(--danger-subtle, #fee2e2);
  color: var(--danger, #dc2626);
  font-weight: 600;
}

.divider {
  width: 1px;
  height: 20px;
  background: var(--border);
  margin: 0 4px;
}
</style>
