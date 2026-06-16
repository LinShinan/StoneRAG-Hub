<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { useRouter, useRoute } from 'vue-router'
import { computed } from 'vue'
import {
  FolderOpen,
  MessageSquare,
  Share2,
  Bot,
  LogOut,
  User,
  Settings,
  Sun,
  Moon,
  ChevronLeft,
  Plus,
  BrainCircuit,
} from 'lucide-vue-next'

const auth = useAuthStore()
const app = useAppStore()
const router = useRouter()
const route = useRoute()

const navItems = computed(() => [
  {
    path: '/dashboard',
    label: '我的知识库',
    icon: FolderOpen,
    exact: true,
  },
  {
    path: '/chat',
    label: '智能AI对话',
    icon: MessageSquare,
  },
  {
    path: '/shares',
    label: '分享管理',
    icon: Share2,
  },
  {
    path: '/agent',
    label: 'Agent 任务',
    icon: Bot,
  },
])

function isActive(path: string, exact?: boolean) {
  if (exact) return route.path === path
  return route.path.startsWith(path)
}

function navigate(path: string) {
  router.push(path)
}

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <aside
    :class="[
      'h-screen sticky top-0 flex flex-col border-r border-gpt-border transition-all duration-300',
      app.sidebarCollapsed ? 'w-[68px]' : 'w-[260px]',
    ]"
    class="bg-gpt-surface/50 backdrop-blur-sm"
  >
    <!-- Logo area -->
    <div class="flex items-center px-5 h-16 border-b border-gpt-border shrink-0">
      <div
        v-if="!app.sidebarCollapsed"
        class="flex items-center gap-2.5 cursor-pointer"
        @click="router.push('/dashboard')"
      >
        <img src="/favicon.png" alt="StoneRAG" class="w-8 h-8 rounded-lg shadow-glow" />
        <span class="font-semibold text-gpt-text tracking-tight">StoneRAG</span>
      </div>
      <div
        v-else
        class="cursor-pointer mx-auto"
        @click="router.push('/dashboard')"
      >
        <img src="/favicon.png" alt="StoneRAG" class="w-8 h-8 rounded-lg shadow-glow" />
      </div>
      <button
        @click="app.toggleSidebar()"
        class="ml-auto p-1.5 rounded-lg hover:bg-white/5 transition-colors text-gpt-muted"
        :class="{ 'ml-0 mx-auto mt-3': app.sidebarCollapsed }"
      >
        <ChevronLeft
          :size="18"
          :class="{ 'rotate-180': app.sidebarCollapsed }"
          class="transition-transform duration-300"
        />
      </button>
    </div>

    <!-- Nav items -->
    <nav class="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
      <button
        v-for="item in navItems"
        :key="item.path"
        @click="navigate(item.path)"
        :class="[
          'w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 group',
          isActive(item.path, item.exact)
            ? 'bg-blue-500/10 text-blue-400 shadow-sm'
            : 'text-gpt-muted hover:text-gpt-text hover:bg-white/5',
        ]"
        :title="app.sidebarCollapsed ? item.label : undefined"
      >
        <component :is="item.icon" :size="20" />
        <span v-if="!app.sidebarCollapsed" class="flex-1 text-left">{{ item.label }}</span>
        <Plus
          v-if="!app.sidebarCollapsed && item.path === '/dashboard'"
          :size="16"
          class="opacity-0 group-hover:opacity-100 transition-opacity"
          @click.stop="navigate('/dashboard')"
        />
      </button>
    </nav>

    <!-- Bottom section -->
    <div class="px-3 py-4 border-t border-gpt-border space-y-2">
      <!-- Theme toggle -->
      <button
        @click="app.toggleTheme()"
        :class="app.sidebarCollapsed ? 'justify-center' : ''"
        class="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm text-gpt-muted hover:text-gpt-text hover:bg-white/5 transition-all"
      >
        <Sun v-if="app.theme === 'dark'" :size="20" />
        <Moon v-else :size="20" />
        <span v-if="!app.sidebarCollapsed">{{ app.theme === 'dark' ? '明亮模式' : '暗黑模式' }}</span>
      </button>

      <!-- Health indicator -->
      <div
        :class="app.sidebarCollapsed ? 'justify-center' : ''"
        class="flex items-center gap-3 px-3 py-2.5 rounded-xl text-xs text-gpt-muted"
      >
        <span class="relative flex h-2.5 w-2.5">
          <span class="animate-ping absolute inline-flex h-full w-full rounded-full bg-emerald-400 opacity-75" />
          <span class="relative inline-flex rounded-full h-2.5 w-2.5 bg-emerald-500" />
        </span>
        <span v-if="!app.sidebarCollapsed">系统运行正常</span>
      </div>

      <!-- User area -->
      <div
        v-if="auth.user"
        :class="app.sidebarCollapsed ? 'justify-center' : ''"
        class="flex items-center gap-3 px-3 py-2.5 rounded-xl"
      >
        <div class="w-8 h-8 rounded-full bg-gradient-to-br from-blue-500 to-purple-500 flex items-center justify-center text-white text-xs font-bold shrink-0">
          {{ auth.user.username.charAt(0).toUpperCase() }}
        </div>
        <div v-if="!app.sidebarCollapsed" class="flex-1 min-w-0">
          <p class="text-sm font-medium text-gpt-text truncate">{{ auth.user.username }}</p>
          <p class="text-xs text-gpt-muted truncate">{{ auth.user.email }}</p>
        </div>
        <button
          @click="handleLogout"
          class="p-1.5 rounded-lg hover:bg-red-500/10 text-gpt-muted hover:text-red-400 transition-all shrink-0"
          title="登出"
        >
          <LogOut :size="16" />
        </button>
      </div>
    </div>
  </aside>
</template>
