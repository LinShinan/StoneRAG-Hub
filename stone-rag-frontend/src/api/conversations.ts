import { api } from './client'
import type {
  ApiResponse,
  PaginatedData,
  Conversation,
  ConversationDetail,
  FeedbackType,
} from '@/types'

export const convApi = {
  list(params?: { page?: number; size?: number; kb_id?: number }) {
    return api.get<PaginatedData<Conversation>>('/conversations', params)
  },

  get(id: number) {
    return api.get<ConversationDetail>(`/conversations/${id}`)
  },

  create(payload: { kb_id: number; title?: string }) {
    return api.post<Conversation>('/conversations', payload)
  },

  delete(id: number) {
    return api.delete(`/conversations/${id}`)
  },

  /**
   * Stream a question via SSE. Returns raw Response for ReadableStream consumption.
   */
  ask(convId: number | 'new', payload: { kb_id: number; question: string }, signal?: AbortSignal) {
    return api.postStream(`/conversations/${convId}/ask`, payload, signal)
  },

  feedback(msgId: number, feedback: FeedbackType) {
    return api.post(`/messages/${msgId}/feedback`, { feedback })
  },
}
