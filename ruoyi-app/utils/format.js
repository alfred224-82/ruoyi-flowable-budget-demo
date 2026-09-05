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

/** 日期时间格式化（yyyy-MM-dd HH:mm） */
export function formatDateTime(value) {
    if (!value) return '-'
    const d = new Date(value)
    if (isNaN(d.getTime())) return String(value)
    return formatDate(d) + ' ' + String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0')
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
