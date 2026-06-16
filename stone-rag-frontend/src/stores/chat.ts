import { defineStore } from 'pinia'
import { ref, shallowRef } from 'vue'
import type { Conversation, Message, Source, SSEThinkingData } from '@/types'
import { convApi } from '@/api/conversations'
import { createSSEParser, parseSSEData } from '@/utils/sse'

export interface StreamingMessage {
  id: string // temporary client-side ID
  role: 'assistant'
  content: string
  thinking: boolean
  thinkingStage: string
  sources: Source[]
  done: boolean
  messageId?: number
  conversationId?: number
  totalTokens?: number
}

export const useChatStore = defineStore('chat', () => {
  const conversations = ref<Conversation[]>([])
  const currentConvId = ref<number | null>(null)
  const messages = ref<Message[]>([])
  const streamingMessage = shallowRef<StreamingMessage | null>(null)
  const isStreaming = ref(false)
  const loadingMessages = ref(false)
  const abortController = shallowRef<AbortController | null>(null)

  async function fetchConversations(kbId?: number) {
    try {
      const res = await convApi.list({ size: 100, kb_id: kbId })
      conversations.value = res.data.items
    } catch {
      // Silently fail — conversations are not critical
    }
  }

  async function fetchMessages(convId: number) {
    loadingMessages.value = true
    try {
      const res = await convApi.get(convId)
      messages.value = res.data.messages
      currentConvId.value = convId
    } catch {
      messages.value = []
    } finally {
      loadingMessages.value = false
    }
  }

  async function createConversation(kbId: number, title?: string): Promise<Conversation> {
    const res = await convApi.create({ kb_id: kbId, title })
    conversations.value.unshift(res.data as Conversation)
    return res.data as Conversation
  }

  async function deleteConversation(id: number) {
    await convApi.delete(id)
    conversations.value = conversations.value.filter((c) => c.id !== id)
    if (currentConvId.value === id) {
      currentConvId.value = null
      messages.value = []
    }
  }

  function cancelStream() {
    if (abortController.value) {
      abortController.value.abort()
      abortController.value = null
    }
    isStreaming.value = false
    streamingMessage.value = null
  }

  /**
   * Send a question and process the SSE stream.
   */
  async function askQuestion(kbId: number, question: string, convId?: number | null) {
    // Cancel any existing stream
    cancelStream()

    const targetConvId = convId || 'new'

    // Create a temporary streaming message
    const tempId = `stream_${Date.now()}`
    streamingMessage.value = {
      id: tempId,
      role: 'assistant',
      content: '',
      thinking: true,
      thinkingStage: 'retrieving',
      sources: [],
      done: false,
    }
    isStreaming.value = true

    const controller = new AbortController()
    abortController.value = controller

    try {
      const response = await convApi.ask(targetConvId as number | 'new', { kb_id: kbId, question }, controller.signal)

      if (!response.ok) {
        const errorJson = await response.json().catch(() => ({ message: '请求失败' }))
        throw new Error(errorJson.message || `HTTP ${response.status}`)
      }

      const reader = response.body!.getReader()
      const decoder = new TextDecoder()
      const parse = createSSEParser()

      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        const text = decoder.decode(value, { stream: true })
        const events = parse(text)

        for (const evt of events) {
          processSSEEvent(evt)
        }
      }
    } catch (err: any) {
      if (err.name === 'AbortError') {
        // User cancelled — streamingMessage is already nulled by cancelStream
        return
      }
      if (streamingMessage.value) {
        streamingMessage.value.content += '\n\n⚠️ 请求失败，请重试'
        streamingMessage.value.done = true
        streamingMessage.value.thinking = false
      }
    } finally {
      isStreaming.value = false
      abortController.value = null
    }
  }

  function processSSEEvent(evt: { event: string; data: string }) {
    const msg = streamingMessage.value
    if (!msg) return

    switch (evt.event) {
      case 'thinking': {
        const data = parseSSEData<SSEThinkingData>(evt)
        if (data) {
          msg.thinkingStage = data.stage
          msg.thinking = true
        }
        break
      }
      case 'sources': {
        const sources = parseSSEData<Source[]>(evt)
        if (sources) {
          msg.sources = sources
        }
        msg.thinking = false
        break
      }
      case 'token': {
        const data = parseSSEData<{ token: string; index: number }>(evt)
        if (data) {
          msg.content += data.token
        }
        msg.thinking = false
        break
      }
      case 'done': {
        const data = parseSSEData<{ message_id: number; conversation_id: number; total_tokens: number }>(evt)
        if (data) {
          msg.messageId = data.message_id
          msg.conversationId = data.conversation_id
          msg.totalTokens = data.total_tokens
        }
        msg.done = true
        msg.thinking = false

        // Add the completed message to the messages list
        const completedMsg: Message = {
          id: msg.messageId || 0,
          role: 'assistant',
          content: msg.content,
          sources_json: msg.sources,
          token_count: msg.totalTokens || null,
          created_at: new Date().toISOString(),
        }
        messages.value.push(completedMsg)

        // Update conversation ID if this was a new conversation
        if (msg.conversationId && currentConvId.value === null) {
          currentConvId.value = msg.conversationId
          fetchConversations()
        }
        break
      }
    }
  }

  /**
   * Clear current chat state (for starting fresh).
   */
  function clearChat() {
    cancelStream()
    currentConvId.value = null
    messages.value = []
    streamingMessage.value = null
  }

  return {
    conversations,
    currentConvId,
    messages,
    streamingMessage,
    isStreaming,
    loadingMessages,
    fetchConversations,
    fetchMessages,
    createConversation,
    deleteConversation,
    askQuestion,
    cancelStream,
    clearChat,
  }
})
