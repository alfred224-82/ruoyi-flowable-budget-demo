import request from '@/utils/request'

// ===== T18.15 语音处理审计日志 =====

export function listVoiceAuditLog(params) {
  return request({ url: '/system/voiceAuditLog/list', method: 'get', params })
}

export function getVoiceAuditSummary() {
  return request({ url: '/system/voiceAuditLog/summary', method: 'get' })
}
