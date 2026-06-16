<script setup lang="ts">
import { X } from 'lucide-vue-next'
import { onMounted, onUnmounted } from 'vue'

const props = defineProps<{
  open: boolean
  title?: string
  maxWidth?: string
}>()

const emit = defineEmits<{
  'update:open': [value: boolean]
}>()

function close() {
  emit('update:open', false)
}

function onKeydown(e: KeyboardEvent) {
  if (e.key === 'Escape') close()
}

onMounted(() => {
  document.addEventListener('keydown', onKeydown)
})

onUnmounted(() => {
  document.removeEventListener('keydown', onKeydown)
})
</script>

<template>
  <Teleport to="body">
    <transition name="fade">
      <div
        v-if="open"
        class="fixed inset-0 z-50 flex items-center justify-center p-4"
        @click.self="close()"
      >
        <!-- Backdrop -->
        <div class="absolute inset-0 bg-black/60 backdrop-blur-sm" />

        <!-- Modal -->
        <div
          :class="maxWidth || 'max-w-lg'"
          class="relative w-full glass-card p-6 shadow-glass animate-slide-up"
        >
          <!-- Header -->
          <div v-if="title || $slots.header" class="flex items-center justify-between mb-5">
            <h3 class="text-lg font-semibold text-gpt-text">
              <slot name="header">{{ title }}</slot>
            </h3>
            <button
              @click="close()"
              class="p-1.5 rounded-lg hover:bg-white/5 transition-colors text-gpt-muted hover:text-gpt-text"
            >
              <X :size="18" />
            </button>
          </div>

          <!-- Body -->
          <slot />
        </div>
      </div>
    </transition>
  </Teleport>
</template>
