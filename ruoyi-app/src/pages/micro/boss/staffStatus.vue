<template>
    <!-- ★M33 T33.5 助教状态设置（§7.5）
         助教出勤列表 → 逐人设置（在岗/忙碌/休息）→ 忙碌暂停接单 / 休息当日不派单
         ★行业术语动态化：助教/导游/司机/教练 -->
    <view class="page" :class="'theme-' + theme.key" :style="rootStyle">
        <view class="card">
            <view class="head">
                <text class="title">{{ staffLabel }}状态设置</text>
                <text class="sub">共 {{ staff.length }} 人 · 忙碌暂停接单 / 休息当日不派单</text>
            </view>
            <view v-if="!selfBookingOn" class="empty">自助预约功能未开通，升级版本后可管理{{ staffLabel }}状态</view>
            <view v-else-if="!loaded" class="empty">加载中…</view>
            <view v-else-if="!staff.length" class="empty">暂无{{ staffLabel }}数据</view>
            <view v-else class="list">
                <view v-for="u in staff" :key="u.userId" class="row">
                    <view class="main">
                        <image v-if="u.avatar" class="avatar" :src="u.avatar" mode="aspectFill" />
                        <view v-else class="avatar avatar-none"><text>{{ (u.nickName || '?').charAt(0) }}</text></view>
                        <view class="info">
                            <text class="name">{{ u.nickName || '-' }}</text>
                            <text class="pill" :class="'t-' + statusOf(u.resourceStatus).type" :style="pillStyle(statusOf(u.resourceStatus).type)">{{ statusOf(u.resourceStatus).label }}</text>
                        </view>
                    </view>
                    <view class="actions">
                        <button v-if="u.resourceStatus !== 'idle'" class="btn-sm primary-soft" :style="pillStyle('primary-soft')" @click="set(u, 'idle')">在岗</button>
                        <button v-if="u.resourceStatus !== 'working'" class="btn-sm warn" @click="set(u, 'working')">忙碌</button>
                        <button v-if="u.resourceStatus !== 'resting'" class="btn-sm gray" @click="set(u, 'resting')">休息</button>
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
import { toHttps } from '../../../utils/format'
import { getThemeSync, loadTenantTheme, themeCssVars } from '../../../utils/theme'
import { hasFeature } from '../../../utils/features'
import { getRoleDisplayName } from '../../../utils/roleDisplay'

const STAFF_STATUS = {
    idle: { label: '在岗', type: 'primary-soft' },
    working: { label: '忙碌', type: 'warn-soft' },
    resting: { label: '休息', type: 'gray' },
    reserved: { label: '服务中', type: 'warn-solid' }
}

export default {
    data() {
        return {
            theme: getThemeSync(),
            selfBookingOn: false,
            loaded: false,
            staff: [],
            staffLabel: '员工'
        }
    },
    computed: { rootStyle() { return themeCssVars(this.theme.key) } },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        this.theme = getThemeSync()
        loadTenantTheme().then(t => { this.theme = t })
        this.selfBookingOn = hasFeature('FEAT_SELF_BOOKING')
        getRoleDisplayName('salesman', '员工').then(n => {
            this.staffLabel = n || '员工'
            uni.setNavigationBarTitle({ title: this.staffLabel + '状态设置' })
        })
        this.load()
    },
    methods: {
        toHttps,
        statusOf(s) { return STAFF_STATUS[s] || { label: s || '在岗', type: 'gray' } },
        pillStyle(type) {
            const primary = this.theme.primaryColor || '#409eff'
            if (type === 'primary-solid') return { background: primary, color: '#fff' }
            if (type === 'primary-soft') return { color: primary, background: primary + '1a' }
            return {}
        },
        async load() {
            if (!this.selfBookingOn) { this.loaded = true; return }
            try {
                const r = await get(config.api.microResourceStaffList)
                this.staff = ((r && r.data) || []).map(u => Object.assign({}, u, { avatar: toHttps(u.avatar || '') }))
            } catch (e) { this.staff = [] }
            this.loaded = true
        },
        async set(u, targetStatus) {
            try {
                await put(config.api.microResourceSet + '?resourceType=staff&resourceId=' + u.userId + '&targetStatus=' + targetStatus)
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
.row { display: flex; align-items: center; justify-content: space-between; padding: 20rpx 0; border-bottom: 1rpx solid var(--border, #f5f6f8); }
.row:last-child { border-bottom: none; }
.main { flex: 1; display: flex; align-items: center; overflow: hidden; }
.avatar { width: 72rpx; height: 72rpx; border-radius: 50%; background: #f0f2f5; margin-right: 16rpx; flex-shrink: 0; }
.avatar-none { display: flex; align-items: center; justify-content: center; font-size: 26rpx; color: #909399; }
.info { flex: 1; display: flex; align-items: center; overflow: hidden; }
.name { font-size: 28rpx; color: var(--text, #303133); font-weight: 600; margin-right: 16rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pill { display: inline-block; font-size: 22rpx; padding: 4rpx 18rpx; border-radius: 10rpx; flex-shrink: 0; }
.t-warn-solid { background: #e6a23c; color: #fff; }
.t-warn-soft { background: #fdf6ec; color: #e6a23c; }
.t-gray { background: var(--brand-weak, #f0f2f5); color: var(--text-sub, #909399); }
.actions { display: flex; gap: 10rpx; flex-shrink: 0; margin-left: 12rpx; }
.btn-sm { font-size: 22rpx; height: 52rpx; line-height: 52rpx; padding: 0 20rpx; border-radius: 26rpx; border: none; margin: 0; }
.btn-sm::after { border: none; }
.btn-sm.primary-soft { border: 1rpx solid transparent; }
.btn-sm.warn { background: #fdf6ec; color: #e6a23c; border: 1rpx solid #f5dab1; }
.btn-sm.gray { background: var(--brand-weak, #f0f2f5); color: var(--text-sub, #606266); }
</style>
