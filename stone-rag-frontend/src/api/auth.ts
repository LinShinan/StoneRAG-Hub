import { api } from './client'
import type { ApiResponse, AuthData, LoginPayload, RegisterPayload, User } from '@/types'

export const authApi = {
  register(payload: RegisterPayload) {
    return api.post<AuthData>('/auth/register', payload)
  },

  login(payload: LoginPayload) {
    return api.post<AuthData>('/auth/login', payload)
  },

  logout() {
    return api.post('/auth/logout')
  },

  me() {
    return api.get<User>('/auth/me')
  },
}
