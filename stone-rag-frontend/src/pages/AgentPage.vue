<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick, computed } from 'vue'
import { useAppStore } from '@/stores/app'
import { kbApi } from '@/api/knowledgeBases'
import { agentApi } from '@/api/agent'
import { createSSEParser, parseSSEData } from '@/utils/sse'
import AppLayout from '@/components/layout/AppLayout.vue'
import LoadingSpinner from '@/components/ui/LoadingSpinner.vue'
import type { KnowledgeBase, SSEAgentStepData, SSEAgentDoneData } from '@/types'
import {
  Bot,
  Send,
  StopCircle,
  Brain,
  Search,
  FileText,
  CheckCircle2,
  ChevronRight,
  Lightbulb,
  Wrench,
  FileOutput,
} from 'lucide-vue-next'

const app = useAppStore()

interface AgentStep {
  type: 'thought' | 'tool_call' | 'tool_result' | 'generate'
  content: string
  tool?: string
  toolArgs?: Record<string, any>
  tokenBuffer?: string
}

const knowledgeBases = ref<KnowledgeBase[]>([])
const selectedKbIds = ref<number[]>([])
const taskInput = ref('')
const isRunning = ref(false)
const steps = ref<AgentStep[]>([])
const finalOutput = ref('')
const totalSteps = ref(0)
const totalTokens = ref(0)
const abortController = ref<AbortController | null>(null)
const outputRef = ref<HTMLElement | null>(null)

const canRun = computed(() => {
  return selectedKbIds.value.length > 0 && taskInput.value.trim() && !isRunning.value
})

async function fetchKnowledgeBases() {
  try {
    const res = await kbApi.list({ size: 50 })
    knowledgeBases.value = res.data.items
  } catch {}
}

function toggleKb(id: number) {
  const idx = selectedKbIds.value.indexOf(id)
  if (idx >= 0) {
    selectedKbIds.value.splice(idx, 1)
  } else {
    selectedKbIds.value.push(id)
  }
}

function getStepIcon(step: AgentStep) {
  switch (step.type) {
    case 'thought': return Lightbulb
    case 'tool_call': return Wrench
    case 'tool_result': return FileOutput
    case 'generate': return FileText
  }
}

function getStepColor(step: AgentStep) {
  switch (step.type) {
    case 'thought': return 'text-amber-400'
    case 'tool_call': return 'text-blue-400'
    case 'tool_result': return 'text-emerald-400'
    case 'generate': return 'text-purple-400'
  }
}

function getStepLabel(step: AgentStep) {
  switch (step.type) {
    case 'thought': return '思考'
    case 'tool_call':
      return `调用工具: ${step.tool || ''}`
    case 'tool_result': return '工具返回'
    case 'generate': return '生成'
  }
}

async function runTask() {
  if (!canRun.value) return

  isRunning.value = true
  steps.value = []
  finalOutput.value = ''
  totalSteps.value = 0
  totalTokens.value = 0

  const controller = new AbortController()
  abortController.value = controller

  try {
    const response = await agentApi.run(
      {
        kb_ids: selectedKbIds.value,
        task: taskInput.value.trim(),
      },
      controller.signal,
    )

    if (!response.ok) {
      const err = await response.json().catch(() => ({ message: 'Agent 任务失败' }))
      throw new Error(err.message || `HTTP ${response.status}`)
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
        if (evt.event === 'agent_step') {
          const data = parseSSEData<SSEAgentStepData>(evt)
          if (!data) continue

          if (data.step === 'thought') {
            steps.value.push({ type: 'thought', content: data.content || '' })
          } else if (data.step === 'tool_call') {
            steps.value.push({
              type: 'tool_call',
              content: `调用 ${data.tool}`,
              tool: data.tool,
              toolArgs: data.args,
            })
          } else if (data.step === 'tool_result') {
            steps.value.push({ type: 'tool_result', content: data.output || '' })
          } else if (data.step === 'generate') {
            // Accumulate tokens
            const lastStep = steps.value[steps.value.length - 1]
            if (lastStep && lastStep.type === 'generate') {
              lastStep.content += data.token || ''
              finalOutput.value += data.token || ''
            } else {
              steps.value.push({ type: 'generate', content: data.token || '' })
              finalOutput.value += data.token || ''
            }
          }
        } else if (evt.event === 'agent_done') {
          const data = parseSSEData<SSEAgentDoneData>(evt)
          if (data) {
            totalSteps.value = data.total_steps
            totalTokens.value = data.total_tokens
          }
        }
      }

      // Auto-scroll
      nextTick(() => {
        if (outputRef.value) {
          outputRef.value.scrollTop = outputRef.value.scrollHeight
        }
      })
    }
  } catch (err: any) {
    if (err.name !== 'AbortError') {
      app.showToast(err.message || 'Agent 执行失败', 'error')
    }
  } finally {
    isRunning.value = false
    abortController.value = null
  }
}

function cancelTask() {
  if (abortController.value) {
    abortController.value.abort()
    abortController.value = null
  }
  isRunning.value = false
}

function handleKeydown(e: KeyboardEvent) {
  if (e.key === 'Enter' && !e.shiftKey) {
    e.preventDefault()
    runTask()
  }
}

onMounted(() => {
  fetchKnowledgeBases()
})
</script>

<template>
  <AppLayout>
    <div class="flex h-[calc(100vh-3rem)] -my-6 -mx-6">
      <!-- Left: Task input & KB selection -->
      <div class="w-[380px] border-r border-gpt-border bg-gpt-surface/30 p-6 flex flex-col shrink-0">
        <h1 class="text-xl font-bold text-gpt-text mb-6 flex items-center gap-2">
          <Bot :size="24" class="text-blue-400" />
          Agent 任务
        </h1>

        <!-- KB Selection -->
        <div class="mb-5">
          <label class="text-sm font-medium text-gpt-text mb-2 block">选择知识库</label>
          <div class="space-y-1.5 max-h-48 overflow-y-auto">
            <button
              v-for="kb in knowledgeBases"
              :key="kb.id"
              @click="toggleKb(kb.id)"
              :class="selectedKbIds.includes(kb.id)
                ? 'border-blue-500/50 bg-blue-500/10 text-blue-400'
                : 'border-gpt-border text-gpt-muted hover:text-gpt-text'"
              class="w-full flex items-center gap-2 px-3 py-2 rounded-xl border text-sm transition-all"
            >
              <span>{{ kb.icon || '📁' }}</span>
              <span class="flex-1 text-left truncate">{{ kb.name }}</span>
              <CheckCircle2
                v-if="selectedKbIds.includes(kb.id)"
                :size="16"
                class="text-blue-400 shrink-0"
              />
            </button>
          </div>
          <p v-if="knowledgeBases.length === 0" class="text-xs text-gpt-dim mt-2">
            暂无知识库，请先创建知识库
          </p>
        </div>

        <!-- Task Input -->
        <div class="flex-1 flex flex-col">
          <label class="text-sm font-medium text-gpt-text mb-2 block">任务描述</label>
          <textarea
            v-model="taskInput"
            @keydown="handleKeydown"
            :disabled="isRunning"
            rows="4"
            placeholder="描述你想让 Agent 完成的任务，例如：&#10;&#10;根据技术文档，帮我写一份微服务架构方案大纲"
            class="flex-1 w-full bg-gpt-bg border border-gpt-border rounded-xl p-4 text-sm text-gpt-text placeholder:text-gpt-dim focus:outline-none focus:border-blue-500/50 transition-all resize-none"
          />
          <button
            v-if="isRunning"
            @click="cancelTask"
            class="w-full mt-3 py-2.5 bg-red-500/10 hover:bg-red-500/20 border border-red-500/30 rounded-xl text-sm text-red-400 font-medium transition-all flex items-center justify-center gap-2"
          >
            <StopCircle :size="18" />
            停止执行
          </button>
          <button
            v-else
            @click="runTask"
            :disabled="!canRun"
            class="w-full mt-3 py-2.5 bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 text-white font-medium rounded-xl text-sm transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2 shadow-glow"
          >
            <Send :size="16" />
            执行任务
          </button>
        </div>
      </div>

      <!-- Right: Execution output -->
      <div class="flex-1 flex flex-col min-w-0">
        <!-- Empty state -->
        <div
          v-if="steps.length === 0 && !isRunning"
          class="flex-1 flex flex-col items-center justify-center text-center p-8"
        >
          <div class="w-24 h-24 rounded-full bg-gradient-to-br from-blue-500/10 to-purple-500/10 flex items-center justify-center mb-6">
            <Brain :size="48" class="text-blue-400/60" />
          </div>
          <h2 class="text-xl font-bold text-gpt-text mb-2">AI Agent 工作台</h2>
          <p class="text-sm text-gpt-muted max-w-lg">
            Agent 可以自动调用知识库检索、文档总结、大纲生成等工具，
            完成复杂的多步骤任务。选择一个或多个知识库，描述你的需求，Agent 会逐步执行并展示过程。
          </p>

          <!-- Available tools -->
          <div class="grid grid-cols-2 gap-3 mt-8 max-w-md">
            <div class="p-4 rounded-xl border border-gpt-border bg-gpt-surface/30">
              <Search :size="20" class="text-blue-400 mb-2" />
              <h4 class="text-sm font-medium text-gpt-text">知识库检索</h4>
              <p class="text-xs text-gpt-dim mt-1">在知识库中搜索相关内容</p>
            </div>
            <div class="p-4 rounded-xl border border-gpt-border bg-gpt-surface/30">
              <FileText :size="20" class="text-emerald-400 mb-2" />
              <h4 class="text-sm font-medium text-gpt-text">文档总结</h4>
              <p class="text-xs text-gpt-dim mt-1">总结指定文档的核心内容</p>
            </div>
            <div class="p-4 rounded-xl border border-gpt-border bg-gpt-surface/30">
              <FileText :size="20" class="text-amber-400 mb-2" />
              <h4 class="text-sm font-medium text-gpt-text">文档对比</h4>
              <p class="text-xs text-gpt-dim mt-1">对比多篇文档的异同点</p>
            </div>
            <div class="p-4 rounded-xl border border-gpt-border bg-gpt-surface/30">
              <FileOutput :size="20" class="text-purple-400 mb-2" />
              <h4 class="text-sm font-medium text-gpt-text">大纲生成</h4>
              <p class="text-xs text-gpt-dim mt-1">根据素材生成结构化大纲</p>
            </div>
          </div>
        </div>

        <!-- Execution output -->
        <div
          v-else
          ref="outputRef"
          class="flex-1 overflow-y-auto p-6 space-y-4"
        >
          <!-- Steps -->
          <div v-for="(step, idx) in steps" :key="idx" class="animate-fade-in">
            <!-- Thought / Tool call / Tool result -->
            <div
              v-if="step.type !== 'generate'"
              class="flex gap-3"
            >
              <div
                :class="{
                  'bg-amber-500/10': step.type === 'thought',
                  'bg-blue-500/10': step.type === 'tool_call',
                  'bg-emerald-500/10': step.type === 'tool_result',
                }"
                class="p-3 rounded-xl max-w-[80%]"
              >
                <div class="flex items-center gap-2 mb-1">
                  <component :is="getStepIcon(step)" :size="14" :class="getStepColor(step)" />
                  <span :class="getStepColor(step)" class="text-xs font-medium">
                    {{ getStepLabel(step) }}
                  </span>
                </div>
                <p class="text-sm text-gpt-muted leading-relaxed whitespace-pre-wrap">
                  {{ step.content }}
                </p>
              </div>
            </div>

            <!-- Generate output -->
            <div v-else class="p-5 rounded-xl border border-gpt-border bg-gpt-surface/50">
              <h4 class="text-sm font-semibold text-gpt-text mb-3 flex items-center gap-2">
                <FileOutput :size="16" class="text-purple-400" />
                生成结果
              </h4>
              <div
                class="text-sm leading-relaxed text-gpt-text whitespace-pre-wrap message-content"
                :class="{ 'typing-cursor': isRunning }"
              >
                {{ step.content }}
              </div>
            </div>
          </div>

          <!-- Loading indicator -->
          <div v-if="isRunning && steps.length > 0" class="flex items-center gap-2 text-sm text-gpt-muted py-2">
            <div class="thinking-ripple">
              <span class="dot" />
              <span class="dot" />
              <span class="dot" />
            </div>
            <span>Agent 正在工作中...</span>
          </div>

          <!-- Done summary -->
          <div
            v-if="!isRunning && totalTokens > 0"
            class="mt-4 pt-4 border-t border-gpt-border flex items-center gap-4 text-xs text-gpt-muted"
          >
            <span>共 {{ totalSteps }} 步操作</span>
            <span>{{ totalTokens }} tokens</span>
          </div>
        </div>
      </div>
    </div>
  </AppLayout>
</template>
