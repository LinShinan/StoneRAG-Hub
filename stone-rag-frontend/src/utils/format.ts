/**
 * Format file size from bytes to human-readable string.
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  const i = Math.floor(Math.log(bytes) / Math.log(1024))
  return `${(bytes / Math.pow(1024, i)).toFixed(i > 0 ? 1 : 0)} ${units[i]}`
}

/**
 * Format a number with thousand separators.
 */
export function formatNumber(n: number | null | undefined): string {
  if (n == null || isNaN(n)) return '0'
  return n.toLocaleString('zh-CN')
}

/**
 * Format a character count — if > 10000, show in 万.
 */
export function formatCharCount(n: number | null | undefined): string {
  if (n == null || isNaN(n)) return '0'
  if (n >= 10000) {
    return `${(n / 10000).toFixed(1)} 万`
  }
  return formatNumber(n)
}

/**
 * Format an ISO date string to relative or absolute time.
 */
export function formatDate(iso: string): string {
  const date = new Date(iso)
  const now = new Date()
  const diff = now.getTime() - date.getTime()
  const mins = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)

  if (mins < 1) return '刚刚'
  if (mins < 60) return `${mins} 分钟前`
  if (hours < 24) return `${hours} 小时前`
  if (days < 7) return `${days} 天前`
  if (days < 365) {
    return `${date.getMonth() + 1}月${date.getDate()}日`
  }
  return `${date.getFullYear()}年${date.getMonth() + 1}月${date.getDate()}日`
}

/**
 * Format a full date-time string.
 */
export function formatDateTime(iso: string): string {
  const d = new Date(iso)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

/**
 * Truncate a string to a max length, adding ellipsis.
 */
export function truncate(str: string, maxLen: number): string {
  if (str.length <= maxLen) return str
  return str.slice(0, maxLen) + '…'
}

/**
 * Get a file icon emoji based on file type.
 */
export function getFileIcon(fileType: string): string {
  const map: Record<string, string> = {
    pdf: '📄',
    docx: '📝',
    md: '📋',
    txt: '📃',
    url: '🌐',
  }
  return map[fileType] || '📎'
}

/**
 * Get status display info.
 */
export function getStatusInfo(status: string): { label: string; color: string; bg: string } {
  const map: Record<string, { label: string; color: string; bg: string }> = {
    uploading: { label: '上传中', color: 'text-yellow-400', bg: 'bg-yellow-400/10' },
    parsing: { label: '解析中', color: 'text-blue-400', bg: 'bg-blue-400/10' },
    embedding: { label: '向量化中', color: 'text-purple-400', bg: 'bg-purple-400/10' },
    ready: { label: '就绪', color: 'text-emerald-400', bg: 'bg-emerald-400/10' },
    failed: { label: '失败', color: 'text-red-400', bg: 'bg-red-400/10' },
  }
  return map[status] || { label: '未知', color: 'text-gray-400', bg: 'bg-gray-400/10' }
}

/**
 * Get a color for a file type in charts.
 */
export function getFileTypeColor(type: string): string {
  const map: Record<string, string> = {
    pdf: '#EF4444',
    docx: '#3B82F6',
    md: '#10B981',
    txt: '#6B7280',
    url: '#F59E0B',
  }
  return map[type] || '#8B949E'
}
