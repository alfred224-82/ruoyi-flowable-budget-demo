/**
 * License功能开关前端工具
 * 根据后端 /system/license/info 返回的 features 列表，控制前端路由/菜单/按钮的可见性
 */
import store from '@/store'
import { getLicenseInfo } from '@/api/system/fee/fundPro'

/**
 * 路由路径 → 所需功能编码 映射表
 * key: 路由path中可能出现的关键词（匹配 route.path 或 route.meta.title）
 * value: 需要的功能编码
 *
 * 说明：后端 @RequireFeature 注解拦截的接口对应的前端页面
 * 如果路由匹配到某个功能编码，且当前License不包含该功能，则路由将被过滤
 */
const ROUTE_FEATURE_MAP = {
  // === AI模块 ===
  'ai/chat': 'BUDGET_AI_CHAT',
  'ai/report': 'BUDGET_AI_REPORT',
  // === 费用高级功能（若后续添加独立菜单页面，在此配置） ===
  'exchangeRate': 'FEE_MULTI_CURRENCY',
  'accrual': 'FEE_ACCRUAL_ENGINE',
  'asset': 'FEE_ACCRUAL_ENGINE',
  'contract': 'FEE_ACCRUAL_ENGINE',
  'loan': 'FEE_LOAN_LEDGER',
  'crossDeptAlloc': 'FEE_CROSS_DEPT_ALLOCATE',
  'desensitize': 'FEE_DESENSITIZE_CUSTOM',
  'subsidy': 'FEE_SUBSIDY_CONFIG',
  'profitTip': 'FEE_PROFIT_CUSTOM_TIP',
  'compliance': 'FEE_COMPLIANCE_CUSTOM',
  'trackControl': 'FEE_TRACK_CONTROL',
  'industryConfig': 'FEE_INDUSTRY_SINGLE',
  // === 高级版增量（M6） ===
  'fee/deptBudget': 'FEAT_DEPT_BUDGET',
  'profitReport': 'FEAT_PROFIT_REPORT',
  'clock': 'FEAT_TIME_CLOCK',
  'credit': 'FEAT_CREDIT_SCORE',
  'exportTemplate': 'FEAT_EXPORT_TEMPLATE_CUSTOM',
  // === 专业版增量（M7） ===
  'fee/org': 'FEAT_MULTI_ORG',
  'fee/orgBudget': 'FEAT_MULTI_ORG',
  'fee/faceConfig': 'FEAT_FACE_RECOGNITION'
}

// 内存缓存：页面内开关渲染用（与 store.getters.features 互为兜底）
const cache = {
  loaded: false,
  edition: '',
  features: new Set()
}

/**
 * 加载当前授权功能集合（带缓存，重复调用不重复请求）
 */
export function loadFeatures(force = false) {
  if (cache.loaded && !force) {
    return Promise.resolve(cache.features)
  }
  return getLicenseInfo().then(res => {
    const info = res.data || {}
    cache.edition = info.edition || ''
    cache.features = new Set(info.features || [])
    cache.loaded = true
    return cache.features
  }).catch(() => {
    // 加载失败视为无专业版能力，页面降级为基础/高级版展示
    cache.loaded = true
    cache.features = new Set()
    return cache.features
  })
}

/**
 * 检查当前License是否包含指定功能
 * @param {string} featureCode 功能编码
 * @returns {boolean}
 */
export function hasFeature(featureCode) {
  if (!featureCode) return true
  const features = store.getters && store.getters.features
  if (features && features.length > 0) {
    return features.includes(featureCode)
  }
  return cache.loaded && cache.features.has(featureCode)
}

/**
 * 检查当前版本等级
 * @param {string} edition 需要的最低版本 (BASIC/ADVANCED/ENTERPRISE/PROFESSIONAL)
 * @returns {boolean}
 */
export function hasEdition(edition) {
  const levels = { 'BASIC': 0, 'ADVANCED': 1, 'ENTERPRISE': 2, 'PROFESSIONAL': 2 }
  const current = store.getters && store.getters.edition
  return (levels[current] || 0) >= (levels[edition] || 0)
}

/**
 * admin 用户不受开关限制（与后端 FeatureInterceptor 一致）
 */
function isAdmin() {
  try {
    const userId = store.getters && store.getters.userId
    return String(userId) === '1'
  } catch (e) {
    return false
  }
}

/**
 * 页面内开关渲染组合判断：开关开启或 admin
 */
export function featureVisible(featureCode) {
  return isAdmin() || hasFeature(featureCode)
}

/**
 * 当前授权版本（BASIC/ADVANCED/PROFESSIONAL）
 */
export function getEdition() {
  return cache.edition
}

/**
 * 根据路由路径获取所需的功能编码
 * @param {string} path 路由路径
 * @returns {string|null} 功能编码，无匹配返回null
 */
export function getRequiredFeature(path) {
  if (!path) return null
  for (const keyword of Object.keys(ROUTE_FEATURE_MAP)) {
    if (path.includes(keyword)) {
      return ROUTE_FEATURE_MAP[keyword]
    }
  }
  return null
}

/**
 * 过滤路由：移除当前License不支持的功能路由
 * @param {Array} routes 路由数组
 * @returns {Array} 过滤后的路由
 */
export function filterRoutesByFeature(routes) {
  if (!routes || routes.length === 0) return routes
  return routes.filter(route => {
    // 检查当前路由是否需要功能授权
    const path = route.path || ''
    const name = route.name || ''
    const title = (route.meta && route.meta.title) || ''
    const checkStr = path + '|' + name + '|' + title

    const requiredFeature = getRequiredFeature(checkStr)
    if (requiredFeature && !hasFeature(requiredFeature)) {
      return false // 功能未授权，过滤掉
    }

    // 递归处理子路由
    if (route.children && route.children.length > 0) {
      route.children = filterRoutesByFeature(route.children)
      // 如果子路由全部被过滤，且当前路由是目录类型，也过滤掉
      if (route.children.length === 0 && route.component === 'Layout') {
        return false
      }
    }
    return true
  })
}

export default {
  hasFeature,
  hasEdition,
  loadFeatures,
  featureVisible,
  getEdition,
  getRequiredFeature,
  filterRoutesByFeature
}
