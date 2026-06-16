<script setup lang="ts">
import { useAppStore } from '@/stores/app'
import { X, CheckCircle, AlertCircle, AlertTriangle, Info } from 'lucide-vue-next'
import { computed } from 'vue'

const app = useAppStore()

const icon = computed(() => {
  switch (app.toastType) {
    case 'success': return CheckCircle
    case 'error': return AlertCircle
    case 'warning': return AlertTriangle
    default: return Info
  }
})

const bgClass = computed(() => {
  switch (app.toastType) {
    case 'success': return 'bg-emerald-500/10 border-emerald-500/30 text-emerald-400'
    case 'error': return 'bg-red-500/10 border-red-500/30 text-red-400'
    case 'warning': return 'bg-amber-500/10 border-amber-500/30 text-amber-400'
    default: return 'bg-blue-500/10 border-blue-500/30 text-blue-400'
  }
})
</script>

<template>
  <Teleport to="body">
    <transition name="slide">
      <div
        v-if="app.toastVisible"
        :class="bgClass"
        class="fixed top-6 right-6 z-[9999] flex items-center gap-3 px-5 py-3.5 rounded-xl border backdrop-blur-xl shadow-glass max-w-md animate-slide-down"
      >
        <component :is="icon" :size="20" />
        <span class="text-sm font-medium flex-1 text-gpt-text">{{ app.toastMessage }}</span>
        <button
          @click="app.toastVisible = false"
          class="text-gpt-muted hover:text-gpt-text transition-colors"
        >
          <X :size="16" />
        </button>
      </div>
    </transition>
  </Teleport>
</template>
