import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User, AuthData, LoginPayload, RegisterPayload } from '@/types'
import { authApi } from '@/api/auth'
import { setToken, clearToken } from '@/api/client'
import { ApiError } from '@/api/client'

// Dev mode mock user — used when backend is unavailable
const DEV_USER: User = {
  id: 1,
  username: 'Dev',
  email: 'dev@localhost',
  avatar_url: null,
  role: 'user',
}

const isDev = import.meta.env.DEV

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(localStorage.getItem('auth_token'))
  const loading = ref(false)
  const devMode = ref(false)

  const isLoggedIn = computed(() => {
    if (devMode.value) return true
    return !!token.value && !!user.value
  })
  const isAdmin = computed(() => user.value?.role === 'admin')

  async function login(payload: LoginPayload) {
    loading.value = true
    try {
      const res = await authApi.login(payload)
      const data = res.data as AuthData
      token.value = data.token
      setToken(data.token)
      // Fetch full user profile
      await fetchMe()
      return data
    } finally {
      loading.value = false
    }
  }

  async function register(payload: RegisterPayload) {
    loading.value = true
    try {
      const res = await authApi.register(payload)
      const data = res.data as AuthData
      token.value = data.token
      setToken(data.token)
      await fetchMe()
      return data
    } finally {
      loading.value = false
    }
  }

  async function fetchMe() {
    if (devMode.value) return
    if (!token.value) return
    try {
      const res = await authApi.me()
      user.value = res.data as User
    } catch (e) {
      if (e instanceof ApiError && e.code === 40002) {
        logout()
      }
      // If backend is unreachable in dev, auto-enable dev mode
      if (isDev) {
        enableDevMode()
      }
    }
  }

  function enableDevMode() {
    devMode.value = true
    token.value = 'dev-mode-token'
    user.value = DEV_USER
    setToken('dev-mode-token')
  }

  function logout() {
    user.value = null
    token.value = null
    devMode.value = false
    clearToken()
    authApi.logout().catch(() => {})
  }

  return {
    user,
    token,
    loading,
    isLoggedIn,
    isAdmin,
    devMode,
    login,
    register,
    fetchMe,
    logout,
    enableDevMode,
  }
})
