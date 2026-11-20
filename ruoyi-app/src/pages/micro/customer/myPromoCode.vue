<template>
    <view class="page" :class="themeClass" :style="pageStyle">
        <!-- ★手绘主题装饰贴纸 -->
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-bubble-qr-tr" :src="iconUrl('hand-bubble')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-sparkle-qr-r" :src="iconUrl('hand-sparkle')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-star-prog-l" :src="iconUrl('hand-star')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-cloud-prog-r" :src="iconUrl('hand-cloud')" mode="aspectFit" />

        <!-- ═══ 个人二维码（复用 M16 推广码机制） ═══ -->
        <view class="card">
            <text class="card-title">我的推广码</text>
            <view v-if="!loaded" class="loading">加载中…</view>
            <view v-else-if="!code" class="empty">
                <text class="empty-text">暂无推广码</text>
                <button class="btn" @click="generate">生成推广码</button>
            </view>
            <view v-else class="code-area">
                <!-- ★H5 推广码：顾客手机浏览器扫码直接进入登录页 -->
                <image v-if="qrcodeUrl" class="qr-img" :src="toHttps(qrcodeUrl)" mode="widthFix" show-menu-by-longpress />
                <text class="qr-tip">分享给朋友，朋友扫码进入登录页即算拉新</text>
                <view class="code-meta">
                    <text class="code-no">推广码：{{ code.codeNo || '-' }}</text>
                    <text class="status" :class="code.status === '0' ? 'on' : 'off'">{{ code.status === '0' ? '启用中' : '已禁用' }}</text>
                </view>
                <button v-if="h5Url" class="btn-secondary" @click="copyH5Url">复制推广链接</button>
            </view>
        </view>

        <!-- ═══ 拉新进度（当前成功拉新人数） ═══ -->
        <view v-if="code" class="card">
            <text class="card-title">拉新进度</text>
            <view class="stat-row">
                <view class="stat-cell">
                    <text class="stat-num">{{ broughtCount }}</text>
                    <text class="stat-label">成功拉新人数</text>
                </view>
            </view>
            <text class="stat-tip">扫码进入登录页并注册成功，即计入您的拉新人数</text>
        </view>

        <!-- ═══ 我的优惠券（若有） ═══ -->
        <view v-if="code && myCoupons.length" class="card">
            <text class="card-title">我的优惠券</text>
            <view v-for="c in myCoupons" :key="c.id" class="coupon-row">
                <text class="coupon-name">{{ c.couponName }}</text>
                <text class="coupon-status" :class="c.status === 'USED' ? 'used' : 'unused'">{{ c.status === 'USED' ? '已使用' : '未使用' }}</text>
            </view>
        </view>

        <!-- ═══ 底部 4 Tab ═══ -->
        <view class="customer-tabbar">
            <view class="tab-item" @click="goHome">
                <text class="tab-icon">🏠</text>
                <text class="tab-text">发现</text>
            </view>
            <view class="tab-item active">
                <text class="tab-icon">🎫</text>
                <text class="tab-text">我的推广码</text>
            </view>
            <view class="tab-item" @click="goMyActivities">
                <text class="tab-icon">📋</text>
                <text class="tab-text">我的活动</text>
            </view>
            <view class="tab-item" @click="goMine">
                <text class="tab-icon">👤</text>
                <text class="tab-text">我的</text>
            </view>
        </view>
    </view>
</template>

<script>
import { get, post } from '../../../utils/request'
import config from '../../../config'
import { toHttps } from '../../../utils/format'
import themeMixin from '../../../utils/themeMixin'
import { iconUrl } from '../../../utils/themeIcons'

export default {
    mixins: [themeMixin],
    data() {
        return {
            loaded: false,
            code: null,
            broughtCount: 0,
            myCoupons: [],
            qrcodeUrl: '',
            h5Url: ''
        }
    },
    onShow() {
        this.loadMyPromotion()
        this.loadQrcode()
    },
    methods: {
        toHttps,
        iconUrl,
        /** ★H5 推广码：后端生成 H5 URL 的二维码（base64 图片） */
        async loadQrcode() {
            try {
                const res = await get(config.api.microPromoMyQrcode)
                const data = (res && res.data) || {}
                this.qrcodeUrl = data.qrcodeUrl || ''
                this.h5Url = data.h5Url || ''
            } catch (e) {
                this.qrcodeUrl = ''
                this.h5Url = ''
            }
        },
        async loadMyPromotion() {
            this.loaded = false
            try {
                const res = await get(config.api.microPromoMy)
                const data = (res && res.data) || {}
                this.code = data.code || null
                this.broughtCount = data.broughtCount || 0
                this.myCoupons = data.myCoupons || []
            } catch (e) {
                this.code = null
                this.broughtCount = 0
                this.myCoupons = []
            }
            this.loaded = true
        },
        async generate() {
            try {
                const res = await post(config.api.microPromoGenerate + '?codeType=personal')
                this.code = (res && res.data) || null
                uni.showToast({ title: '推广码已生成', icon: 'success' })
                this.loadQrcode()
            } catch (e) {
                uni.showToast({ title: '生成失败', icon: 'none' })
            }
        },
        copyH5Url() {
            if (!this.h5Url) return
            uni.setClipboardData({
                data: this.h5Url,
                success: () => uni.showToast({ title: '链接已复制', icon: 'none' })
            })
        },
        goHome() { uni.reLaunch({ url: '/pages/micro/customer/home' }) },
        goMyActivities() { uni.navigateTo({ url: '/pages/micro/customer/myActivities' }) },
        goMine() { uni.navigateTo({ url: '/pages/micro/customer/mine' }) }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 140rpx; min-height: 100vh; background: var(--page-bg, #f5f6f8); position: relative; }
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 28rpx; margin-bottom: 20rpx; box-shadow: var(--shadow-card, none); position: relative; z-index: 1; }
.card-title { font-size: 30rpx; font-weight: bold; display: block; margin-bottom: 20rpx; color: var(--text, #303133); }

/* 二维码区 */
.code-area { text-align: center; }
.qr-img { width: 400rpx; height: 400rpx; display: block; margin: 0 auto 16rpx; }
.qr-tip { font-size: 24rpx; color: var(--text-sub, #909399); display: block; text-align: center; margin-bottom: 16rpx; }
.code-meta { display: flex; align-items: center; justify-content: center; gap: 16rpx; margin-bottom: 16rpx; }
.code-no { font-size: 26rpx; color: var(--text-sub, #606266); }
.status { font-size: 24rpx; padding: 4rpx 16rpx; border-radius: var(--radius-tag, 6rpx); }
.status.on { background: #f0f9eb; color: #67c23a; }
.status.off { background: #fef0f0; color: #f56c6c; }

/* 拉新进度 */
.stat-row { display: flex; justify-content: center; gap: 60rpx; padding: 20rpx 0; }
.stat-cell { text-align: center; }
.stat-num { font-size: 64rpx; font-weight: bold; color: var(--brand, #67c23a); display: block; }
.stat-label { font-size: 24rpx; color: var(--text-sub, #909399); display: block; margin-top: 8rpx; }
.stat-tip { display: block; text-align: center; font-size: 22rpx; color: var(--text-sub, #c0c4cc); margin-top: 12rpx; }

/* 优惠券 */
.coupon-row { display: flex; align-items: center; padding: 16rpx 0; border-bottom: 1rpx solid var(--border, #f0f2f5); }
.coupon-row:last-child { border-bottom: none; }
.coupon-name { flex: 1; font-size: 28rpx; color: var(--text, #303133); }
.coupon-status { font-size: 24rpx; padding: 4rpx 16rpx; border-radius: var(--radius-tag, 6rpx); }
.coupon-status.unused { background: #f0f9eb; color: #67c23a; }
.coupon-status.used { background: #f4f4f5; color: #909399; }

/* 按钮 */
.btn { background: var(--brand, #67c23a); color: #fff; border-radius: var(--radius-btn, 12rpx); font-size: 28rpx; margin-top: 20rpx; }
.btn-secondary { background: var(--card-bg, #fff); color: var(--brand, #67c23a); border: 2rpx solid var(--brand, #67c23a); border-radius: var(--radius-btn, 12rpx); font-size: 26rpx; margin-top: 16rpx; }

/* 空态/加载 */
.loading, .empty { text-align: center; color: var(--text-sub, #909399); padding: 60rpx 0; }
.empty-text { display: block; font-size: 26rpx; margin-bottom: 16rpx; }

/* ★ 底部 4 Tab */
.customer-tabbar {
    position: fixed; left: 0; right: 0; bottom: 0; z-index: 900;
    display: flex;
    background: var(--card-bg, #fff);
    border-top: 1rpx solid var(--border, #e4e7ed);
    padding-bottom: env(safe-area-inset-bottom);
}
.tab-item { flex: 1; text-align: center; padding: 12rpx 0 14rpx; }
.tab-icon { font-size: 40rpx; display: block; line-height: 1.2; }
.tab-text { font-size: 22rpx; color: var(--text-sub, #909399); display: block; margin-top: 4rpx; }
.tab-item.active .tab-text { color: var(--brand, #409eff); font-weight: bold; }

/* ★手绘主题装饰贴纸 */
.sticker { position: absolute; opacity: .8; pointer-events: none; z-index: 0; }
.sticker-bubble-qr-tr { top: 10rpx; right: 10rpx; width: 56rpx; height: 56rpx; transform: rotate(8deg); }
.sticker-sparkle-qr-r { top: 60rpx; right: 6rpx; width: 32rpx; height: 32rpx; transform: rotate(15deg); }
.sticker-star-prog-l { bottom: 160rpx; left: 16rpx; width: 44rpx; height: 44rpx; transform: rotate(-12deg); }
.sticker-cloud-prog-r { bottom: 160rpx; right: 16rpx; width: 72rpx; height: 56rpx; transform: rotate(-5deg); }
</style>
