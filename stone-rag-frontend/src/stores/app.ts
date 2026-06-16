import { defineStore } from 'pinia'
import { ref } from 'vue'

export type ThemeMode = 'dark' | 'light'

const THEME_KEY = 'stone_rag_theme'

function getSavedTheme(): ThemeMode {
  try {
    const saved = localStorage.getItem(THEME_KEY)
    if (saved === 'light' || saved === 'dark') return saved
  } catch {}
  return 'dark'
}

function applyThemeToDOM(theme: ThemeMode) {
  const root = document.documentElement
  if (theme === 'light') {
    root.classList.add('light')
    root.setAttribute('data-theme', 'light')
  } else {
    root.classList.remove('light')
    root.setAttribute('data-theme', 'dark')
  }
  try {
    localStorage.setItem(THEME_KEY, theme)
  } catch {}
}

export const useAppStore = defineStore('app', () => {
  const theme = ref<ThemeMode>(getSavedTheme())
  const sidebarCollapsed = ref(false)
  const toastMessage = ref('')
  const toastType = ref<'success' | 'error' | 'warning' | 'info'>('info')
  const toastVisible = ref(false)
  let toastTimer: ReturnType<typeof setTimeout> | null = null

  // Apply saved theme immediately on store creation
  applyThemeToDOM(theme.value)

  function toggleTheme() {
    theme.value = theme.value === 'dark' ? 'light' : 'dark'
    applyThemeToDOM(theme.value)
  }

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function showToast(message: string, type: 'success' | 'error' | 'warning' | 'info' = 'info', duration = 4000) {
    toastMessage.value = message
    toastType.value = type
    toastVisible.value = true
    if (toastTimer) clearTimeout(toastTimer)
    toastTimer = setTimeout(() => {
      toastVisible.value = false
    }, duration)
  }

  return {
    theme,
    sidebarCollapsed,
    toastMessage,
    toastType,
    toastVisible,
    toggleTheme,
    toggleSidebar,
    showToast,
  }
})
