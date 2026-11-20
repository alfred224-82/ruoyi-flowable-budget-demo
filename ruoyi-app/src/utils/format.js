/**
 * 通用格式化与状态映射（M8 小程序）
 */

/** 金额千分位（保留2位） */
export function formatAmount(value) {
    const num = Number(value)
    if (isNaN(num)) return '0.00'
    return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

/** 万元展示（看板大数） */
export function formatWan(value) {
    const num = Number(value)
    if (isNaN(num)) return '0'
    if (Math.abs(num) >= 10000) {
        return (num / 10000).toFixed(2) + '万'
    }
    return formatAmount(num)
}

/** 日期格式化（后端 Date → yyyy-MM-dd） */
export function formatDate(value) {
    if (!value) return '-'
    const d = new Date(value)
    if (isNaN(d.getTime())) return String(value).substring(0, 10)
    const y = d.getFullYear()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return `${y}-${m}-${day}`
}

/** 日期时间格式化（yyyy-MM-dd HH:mm:ss）——T30.1 三端时间精度统一到秒 */
export function formatDateTime(value) {
    if (!value) return '-'
    const d = new Date(value)
    if (isNaN(d.getTime())) return String(value)
    return formatDate(d) + ' ' + String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0') + ':' + String(d.getSeconds()).padStart(2, '0')
}

/** 项目状态（与后端 fee_project.status 一致） */
export const PROJECT_STATUS = {
    DRAFT: { label: '草稿', color: '#909399' },
    PENDING_APPROVAL: { label: '待审批', color: '#e6a23c' },
    ACTIVE: { label: '进行中', color: '#67c23a' },
    COMPLETED: { label: '已完成', color: '#409eff' },
    CLOSED: { label: '已关闭', color: '#909399' },
    LOST: { label: '烂账', color: '#f56c6c' }
}

/** 报销单状态 */
export const EXPENSE_STATUS = {
    DRAFT: { label: '草稿', color: '#909399' },
    SUBMITTED: { label: '待审批', color: '#e6a23c' },
    APPROVED: { label: '已通过', color: '#67c23a' },
    REJECTED: { label: '已驳回', color: '#f56c6c' },
    OVER_BUDGET_PENDING: { label: '超预算审批中', color: '#e6a23c' }
}

/** 收入确认核验状态（三态） */
export const INCOME_STATUS = {
    PENDING: { label: '待核验', color: '#e6a23c' },
    VERIFIED: { label: '已核验', color: '#67c23a' },
    REJECTED: { label: '已驳回', color: '#f56c6c' }
}

/** 付款单状态 */
export const PAYMENT_STATUS = {
    PENDING: { label: '待审批', color: '#e6a23c' },
    APPROVED: { label: '已通过', color: '#67c23a' },
    REJECTED: { label: '已驳回', color: '#f56c6c' }
}

/** 合同变更状态 */
export const CHANGE_STATUS = {
    PENDING: { label: '待审批', color: '#e6a23c' },
    REJECTED: { label: '已驳回', color: '#f56c6c' },
    CONFIRMED: { label: '已确认', color: '#67c23a' },
    OVERDUE: { label: '逾期未确认', color: '#f56c6c' }
}

/** 补票记录状态 */
export const SUPPLEMENT_STATUS = {
    PENDING: { label: '待补票', color: '#e6a23c' },
    COMPLETED: { label: '已补票', color: '#67c23a' },
    OVERDUE: { label: '已超期', color: '#f56c6c' }
}

/** 通用状态取词（未知状态原样返回） */
export function statusText(map, status) {
    return (map[status] || {}).label || status || '-'
}

/** 通用状态颜色 */
export function statusColor(map, status) {
    return (map[status] || {}).color || '#909399'
}

/** 当前日期 yyyy-MM-dd */
export function today() {
    return formatDate(new Date())
}

/** 当前报销年月 yyyy-MM */
export function currentPeriod() {
    return today().substring(0, 7)
}

/** 指定日期加 N 天（返回 yyyy-MM-dd，后补通道默认 +7 天） */
export function addDays(dateStr, days) {
    const d = dateStr ? new Date(dateStr) : new Date()
    d.setDate(d.getDate() + days)
    return formatDate(d)
}

/** ========== 小微版状态枚举（T15.15~T15.20） ========== */

/** 预约订单状态（与后端 BookingOrderStatus 枚举一致） */
export const BOOKING_STATUS = {
    PENDING_SERVICE: { label: '待服务', color: '#e6a23c' },
    PENDING_PAY: { label: '未收款', color: '#409eff' },
    COMPLETED: { label: '已完成', color: '#67c23a' },
    CANCELLED: { label: '已取消', color: '#909399' },
    NO_SHOW: { label: '已爽约', color: '#f56c6c' }
}

/** 收款状态（V1 零支付：仅 PENDING_PAY/RECEIVED 记账口径；全租户统一文案：未收款/已收款） */
export const PAY_STATUS = {
    PENDING_PAY: { label: '未收款', color: '#e6a23c' },
    RECEIVED: { label: '已收款', color: '#67c23a' }
}

/**
 * 解析自助预约 remark JSON 中的费用明细（costDetails / totalAmount）。
 * 历史订单无 costDetails 时返回空明细，由页面回退展示 amount。
 * @returns {{details:Array, total:number, hours:number, peopleCount:number, remarkText:string}}
 */
export function parseBookingCost(remark) {
    const empty = { details: [], total: 0, hours: 0, peopleCount: 0, remarkText: '' }
    if (!remark || typeof remark !== 'string') return empty
    let obj = null
    try { obj = JSON.parse(remark) } catch (e) { return empty }
    if (!obj || typeof obj !== 'object') return empty
    const details = Array.isArray(obj.costDetails) ? obj.costDetails : []
    let total = Number(obj.totalAmount)
    if (isNaN(total)) total = details.reduce((s, d) => s + Number(d.subtotal || 0), 0)
    return {
        details,
        total,
        hours: Number(obj.hours || 0),
        peopleCount: Number(obj.peopleCount || 0),
        remarkText: obj.remark || ''
    }
}

/** ========== 图片 URL 协议修正（微信已禁止 HTTP 加载图片） ========== */

/** 判断 URL 是否指向本地/回环地址（开发环境 MinIO 等仅支持 HTTP） */
function isLocalUrl(url) {
    return /^https?:\/\/(localhost|127\.0\.0\.1|0\.0\.0\.0|192\.168\.\d+\.\d+|10\.\d+\.\d+\.\d+|172\.(1[6-9]|2\d|3[01])\.\d+\.\d+)/i.test(url)
}

/**
 * 将 http:// 链接转为 https://，避免微信/HTTPS 页面混合内容拦截。
 * <p>智能协议适配：
 *   - 微信小程序：强制转 HTTPS（微信禁止 HTTP 资源）
 *   - H5 页面 HTTPS：转 HTTPS（避免混合内容被浏览器拦截）
 *   - H5 页面 HTTP：保持原协议（OSS 无 HTTPS 时 HTTP 页面可正常加载）
 *   - 本地回环地址：不转换（开发环境 MinIO 等仅支持 HTTP）
 */
export function toHttps(url) {
    if (!url) return url
    if (typeof url !== 'string') return url
    if (isLocalUrl(url)) return url
    // ★小程序环境无 window.location：强制转 HTTPS（微信禁止 HTTP）
    if (typeof window === 'undefined') return url.replace(/^http:\/\//i, 'https://')
    // ★H5 环境：仅当页面本身是 HTTPS 时才转换，避免 HTTP 页面 + HTTP OSS 被误转
    if (window.location.protocol === 'https:') return url.replace(/^http:\/\//i, 'https://')
    return url
}

/** ========== 头像多档尺寸切换（T26 头像优化） ========== */

/**
 * 头像 URL 协议修正（T26 头像优化 · 20261017 修正）
 * <p>sys_user.avatar 存储的 URL 即为正确档位的 OSS 地址（上传时 AvatarImageProcessor
 *   已将 md 档 URL 入库），前端不应再做 _md→_sm 转换——_sm 文件可能因上传降级而不存在，
 *   强行转换会导致 404 空白头像。
 * <p>本函数仅做 http→https 协议修正，保证微信/HTTPS 页面能正常加载。
 *
 * @param {string} url 存储的头像 URL
 * @param {'sm'|'md'|'lg'} [_size] 保留参数签名兼容旧调用，实际不再转换档位
 * @returns {string} 协议修正后的 URL（原样返回）
 */
export function avatarSize(url, _size) {
    return toHttps(url)
}

/** ========== 多图字段解析（V3.9 §一.3 活动海报 1~6 张，逗号分隔存储） ========== */

/**
 * 判断 token 是否为可渲染的图片地址。
 * <p>纯数字 token 是未翻译的 ossId（后端翻译 fail-soft 或漏翻译接口返回原值），
 *   直接作 image src 会 404 空白，展示层统一过滤后回落默认封面。
 */
export function isImageUrl(url) {
    return !!url && typeof url === 'string' && !/^\d+$/.test(url.trim())
}

/** 多图串拆为数组（过滤空 token 与未翻译的纯数字 ossId）；非字符串/空值返回空数组
 *  <p>兼容任意分隔符：后端 selectUrlByIds 拼接符（~~/～～ 等）与 PC 端逗号存储
 */
export function splitImages(value) {
    if (!value || typeof value !== 'string') return []
    // ★优先正则提取所有 http(s) URL：URL 本身不含空格/逗号/波浪号，故遇分隔符即截断，对任意分隔符均健壮
    const urls = value.match(/https?:\/\/[^\s,，~～]+/g)
    if (urls && urls.length) return urls.filter(isImageUrl)
    // 回落：ossId 逗号串或单值
    return value.split(',').map(s => s.trim()).filter(s => isImageUrl(s))
}

/** 取逗号分隔多图串的首图（列表卡封面展示用）；无图返回空串 */
export function firstImage(value) {
    const list = splitImages(value)
    return list.length ? list[0] : ''
}

/**
 * ★活动是否已结束（与后端 PromotionActivityServiceImpl#isActivityEnded 同口径）：
 *   ① status=ENDED；② endTime 已过；③ endTime 缺失时回落 startTime 已过（时点型活动，如按发车时间的大巴游）。
 * <p>已结束活动不得出现在顾客首页/浏览页「最新活动」，归入「我的活动 → 已结束」Tab。
 */
export function isActivityEnded(item) {
    if (!item) return false
    if (item.status === 'ENDED') return true
    const now = Date.now()
    if (item.endTime) return now > new Date(item.endTime).getTime()
    return !!item.startTime && now > new Date(item.startTime).getTime()
}

/** ★活动是否被 Boss 锁定（is_locked 为运行时权威标志）：锁定活动对客只读，文案「活动已锁定」/按钮「查看活动」 */
export function isActivityLocked(item) {
    return !!(item && item.isLocked)
}
