// ── API Response ──
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

export interface PaginatedData<T> {
  items: T[]
  total: number
  page: number
  size: number
}

// ── Auth ──
export interface User {
  id: number
  username: string
  email: string
  avatar_url: string | null
  role: 'user' | 'admin'
}

export interface AuthData {
  id: number
  username: string
  token: string
}

export interface LoginPayload {
  email: string
  password: string
}

export interface RegisterPayload {
  username: string
  email: string
  password: string
}

// ── Knowledge Base ──
export interface KnowledgeBase {
  id: number
  name: string
  description: string
  icon: string
  embedding_model: string
  chunk_size: number
  chunk_overlap: number
  doc_count: number
  total_chunks: number
  created_at: string
}

export interface KnowledgeBaseStats {
  doc_count: number
  chunk_count: number
  total_chars: number
  file_type_dist: Record<string, number>
  recent_docs: Array<{ id: number; title: string; created_at: string }>
}

export interface CreateKBPayload {
  name: string
  description?: string
  icon?: string
  embedding_model?: string
  chunk_size?: number
  chunk_overlap?: number
}

export interface UpdateKBPayload {
  name?: string
  description?: string
  icon?: string
}

// ── Document ──
export type DocStatus = 'uploading' | 'parsing' | 'embedding' | 'ready' | 'error'
export type DocFileType = 'pdf' | 'docx' | 'md' | 'txt' | 'url'

export interface Document {
  id: number
  title: string
  file_type: DocFileType
  file_size: number
  status: DocStatus
  chunk_count: number
  error_msg: string | null
  tags_json: string[] | null
  created_at: string
}

export interface DocumentDetail extends Document {
  kb_id: number
  chunks_preview: ChunkPreview[]
}

export interface ChunkPreview {
  index: number
  content_text: string
  char_count: number
}

// ── Conversation & Messages ──
export interface Conversation {
  id: number
  title: string
  kb_id: number
  created_at: string
  updated_at: string
}

export interface Message {
  id: number
  role: 'user' | 'assistant'
  content: string
  sources_json: Source[] | null
  token_count: number | null
  created_at: string
}

export interface ConversationDetail {
  conversation: Conversation
  messages: Message[]
}

export interface Source {
  doc_title: string
  chunk_id: number
  score: number
  text: string
}

// ── SSE Events ──
export type SSEEventType = 'thinking' | 'sources' | 'token' | 'done' | 'agent_step' | 'agent_done'

export interface SSEThinkingData {
  stage: 'retrieving' | 'reranking' | 'generating'
}

export interface SSETokenData {
  token: string
  index: number
}

export interface SSEDoneData {
  message_id: number
  conversation_id: number
  total_tokens: number
}

export interface SSEAgentStepData {
  step: 'thought' | 'tool_call' | 'tool_result' | 'generate'
  content?: string
  tool?: string
  args?: Record<string, any>
  output?: string
  token?: string
}

export interface SSEAgentDoneData {
  task_id: string
  total_steps: number
  total_tokens: number
}

// ── Share ──
export interface Share {
  share_code: string
  kb_id: number
  kb_name: string
  permission: 'view' | 'ask'
  expire_at: string | null
  view_count: number
  created_at: string
}

export interface PublicShare {
  kb_name: string
  kb_description: string
  owner_name: string
  permission: 'view' | 'ask'
  doc_count: number
  view_count: number
}

export interface CreateSharePayload {
  kb_id: number
  permission: 'view' | 'ask'
  expire_at?: string
}

// ── Agent ──
export interface AgentTaskPayload {
  kb_ids: number[]
  task: string
  model?: string
}

// ── Feedback ──
export type FeedbackType = 'up' | 'down'
