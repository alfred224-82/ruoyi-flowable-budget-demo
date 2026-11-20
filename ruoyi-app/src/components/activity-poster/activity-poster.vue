<template>
    <!-- ★M37 活动推广海报生成组件（§十七 T37.1~T37.4）
         · 五要素：活动主视觉 + 标题/时间/地点/价格 + 实时剩余名额 + 二维码（含隐形推广码）+ 行动指令
         · 一人一码：图面一致，仅二维码参数（ref_u）不同 → 扫码自动归因
         · 门店码：不带员工 ID → 扫码计入门店自然流量
         · H5：优先 html-to-image，失败降级 html2canvas；小程序：uni.canvasToTempFilePath
         · 封面转 base64 内嵌避免 canvas 跨域污染；二维码本身为 data URL 无污染 -->
    <view class="ap-wrap">
        <slot name="trigger" :generating="generating" :generate="generate">
            <button class="ap-btn" :class="{ 'is-disabled': generating }" @click="generate">
                {{ generating ? '生成中…' : (btnText || '生成我的海报') }}
            </button>
        </slot>

        <!-- ★H5 海报源节点：放在 scroll-view 外部，避免滚动容器克隆导致内容重复渲染 -->
        <!-- #ifdef H5 -->
        <view id="ap-shot-dom" class="ap-shot ap-shot-offscreen" :style="shotStyle">
            <!-- ① 活动主视觉 -->
            <view class="shot-visual" :style="visualStyle">
                <image v-if="cover" class="shot-cover" :src="cover" mode="aspectFill" />
                <view class="shot-mask"></view>
                <text class="shot-title">{{ data.title || '精彩活动' }}</text>
            </view>
            <!-- ② 标题/时间/地点/价格 -->
            <view class="shot-card">
                <view class="shot-row"><text class="shot-ico">🕐</text><text class="shot-val">{{ data.timeText || '时间待定' }}</text></view>
                <view class="shot-row"><text class="shot-ico">📍</text><text class="shot-val">{{ data.locationText || '门店现场' }}</text></view>
                <view class="shot-row"><text class="shot-ico">💰</text><text class="shot-val">{{ data.priceText || '详询门店' }}</text></view>
                <!-- ③ 实时剩余名额 -->
                <view class="shot-badge" :style="{ background: brandColor }">
                    <text class="shot-badge-text">{{ data.remainingText || '名额充足' }}</text>
                </view>
            </view>
            <!-- ④ 二维码 + ⑤ 行动指令 -->
            <view class="shot-bottom">
                <image v-if="data.qrcodeUrl" class="shot-qr" :src="data.qrcodeUrl" mode="widthFix" />
                <view class="shot-cta">
                    <text class="shot-cta-main">{{ data.actionText || '扫码看详情并报名' }}</text>
                    <text class="shot-cta-sub">{{ data.ownerType || '' }}</text>
                    <text class="shot-cta-name">{{ data.ownerName || '' }}</text>
                </view>
            </view>
            <text class="shot-foot">{{ data.footer || '由「小账本」生成' }}</text>
            <!-- ★水印：租户名称+时间戳，45°斜排6条 -->
            <view class="shot-watermark">
                <text class="wm-line">{{ getWatermarkText() }}</text>
                <text class="wm-line">{{ getWatermarkText() }}</text>
                <text class="wm-line">{{ getWatermarkText() }}</text>
                <text class="wm-line">{{ getWatermarkText() }}</text>
                <text class="wm-line">{{ getWatermarkText() }}</text>
                <text class="wm-line">{{ getWatermarkText() }}</text>
            </view>
        </view>
        <!-- #endif -->

        <view v-if="visible" class="ap-mask" @click="close">
            <view class="ap-panel" @click.stop>
                <view class="ap-head">
                    <text class="ap-title">{{ codeType === 'store' ? '门店推广码' : '我的推广海报' }}</text>
                    <text class="ap-close" @click="close">×</text>
                </view>
                <scroll-view class="ap-body" scroll-y>
                    <!-- 小程序：Canvas 2D 绘制 -->
                    <!-- #ifndef H5 -->
                    <canvas class="ap-canvas" canvas-id="apCanvas" id="apCanvas"
                            :style="{ width: canvasW + 'px', height: canvasH + 'px' }"></canvas>
                    <!-- #endif -->

                    <!-- 生成结果预览 -->
                    <view v-if="resultUrl" class="ap-preview">
                        <image class="ap-preview-img" :src="resultUrl" mode="widthFix" show-menu-by-longpress />
                        <text class="ap-preview-meta">{{ previewMeta }}</text>
                    </view>
                    <text class="ap-tip">{{ tipText }}</text>
                </scroll-view>
                <view class="ap-actions">
                    <!-- #ifdef H5 -->
                    <button v-if="canNativeShare" class="ap-act primary" @click="nativeShare">分享图片</button>
                    <!-- #endif -->
                    <button class="ap-act" @click="saveImage">{{ saveBtnText }}</button>
                    <button class="ap-act gray" @click="close">关闭</button>
                </view>
            </view>
        </view>
    </view>
</template>

<script>
import { toHttps } from '../../utils/format'
import { overlayWatermark, buildWatermarkText } from '../../utils/watermark'

/** 四主题品牌色（对齐 theme_tokens.css） */
const THEME_COLORS = {
    theme_fresh: '#4F8FF7', theme_vivid: '#F5455C', theme_dark: '#5B8FFF', theme_handdrawn: '#FFB347',
    t1: '#4F8FF7', t2: '#F5455C', t3: '#5B8FFF', t4: '#FFB347'
}
const THEME_GRADIENT = {
    theme_fresh: 'linear-gradient(135deg,#4F8FF7,#7AB8FF)', theme_vivid: 'linear-gradient(135deg,#F5455C,#FF7A45)',
    theme_dark: 'linear-gradient(135deg,#5B8FFF,#00E5FF)', theme_handdrawn: 'linear-gradient(135deg,#FFB347,#FF8A8A)',
    t1: 'linear-gradient(135deg,#4F8FF7,#7AB8FF)', t2: 'linear-gradient(135deg,#F5455C,#FF7A45)',
    t3: 'linear-gradient(135deg,#5B8FFF,#00E5FF)', t4: 'linear-gradient(135deg,#FFB347,#FF8A8A)'
}

/** URL → base64（H5，避免 canvas 跨域污染） */
function urlToBase64(url) {
    return new Promise((resolve) => {
        if (!url) return resolve('')
        if (url.startsWith('data:')) return resolve(url)
        // #ifdef H5
        const img = new Image()
        img.crossOrigin = 'anonymous'
        img.onload = () => {
            try {
                const c = document.createElement('canvas')
                c.width = img.naturalWidth
                c.height = img.naturalHeight
                c.getContext('2d').drawImage(img, 0, 0)
                resolve(c.toDataURL('image/jpeg', 0.9))
            } catch (e) { resolve('') }
        }
        img.onerror = () => resolve('')
        img.src = toHttps(url)
        // #endif
        // #ifndef H5
        resolve(toHttps(url))
        // #endif
    })
}

export default {
    name: 'ActivityPoster',
    props: {
        // 海报数据（后端 /micro/poster/data 返回）
        data: { type: Object, default: () => ({}) },
        // 码类型：staff=一人一码 / store=门店码
        codeType: { type: String, default: 'staff' },
        // 活动ID（日志用）
        activityId: { type: [String, Number], default: '' },
        // 按钮文案
        btnText: { type: String, default: '' }
    },
    data() {
        return {
            visible: false,
            generating: false,
            resultUrl: '',
            resultFile: null,
            usedLib: '',
            previewMeta: '',
            tipText: '',
            canNativeShare: false,
            cover: '',
            canvasW: 340,
            canvasH: 544
        }
    },
    computed: {
        brandColor() { return THEME_COLORS[this.data.themeKey] || '#4F8FF7' },
        visualStyle() {
            return this.cover ? {} : { background: THEME_GRADIENT[this.data.themeKey] || THEME_GRADIENT.theme_fresh }
        },
        shotStyle() { return { '--ap-brand': this.brandColor } },
        saveBtnText() {
            // #ifdef H5
            const inWx = typeof navigator !== 'undefined' && /MicroMessenger/i.test(navigator.userAgent)
            return inWx ? '长按图片保存' : '保存 / 下载图片'
            // #endif
            // #ifndef H5
            return '保存到相册'
            // #endif
        }
    },
    methods: {
        /** ★水印文字：租户名称 + 年月日时分秒（method 而非 computed，保证每次截图取实时时间） */
        getWatermarkText() {
            return buildWatermarkText()
        },
        async generate() {
            if (this.generating) return
            if (!this.data || !this.data.title) {
                uni.showToast({ title: '海报数据未就绪', icon: 'none' })
                return
            }
            this.visible = true
            this.generating = true
            this.resultUrl = ''
            this.tipText = '正在生成…'
            try {
                // 封面预转 base64（避免 canvas 跨域污染）
                this.cover = await urlToBase64(this.data.coverUrl || this.data.bgImageUrl || '')
                // #ifdef H5
                await this.$nextTick()
                await this.generateH5()
                // #endif
                // #ifndef H5
                await this.$nextTick()
                await this.generateMp()
                // #endif
                this.$emit('logged', { activityId: this.activityId, codeType: this.codeType, at: Date.now(), lib: this.usedLib })
            } catch (e) {
                this.tipText = '生成失败：' + (e && e.message ? e.message : e)
            } finally {
                this.generating = false
            }
        },
        // #ifdef H5
        async generateH5() {
            const el = document.getElementById('ap-shot-dom')
            if (!el) throw new Error('未找到海报节点')
            // ★截图前用内联样式精确覆盖：left:0 抵消 CSS 类的 -9999px
            el.style.cssText = 'position:fixed;left:0;top:0;z-index:-1;width:90vw;max-width:680px;'
            let dataUrl = ''
            try {
            // ★html-to-image 克隆时排除水印节点，避免 absolute 覆盖层干扰布局
            const skipWatermark = (node) => !node.classList || !node.classList.contains('shot-watermark')
            // ① 优先 html-to-image
            try {
                const htmlToImage = window.htmlToImage || window.__htmlToImage
                if (htmlToImage && htmlToImage.toJpeg) {
                    dataUrl = await htmlToImage.toJpeg(el, { pixelRatio: 2, quality: 0.92, backgroundColor: '#ffffff', filter: skipWatermark })
                    this.usedLib = 'html-to-image'
                } else {
                    const libUrl = '/lib/html-to-image.min.js'
                    const mod = await import(/* @vite-ignore */ libUrl).catch(() => null)
                    if (mod && mod.toJpeg) {
                        dataUrl = await mod.toJpeg(el, { pixelRatio: 2, quality: 0.92, backgroundColor: '#ffffff', filter: skipWatermark })
                        this.usedLib = 'html-to-image'
                    }
                }
            } catch (e) { /* 降级 */ }
            // ② 降级 html2canvas
            if (!dataUrl) {
                const html2canvas = window.html2canvas
                if (!html2canvas) throw new Error('html-to-image / html2canvas 均未加载')
                const canvas = await html2canvas(el, { scale: 2, useCORS: true, backgroundColor: '#ffffff' })
                dataUrl = canvas.toDataURL('image/jpeg', 0.92)
                this.usedLib = 'html2canvas'
            }
            // ★在 canvas 上绘制水印（45°斜排6条），避免 DOM 水印干扰截图布局
            dataUrl = await overlayWatermark(dataUrl, this.getWatermarkText())
            } finally {
                // ★截图完成后清除内联样式，让 CSS 类重新接管
                el.style.cssText = ''
            }
            this.resultUrl = dataUrl
            this.previewMeta = Math.round(dataUrl.length * 0.75 / 1024) + ' KB · ' + this.usedLib
            try {
                const blob = await (await fetch(dataUrl)).blob()
                const file = new File([blob], '推广海报.jpg', { type: 'image/jpeg' })
                this.resultFile = file
                this.canNativeShare = !!(navigator.canShare && navigator.canShare({ files: [file] }))
            } catch (e) { this.canNativeShare = false }
            const inWx = /MicroMessenger/i.test(navigator.userAgent)
            if (this.canNativeShare) {
                this.tipText = '点「分享图片」可直接分享到微信 / 短信等'
            } else if (inWx) {
                this.tipText = '微信内无法一键分享。请长按下方图片 → 保存到相册，再发到群里。'
            } else {
                this.tipText = '当前环境不支持直接分享。长按图片保存，或点下方按钮下载。'
            }
        },
        async nativeShare() {
            if (!this.resultFile) return
            try {
                await navigator.share({ files: [this.resultFile], title: '推广海报' })
            } catch (e) {
                if (e && e.name !== 'AbortError') uni.showToast({ title: '分享失败，可长按图片保存', icon: 'none' })
            }
        },
        // #endif
        // #ifndef H5
        async generateMp() {
            const d = this.data
            const W = 340, H = 544
            this.canvasW = W; this.canvasH = H
            await this.$nextTick()
            const ctx = uni.createCanvasContext('apCanvas', this)
            const brand = this.brandColor
            // 主视觉区
            ctx.setFillStyle(brand)
            ctx.fillRect(0, 0, W, 200)
            if (this.cover) {
                try { ctx.drawImage(this.cover, 0, 0, W, 200) } catch (e) { /* 忽略 */ }
            }
            // 标题
            ctx.setFillStyle('#ffffff')
            ctx.setFontSize(20)
            ctx.fillText(this.truncate(d.title || '精彩活动', 14), 16, 176)
            // 信息卡
            ctx.setFillStyle('#ffffff')
            ctx.fillRect(16, 216, W - 32, 150)
            ctx.setFillStyle('#303133')
            ctx.setFontSize(13)
            ctx.fillText('时间：' + this.truncate(d.timeText || '', 18), 28, 244)
            ctx.fillText('地点：' + this.truncate(d.locationText || '', 18), 28, 272)
            ctx.fillText('费用：' + this.truncate(d.priceText || '', 18), 28, 300)
            // 剩余名额徽标
            ctx.setFillStyle(brand)
            ctx.fillRect(28, 320, 150, 30)
            ctx.setFillStyle('#ffffff')
            ctx.setFontSize(13)
            ctx.fillText(d.remainingText || '名额充足', 40, 340)
            // 二维码（小程序：data URL 可能无法直接 drawImage，降级用服务端渲染图或提示）
            // 行动指令
            ctx.setFillStyle('#303133')
            ctx.setFontSize(15)
            ctx.fillText(d.actionText || '扫码看详情并报名', 190, 410)
            ctx.setFillStyle('#909399')
            ctx.setFontSize(11)
            ctx.fillText(d.ownerType || '', 190, 434)
            ctx.fillText(d.ownerName || '', 190, 454)
            ctx.setFillStyle('#a0a6b0')
            ctx.setFontSize(10)
            ctx.fillText(d.footer || '由「小账本」生成', 120, 520)
            // ★水印：45°斜排6条（小程序 canvas 同样加水印，与 H5 统一）
            const wmText = this.getWatermarkText()
            const wmFontSize = Math.round(W * 0.055)
            ctx.setFillStyle('rgba(0, 0, 0, 0.07)')
            ctx.setFontSize(wmFontSize)
            ctx.setTextAlign('center')
            ctx.setTextBaseline('middle')
            for (let i = 0; i < 6; i++) {
                const py = H * (0.15 + 0.7 * i / 5)
                ctx.save()
                ctx.translate(W / 2, py)
                ctx.rotate(-Math.PI / 4)
                ctx.fillText(wmText, 0, 0)
                ctx.restore()
            }
            ctx.draw(false, () => {
                uni.canvasToTempFilePath({
                    canvasId: 'apCanvas', fileType: 'jpg', quality: 0.92,
                    success: (res) => {
                        this.resultUrl = res.tempFilePath
                        this.previewMeta = '小程序 Canvas 输出'
                        this.usedLib = 'uni-canvas'
                        this.tipText = '长按图片保存到相册，或点下方按钮保存'
                    },
                    fail: (e) => { this.tipText = '生成失败：' + (e.errMsg || '') }
                }, this)
            })
        },
        truncate(s, n) { s = String(s || ''); return s.length > n ? s.substring(0, n) + '…' : s },
        // #endif
        saveImage() {
            if (!this.resultUrl) return
            // #ifdef H5
            const a = document.createElement('a')
            a.href = this.resultUrl
            a.download = '推广海报.jpg'
            document.body.appendChild(a)
            a.click()
            document.body.removeChild(a)
            uni.showToast({ title: '已保存到下载目录', icon: 'none' })
            // #endif
            // #ifndef H5
            uni.saveImageToPhotosAlbum({
                filePath: this.resultUrl,
                success: () => uni.showToast({ title: '已保存到相册', icon: 'success' }),
                fail: () => uni.showToast({ title: '保存失败', icon: 'none' })
            })
            // #endif
        },
        close() { this.visible = false }
    }
}
</script>

<style scoped>
.ap-wrap { display: inline-block; }
.ap-btn { background: var(--brand, #409eff); color: #fff; border-radius: 40rpx; height: 72rpx; line-height: 72rpx; font-size: 26rpx; padding: 0 32rpx; border: none; }
.ap-btn.is-disabled { opacity: 0.6; }
.ap-btn::after { border: none; }

.ap-mask { position: fixed; inset: 0; background: rgba(0,0,0,.5); z-index: 9999; display: flex; align-items: center; justify-content: center; }
.ap-panel { width: 90vw; max-width: 680rpx; max-height: 90vh; background: #fff; border-radius: 24rpx; display: flex; flex-direction: column; overflow: hidden; }
.ap-head { display: flex; justify-content: space-between; align-items: center; padding: 24rpx 32rpx; border-bottom: 1rpx solid #f0f2f5; }
.ap-title { font-size: 32rpx; font-weight: bold; color: #1a1a1a; }
.ap-close { font-size: 40rpx; color: #909399; line-height: 1; padding: 0 8rpx; }
.ap-body { flex: 1; min-height: 0; padding: 24rpx 32rpx; }

/* ===== 海报源节点（竖版 3:4）：width 始终在基础类上，offscreen 只管定位 ===== */
.ap-shot { position: relative; width: 90vw; max-width: 680px; background: #fff; border-radius: 16rpx; overflow: hidden; border: 1rpx solid #f0f2f5; box-sizing: border-box; }
.ap-shot-offscreen { position: fixed; left: -9999px; top: 0; z-index: -1; }
.shot-visual { position: relative; width: 100%; height: 320rpx; overflow: hidden; }
.shot-cover { width: 100%; height: 100%; }
.shot-mask { position: absolute; left: 0; right: 0; bottom: 0; height: 140rpx; background: linear-gradient(180deg, rgba(0,0,0,0), rgba(0,0,0,.55)); }
.shot-title { position: absolute; left: 24rpx; right: 24rpx; bottom: 24rpx; color: #fff; font-size: 36rpx; font-weight: bold; line-height: 1.3; text-shadow: 0 2rpx 8rpx rgba(0,0,0,.4); }
.shot-card { padding: 24rpx; }
.shot-row { display: flex; align-items: center; margin-bottom: 14rpx; }
.shot-ico { font-size: 26rpx; margin-right: 12rpx; }
.shot-val { font-size: 26rpx; color: #303133; flex: 1; }
.shot-badge { display: inline-block; margin-top: 8rpx; padding: 10rpx 28rpx; border-radius: 30rpx; }
.shot-badge-text { color: #fff; font-size: 28rpx; font-weight: bold; }
.shot-bottom { display: flex; align-items: center; padding: 8rpx 24rpx 16rpx; }
.shot-qr { width: 200rpx; height: 200rpx; flex-shrink: 0; background: #fff; }
.shot-cta { flex: 1; margin-left: 28rpx; }
.shot-cta-main { display: block; font-size: 32rpx; font-weight: bold; color: #1a1a1a; }
.shot-cta-sub { display: block; font-size: 22rpx; color: #909399; margin-top: 12rpx; }
.shot-cta-name { display: block; font-size: 24rpx; color: #606266; margin-top: 6rpx; }
.shot-foot { display: block; text-align: center; font-size: 20rpx; color: #a0a6b0; padding: 12rpx 0 20rpx; }

/* ★水印：45°斜排，6条均布覆盖整个海报区域 */
.shot-watermark { position: absolute; inset: 0; display: flex; flex-direction: column; justify-content: space-around; align-items: center; pointer-events: none; overflow: hidden; z-index: 10; }
.wm-line { font-size: 30rpx; color: rgba(0, 0, 0, 0.07); transform: rotate(-45deg); white-space: nowrap; letter-spacing: 4rpx; font-weight: bold; }

.ap-canvas { display: block; margin: 0 auto; background: #fff; }
.ap-preview { margin-top: 20rpx; }
.ap-preview-img { width: 100%; border-radius: 12rpx; border: 1rpx solid #f0f2f5; }
.ap-preview-meta { display: block; font-size: 22rpx; color: #909399; text-align: center; margin-top: 8rpx; }
.ap-tip { display: block; font-size: 24rpx; color: #606266; text-align: center; margin-top: 16rpx; line-height: 1.5; }
.ap-actions { display: flex; gap: 16rpx; padding: 20rpx 32rpx calc(env(safe-area-inset-bottom) + 20rpx); border-top: 1rpx solid #f0f2f5; }
.ap-act { flex: 1; height: 76rpx; line-height: 76rpx; font-size: 26rpx; border-radius: 40rpx; border: none; background: var(--brand, #409eff); color: #fff; }
.ap-act::after { border: none; }
.ap-act.gray { background: #f0f2f5; color: #606266; }
.ap-act.primary { background: #67c23a; }
</style>
