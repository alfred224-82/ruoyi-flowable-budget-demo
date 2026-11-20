import App from './App'

// #ifdef H5
/**
 * 开发模式 SW 自愈：浏览器残留的旧 Service Worker 会拦截 /prod-api/* 并返回其旧缓存（HTML 网页）响应，
 * 导致「服务器正常但浏览器永远拿到网页」的静默污染（暂无门店/接口返回网页而非数据 根因）。
 * dev 启动时自动注销全部 SW 并清空缓存；若当前页正被旧 SW 控制，清理后自动重载一次让请求直连网络。
 * 生产环境按 index.html 注册新版 sw.js（CACHE_VERSION 升级强制旧缓存失效 + 仅缓存 JSON）。
 */
if (import.meta.env.DEV && typeof navigator !== 'undefined' && navigator.serviceWorker) {
    console.log('[sw-clean] dev 启动，当前页 SW 控制态=', !!navigator.serviceWorker.controller)
    const cleanSw = () => Promise.all([
        navigator.serviceWorker.getRegistrations().then((regs) => {
            console.log('[sw-clean] 发现 SW 注册=', regs.length, regs.map((r) => r.scope))
            return Promise.all(regs.map((r) => r.unregister()))
        }),
        (typeof caches !== 'undefined'
            ? caches.keys().then((keys) => {
                console.log('[sw-clean] 发现缓存=', keys)
                return Promise.all(keys.map((k) => caches.delete(k)))
            })
            : Promise.resolve())
    ])
    if (navigator.serviceWorker.controller) {
        cleanSw().then(() => {
            console.log('[sw-clean] 旧 SW 控制中，清理完成自动重载一次')
            window.location.reload()
        })
    } else {
        cleanSw().then(() => console.log('[sw-clean] 清理完成，无 SW 控制'))
    }
}
// #endif

// #ifndef VUE3
import Vue from 'vue'
Vue.config.productionTip = false
App.mpType = 'app'
const app = new Vue(App)
app.$mount()
// #endif

// #ifdef VUE3
import { createSSRApp } from 'vue'
export function createApp() {
    const app = createSSRApp(App)
    return { app }
}
// #endif
