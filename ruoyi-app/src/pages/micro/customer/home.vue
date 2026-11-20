<template>
    <view class="page" :class="themeClass" :style="pageStyle">
        <!-- ★手绘主题装饰贴纸（仅 theme_handdrawn 显示） -->
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-star-tl" :src="iconUrl('hand-star')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-sparkle-tl" :src="iconUrl('hand-sparkle')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-bubble-tr" :src="iconUrl('hand-bubble')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-cloud-br" :src="iconUrl('hand-cloud')" mode="aspectFit" />

        <!-- ═══ 顶部门店名（主题渐变头部） ═══ -->
        <view class="shop-header" :style="{ background: headerBg }">
            <text class="shop-name" :style="{ color: headerText }">{{ tenantName }}</text>
            <text v-if="tenantCity" class="shop-city" :style="{ color: headerSubText }">{{ tenantCity }}</text>
            <text class="shop-desc" :style="{ color: headerSubText }">发现精彩活动，预约心仪服务</text>
        </view>

        <!-- ═══ 板块一：最新活动 ═══ -->
        <view class="section">
            <view class="section-header">
                <text class="section-title">🎉 最新活动</text>
                <text class="section-more" @click="goActivityBrowse">查看全部 ›</text>
            </view>
            <view v-if="!actLoaded" class="loading">加载中…</view>
            <view v-else-if="!activities.length" class="empty-tip">暂无活动</view>
            <view v-else class="act-cards">
                <view v-for="item in activities" :key="item.id" class="act-card" @click="viewActivity(item)">
                    <image v-if="item.bgImageUrl || item.coverUrl"
                           class="act-bg" :src="toHttps(item.bgImageUrl || firstImage(item.coverUrl))" mode="aspectFill" />
                    <view v-else class="act-bg act-bg-default" :style="{ background: themeGradient(item.themeKey || themeKey) }"></view>
                    <view class="act-overlay"></view>
                    <view class="act-bottom">
                        <view class="act-text">
                            <text class="act-title">{{ item.title || '-' }}</text>
                            <text class="act-status">{{ activityStatusText(item) }}</text>
                        </view>
                        <view class="act-btn">{{ activityBtnText(item) }}</view>
                    </view>
                </view>
            </view>
        </view>

        <!-- ═══ 板块二：门店排班（★FEAT_STAFF_SCHEDULE 开关） ═══ -->
        <view v-if="featStaffSchedule" class="section">
            <view class="section-header">
                <text class="section-title">👥 门店排班</text>
                <text v-if="selfBookingOn" class="section-more" @click="goSelfBooking">预约{{ staffRoleName }} ›</text>
            </view>
            <view v-if="!storeScheduleLoaded" class="loading">加载中…</view>
            <view v-else-if="!hasStoreSchedule" class="empty-tip">暂无排班</view>
            <view v-else class="store-schedule">
                <view v-for="(group, date) in storeScheduleGroups" :key="date" class="ss-day">
                    <text class="ss-date">{{ formatDateLabel(date) }}</text>
                    <view class="ss-staff-list">
                        <view v-for="sch in group" :key="sch.id" class="ss-staff">
                            <image v-if="sch.staffAvatar" class="ss-avatar" :src="toHttps(sch.staffAvatar)" mode="aspectFill" />
                            <view v-else class="ss-avatar ss-avatar-default">👤</view>
                            <view class="ss-info">
                                <text class="ss-name">{{ sch.staffName || staffRoleName }}</text>
                                <text class="ss-time">
                                    <text class="ss-shift" :class="'ss-shift-' + sch.shiftType">{{ sch.shiftType === 'morning' ? '早班' : '晚班' }}</text>
                                    {{ formatHour(sch.startTime) }}-{{ formatHour(sch.endTime) }}
                                </text>
                            </view>
                        </view>
                    </view>
                </view>
            </view>
        </view>

        <!-- ═══ 板块三：自助预约入口（★FEAT_SELF_BOOKING + 租户策略双开关） ═══ -->
        <view v-if="selfBookingOn" class="section">
            <view class="section-header">
                <text class="section-title">📅 自助预约</text>
            </view>
            <view class="booking-card" @click="goSelfBooking">
                <text class="booking-icon">🎱</text>
                <view class="booking-info">
                    <text class="booking-title">自助预约</text>
                    <text class="booking-desc">{{ bookingDesc }}</text>
                </view>
                <view class="booking-btn">立即预约</view>
            </view>
        </view>

        <!-- ═══ 底部 4 Tab：发现 / 我的推广码 / 我的活动 / 我的 ═══ -->
        <view class="customer-tabbar">
            <view class="tab-item active">
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
            <view class="tab-item" @click="goMine">
                <text class="tab-icon">👤</text>
                <text class="tab-text">我的</text>
            </view>
        </view>
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get } from '../../../utils/request'
import config from '../../../config'
import { toHttps, firstImage, isActivityEnded, isActivityLocked } from '../../../utils/format'
import { getTenantId } from '../../../utils/roles'
import { themeGradient, themeGradientText } from '../../../utils/theme'
import themeMixin from '../../../utils/themeMixin'
import { iconUrl } from '../../../utils/themeIcons'
import { hasFeature } from '../../../utils/features'
import { getRoleDisplayName } from '../../../utils/roleDisplay'
import { getBookingResourcePolicy } from '../../../utils/bookingPolicy'

export default {
    mixins: [themeMixin],
    data() {
        return {
            tenantName: '欢迎到店',
            tenantCity: '',
            actLoaded: false,
            activities: [],
            // ★自助预约入口开关
            selfBookingOn: false,
            policy: { projectBookingEnabled: 1, roomBookingEnabled: 1, staffBookingEnabled: 1 },
            staffRoleName: '员工',
            // ★T22.5 门店排班
            featStaffSchedule: false,
            storeScheduleLoaded: false,
            storeSchedules: {}
        }
    },
    computed: {
        headerBg() { return themeGradient(this.themeKey) },
        headerText() { return themeGradientText(this.themeKey).text },
        headerSubText() { return themeGradientText(this.themeKey).sub },
        bookingDesc() {
            const parts = []
            if (this.policy.projectBookingEnabled !== 0) parts.push('项目')
            if (this.policy.roomBookingEnabled !== 0) parts.push('房间')
            if (this.policy.staffBookingEnabled !== 0) parts.push(this.staffRoleName)
            return (parts.length ? parts.join(' / ') : '服务') + '，按时计费'
        },
        hasStoreSchedule() {
            return Object.keys(this.storeSchedules).some(k => this.storeSchedules[k] && this.storeSchedules[k].length > 0)
        },
        storeScheduleGroups() {
            const groups = {}
            const today = new Date()
            today.setHours(0, 0, 0, 0)
            const tomorrow = new Date(today.getTime() + 86400000)
            const todayStr = this.toDateStr(today)
            const tomorrowStr = this.toDateStr(tomorrow)
            if (this.storeSchedules[todayStr] && this.storeSchedules[todayStr].length) {
                groups[todayStr] = this.storeSchedules[todayStr]
            }
            if (this.storeSchedules[tomorrowStr] && this.storeSchedules[tomorrowStr].length) {
                groups[tomorrowStr] = this.storeSchedules[tomorrowStr]
            }
            return groups
        }
    },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        this.loadHome()
        this.loadSelfBookingOn()
        getRoleDisplayName('salesman', '员工').then(n => { this.staffRoleName = n })
        this.featStaffSchedule = hasFeature('FEAT_STAFF_SCHEDULE')
        if (this.featStaffSchedule) {
            this.loadStoreSchedules()
        }
    },
    methods: {
        toHttps,
        firstImage,
        iconUrl,
        themeGradient,

        /** ★T32.1 首页聚合：一次拉取门店名 + 最新活动 */
        async loadHome() {
            this.actLoaded = false
            try {
                const res = await get(config.api.microCustomerHome)
                const data = (res && res.data) || {}
                this.tenantName = data.tenantName || '欢迎到店'
                this.tenantCity = data.tenantCity || ''
                const list = Array.isArray(data.activities) ? data.activities : []
                // ★防线：已结束活动不上首页
                this.activities = list.filter(a => !isActivityEnded(a)).slice(0, 3)
            } catch (e) {
                // fail-soft：降级走旧接口
                this.activities = []
                this.loadActivitiesFallback()
            } finally {
                this.actLoaded = true
            }
        },

        /** 降级：直接调活动浏览接口 */
        async loadActivitiesFallback() {
            try {
                const tid = getTenantId()
                if (!tid) return
                const res = await get(config.api.microActivityBrowse, { tenantId: tid })
                const list = (res && res.data) || []
                this.activities = list.filter(a => !isActivityEnded(a)).slice(0, 3)
            } catch (e) { this.activities = [] }
        },

        activityStatusText(item) {
            if (isActivityEnded(item)) return '已结束'
            if (isActivityLocked(item)) return '活动已锁定'
            return '报名进行中'
        },
        activityBtnText(item) {
            return (isActivityEnded(item) || isActivityLocked(item)) ? '查看活动' : '参与活动'
        },

        goActivityBrowse() {
            uni.navigateTo({ url: '/pages/micro/activity/browse' })
        },
        viewActivity(item) {
            uni.navigateTo({ url: '/pages/micro/customer/activityDetail?id=' + encodeURIComponent(JSON.stringify(item)) })
        },
        goSelfBooking() {
            uni.navigateTo({ url: '/pages/micro/selfBooking/index' })
        },
        goPromo() {
            uni.navigateTo({ url: '/pages/micro/customer/myPromoCode' })
        },
        goMyActivities() {
            uni.navigateTo({ url: '/pages/micro/customer/myActivities' })
        },
        goMine() {
            uni.navigateTo({ url: '/pages/micro/customer/mine' })
        },

        async loadSelfBookingOn() {
            if (!hasFeature('FEAT_SELF_BOOKING')) {
                this.selfBookingOn = false
                return
            }
            const p = await getBookingResourcePolicy()
            this.policy = p
            const anyResource = p.projectBookingEnabled !== 0
                || p.roomBookingEnabled !== 0
                || p.staffBookingEnabled !== 0
            this.selfBookingOn = !!p.customerSelfBooking && anyResource
        },

        async loadStoreSchedules() {
            this.storeScheduleLoaded = false
            try {
                const today = new Date()
                today.setHours(0, 0, 0, 0)
                const tomorrow = new Date(today.getTime() + 86400000)
                const res = await get(config.api.microStaffScheduleStore, {
                    startDate: this.toDateStr(today),
                    endDate: this.toDateStr(tomorrow)
                })
                this.storeSchedules = (res && res.data) || {}
            } catch (e) {
                this.storeSchedules = {}
            } finally {
                this.storeScheduleLoaded = true
            }
        },
        toDateStr(d) {
            const y = d.getFullYear()
            const m = String(d.getMonth() + 1).padStart(2, '0')
            const day = String(d.getDate()).padStart(2, '0')
            return y + '-' + m + '-' + day
        },
        formatDateLabel(dateStr) {
            if (!dateStr) return ''
            const d = new Date(dateStr)
            const today = new Date()
            today.setHours(0, 0, 0, 0)
            d.setHours(0, 0, 0, 0)
            const diff = Math.round((d - today) / 86400000)
            if (diff === 0) return '今天'
            if (diff === 1) return '明天'
            return (d.getMonth() + 1) + '/' + d.getDate()
        },
        formatHour(t) {
            if (!t) return ''
            return t.substring(0, 5)
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 140rpx; background: var(--page-bg, #f5f6f8); min-height: 100vh; position: relative; }

/* ★ 主题渐变头部 */
.shop-header {
    background: var(--tgrad, linear-gradient(135deg, #4F8FF7 0%, #7AB8FF 100%));
    border-radius: var(--radius-card, 16rpx);
    padding: 48rpx 36rpx;
    margin-bottom: 32rpx;
    position: relative;
    z-index: 1;
}
.shop-name { font-size: 40rpx; font-weight: bold; color: #fff; display: block; }
.shop-city { font-size: 26rpx; color: rgba(255,255,255,0.85); margin-top: 8rpx; display: block; }
.shop-desc { font-size: 28rpx; color: rgba(255,255,255,0.85); margin-top: 12rpx; display: block; }

/* ★ 板块通用 */
.section { margin-bottom: 32rpx; position: relative; z-index: 1; }
.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20rpx; }
.section-title { font-size: 34rpx; font-weight: bold; color: var(--text, #1a1a1a); }
.section-more { font-size: 26rpx; color: var(--text-sub, #909399); }

/* ★ 活动大图卡片 */
.act-cards { display: flex; flex-direction: column; gap: 20rpx; }
.act-card { position: relative; height: 320rpx; border-radius: 20rpx; overflow: hidden; }
.act-card:active { transform: scale(0.98); }
.act-bg { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }
.act-bg-default { background: var(--tgrad, linear-gradient(135deg, #4F8FF7, #7AB8FF)); }
.act-overlay {
    position: absolute; bottom: 0; left: 0; right: 0; height: 60%;
    background: linear-gradient(to top, rgba(0,0,0,0.75) 0%, transparent 100%);
}
.act-bottom {
    position: absolute; bottom: 0; left: 0; right: 0;
    display: flex; align-items: flex-end; justify-content: space-between;
    padding: 28rpx 32rpx;
}
.act-text { flex: 1; }
.act-title { font-size: 36rpx; font-weight: bold; color: #fff; display: block; line-height: 1.3; }
.act-status { font-size: 26rpx; color: rgba(255,255,255,0.8); margin-top: 8rpx; display: block; }
.act-btn {
    background: var(--card-bg, #fff);
    color: var(--text, #1a1a1a);
    font-size: 26rpx; font-weight: 600;
    padding: 14rpx 32rpx; border-radius: 40rpx;
    white-space: nowrap; flex-shrink: 0; margin-left: 20rpx;
}

/* ★ 自助预约卡片 */
.booking-card {
    display: flex; align-items: center;
    background: var(--card-bg, #fff);
    border-radius: var(--radius-card, 20rpx);
    padding: 36rpx 32rpx;
    box-shadow: var(--shadow-card, 0 4rpx 20rpx rgba(0,0,0,0.04));
}
.booking-card:active { transform: scale(0.98); }
.booking-icon { font-size: 64rpx; margin-right: 24rpx; flex-shrink: 0; }
.booking-info { flex: 1; }
.booking-title { font-size: 34rpx; font-weight: bold; color: var(--text, #1a1a1a); display: block; }
.booking-desc { font-size: 26rpx; color: var(--text-sub, #909399); margin-top: 8rpx; display: block; }
.booking-btn {
    background: var(--brand, #409eff); color: #fff;
    font-size: 28rpx; font-weight: 600;
    padding: 18rpx 40rpx; border-radius: 40rpx;
    white-space: nowrap; flex-shrink: 0;
}

/* ★ 门店排班 */
.store-schedule { display: flex; flex-direction: column; gap: 20rpx; }
.ss-day { background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 24rpx; }
.ss-date { font-size: 28rpx; font-weight: bold; color: var(--text, #303133); display: block; margin-bottom: 16rpx; }
.ss-staff-list { display: flex; flex-direction: column; gap: 16rpx; }
.ss-staff { display: flex; align-items: center; gap: 16rpx; }
.ss-avatar { width: 64rpx; height: 64rpx; border-radius: 50%; flex-shrink: 0; background: var(--brand-weak, #f0f2f5); }
.ss-avatar-default { display: flex; align-items: center; justify-content: center; font-size: 32rpx; }
.ss-info { flex: 1; }
.ss-name { font-size: 28rpx; color: var(--text, #303133); font-weight: 500; display: block; }
.ss-time { font-size: 24rpx; color: var(--text-sub, #909399); margin-top: 4rpx; display: block; }
.ss-shift { display: inline-block; font-size: 20rpx; padding: 2rpx 10rpx; border-radius: 4rpx; margin-right: 8rpx; color: #fff; }
.ss-shift-morning { background: var(--brand, #409eff); }
.ss-shift-evening { background: #e6a23c; }

/* ★ 底部 4 Tab（发现 / 我的推广码 / 我的活动 / 我的） */
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

/* ★ 通用 */
.loading { text-align: center; color: var(--text-sub, #909399); padding: 40rpx 0; font-size: 26rpx; }
.empty-tip { text-align: center; color: var(--text-sub, #c0c4cc); padding: 40rpx 0; font-size: 26rpx; }

/* ★手绘主题装饰贴纸 */
.sticker { position: absolute; opacity: .8; pointer-events: none; z-index: 0; }
.sticker-star-tl { top: 16rpx; left: 16rpx; width: 56rpx; height: 56rpx; transform: rotate(-10deg); }
.sticker-sparkle-tl { top: 40rpx; left: 60rpx; width: 36rpx; height: 36rpx; transform: rotate(15deg); }
.sticker-bubble-tr { top: 10rpx; right: 10rpx; width: 64rpx; height: 64rpx; transform: rotate(8deg); }
.sticker-cloud-br { bottom: 140rpx; right: 16rpx; width: 90rpx; height: 72rpx; transform: rotate(5deg); }
</style>
