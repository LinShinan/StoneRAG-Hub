<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAppStore } from '@/stores/app'
import { kbApi } from '@/api/knowledgeBases'
import { ApiError } from '@/api/client'
import AppLayout from '@/components/layout/AppLayout.vue'
import Modal from '@/components/ui/Modal.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import LoadingSpinner from '@/components/ui/LoadingSpinner.vue'
import { formatNumber, formatCharCount, formatDate } from '@/utils/format'
import type { KnowledgeBase, CreateKBPayload } from '@/types'
import {
  Plus,
  Search,
  BookOpen,
  FileText,
  Hash,
  Layers,
  MoreHorizontal,
  Edit3,
  Trash2,
  FolderOpen,
  ChevronLeft,
  ChevronRight,
} from 'lucide-vue-next'
import { reactive } from 'vue'

const router = useRouter()
const app = useAppStore()

const knowledgeBases = ref<KnowledgeBase[]>([])
const loading = ref(true)
const searchKeyword = ref('')
const total = ref(0)
const page = ref(1)
const pageSize = ref(9)

// Create KB modal
const showCreateModal = ref(false)
const createLoading = ref(false)
// ── Icon picker ──
const iconOptions = ['📁', '📚', '📝', '💡', '🔬', '📊', '🗂️', '💼', '🎯', '🚀', '⚙️', '🔒', '🌐', '📖', '🏗️', '🤖']
const showIconPicker = ref(false)
const showEditIconPicker = ref(false)

const createForm = reactive<CreateKBPayload>({
  name: '',
  description: '',
  icon: '📁',
  embedding_model: 'text-embedding-3-small',
  chunk_size: 500,
  chunk_overlap: 50,
})

// Edit KB modal
const showEditModal = ref(false)
const editLoading = ref(false)
const editingKB = ref<KnowledgeBase | null>(null)
const editForm = reactive({ name: '', description: '', icon: '📁' })

// Delete KB confirmation
const showDeleteModal = ref(false)
const deletingKB = ref<KnowledgeBase | null>(null)
const deleteLoading = ref(false)

// Context menu
const contextMenuKB = ref<number | null>(null)

async function fetchKnowledgeBases() {
  loading.value = true
  try {
    // AbortController for timeout — prevents infinite loading
    const ctrl = new AbortController()
    const timer = setTimeout(() => ctrl.abort(), 15000)

    const res = await kbApi.list({
      page: page.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
    }, ctrl.signal)

    clearTimeout(timer)
    knowledgeBases.value = res.data.items || []
    total.value = res.data.total || 0
  } catch (e: any) {
    if (e instanceof ApiError && e.code === 40002) return
    if (e?.name === 'AbortError') {
      app.showToast('请求超时，请检查后端是否启动', 'warning')
    } else {
      app.showToast('加载知识库失败：' + (e.message || '未知错误'), 'error')
    }
  } finally {
    loading.value = false
  }
}

function openKnowledgeBase(kb: KnowledgeBase) {
  router.push(`/kb/${kb.id}`)
}

// ── Create KB ──
function openCreateModal() {
  createForm.name = ''
  createForm.description = ''
  createForm.icon = '📁'
  showCreateModal.value = true
}

async function handleCreate() {
  if (!createForm.name.trim()) return
  createLoading.value = true
  try {
    await kbApi.create({
      name: createForm.name.trim(),
      description: createForm.description?.trim() || undefined,
      icon: createForm.icon,
      embedding_model: createForm.embedding_model,
      chunk_size: createForm.chunk_size,
      chunk_overlap: createForm.chunk_overlap,
    })
    showCreateModal.value = false
    app.showToast('知识库创建成功！', 'success')
    await fetchKnowledgeBases()
  } catch (e) {
    const msg = e instanceof ApiError ? e.message : '创建失败'
    app.showToast(msg, 'error')
  } finally {
    createLoading.value = false
  }
}

// ── Edit KB ──
function openEditModal(kb: KnowledgeBase) {
  editingKB.value = kb
  editForm.name = kb.name
  editForm.description = kb.description || ''
  editForm.icon = kb.icon || '📁'
  showEditModal.value = true
  contextMenuKB.value = null
}

async function handleEdit() {
  if (!editForm.name.trim() || !editingKB.value) return
  editLoading.value = true
  try {
    await kbApi.update(editingKB.value.id, {
      name: editForm.name.trim(),
      description: editForm.description.trim(),
      icon: editForm.icon,
    })
    showEditModal.value = false
    app.showToast('知识库已更新', 'success')
    await fetchKnowledgeBases()
  } catch (e) {
    const msg = e instanceof ApiError ? e.message : '更新失败'
    app.showToast(msg, 'error')
  } finally {
    editLoading.value = false
  }
}

// ── Delete KB ──
function openDeleteModal(kb: KnowledgeBase) {
  deletingKB.value = kb
  showDeleteModal.value = true
  contextMenuKB.value = null
}

async function handleDelete() {
  if (!deletingKB.value) return
  deleteLoading.value = true
  try {
    await kbApi.delete(deletingKB.value.id)
    showDeleteModal.value = false
    app.showToast('知识库已删除', 'success')
    await fetchKnowledgeBases()
  } catch (e) {
    const msg = e instanceof ApiError ? e.message : '删除失败'
    app.showToast(msg, 'error')
  } finally {
    deleteLoading.value = false
  }
}

// ── Pagination ──
function changePage(p: number) {
  page.value = p
  fetchKnowledgeBases()
}

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

// ── Search ──
let searchTimer: ReturnType<typeof setTimeout>
function onSearchInput() {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    page.value = 1 // reset to first page on new search
    fetchKnowledgeBases()
  }, 300)
}

onMounted(() => {
  fetchKnowledgeBases()
})
</script>

<template>
  <AppLayout>
    <!-- Header -->
    <div class="flex items-center justify-between mb-8">
      <div>
        <h1 class="text-2xl font-bold text-gpt-text">我的知识库</h1>
        <p class="text-sm text-gpt-muted mt-1">共 {{ total }} 个知识库</p>
      </div>
      <div class="flex items-center gap-3">
        <!-- Search -->
        <div class="relative">
          <Search :size="16" class="absolute left-3 top-1/2 -translate-y-1/2 text-gpt-dim" />
          <input
            v-model="searchKeyword"
            @input="onSearchInput"
            type="text"
            placeholder="搜索知识库..."
            class="w-64 pl-9 pr-4 py-2.5 bg-gpt-surface border border-gpt-border rounded-xl text-sm text-gpt-text placeholder:text-gpt-dim focus:outline-none focus:border-blue-500/50 transition-all"
          />
        </div>
        <button
          @click="openCreateModal"
          class="flex items-center gap-2 px-4 py-2.5 bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 text-white font-medium rounded-xl transition-all shadow-glow text-sm"
        >
          <Plus :size="18" />
          新建知识库
        </button>
      </div>
    </div>

    <!-- Loading -->
    <LoadingSpinner v-if="loading" text="加载知识库中..." />

    <!-- Empty -->
    <EmptyState
      v-else-if="knowledgeBases.length === 0"
      icon="📁"
      title="还没有知识库"
      description="点击右上角的「新建知识库」按钮，上传你的第一个知识库文档吧"
    >
      <button
        @click="openCreateModal"
        class="mt-4 inline-flex items-center gap-2 px-5 py-2.5 bg-gradient-to-r from-blue-500 to-blue-600 text-white rounded-xl font-medium text-sm hover:shadow-glow transition-all"
      >
        <Plus :size="18" />
        创建第一个知识库
      </button>
    </EmptyState>

    <!-- KB Grid -->
    <div v-else class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      <div
        v-for="kb in knowledgeBases"
        :key="kb.id"
        @click="openKnowledgeBase(kb)"
        class="group glass-card p-5 cursor-pointer hover:border-blue-500/20 hover:shadow-glow transition-all duration-300 relative animate-fade-in"
      >
        <!-- Card Header -->
        <div class="flex items-start justify-between mb-3">
          <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500/20 to-emerald-500/20 flex items-center justify-center text-2xl shrink-0">
            {{ kb.icon || '📁' }}
          </div>
          <div class="relative" @click.stop>
            <button
              @click="contextMenuKB = contextMenuKB === kb.id ? null : kb.id"
              class="p-1.5 rounded-lg opacity-0 group-hover:opacity-100 hover:bg-white/5 transition-all text-gpt-muted"
            >
              <MoreHorizontal :size="16" />
            </button>
            <!-- Dropdown -->
            <div
              v-if="contextMenuKB === kb.id"
              class="absolute right-0 top-full mt-1 w-36 glass-card rounded-xl p-1.5 shadow-glass z-10 animate-slide-down"
            >
              <button
                @click="openEditModal(kb)"
                class="w-full flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-gpt-text hover:bg-white/5 transition-colors"
              >
                <Edit3 :size="14" />
                编辑
              </button>
              <button
                @click="openDeleteModal(kb)"
                class="w-full flex items-center gap-2 px-3 py-2 rounded-lg text-sm text-red-400 hover:bg-red-500/10 transition-colors"
              >
                <Trash2 :size="14" />
                删除
              </button>
            </div>
          </div>
        </div>

        <!-- Card Body -->
        <h3 class="font-semibold text-gpt-text mb-1 group-hover:text-blue-400 transition-colors line-clamp-1">
          {{ kb.name }}
        </h3>
        <p class="text-xs text-gpt-muted mb-4 line-clamp-2 min-h-[2.5em]">
          {{ kb.description || '暂无描述' }}
        </p>

        <!-- Stats -->
        <div class="flex items-center gap-4 text-xs text-gpt-muted">
          <div class="flex items-center gap-1.5">
            <FileText :size="14" />
            <span>{{ kb.doc_count }} 文档</span>
          </div>
          <div class="flex items-center gap-1.5">
            <Layers :size="14" />
            <span>{{ formatNumber(kb.total_chunks) }} 切片</span>
          </div>
        </div>

        <!-- Footer -->
        <div class="mt-3 pt-3 border-t border-gpt-border flex items-center justify-between text-xs text-gpt-dim">
          <span>{{ formatDate(kb.created_at) }}</span>
          <span class="text-gpt-accent opacity-0 group-hover:opacity-100 transition-opacity font-medium">
            进入 →
          </span>
        </div>
      </div>
    </div>

    <!-- ─── Pagination ─── -->
    <div
      v-if="totalPages > 1"
      class="flex items-center justify-center gap-2 mt-8 pt-6 border-t border-gpt-border"
    >
      <button
        @click="changePage(page - 1)"
        :disabled="page <= 1"
        class="p-2 rounded-lg border border-gpt-border text-gpt-muted hover:text-gpt-text hover:bg-white/5 transition-all disabled:opacity-30 disabled:cursor-not-allowed"
      >
        <ChevronLeft :size="16" />
      </button>

      <template v-for="p in totalPages" :key="p">
        <button
          v-if="p === 1 || p === totalPages || Math.abs(p - page) <= 1"
          @click="changePage(p)"
          :class="p === page
            ? 'bg-blue-500 text-white shadow-glow'
            : 'border border-gpt-border text-gpt-muted hover:text-gpt-text hover:bg-white/5'"
          class="min-w-[36px] h-9 rounded-lg text-sm font-medium transition-all"
        >
          {{ p }}
        </button>
        <span
          v-else-if="Math.abs(p - page) === 2"
          class="text-gpt-dim text-sm px-1"
        >...</span>
      </template>

      <button
        @click="changePage(page + 1)"
        :disabled="page >= totalPages"
        class="p-2 rounded-lg border border-gpt-border text-gpt-muted hover:text-gpt-text hover:bg-white/5 transition-all disabled:opacity-30 disabled:cursor-not-allowed"
      >
        <ChevronRight :size="16" />
      </button>
    </div>

    <!-- ─── Create KB Modal ─── -->
    <Modal v-model:open="showCreateModal" title="新建知识库" max-width="max-w-md">
      <div class="space-y-4">
        <div class="space-y-1.5">
          <label class="text-sm font-medium text-gpt-text">名称 <span class="text-red-400">*</span></label>
          <input
            v-model="createForm.name"
            type="text"
            placeholder="例如：工作文档"
            class="w-full px-4 py-2.5 bg-gpt-bg border border-gpt-border rounded-xl text-sm text-gpt-text placeholder:text-gpt-dim focus:outline-none focus:border-blue-500/50 transition-all"
          />
        </div>
        <div class="space-y-1.5">
          <label class="text-sm font-medium text-gpt-text">描述</label>
          <textarea
            v-model="createForm.description"
            rows="2"
            placeholder="简单描述这个知识库的内容..."
            class="w-full px-4 py-2.5 bg-gpt-bg border border-gpt-border rounded-xl text-sm text-gpt-text placeholder:text-gpt-dim focus:outline-none focus:border-blue-500/50 transition-all resize-none"
          />
        </div>
        <!-- Icon picker -->
        <div class="space-y-1.5">
          <label class="text-sm font-medium text-gpt-text">图标</label>
          <div class="relative">
            <button
              @click="showIconPicker = !showIconPicker"
              class="w-full flex items-center gap-2 px-4 py-2.5 bg-gpt-bg border border-gpt-border rounded-xl text-sm text-gpt-text hover:border-blue-500/50 transition-all"
            >
              <span class="text-xl">{{ createForm.icon }}</span>
              <span class="text-gpt-muted">{{ createForm.icon === '📁' ? '文件夹' : createForm.icon === '📚' ? '书籍' : createForm.icon === '📝' ? '笔记' : createForm.icon === '💡' ? '灵感' : createForm.icon === '🔬' ? '研究' : createForm.icon === '📊' ? '数据' : createForm.icon === '🗂️' ? '归档' : createForm.icon === '💼' ? '工作' : createForm.icon === '🎯' ? '目标' : createForm.icon === '🚀' ? '火箭' : createForm.icon === '⚙️' ? '工程' : createForm.icon === '🔒' ? '私密' : createForm.icon === '🌐' ? '网络' : createForm.icon === '📖' ? '手册' : createForm.icon === '🏗️' ? '架构' : createForm.icon === '🤖' ? 'AI' : '自定义' }}</span>
            </button>
            <!-- Emoji grid -->
            <div v-if="showIconPicker" class="absolute top-full mt-1 w-full glass-card p-2 grid grid-cols-8 gap-1 z-20 animate-slide-down">
              <button
                v-for="icon in iconOptions"
                :key="icon"
                @click="createForm.icon = icon; showIconPicker = false"
                class="w-9 h-9 flex items-center justify-center rounded-lg text-lg hover:bg-white/10 transition-colors"
                :class="{ 'bg-blue-500/20 ring-1 ring-blue-500/50': createForm.icon === icon }"
              >{{ icon }}</button>
            </div>
          </div>
          <!-- Custom emoji input -->
          <input
            v-model="createForm.icon"
            type="text"
            maxlength="2"
            placeholder="或直接输入 emoji"
            class="w-full px-4 py-2 bg-gpt-bg border border-gpt-border rounded-xl text-sm text-gpt-text placeholder:text-gpt-dim focus:outline-none focus:border-blue-500/50 transition-all"
            @focus="showIconPicker = false"
          />
        </div>
        <div class="grid grid-cols-2 gap-3">
          <div class="space-y-1.5">
            <label class="text-sm font-medium text-gpt-text">切片大小</label>
            <input
              v-model.number="createForm.chunk_size"
              type="number"
              class="w-full px-4 py-2.5 bg-gpt-bg border border-gpt-border rounded-xl text-sm text-gpt-text focus:outline-none focus:border-blue-500/50 transition-all"
            />
          </div>
          <div class="space-y-1.5">
            <label class="text-sm font-medium text-gpt-text">重叠大小</label>
            <input
              v-model.number="createForm.chunk_overlap"
              type="number"
              class="w-full px-4 py-2.5 bg-gpt-bg border border-gpt-border rounded-xl text-sm text-gpt-text focus:outline-none focus:border-blue-500/50 transition-all"
            />
          </div>
        </div>
        <button
          @click="handleCreate"
          :disabled="createLoading || !createForm.name.trim()"
          class="w-full py-2.5 bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 text-white font-medium rounded-xl transition-all disabled:opacity-50 disabled:cursor-not-allowed text-sm flex items-center justify-center gap-2"
        >
          <span v-if="createLoading" class="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
          <span v-else>创建知识库</span>
        </button>
      </div>
    </Modal>

    <!-- ─── Edit KB Modal ─── -->
    <Modal v-model:open="showEditModal" title="编辑知识库" max-width="max-w-md">
      <div class="space-y-4">
        <div class="space-y-1.5">
          <label class="text-sm font-medium text-gpt-text">名称 <span class="text-red-400">*</span></label>
          <input
            v-model="editForm.name"
            type="text"
            class="w-full px-4 py-2.5 bg-gpt-bg border border-gpt-border rounded-xl text-sm text-gpt-text focus:outline-none focus:border-blue-500/50 transition-all"
          />
        </div>
        <div class="space-y-1.5">
          <label class="text-sm font-medium text-gpt-text">描述</label>
          <textarea
            v-model="editForm.description"
            rows="2"
            class="w-full px-4 py-2.5 bg-gpt-bg border border-gpt-border rounded-xl text-sm text-gpt-text focus:outline-none focus:border-blue-500/50 transition-all resize-none"
          />
        </div>
        <!-- Icon picker (edit) -->
        <div class="space-y-1.5">
          <label class="text-sm font-medium text-gpt-text">图标</label>
          <div class="relative">
            <button
              @click="showEditIconPicker = !showEditIconPicker"
              class="w-full flex items-center gap-2 px-4 py-2.5 bg-gpt-bg border border-gpt-border rounded-xl text-sm text-gpt-text hover:border-blue-500/50 transition-all"
            >
              <span class="text-xl">{{ editForm.icon }}</span>
              <span class="text-gpt-muted text-xs">点击更换</span>
            </button>
            <div v-if="showEditIconPicker" class="absolute top-full mt-1 w-full glass-card p-2 grid grid-cols-8 gap-1 z-20 animate-slide-down">
              <button
                v-for="icon in iconOptions"
                :key="icon"
                @click="editForm.icon = icon; showEditIconPicker = false"
                class="w-9 h-9 flex items-center justify-center rounded-lg text-lg hover:bg-white/10 transition-colors"
                :class="{ 'bg-blue-500/20 ring-1 ring-blue-500/50': editForm.icon === icon }"
              >{{ icon }}</button>
            </div>
          </div>
          <input
            v-model="editForm.icon"
            type="text"
            maxlength="2"
            placeholder="或直接输入 emoji"
            class="w-full px-4 py-2 bg-gpt-bg border border-gpt-border rounded-xl text-sm text-gpt-text placeholder:text-gpt-dim focus:outline-none focus:border-blue-500/50 transition-all"
            @focus="showEditIconPicker = false"
          />
        </div>
        <button
          @click="handleEdit"
          :disabled="editLoading || !editForm.name.trim()"
          class="w-full py-2.5 bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 text-white font-medium rounded-xl transition-all disabled:opacity-50 disabled:cursor-not-allowed text-sm"
        >
          {{ editLoading ? '保存中...' : '保存更改' }}
        </button>
      </div>
    </Modal>

    <!-- ─── Delete KB Modal ─── -->
    <Modal v-model:open="showDeleteModal" title="删除知识库" max-width="max-w-sm">
      <p class="text-sm text-gpt-muted mb-2">
        确定要删除知识库「<span class="text-gpt-text font-medium">{{ deletingKB?.name }}</span>」吗？
      </p>
      <p class="text-xs text-red-400 mb-5">
        ⚠️ 此操作不可撤销，将同时删除所有文档、分段及向量数据。
      </p>
      <div class="flex gap-3">
        <button
          @click="showDeleteModal = false"
          class="flex-1 py-2.5 border border-gpt-border rounded-xl text-sm text-gpt-muted hover:text-gpt-text hover:bg-white/5 transition-all"
        >
          取消
        </button>
        <button
          @click="handleDelete"
          :disabled="deleteLoading"
          class="flex-1 py-2.5 bg-red-500/10 hover:bg-red-500/20 border border-red-500/30 rounded-xl text-sm text-red-400 font-medium transition-all disabled:opacity-50"
        >
          {{ deleteLoading ? '删除中...' : '确认删除' }}
        </button>
      </div>
    </Modal>
  </AppLayout>
</template>
