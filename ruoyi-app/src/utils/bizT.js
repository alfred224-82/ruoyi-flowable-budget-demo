/**
 * 文案映射 helper（T10.5 i18n，小微版）
 * <p>
 * 三维合并优先级：租户 module_overrides.copy > biz_copy(biz_code,lang) > biz_bundle.copy > 前端硬编码兜底。
 * <p>
 * 用法：
 *   import { bizT, initBizCopy } from '../../utils/bizT'
 *   // App.vue onLaunch 时初始化（登录后调一次即可）
 *   await initBizCopy()
 *   // 任意页面使用
 *   <text>{{ bizT('booking.submit_btn', '提交预约') }}</text>
 *
 * @author ruoyi
 * @date 2026-09-16
 */
import { get } from './request'
import config from '../config'

/** 内存缓存：合并后的文案映射表 { key: value } */
let _copyMap = {}

/**
 * 初始化文案映射（登录后调用，从后端拉取合并后的 copy 表）
 */
export async function initBizCopy() {
    try {
        const res = await get(config.api.microBizCopyAll)
        const list = (res && res.data) || []
        _copyMap = {}
        if (Array.isArray(list)) {
            list.forEach(item => {
                if (item.copyKey && item.copyValue) {
                    _copyMap[item.copyKey] = item.copyValue
                }
            })
        }
    } catch (e) {
        // 拉取失败不阻塞主流程，回落硬编码兜底
        console.warn('[bizT] initBizCopy 失败，回落硬编码兜底')
    }
}

/**
 * 取文案：优先 biz_copy 映射表命中，否则返回 fallback 硬编码
 *
 * @param {string} key     文案 key（如 'booking.submit_btn'）
 * @param {string} fallback 兜底文案（硬编码中文）
 * @returns {string}
 */
export function bizT(key, fallback) {
    if (!key) return fallback || ''
    return _copyMap[key] || fallback || key
}

/**
 * 手动设置文案映射（用于离线/测试场景）
 */
export function setBizCopy(map) {
    _copyMap = Object.assign({}, map || {})
}
