/* ============================================================
 * Service Worker — 【已关闭】正常缓存策略版本（保留供重新启用）
 *
 * 当前状态：
 *   ★PWA 已默认关闭（vite.config.js 中 PWA_ENABLED 默认 false）：新构建不再注册本文件，
 *   且 index.html 会注入清理脚本注销已访问设备上的残留 SW；
 *   重新启用：构建时设 VITE_PWA_ENABLED=true。
 *
 * 策略（吸取历史踩坑经验）：
 *   - 导航请求（HTML）      → Network-First（★v7 起 bypass HTTP 缓存），离线时回退缓存
 *   - /prod-api/*           → Network-First（★v7 起 no-store 完全绕过 HTTP 缓存），绝不写缓存
 *                             （避免旧响应污染 API；也保证扫码进活动详情的匿名白名单接口总是实时拉取）
 *   - /oss/*（MinIO 图片）  → Network-First（历史踩坑：Cache-First 首次遇到 403/404
 *                              会把错误响应写死到缓存，导致图片永远打不开）
 *   - manifest.webmanifest  → Network-First，不预缓存（历史踩坑：预缓存后浏览器
 *                              可能长期读到旧内容 / 错误内容）
 *   - 版本化静态资源         → Cache-First（/h5/assets/*.js|css、字体、icon-*.png）
 *
 * ★v7 变更（修复“手机旧版 / PC 正常”差异）：
 *   iOS/微信会对未设 Cache-Control 的 index.html 做启发式缓存 → 旧 HTML 引用旧 hash JS
 *   → 本 SW 对 assets 的 Cache-First 命中旧包 → 手机永远跑修复前代码。
 *   故导航请求 no-cache 重新验证、API 请求 no-store、预缓存 bypass HTTP 缓存，
 *   并配合 nginx 对 /h5/index.html 设 no-store（见部署 nginx.conf）。
 *
 * 升级方式：改动本文件任何逻辑时，必须同步 +1 CACHE_VERSION，
 *           老客户端 activate 阶段会自动清理旧版本 cache。
 * ============================================================ */

const CACHE_VERSION = 'v7'
const STATIC_CACHE = `static-${CACHE_VERSION}`
const RUNTIME_CACHE = `runtime-${CACHE_VERSION}`

// 预缓存列表：只放确定不会变的入口文件；不放 manifest / API / OSS 图片
const PRE_CACHE_URLS = [
  '/h5/',
  '/h5/index.html'
]

self.addEventListener('install', (event) => {
  event.waitUntil(
    caches.open(STATIC_CACHE)
      // ★预缓存 bypass HTTP 缓存：避免把启发式缓存里的旧 index.html 写进预缓存
      .then((cache) => Promise.all(PRE_CACHE_URLS.map((u) =>
        fetch(u, { cache: 'reload' }).then((r) => cache.put(u, r)).catch(() => { /* 单条失败不阻塞安装 */ })
      )))
      .then(() => self.skipWaiting())
  )
})

self.addEventListener('activate', (event) => {
  event.waitUntil(
    (async () => {
      // 清理旧版本 cache
      const keys = await caches.keys()
      await Promise.all(
        keys
          .filter((k) => !k.endsWith(CACHE_VERSION))
          .map((k) => caches.delete(k))
      )
      await self.clients.claim()
    })()
  )
})

/** 判断是否应走 Network-First（不写缓存 / 缓存仅作离线兜底） */
function isNetworkFirst(url) {
  const path = url.pathname
  return (
    path.startsWith('/prod-api/') ||
    path.startsWith('/oss/') ||
    path.endsWith('/manifest.webmanifest') ||
    url.origin !== self.location.origin // 跨源一律 Network-First，避免污染
  )
}

/** 判断是否为导航请求（HTML 页面） */
function isNavigation(request, url) {
  if (request.mode === 'navigate') return true
  // 兼容部分浏览器：接受 text/html 且路径无扩展名
  const accept = request.headers.get('accept') || ''
  return accept.includes('text/html') && !/\.[a-zA-Z0-9]+$/.test(url.pathname)
}

self.addEventListener('fetch', (event) => {
  const { request } = event
  if (request.method !== 'GET') return

  const url = new URL(request.url)

  // 导航请求：Network-First，离线回退 /h5/index.html
  if (isNavigation(request, url)) {
    event.respondWith(
      // ★no-cache 重新验证：nginx 若漏配 index.html no-store，iOS/微信会启发式缓存旧 HTML，
      //   导致手机长期加载旧版 JS  bundle（症状：新功能 PC 正常、手机不生效）
      fetch(request, { cache: 'no-cache' })
        .then((resp) => {
          const copy = resp.clone()
          caches.open(RUNTIME_CACHE).then((c) => c.put('/h5/index.html', copy)).catch(() => {})
          return resp
        })
        .catch(() => caches.match('/h5/index.html').then((r) => r || Response.error()))
    )
    return
  }

  // API / OSS / manifest / 跨源：Network-First，成功不写缓存（避免历史踩坑）
  if (isNetworkFirst(url)) {
    // ★API 完全绕过 HTTP 缓存（no-store）：防旧接口响应（如配置座位图前的 seat/map）
    //   被手机启发式缓存导致移动端与 PC 数据不一致；/oss 与跨源保持 no-cache 重新验证
    const cacheMode = url.pathname.startsWith('/prod-api/') ? 'no-store' : 'no-cache'
    event.respondWith(
      fetch(new Request(request, { cache: cacheMode }))
        .catch(() => caches.match(request).then((r) => r || Response.error()))
    )
    return
  }

  // 同源静态资源（版本化 JS/CSS/字体/icon）：Cache-First
  event.respondWith(
    caches.match(request).then((cached) => {
      if (cached) return cached
      return fetch(request).then((resp) => {
        if (resp && resp.status === 200 && resp.type === 'basic') {
          const copy = resp.clone()
          caches.open(STATIC_CACHE).then((c) => c.put(request, copy)).catch(() => {})
        }
        return resp
      })
    })
  )
})
