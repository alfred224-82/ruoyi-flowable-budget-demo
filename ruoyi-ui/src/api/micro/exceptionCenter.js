import request from '@/utils/request'

// ===== T18.8 业务异常处理中心 =====

export function getExceptionList() {
  return request({ url: '/micro/exceptionCenter', method: 'get' })
}

export function resolveException(exceptionType, sourceId) {
  return request({
    url: '/micro/exceptionCenter/resolve',
    method: 'put',
    params: { exceptionType, sourceId }
  })
}
