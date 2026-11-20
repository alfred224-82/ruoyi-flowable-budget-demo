<template>
    <view class="page" :class="themeClass" :style="pageStyle">
        <!-- ★手绘主题装饰贴纸 -->
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-star-tl" :src="iconUrl('hand-star')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-bubble-tr" :src="iconUrl('hand-bubble')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-sparkle-tr" :src="iconUrl('hand-sparkle')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-cloud-bl" :src="iconUrl('hand-cloud')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-sparkle-bl" :src="iconUrl('hand-sparkle')" mode="aspectFit" />

        <!-- ═══ 顶部：头像 + 姓名 + 顾客身份 ═══ -->
        <view class="card user-card" :style="{ background: userCardBg }">
            <image v-if="user.avatar" class="avatar-img" :src="avatarSize(user.avatar, 'md')" mode="aspectFill" @error="user.avatar = ''" />
            <view v-else class="avatar-default">
                <text class="avatar-char">{{ (user.nickName || '客').charAt(0) }}</text>
            </view>
            <view class="user-info">
                <text class="user-name" :style="{ color: userCardText }">{{ user.nickName || '顾客' }}</text>
                <text class="user-role" :style="{ color: userCardSubText }">顾客</text>
            </view>
        </view>

        <!-- ═══ 收款方式说明 ═══ -->
        <view class="card">
            <text class="card-title">收款方式说明</text>
            <text class="pay-desc">{{ payInfo || '本店支持到店付款，服务完成后请向前台或老板付款。本小程序不提供在线支付。' }}</text>
        </view>

        <!-- ═══ 菜单列表 ═══ -->
        <view class="card menu-card">
            <view class="menu-item" @click="go('/pages/micro/customerBook/list')">
                <text class="menu-icon">📅</text>
                <text class="menu-text">我的预约</text>
                <text class="menu-arrow">›</text>
            </view>
            <view class="menu-item" @click="go('/pages/micro/account/settings')">
                <text class="menu-icon">⚙️</text>
                <text class="menu-text">账号设置</text>
                <text class="menu-arrow">›</text>
            </view>
            <view class="menu-item" @click="go('/pages/micro/customer/complaint')">
                <text class="menu-icon">💬</text>
                <text class="menu-text">投诉反馈</text>
                <text class="menu-arrow">›</text>
            </view>
            <view class="menu-item" @click="go('/pages/micro/customer/contactUs')">
                <text class="menu-icon">📞</text>
                <text class="menu-text">联系我们</text>
                <text class="menu-arrow">›</text>
            </view>
        </view>

        <!-- ═══ 退出登录（红色描边二次确认） ═══ -->
        <button class="btn-logout" @click="logout">退出登录</button>

        <!-- ═══ 底部 4 Tab ═══ -->
        <view class="customer-tabbar">
            <view class="tab-item" @click="goHome">
                <text class="tab-icon">🏠</text>
                <text class="tab-text">发现</text>
            </view>
            <view class="tab-item" @click="goPromo">
                <text class="tab-icon">🎫</text>
                <text class="tab-text">我的推广码</text>
            </view>
            <view class="tab-item" @click="goMyActivities">
                <text class="tab-icon">📋</text>
                <text class="tab-text">我的活动</text>
            </view>
            <view class="tab-item active">
                <text class="tab-icon">👤</text>
                <text class="tab-text">我的</text>
            </view>
        </view>
    </view>
</template>

<script>
import { getToken, getUser, setUser, clearAuth } from '../../../utils/auth'
import { get } from '../../../utils/request'
import config from '../../../config'
import { avatarSize } from '../../../utils/format'
import { themeGradient, themeGradientText } from '../../../utils/theme'
import themeMixin from '../../../utils/themeMixin'
import { iconUrl } from '../../../utils/themeIcons'

export default {
    mixins: [themeMixin],
    data() {
        return {
            user: {},
            payInfo: ''
        }
    },
    computed: {
        userCardBg() { return themeGradient(this.themeKey) },
        userCardText() { return themeGradientText(this.themeKey).text },
        userCardSubText() { return themeGradientText(this.themeKey).sub }
    },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        this.user = getUser()
        this.loadProfile()
        this.loadPayInfo()
    },
    methods: {
        avatarSize,
        iconUrl,
        go(url) { uni.navigateTo({ url }) },

        /** ★昵称/头像以 /micro/profile 为准刷新 */
        async loadProfile() {
            try {
                const res = await get(config.api.microProfile)
                const data = (res && res.data) || {}
                this.user = Object.assign({}, this.user, {
                    nickName: data.nickName || this.user.nickName,
                    avatar: data.avatar || '',
                    phonenumber: data.phonenumber || this.user.phonenumber
                })
                setUser(this.user)
            } catch (e) { /* ignore */ }
        },
        async loadPayInfo() {
            try {
                const res = await get(config.api.microCustomerPayInfo)
                this.payInfo = (res && res.data) || ''
            } catch (e) { /* ignore */ }
        },
        /** ★退出登录：红色描边 + 二次确认 */
        logout() {
            uni.showModal({
                title: '提示',
                content: '确认退出登录？',
                confirmColor: '#f56c6c',
                success: (r) => { if (r.confirm) clearAuth() }
            })
        },

        goHome() { uni.reLaunch({ url: '/pages/micro/customer/home' }) },
        goPromo() { uni.navigateTo({ url: '/pages/micro/customer/myPromoCode' }) },
        goMyActivities() { uni.navigateTo({ url: '/pages/micro/customer/myActivities' }) }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 140rpx; min-height: 100vh; background: var(--page-bg, #f5f6f8); position: relative; }
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 28rpx; margin-bottom: 20rpx; box-shadow: var(--shadow-card, none); position: relative; z-index: 1; }

/* 用户卡片 */
.user-card { display: flex; align-items: center; padding: 36rpx 28rpx; }
.avatar-img { width: 108rpx; height: 108rpx; border-radius: 50%; border: 4rpx solid rgba(255,255,255,.6); }
.avatar-default { width: 108rpx; height: 108rpx; border-radius: 50%; background: rgba(255,255,255,.25); display: flex; align-items: center; justify-content: center; border: 4rpx solid rgba(255,255,255,.6); }
.avatar-char { font-size: 48rpx; color: #fff; font-weight: bold; }
.user-info { margin-left: 24rpx; }
.user-name { font-size: 36rpx; font-weight: bold; display: block; }
.user-role { font-size: 26rpx; margin-top: 6rpx; display: block; opacity: .85; }

/* 收款方式 */
.card-title { font-size: 28rpx; font-weight: bold; display: block; margin-bottom: 12rpx; color: var(--text, #303133); }
.pay-desc { font-size: 26rpx; color: var(--text-sub, #606266); line-height: 1.6; }

/* 菜单列表 */
.menu-card { padding: 0 28rpx; }
.menu-item { display: flex; align-items: center; padding: 28rpx 0; border-bottom: 1rpx solid var(--border, #f0f2f5); }
.menu-item:last-child { border-bottom: none; }
.menu-icon { font-size: 36rpx; margin-right: 16rpx; }
.menu-text { flex: 1; font-size: 28rpx; color: var(--text, #303133); }
.menu-arrow { font-size: 32rpx; color: var(--text-sub, #c0c4cc); }

/* 退出登录 */
.btn-logout { background: var(--card-bg, #fff); color: var(--danger, #f56c6c); border: 2rpx solid var(--danger, #f56c6c); border-radius: var(--radius-btn, 12rpx); margin-top: 40rpx; font-size: 30rpx; position: relative; z-index: 1; }

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
.sticker-star-tl { top: 16rpx; left: 16rpx; width: 52rpx; height: 52rpx; transform: rotate(-10deg); }
.sticker-bubble-tr { top: 10rpx; right: 16rpx; width: 56rpx; height: 56rpx; transform: rotate(8deg); }
.sticker-sparkle-tr { top: 40rpx; right: 6rpx; width: 32rpx; height: 32rpx; transform: rotate(-15deg); }
.sticker-cloud-bl { bottom: 140rpx; left: 16rpx; width: 64rpx; height: 52rpx; transform: rotate(5deg); }
.sticker-sparkle-bl { bottom: 160rpx; left: 80rpx; width: 28rpx; height: 28rpx; transform: rotate(15deg); }
</style>
