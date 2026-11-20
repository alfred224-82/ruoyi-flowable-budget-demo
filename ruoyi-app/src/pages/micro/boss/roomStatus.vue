<template>
    <!-- ★M33 T33.4 房间状态设置（§7.4）
         车辆/房间资源列表 → 逐项设置使用状态（空闲/使用中）→ 使用中不可被预约
         ★行业术语动态化：房间/车辆/场地（读后端行业映射） -->
    <view class="page" :class="'theme-' + theme.key" :style="rootStyle">
        <view class="card">
            <view class="head">
                <text class="title">{{ spaceLabel }}状态设置</text>
                <text class="sub">共 {{ rooms.length }} 项 · 使用中不可被预约</text>
            </view>
            <view v-if="!selfBookingOn" class="empty">自助预约功能未开通，升级版本后可管理{{ spaceLabel }}状态</view>
            <view v-else-if="!loaded" class="empty">加载中…</view>
            <view v-else-if="!rooms.length" class="empty">暂无{{ spaceLabel }}数据</view>
            <view v-else class="list">
                <view v-for="r in rooms" :key="r.id" class="row">
                    <view class="main">
                        <text class="name">{{ r.name }}</text>
                        <text class="pill" :class="'t-' + statusOf(r.resourceStatus).type" :style="pillStyle(statusOf(r.resourceStatus).type)">{{ statusOf(r.resourceStatus).label }}</text>
                    </view>
                    <view class="actions">
                        <button v-if="r.resourceStatus !== 'idle'" class="btn-sm primary" :style="{ background: theme.primaryColor }" @click="set(r, 'idle')">设空闲</button>
                        <button v-if="r.resourceStatus !== 'in_use'" class="btn-sm warn" @click="set(r, 'in_use')">设使用</button>
                    </view>
                </view>
            </view>
        </view>
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get, put } from '../../../utils/request'
import config from '../../../config'
import { getThemeSync, loadTenantTheme, themeCssVars } from '../../../utils/theme'
import { hasFeature } from '../../../utils/features'
import { getSpaceDisplayName } from '../../../utils/roleDisplay'

const ROOM_STATUS = {
    idle: { label: '空闲', type: 'primary-solid' },
    reserved: { label: '预约中', type: 'warn-soft' },
    in_use: { label: '使用中', type: 'warn-solid' }
}

export default {
    data() {
        return {
            theme: getThemeSync(),
            selfBookingOn: false,
            loaded: false,
            rooms: [],
            spaceLabel: '房间'
        }
    },
    computed: { rootStyle() { return themeCssVars(this.theme.key) } },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        uni.setNavigationBarTitle({ title: this.spaceLabel + '状态设置' })
        this.theme = getThemeSync()
        loadTenantTheme().then(t => { this.theme = t })
        this.selfBookingOn = hasFeature('FEAT_SELF_BOOKING')
        getSpaceDisplayName('房间').then(n => {
            this.spaceLabel = n || '房间'
            uni.setNavigationBarTitle({ title: this.spaceLabel + '状态设置' })
        })
        this.load()
    },
    methods: {
        statusOf(s) { return ROOM_STATUS[s] || { label: s || '空闲', type: 'gray' } },
        pillStyle(type) {
            const primary = this.theme.primaryColor || '#409eff'
            if (type === 'primary-solid') return { background: primary, color: '#fff' }
            if (type === 'primary-soft') return { color: primary, background: primary + '1a' }
            return {}
        },
        async load() {
            if (!this.selfBookingOn) { this.loaded = true; return }
            try { const r = await get(config.api.microResourceRoomList); this.rooms = (r && r.data) || [] } catch (e) { this.rooms = [] }
            this.loaded = true
        },
        async set(r, targetStatus) {
            try {
                await put(config.api.microResourceSet + '?resourceType=room&resourceId=' + r.id + '&targetStatus=' + targetStatus)
                uni.showToast({ title: '设置成功', icon: 'success' })
                this.load()
            } catch (e) { /* 请求层统一提示 */ }
        }
    }
}
</script>

<style scoped>
.page { min-height: 100vh; padding: 24rpx; background: var(--page-bg, #f5f6f8); box-sizing: border-box; }
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 24rpx); padding: 32rpx; box-shadow: var(--shadow-card, 0 4rpx 20rpx rgba(0, 0, 0, 0.05)); }
.head { margin-bottom: 24rpx; }
.title { display: block; font-size: 34rpx; font-weight: bold; color: var(--text, #1a1a1a); }
.sub { display: block; font-size: 24rpx; color: var(--text-sub, #909399); margin-top: 8rpx; }
.empty { text-align: center; color: var(--text-sub, #909399); font-size: 26rpx; padding: 60rpx 0; }
.list { display: flex; flex-direction: column; }
.row { display: flex; align-items: center; justify-content: space-between; padding: 24rpx 0; border-bottom: 1rpx solid var(--border, #f5f6f8); }
.row:last-child { border-bottom: none; }
.main { flex: 1; display: flex; align-items: center; overflow: hidden; }
.name { font-size: 28rpx; color: var(--text, #303133); font-weight: 600; margin-right: 16rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pill { display: inline-block; font-size: 22rpx; padding: 4rpx 18rpx; border-radius: 10rpx; flex-shrink: 0; }
.t-warn-solid { background: #e6a23c; color: #fff; }
.t-warn-soft { background: #fdf6ec; color: #e6a23c; }
.t-gray { background: var(--brand-weak, #f0f2f5); color: var(--text-sub, #909399); }
.actions { display: flex; gap: 12rpx; flex-shrink: 0; margin-left: 16rpx; }
.btn-sm { font-size: 24rpx; height: 56rpx; line-height: 56rpx; padding: 0 24rpx; border-radius: 28rpx; border: none; margin: 0; }
.btn-sm::after { border: none; }
.btn-sm.primary { color: #fff; }
.btn-sm.warn { background: #fdf6ec; color: #e6a23c; border: 1rpx solid #f5dab1; }
</style>
