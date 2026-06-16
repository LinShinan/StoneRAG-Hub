<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { shareApi } from '@/api/shares'
import { ApiError } from '@/api/client'
import { createSSEParser, parseSSEData } from '@/utils/sse'
import type { PublicShare, Source } from '@/types'
import {
  Send,
  Bot,
  Brain,
  BookOpen,
  FileText,
  Eye,
  MessageSquare,
  Globe,
  AlertCircle,
} from 'lucide-vue-next'

const route = useRoute()
const app = useAppStore()

const shareCode = route.params.code as string

// ── State ──
const shareInfo = ref<PublicShare | null>(null)
const loading = ref(true)
const questionInput = ref('')
const isStreaming = ref(false)
const streamContent = ref('')
const streamDone = ref(false)
const streamSources = ref<Source[]>([])
const streamThinking = ref(false)
const streamThinkingStage = ref('')
const abortController = ref<AbortController | null>(null)
const rateLimited = ref(false)
const rateLimitTimer = ref<ReturnType<typeof setTimeout> | null>(null)

const messages = ref<Array<{ role: 'user' | 'assistant'; content: string; sources?: Source[] }>>([])
const chatContainer = ref<HTMLElement | null>(null)

// ── Fetch Share Info ──
async function fetchShareInfo() {
  loading.value = true
  try {
    const res = await shareApi.getPublic(shareCode)
    shareInfo.value = res.data as PublicShare
  } catch (e) {
    if (e instanceof ApiError) {
      app.showToast(e.message, 'error')
    } else {
      app.showToast('加载分享信息失败，请检查链接是否正确', 'error')
    }
  } finally {
    loading.value = false
  }
}

// ── Ask Question ──
async function sendQuestion() {
  const q = questionInput.value.trim()
  if (!q || isStreaming.value || rateLimited.value) return

  if (shareInfo.value?.permission !== 'ask') {
    app.showToast('此分享仅支持查看，不支持提问', 'warning')
    return
  }

  // Add user message
  messages.value.push({ role: 'user', content: q })
  questionInput.value = ''

  // Setup streaming
  isStreaming.value = true
  streamContent.value = ''
  streamDone.value = false
  streamSources.value = []
  streamThinking.value = true
  streamThinkingStage.value = 'retrieving'

  scrollToBottom()

  const controller = new AbortController()
  abortController.value = controller

  try {
    const response = await shareApi.askPublic(shareCode, q, controller.signal)

    if (!response.ok) {
      const err = await response.json().catch(() => ({ code: 50000, message: '请求失败' }))
      if (err.code === 40003) {
        // Rate limited
        rateLimited.value = true
        app.showToast('您提问太快啦，请稍等一分钟再试～', 'warning')
        rateLimitTimer.value = setTimeout(() => {
          rateLimited.value = false
        }, 60000)
        // Remove the user message since it wasn't processed
        messages.value.pop()
        isStreaming.value = false
        streamThinking.value = false
        return
      }
      throw new Error(err.message || '请求失败')
    }

    const reader = response.body!.getReader()
    const decoder = new TextDecoder()
    const parse = createSSEParser()

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      const text = decoder.decode(value, { stream: true })
      const events = parse(text)

      for (const evt of events) {
        if (evt.event === 'thinking') {
          const data = parseSSEData<{ stage: string }>(evt)
          if (data) {
            streamThinkingStage.value = data.stage
            streamThinking.value = true
          }
        } else if (evt.event === 'sources') {
          const sources = parseSSEData<Source[]>(evt)
          if (sources) {
            streamSources.value = sources
          }
          streamThinking.value = false
        } else if (evt.event === 'token') {
          const data = parseSSEData<{ token: string }>(evt)
          if (data) {
            streamContent.value += data.token
          }
          streamThinking.value = false
        } else if (evt.event === 'done') {
          streamDone.value = true
          streamThinking.value = false
        }
      }

      scrollToBottom()
    }

    // Add completed message
    messages.value.push({
      role: 'assistant',
      content: streamContent.value,
      sources: streamSources.value,
    })
  } catch (err: any) {
    if (err.name !== 'AbortError') {
      app.showToast(err.message || '请求失败，请稍后重试', 'error')
    }
  } finally {
    isStreaming.value = false
    abortController.value = null
    streamContent.value = ''
    streamSources.value = []
    streamThinking.value = false
    streamDone.value = false
  }
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    sendQuestion()
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

onMounted(() => {
  fetchShareInfo()
})

onUnmounted(() => {
  if (abortController.value) {
    abortController.value.abort()
  }
  if (rateLimitTimer.value) {
    clearTimeout(rateLimitTimer.value)
  }
})
</script>

<template>
  <div class="min-h-screen bg-gpt-bg flex flex-col">
    <!-- Header -->
    <header class="border-b border-gpt-border bg-gpt-surface/50 backdrop-blur-sm px-6 py-4">
      <div class="max-w-4xl mx-auto flex items-center gap-4">
        <img src="/favicon.png" alt="StoneRAG" class="w-10 h-10 rounded-xl shrink-0" />
        <div class="flex-1 min-w-0">
          <h1 class="text-lg font-bold text-gpt-text truncate">
            来自 <span class="text-blue-400">{{ shareInfo?.owner_name || '...' }}</span> 的共享知识库
          </h1>
          <p class="text-sm text-gpt-muted">
            📁 {{ shareInfo?.kb_name || '加载中...' }}
            <span v-if="shareInfo" class="ml-3">
              · {{ shareInfo.doc_count }} 篇文档
              · {{ shareInfo.view_count }} 次浏览
            </span>
          </p>
        </div>
        <div
          v-if="shareInfo"
          :class="shareInfo.permission === 'ask' ? 'bg-blue-500/10 text-blue-400' : 'bg-emerald-500/10 text-emerald-400'"
          class="px-3 py-1.5 rounded-xl text-sm font-medium flex items-center gap-1.5"
        >
          <component :is="shareInfo.permission === 'ask' ? MessageSquare : Eye" :size="16" />
          {{ shareInfo.permission === 'ask' ? '可提问' : '仅查看' }}
        </div>
      </div>
    </header>

    <!-- Loading -->
    <div v-if="loading" class="flex-1 flex items-center justify-center">
      <div class="text-center">
        <div class="w-12 h-12 border-2 border-gpt-border border-t-blue-400 rounded-full animate-spin mx-auto mb-4" />
        <p class="text-gpt-muted text-sm">加载分享内容中...</p>
      </div>
    </div>

    <!-- Error / Not Found -->
    <div v-else-if="!shareInfo" class="flex-1 flex items-center justify-center">
      <div class="text-center">
        <AlertCircle :size="48" class="text-red-400 mx-auto mb-4" />
        <h2 class="text-xl font-bold text-gpt-text mb-2">分享不存在或已失效</h2>
        <p class="text-gpt-muted">请检查分享链接是否正确，或联系分享者重新获取</p>
      </div>
    </div>

    <!-- Chat Interface -->
    <template v-else>
      <div class="flex-1 flex flex-col max-w-4xl mx-auto w-full">
        <!-- Messages -->
        <div
          ref="chatContainer"
          class="flex-1 overflow-y-auto px-6 py-6 space-y-6"
        >
          <!-- Welcome -->
          <div
            v-if="messages.length === 0 && !isStreaming"
            class="flex flex-col items-center justify-center h-full text-center"
          >
            <div class="w-20 h-20 rounded-full bg-gradient-to-br from-blue-500/20 to-emerald-500/20 flex items-center justify-center mb-6">
              <Globe :size="40" class="text-blue-400" />
            </div>
            <h2 class="text-2xl font-bold text-gpt-text mb-2">
              {{ shareInfo.permission === 'ask' ? '欢迎来到共享知识库' : '共享知识库浏览' }}
            </h2>
            <p class="text-gpt-muted max-w-md">
              <template v-if="shareInfo.permission === 'ask'">
                {{ shareInfo.owner_name }} 分享了「{{ shareInfo.kb_name }}」知识库，
                你可以向 AI 提问关于此知识库内容的问题。
              </template>
              <template v-else>
                {{ shareInfo.owner_name }} 分享了「{{ shareInfo.kb_name }}」知识库供你浏览。
              </template>
            </p>
          </div>

          <!-- Messages -->
          <template v-for="(msg, idx) in messages" :key="idx">
            <!-- User -->
            <div v-if="msg.role === 'user'" class="flex justify-end animate-fade-in">
              <div class="max-w-[75%] bg-gradient-to-br from-blue-500 to-blue-600 text-white rounded-2xl rounded-br-md px-5 py-3 shadow-glow">
                <p class="text-sm leading-relaxed whitespace-pre-wrap">{{ msg.content }}</p>
              </div>
            </div>

            <!-- Assistant -->
            <div v-else class="flex gap-4 animate-fade-in">
              <div class="w-8 h-8 rounded-full bg-gradient-to-br from-emerald-400 to-emerald-600 flex items-center justify-center shrink-0 mt-1">
                <Bot :size="16" class="text-white" />
              </div>
              <div class="flex-1 min-w-0 max-w-[85%]">
                <div
                  class="text-sm leading-relaxed text-gpt-text whitespace-pre-wrap message-content"
                  v-html="msg.content.replace(/\n/g, '<br>')"
                />
                <!-- Sources -->
                <div v-if="msg.sources && msg.sources.length > 0" class="mt-3 flex flex-wrap gap-2">
                  <div
                    v-for="(s, si) in msg.sources"
                    :key="si"
                    class="px-3 py-1.5 rounded-lg border border-gpt-border bg-gpt-surface/50 text-xs text-gpt-muted"
                  >
                    📄 {{ s.doc_title }}
                  </div>
                </div>
              </div>
            </div>
          </template>

          <!-- Streaming -->
          <div v-if="isStreaming" class="flex gap-4 animate-fade-in">
            <div class="w-8 h-8 rounded-full bg-gradient-to-br from-emerald-400 to-emerald-600 flex items-center justify-center shrink-0 mt-1">
              <Bot :size="16" class="text-white" />
            </div>
            <div class="flex-1 min-w-0 max-w-[85%]">
              <!-- Thinking -->
              <div v-if="streamThinking" class="flex items-center gap-2 text-sm text-gpt-muted mb-2">
                <span class="thinking-ripple">
                  <span class="dot" />
                  <span class="dot" />
                  <span class="dot" />
                </span>
                <span>
                  {{
                    streamThinkingStage === 'retrieving' ? '正在检索相关文档...' :
                    streamThinkingStage === 'reranking' ? '正在重排序...' :
                    '正在生成回答...'
                  }}
                </span>
              </div>

              <!-- Sources -->
              <div v-if="streamSources.length > 0" class="flex flex-wrap gap-2 mb-2">
                <div
                  v-for="(s, si) in streamSources"
                  :key="si"
                  class="px-3 py-1.5 rounded-lg border border-gpt-border bg-gpt-surface/50 text-xs text-gpt-muted"
                >
                  📄 {{ s.doc_title }}
                </div>
              </div>

              <!-- Content -->
              <div
                v-if="streamContent"
                class="text-sm leading-relaxed text-gpt-text message-content"
                :class="{ 'typing-cursor': !streamDone }"
              >
                {{ streamContent }}
              </div>
            </div>
          </div>
        </div>

        <!-- Input -->
        <div class="px-6 py-4 border-t border-gpt-border bg-gpt-bg/80 backdrop-blur-sm">
          <div class="flex items-end gap-3 bg-gpt-surface border border-gpt-border rounded-2xl px-4 py-3 focus-within:border-blue-500/50 focus-within:shadow-glow transition-all">
            <textarea
              v-model="questionInput"
              @keydown="handleKeydown"
              :disabled="isStreaming || rateLimited"
              rows="1"
              :placeholder="rateLimited ? '请稍等一分钟再提问...' : shareInfo.permission === 'ask' ? '输入你的问题...' : '此分享仅支持查看'"
              :readonly="shareInfo.permission !== 'ask'"
              class="flex-1 bg-transparent resize-none text-sm text-gpt-text placeholder:text-gpt-dim focus:outline-none"
            />
            <button
              @click="sendQuestion"
              :disabled="!questionInput.trim() || isStreaming || rateLimited || shareInfo.permission !== 'ask'"
              class="p-2 rounded-xl transition-all shrink-0"
              :class="questionInput.trim() && !isStreaming && !rateLimited && shareInfo.permission === 'ask'
                ? 'bg-blue-500 text-white hover:bg-blue-600 shadow-glow'
                : 'bg-gpt-border text-gpt-dim cursor-not-allowed'"
            >
              <Send :size="20" />
            </button>
          </div>
          <p class="text-xs text-gpt-dim text-center mt-2">
            {{ rateLimited ? '⏳ 频率限制，每分钟最多提问 5 次' : '基于知识库内容的 AI 问答' }}
          </p>
        </div>
      </div>
    </template>
  </div>
</template>
