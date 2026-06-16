import { api } from './client'
import type { AgentTaskPayload } from '@/types'

export const agentApi = {
  /**
   * Submit an agent task and stream results via SSE.
   */
  run(payload: AgentTaskPayload, signal?: AbortSignal) {
    return api.postStream('/agent/task', payload, signal)
  },
}
