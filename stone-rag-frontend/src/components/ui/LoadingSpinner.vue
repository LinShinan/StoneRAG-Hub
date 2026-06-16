<script setup lang="ts">
defineProps<{
  size?: 'sm' | 'md' | 'lg'
  text?: string
}>()
</script>

<template>
  <div class="flex flex-col items-center justify-center gap-3 py-8">
    <div
      :class="{
        'w-6 h-6': size === 'sm',
        'w-10 h-10': size === 'md' || !size,
        'w-14 h-14': size === 'lg',
      }"
      class="border-2 border-gpt-border border-t-gpt-accent rounded-full animate-spin"
    />
    <p v-if="text" class="text-sm text-gpt-muted">{{ text }}</p>
  </div>
</template>
