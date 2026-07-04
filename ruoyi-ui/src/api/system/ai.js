import request from '@/utils/request'
import { getToken } from '@/utils/auth'

/**
 * AI对话 - 同步模式
 * @param {Object} data - { sessionId, message, sheetId, stream }
 */
export function aiChat(data) {
  return request({
    url: '/ai/chat',
    method: 'post',
    data: data,
    timeout: 120000 // AI响应可能较慢，设置2分钟超时
  })
}

/**
 * AI对话 - 流式模式（SSE）
 * @param {Object} data - { sessionId, message, sheetId, stream }
 * @param {Function} onMessage - 收到消息回调 (token) => {}
 * @param {Function} onDone - 完成回调 () => {}
 * @param {Function} onError - 错误回调 (error) => {}
 * @returns {Function} abort函数，用于取消请求
 */
export function aiChatStream(data, onMessage, onDone, onError) {
  const token = 'Bearer ' + getToken()
  const baseUrl = process.env.VUE_APP_BASE_API

  const controller = new AbortController()

  fetch(baseUrl + '/ai/chat/stream', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': token
    },
    body: JSON.stringify(data),
    signal: controller.signal
  }).then(async response => {
    if (!response.ok) {
      throw new Error('AI服务请求失败: ' + response.status)
    }
    const reader = response.body.getReader()
    const decoder = new TextDecoder('utf-8')
    let buffer = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() // 保留未完成的行

      for (const line of lines) {
        if (line.startsWith('data:')) {
          const content = line.substring(5).trim()
          if (content === '[DONE]') {
            onDone && onDone()
            return
          }
          if (content) {
            onMessage && onMessage(content)
          }
        } else if (line.startsWith('event:error')) {
          // 下一条data是错误信息
        }
      }
    }
    onDone && onDone()
  }).catch(error => {
    if (error.name !== 'AbortError') {
      onError && onError(error)
    }
  })

  return () => controller.abort()
}

/**
 * 清除AI会话
 * @param {String} sessionId - 会话ID
 */
export function clearAiSession(sessionId) {
  return request({
    url: '/ai/chat/session/' + sessionId,
    method: 'delete'
  })
}

/**
 * 检查AI服务状态
 */
export function getAiStatus() {
  return request({
    url: '/ai/chat/status',
    method: 'get'
  })
}
