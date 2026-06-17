import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/pages/LoginPage.vue'),
      meta: { guest: true },
    },
    {
      path: '/',
      redirect: '/dashboard',
    },
    {
      path: '/dashboard',
      name: 'Dashboard',
      component: () => import('@/pages/DashboardPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/kb/:id',
      name: 'KnowledgeBaseDetail',
      component: () => import('@/pages/KBDetailPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/chat',
      name: 'Chat',
      component: () => import('@/pages/ChatPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/chat/:convId',
      name: 'ChatConversation',
      component: () => import('@/pages/ChatPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/shares',
      name: 'Shares',
      component: () => import('@/pages/SharesPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/agent',
      name: 'Agent',
      component: () => import('@/pages/AgentPage.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/share/:code',
      name: 'PublicShare',
      component: () => import('@/pages/PublicSharePage.vue'),
      meta: { guest: true },
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/dashboard',
    },
  ],
})

router.beforeEach(async (to, _from, next) => {
  const auth = useAuthStore()

  // Try to fetch user if we have a token but no user (e.g. after page refresh)
  if (auth.token && !auth.user) {
    try {
      await auth.fetchMe()
    } catch {
      // fetchMe handles 40002 internally — clears token and redirects to login
    }
  }

  // Guest-only pages (login) — redirect to dashboard if already logged in
  if (to.meta.guest && auth.isLoggedIn) {
    return next('/dashboard')
  }

  // Protected pages — redirect to login if not authenticated
  if (to.meta.requiresAuth && !auth.isLoggedIn) {
    return next('/login')
  }

  next()
})

export default router
