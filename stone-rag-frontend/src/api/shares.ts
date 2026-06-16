import { api } from './client'
import type { ApiResponse, PaginatedData, Share, PublicShare, CreateSharePayload } from '@/types'

export const shareApi = {
  list() {
    return api.get<PaginatedData<Share>>('/shares')
  },

  create(payload: CreateSharePayload) {
    return api.post<{ share_code: string; share_url: string }>('/shares', payload)
  },

  delete(shareCode: string) {
    return api.delete(`/shares/${shareCode}`)
  },

  /**
   * Public — no auth required.
   */
  getPublic(shareCode: string) {
    return api.get<PublicShare>(`/public/shares/${shareCode}`)
  },

  /**
   * Public ask — no auth required. Returns raw SSE response.
   */
  askPublic(shareCode: string, question: string, signal?: AbortSignal) {
    return api.postStream(`/public/shares/${shareCode}/ask`, { question }, signal)
  },
}
