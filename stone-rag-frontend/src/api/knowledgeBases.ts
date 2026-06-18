import { api } from './client'
import type {
  ApiResponse,
  PaginatedData,
  KnowledgeBase,
  KnowledgeBaseStats,
  CreateKBPayload,
  UpdateKBPayload,
} from '@/types'

export const kbApi = {
  list(params?: { page?: number; size?: number; keyword?: string }, signal?: AbortSignal) {
    return api.get<PaginatedData<KnowledgeBase>>('/knowledge-bases', params, signal)
  },

  get(id: number) {
    return api.get<KnowledgeBase>(`/knowledge-bases/${id}`)
  },

  create(payload: CreateKBPayload) {
    return api.post<KnowledgeBase>('/knowledge-bases', payload)
  },

  update(id: number, payload: UpdateKBPayload) {
    return api.put<KnowledgeBase>(`/knowledge-bases/${id}`, payload)
  },

  delete(id: number) {
    return api.delete(`/knowledge-bases/${id}`)
  },

  stats(id: number) {
    return api.get<KnowledgeBaseStats>(`/knowledge-bases/${id}/stats`)
  },
}
