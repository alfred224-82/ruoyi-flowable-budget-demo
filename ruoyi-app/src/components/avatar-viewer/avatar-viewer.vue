<template>
    <!-- 头像两档加载 + 点击放大查看器（T31.4）
         用法一（包裹模式）：<avatar-viewer :src="avatarMd" :name="userName" size="80rpx" />
         用法二（全局遮罩）：页面底部放 <avatar-viewer ref="viewer" />，
                   列表中 <image @click="$refs.viewer.open(item.avatar, item.name)" />
         两档规格：缩略档 _md.webp 120px（首屏）→ 放大档 _xl.webp 400px（点击后加载） -->
    <view class="av-wrap">
        <!-- 缩略图（首屏仅加载 md 档） -->
        <image v-if="src" class="av-thumb" :class="{ 'av-round': round }"
               :src="displaySrc" :style="thumbStyle"
               mode="aspectFill" @click.stop="handleClick" />
        <slot v-else />

        <!-- 放大遮罩层 -->
        <view v-if="visible" class="av-mask" @click="close" @touchstart="onTouchStart" @touchmove="onTouchMove">
            <text class="av-tip">点击任意处关闭</text>
            <image class="av-full" :class="imgClass" :src="currentFull" mode="aspectFill" />
            <text v-if="displayName" class="av-cap">{{ displayName }}</text>
            <view class="av-close" @click.stop="close">
                <text class="av-close-x">×</text>
            </view>
        </view>
    </view>
</template>

<script>
/**
 * 头像点击放大查看器（T31.4，设计方案 §十一）
 *
 * 设计要点：
 * 1. 两档规格：缩略档 _md.webp 120px（2~4 KB，首屏加载）+ 放大档 _xl.webp 400px（16~21 KB，点击后加载）
 * 2. 渐进加载：先用已缓存的缩略图模糊放大占位 → xl 下载完平滑替换
 * 3. 路径推导：自动从 _md 推导到 _xl（无需手动传 xl 路径）
 * 4. 关闭方式：点击遮罩 / Esc（H5）/ iOS 下滑 / 底部 × 按钮
 * 5. 事件委托：包裹模式下点击即触发，列表刷新/动态插入无需重绑
 */
export default {
    name: 'AvatarViewer',
    props: {
        // 缩略档图片地址（xxx_md.webp / xxx_md.jpg）
        src: { type: String, default: '' },
        // 放大档地址（可选，不传则自动从 src 推导 _md → _xl）
        xlSrc: { type: String, default: '' },
        // 姓名（放大时底部显示）
        name: { type: String, default: '' },
        // 缩略图尺寸（CSS 值，如 '80rpx' / '120rpx'）
        size: { type: String, default: '80rpx' },
        // 是否圆形（默认 true）
        round: { type: Boolean, default: true }
    },
    data() {
        return {
            visible: false,
            displayName: '',
            currentFull: '',
            imgLoaded: false,
            touchStartY: 0
        }
    },
    computed: {
        /** 缩略图显示地址（H5 兼容 http→https） */
        displaySrc() {
            return this.toHttps(this.src)
        },
        /** 缩略图内联样式 */
        thumbStyle() {
            return { width: this.size, height: this.size }
        },
        /** 放大图 class（loading=模糊占位，ready=清晰） */
        imgClass() {
            return this.imgLoaded ? 'ready' : 'loading'
        }
    },
    methods: {
        /**
         * 打开放大查看器（可供父组件 ref 调用）
         * @param {string} [src] 缩略图地址（ref 模式时必传）
         * @param {string} [name] 姓名
         */
        open(src, name) {
            const thumbSrc = src || this.src
            if (!thumbSrc) return
            this.displayName = name !== undefined ? name : this.name
            // 第一步：用缩略图模糊放大立即显示（已在缓存里，0 延迟）
            this.currentFull = this.toHttps(thumbSrc)
            this.imgLoaded = false
            this.visible = true
            // 第二步：后台加载 xl 大图，到位后平滑替换
            const xlUrl = this.toHttps(this.xlSrc || this.deriveXl(thumbSrc))
            if (xlUrl && xlUrl !== this.currentFull) {
                this.loadXl(xlUrl)
            } else {
                this.imgLoaded = true
            }
        },

        /** 关闭查看器 */
        close() {
            this.visible = false
            this.imgLoaded = false
            setTimeout(() => { this.currentFull = '' }, 240)
        },

        /** 点击缩略图触发 */
        handleClick() {
            this.open()
            this.$emit('zoom', { src: this.src, name: this.name })
        },

        /**
         * 推导 xl 大图路径：xxx_md.webp → xxx_xl.webp
         * 匹配不到则用原图（不报错）
         */
        deriveXl(src) {
            if (!src) return ''
            const m = src.match(/^(.*)_md\.(webp|jpg|jpeg|png)$/i)
            return m ? `${m[1]}_xl.webp` : src
        },

        /** 后台加载 xl 大图 */
        loadXl(url) {
            // #ifdef H5
            const img = new Image()
            img.onload = () => {
                if (!this.visible) return // 防止快速连点导致错图
                this.currentFull = url
                this.imgLoaded = true
            }
            img.onerror = () => { this.imgLoaded = true } // 失败就保持缩略图
            img.src = url
            // #endif
            // #ifndef H5
            // 小程序端用 uni.getImageInfo 预加载
            uni.getImageInfo({
                src: url,
                success: () => {
                    if (!this.visible) return
                    this.currentFull = url
                    this.imgLoaded = true
                },
                fail: () => { this.imgLoaded = true }
            })
            // #endif
        },

        /** iOS 下滑关闭 */
        onTouchStart(e) {
            this.touchStartY = e.touches ? e.touches[0].clientY : 0
        },
        onTouchMove(e) {
            if (!this.touchStartY) return
            const dy = (e.touches ? e.touches[0].clientY : 0) - this.touchStartY
            if (dy > 80) {
                this.close()
                this.touchStartY = 0
            }
        },

        /** HTTP→HTTPS 兼容（微信禁止 HTTP 加载图片） */
        toHttps(url) {
            if (!url || !url.startsWith('http://')) return url
            return 'https:' + url.substring(4)
        }
    },

    // #ifdef H5
    mounted() {
        // H5 端监听 Esc 键关闭
        this._escHandler = (e) => {
            if (e.key === 'Escape' && this.visible) this.close()
        }
        document.addEventListener('keydown', this._escHandler)
    },
    beforeDestroy() {
        if (this._escHandler) document.removeEventListener('keydown', this._escHandler)
    }
    // #endif
}
</script>

<style scoped>
/* ---------- 缩略图 ---------- */
.av-wrap { display: inline-block; position: relative; }
.av-thumb { border-radius: 8rpx; cursor: pointer; transition: transform 120ms ease; }
.av-thumb:active { transform: scale(.94); }
.av-round { border-radius: 50%; }

/* ---------- 遮罩层 ---------- */
.av-mask {
    position: fixed; top: 0; left: 0; right: 0; bottom: 0;
    z-index: 9999;
    background: rgba(0, 0, 0, .82);
    display: flex; align-items: center; justify-content: center;
    flex-direction: column;
    animation: av-fade-in 220ms ease both;
}
@keyframes av-fade-in { from { opacity: 0; } to { opacity: 1; } }

/* ---------- 放大图 ---------- */
.av-full {
    width: 560rpx; height: 560rpx;
    border-radius: 50%;
    border: 6rpx solid rgba(255, 255, 255, .9);
    box-shadow: 0 24rpx 80rpx rgba(0, 0, 0, .5);
    transform: scale(.86);
    transition: transform 260ms cubic-bezier(.34, 1.4, .64, 1), filter 200ms ease;
}
.av-mask .av-full { transform: scale(1); }

/* 加载态：缩略图模糊放大占位 */
.av-full.loading { filter: blur(16rpx); }
.av-full.ready { filter: none; }

/* ---------- 姓名标注 ---------- */
.av-cap {
    margin-top: 32rpx;
    color: #fff; font-size: 30rpx; font-weight: 600;
    text-shadow: 0 4rpx 16rpx rgba(0, 0, 0, .6);
}

/* ---------- 关闭提示 & 按钮 ---------- */
.av-tip {
    position: absolute; top: 48rpx; right: 32rpx;
    color: rgba(255, 255, 255, .75); font-size: 26rpx;
}
.av-close {
    margin-top: 48rpx;
    width: 80rpx; height: 80rpx; border-radius: 50%;
    border: 2rpx solid rgba(255, 255, 255, .5);
    background: rgba(255, 255, 255, .12);
    display: flex; align-items: center; justify-content: center;
}
.av-close-x { color: #fff; font-size: 40rpx; line-height: 1; }

/* ---------- 尊重「减弱动效」 ---------- */
@media (prefers-reduced-motion: reduce) {
    .av-mask, .av-full { transition-duration: .01ms !important; animation-duration: .01ms !important; }
}
</style>
