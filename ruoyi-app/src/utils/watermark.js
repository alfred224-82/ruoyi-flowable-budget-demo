/**
 * ★图片水印公共工具（share-image-generator / activity-poster 共用）
 * · 水印文字：租户名称 + YYYYMMDDHHmmss
 * · canvas 绘制：45° 斜排、大字体、6 条均布
 * · 两个截图组件调用 overlayWatermark(dataUrl) 即可，保证水印样式统一
 */
import { getTenantName } from './roles'

/**
 * 生成水印文字：{租户名称} {YYYYMMDDHHmmss}
 * @returns {string}
 */
export function buildWatermarkText() {
    const name = getTenantName() || ''
    const now = new Date()
    const pad = n => String(n).padStart(2, '0')
    const ts = now.getFullYear() + pad(now.getMonth() + 1) + pad(now.getDate())
        + pad(now.getHours()) + pad(now.getMinutes()) + pad(now.getSeconds())
    return name ? name + '  ' + ts : ts
}

/**
 * 在已有图片 dataUrl 上叠加水印，返回新的 dataUrl
 * @param {string} srcDataUrl - 原始图片 data URL
 * @param {string} [text] - 水印文字（默认自动取租户名称+时间戳）
 * @returns {Promise<string>} 叠加水印后的 JPEG data URL
 */
export function overlayWatermark(srcDataUrl, text) {
    const watermark = text || buildWatermarkText()
    return new Promise((resolve) => {
        const img = new Image()
        img.onload = () => {
            const c = document.createElement('canvas')
            c.width = img.naturalWidth
            c.height = img.naturalHeight
            const ctx = c.getContext('2d')
            ctx.drawImage(img, 0, 0)

            // ★水印参数：大字体、6 条均布、45° 斜排
            const fontSize = Math.round(c.width * 0.055)   // 约占画布宽 5.5%
            ctx.save()
            ctx.font = `bold ${fontSize}px "PingFang SC", "Microsoft YaHei", sans-serif`
            ctx.fillStyle = 'rgba(0, 0, 0, 0.07)'
            ctx.textAlign = 'center'
            ctx.textBaseline = 'middle'

            const cx = c.width / 2
            const cy = c.height / 2
            // 6 条沿 Y 轴均匀分布（从 15% 到 85% 高度）
            const count = 6
            for (let i = 0; i < count; i++) {
                const py = c.height * (0.15 + 0.7 * i / (count - 1))
                ctx.save()
                ctx.translate(cx, py)
                ctx.rotate(-Math.PI / 4)
                ctx.fillText(watermark, 0, 0)
                ctx.restore()
            }
            ctx.restore()

            resolve(c.toDataURL('image/jpeg', 0.92))
        }
        img.onerror = () => resolve(srcDataUrl)
        img.src = srcDataUrl
    })
}
