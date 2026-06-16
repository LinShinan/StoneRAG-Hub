/**
 * SSE (Server-Sent Events) parser for fetch + ReadableStream.
 *
 * Parses the standard SSE format:
 *   event: <type>\n
 *   data: <json>\n\n
 */

export interface SSEEvent {
  event: string
  data: string
}

/**
 * Parse raw SSE text into structured events.
 * Handles partial chunks — incomplete events are buffered.
 */
export function createSSEParser() {
  let buffer = ''

  return function parse(chunk: string): SSEEvent[] {
    const events: SSEEvent[] = []
    buffer += chunk

    const parts = buffer.split('\n\n')
    // The last part might be incomplete — keep it in the buffer
    buffer = parts.pop() || ''

    for (const part of parts) {
      if (!part.trim()) continue

      let eventType = 'message'
      let data = ''

      const lines = part.split('\n')
      for (const line of lines) {
        if (line.startsWith('event: ')) {
          eventType = line.slice(7).trim()
        } else if (line.startsWith('data: ')) {
          data = line.slice(6)
        }
      }

      if (data) {
        events.push({ event: eventType, data })
      }
    }

    return events
  }
}

/**
 * Parse JSON data from an SSE event, with error handling.
 */
export function parseSSEData<T>(event: SSEEvent): T | null {
  try {
    return JSON.parse(event.data) as T
  } catch {
    console.warn(`Failed to parse SSE data for event "${event.event}":`, event.data)
    return null
  }
}
