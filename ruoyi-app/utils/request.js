/**
 * 统一请求封装（JWT：Authorization 头；401 回登录页；租户到期提示透传）
 */
import config from '../config'
import { getToken, clearAuth } from './auth'

const request = (options) => {
    return new Promise((resolve, reject) => {
        const token = getToken()
        uni.request({
            url: config.baseUrl + options.url,
            method: options.method || 'GET',
            data: options.data || {},
            timeout: config.timeout,
            header: {
                'Content-Type': 'application/json;charset=utf-8',
                ...(token ? { Authorization: 'Bearer ' + token } : {}),
                ...(options.header || {})
            },
            success: (res) => {
                const body = res.data || {}
                // 框架统一响应：{ code, msg, data }；200=成功
                if (res.statusCode === 401 || body.code === 401) {
                    clearAuth()
                    return reject(new Error('登录已过期，请重新登录'))
                }
                if (body.code !== undefined && body.code !== 200) {
                    // 租户到期/停用（T3.4）、功能未授权（防串②层）等错误直接提示
                    uni.showToast({ title: body.msg || '请求失败', icon: 'none' })
                    return reject(new Error(body.msg || '请求失败'))
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

export const get = (url, data) => request({ url, method: 'GET', data })
export const post = (url, data) => request({ url, method: 'POST', data })
export const put = (url, data) => request({ url, method: 'PUT', data })
export const del = (url, data) => request({ url, method: 'DELETE', data })

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
