<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { ApiError } from '@/api/client'
import { Eye, EyeOff, Mail, Lock, User, ArrowRight, BrainCircuit } from 'lucide-vue-next'

const router = useRouter()
const auth = useAuthStore()
const app = useAppStore()

const isLogin = ref(true)
const showPassword = ref(false)
const loading = ref(false)

const form = reactive({
  username: '',
  email: '',
  password: '',
})

const errors = reactive({
  username: '',
  email: '',
  password: '',
})

function clearErrors() {
  errors.username = ''
  errors.email = ''
  errors.password = ''
}

function validate(): boolean {
  clearErrors()
  let valid = true

  if (!isLogin.value && !form.username.trim()) {
    errors.username = '请输入用户名'
    valid = false
  }
  if (!form.email.trim()) {
    errors.email = '请输入邮箱'
    valid = false
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
    errors.email = '邮箱格式不正确'
    valid = false
  }
  if (!form.password) {
    errors.password = '请输入密码'
    valid = false
  } else if (form.password.length < 6) {
    errors.password = '密码至少 6 位'
    valid = false
  }

  return valid
}

async function handleSubmit() {
  if (!validate()) return

  loading.value = true
  try {
    if (isLogin.value) {
      await auth.login({ email: form.email, password: form.password })
    } else {
      await auth.register({
        username: form.username,
        email: form.email,
        password: form.password,
      })
    }
    app.showToast(
      isLogin.value ? '登录成功，欢迎回来！' : '注册成功，欢迎加入！',
      'success',
    )
    router.push('/dashboard')
  } catch (e) {
    const message = e instanceof ApiError
      ? e.message
      : '网络错误，请稍后重试'
    app.showToast(message, 'error')
  } finally {
    loading.value = false
  }
}

function switchMode() {
  isLogin.value = !isLogin.value
  clearErrors()
}
</script>

<template>
  <div class="min-h-screen flex bg-gpt-bg">
    <!-- Left: Hero / Brand -->
    <div class="hidden lg:flex lg:w-[45%] relative overflow-hidden bg-gpt-surface">
      <!-- Decorative background -->
      <div class="absolute inset-0">
        <div class="absolute top-20 left-10 w-72 h-72 bg-blue-500/10 rounded-full blur-3xl" />
        <div class="absolute bottom-20 right-10 w-96 h-96 bg-emerald-500/10 rounded-full blur-3xl" />
        <div class="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[600px] h-[600px] bg-gradient-to-br from-blue-500/5 to-emerald-500/5 rounded-full blur-2xl" />
      </div>
      <!-- Grid pattern overlay -->
      <div class="absolute inset-0 opacity-[0.03]"
        style="background-image: linear-gradient(rgba(255,255,255,0.1) 1px, transparent 1px), linear-gradient(90deg, rgba(255,255,255,0.1) 1px, transparent 1px); background-size: 60px 60px;" />

      <!-- Content -->
      <div class="relative z-10 flex flex-col justify-center px-16 h-full">
        <div class="mb-10">
          <img src="/favicon.png" alt="StoneRAG" class="w-14 h-14 rounded-2xl shadow-glow mb-6" />
          <h1 class="text-4xl font-bold text-gpt-text mb-4 leading-tight">
            StoneRAG<span class="gradient-text"> Hub</span>
          </h1>
          <p class="text-xl text-gpt-muted leading-relaxed">
            磐石般稳固的个人知识大脑<br />
            智能检索 · 深度理解 · 精准问答
          </p>
        </div>

        <div class="space-y-4">
          <div class="flex items-center gap-3 text-sm text-gpt-muted">
            <div class="w-8 h-8 rounded-lg bg-blue-500/10 flex items-center justify-center">
              <BrainCircuit :size="16" class="text-blue-400" />
            </div>
            <span>RAG 增强检索，让 AI 真正理解你的文档</span>
          </div>
          <div class="flex items-center gap-3 text-sm text-gpt-muted">
            <div class="w-8 h-8 rounded-lg bg-emerald-500/10 flex items-center justify-center">
              <Lock :size="16" class="text-emerald-400" />
            </div>
            <span>私有化知识库，数据安全可控</span>
          </div>
          <div class="flex items-center gap-3 text-sm text-gpt-muted">
            <div class="w-8 h-8 rounded-lg bg-purple-500/10 flex items-center justify-center">
              <ArrowRight :size="16" class="text-purple-400" />
            </div>
            <span>支持 PDF、Word、Markdown、网页等多种格式</span>
          </div>
        </div>
      </div>
    </div>

    <!-- Right: Login Form -->
    <div class="flex-1 flex items-center justify-center p-8">
      <div class="w-full max-w-md">
        <!-- Mobile logo -->
        <div class="lg:hidden flex items-center gap-3 mb-10 justify-center">
          <img src="/favicon.png" alt="StoneRAG" class="w-10 h-10 rounded-xl shadow-glow" />
          <span class="font-semibold text-xl text-gpt-text">StoneRAG Hub</span>
        </div>

        <!-- Card -->
        <div class="glass-card p-8 animate-slide-up">
          <h2 class="text-2xl font-bold text-gpt-text mb-1">
            {{ isLogin ? '欢迎回来' : '创建账号' }}
          </h2>
          <p class="text-sm text-gpt-muted mb-8">
            {{ isLogin ? '登录以继续使用你的知识库' : '注册一个账号开始构建知识大脑' }}
          </p>

          <form @submit.prevent="handleSubmit" class="space-y-5">
            <!-- Username (register only) -->
            <div v-if="!isLogin" class="space-y-1.5">
              <label class="text-sm font-medium text-gpt-text">用户名</label>
              <div class="relative">
                <User :size="18" class="absolute left-3.5 top-1/2 -translate-y-1/2 text-gpt-dim" />
                <input
                  v-model="form.username"
                  type="text"
                  placeholder="请输入用户名"
                  class="w-full pl-11 pr-4 py-3 bg-gpt-bg border rounded-xl text-gpt-text placeholder:text-gpt-dim focus:outline-none transition-all duration-200"
                  :class="errors.username ? 'border-red-500/50' : 'border-gpt-border focus:border-blue-500/50 focus:shadow-glow'"
                />
              </div>
              <p v-if="errors.username" class="text-xs text-red-400">{{ errors.username }}</p>
            </div>

            <!-- Email -->
            <div class="space-y-1.5">
              <label class="text-sm font-medium text-gpt-text">邮箱</label>
              <div class="relative">
                <Mail :size="18" class="absolute left-3.5 top-1/2 -translate-y-1/2 text-gpt-dim" />
                <input
                  v-model="form.email"
                  type="email"
                  placeholder="请输入邮箱"
                  class="w-full pl-11 pr-4 py-3 bg-gpt-bg border rounded-xl text-gpt-text placeholder:text-gpt-dim focus:outline-none transition-all duration-200"
                  :class="errors.email ? 'border-red-500/50' : 'border-gpt-border focus:border-blue-500/50 focus:shadow-glow'"
                />
              </div>
              <p v-if="errors.email" class="text-xs text-red-400">{{ errors.email }}</p>
            </div>

            <!-- Password -->
            <div class="space-y-1.5">
              <label class="text-sm font-medium text-gpt-text">密码</label>
              <div class="relative">
                <Lock :size="18" class="absolute left-3.5 top-1/2 -translate-y-1/2 text-gpt-dim" />
                <input
                  v-model="form.password"
                  :type="showPassword ? 'text' : 'password'"
                  placeholder="请输入密码（至少 6 位）"
                  class="w-full pl-11 pr-12 py-3 bg-gpt-bg border rounded-xl text-gpt-text placeholder:text-gpt-dim focus:outline-none transition-all duration-200"
                  :class="errors.password ? 'border-red-500/50' : 'border-gpt-border focus:border-blue-500/50 focus:shadow-glow'"
                />
                <button
                  type="button"
                  @click="showPassword = !showPassword"
                  class="absolute right-3.5 top-1/2 -translate-y-1/2 text-gpt-dim hover:text-gpt-muted transition-colors"
                >
                  <EyeOff v-if="showPassword" :size="18" />
                  <Eye v-else :size="18" />
                </button>
              </div>
              <p v-if="errors.password" class="text-xs text-red-400">{{ errors.password }}</p>
            </div>

            <!-- Submit -->
            <button
              type="submit"
              :disabled="loading"
              class="w-full py-3 bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 text-white font-medium rounded-xl transition-all duration-200 disabled:opacity-50 disabled:cursor-not-allowed shadow-glow flex items-center justify-center gap-2"
            >
              <span v-if="loading" class="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
              <span v-else>{{ isLogin ? '登录' : '注册' }}</span>
            </button>
          </form>

          <!-- Switch mode -->
          <p class="mt-6 text-center text-sm text-gpt-muted">
            {{ isLogin ? '还没有账号？' : '已有账号？' }}
            <button
              @click="switchMode"
              class="text-blue-400 hover:text-blue-300 font-medium transition-colors"
            >
              {{ isLogin ? '立即注册' : '去登录' }}
            </button>
          </p>
        </div>
      </div>
    </div>
  </div>
</template>
