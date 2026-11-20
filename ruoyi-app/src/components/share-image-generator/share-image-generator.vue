<template>
    <!-- ★M33 T33.10 名单截图分享组件（§十三）
         · 前端生成干净长图：座位号 + 姓名 + 脱敏手机号 + 主持单独一块 + 页脚引导语
         · 手机号 DOM 只存脱敏值（138****4521），完整号码仅拨号时临时拼 tel:
         · 分享三路径：navigator.share / 微信长按保存 / 下载兜底
         · 头像转 base64 内嵌避免 canvas 跨域污染
         · 生成动作记日志（emit('logged')）
         · H5：优先 html-to-image，失败降级 html2canvas；小程序：uni.canvasToTempFilePath -->
    <view class="sig-wrap">
        <!-- 触发按钮（可由外部插槽自定义） -->
        <slot name="trigger" :generating="generating" :generate="generate">
            <button class="sig-btn" :class="{ 'is-disabled': generating }" @click="generate">
                {{ generating ? '生成中…' : (btnText || '分享座位表到群') }}
            </button>
        </slot>

        <!-- ★H5 截图源节点：必须放在 scroll-view 外部，避免滚动容器克隆导致内容重复渲染 -->
        <!-- #ifdef H5 -->
        <view id="sig-shot-dom" class="sig-shot sig-shot-offscreen" ref="shot">
            <view class="shot-head">
                <text class="shot-title">{{ activityTitle || '活动报名名单' }}</text>
                <text class="shot-sub">{{ activityTime || '' }}</text>
            </view>
            <!-- ★座位分布图（截图专用，有座位数据时展示）
                 · 底图 / 头像均用 <view> + 内联 background-image 绕开 uni-image 自定义元素，
                   html-to-image 对 CSS url() 内联分支最稳，避免 uni-image 内部 <img>/<div> 结构克隆丢失
                 · 容器 padding-bottom 已按纵横比撑开，background-size:100% 100% 铺满不会变形
                 · 已占座位显示圆形头像，无头像时降级为昵称首字 -->
            <view v-if="hasSeatMap" class="shot-seatmap">
                <text class="shot-block-title">座位分布</text>
                <view class="shot-seatmap-canvas" :class="seatDensityClass" :style="{ paddingBottom: (seatMapAspect * 100) + '%' }">
                    <view v-if="seatBgBase64" class="shot-seatmap-bg" :style="seatBgStyle"></view>
                    <view v-for="(s, i) in seats" :key="'ss' + i"
                          class="shot-seatbox" :class="seatBoxClass(s)"
                          :style="{ left: (parseFloat(s.posXPercent || 0) * 100) + '%', top: (parseFloat(s.posYPercent || 0) * 100) + '%' }">
                        <text class="shot-seatbox-no" :class="{ 'is-top': seatHasAvatar(s) }">{{ s.seatNo || (i + 1) }}</text>
                        <view v-if="seatHasAvatar(s)" class="shot-seatbox-avatar" :style="seatAvatarStyle(s)"></view>
                        <view v-else-if="s.userId" class="shot-seatbox-avatar shot-seatbox-avatar-none"><text>{{ (s.userNickName || '?').charAt(0) }}</text></view>
                    </view>
                </view>
                <view class="shot-legend">
                    <view class="shot-legend-item"><view class="shot-legend-dot host"></view><text>主持</text></view>
                    <view class="shot-legend-item"><view class="shot-legend-dot confirmed"></view><text>已确认</text></view>
                    <view class="shot-legend-item"><view class="shot-legend-dot pending"></view><text>未确认</text></view>
                </view>
            </view>
            <!-- 主持位单独一块 -->
            <view v-if="hostRows.length" class="shot-block">
                <text class="shot-block-title">主持人（不计入名额）</text>
                <view v-for="(h, i) in hostRows" :key="'h' + i" class="shot-row">
                    <text class="shot-seat host">主持</text>
                    <image v-if="h.avatar" class="shot-avatar" :src="h.avatar" mode="aspectFill" />
                    <view v-else class="shot-avatar shot-avatar-none"><text>{{ (h.name || '?').charAt(0) }}</text></view>
                    <view class="shot-info">
                        <text class="shot-name">{{ h.name || '-' }}</text>
                        <text v-if="h.phoneMasked" class="shot-phone">{{ h.phoneMasked }}</text>
                    </view>
                </view>
            </view>
            <!-- 顾客名单 -->
            <view class="shot-block">
                <text class="shot-block-title">顾客名单（{{ memberRows.length }}人）</text>
                <view v-for="(m, i) in memberRows" :key="'m' + i" class="shot-row">
                    <text class="shot-seat" :class="{ 'is-confirmed': m.confirmed }">{{ m.seatNo || '-' }}</text>
                    <image v-if="m.avatar" class="shot-avatar" :src="m.avatar" mode="aspectFill" />
                    <view v-else class="shot-avatar shot-avatar-none"><text>{{ (m.name || '?').charAt(0) }}</text></view>
                    <view class="shot-info">
                        <text class="shot-name">{{ m.name || '-' }}</text>
                        <text class="shot-phone">{{ m.phoneMasked || '-' }}</text>
                    </view>
                    <text class="shot-status" :class="m.confirmed ? 'ok' : 'wait'">{{ m.confirmed ? '已确认' : '待确认' }}</text>
                </view>
                <view v-if="!memberRows.length" class="shot-empty">暂无顾客报名</view>
            </view>
            <!-- 页脚引导语 -->
            <view class="shot-foot">
                <text class="shot-foot-text">请核对您的座位</text>
                <text class="shot-foot-note">手机号已脱敏显示，仅门店与本人可见完整号码</text>
            </view>
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

        <!-- 结果弹窗：★整页覆盖原页面（含底部「全部确认」等按钮），图片区独立滚动（53 人长图上下滑动查看），
             操作按钮固定遮罩底部不与页面按钮重叠；点关闭回到原页面。
             H5 端弹出时挂到 body，脱离 scroll-view 层叠上下文（否则页面底部按钮会盖住弹窗） -->
        <view v-if="visible" ref="sigMask" class="sig-mask" @click="close">
            <view class="sig-panel" @click.stop>
                <view class="sig-head">
                    <text class="sig-title">报名名单</text>
                    <text class="sig-close" @click="close">×</text>
                </view>
                <scroll-view class="sig-body" scroll-y>
                    <!-- 小程序：使用 Canvas 2D 绘制 -->
                    <!-- #ifndef H5 -->
                    <canvas class="sig-canvas" canvas-id="sigCanvas" id="sigCanvas"
                            :style="{ width: canvasW + 'px', height: canvasH + 'px' }"></canvas>
                    <!-- #endif -->

                    <!-- 生成结果预览 -->
                    <view v-if="resultUrl" class="sig-preview">
                        <text class="sig-scroll-hint">图为长图：上下滑动查看完整内容，长按图片可保存</text>
                        <image class="sig-preview-img" :src="resultUrl" mode="widthFix" show-menu-by-longpress />
                        <text class="sig-preview-meta">{{ previewMeta }}</text>
                    </view>
                    <text class="sig-tip">{{ tipText }}</text>
                </scroll-view>
                <view class="sig-actions">
                    <!-- #ifdef H5 -->
                    <button v-if="canNativeShare" class="sig-act primary" @click="nativeShare">分享图片</button>
                    <!-- #endif -->
                    <button class="sig-act" @click="saveImage">{{ saveBtnText }}</button>
                    <button class="sig-act gray" @click="close">关闭</button>
                </view>
            </view>
        </view>
    </view>
</template>

<script>
import { toHttps } from '../../utils/format'
import { overlayWatermark, buildWatermarkText } from '../../utils/watermark'

/** 手机号脱敏：前三后四（138****4521） */
function maskPhone(p) {
    if (!p) return ''
    const s = String(p)
    if (s.length < 7) return s
    return s.substring(0, 3) + '****' + s.substring(s.length - 4)
}

/** URL → base64（H5，避免 canvas 跨域污染）
 *  ★降级策略：任何环节失败（CORS/加载/canvas 污染）都返回原 URL，
 *    避免 seatBgBase64 为空字符串导致 v-if 不渲染；html-to-image 会再试一次 fetch 内联 */
function urlToBase64(url) {
    return new Promise((resolve) => {
        if (!url) return resolve('')
        if (url.startsWith('data:')) return resolve(url)
        // #ifdef H5
        const finalUrl = toHttps(url)
        const img = new Image()
        img.crossOrigin = 'anonymous'
        img.onload = () => {
            try {
                const c = document.createElement('canvas')
                c.width = img.naturalWidth
                c.height = img.naturalHeight
                c.getContext('2d').drawImage(img, 0, 0)
                resolve(c.toDataURL('image/jpeg', 0.9))
            } catch (e) {
                // canvas 被跨域污染：toDataURL 报错，降级返回原 URL
                console.warn('[share-image] canvas 污染降级为原 URL:', url, e)
                resolve(finalUrl)
            }
        }
        img.onerror = (e) => {
            // 图片加载失败（多为 CORS）：降级返回原 URL，交给 html-to-image 再试
            console.warn('[share-image] 图片加载失败降级为原 URL:', url, e)
            resolve(finalUrl)
        }
        // ★缓存击穿：页面 <image> 会先以无 Origin 头请求同 URL（响应无 CORS 头）并被缓存，
        //   后续 crossOrigin 加载命中污染缓存必失败 → 截图丢座位底图；加时间戳参数强制带 Origin 新请求
        const sep = finalUrl.indexOf('?') >= 0 ? '&' : '?'
        img.src = finalUrl + sep + '_cors=' + Date.now()
        // #endif
        // #ifndef H5
        resolve(toHttps(url))
        // #endif
    })
}

export default {
    name: 'ShareImageGenerator',
    props: {
        // 活动标题
        activityTitle: { type: String, default: '' },
        // 活动时间文本
        activityTime: { type: String, default: '' },
        // 活动 ID（用于日志）
        activityId: { type: [String, Number], default: '' },
        // 主持人列表：[{name, avatar, phone}]
        hosts: { type: Array, default: () => [] },
        // 顾客名单：[{name, avatar, phone, seatNo, confirmed}]
        members: { type: Array, default: () => [] },
        // 按钮文案
        btnText: { type: String, default: '' },
        // ★座位分布图（可选）
        seatMapBgUrl: { type: String, default: '' },
        seats: { type: Array, default: () => [] },
        seatLayoutWidth: { type: Number, default: 1920 },
        seatLayoutHeight: { type: Number, default: 1080 }
    },
    data() {
        return {
            visible: false,
            generating: false,
            resultUrl: '',
            resultBlob: null,
            usedLib: '',
            previewMeta: '',
            tipText: '',
            canNativeShare: false,
            canvasW: 340,
            canvasH: 600,
            seatBgBase64: '',
            // ★座位背景图本地路径（小程序 canvas drawImage 使用）
            seatBgLocalPath: '',
            // ★座位头像资源映射：url -> { base64, localPath }（url 去重，避免重复拉取）
            seatAvatarMap: {}
        }
    },
    computed: {
        /** 主持人行（头像已 base64 化后写入） */
        hostRows() {
            return (this.hosts || []).map(h => ({
                name: h.name || h.nickName || '',
                avatar: h.avatarBase64 || toHttps(h.avatar || ''),
                phoneMasked: h.phoneMasked || maskPhone(h.phone || h.phoneNumber || '')
            }))
        },
        /** 顾客行（手机号 DOM 只存脱敏值） */
        memberRows() {
            return (this.members || []).map(m => ({
                name: m.name || m.nickName || '',
                avatar: m.avatarBase64 || toHttps(m.avatar || ''),
                phoneMasked: m.phoneMasked || maskPhone(m.phone || m.phoneNumber || ''),
                seatNo: m.seatNo || m.seat || '',
                confirmed: !!(m.confirmed || m.isConfirmed)
            }))
        },
        saveBtnText() {
            // #ifdef H5
            const inWx = typeof navigator !== 'undefined' && /MicroMessenger/i.test(navigator.userAgent)
            return inWx ? '长按图片保存' : '保存 / 下载图片'
            // #endif
            // #ifndef H5
            return '保存到相册'
            // #endif
        },
        /** ★是否有座位布局图 */
        hasSeatMap() {
            return !!(this.seats && this.seats.length && (this.seatMapBgUrl || true))
        },
        /** ★座位图纵横比（用于截图容器高度） */
        seatMapAspect() {
            return (this.seatLayoutHeight || 1080) / (this.seatLayoutWidth || 1920)
        },
        /** ★座位块密度档位：座位越多块越小，避免 53 座等密集布局互相重叠 */
        seatDensityClass() {
            const n = (this.seats || []).length
            if (n > 32) return 'xdense'
            if (n > 16) return 'dense'
            return ''
        },
        /** ★底图内联 style：background-image 拼 URL（base64 或降级后的原 URL），html-to-image 会识别 data: 直接内联 */
        seatBgStyle() {
            if (!this.seatBgBase64) return {}
            return { backgroundImage: 'url("' + this.seatBgBase64 + '")' }
        },
    },
    watch: {
        /** ★H5：弹窗打开时把遮罩挂到 body。
         *  遮罩在页面 scroll-view 内时会被其层叠上下文（-webkit-overflow-scrolling）困住，
         *  顶部导航栏 / 页面底部「全部确认」按钮会盖住遮罩；挂到 body 后 fixed 覆盖整个视口。
         *  ★uni-app vue2 H5 中 <view> 的 ref 可能是组件实例，必须解析出真实 DOM 再 appendChild */
        visible(v) {
            // #ifdef H5
            if (v) this.$nextTick(() => {
                const el = this.resolveMaskEl()
                if (el && el.parentNode !== document.body) document.body.appendChild(el)
            })
            // #endif
        }
    },
    beforeDestroy() {
        // #ifdef H5
        // 遮罩可能已被移到 body：组件销毁时手动清理，避免残留
        const el = this.resolveMaskEl()
        if (el && el.parentNode === document.body) document.body.removeChild(el)
        // #endif
    },
    methods: {
        maskPhone,
        /** ★解析遮罩真实 DOM 节点：ref 在 uni-app H5 可能返回组件实例（取 $el），兜底 querySelector */
        resolveMaskEl() {
            const ref = this.$refs.sigMask
            let el = null
            if (ref) el = ref.$el || (ref.nodeType === 1 ? ref : null)
            if (!el && this.$el && this.$el.querySelector) el = this.$el.querySelector('.sig-mask')
            return el || null
        },
        /** ★水印文字：租户名称 + 年月日时分秒（method 而非 computed，保证每次截图取实时时间） */
        getWatermarkText() {
            return buildWatermarkText()
        },
        /** 打开弹窗并生成图 */
        async generate() {
            if (this.generating) return
            this.visible = true
            this.generating = true
            this.resultUrl = ''
            this.tipText = '正在生成…'
            try {
                // 头像预转 base64（避免 canvas 跨域污染）
                await this.preloadAvatars()
                // ★调试日志：确认座位底图 / 头像预加载结果
                console.log('[share-image] preload 完成:', {
                    seatBgBase64Len: (this.seatBgBase64 || '').length,
                    seatBgIsData: (this.seatBgBase64 || '').startsWith('data:'),
                    seatBgLocalPath: this.seatBgLocalPath,
                    seatAvatarKeys: Object.keys(this.seatAvatarMap || {}).length,
                    seatAvatarSample: Object.values(this.seatAvatarMap || {})[0],
                    seatsTotal: (this.seats || []).length,
                    seatsWithAvatar: (this.seats || []).filter(s => s.userAvatar).length
                })
                // #ifdef H5
                await this.$nextTick()
                // ★等 background-image 异步加载完成（CSS 背景图不受 nextTick 控制）
                await new Promise(r => setTimeout(r, 250))
                await this.generateH5()
                // #endif
                // #ifndef H5
                await this.$nextTick()
                await this.generateMp()
                // #endif
                // 记录生成日志（外部可挂接后端接口）
                this.$emit('logged', { activityId: this.activityId, at: Date.now(), lib: this.usedLib })
            } catch (e) {
                this.tipText = '生成失败：' + (e && e.message ? e.message : e)
            } finally {
                this.generating = false
            }
        },
        /** 头像 + 座位背景图 + 座位头像批量预加载（并发，同源 URL 去重） */
        async preloadAvatars() {
            const tasks = []
            const patch = (arr) => arr.forEach(item => {
                if (item && item.avatar && !item.avatarBase64) {
                    tasks.push(urlToBase64(item.avatar).then(b64 => { item.avatarBase64 = b64 }))
                }
            })
            patch(this.hosts || [])
            patch(this.members || [])
            // ★座位背景图：H5 转 base64 避免跨域污染；小程序取本地路径供 ctx.drawImage
            if (this.seatMapBgUrl) {
                tasks.push(this.loadImageAsset(this.seatMapBgUrl).then(a => {
                    this.seatBgBase64 = a.base64
                    this.seatBgLocalPath = a.localPath
                }))
            }
            // ★座位头像：同一头像 URL 只拉一次，写入 seatAvatarMap 供模板 / canvas 取用
            const seen = {}
            const seats = this.seats || []
            for (let i = 0; i < seats.length; i++) {
                const url = seats[i] && seats[i].userAvatar
                if (!url || seen[url]) continue
                seen[url] = true
                tasks.push(this.loadImageAsset(url).then(a => {
                    this.$set(this.seatAvatarMap, url, a)
                }))
            }
            await Promise.all(tasks)
        },
        /** 加载图片资源：H5 返回 base64（canvas 免跨域）；小程序返回本地路径（drawImage 使用） */
        loadImageAsset(url) {
            return new Promise(resolve => {
                const result = { base64: '', localPath: '' }
                if (!url) return resolve(result)
                // #ifdef H5
                urlToBase64(url).then(b64 => {
                    result.base64 = b64 || ''
                    result.localPath = toHttps(url)
                    resolve(result)
                })
                // #endif
                // #ifndef H5
                uni.getImageInfo({
                    src: toHttps(url),
                    success: (info) => { result.localPath = (info && info.path) || ''; resolve(result) },
                    fail: () => resolve(result)
                })
                // #endif
            })
        },
        /** 座位是否有可用头像（已占 + URL + 预加载成功） */
        seatHasAvatar(s) {
            if (!s || !s.userId || !s.userAvatar) return false
            const asset = this.seatAvatarMap[s.userAvatar]
            if (!asset) return false
            // #ifdef H5
            return !!asset.base64
            // #endif
            // #ifndef H5
            return !!asset.localPath
            // #endif
        },
        /** 座位头像渲染源：H5 优先 base64，小程序优先本地路径 */
        seatAvatarSrc(s) {
            const url = s && s.userAvatar
            if (!url) return ''
            const asset = this.seatAvatarMap[url]
            if (!asset) return toHttps(url)
            // #ifdef H5
            return asset.base64 || toHttps(url)
            // #endif
            // #ifndef H5
            return asset.localPath || toHttps(url)
            // #endif
        },
        /** ★座位头像内联 style（H5：view + background-image 绕开 uni-image） */
        seatAvatarStyle(s) {
            const src = this.seatAvatarSrc(s)
            if (!src) return {}
            return { backgroundImage: 'url("' + src + '")' }
        },
        /** ★座位块样式类：橙=主持 / 绿=已确认 / 灰=未确认 / 浅=空闲 */
        seatBoxClass(s) {
            if (s.status === 'PRESET' || s.presetRole) return 'host'
            if (s.status === 'FREE' || !s.userId) return 'free'
            return s.confirmed ? 'confirmed' : 'pending'
        },
        // #ifdef H5
        async generateH5() {
            const el = document.getElementById('sig-shot-dom')
            if (!el) throw new Error('未找到截图节点')
            // ★截图前用内联样式精确覆盖：left:auto 抵消 CSS 类的 -9999px
            el.style.cssText = 'position:fixed;left:0;top:0;z-index:-1;width:88vw;max-width:640px;'
            let dataUrl = ''
            try {
            // ★html-to-image 克隆时排除水印节点，避免 absolute 覆盖层干扰布局
            const skipWatermark = (node) => !node.classList || !node.classList.contains('shot-watermark')
            // ① 优先 html-to-image（19KB）
            try {
                const htmlToImage = window.htmlToImage || (window.__htmlToImage)
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
                // ★截图完成后清除内联样式，让 CSS 类重新接管（offscreen 类把元素推回屏外）
                el.style.cssText = ''
            }
            this.resultUrl = dataUrl
            const bytes = Math.round(dataUrl.length * 0.75 / 1024)
            this.previewMeta = bytes + ' KB · ' + this.usedLib
            // 检测原生分享能力
            try {
                const blob = await (await fetch(dataUrl)).blob()
                this.resultBlob = blob
                const file = new File([blob], '报名名单.jpg', { type: 'image/jpeg' })
                this.canNativeShare = !!(navigator.canShare && navigator.canShare({ files: [file] }))
                this.resultFile = file
            } catch (e) { this.canNativeShare = false }
            // 提示语：微信内 / 普通浏览器 / 都不支持
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
                await navigator.share({ files: [this.resultFile], title: '报名名单' })
            } catch (e) {
                if (e && e.name !== 'AbortError') uni.showToast({ title: '分享失败，可长按图片保存', icon: 'none' })
            }
        },
        // #endif
        // #ifndef H5
        async generateMp() {
            // 小程序：使用 uni.createCanvasContext 绘制
            const rows = this.memberRows
            const hosts = this.hostRows
            const seats = this.seats || []
            const rowH = 44
            const headH = 80
            const blockGap = 16
            // ★座位图区高度（简化版：标题 + 地图区 + 图例）
            const hasSeat = seats.length > 0
            const mapW = 308  // W - 32
            const mapH = hasSeat ? Math.round(mapW * this.seatMapAspect) : 0
            const seatMapH = hasSeat ? (20 + mapH + 20 + blockGap) : 0
            const hostBlockH = hosts.length ? (32 + hosts.length * rowH + blockGap) : 0
            const memberBlockH = 32 + Math.max(rows.length, 1) * rowH + blockGap
            const footH = 60
            const W = 340
            const H = headH + seatMapH + hostBlockH + memberBlockH + footH + 20
            this.canvasW = W
            this.canvasH = H
            await this.$nextTick()
            const ctx = uni.createCanvasContext('sigCanvas', this)
            ctx.setFillStyle('#ffffff')
            ctx.fillRect(0, 0, W, H)
            // 头部
            ctx.setFillStyle('#1a1a1a')
            ctx.setFontSize(18)
            ctx.fillText(this.activityTitle || '活动报名名单', 16, 30)
            ctx.setFillStyle('#909399')
            ctx.setFontSize(12)
            ctx.fillText(this.activityTime || '', 16, 52)
            let y = headH
            // ★座位分布图（小程序）：底图 + 座位色块 + 圆形头像 + 座位号
            if (hasSeat) {
                ctx.setFillStyle('#606266')
                ctx.setFontSize(13)
                ctx.fillText('座位分布', 16, y + 14)
                y += 20
                // 地图区域背景色（底图未加载时兜底）
                ctx.setFillStyle('#f5f6f8')
                ctx.fillRect(16, y, mapW, mapH)
                // ★T33.10 修复：绘制座位底图（本地路径，铺满地图区域）
                if (this.seatBgLocalPath) {
                    ctx.drawImage(this.seatBgLocalPath, 16, y, mapW, mapH)
                }
                // 绘制每个座位
                // ★座位块随座位数增多缩小（与 H5 密度档位同口径），避免 53 座等密集布局重叠
                const seatRatio = seats.length > 32 ? 0.022 : (seats.length > 16 ? 0.032 : 0.045)
                const seatW = Math.max(10, Math.round(mapW * seatRatio))
                const seatH = Math.round(seatW * 1.2)
                const colors = { host: ['#fdf6ec', '#e6a23c'], confirmed: ['#f0f9eb', '#67c23a'], pending: ['#f4f4f5', '#909399'], free: ['#fafafa', '#e4e7ed'] }
                const avatarTasks = []
                seats.forEach((s) => {
                    const cls = this.seatBoxClass(s)
                    const [bg, fg] = colors[cls] || colors.free
                    const sx = 16 + parseFloat(s.posXPercent || 0) * mapW - seatW / 2
                    const sy = y + parseFloat(s.posYPercent || 0) * mapH - seatH / 2
                    ctx.setFillStyle(bg)
                    ctx.fillRect(sx, sy, seatW, seatH)
                    ctx.setStrokeStyle(fg)
                    ctx.setLineWidth(0.5)
                    ctx.strokeRect(sx, sy, seatW, seatH)
                    ctx.setFillStyle(fg)
                    ctx.setTextAlign('center')
                    // ★头像或座位号
                    const asset = s.userAvatar && this.seatAvatarMap[s.userAvatar]
                    const avatarPath = asset && asset.localPath
                    if (s.userId && avatarPath) {
                        // 座位号顶部小字
                        ctx.setFontSize(Math.max(6, Math.round(seatW * 0.32)))
                        ctx.fillText(String(s.seatNo || ''), sx + seatW / 2, sy + Math.round(seatW * 0.38))
                        // 头像延后统一绘制（clip 圆需要 save/restore）
                        const r = Math.round(seatW * 0.36)
                        avatarTasks.push({
                            path: avatarPath,
                            cx: sx + seatW / 2,
                            cy: sy + seatH - r - Math.round(seatW * 0.12),
                            r: r
                        })
                    } else {
                        ctx.setFontSize(Math.max(6, Math.round(seatW * 0.45)))
                        ctx.fillText(String(s.seatNo || ''), sx + seatW / 2, sy + seatH / 2 + 2)
                    }
                    ctx.setTextAlign('left')
                })
                // ★圆形头像绘制（save + clip + drawImage + restore）
                avatarTasks.forEach(t => {
                    ctx.save()
                    ctx.beginPath()
                    ctx.arc(t.cx, t.cy, t.r, 0, Math.PI * 2)
                    ctx.clip()
                    ctx.drawImage(t.path, t.cx - t.r, t.cy - t.r, t.r * 2, t.r * 2)
                    ctx.restore()
                })
                y += mapH
                // 图例
                y += 6
                const legendY = y + 8
                const legendItems = [['主持', '#e6a23c'], ['已确认', '#67c23a'], ['未确认', '#909399']]
                let lx = 16 + (mapW - 180) / 2
                legendItems.forEach(([label, color]) => {
                    ctx.setFillStyle(color)
                    ctx.fillRect(lx, legendY - 6, 8, 8)
                    ctx.setFillStyle('#606266')
                    ctx.setFontSize(9)
                    ctx.fillText(label, lx + 12, legendY + 1)
                    lx += 60
                })
                y += 20
                // 分隔线
                ctx.setStrokeStyle('#f0f2f5')
                ctx.setLineWidth(0.5)
                ctx.moveTo(16, y)
                ctx.lineTo(16 + mapW, y)
                ctx.stroke()
                y += blockGap
            }
            // 主持块
            if (hosts.length) {
                ctx.setFillStyle('#e6a23c')
                ctx.setFontSize(13)
                ctx.fillText('主持人（不计入名额）', 16, y + 16)
                y += 32
                hosts.forEach(h => {
                    ctx.setFillStyle('#303133')
                    ctx.setFontSize(13)
                    ctx.fillText('主持', 16, y + 20)
                    ctx.fillText(h.name || '-', 70, y + 20)
                    ctx.setFillStyle('#909399')
                    ctx.setFontSize(11)
                    ctx.fillText(h.phoneMasked || '', 180, y + 20)
                    y += rowH
                })
                y += blockGap
            }
            // 顾客块
            ctx.setFillStyle('#303133')
            ctx.setFontSize(13)
            ctx.fillText('顾客名单（' + rows.length + '人）', 16, y + 16)
            y += 32
            if (!rows.length) {
                ctx.setFillStyle('#c0c4cc')
                ctx.setFontSize(12)
                ctx.fillText('暂无顾客报名', 16, y + 20)
                y += rowH
            } else {
                rows.forEach(m => {
                    ctx.setFillStyle('#303133')
                    ctx.setFontSize(13)
                    ctx.fillText(m.seatNo || '-', 16, y + 20)
                    ctx.fillText(m.name || '-', 70, y + 20)
                    ctx.setFillStyle('#909399')
                    ctx.setFontSize(11)
                    ctx.fillText(m.phoneMasked || '-', 180, y + 20)
                    ctx.setFillStyle(m.confirmed ? '#67c23a' : '#e6a23c')
                    ctx.fillText(m.confirmed ? '已确认' : '待确认', 270, y + 20)
                    y += rowH
                })
            }
            y += blockGap
            // 页脚
            ctx.setFillStyle('#606266')
            ctx.setFontSize(12)
            ctx.fillText('请核对您的座位', 16, y + 16)
            ctx.setFillStyle('#909399')
            ctx.setFontSize(10)
            ctx.fillText('手机号已脱敏显示，仅门店与本人可见完整号码', 16, y + 34)
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
                    canvasId: 'sigCanvas',
                    fileType: 'jpg',
                    quality: 0.92,
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
        // #endif
        /** 保存 / 下载 */
        saveImage() {
            if (!this.resultUrl) return
            // #ifdef H5
            const a = document.createElement('a')
            a.href = this.resultUrl
            a.download = '报名名单.jpg'
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
        close() {
            this.visible = false
        }
    }
}
</script>

<style scoped>
.sig-wrap { display: inline-block; }
.sig-btn { background: var(--brand, #409eff); color: #fff; border-radius: 40rpx; height: 72rpx; line-height: 72rpx; font-size: 26rpx; padding: 0 32rpx; border: none; }
.sig-btn.is-disabled { opacity: 0.6; }
.sig-btn::after { border: none; }

/* ★整页遮罩：完全覆盖原页面（含底部操作按钮），图片区滚动、按钮固定遮罩底部 */
.sig-mask { position: fixed; inset: 0; background: var(--page-bg, #f5f6f8); z-index: 9999; display: flex; justify-content: center; }
.sig-panel { width: 100%; max-width: 640px; height: 100%; background: #fff; display: flex; flex-direction: column; overflow: hidden; }
.sig-head { display: flex; justify-content: space-between; align-items: center; padding: 24rpx 32rpx; border-bottom: 1rpx solid #f0f2f5; }
.sig-title { font-size: 32rpx; font-weight: bold; color: #1a1a1a; }
.sig-close { font-size: 40rpx; color: #909399; line-height: 1; padding: 0 8rpx; }
.sig-body { flex: 1; min-height: 0; padding: 24rpx 32rpx; }

/* 截图源节点（DOM 版）：width 始终在基础类上，offscreen 只管定位 */
.sig-shot { position: relative; width: 88vw; max-width: 640px; background: #fff; padding: 24rpx; border-radius: 16rpx; border: 1rpx solid #f0f2f5; box-sizing: border-box; }

/* ★座位分布图（截图内） */
.shot-seatmap { margin-bottom: 16rpx; padding-bottom: 16rpx; border-bottom: 1rpx solid #f0f2f5; }
.shot-seatmap-canvas { position: relative; width: 100%; margin-top: 10rpx; background: #f5f6f8; border-radius: 8rpx; overflow: hidden; }
.shot-seatmap-bg { position: absolute; left: 0; top: 0; width: 100%; height: 100%; background-size: 100% 100%; background-repeat: no-repeat; background-position: center; }
.shot-seatbox { position: absolute; transform: translate(-50%, -50%); width: 7%; padding-bottom: 8.4%; border-radius: 4rpx; border: 1rpx solid #e4e7ed; background: #fafafa; box-sizing: border-box; overflow: hidden; }
.shot-seatbox-no { position: absolute; left: 50%; top: 50%; transform: translate(-50%, -50%); font-size: 16rpx; color: #909399; line-height: 1; z-index: 2; pointer-events: none; }
/* ★有头像时座位号上移到顶部小字，避免遮挡头像 */
.shot-seatbox-no.is-top { top: 6%; transform: translateX(-50%); font-size: 12rpx; }
/* ★座位头像（圆形，占据座位块下部；background-image 由内联 style 注入） */
.shot-seatbox-avatar { position: absolute; left: 50%; top: 58%; transform: translate(-50%, -50%); width: 72%; height: 60%; border-radius: 50%; background-color: #f0f2f5; background-size: cover; background-position: center; background-repeat: no-repeat; z-index: 1; }
.shot-seatbox-avatar-none { display: flex; align-items: center; justify-content: center; font-size: 20rpx; color: #909399; }
.shot-seatbox.host { border-color: #e6a23c; background: #fdf6ec; }
/* ★密集档（>16 座）：座位块与字号缩小，避免重叠 */
.shot-seatmap-canvas.dense .shot-seatbox { width: 4.8%; padding-bottom: 5.8%; }
.shot-seatmap-canvas.dense .shot-seatbox-no { font-size: 12rpx; }
.shot-seatmap-canvas.dense .shot-seatbox-no.is-top { font-size: 10rpx; }
.shot-seatmap-canvas.dense .shot-seatbox-avatar-none { font-size: 14rpx; }
/* ★超密集档（>32 座，如 53 座剧院式）：进一步缩小圆点/座位块 */
.shot-seatmap-canvas.xdense .shot-seatbox { width: 3.4%; padding-bottom: 4.1%; }
.shot-seatmap-canvas.xdense .shot-seatbox-no { font-size: 10rpx; }
.shot-seatmap-canvas.xdense .shot-seatbox-no.is-top { font-size: 8rpx; }
.shot-seatmap-canvas.xdense .shot-seatbox-avatar-none { font-size: 10rpx; }
.shot-seatbox.host .shot-seatbox-no { color: #e6a23c; }
.shot-seatbox.confirmed { border-color: #67c23a; background: #f0f9eb; }
.shot-seatbox.confirmed .shot-seatbox-no { color: #67c23a; }
.shot-seatbox.pending { border-color: #909399; background: #f4f4f5; }
.shot-seatbox.pending .shot-seatbox-no { color: #909399; }
.shot-legend { display: flex; gap: 32rpx; justify-content: center; margin-top: 12rpx; }
.shot-legend-item { display: flex; align-items: center; gap: 8rpx; font-size: 20rpx; color: #606266; }
.shot-legend-dot { width: 16rpx; height: 16rpx; border-radius: 50%; }
.shot-legend-dot.host { background: #e6a23c; }
.shot-legend-dot.confirmed { background: #67c23a; }
.shot-legend-dot.pending { background: #c0c4cc; }
.sig-shot-offscreen { position: fixed; left: -9999px; top: 0; z-index: -1; }
.shot-head { padding-bottom: 16rpx; border-bottom: 1rpx solid #f0f2f5; margin-bottom: 16rpx; }
.shot-title { display: block; font-size: 32rpx; font-weight: bold; color: #1a1a1a; }
.shot-sub { display: block; font-size: 22rpx; color: #909399; margin-top: 6rpx; }
.shot-block { margin-bottom: 16rpx; }
.shot-block-title { display: block; font-size: 24rpx; color: #606266; margin-bottom: 10rpx; font-weight: 600; }
.shot-row { display: flex; align-items: center; padding: 10rpx 0; border-bottom: 1rpx solid #f5f6f8; }
.shot-row:last-child { border-bottom: none; }
.shot-seat { width: 70rpx; font-size: 24rpx; font-weight: bold; color: #303133; flex-shrink: 0; }
.shot-seat.host { color: #e6a23c; }
.shot-seat.is-confirmed { color: #67c23a; }
.shot-avatar { width: 56rpx; height: 56rpx; border-radius: 50%; margin-right: 16rpx; flex-shrink: 0; background: #f0f2f5; }
.shot-avatar-none { display: flex; align-items: center; justify-content: center; font-size: 22rpx; color: #909399; }
.shot-info { flex: 1; overflow: hidden; }
.shot-name { display: block; font-size: 26rpx; color: #1a1a1a; }
.shot-phone { display: block; font-size: 22rpx; color: #909399; margin-top: 2rpx; }
.shot-status { font-size: 22rpx; padding: 4rpx 14rpx; border-radius: 8rpx; flex-shrink: 0; }
.shot-status.ok { background: #f0f9eb; color: #67c23a; }
.shot-status.wait { background: #fdf6ec; color: #e6a23c; }
.shot-empty { text-align: center; color: #c0c4cc; font-size: 24rpx; padding: 24rpx 0; }
.shot-foot { padding-top: 16rpx; border-top: 1rpx solid #f0f2f5; }
.shot-foot-text { display: block; font-size: 24rpx; color: #606266; text-align: center; }
.shot-foot-note { display: block; font-size: 20rpx; color: #909399; text-align: center; margin-top: 6rpx; }

/* ★水印：45°斜排，6条均布覆盖整个截图区域 */
.shot-watermark { position: absolute; inset: 0; display: flex; flex-direction: column; justify-content: space-around; align-items: center; pointer-events: none; overflow: hidden; z-index: 10; }
.wm-line { font-size: 30rpx; color: rgba(0, 0, 0, 0.07); transform: rotate(-45deg); white-space: nowrap; letter-spacing: 4rpx; font-weight: bold; }

/* 小程序 canvas */
.sig-canvas { display: block; margin: 0 auto; background: #fff; }

/* 预览与操作 */
.sig-preview { margin-top: 20rpx; }
.sig-scroll-hint { display: block; font-size: 22rpx; color: #909399; text-align: center; margin-bottom: 12rpx; }
.sig-preview-img { width: 100%; border-radius: 12rpx; border: 1rpx solid #f0f2f5; }
.sig-preview-meta { display: block; font-size: 22rpx; color: #909399; text-align: center; margin-top: 8rpx; }
.sig-tip { display: block; font-size: 24rpx; color: #606266; text-align: center; margin-top: 16rpx; line-height: 1.5; }
.sig-actions { display: flex; gap: 16rpx; padding: 20rpx 32rpx calc(env(safe-area-inset-bottom) + 20rpx); border-top: 1rpx solid #f0f2f5; background: #fff; flex-shrink: 0; }
.sig-act { flex: 1; height: 76rpx; line-height: 76rpx; font-size: 26rpx; border-radius: 40rpx; border: none; background: var(--brand, #409eff); color: #fff; }
.sig-act::after { border: none; }
.sig-act.gray { background: #f0f2f5; color: #606266; }
.sig-act.primary { background: #67c23a; }
</style>
