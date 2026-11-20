<template>
    <view class="page" :class="themeClass" :style="pageStyle">
        <!-- ★手绘主题装饰贴纸（仅 theme_handdrawn 显示） -->
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-bubble-qr-tr" :src="iconUrl('hand-bubble')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-sparkle-qr-r" :src="iconUrl('hand-sparkle')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-bubble-qr-l" :src="iconUrl('hand-bubble')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-star-qr-r" :src="iconUrl('hand-star')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-cloud-qr-r" :src="iconUrl('hand-cloud')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-star-prog-l" :src="iconUrl('hand-star')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-sparkle-prog-l" :src="iconUrl('hand-sparkle')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-cloud-prog-r" :src="iconUrl('hand-cloud')" mode="aspectFit" />
        <view class="card">
            <text class="card-title">我的分享码</text>
            <view v-if="!loaded" class="loading">加载中…</view>
            <view v-else-if="!code" class="empty">
                <text>暂无分享码</text>
                <button class="btn" @click="generate">生成分享码</button>
            </view>
            <view v-else class="code-area">
                <!-- ★H5推广码：顾客手机浏览器扫码直接进入登录页 -->
                <image v-if="qrcodeUrl" class="qr-img" :src="toHttps(qrcodeUrl)" mode="widthFix" show-menu-by-longpress />
                <text class="qr-tip">分享给朋友，朋友扫码进入登录页即算拉新</text>
                <text class="status" :class="code.status === '0' ? 'on' : 'off'">{{ code.status === '0' ? '启用中' : '已禁用' }}</text>
            </view>
        </view>

        <view v-if="code" class="card">
            <text class="card-title">拉新进度</text>
            <view class="stat-row">
                <text class="stat-num">{{ broughtCount }}</text>
                <text class="stat-label">成功拉新人数</text>
            </view>
        </view>

        <view v-if="code && myCoupons.length" class="card">
            <text class="card-title">我的优惠券</text>
            <view v-for="c in myCoupons" :key="c.id" class="coupon-row">
                <text class="coupon-name">{{ c.couponName }}</text>
                <text class="coupon-status">{{ c.status === 'USED' ? '已使用' : '未使用' }}</text>
            </view>
        </view>

        <!-- ★V3.7 底部导航 -->
        <micro-tabbar current="/pages/micro/customer/promo" />
    </view>
</template>

<script>
import { get, post } from '../../../utils/request'
import config from '../../../config'
import { toHttps } from '../../../utils/format'
import MicroTabbar from '../../../components/micro-tabbar/micro-tabbar.vue'
import themeMixin from '../../../utils/themeMixin'
import { iconUrl } from '../../../utils/themeIcons'

export default {
    components: { MicroTabbar },
    mixins: [themeMixin],
    data() {
        return { loaded: false, code: null, broughtCount: 0, myCoupons: [], qrcodeUrl: '', h5Url: '' }
    },
    onShow() { this.loadMyPromotion(); this.loadQrcode() },
    methods: {
        toHttps,
        iconUrl,
        /** ★H5推广码：后端生成 H5 URL 的二维码（base64 图片） */
        async loadQrcode() {
            try {
                const res = await get(config.api.microPromoMyQrcode)
                const data = (res && res.data) || {}
                this.qrcodeUrl = data.qrcodeUrl || ''
                this.h5Url = data.h5Url || ''
            } catch (e) { this.qrcodeUrl = '' }
        },
        async loadMyPromotion() {
            this.loaded = false
            try {
                const res = await get(config.api.microPromoMy)
                const data = (res && res.data) || {}
                this.code = data.code || null
                this.broughtCount = data.broughtCount || 0
                this.myCoupons = data.myCoupons || []
            } catch (e) { /* ignore */ }
            this.loaded = true
        },
        async generate() {
            try {
                const res = await post(config.api.microPromoGenerate + '?codeType=personal')
                this.code = (res && res.data) || null
            } catch (e) {
                uni.showToast({ title: '生成失败', icon: 'none' })
            }
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 140rpx; min-height: 100vh; background: var(--page-bg, #f5f6f8); }
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 28rpx; margin-bottom: 20rpx; box-shadow: var(--shadow-card, none); }
.card-title { font-size: 30rpx; font-weight: bold; display: block; margin-bottom: 20rpx; color: var(--text, #303133); }
.code-area { text-align: center; }
.qr-img { width: 360rpx; height: 360rpx; display: block; margin: 0 auto 16rpx; }
.qr-tip { font-size: 24rpx; color: var(--text-sub, #909399); display: block; text-align: center; margin-bottom: 12rpx; }
.scene { font-size: 26rpx; color: var(--text-sub, #606266); display: block; word-break: break-all; margin-bottom: 12rpx; }
.status { font-size: 24rpx; padding: 4rpx 16rpx; border-radius: var(--radius-tag, 6rpx); }
.status.on { background: #f0f9eb; color: #67c23a; }
.status.off { background: #fef0f0; color: #f56c6c; }
.stat-row { text-align: center; }
.stat-num { font-size: 48rpx; font-weight: bold; color: var(--brand, #67c23a); display: block; }
.stat-label { font-size: 24rpx; color: var(--text-sub, #909399); display: block; margin-top: 8rpx; }
.coupon-row { display: flex; align-items: center; padding: 16rpx 0; border-bottom: 1rpx solid var(--border, #f0f2f5); }
.coupon-row:last-child { border-bottom: none; }
.coupon-name { flex: 1; font-size: 28rpx; color: var(--text, #303133); }
.coupon-status { font-size: 24rpx; color: var(--text-sub, #909399); }
.btn { background: var(--brand, #67c23a); color: #fff; border-radius: var(--radius-btn, 12rpx); font-size: 28rpx; margin-top: 20rpx; }
.loading, .empty { text-align: center; color: var(--text-sub, #909399); padding: 60rpx 0; }

/* ★手绘主题装饰贴纸 */
.page { position: relative; }
.sticker { position: absolute; opacity: .8; pointer-events: none; z-index: 0; }
.card { position: relative; z-index: 1; }
.sticker-bubble-qr-tr { top: 10rpx; right: 10rpx; width: 56rpx; height: 56rpx; transform: rotate(8deg); }
.sticker-sparkle-qr-r { top: 60rpx; right: 6rpx; width: 32rpx; height: 32rpx; transform: rotate(15deg); }
.sticker-bubble-qr-l { top: 120rpx; left: 6rpx; width: 52rpx; height: 52rpx; transform: rotate(-6deg); }
.sticker-star-qr-r { top: 100rpx; right: 6rpx; width: 44rpx; height: 44rpx; transform: rotate(-10deg); }
.sticker-cloud-qr-r { top: 160rpx; right: 6rpx; width: 56rpx; height: 44rpx; transform: rotate(5deg); }
.sticker-star-prog-l { bottom: 16rpx; left: 16rpx; width: 44rpx; height: 44rpx; transform: rotate(-12deg); }
.sticker-sparkle-prog-l { bottom: 40rpx; left: 60rpx; width: 28rpx; height: 28rpx; transform: rotate(20deg); }
.sticker-cloud-prog-r { bottom: 16rpx; right: 16rpx; width: 72rpx; height: 56rpx; transform: rotate(-5deg); }
</style>
