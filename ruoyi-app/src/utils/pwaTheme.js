/**
 * H5 PWA 主题动态注入（M27 T27.4）
 * v1.81 已实现 manifest.webmanifest 静态 theme_color；本工具改为运行时按当前租户主题注入：
 * 读取租户主题色板（loadTenantTheme 缓存）→ 更新 <meta name="theme-color"> + 用 Blob 重写 manifest 的
 * theme_color / background_color，使「添加到桌面」的 PWA 外壳色跟随租户主题，与小程序 / PC 三端一致。
 * 仅 H5 生效（条件编译），接口失败 fail-soft 回退默认色板，不阻断页面。
 */
import { getThemeSync, loadTenantTheme } from './theme'

/** 用主题色板更新 meta theme-color（立即生效，影响浏览器地址栏 / 状态栏着色） */
function applyMetaThemeColor(theme) {
    if (typeof document === 'undefined' || !theme) return
    let meta = document.querySelector('meta[name="theme-color"]')
    if (!meta) {
        meta = document.createElement('meta')
        meta.setAttribute('name', 'theme-color')
        document.head.appendChild(meta)
    }
    meta.setAttribute('content', theme.primaryColor || '#4A90E2')
}

/**
 * 【已停用 PWA】manifest 重写逻辑不再执行。
 *
 * 历史原因：旧版本用 Blob URL 重写 link[rel=manifest]，实现租户主题色同步到桌面图标。
 * 副作用：Blob URL 作为 manifest base 时，其中声明的绝对路径（start_url=/h5/、icons.src=/h5/icon-*.png）
 * 会被浏览器解析为 blob:xxx/h5/... 导致 "Manifest: property 'xxx' ignored, URL is invalid" 报错刷屏。
 *
 * PWA 停用后（index.html 不再注入 link[rel=manifest]），本函数直接返回；
 * 保留函数壳避免 App.vue 等调用方修改。
 */
async function injectManifest(theme) {
    // no-op：PWA 已停用，不再重写 manifest
    return
}

/**
 * 应用当前租户主题到 PWA 外壳（meta + manifest）。
 * @param {Object} theme 可选，直接传入色板；缺省时先拉取租户最新主题，回退本地缓存。
 */
export function applyPwaTheme(theme) {
    // #ifdef H5
    if (theme) {
        applyMetaThemeColor(theme)
        injectManifest(theme)
        return Promise.resolve(theme)
    }
    return loadTenantTheme().then(t => {
        const finalTheme = t || getThemeSync()
        applyMetaThemeColor(finalTheme)
        injectManifest(finalTheme)
        return finalTheme
    }).catch(() => {
        const cached = getThemeSync()
        applyMetaThemeColor(cached)
        injectManifest(cached)
        return cached
    })
    // #endif
    // eslint-disable-next-line no-unreachable
    return Promise.resolve(theme || getThemeSync())
}
