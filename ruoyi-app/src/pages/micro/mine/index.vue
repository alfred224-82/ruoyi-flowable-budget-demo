<template>
    <view class="page">
        <!-- 用户信息 -->
        <view class="card user-card">
            <!-- ★头像：有照片展示照片，无照片降级为首字 -->
            <image v-if="user.avatar" class="avatar-img" :src="avatarSize(user.avatar, 'sm')" mode="aspectFill" @error="user.avatar = ''" />
            <view v-else class="avatar">{{ (user.nickName || '老').charAt(0) }}</view>
            <view class="user-info">
                <text class="name">{{ user.nickName || '-' }}</text>
                <text class="role">老板</text>
            </view>
        </view>

        <!-- 消息通知 -->
        <view class="card">
            <text class="card-title">消息通知</text>
            <view v-if="!messages.length" class="empty">暂无新消息</view>
            <view v-else>
                <view v-for="msg in messages" :key="msg.id" class="msg-item" :class="{ unread: !msg.isRead }" @click="readMsg(msg)">
                    <text class="msg-title">{{ msg.title }}</text>
                    <text class="msg-time">{{ msg.createTime }}</text>
                </view>
            </view>
            <view v-if="unreadCount > 0" class="msg-badge">未读 {{ unreadCount }} 条</view>
        </view>

        <!-- 店铺入口 -->
        <view class="card" @click="go('/pages/micro/shop/config')">
            <text class="link-text">店铺设置 ›</text>
        </view>

        <!-- 切换租户（T29.4：清除租户号 + 登录态 → 回租户号输入页重新登录）-->
        <view class="card" v-if="isMicro" @click="switchTenantNo">
            <text class="link-text">切换租户 ›</text>
        </view>

        <!-- 账号设置 -->
        <view class="card" @click="go('/pages/micro/account/settings')">
            <text class="link-text">账号设置 ›</text>
        </view>

        <!-- 退出登录 -->
        <button class="btn-logout" @click="logout">退出登录</button>
    </view>
</template>

<script>
import { getToken, getUser, setUser, clearAuth, clearAuthTo } from '../../../utils/auth'
import { get, put } from '../../../utils/request'
import { toHttps, avatarSize } from '../../../utils/format'
import { isMicroVersion, clearTenantContext } from '../../../utils/roles'
import { clearTenantNo, clearSelectedStore, clearTenantStoreCandidates } from '../../../utils/store'
import config from '../../../config'

export default {
    data() { return { user: {}, messages: [], unreadCount: 0, isMicro: false } },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        this.user = getUser()
        this.isMicro = isMicroVersion()
        this.loadProfile()
        this.loadMessages()
    },
    methods: {
        toHttps,
        avatarSize,
        go(url) { uni.navigateTo({ url }) },
        /** ★昵称/头像以 /micro/profile 为准刷新（手机号一键登录流程不写用户缓存，仅读缓存会缺头像） */
        async loadProfile() {
            try {
                const res = await get(config.api.microProfile)
                const data = (res && res.data) || {}
                this.user = Object.assign({}, this.user, { nickName: data.nickName || this.user.nickName, avatar: data.avatar || '', phonenumber: data.phonenumber || this.user.phonenumber })
                setUser(this.user)
            } catch (e) { /* ignore */ }
        },
        /** 切换租户（T29.4）：二次确认 → 清除租户号/门店/门店候选/多租户上下文/登录态 → 回租户号输入页 */
        switchTenantNo() {
            uni.showModal({
                title: '切换租户',
                content: '切换租户需清除当前登录并重新输入租户号，是否继续？',
                success: (r) => {
                    if (!r.confirm) return
                    clearTenantNo()
                    clearSelectedStore()
                    // ★同时清门店候选：否则入口分流会把用户从租户号输入页弹回旧门店列表
                    clearTenantStoreCandidates()
                    clearTenantContext()
                    clearAuthTo('/pages/micro/tenant/input')
                }
            })
        },
        async loadMessages() {
            try {
                const [listRes, unreadRes] = await Promise.all([
                    get(config.api.microNotifyList, { pageNum: 1, pageSize: 10 }),
                    get(config.api.microNotifyUnread)
                ])
                this.messages = (listRes && listRes.rows) || []
                this.unreadCount = (unreadRes && unreadRes.data) || 0
            } catch (e) { /* ignore */ }
        },
        async readMsg(msg) {
            if (!msg.isRead) {
                try { await put(config.api.microNotifyRead + '/' + msg.id); msg.isRead = 1; this.unreadCount = Math.max(0, this.unreadCount - 1) } catch (e) { /* ignore */ }
            }
        },
        logout() {
            uni.showModal({
                title: '提示', content: '确认退出登录？',
                success: (res) => { if (res.confirm) clearAuth() }
            })
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 32rpx; margin-bottom: 24rpx; }
.user-card { display: flex; align-items: center; }
.avatar-img { width: 96rpx; height: 96rpx; border-radius: 50%; }
.avatar { width: 96rpx; height: 96rpx; border-radius: 50%; background: #409eff; color: #fff; font-size: 40rpx; display: flex; align-items: center; justify-content: center; }
.user-info { margin-left: 24rpx; }
.name { font-size: 32rpx; font-weight: bold; display: block; }
.role { font-size: 24rpx; color: #409eff; margin-top: 8rpx; display: block; }
.card-title { font-size: 30rpx; font-weight: bold; display: block; margin-bottom: 16rpx; }
.msg-item { padding: 16rpx 0; border-bottom: 1rpx solid #f0f2f5; }
.msg-item.unread { border-left: 6rpx solid #409eff; padding-left: 12rpx; }
.msg-title { font-size: 28rpx; color: #303133; display: block; }
.msg-time { font-size: 22rpx; color: #c0c4cc; margin-top: 8rpx; display: block; }
.msg-badge { font-size: 24rpx; color: #409eff; margin-top: 16rpx; }
.link-text { font-size: 28rpx; color: #409eff; }
.btn-logout { background: #fff; color: #f56c6c; border: 2rpx solid #f56c6c; border-radius: 12rpx; margin-top: 40rpx; font-size: 30rpx; }
.empty { font-size: 26rpx; color: #909399; padding: 20rpx 0; }
</style>
