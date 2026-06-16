<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, watch, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { useChatStore } from '@/stores/chat'
import { kbApi } from '@/api/knowledgeBases'
import { convApi } from '@/api/conversations'
import { ApiError } from '@/api/client'
import AppLayout from '@/components/layout/AppLayout.vue'
import LoadingSpinner from '@/components/ui/LoadingSpinner.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import Modal from '@/components/ui/Modal.vue'
import { formatDate, formatDateTime } from '@/utils/format'
import type { Conversation, KnowledgeBase, Source, Message } from '@/types'
import {
  Send,
  Plus,
  MessageSquare,
  Trash2,
  Edit3,
  Check,
  X,
  FileText,
  ChevronRight,
  Brain,
  User,
  Bot,
  ThumbsUp,
  ThumbsDown,
  StopCircle,
  BookOpen,
  Database,
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const app = useAppStore()
const chat = useChatStore()

// ── State ──
const knowledgeBases = ref<KnowledgeBase[]>([])
const selectedKbId = ref<number | null>(null)
const questionInput = ref('')
const chatContainer = ref<HTMLElement | null>(null)
const inputRef = ref<HTMLTextAreaElement | null>(null)

// Rename conversation
const showRenameModal = ref(false)
const renamingConv = ref<Conversation | null>(null)
const renameTitle = ref('')
const renameLoading = ref(false)

// Expanded source
const expandedSource = ref<number | null>(null) // message index

// Feedback state
const feedbackGiven = ref<Record<number, 'up' | 'down'>>({})

// ── Computed ──
const convId = computed(() => {
  const id = route.params.convId
  return id ? Number(id) : null
})

const canSend = computed(() => {
  return selectedKbId.value && questionInput.value.trim() && !chat.isStreaming
})

// ── Data Fetching ──
async function fetchKnowledgeBases() {
  try {
    const res = await kbApi.list({ size: 50 })
    knowledgeBases.value = res.data.items
  } catch {}
}

async function loadConversation(id: number) {
  await chat.fetchMessages(id)
  // Auto-select KB from conversation
  if (chat.messages.length > 0) {
    // We need to get the conversation's kb_id from conversations list
    const conv = chat.conversations.find((c) => c.id === id)
    if (conv) {
      selectedKbId.value = conv.kb_id
    }
  }
  scrollToBottom()
}

// ── Actions ──
async function sendMessage() {
  if (!canSend.value) return

  const question = questionInput.value.trim()
  const kbId = selectedKbId.value!

  // Add user message locally
  const userMsg: Message = {
    id: Date.now(),
    role: 'user',
    content: question,
    sources_json: null,
    token_count: null,
    created_at: new Date().toISOString(),
  }
  chat.messages.push(userMsg)

  // Clear input
  questionInput.value = ''
  if (inputRef.value) {
    inputRef.value.style.height = 'auto'
  }

  scrollToBottom()

  // Send to backend
  await chat.askQuestion(kbId, question, convId.value)

  // After streaming, scroll
  scrollToBottom()

  // Update URL if new conversation
  if (chat.currentConvId && !convId.value) {
    router.replace(`/chat/${chat.currentConvId}`)
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendMessage()
  }
}

function autoResize(el: EventTarget | null) {
  const textarea = el as HTMLTextAreaElement | null
  if (!textarea) return
  textarea.style.height = 'auto'
  textarea.style.height = Math.min(textarea.scrollHeight, 200) + 'px'
}

function scrollToBottom() {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

// ── Conversation Management ──
function startNewChat() {
  chat.clearChat()
  router.replace('/chat')
}

function selectConversation(conv: Conversation) {
  router.push(`/chat/${conv.id}`)
}

function openRenameModal(conv: Conversation) {
  renamingConv.value = conv
  renameTitle.value = conv.title
  showRenameModal.value = true
}

async function handleRename() {
  // Note: The API doesn't have a rename endpoint in the spec, so we'd need to add one
  // For now, close the modal
  showRenameModal.value = false
  app.showToast('重命名功能需要后端支持', 'warning')
}

async function handleDeleteConv(conv: Conversation) {
  if (!confirm(`确定要删除对话「${conv.title}」吗？`)) return
  await chat.deleteConversation(conv.id)
  if (convId.value === conv.id) {
    router.replace('/chat')
  }
}

async function handleFeedback(msgId: number, type: 'up' | 'down') {
  try {
    await convApi.feedback(msgId, type)
    feedbackGiven.value[msgId] = type
    app.showToast(type === 'up' ? '感谢反馈！' : '我们会努力改进', 'success')
  } catch {}
}

// ── Helpers ──
function getSourceKey(source: Source, msgIdx: number, sourceIdx: number): string {
  return `${msgIdx}_${sourceIdx}`
}

// ── Lifecycle ──
onMounted(async () => {
  await fetchKnowledgeBases()
  await chat.fetchConversations()

  // Set selected KB automatically if there's only one
  if (knowledgeBases.value.length === 1) {
    selectedKbId.value = knowledgeBases.value[0].id
  }

  // Load conversation if ID in route
  if (convId.value) {
    await loadConversation(convId.value)
  }

  // Focus input
  nextTick(() => {
    inputRef.value?.focus()
  })
})

// Watch route changes
watch(() => route.params.convId, async (newId) => {
  if (newId) {
    await loadConversation(Number(newId))
  } else {
    chat.clearChat()
  }
})
</script>

<template>
  <AppLayout>
    <div class="flex h-[calc(100vh-3rem)] -my-6 -mx-6">
      <!-- ─── Conversation Sidebar ─── -->
      <div class="w-72 border-r border-gpt-border flex flex-col bg-gpt-surface/30 shrink-0">
        <!-- Header -->
        <div class="px-4 py-4 border-b border-gpt-border">
          <button
            @click="startNewChat"
            class="w-full flex items-center justify-center gap-2 px-4 py-2.5 bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 text-white font-medium rounded-xl transition-all text-sm shadow-glow"
          >
            <Plus :size="18" />
            新对话
          </button>
        </div>

        <!-- KB Selector -->
        <div class="px-4 py-3 border-b border-gpt-border">
          <label class="text-xs font-medium text-gpt-dim mb-1.5 block">知识库</label>
          <select
            v-model="selectedKbId"
            class="w-full px-3 py-2 bg-gpt-bg border border-gpt-border rounded-xl text-sm text-gpt-text focus:outline-none focus:border-blue-500/50 transition-all"
          >
            <option :value="null" disabled>选择知识库</option>
            <option v-for="kb in knowledgeBases" :key="kb.id" :value="kb.id">
              {{ kb.icon || '📁' }} {{ kb.name }}
            </option>
          </select>
        </div>

        <!-- Conversation List -->
        <div class="flex-1 overflow-y-auto px-3 py-3 space-y-1">
          <p class="text-xs text-gpt-dim px-2 mb-2">历史对话</p>

          <div
            v-for="conv in chat.conversations"
            :key="conv.id"
            :class="[
              'group flex items-center gap-2 px-3 py-2.5 rounded-xl cursor-pointer transition-all text-sm',
              convId === conv.id
                ? 'bg-blue-500/10 text-blue-400'
                : 'text-gpt-muted hover:text-gpt-text hover:bg-white/5',
            ]"
            @click="selectConversation(conv)"
          >
            <MessageSquare :size="15" class="shrink-0" />
            <span class="flex-1 truncate">{{ conv.title }}</span>
            <div class="hidden group-hover:flex items-center gap-0.5 shrink-0">
              <button
                @click.stop="openRenameModal(conv)"
                class="p-1 rounded hover:bg-white/10 text-gpt-dim hover:text-gpt-text transition-colors"
              >
                <Edit3 :size="12" />
              </button>
              <button
                @click.stop="handleDeleteConv(conv)"
                class="p-1 rounded hover:bg-red-500/10 text-gpt-dim hover:text-red-400 transition-colors"
              >
                <Trash2 :size="12" />
              </button>
            </div>
          </div>

          <EmptyState
            v-if="chat.conversations.length === 0"
            icon="💬"
            title="暂无对话"
            description="选择一个知识库，开始提问吧"
          />
        </div>
      </div>

      <!-- ─── Chat Main Area ─── -->
      <div class="flex-1 flex flex-col min-w-0">
        <!-- Messages -->
        <div
          ref="chatContainer"
          class="flex-1 overflow-y-auto px-8 py-6 space-y-6"
        >
          <!-- Empty state -->
          <div
            v-if="chat.messages.length === 0 && !chat.streamingMessage"
            class="flex flex-col items-center justify-center h-full text-center"
          >
            <div class="w-20 h-20 rounded-full bg-gradient-to-br from-blue-500/20 to-emerald-500/20 flex items-center justify-center mb-6">
              <Brain :size="40" class="text-blue-400" />
            </div>
            <h2 class="text-2xl font-bold text-gpt-text mb-2">StoneRAG Hub</h2>
            <p class="text-gpt-muted max-w-md mb-6">
              选择一个知识库，向我提问任何关于你文档的问题。
              我会基于你的知识库内容给出精准答案。
            </p>
            <div class="grid grid-cols-2 gap-3 max-w-lg w-full">
              <button
                v-for="suggestion in [
                  '这份文档的核心内容是什么？',
                  '帮我总结关键要点',
                  '文档中提到的技术方案有哪些？',
                  '对比分析文档中的方案优劣',
                ]"
                :key="suggestion"
                @click="questionInput = suggestion; inputRef?.focus()"
                :disabled="!selectedKbId"
                class="text-left px-4 py-3 rounded-xl border border-gpt-border text-sm text-gpt-muted hover:text-gpt-text hover:border-gpt-accent/30 hover:bg-blue-500/5 transition-all disabled:opacity-40 disabled:cursor-not-allowed"
              >
                {{ suggestion }}
              </button>
            </div>
          </div>

          <!-- Message bubbles -->
          <template v-for="(msg, idx) in chat.messages" :key="msg.id">
            <!-- User message -->
            <div v-if="msg.role === 'user'" class="flex justify-end animate-fade-in">
              <div class="max-w-[75%] bg-gradient-to-br from-blue-500 to-blue-600 text-white rounded-2xl rounded-br-md px-5 py-3 shadow-glow">
                <p class="text-sm leading-relaxed whitespace-pre-wrap">{{ msg.content }}</p>
              </div>
            </div>

            <!-- Assistant message -->
            <div v-else class="flex gap-4 animate-fade-in">
              <div class="w-8 h-8 rounded-full bg-gradient-to-br from-emerald-400 to-emerald-600 flex items-center justify-center shrink-0 mt-1">
                <Bot :size="16" class="text-white" />
              </div>
              <div class="flex-1 min-w-0 max-w-[85%]">
                <!-- Sources -->
                <div v-if="msg.sources_json && msg.sources_json.length > 0" class="mb-3">
                  <div class="flex items-center gap-2 mb-2">
                    <BookOpen :size="14" class="text-blue-400" />
                    <span class="text-xs font-medium text-blue-400">引用来源</span>
                  </div>
                  <div class="grid grid-cols-1 gap-2">
                    <div
                      v-for="(source, si) in msg.sources_json"
                      :key="si"
                      class="p-3 rounded-xl border border-gpt-border bg-gpt-surface/50 hover:border-blue-500/20 transition-colors cursor-pointer"
                      @click="expandedSource = expandedSource === getSourceKey(source, idx, si) as any ? null : getSourceKey(source, idx, si) as any"
                    >
                      <div class="flex items-center justify-between">
                        <div class="flex items-center gap-2 text-sm">
                          <FileText :size="14" class="text-gpt-muted" />
                          <span class="text-gpt-text font-medium">{{ source.doc_title }}</span>
                          <span class="text-xs px-1.5 py-0.5 rounded-md bg-blue-500/10 text-blue-400">
                            相关度 {{ (source.score * 100).toFixed(0) }}%
                          </span>
                        </div>
                        <ChevronRight
                          :size="14"
                          :class="expandedSource === (getSourceKey(source, idx, si) as any) ? 'rotate-90' : ''"
                          class="text-gpt-dim transition-transform"
                        />
                      </div>
                      <p
                        v-if="expandedSource === (getSourceKey(source, idx, si) as any)"
                        class="mt-2 text-sm text-gpt-muted leading-relaxed animate-slide-down"
                      >
                        {{ source.text }}
                      </p>
                    </div>
                  </div>
                </div>

                <!-- Message content -->
                <div
                  class="text-sm leading-relaxed text-gpt-text message-content"
                  v-html="msg.content.replace(/\n/g, '<br>')"
                />

                <!-- Token count & feedback -->
                <div v-if="msg.token_count" class="flex items-center gap-3 mt-2">
                  <span class="text-xs text-gpt-dim">{{ msg.token_count }} tokens</span>
                  <button
                    v-if="!feedbackGiven[msg.id]"
                    @click="handleFeedback(msg.id, 'up')"
                    class="p-1 rounded hover:bg-white/5 text-gpt-dim hover:text-emerald-400 transition-colors"
                  >
                    <ThumbsUp :size="13" />
                  </button>
                  <button
                    v-if="!feedbackGiven[msg.id]"
                    @click="handleFeedback(msg.id, 'down')"
                    class="p-1 rounded hover:bg-white/5 text-gpt-dim hover:text-red-400 transition-colors"
                  >
                    <ThumbsDown :size="13" />
                  </button>
                  <span
                    v-if="feedbackGiven[msg.id]"
                    class="text-xs"
                    :class="feedbackGiven[msg.id] === 'up' ? 'text-emerald-400' : 'text-red-400'"
                  >
                    {{ feedbackGiven[msg.id] === 'up' ? '👍 已点赞' : '👎 已反馈' }}
                  </span>
                </div>
              </div>
            </div>
          </template>

          <!-- Streaming message -->
          <div v-if="chat.streamingMessage" class="flex gap-4 animate-fade-in">
            <div class="w-8 h-8 rounded-full bg-gradient-to-br from-emerald-400 to-emerald-600 flex items-center justify-center shrink-0 mt-1">
              <Bot :size="16" class="text-white" />
            </div>
            <div class="flex-1 min-w-0 max-w-[85%]">
              <!-- Thinking animation -->
              <div v-if="chat.streamingMessage.thinking" class="mb-3">
                <div class="flex items-center gap-2 text-sm text-gpt-muted">
                  <span class="thinking-ripple">
                    <span class="dot" />
                    <span class="dot" />
                    <span class="dot" />
                  </span>
                  <span>
                    {{
                      chat.streamingMessage.thinkingStage === 'retrieving' ? '正在检索相关文档...' :
                      chat.streamingMessage.thinkingStage === 'reranking' ? '正在重排序结果...' :
                      '正在生成回答...'
                    }}
                  </span>
                </div>
              </div>

              <!-- Sources (during streaming) -->
              <div v-if="chat.streamingMessage.sources.length > 0" class="mb-3">
                <div class="flex items-center gap-2 mb-2">
                  <BookOpen :size="14" class="text-blue-400" />
                  <span class="text-xs font-medium text-blue-400">引用来源</span>
                </div>
                <div class="flex flex-wrap gap-2">
                  <div
                    v-for="(source, si) in chat.streamingMessage.sources"
                    :key="si"
                    class="px-3 py-1.5 rounded-lg border border-gpt-border bg-gpt-surface/50 text-xs text-gpt-muted"
                  >
                    📄 {{ source.doc_title }}
                    <span class="ml-1 text-blue-400">({{ (source.score * 100).toFixed(0) }}%)</span>
                  </div>
                </div>
              </div>

              <!-- Streaming content -->
              <div
                v-if="chat.streamingMessage.content"
                class="text-sm leading-relaxed text-gpt-text message-content"
                :class="{ 'typing-cursor': !chat.streamingMessage.done }"
              >
                {{ chat.streamingMessage.content }}
              </div>
            </div>
          </div>
        </div>

        <!-- Input area -->
        <div class="px-8 py-4 border-t border-gpt-border bg-gpt-bg/80 backdrop-blur-sm">
          <div class="max-w-4xl mx-auto">
            <div class="flex items-end gap-3 bg-gpt-surface border border-gpt-border rounded-2xl px-4 py-3 focus-within:border-blue-500/50 focus-within:shadow-glow transition-all">
              <textarea
                ref="inputRef"
                v-model="questionInput"
                @keydown="handleKeydown"
                @input="autoResize($event.target)"
                :disabled="chat.isStreaming"
                rows="1"
                placeholder="输入你的问题... (Shift+Enter 换行)"
                class="flex-1 bg-transparent resize-none text-sm text-gpt-text placeholder:text-gpt-dim focus:outline-none max-h-[200px]"
              />
              <button
                v-if="chat.isStreaming"
                @click="chat.cancelStream()"
                class="p-2 rounded-xl bg-red-500/10 hover:bg-red-500/20 text-red-400 transition-all shrink-0"
                title="停止生成"
              >
                <StopCircle :size="20" />
              </button>
              <button
                v-else
                @click="sendMessage"
                :disabled="!canSend"
                class="p-2 rounded-xl transition-all shrink-0"
                :class="canSend ? 'bg-blue-500 text-white hover:bg-blue-600 shadow-glow' : 'bg-gpt-border text-gpt-dim cursor-not-allowed'"
              >
                <Send :size="20" />
              </button>
            </div>
            <p class="text-xs text-gpt-dim text-center mt-2">
              {{ selectedKbId ? '已选择知识库，AI 将基于知识库内容回答' : '请先选择一个知识库开始对话' }}
            </p>
          </div>
        </div>
      </div>
    </div>

    <!-- ─── Rename Modal ─── -->
    <Modal v-model:open="showRenameModal" title="重命名对话" max-width="max-w-sm">
      <div class="space-y-4">
        <input
          v-model="renameTitle"
          type="text"
          class="w-full px-4 py-2.5 bg-gpt-bg border border-gpt-border rounded-xl text-sm text-gpt-text focus:outline-none focus:border-blue-500/50 transition-all"
        />
        <div class="flex gap-3">
          <button
            @click="showRenameModal = false"
            class="flex-1 py-2 border border-gpt-border rounded-xl text-sm text-gpt-muted hover:text-gpt-text transition-all"
          >
            取消
          </button>
          <button
            @click="handleRename"
            :disabled="renameLoading"
            class="flex-1 py-2 bg-gradient-to-r from-blue-500 to-blue-600 text-white font-medium rounded-xl text-sm transition-all disabled:opacity-50"
          >
            确认
          </button>
        </div>
      </div>
    </Modal>
  </AppLayout>
</template>
