import { api } from './client'
import type { ApiResponse, PaginatedData, Document, DocumentDetail } from '@/types'

export const docApi = {
  list(kbId: number, params?: { page?: number; size?: number; status?: string; file_type?: string }) {
    return api.get<PaginatedData<Document>>(`/knowledge-bases/${kbId}/documents`, params)
  },

  get(id: number) {
    return api.get<DocumentDetail>(`/documents/${id}`)
  },

  upload(kbId: number, file: File) {
    const formData = new FormData()
    formData.append('file', file)

    const token = localStorage.getItem('auth_token')
    const headers: Record<string, string> = {}
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    return fetch(`/api/knowledge-bases/${kbId}/documents/upload`, {
      method: 'POST',
      headers,
      body: formData,
    }).then((r) => r.json()) as Promise<ApiResponse<Document>>
  },

  importUrl(kbId: number, url: string) {
    return api.post<Document>(`/knowledge-bases/${kbId}/documents/import-url`, { url })
  },

  delete(id: number) {
    return api.delete(`/documents/${id}`)
  },

  reprocess(id: number) {
    return api.post<Document>(`/documents/${id}/reprocess`)
  },
}
