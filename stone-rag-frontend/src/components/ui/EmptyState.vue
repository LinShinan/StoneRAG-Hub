<script setup lang="ts">
defineProps<{
  icon?: string
  title: string
  description?: string
}>()
</script>

<template>
  <div class="flex flex-col items-center justify-center py-16 text-center">
    <div class="text-5xl mb-4 opacity-60">{{ icon || '📭' }}</div>
    <h3 class="text-lg font-medium text-gpt-text mb-1.5">{{ title }}</h3>
    <p v-if="description" class="text-sm text-gpt-muted max-w-sm">{{ description }}</p>
    <slot />
  </div>
</template>
