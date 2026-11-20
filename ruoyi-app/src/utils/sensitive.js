/**
 * 敏感词前端预检工具（T32.8，设计方案 §十二 客诉预防）
 * ------------------------------------------------------------------
 * 前端预检 + 服务端终检（前端只为即时反馈，服务端是权威）。
 *
 * 使用方式：
 *   import { checkSensitive, buildHitMessage } from '@/utils/sensitive'
 *
 *   // 单段文本预检（防抖后调用）
 *   const res = await checkSensitive('稳赚不赔，快来参与', { bizType: 'complaint' })
 *   if (!res.pass) {
 *     uni.showToast({ title: buildHitMessage(res.hitWords), icon: 'none' })
 *     // 按钮置灰
 *   }
 *
 *   // 表单多字段联合预检
 *   const res = await checkSensitiveBatch([
 *     { field: 'title', text: form.title, bizType: 'activity' },
 *     { field: 'content', text: form.content, bizType: 'activity' }
 *   ])
 *
 * 设计要点：
 *   ① 二档（通过 / 不通过），不做「警告但可提交」中间档；
 *   ② 命中即禁止提交，按钮置灰，提示必须指出具体是哪个词；
 *   ③ 变体归一化由后端引擎统一处理（繁简/全角/谐音），前端不重复实现；
 *   ④ 防抖 400ms，避免每次按键都发请求；
 *   ⑤ FEAT_SENSITIVE_WORD 关闭时后端直接 PASS，前端无感降级。
 */
import { post } from './request'
import config from '../config'

/** 防抖计时器（按 key 隔离，支持多输入框并行） */
const debounceTimers = {}

/** 默认防抖延迟（毫秒） */
const DEFAULT_DEBOUNCE = 400

/**
 * 单段文本敏感词预检（带防抖）
 *
 * @param {String} text 待检文本
 * @param {Object} [opts]
 * @param {String} [opts.bizType='other'] 业务类型：complaint/activity/store/rating/other
 * @param {String} [opts.bizId]           业务主键（可空）
 * @param {String} [opts.key='default']   防抖 key（多输入框场景区分）
 * @param {Number} [opts.debounce=400]    防抖延迟（毫秒），0=立即执行
 * @returns {Promise<{pass:Boolean, hitWords:Array, raw:Object}>}
 *          pass=true 放行；pass=false 命中禁提交，hitWords=[{word,category,position}]
 */
export function checkSensitive(text, opts = {}) {
    const {
        bizType = 'other',
        bizId = '',
        key = 'default',
        debounce = DEFAULT_DEBOUNCE
    } = opts

    // 空文本直接放行
    if (!text || !String(text).trim()) {
        return Promise.resolve({ pass: true, hitWords: [], raw: null })
    }

    const doCheck = () => {
        return post(config.api.microSensitiveCheck, {
            text: String(text),
            bizType,
            bizId
        }).then(res => {
            const data = (res && res.data) || {}
            const hitWords = Array.isArray(data.hitWords) ? data.hitWords : []
            return {
                pass: data.result !== 'FAIL',
                hitWords,
                raw: data
            }
        }).catch(() => {
            // 网络/接口异常 fail-soft：放行，由服务端终检兜底
            return { pass: true, hitWords: [], raw: null }
        })
    }

    if (!debounce || debounce <= 0) {
        return doCheck()
    }

    return new Promise(resolve => {
        if (debounceTimers[key]) clearTimeout(debounceTimers[key])
        debounceTimers[key] = setTimeout(() => {
            delete debounceTimers[key]
            doCheck().then(resolve)
        }, debounce)
    })
}

/**
 * 多字段联合预检（任一字段命中即整体 FAIL）
 *
 * @param {Array<{field:String, text:String, bizType?:String, bizId?:String}>} fields
 * @param {Object} [opts] 同 checkSensitive
 * @returns {Promise<{pass:Boolean, hitWords:Array, hitsByField:Object}>}
 */
export async function checkSensitiveBatch(fields, opts = {}) {
    if (!Array.isArray(fields) || fields.length === 0) {
        return { pass: true, hitWords: [], hitsByField: {} }
    }
    const hitsByField = {}
    const allHits = []
    let pass = true
    for (const f of fields) {
        if (!f || !f.text) continue
        const res = await checkSensitive(f.text, {
            bizType: f.bizType || opts.bizType || 'other',
            bizId: f.bizId || opts.bizId || '',
            key: 'batch_' + (f.field || Math.random()),
            debounce: 0
        })
        if (!res.pass) {
            pass = false
            hitsByField[f.field] = res.hitWords
            allHits.push(...res.hitWords)
        }
    }
    return { pass, hitWords: allHits, hitsByField }
}

/**
 * 构造命中提示文案（按钮置灰时展示）
 * <p>规范：必须指出具体是哪个词，例：「文案包含易引发客诉的用语：稳赚不赔」
 *
 * @param {Array<{word:String, category?:String}>} hitWords
 * @returns {String}
 */
export function buildHitMessage(hitWords) {
    if (!Array.isArray(hitWords) || hitWords.length === 0) {
        return '文案包含敏感词，请修改后重试'
    }
    const words = hitWords.map(h => h && h.word).filter(Boolean)
    if (words.length === 0) {
        return '文案包含敏感词，请修改后重试'
    }
    // 去重保序
    const uniq = [...new Set(words)]
    return '文案包含易引发客诉的用语：' + uniq.join('、')
}

/**
 * 清除指定 key 的防抖计时器（页面卸载时调用，防止内存泄漏）
 * @param {String} [key] 不传则清除全部
 */
export function clearSensitiveDebounce(key) {
    if (key) {
        if (debounceTimers[key]) {
            clearTimeout(debounceTimers[key])
            delete debounceTimers[key]
        }
    } else {
        Object.keys(debounceTimers).forEach(k => {
            clearTimeout(debounceTimers[k])
            delete debounceTimers[k]
        })
    }
}

export default {
    checkSensitive,
    checkSensitiveBatch,
    buildHitMessage,
    clearSensitiveDebounce
}
