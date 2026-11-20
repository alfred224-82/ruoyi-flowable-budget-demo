<template>
    <!-- ★M33 T33.3 状态设置中心（§7.3）
         四类状态各一张卡片：房间状态设置 / 助教状态设置 / 活动状态设置 / 预约失败通知
         ★与 bossTodo/index.vue 枢纽态同口径，独立路由便于从老板端 H5 菜单直达 -->
    <view class="page" :class="'theme-' + theme.key" :style="rootStyle">
        <view class="hub-wrap">
            <!-- ① 空间资源状态设置（★行业术语动态化：房间/车辆/场地） -->
            <view class="hub-card" @click="go('/pages/micro/boss/roomStatus')">
                <view class="hub-row">
                    <view class="hub-icon bg-blue">🖥️</view>
                    <view class="hub-main">
                        <text class="hub-title">{{ spaceLabel }}状态设置</text>
                        <text class="hub-desc">管理当前{{ spaceLabel }}开放、占用与维护状态</text>
                        <text class="hub-pill" :class="'t-' + roomPill.type" :style="pillStyle(roomPill.type)">{{ roomPill.label }}</text>
                    </view>
                    <text class="hub-arrow">›</text>
                </view>
            </view>

            <!-- ② 员工状态设置（★行业术语动态化：助教/导游/司机/教练） -->
            <view class="hub-card" @click="go('/pages/micro/boss/staffStatus')">
                <view class="hub-row">
                    <view class="hub-icon bg-blue">👤</view>
                    <view class="hub-main">
                        <text class="hub-title">{{ staffLabel }}状态设置</text>
                        <text class="hub-desc">设置{{ staffLabel }}在线、忙碌与休息状态</text>
                        <text class="hub-pill" :class="'t-' + staffPill.type" :style="pillStyle(staffPill.type)">{{ staffPill.label }}</text>
                    </view>
                    <text class="hub-arrow">›</text>
                </view>
            </view>

            <!-- ③ 活动状态设置 -->
            <view class="hub-card" @click="go('/pages/micro/boss/activityList')">
                <view class="hub-row">
                    <view class="hub-icon bg-blue">📅</view>
                    <view class="hub-main">
                        <text class="hub-title">活动状态设置</text>
                        <text class="hub-desc">控制活动报名、进行与结束状态</text>
                        <text class="hub-pill" :class="'t-' + actPill.type" :style="pillStyle(actPill.type)">{{ actPill.label }}</text>
                    </view>
                    <text class="hub-arrow">›</text>
                </view>
            </view>

            <!-- ④ 预约失败通知（右上角计数徽章） -->
            <view class="hub-card" @click="go('/pages/micro/boss/failNotify')">
                <text v-if="failedCount" class="hub-badge">{{ failedCount }}条</text>
                <view class="hub-row">
                    <view class="hub-icon bg-warn">⚠️</view>
                    <view class="hub-main">
                        <text class="hub-title">预约失败通知</text>
                        <text class="hub-desc">查看未成功预约记录与原因</text>
                    </view>
                    <text class="hub-arrow">›</text>
                </view>
            </view>
        </view>

        <micro-tabbar current="/pages/micro/boss/statusCenter" />
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get } from '../../../utils/request'
import config from '../../../config'
import { getThemeSync, loadTenantTheme, themeCssVars } from '../../../utils/theme'
import { hasFeature } from '../../../utils/features'
import { getRoleDisplayName, getSpaceDisplayName } from '../../../utils/roleDisplay'
import MicroTabbar from '../../../components/micro-tabbar/micro-tabbar.vue'

export default {
    components: { MicroTabbar },
    data() {
        return {
            theme: getThemeSync(),
            selfBookingOn: false,
            rooms: [],
            staff: [],
            activities: [],
            failedCount: 0,
            staffLabel: '员工',
            spaceLabel: '房间'
        }
    },
    computed: {
        rootStyle() { return themeCssVars(this.theme.key) },
        roomPill() {
            if (!this.selfBookingOn) return { label: '未开通', type: 'gray' }
            if (!this.rooms.length) return { label: '暂无' + this.spaceLabel, type: 'gray' }
            const busy = this.rooms.some(r => r.resourceStatus === 'in_use' || r.resourceStatus === 'reserved')
            return busy ? { label: '占用中', type: 'warn-solid' } : { label: '空闲', type: 'primary-solid' }
        },
        staffPill() {
            if (!this.selfBookingOn) return { label: '未开通', type: 'gray' }
            if (!this.staff.length) return { label: '暂无' + this.staffLabel, type: 'gray' }
            const onDuty = this.staff.some(u => u.resourceStatus === 'idle' || u.resourceStatus === 'working')
            return onDuty ? { label: '在岗', type: 'primary-soft' } : { label: '休息中', type: 'gray' }
        },
        actPill() {
            if (!this.activities.length) return { label: '暂无活动', type: 'gray' }
            const a = this.activities[0]
            if (a.status === 'DRAFT') return { label: '已下架', type: 'gray' }
            if (a.status === 'ENDED') return { label: '已结束', type: 'gray' }
            if (a.isLocked) return { label: '已锁定', type: 'warn-solid' }
            return { label: '报名中', type: 'success-soft' }
        }
    },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        this.theme = getThemeSync()
        loadTenantTheme().then(t => { this.theme = t })
        this.selfBookingOn = hasFeature('FEAT_SELF_BOOKING')
        getRoleDisplayName('salesman', '员工').then(n => { this.staffLabel = n || '员工' })
        getSpaceDisplayName('房间').then(n => { this.spaceLabel = n || '房间' })
        this.loadSummary()
    },
    methods: {
        go(url) { uni.navigateTo({ url }) },
        pillStyle(type) {
            const primary = this.theme.primaryColor || '#409eff'
            if (type === 'primary-solid') return { background: primary, color: '#fff' }
            if (type === 'primary-soft') return { color: primary, background: primary + '1a' }
            return {}
        },
        async loadSummary() {
            if (this.selfBookingOn) {
                try { const r = await get(config.api.microResourceRoomList); this.rooms = (r && r.data) || [] } catch (e) { this.rooms = [] }
                try { const r = await get(config.api.microResourceStaffList); this.staff = (r && r.data) || [] } catch (e) { this.staff = [] }
                try {
                    const r = await get(config.api.microNotifyList, { messageType: 'RESERVATION_FAILED', pageNum: 1, pageSize: 1 })
                    this.failedCount = (r && r.total) || 0
                } catch (e) { this.failedCount = 0 }
            }
            try { const r = await get(config.api.microActivityStats); this.activities = ((r && r.data) || []).slice(0, 5) } catch (e) { this.activities = [] }
        }
    }
}
</script>

<style scoped>
.page { min-height: 100vh; box-sizing: border-box; padding: 24rpx 24rpx 140rpx; background: var(--page-bg, #f5f6f8); }
.hub-wrap { display: flex; flex-direction: column; gap: 24rpx; }
.hub-card { position: relative; background: var(--card-bg, #fff); border-radius: var(--radius-card, 24rpx); padding: 28rpx 32rpx; box-shadow: var(--shadow-card, 0 4rpx 20rpx rgba(0, 0, 0, 0.05)); overflow: hidden; }
.hub-row { display: flex; align-items: center; }
.hub-icon { width: 88rpx; height: 88rpx; border-radius: 20rpx; display: flex; align-items: center; justify-content: center; font-size: 44rpx; margin-right: 24rpx; flex-shrink: 0; }
.bg-blue { background: var(--brand-weak, #e8f1fb); }
.bg-warn { background: #fdf6ec; }
.hub-main { flex: 1; overflow: hidden; }
.hub-title { display: block; font-size: 32rpx; font-weight: bold; color: var(--text, #1a1a1a); margin-bottom: 8rpx; }
.hub-desc { display: block; font-size: 24rpx; color: var(--text-sub, #909399); margin-bottom: 12rpx; }
.hub-pill { display: inline-block; font-size: 22rpx; padding: 4rpx 20rpx; border-radius: 10rpx; }
.t-warn-solid { background: #e6a23c; color: #fff; }
.t-warn-soft { background: #fdf6ec; color: #e6a23c; }
.t-success-soft { background: #e8f8e0; color: #52b647; }
.t-gray { background: var(--brand-weak, #f0f2f5); color: var(--text-sub, #909399); }
.hub-arrow { font-size: 44rpx; color: #c0c4cc; margin-left: 16rpx; flex-shrink: 0; }
.hub-badge { position: absolute; top: 0; right: 0; background: #f56c6c; color: #fff; font-size: 22rpx; padding: 6rpx 20rpx; border-radius: 0 24rpx 0 20rpx; z-index: 1; }
</style>
