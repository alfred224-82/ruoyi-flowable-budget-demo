<template>
    <view class="page" :class="themeClass" :style="pageStyle">
        <view class="card user-card">
            <!-- ★头像：有照片展示照片，无照片降级为首字 -->
            <image v-if="user.avatar" class="avatar-img" :src="avatarSize(user.avatar, 'sm')" mode="aspectFill" @error="user.avatar = ''" />
            <view v-else class="avatar">{{ (user.nickName || '业').charAt(0) }}</view>
            <view class="user-info">
                <text class="name">{{ user.nickName || '-' }}</text>
                <!-- ★身份：后端下发的角色显示名（租户角色映射动态名，如司机），无映射回退通用名 -->
                <text class="role">{{ user.roleDisplayName || '业务员' }}</text>
            </view>
        </view>
        <view class="card">
            <view class="info-row"><text class="label">手机号</text><text>{{ user.phonenumber || '-' }}</text></view>
        </view>
        <view class="card" @click="go('/pages/micro/account/settings')">
            <text class="link-text">账号设置 ›</text>
        </view>
        <view class="card" @click="go('/pages/micro/staffBook/list')">
            <text class="link-text">我的活动 ›</text>
        </view>
        <button class="btn-logout" @click="logout">退出登录</button>

        <!-- ★V3.7 底部导航 -->
        <micro-tabbar current="/pages/micro/staffMine/index" />
    </view>
</template>

<script>
import { getToken, getUser, setUser, clearAuth } from '../../../utils/auth'
import { get } from '../../../utils/request'
import { toHttps, avatarSize } from '../../../utils/format'
import config from '../../../config'
import MicroTabbar from '../../../components/micro-tabbar/micro-tabbar.vue'
import themeMixin from '../../../utils/themeMixin'

export default {
    components: { MicroTabbar },
    mixins: [themeMixin],
    data() { return { user: {} } },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        this.user = getUser()
        this.loadProfile()
    },
    methods: {
        toHttps,
        avatarSize,
        /** ★昵称/头像以 /micro/profile 为准刷新（手机号一键登录流程不写用户缓存，仅读缓存会缺头像） */
        async loadProfile() {
            try {
                const res = await get(config.api.microProfile)
                const data = (res && res.data) || {}
                this.user = Object.assign({}, this.user, { nickName: data.nickName || this.user.nickName, avatar: data.avatar || '', phonenumber: data.phonenumber || this.user.phonenumber, roleDisplayName: data.roleDisplayName || '' })
                setUser(this.user)
            } catch (e) { /* ignore */ }
        },
        go(url) { uni.navigateTo({ url }) },
        logout() {
            uni.showModal({ title: '提示', content: '确认退出登录？', success: (r) => { if (r.confirm) clearAuth() } })
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 140rpx; background: var(--page-bg, #f5f6f8); }
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 32rpx; margin-bottom: 24rpx; box-shadow: var(--shadow-card, none); }
.user-card { display: flex; align-items: center; }
.avatar-img { width: 96rpx; height: 96rpx; border-radius: 50%; }
.avatar { width: 96rpx; height: 96rpx; border-radius: 50%; background: var(--brand, #67c23a); color: #fff; font-size: 40rpx; display: flex; align-items: center; justify-content: center; }
.user-info { margin-left: 24rpx; }
.name { font-size: 32rpx; font-weight: bold; display: block; color: var(--text, #303133); }
.role { font-size: 24rpx; color: #67c23a; margin-top: 8rpx; display: block; }
.info-row { display: flex; justify-content: space-between; padding: 16rpx 0; font-size: 28rpx; color: var(--text, #303133); }
.info-row .label { color: var(--text-sub, #909399); }
.link-text { font-size: 28rpx; color: var(--brand, #409eff); }
.btn-logout { background: var(--card-bg, #fff); color: #f56c6c; border: 2rpx solid #f56c6c; border-radius: var(--radius-btn, 12rpx); margin-top: 40rpx; font-size: 30rpx; }
</style>
