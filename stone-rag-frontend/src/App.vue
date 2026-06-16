<script setup lang="ts">
import { useAppStore } from '@/stores/app'
import ToastNotification from '@/components/ui/ToastNotification.vue'

const app = useAppStore()
</script>

<template>
  <router-view v-slot="{ Component }">
    <transition name="fade" mode="out-in">
      <component :is="Component" />
    </transition>
  </router-view>

  <!-- Global Toast -->
  <ToastNotification />
</template>
