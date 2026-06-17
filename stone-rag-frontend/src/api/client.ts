/**
 * Base HTTP client with JWT auth and error handling.
 *
 * Uses fetch (not axios) to enable ReadableStream for SSE.
 */

import type { ApiResponse } from '@/types'

const BASE_URL = '/api'

// ── Token Management ──

function getToken(): string | null {
  return localStorage.getItem('auth_token')
}

export function setToken(token: string): void {
  localStorage.setItem('auth_token', token)
}

export function clearToken(): void {
  localStorage.removeItem('auth_token')
}

// ── Request Builder ──

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  body?: unknown
  headers?: Record<string, string>
  params?: Record<string, string | number | undefined>
  signal?: AbortSignal
  rawResponse?: boolean // Return raw Response instead of parsed JSON
}

/**
 * Core request function. All API calls go through here.
 */
export async function request<T = any>(
  endpoint: string,
  options: RequestOptions = {},
): Promise<ApiResponse<T>> {
  const { method = 'GET', body, headers = {}, params, signal, rawResponse } = options

  // Build URL with query params
  let url = `${BASE_URL}${endpoint}`
  if (params) {
    const searchParams = new URLSearchParams()
    for (const [key, value] of Object.entries(params)) {
      if (value !== undefined) {
        searchParams.set(key, String(value))
      }
    }
    const qs = searchParams.toString()
    if (qs) url += `?${qs}`
  }

  // Build headers
  const reqHeaders: Record<string, string> = {
    ...headers,
  }

  // Auth token
  const token = getToken()
  if (token) {
    reqHeaders['Authorization'] = `Bearer ${token}`
  }

  // JSON content-type for requests with body
  if (body !== undefined && !headers['Content-Type']) {
    reqHeaders['Content-Type'] = 'application/json'
  }

  // Build fetch options
  const fetchOptions: RequestInit = {
    method,
    headers: reqHeaders,
    signal,
  }

  if (body !== undefined) {
    fetchOptions.body = JSON.stringify(body)
  }

  // Execute
  const response = await fetch(url, fetchOptions)

  if (rawResponse) {
    // Return raw response (for SSE streaming)
    if (!response.ok) {
      await handleError(response)
    }
    return response as unknown as ApiResponse<T>
  }

  // Parse JSON
  const json: ApiResponse<T> = await response.json()

  // Handle business errors
  if (json.code !== 200) {
    handleBusinessError(json.code, json.message)
  }

  return json
}

/**
 * Handle HTTP-level errors.
 */
async function handleError(response: Response): Promise<never> {
  if (response.status === 401) {
    handleBusinessError(40002, '登录已过期，请重新登录')
  }

  try {
    const json = await response.json()
    handleBusinessError(json.code || 50000, json.message || '请求失败')
  } catch {
    throw new ApiError(50000, `HTTP ${response.status}: 请求失败`)
  }
}

/**
 * Handle business-logic error codes.
 */
function handleBusinessError(code: number, message: string): never {
  // Auth failures — redirect to login
  if (code === 40002) {
    clearToken()
    // Redirect to login if not already there
    if (window.location.pathname !== '/login') {
      window.location.href = '/login'
    }
  }

  throw new ApiError(code, message)
}

/**
 * Custom API error class.
 */
export class ApiError extends Error {
  code: number

  constructor(code: number, message: string) {
    super(message)
    this.code = code
    this.name = 'ApiError'
  }
}

// ── Convenience Methods ──

export const api = {
  get<T>(endpoint: string, params?: Record<string, any>, signal?: AbortSignal) {
    return request<T>(endpoint, { method: 'GET', params, signal })
  },

  post<T>(endpoint: string, body?: unknown, signal?: AbortSignal) {
    return request<T>(endpoint, { method: 'POST', body, signal })
  },

  put<T>(endpoint: string, body?: unknown) {
    return request<T>(endpoint, { method: 'PUT', body })
  },

  delete<T>(endpoint: string) {
    return request<T>(endpoint, { method: 'DELETE' })
  },

  /**
   * POST with streaming response. Returns the raw Response object
   * so callers can use response.body.getReader() for SSE.
   */
  postStream(endpoint: string, body?: unknown, signal?: AbortSignal): Promise<Response> {
    const token = getToken()
    const headers: Record<string, string> = {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
    }
    if (token) {
      headers['Authorization'] = `Bearer ${token}`
    }

    return fetch(`${BASE_URL}${endpoint}`, {
      method: 'POST',
      headers,
      body: body ? JSON.stringify(body) : undefined,
      signal,
    })
  },
}
