/**
 * 三张报表共用格式化与导出工具（M38/M39）
 * <p>差额着色铁律（§8.3）：负=红、零=灰、正=绿，且金额始终带正负号；
 * CSV 导出在 H5 端用 Blob + BOM 触发下载（Excel 中文不乱码），非 H5 端提示到浏览器导出。
 */
import { formatAmount } from './format'

/**
 * 差额着色 class：负 diff-neg（红）/ 零 diff-zero（灰）/ 正 diff-pos（绿）
 * @param {number|string} value 差额
 * @returns {string} class 名
 */
export function diffClass(value) {
    const num = Number(value)
    if (isNaN(num) || num === 0) return 'diff-zero'
    return num < 0 ? 'diff-neg' : 'diff-pos'
}

/**
 * 带正负号金额（供差额列使用）：+1,234.00 / -1,234.00 / 0.00
 * @param {number|string} value 金额
 * @returns {string} 带符号千分位金额
 */
export function signedAmount(value) {
    const num = Number(value)
    if (isNaN(num)) return '0.00'
    if (num > 0) return '+' + formatAmount(num)
    if (num < 0) return '-' + formatAmount(Math.abs(num))
    return '0.00'
}

/**
 * 差额展示文本（符号 + 金额，前端着色由 diffClass 决定）
 * @param {number|string} value 差额
 */
export function diffText(value) {
    return signedAmount(value)
}

/** 数组按 getter 求和（null 视为 0） */
export function sumBy(rows, getter) {
    if (!Array.isArray(rows)) return 0
    return rows.reduce((s, r) => s + (Number(getter(r)) || 0), 0)
}

/**
 * 二维数组转 CSV 文本（自动转义逗号/引号/换行）
 * @param {Array<Array<string|number>>} rows 行数据（首行通常为表头）
 * @returns {string} CSV 文本（不含 BOM）
 */
export function toCsv(rows) {
    if (!Array.isArray(rows)) return ''
    return rows.map(cols => (cols || []).map(cell => {
        const s = cell === null || cell === undefined ? '' : String(cell)
        return /[",\n]/.test(s) ? '"' + s.replace(/"/g, '""') + '"' : s
    }).join(',')).join('\n')
}

/**
 * 导出 CSV（H5：Blob + BOM 下载；非 H5：提示到浏览器导出）
 * @param {string} filename 文件名（含 .csv）
 * @param {string} content CSV 文本
 */
export function exportCsv(filename, content) {
    const BOM = '\uFEFF'
    // #ifdef H5
    try {
        const blob = new Blob([BOM + (content || '')], { type: 'text/csv;charset=utf-8' })
        const url = URL.createObjectURL(blob)
        const a = document.createElement('a')
        a.href = url
        a.download = filename || 'export.csv'
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
        setTimeout(() => URL.revokeObjectURL(url), 1000)
        uni.showToast({ title: '已开始导出', icon: 'none' })
    } catch (e) {
        uni.showToast({ title: '导出失败', icon: 'none' })
    }
    // #endif
    // #ifndef H5
    uni.showToast({ title: '请在浏览器中打开后导出', icon: 'none' })
    // #endif
}

/** 结算状态中文（对齐后端 activity_settlement.status） */
export function settleStatusText(status, settled) {
    if (settled === false) return '未结算'
    if (status === 'LOCKED') return '已锁定'
    if (status === 'SUBMITTED') return '已提交'
    if (status === 'DRAFT') return '草稿'
    return status || '-'
}

/** 保本进度条宽度（%，封顶 100 供进度条渲染） */
export function progressWidth(percent) {
    const num = Number(percent)
    if (isNaN(num) || num < 0) return 0
    return num > 100 ? 100 : num
}
