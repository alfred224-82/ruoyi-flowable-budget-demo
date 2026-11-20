import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import fs from 'fs'
import path from 'path'
import http from 'http'

/**
 * API 代理插件（enforce: 'pre' 确保在 uni 插件 SPA fallback 之前运行）
 * 解决 @dcloudio/vite-plugin-uni 的 catch-all 中间件拦截 /prod-api 请求的问题
 */
function apiProxyPlugin() {
  return {
    name: 'api-proxy',
    enforce: 'pre',
    configureServer(server) {
      server.middlewares.use('/prod-api', (req, res, next) => {
        // 去掉 /prod-api 前缀（req.url 已包含 query string）
        const targetPath = req.url.replace(/^\/prod-api/, '') || '/'
        const options = {
          hostname: 'localhost',
          port: 8080,
          path: targetPath,
          method: req.method,
          headers: { ...req.headers, host: 'localhost:8080' }
        }
        const proxyReq = http.request(options, (proxyRes) => {
          // API 响应强制 no-store：无 Cache-Control 时浏览器会按 etag/last-modified 启发式缓存，
          // 曾导致旧响应（nginx HTML）数小时内冒充接口数据，此处从源头铲除
          res.writeHead(proxyRes.statusCode, {
            ...proxyRes.headers,
            'cache-control': 'no-store, no-cache, must-revalidate',
            'pragma': 'no-cache'
          })
          proxyRes.pipe(res, { end: true })
        })
        proxyReq.on('error', (err) => {
          console.error('[api-proxy] Error:', err.message)
          res.writeHead(502, { 'Content-Type': 'application/json' })
          res.end(JSON.stringify({ code: 500, msg: '后端服务不可用: ' + err.message }))
        })
        req.pipe(proxyReq, { end: true })
      })
    }
  }
}

/**
 * PWA 总开关（★当前默认关闭 · 排查“手机跑旧缓存”问题）
 *
 * 现状：为彻底排除 Service Worker / HTTP 缓存对手机端的污染，PWA 自 2026-10-25 起默认关闭：
 *       生产构建不再注入 manifest 与 SW 注册脚本，改为注入「注销 SW + 清空 caches」
 *       清理脚本，把已访问设备上残留的 Service Worker 卸载干净。
 *       验证完毕需重新启用时，构建设置环境变量 VITE_PWA_ENABLED=true 即可，无需改代码。
 *
 * 启用前置条件（部署侧务必确认，否则 PWA 会静默失效或引发缓存污染）：
 *   1) 生产环境已配置 HTTPS（Service Worker 仅在安全上下文注册）✓
 *   2) Nginx 已把 /h5/ 映射到 dist/build/h5/，并给 .webmanifest 设置
 *      Content-Type: application/manifest+json，给 sw.js / index.html 设置 no-store（见部署 nginx.conf）
 *   3) public/sw.js 里的 CACHE_VERSION 已按当前发布节奏 +1
 *   4) 后端 oss.endpoint 不再返回 localhost / 127.0.0.1（否则 SW Network-First
 *      仍会把错误 URL 呈现给页面）
 *
 * ★扫码进活动详情白名单不受影响：扫码 URL 为 hash 路由（#/pages/...?scene=），
 *   实际 HTTP 请求是 /h5/；匿名接口 /micro/activity/browse、/micro/activity/signupDetail、/login/visitorLogin
 *   走 /prod-api/*，无 SW 时本就是纯网络请求，白名单语义完全保留。
 */
const PWA_ENABLED = String(process.env.VITE_PWA_ENABLED || 'false').toLowerCase() === 'true'

// public/ 资源插件：uni-app H5 不自动处理 public/ 目录，需手动复制（PWA 资源 + 第三方 SDK 等）
function pwaAssetsPlugin() {
  const srcDir = path.resolve(__dirname, 'public')
  const mimeTypes = {
    '.webmanifest': 'application/manifest+json',
    '.js': 'application/javascript',
    '.png': 'image/png'
  }

  return {
    name: 'pwa-assets',
    // 开发模式：通过中间件直接 serve public/ 下的任意文件（PWA 资源等）
    configureServer(server) {
      server.middlewares.use((req, res, next) => {
        const url = req.url || ''
        // 匹配 /h5/ 前缀下的 public 文件（不含子目录，防穿越）
        const match = url.match(/^\/h5\/([^/?#]+)$/)
        if (match) {
          const file = match[1]
          const filePath = path.join(srcDir, file)
          if (fs.existsSync(filePath) && fs.statSync(filePath).isFile()) {
            const ext = path.extname(file)
            res.setHeader('Content-Type', mimeTypes[ext] || 'application/octet-stream')
            fs.createReadStream(filePath).pipe(res)
            return
          }
        }
        next()
      })
    },
    // HTML 占位符替换：按 PWA_ENABLED 开关决定注入内容。
    // 开关打开（VITE_PWA_ENABLED=true）：注入 <link rel="manifest"> 与幂等 SW 注册脚本；
    //           开发模式仍不注册（localhost:3000 频繁热更新，SW 会污染 dev 请求）。
    // 开关关闭（当前默认）：manifest 占位符替换为空；SW 占位符注入「注销 SW + 清 caches」
    //           清理脚本——已访问过的手机残留旧 SW，仅停止注册无法卸载它们。
    transformIndexHtml(html) {
      const isBuild = process.env.NODE_ENV === 'production'
      const manifestLink = (PWA_ENABLED && isBuild)
        ? '<link rel="manifest" href="/h5/manifest.webmanifest" />'
        : ''
      const swScript = (PWA_ENABLED && isBuild)
        ? `<script>
    // PWA 启用：注册 /h5/sw.js（scope=/h5/），只在 HTTPS 或 localhost 下才会成功
    (function() {
      if (!('serviceWorker' in navigator)) return;
      window.addEventListener('load', function() {
        navigator.serviceWorker.register('/h5/sw.js', { scope: '/h5/' })
          .then(function(reg) { console.log('[PWA] SW registered, scope=', reg.scope); })
          .catch(function(err) { console.warn('[PWA] SW register failed:', err); });
      });
    })();
  </script>`
        : (isBuild
          ? `<script>
    // PWA 已关闭：注销任何残留 Service Worker 并清空全部 caches，回退纯浏览器 HTTP 行为。
    // 当前页仍被旧 SW 控制时自动重载一次（sessionStorage 防循环），重载后即为无 SW 干净环境。
    (function() {
      if (!('serviceWorker' in navigator)) return;
      var clean = function() {
        navigator.serviceWorker.getRegistrations().then(function(regs) {
          regs.forEach(function(r) { r.unregister(); });
        });
        if (window.caches) caches.keys().then(function(ks) { ks.forEach(function(k) { caches.delete(k); }); });
      };
      if (navigator.serviceWorker.controller && !sessionStorage.getItem('sw-killed')) {
        sessionStorage.setItem('sw-killed', '1');
        clean();
        window.location.reload();
        return;
      }
      clean();
    })();
  </script>`
          : '')
      return html
        .replace('<!-- %PWA_MANIFEST% -->', manifestLink)
        .replace('<!-- %PWA_SW% -->', swScript)
    },
    // 构建模式：构建完成后把 public/ 下全部文件复制到输出目录（PWA 资源 + 第三方库）
    closeBundle() {
      const destDir = path.resolve(__dirname, 'dist/build/h5')
      if (!fs.existsSync(destDir)) fs.mkdirSync(destDir, { recursive: true })
      if (!fs.existsSync(srcDir)) return

      fs.readdirSync(srcDir).forEach(file => {
        const srcPath = path.join(srcDir, file)
        if (!fs.statSync(srcPath).isFile()) return
        fs.copyFileSync(srcPath, path.join(destDir, file))
        console.log(`[public] Copied ${file} to dist/build/h5/`)
      })
    }
  }
}

export default defineConfig({
  base: '/h5/',
  plugins: [apiProxyPlugin(), uni(), pwaAssetsPlugin()],
  server: {
    port: 3000
  }
})
