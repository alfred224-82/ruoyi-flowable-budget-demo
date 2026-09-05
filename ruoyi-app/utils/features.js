/**
 * 版本能力开关（防串第③层：前端动态渲染，T4.3/T8.5）
 * 登录后调用 /system/edition/features 获取当前租户开关集；
 * 未解锁的能力入口不显示（后端 @RequireFeature 仍为最终防线）。
 */
import { get } from './request'
import config from '../config'

const FEATURES_KEY = 'app_edition_features'
const EDITION_KEY = 'app_edition'

/**
 * 直接用 /getInfo 下发的能力开关初始化（登录时调用，少一次请求）
 */
export function setEditionFeatures(features, edition) {
    uni.setStorageSync(FEATURES_KEY, features || [])
    uni.setStorageSync(EDITION_KEY, edition || 'BASIC')
}

/** 拉取并缓存当前租户版本能力 */
export function loadEditionFeatures() {
    return get(config.api.editionFeatures).then(res => {
        const data = res.data || {}
        const features = data.features || []
        uni.setStorageSync(FEATURES_KEY, features)
        uni.setStorageSync(EDITION_KEY, data.edition || 'BASIC')
        return features
    }).catch(() => {
        // 接口失败时按基础版能力兜底（最小集），由后端拦截保底
        uni.setStorageSync(FEATURES_KEY, [])
        return []
    })
}

/** 判断某个能力开关是否开启（如 FEAT_TIME_CLOCK） */
export function hasFeature(code) {
    const features = uni.getStorageSync(FEATURES_KEY) || []
    return features.includes(code)
}

/** 获取当前版本编码（BASIC/ADVANCED/PROFESSIONAL） */
export function getEdition() {
    return uni.getStorageSync(EDITION_KEY) || 'BASIC'
}
