<template>
    <view class="page">
        <!-- 头像区 -->
        <view class="card profile-card">
            <view class="avatar-area">
                <text class="avatar-text">{{ initials }}</text>
            </view>
            <text class="nick-name">{{ nickName || '业务员' }}</text>
            <text class="role-tag">{{ roleLabel }}</text>
        </view>

        <!-- 功能列表 -->
        <view class="card menu-card">
            <view class="menu-item" @click="goIncome">
                <text class="menu-label">收入汇总</text>
                <text class="menu-arrow">></text>
            </view>
            <view class="menu-item" @click="goPromo">
                <text class="menu-label">我的推广码</text>
                <text class="menu-arrow">></text>
            </view>
            <view class="menu-item" @click="goCustomers">
                <text class="menu-label">我的顾客</text>
                <text class="menu-arrow">></text>
            </view>
        </view>

        <view class="card menu-card">
            <view class="menu-item" @click="goMessages">
                <text class="menu-label">消息中心</text>
                <text class="menu-badge" v-if="unreadCount > 0">{{ unreadCount }}</text>
                <text class="menu-arrow">></text>
            </view>
            <view class="menu-item" @click="handleLogout">
                <text class="menu-label logout">退出登录</text>
                <text class="menu-arrow">></text>
            </view>
        </view>
    </view>
</template>

<script>
import { getToken, removeToken } from '../../../utils/auth'
import { get } from '../../../utils/request'
import config from '../../../config'

export default {
    data() {
        return { nickName: '', roleLabel: '业务员', unreadCount: 0 }
    },
    computed: {
        initials() {
            return (this.nickName || '业').substring(0, 1)
        }
    },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        this.loadInfo()
    },
    methods: {
        async loadInfo() {
            // ★v1.42 昵称改读 /micro/profile（修复 getApp().globalData 全代码库无写入导致昵称恒空）
            try {
                const res = await get(config.api.microProfile)
                const data = (res && res.data) || {}
                this.nickName = data.nickName || ''
                // ★身份标签：后端下发的角色显示名（租户角色映射动态名，如司机），无映射回退通用名
                if (data.roleDisplayName) this.roleLabel = data.roleDisplayName
            } catch (e) { /* ignore */ }
            try {
                const res = await get(config.api.messageUnread)
                this.unreadCount = (res && res.data) || 0
            } catch (e) { /* ignore */ }
        },
        goIncome() { uni.navigateTo({ url: '/pages/micro/staffIncome/index' }) },
        goPromo() { uni.navigateTo({ url: '/pages/micro/staff/promo' }) },
        goCustomers() { uni.navigateTo({ url: '/pages/micro/staff/customers' }) },
        goMessages() { uni.showToast({ title: '消息中心', icon: 'none' }) },
        handleLogout() {
            uni.showModal({
                title: '提示',
                content: '确定退出登录？',
                success: (res) => {
                    if (res.confirm) {
                        removeToken()
                        uni.reLaunch({ url: '/pages/login/index' })
                    }
                }
            })
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; min-height: 100vh; background: #f5f6f8; }
.profile-card { text-align: center; padding: 48rpx 28rpx; }
.avatar-area { width: 120rpx; height: 120rpx; border-radius: 60rpx; background: linear-gradient(135deg, #409eff, #66b1ff); margin: 0 auto 20rpx; display: flex; align-items: center; justify-content: center; }
.avatar-text { color: #fff; font-size: 48rpx; font-weight: bold; }
.nick-name { font-size: 34rpx; font-weight: bold; display: block; }
.role-tag { font-size: 24rpx; color: #909399; display: block; margin-top: 8rpx; }
.menu-card { padding: 0; }
.menu-item { display: flex; align-items: center; padding: 32rpx 28rpx; border-bottom: 1rpx solid #f0f2f5; }
.menu-item:last-child { border-bottom: none; }
.menu-label { flex: 1; font-size: 30rpx; color: #303133; }
.menu-label.logout { color: #f56c6c; }
.menu-badge { background: #f56c6c; color: #fff; font-size: 20rpx; padding: 2rpx 12rpx; border-radius: 20rpx; margin-right: 8rpx; }
.menu-arrow { font-size: 28rpx; color: #c0c4cc; }
</style>
