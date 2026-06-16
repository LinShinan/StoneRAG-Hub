<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { kbApi } from '@/api/knowledgeBases'
import { docApi } from '@/api/documents'
import { ApiError } from '@/api/client'
import AppLayout from '@/components/layout/AppLayout.vue'
import LoadingSpinner from '@/components/ui/LoadingSpinner.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import Modal from '@/components/ui/Modal.vue'
import {
  formatFileSize,
  formatCharCount,
  formatDate,
  formatDateTime,
  getFileIcon,
  getStatusInfo,
  getFileTypeColor,
} from '@/utils/format'
import type { KnowledgeBase, KnowledgeBaseStats, Document as Doc, DocumentDetail } from '@/types'
import {
  ArrowLeft,
  Upload,
  FileText,
  Trash2,
  RefreshCw,
  Link,
  ChevronDown,
  ChevronUp,
  Eye,
  AlertCircle,
  BarChart3,
  PieChart,
  BookOpen,
} from 'lucide-vue-next'

const route = useRoute()
const router = useRouter()
const app = useAppStore()

const kbId = computed(() => Number(route.params.id))

// ── State ──
const kb = ref<KnowledgeBase | null>(null)
const stats = ref<KnowledgeBaseStats | null>(null)
const documents = ref<Doc[]>([])
const loading = ref(true)
const docsLoading = ref(false)
const uploading = ref(false)

// Document detail preview
const previewDoc = ref<DocumentDetail | null>(null)
const previewLoading = ref(false)
const showPreview = ref(false)

// Import URL modal
const showImportModal = ref(false)
const importUrl = ref('')
const importLoading = ref(false)

// Delete document confirmation
const showDeleteDocModal = ref(false)
const deletingDoc = ref<Doc | null>(null)
const deleteDocLoading = ref(false)

// File input ref
const fileInput = ref<HTMLInputElement | null>(null)

// Polling for document status
let pollTimer: ReturnType<typeof setInterval> | null = null

// ── Data Fetching ──
async function fetchAll() {
  loading.value = true
  try {
    const [kbRes, statsRes, docsRes] = await Promise.all([
      kbApi.get(kbId.value),
      kbApi.stats(kbId.value),
      docApi.list(kbId.value, { size: 50 }),
    ])
    kb.value = kbRes.data as KnowledgeBase
    stats.value = statsRes.data as KnowledgeBaseStats
    documents.value = docsRes.data.items
  } catch (e) {
    app.showToast('加载知识库信息失败', 'error')
  } finally {
    loading.value = false
  }
}

async function fetchDocuments() {
  docsLoading.value = true
  try {
    const res = await docApi.list(kbId.value, { size: 50 })
    documents.value = res.data.items
  } catch {
    // Silently fail
  } finally {
    docsLoading.value = false
  }
}

// ── Upload ──
function triggerUpload() {
  fileInput.value?.click()
}

async function handleFileSelect(e: Event) {
  const target = e.target as HTMLInputElement
  const file = target.files?.[0]
  if (!file) return

  // Validate
  const allowedTypes = ['application/pdf', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', 'text/markdown', 'text/plain']
  const allowedExts = ['.pdf', '.docx', '.md', '.txt']
  const ext = '.' + file.name.split('.').pop()?.toLowerCase()

  if (!allowedExts.includes(ext)) {
    app.showToast('不支持的文件格式，仅支持 PDF、DOCX、MD、TXT', 'warning')
    return
  }
  if (file.size > 50 * 1024 * 1024) {
    app.showToast('文件不能超过 50MB', 'warning')
    return
  }

  uploading.value = true
  try {
    await docApi.upload(kbId.value, file)
    app.showToast('文档上传成功，正在解析中...', 'success')
    await fetchDocuments()
    startPolling()
  } catch (e) {
    const msg = e instanceof ApiError ? e.message : '上传失败'
    app.showToast(msg, 'error')
  } finally {
    uploading.value = false
    // Reset file input
    if (fileInput.value) fileInput.value.value = ''
  }
}

// ── Polling ──
function startPolling() {
  stopPolling()
  pollTimer = setInterval(async () => {
    const hasProcessing = documents.value.some(
      (d) => d.status === 'uploading' || d.status === 'parsing' || d.status === 'embedding',
    )
    if (!hasProcessing) {
      stopPolling()
      return
    }
    await fetchDocuments()
    // Also update stats
    try {
      const res = await kbApi.stats(kbId.value)
      stats.value = res.data as KnowledgeBaseStats
    } catch {}
  }, 2000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

// ── Document Actions ──
async function viewDocumentPreview(doc: Doc) {
  previewLoading.value = true
  showPreview.value = true
  try {
    const res = await docApi.get(doc.id)
    previewDoc.value = res.data as DocumentDetail
  } catch {
    app.showToast('加载文档详情失败', 'error')
    showPreview.value = false
  } finally {
    previewLoading.value = false
  }
}

function openDeleteDocModal(doc: Doc) {
  deletingDoc.value = doc
  showDeleteDocModal.value = true
}

async function handleDeleteDoc() {
  if (!deletingDoc.value) return
  deleteDocLoading.value = true
  try {
    await docApi.delete(deletingDoc.value.id)
    showDeleteDocModal.value = false
    app.showToast('文档已删除', 'success')
    await fetchDocuments()
    // Refresh stats
    const res = await kbApi.stats(kbId.value)
    stats.value = res.data as KnowledgeBaseStats
  } catch (e) {
    const msg = e instanceof ApiError ? e.message : '删除失败'
    app.showToast(msg, 'error')
  } finally {
    deleteDocLoading.value = false
  }
}

async function reprocessDoc(doc: Doc) {
  try {
    await docApi.reprocess(doc.id)
    app.showToast('已提交重新处理', 'success')
    await fetchDocuments()
    startPolling()
  } catch (e) {
    const msg = e instanceof ApiError ? e.message : '重新处理失败'
    app.showToast(msg, 'error')
  }
}

// ── Import URL ──
async function handleImportUrl() {
  if (!importUrl.value.trim()) return
  importLoading.value = true
  try {
    await docApi.importUrl(kbId.value, importUrl.value.trim())
    showImportModal.value = false
    importUrl.value = ''
    app.showToast('网页导入成功，正在解析...', 'success')
    await fetchDocuments()
    startPolling()
  } catch (e) {
    const msg = e instanceof ApiError ? e.message : '导入失败'
    app.showToast(msg, 'error')
  } finally {
    importLoading.value = false
  }
}

// ── Pie Chart (simple CSS) ──
const fileTypeEntries = computed(() => {
  if (!stats.value?.file_type_dist) return []
  return Object.entries(stats.value.file_type_dist).map(([type, count]) => ({
    type,
    count,
    color: getFileTypeColor(type),
    label: type.toUpperCase(),
  }))
})

const totalDocsForChart = computed(() =>
  fileTypeEntries.value.reduce((sum, e) => sum + e.count, 0),
)

function pieConicGradient(): string {
  if (fileTypeEntries.value.length === 0) return 'conic-gradient(#30363D 0%, #30363D 100%)'
  let cumulative = 0
  const segments = fileTypeEntries.value.map((entry) => {
    const pct = (entry.count / totalDocsForChart.value) * 100
    const start = cumulative
    cumulative += pct
    return `${entry.color} ${start}% ${cumulative}%`
  })
  return `conic-gradient(${segments.join(', ')})`
}

// ── Lifecycle ──
onMounted(() => {
  fetchAll()
  // Check if there are processing docs
  if (documents.value.some((d) => d.status !== 'ready' && d.status !== 'error')) {
    startPolling()
  }
})

onUnmounted(() => {
  stopPolling()
})
</script>

<template>
  <AppLayout>
    <!-- Back & Header -->
    <div class="flex items-center gap-4 mb-6">
      <button
        @click="router.push('/dashboard')"
        class="p-2 rounded-xl hover:bg-white/5 transition-colors text-gpt-muted hover:text-gpt-text"
      >
        <ArrowLeft :size="20" />
      </button>
      <div v-if="kb" class="flex-1">
        <h1 class="text-2xl font-bold text-gpt-text">{{ kb.name }}</h1>
        <p v-if="kb.description" class="text-sm text-gpt-muted mt-0.5">{{ kb.description }}</p>
      </div>
    </div>

    <LoadingSpinner v-if="loading" text="加载知识库信息中..." />

    <template v-else-if="kb">
      <div class="flex gap-6">
        <!-- ─── Left: Document List ─── -->
        <div class="flex-1 min-w-0">
          <!-- Actions bar -->
          <div class="flex items-center gap-3 mb-4">
            <input
              ref="fileInput"
              type="file"
              accept=".pdf,.docx,.md,.txt"
              class="hidden"
              @change="handleFileSelect"
            />
            <button
              @click="triggerUpload"
              :disabled="uploading"
              class="flex items-center gap-2 px-4 py-2.5 bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 text-white font-medium rounded-xl transition-all text-sm shadow-glow disabled:opacity-50"
            >
              <Upload :size="16" />
              {{ uploading ? '上传中...' : '上传文件' }}
            </button>
            <button
              @click="showImportModal = true"
              class="flex items-center gap-2 px-4 py-2.5 border border-gpt-border rounded-xl text-sm text-gpt-muted hover:text-gpt-text hover:bg-white/5 transition-all"
            >
              <Link :size="16" />
              导入网页
            </button>
            <span class="text-xs text-gpt-dim ml-auto">
              支持 PDF、DOCX、MD、TXT，最大 50MB
            </span>
          </div>

          <!-- Document List -->
          <LoadingSpinner v-if="docsLoading" size="sm" />
          <EmptyState
            v-else-if="documents.length === 0"
            icon="📄"
            title="还没有文档"
            description="上传文件或导入网页，开始构建你的知识库"
          />
          <div v-else class="space-y-2">
            <div
              v-for="doc in documents"
              :key="doc.id"
              class="flex items-center gap-4 p-4 rounded-xl border border-gpt-border hover:border-blue-500/20 hover:bg-white/[0.02] transition-all group cursor-pointer"
              @click="doc.status === 'ready' ? viewDocumentPreview(doc) : null"
            >
              <!-- Status indicator -->
              <div class="relative shrink-0">
                <div
                  v-if="doc.status === 'ready'"
                  class="w-10 h-10 rounded-xl bg-emerald-500/10 flex items-center justify-center text-xl"
                >
                  {{ getFileIcon(doc.file_type) }}
                </div>
                <div
                  v-else-if="doc.status === 'error'"
                  class="w-10 h-10 rounded-xl bg-red-500/10 flex items-center justify-center"
                >
                  <AlertCircle :size="20" class="text-red-400" />
                </div>
                <div
                  v-else
                  class="w-10 h-10 rounded-xl bg-blue-500/10 flex items-center justify-center"
                >
                  <div class="w-5 h-5 border-2 border-blue-400/30 border-t-blue-400 rounded-full animate-spin" />
                </div>
              </div>

              <!-- Info -->
              <div class="flex-1 min-w-0">
                <div class="flex items-center gap-2">
                  <h4 class="text-sm font-medium text-gpt-text truncate">{{ doc.title }}</h4>
                  <span
                    :class="[getStatusInfo(doc.status).bg, getStatusInfo(doc.status).color]"
                    class="px-2 py-0.5 rounded-md text-xs font-medium shrink-0"
                  >
                    {{ getStatusInfo(doc.status).label }}
                  </span>
                </div>
                <div class="flex items-center gap-3 mt-1 text-xs text-gpt-dim">
                  <span>{{ doc.file_type.toUpperCase() }}</span>
                  <span>{{ formatFileSize(doc.file_size) }}</span>
                  <span v-if="doc.status === 'ready'">{{ doc.chunk_count }} 切片</span>
                  <span>{{ formatDate(doc.created_at) }}</span>
                </div>
                <!-- Error message -->
                <p
                  v-if="doc.status === 'error' && doc.error_msg"
                  class="text-xs text-red-400 mt-1 truncate"
                >
                  {{ doc.error_msg }}
                </p>
              </div>

              <!-- Actions -->
              <div class="flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity shrink-0">
                <button
                  v-if="doc.status === 'error'"
                  @click.stop="reprocessDoc(doc)"
                  class="p-1.5 rounded-lg hover:bg-white/5 text-gpt-muted hover:text-blue-400 transition-colors"
                  title="重新处理"
                >
                  <RefreshCw :size="16" />
                </button>
                <button
                  v-if="doc.status === 'ready'"
                  @click.stop="viewDocumentPreview(doc)"
                  class="p-1.5 rounded-lg hover:bg-white/5 text-gpt-muted hover:text-gpt-accent transition-colors"
                  title="查看预览"
                >
                  <Eye :size="16" />
                </button>
                <button
                  @click.stop="openDeleteDocModal(doc)"
                  class="p-1.5 rounded-lg hover:bg-red-500/10 text-gpt-muted hover:text-red-400 transition-colors"
                  title="删除"
                >
                  <Trash2 :size="16" />
                </button>
              </div>
            </div>
          </div>

          <!-- Document Preview Panel (expandable) -->
          <transition name="slide">
            <div
              v-if="showPreview && previewDoc"
              class="mt-4 glass-card p-6 animate-slide-down"
            >
              <div class="flex items-center justify-between mb-4">
                <h3 class="font-semibold text-gpt-text flex items-center gap-2">
                  <Eye :size="18" />
                  文档预览：{{ previewDoc.title }}
                </h3>
                <button
                  @click="showPreview = false"
                  class="p-1.5 rounded-lg hover:bg-white/5 text-gpt-muted"
                >
                  ✕
                </button>
              </div>
              <LoadingSpinner v-if="previewLoading" size="sm" text="加载预览中..." />
              <template v-else>
                <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
                  <div
                    v-for="chunk in previewDoc.chunks_preview"
                    :key="chunk.index"
                    class="p-4 rounded-xl border border-gpt-border bg-gpt-bg/50 hover:border-blue-500/20 transition-colors"
                  >
                    <div class="flex items-center justify-between mb-2">
                      <span class="text-xs font-medium text-gpt-accent">切片 #{{ chunk.index + 1 }}</span>
                      <span class="text-xs text-gpt-dim">{{ chunk.char_count }} 字</span>
                    </div>
                    <p class="text-sm text-gpt-muted leading-relaxed line-clamp-4">
                      {{ chunk.content_text }}
                    </p>
                  </div>
                </div>
              </template>
            </div>
          </transition>
        </div>

        <!-- ─── Right: Stats Panel ─── -->
        <div class="w-80 shrink-0 space-y-4">
          <!-- Summary card -->
          <div class="glass-card p-5">
            <h3 class="text-sm font-semibold text-gpt-text mb-4 flex items-center gap-2">
              <BarChart3 :size="16" class="text-blue-400" />
              知识库概览
            </h3>

            <div class="space-y-4">
              <div class="flex items-center justify-between">
                <span class="text-sm text-gpt-muted">文档总数</span>
                <span class="text-lg font-bold text-gpt-text">{{ stats?.doc_count || 0 }}</span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-sm text-gpt-muted">切片总数</span>
                <span class="text-lg font-bold text-gpt-text">{{ formatCharCount(stats?.chunk_count || 0) }}</span>
              </div>
              <div class="flex items-center justify-between">
                <span class="text-sm text-gpt-muted">总字符数</span>
                <span class="text-lg font-bold text-gpt-text">{{ formatCharCount(stats?.total_chars || 0) }}</span>
              </div>
            </div>
          </div>

          <!-- File type distribution -->
          <div class="glass-card p-5" v-if="fileTypeEntries.length > 0">
            <h3 class="text-sm font-semibold text-gpt-text mb-4 flex items-center gap-2">
              <PieChart :size="16" class="text-emerald-400" />
              文件类型分布
            </h3>

            <!-- CSS Pie Chart -->
            <div class="flex items-center gap-4 mb-4">
              <div
                class="w-24 h-24 rounded-full shrink-0"
                :style="{ background: pieConicGradient() }"
              />
              <div class="space-y-2 flex-1">
                <div
                  v-for="entry in fileTypeEntries"
                  :key="entry.type"
                  class="flex items-center gap-2 text-xs"
                >
                  <span
                    class="w-3 h-3 rounded-sm shrink-0"
                    :style="{ backgroundColor: entry.color }"
                  />
                  <span class="text-gpt-muted">{{ entry.label }}</span>
                  <span class="text-gpt-text font-medium ml-auto">{{ entry.count }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- Recent docs -->
          <div class="glass-card p-5" v-if="stats?.recent_docs?.length">
            <h3 class="text-sm font-semibold text-gpt-text mb-3 flex items-center gap-2">
              <BookOpen :size="16" class="text-amber-400" />
              最近文档
            </h3>
            <div class="space-y-2">
              <div
                v-for="doc in stats.recent_docs.slice(0, 5)"
                :key="doc.id"
                class="flex items-center gap-2 text-sm"
              >
                <span class="text-xs">{{ getFileIcon('md') }}</span>
                <span class="text-gpt-text truncate flex-1">{{ doc.title }}</span>
                <span class="text-xs text-gpt-dim shrink-0">{{ formatDate(doc.created_at) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- ─── Import URL Modal ─── -->
    <Modal v-model:open="showImportModal" title="导入网页" max-width="max-w-md">
      <div class="space-y-4">
        <div class="space-y-1.5">
          <label class="text-sm font-medium text-gpt-text">网页 URL</label>
          <input
            v-model="importUrl"
            type="url"
            placeholder="https://example.com/article"
            class="w-full px-4 py-2.5 bg-gpt-bg border border-gpt-border rounded-xl text-sm text-gpt-text placeholder:text-gpt-dim focus:outline-none focus:border-blue-500/50 transition-all"
          />
        </div>
        <button
          @click="handleImportUrl"
          :disabled="importLoading || !importUrl.trim()"
          class="w-full py-2.5 bg-gradient-to-r from-blue-500 to-blue-600 text-white font-medium rounded-xl text-sm transition-all disabled:opacity-50"
        >
          {{ importLoading ? '导入中...' : '确认导入' }}
        </button>
      </div>
    </Modal>

    <!-- ─── Delete Document Modal ─── -->
    <Modal v-model:open="showDeleteDocModal" title="删除文档" max-width="max-w-sm">
      <p class="text-sm text-gpt-muted mb-1">
        确定要删除文档「<span class="text-gpt-text font-medium">{{ deletingDoc?.title }}</span>」吗？
      </p>
      <p class="text-xs text-red-400 mb-5">
        ⚠️ 此操作将同时删除所有分段及向量数据。
      </p>
      <div class="flex gap-3">
        <button
          @click="showDeleteDocModal = false"
          class="flex-1 py-2.5 border border-gpt-border rounded-xl text-sm text-gpt-muted hover:text-gpt-text transition-all"
        >
          取消
        </button>
        <button
          @click="handleDeleteDoc"
          :disabled="deleteDocLoading"
          class="flex-1 py-2.5 bg-red-500/10 hover:bg-red-500/20 border border-red-500/30 rounded-xl text-sm text-red-400 font-medium transition-all disabled:opacity-50"
        >
          {{ deleteDocLoading ? '删除中...' : '确认删除' }}
        </button>
      </div>
    </Modal>
  </AppLayout>
</template>
