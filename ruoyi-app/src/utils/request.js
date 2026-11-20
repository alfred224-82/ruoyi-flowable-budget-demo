/**
 * 统一请求封装（JWT：Authorization 头；401 回登录页；租户到期提示透传）
 */
import config from '../config'
import { getToken, clearAuth } from './auth'

const isH5 = typeof window !== 'undefined' && typeof navigator !== 'undefined'
const isDev = typeof import.meta !== 'undefined' && !!import.meta.env && !!import.meta.env.DEV

/** 定位日志上下文：页面地址 + SW 控制态（「请求未走代理 / 被 SW 拦截」类错配一次看全） */
const diagContext = () => ({
    page: isH5 ? window.location.href : 'mp',
    origin: isH5 ? window.location.origin : 'mp',
    sw: isH5 && navigator.serviceWorker ? !!navigator.serviceWorker.controller : false
})

const request = (options) => {
    return new Promise((resolve, reject) => {
        const token = getToken()
        let fullUrl = config.baseUrl + options.url
        // dev 防缓存：无 Cache-Control 的响应会被浏览器启发式缓存（已发生事故：nginx HTML 被缓存数小时冒充接口响应），
        // dev 模式 GET 请求追加时间戳参数强制走网络
        if (isH5 && isDev && (options.method || 'GET').toUpperCase() === 'GET') {
            fullUrl += (fullUrl.includes('?') ? '&' : '?') + '_cb=' + Date.now()
        }
        const ctx = diagContext()
        // 定位日志①：请求发出前的完整地址与运行环境
        console.log('[request] →', options.method || 'GET', fullUrl, '| page=', ctx.page, '| swControlled=', ctx.sw)
        uni.request({
            url: fullUrl,
            method: options.method || 'GET',
            data: options.data || {},
            timeout: config.timeout,
            header: {
                'Content-Type': 'application/json;charset=utf-8',
                ...(token ? { Authorization: 'Bearer ' + token } : {}),
                ...(options.header || {})
            },
            success: (res) => {
                const body = res.data
                const header = res.header || {}
                const ctype = header['Content-Type'] || header['content-type'] || ''
                // 定位日志②：每次响应的状态码与 Content-Type
                console.log('[request] ←', fullUrl, '| statusCode=', res.statusCode, '| content-type=', ctype, '| typeof body=', typeof body)
                // 防线①：接口必须返回框架 JSON；返回字符串（HTML 页面）说明请求落到前端站点 SPA 兜底（代理/访问地址错配）或被 SW 旧缓存拦截，
                // 绝不允许静默透传，否则调用方把 undefined 当空列表（如「暂无门店」）吞掉真实故障
                if (typeof body === 'string') {
                    console.error('[request] ★接口返回网页而非数据', fullUrl,
                        '| statusCode=', res.statusCode,
                        '| content-type=', ctype,
                        '| page=', ctx.page,
                        '| swControlled=', ctx.sw,
                        '| headers=', JSON.stringify(header),
                        '| bodyHead=', body.slice(0, 300))
                    // 弹窗直出关键定位信息（页面源 vs 请求源是否同源、SW 是否拦截），便于截图反馈
                    uni.showModal({
                        title: '接口诊断',
                        content: '页面源：' + ctx.origin + '\n请求地址：' + fullUrl + '\n状态码：' + res.statusCode + '\nSW拦截：' + (ctx.sw ? '是' : '否') + '\n请将此截图反馈开发',
                        showCancel: false
                    })
                    return reject(new Error('接口返回非 JSON 响应（statusCode=' + res.statusCode + '）'))
                }
                const data = body || {}
                // 框架统一响应：{ code, msg, data }；200=成功
                if (res.statusCode === 401 || data.code === 401) {
                    clearAuth()
                    return reject(new Error('登录已过期，请重新登录'))
                }
                // 防线②：Spring 原生错误（404/500 白牌 JSON）无 code 字段，显式报错避免被当「无数据」静默吞掉
                if (data.code === undefined) {
                    console.error('[request] ★响应缺少 code 字段', fullUrl, '| statusCode=', res.statusCode, '| body=', JSON.stringify(data).slice(0, 300), '| page=', ctx.page)
                    if (!options.silent) {
                        uni.showToast({ title: '接口异常（状态码 ' + res.statusCode + '），请稍后重试', icon: 'none' })
                    }
                    return reject(new Error('接口响应缺少 code 字段（statusCode=' + res.statusCode + '）'))
                }
                if (data.code !== 200) {
                    // 租户到期/停用（T3.4）、功能未授权（防串②层）等错误直接提示
                    uni.showToast({ title: data.msg || '请求失败', icon: 'none' })
                    return reject(new Error(data.msg || '请求失败'))
                }
                resolve(data)
            },
            fail: (err) => {
                // 定位日志③：网络层失败（代理不通/后端未启动/跨域拦截）
                console.error('[request] ★网络失败', fullUrl, '| err=', err && err.errMsg, '| page=', ctx.page, '| swControlled=', ctx.sw)
                uni.showToast({ title: '网络异常，请稍后重试', icon: 'none' })
                reject(err)
            }
        })
    })
}

export const get = (url, data, silent) => request({ url, method: 'GET', data, silent })
export const post = (url, data, silent) => request({ url, method: 'POST', data, silent })
export const put = (url, data, silent) => request({ url, method: 'PUT', data, silent })
export const del = (url, data, silent) => request({ url, method: 'DELETE', data, silent })

/**
 * 文件上传（补票发票图片等，T8.4）
 * uni.uploadFile 响应体为字符串，需自行解析框架统一响应 { code, msg, data }
 */
export const upload = (url, filePath, formData) => {
    return new Promise((resolve, reject) => {
        const token = getToken()
        uni.uploadFile({
            url: config.baseUrl + url,
            filePath,
            name: 'file',
            formData: formData || {},
            timeout: config.timeout,
            header: token ? { Authorization: 'Bearer ' + token } : {},
            success: (res) => {
                let body = {}
                try {
                    body = JSON.parse(res.data)
                } catch (e) {
                    uni.showToast({ title: '上传响应解析失败', icon: 'none' })
                    return reject(new Error('上传响应解析失败'))
                }
                if (res.statusCode === 401 || body.code === 401) {
                    clearAuth()
                    return reject(new Error('登录已过期，请重新登录'))
                }
                if (body.code !== undefined && body.code !== 200) {
                    uni.showToast({ title: body.msg || '上传失败', icon: 'none' })
                    return reject(new Error(body.msg || '上传失败'))
                }
                resolve(body)
            },
            fail: (err) => {
                uni.showToast({ title: '网络异常，请稍后重试', icon: 'none' })
                reject(err)
            }
        })
    })
}

export default request
