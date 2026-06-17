import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { User, AuthData, LoginPayload, RegisterPayload } from '@/types'
import { authApi } from '@/api/auth'
import { setToken, clearToken } from '@/api/client'
import { ApiError } from '@/api/client'

export const useAuthStore = defineStore('auth', () => {
  const user = ref<User | null>(null)
  const token = ref<string | null>(localStorage.getItem('auth_token'))
  const loading = ref(false)

  const isLoggedIn = computed(() => !!token.value && !!user.value)
  const isAdmin = computed(() => user.value?.role === 'admin')

  async function login(payload: LoginPayload) {
    loading.value = true
    try {
      const res = await authApi.login(payload)
      const data = res.data as AuthData
      token.value = data.token
      setToken(data.token)
      // Fetch full user profile after login
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
    if (!token.value) return
    try {
      const res = await authApi.me()
      user.value = res.data as User
    } catch (e) {
      // 40002: Token invalid/expired → auto logout
      if (e instanceof ApiError && e.code === 40002) {
        logout()
      }
      throw e
    }
  }

  function logout() {
    user.value = null
    token.value = null
    clearToken()
    // Best-effort server logout
    authApi.logout().catch(() => {})
  }

  return {
    user,
    token,
    loading,
    isLoggedIn,
    isAdmin,
    login,
    register,
    fetchMe,
    logout,
  }
})
