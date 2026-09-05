import request from '@/utils/request'

// ==================== 工作流配置 ====================

/** 获取全部工作流配置 */
export function listWorkflowConfig() {
  return request({ url: '/fee/init/workflow/list', method: 'get' })
}

/** 获取单条工作流配置 */
export function getWorkflowConfig(workflowKey) {
  return request({ url: '/fee/init/workflow/' + workflowKey, method: 'get' })
}

/** 保存工作流配置 */
export function saveWorkflowConfig(data) {
  return request({ url: '/fee/init/workflow/save', method: 'post', data })
}

/** 部署工作流 */
export function deployWorkflow(id) {
  return request({ url: '/fee/init/workflow/deploy/' + id, method: 'post' })
}

/** 停用工作流 */
export function disableWorkflow(id) {
  return request({ url: '/fee/init/workflow/disable/' + id, method: 'post' })
}

/** 启用工作流 */
export function enableWorkflow(id) {
  return request({ url: '/fee/init/workflow/enable/' + id, method: 'post' })
}

// ==================== 资金池配置 ====================

/** 获取资金池配置列表 */
export function listFundPoolConfig() {
  return request({ url: '/fee/init/fundPool/list', method: 'get' })
}

/** 保存资金池配置 */
export function saveFundPoolConfig(data) {
  return request({ url: '/fee/init/fundPool/save', method: 'post', data })
}
