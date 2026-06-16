<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useAppStore } from '@/stores/app'
import { kbApi } from '@/api/knowledgeBases'
import { shareApi } from '@/api/shares'
import { ApiError } from '@/api/client'
import AppLayout from '@/components/layout/AppLayout.vue'
import Modal from '@/components/ui/Modal.vue'
import LoadingSpinner from '@/components/ui/LoadingSpinner.vue'
import EmptyState from '@/components/ui/EmptyState.vue'
import { formatDate, formatDateTime } from '@/utils/format'
import type { KnowledgeBase, Share, CreateSharePayload } from '@/types'
import {
  Plus,
  Trash2,
  Link2,
  Copy,
  Globe,
  Eye,
  MessageSquare,
  Calendar,
  Lock,
  Unlock,
  ExternalLink,
} from 'lucide-vue-next'

const app = useAppStore()

const shares = ref<Share[]>([])
const knowledgeBases = ref<KnowledgeBase[]>([])
const loading = ref(true)

// Create share modal
const showCreateModal = ref(false)
const createLoading = ref(false)
const createForm = ref<CreateSharePayload>({
  kb_id: 0,
  permission: 'ask',
  expire_at: undefined,
})

// Delete confirmation
const showDeleteModal = ref(false)
const deletingShare = ref<Share | null>(null)
const deleteLoading = ref(false)

async function fetchShares() {
  try {
    const res = await shareApi.list()
    shares.value = res.data.items
  } catch (e) {
    if (!(e instanceof ApiError && e.code === 40002)) {
      app.showToast('加载分享列表失败', 'error')
    }
  }
}

async function fetchKnowledgeBases() {
  try {
    const res = await kbApi.list({ size: 50 })
    knowledgeBases.value = res.data.items
    if (knowledgeBases.value.length > 0) {
      createForm.value.kb_id = knowledgeBases.value[0].id
    }
  } catch {}
}

async function handleCreate() {
  if (!createForm.value.kb_id) return
  createLoading.value = true
  try {
    const res = await shareApi.create(createForm.value)
    showCreateModal.value = false
    app.showToast('分享链接已创建！', 'success')
    await fetchShares()

    // Copy to clipboard
    const data = res.data as { share_code: string; share_url: string }
    const shareUrl = `${window.location.origin}${data.share_url}`
    try {
      await navigator.clipboard.writeText(shareUrl)
      app.showToast('分享链接已复制到剪贴板', 'success')
    } catch {}
  } catch (e) {
    const msg = e instanceof ApiError ? e.message : '创建失败'
    app.showToast(msg, 'error')
  } finally {
    createLoading.value = false
  }
}

function openDeleteModal(share: Share) {
  deletingShare.value = share
  showDeleteModal.value = true
}

async function handleDelete() {
  if (!deletingShare.value) return
  deleteLoading.value = true
  try {
    await shareApi.delete(deletingShare.value.share_code)
    showDeleteModal.value = false
    app.showToast('分享已取消', 'success')
    await fetchShares()
  } catch (e) {
    const msg = e instanceof ApiError ? e.message : '取消分享失败'
    app.showToast(msg, 'error')
  } finally {
    deleteLoading.value = false
  }
}

async function copyShareUrl(share: Share) {
  const url = `${window.location.origin}/share/${share.share_code}`
  try {
    await navigator.clipboard.writeText(url)
    app.showToast('链接已复制！', 'success')
  } catch {
    app.showToast('复制失败，请手动复制', 'error')
  }
}

function openShareUrl(share: Share) {
  window.open(`/share/${share.share_code}`, '_blank')
}

onMounted(async () => {
  await Promise.all([fetchShares(), fetchKnowledgeBases()])
  loading.value = false
})
</script>

<template>
  <AppLayout>
    <div class="flex items-center justify-between mb-8">
      <div>
        <h1 class="text-2xl font-bold text-gpt-text">分享管理</h1>
        <p class="text-sm text-gpt-muted mt-1">管理你的知识库分享链接</p>
      </div>
      <button
        @click="showCreateModal = true"
        class="flex items-center gap-2 px-4 py-2.5 bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 text-white font-medium rounded-xl transition-all text-sm shadow-glow"
      >
        <Plus :size="18" />
        创建分享
      </button>
    </div>

    <LoadingSpinner v-if="loading" text="加载分享列表中..." />

    <EmptyState
      v-else-if="shares.length === 0"
      icon="🔗"
      title="暂无分享"
      description="将知识库分享给他人，支持「仅查看」和「可提问」两种模式"
    >
      <button
        @click="showCreateModal = true"
        class="mt-4 inline-flex items-center gap-2 px-5 py-2.5 bg-gradient-to-r from-blue-500 to-blue-600 text-white rounded-xl font-medium text-sm"
      >
        <Plus :size="18" />
        创建第一个分享
      </button>
    </EmptyState>

    <div v-else class="space-y-3">
      <div
        v-for="share in shares"
        :key="share.share_code"
        class="flex items-center gap-5 p-5 rounded-xl border border-gpt-border hover:border-blue-500/20 hover:bg-white/[0.02] transition-all animate-fade-in"
      >
        <!-- Icon -->
        <div
          :class="share.permission === 'ask' ? 'bg-blue-500/10' : 'bg-emerald-500/10'"
          class="w-12 h-12 rounded-xl flex items-center justify-center shrink-0"
        >
          <Globe
            :size="22"
            :class="share.permission === 'ask' ? 'text-blue-400' : 'text-emerald-400'"
          />
        </div>

        <!-- Info -->
        <div class="flex-1 min-w-0">
          <div class="flex items-center gap-2 mb-1">
            <h3 class="font-semibold text-gpt-text">📁 {{ share.kb_name }}</h3>
            <span
              :class="share.permission === 'ask' ? 'bg-blue-500/10 text-blue-400' : 'bg-emerald-500/10 text-emerald-400'"
              class="px-2 py-0.5 rounded-md text-xs font-medium flex items-center gap-1"
            >
              <component :is="share.permission === 'ask' ? MessageSquare : Eye" :size="12" />
              {{ share.permission === 'ask' ? '可提问' : '仅查看' }}
            </span>
          </div>
          <div class="flex items-center gap-4 text-xs text-gpt-dim">
            <span class="flex items-center gap-1">
              <Link2 :size="12" />
              <code class="text-gpt-accent font-mono">{{ share.share_code }}</code>
            </span>
            <span class="flex items-center gap-1">
              <Eye :size="12" />
              {{ share.view_count }} 次浏览
            </span>
            <span v-if="share.expire_at" class="flex items-center gap-1">
              <Calendar :size="12" />
              过期：{{ formatDate(share.expire_at) }}
            </span>
            <span v-else class="flex items-center gap-1 text-emerald-400">
              <Lock :size="12" class="rotate-180" />
              永久有效
            </span>
          </div>
        </div>

        <!-- Actions -->
        <div class="flex items-center gap-2 shrink-0">
          <button
            @click="copyShareUrl(share)"
            class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-gpt-border text-xs text-gpt-muted hover:text-gpt-text hover:bg-white/5 transition-all"
          >
            <Copy :size="14" />
            复制链接
          </button>
          <button
            @click="openShareUrl(share)"
            class="flex items-center gap-1.5 px-3 py-1.5 rounded-lg border border-gpt-border text-xs text-gpt-muted hover:text-blue-400 hover:border-blue-500/30 transition-all"
          >
            <ExternalLink :size="14" />
            预览
          </button>
          <button
            @click="openDeleteModal(share)"
            class="p-1.5 rounded-lg hover:bg-red-500/10 text-gpt-muted hover:text-red-400 transition-colors"
          >
            <Trash2 :size="16" />
          </button>
        </div>
      </div>
    </div>

    <!-- ─── Create Share Modal ─── -->
    <Modal v-model:open="showCreateModal" title="创建分享" max-width="max-w-md">
      <div class="space-y-4">
        <div class="space-y-1.5">
          <label class="text-sm font-medium text-gpt-text">知识库 <span class="text-red-400">*</span></label>
          <select
            v-model="createForm.kb_id"
            class="w-full px-4 py-2.5 bg-gpt-bg border border-gpt-border rounded-xl text-sm text-gpt-text focus:outline-none focus:border-blue-500/50 transition-all"
          >
            <option :value="0" disabled>选择知识库</option>
            <option v-for="kb in knowledgeBases" :key="kb.id" :value="kb.id">
              {{ kb.icon || '📁' }} {{ kb.name }}
            </option>
          </select>
        </div>

        <div class="space-y-1.5">
          <label class="text-sm font-medium text-gpt-text">权限</label>
          <div class="grid grid-cols-2 gap-2">
            <button
              @click="createForm.permission = 'view'"
              :class="createForm.permission === 'view'
                ? 'border-blue-500/50 bg-blue-500/10 text-blue-400'
                : 'border-gpt-border text-gpt-muted hover:text-gpt-text'"
              class="flex flex-col items-center gap-2 px-4 py-3 rounded-xl border transition-all text-sm"
            >
              <Eye :size="20" />
              <span class="font-medium">仅查看</span>
              <span class="text-xs opacity-70">只能浏览文档</span>
            </button>
            <button
              @click="createForm.permission = 'ask'"
              :class="createForm.permission === 'ask'
                ? 'border-blue-500/50 bg-blue-500/10 text-blue-400'
                : 'border-gpt-border text-gpt-muted hover:text-gpt-text'"
              class="flex flex-col items-center gap-2 px-4 py-3 rounded-xl border transition-all text-sm"
            >
              <MessageSquare :size="20" />
              <span class="font-medium">可提问</span>
              <span class="text-xs opacity-70">可以提问 AI</span>
            </button>
          </div>
        </div>

        <div class="space-y-1.5">
          <label class="text-sm font-medium text-gpt-text">过期时间（可选）</label>
          <input
            v-model="createForm.expire_at"
            type="datetime-local"
            class="w-full px-4 py-2.5 bg-gpt-bg border border-gpt-border rounded-xl text-sm text-gpt-text focus:outline-none focus:border-blue-500/50 transition-all"
          />
        </div>

        <button
          @click="handleCreate"
          :disabled="createLoading || !createForm.kb_id"
          class="w-full py-2.5 bg-gradient-to-r from-blue-500 to-blue-600 text-white font-medium rounded-xl text-sm transition-all disabled:opacity-50"
        >
          {{ createLoading ? '创建中...' : '创建分享链接' }}
        </button>
      </div>
    </Modal>

    <!-- ─── Delete Share Modal ─── -->
    <Modal v-model:open="showDeleteModal" title="取消分享" max-width="max-w-sm">
      <p class="text-sm text-gpt-muted mb-5">
        确定要取消知识库「<span class="text-gpt-text font-medium">{{ deletingShare?.kb_name }}</span>」的分享吗？分享链接将立即失效。
      </p>
      <div class="flex gap-3">
        <button
          @click="showDeleteModal = false"
          class="flex-1 py-2.5 border border-gpt-border rounded-xl text-sm text-gpt-muted hover:text-gpt-text transition-all"
        >
          保留
        </button>
        <button
          @click="handleDelete"
          :disabled="deleteLoading"
          class="flex-1 py-2.5 bg-red-500/10 hover:bg-red-500/20 border border-red-500/30 rounded-xl text-sm text-red-400 font-medium transition-all disabled:opacity-50"
        >
          {{ deleteLoading ? '取消中...' : '确认取消' }}
        </button>
      </div>
    </Modal>
  </AppLayout>
</template>
