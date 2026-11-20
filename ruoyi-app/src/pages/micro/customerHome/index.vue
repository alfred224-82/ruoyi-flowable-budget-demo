<template>
    <view class="page" :class="themeClass" :style="pageStyle">
        <!-- ★手绘主题装饰贴纸（仅 theme_handdrawn 显示） -->
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-star-tl" :src="iconUrl('hand-star')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-sparkle-tl" :src="iconUrl('hand-sparkle')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-sparkle-tr" :src="iconUrl('hand-sparkle')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-bubble-tr" :src="iconUrl('hand-bubble')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-bubble-bl" :src="iconUrl('hand-bubble')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-sparkle-bl" :src="iconUrl('hand-sparkle')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-cloud-br" :src="iconUrl('hand-cloud')" mode="aspectFit" />
        <!-- ★ 主题渐变头部（20261016 四主题重设计：随租户主题渐变 + 前景文字色自适应） -->
        <view class="shop-header" :style="{ background: headerBg }">
            <text class="shop-name" :style="{ color: headerText }">{{ tenantName || '欢迎到店' }}</text>
            <text class="shop-desc" :style="{ color: headerSubText }">发现精彩活动，预约心仪服务</text>
        </view>

        <!-- ═══ 板块一：最新活动 ═══ -->
        <view class="section">
            <view class="section-header">
                <text class="section-title">🎉 最新活动</text>
                <text class="section-more" @click="goActivity">查看全部 ›</text>
            </view>
            <view v-if="!actLoaded" class="loading">加载中…</view>
            <view v-else-if="!activities.length" class="empty-tip">暂无活动</view>
            <view v-else class="act-cards">
                <view v-for="item in activities" :key="item.id" class="act-card" @click="viewActivity(item)">
                    <!-- 背景图 -->
                    <image v-if="item.bgImageUrl || item.coverUrl"
                           class="act-bg" :src="toHttps(item.bgImageUrl || firstImage(item.coverUrl))" mode="aspectFill" />
                    <view v-else class="act-bg act-bg-default" :style="{ background: themeBg(item.themeKey) }"></view>
                    <!-- 底部渐变遮罩 -->
                    <view class="act-overlay"></view>
                    <!-- 文字 + 按钮 -->
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

        <!-- ═══ 板块二：门店排班（★T22.5 FEAT_STAFF_SCHEDULE 开关） ═══ -->
        <view v-if="featStaffSchedule" class="section">
            <view class="section-header">
                <text class="section-title">👥 门店排班</text>
                <text v-if="selfBookingOn" class="section-more" @click="goSelfBooking">预约{{ staffRoleName }} ›</text>
            </view>
            <view v-if="!storeScheduleLoaded" class="loading">加载中…</view>
            <view v-else-if="!hasStoreSchedule" class="empty-tip">近两天暂无排班</view>
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

        <!-- ═══ 板块三：自助预约（★FEAT_SELF_BOOKING + 租户策略 customer_self_booking 双开关） ═══ -->
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

        <!-- ★V3.7 底部导航 -->
        <micro-tabbar current="/pages/micro/customerHome/index" />
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get, post } from '../../../utils/request'
import config from '../../../config'
import { toHttps, firstImage, isActivityEnded, isActivityLocked } from '../../../utils/format'
import { getTenantName, getTenantId } from '../../../utils/roles'
import { themeGradient, themeGradientText } from '../../../utils/theme'
import themeMixin from '../../../utils/themeMixin'
import { iconUrl } from '../../../utils/themeIcons'
import { hasFeature } from '../../../utils/features'
import { getRoleDisplayName } from '../../../utils/roleDisplay'
import { getBookingResourcePolicy } from '../../../utils/bookingPolicy'
import MicroTabbar from '../../../components/micro-tabbar/micro-tabbar.vue'

export default {
    components: { MicroTabbar },
    mixins: [themeMixin],
    data() {
        return {
            tenantName: '',
            profile: {},
            pendingScene: '',
            actLoaded: false,
            activities: [],
            detailVisible: false,
            detailItem: {},
            // ★自助预约入口开关（FEAT_SELF_BOOKING + 租户策略 customer_self_booking）
            selfBookingOn: false,
            // ★REQ-01 三类资源控制策略（默认全开放，入口文案据此动态展示）
            policy: {
                projectBookingEnabled: 1, roomBookingEnabled: 1, staffBookingEnabled: 1
            },
            // ★行业角色显示名（salesman 映射：助教/导游/司机，未配置回退员工）
            staffRoleName: '员工',
            // ★T22.5 门店排班
            featStaffSchedule: false,
            storeScheduleLoaded: false,
            storeSchedules: {}  // { '2026-10-10': [...], '2026-10-11': [...] }
        }
    },
    computed: {
        /** 头部主题渐变背景 */
        headerBg() { return themeGradient(this.themeKey) },
        /** 头部渐变上的前景文字色（浅色渐变用深色字，深色/高饱和用白字） */
        headerText() { return themeGradientText(this.themeKey).text },
        headerSubText() { return themeGradientText(this.themeKey).sub },
        /** ★REQ-01 自助预约入口文案：仅列出对客户开放的资源 */
        bookingDesc() {
            const parts = []
            if (this.policy.projectBookingEnabled !== 0) parts.push('项目')
            if (this.policy.roomBookingEnabled !== 0) parts.push('房间')
            if (this.policy.staffBookingEnabled !== 0) parts.push(this.staffRoleName)
            return (parts.length ? parts.join(' / ') : '服务') + '，按时计费'
        },
        /** ★T22.5 是否有门店排班数据 */
        hasStoreSchedule() {
            return Object.keys(this.storeSchedules).some(k => this.storeSchedules[k] && this.storeSchedules[k].length > 0)
        },
        /** ★T22.5 按日期分组（仅展示今明两天） */
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
    onLoad(options) {
        if (options && options.scene) {
            this.pendingScene = decodeURIComponent(options.scene)
        }
    },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        this.tenantName = getTenantName()
        this.loadActivities()
        // ★自助预约入口开关 + 行业角色文案动态化
        this.loadSelfBookingOn()
        getRoleDisplayName('salesman', '员工').then(n => { this.staffRoleName = n })
        // ★T22.5 门店排班（FEAT_STAFF_SCHEDULE 功能开关）
        this.featStaffSchedule = hasFeature('FEAT_STAFF_SCHEDULE')
        if (this.featStaffSchedule) {
            this.loadStoreSchedules()
        }
        if (this.pendingScene) {
            const scene = this.pendingScene
            this.pendingScene = ''
            this.handlePromoScene(scene)
        }
    },
    methods: {
        toHttps,
        firstImage,
        iconUrl,
        themeBg(themeKey) { return themeGradient(themeKey) },

        /** 活动状态文案：已结束 > 已锁定 > 报名进行中 */
        activityStatusText(item) {
            if (isActivityEnded(item)) return '已结束'
            if (isActivityLocked(item)) return '活动已锁定'
            return '报名进行中'
        },
        /** 活动按钮文案：锁定/结束活动对客只读 → 查看活动 */
        activityBtnText(item) {
            return (isActivityEnded(item) || isActivityLocked(item)) ? '查看活动' : '参与活动'
        },

        // ===== T16.6 扫码归因 =====
        async handlePromoScene(scene) {
            try {
                const params = {}
                scene.split('&').forEach(pair => {
                    const [k, v] = pair.split('=', 2)
                    if (k) params[k] = v || ''
                })
                if (!params.u) return
                const profile = await this.ensureProfile()
                const customerUserId = profile.userId
                if (!customerUserId) return
                await post(config.api.microPromoScan, {
                    sceneParam: scene,
                    customerUserId: customerUserId,
                    customerPhone: profile.phonenumber || ''
                })
            } catch (e) { /* 归因失败不阻塞主流程 */ }
        },
        async ensureProfile() {
            if (this.profile.userId) return this.profile
            try {
                const res = await get(config.api.microProfile)
                this.profile = (res && res.data) || {}
            } catch (e) { this.profile = {} }
            return this.profile
        },

        // ===== 最新活动 =====
        async loadActivities() {
            this.actLoaded = false
            try {
                const tid = getTenantId()
                if (!tid) { this.activities = []; return }
                const res = await get(config.api.microActivityBrowse, { tenantId: tid })
                const list = (res && res.data) || []
                // ★防线：已结束活动不上首页（后端 listPublished 已过滤，前端兼容旧后端快照）
                this.activities = list.filter(a => !isActivityEnded(a)).slice(0, 3)
            } catch (e) { this.activities = [] } finally { this.actLoaded = true }
        },
        goActivity() {
            uni.navigateTo({ url: '/pages/micro/activity/browse' })
        },
        goSelfBooking() {
            uni.navigateTo({ url: '/pages/micro/selfBooking/index' })
        },
        /** ★自助预约入口开关：版本能力 FEAT_SELF_BOOKING 与租户策略 customer_self_booking 均需开启；
         *   REQ-01：三类资源全部未开放时隐藏入口 */
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
        viewActivity(item) {
            uni.navigateTo({ url: '/pages/micro/activity/detail?id=' + encodeURIComponent(JSON.stringify(item)) })
        },

        // ===== ★T22.5 门店排班 =====
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
.page { padding: 24rpx; padding-bottom: 140rpx; background: var(--page-bg, #f5f6f8); min-height: 100vh; }

/* ★ 主题渐变头部（背景/文字色由模板 :style 注入，此处为兜底） */
.shop-header {
    background: var(--tgrad, linear-gradient(135deg, #4F8FF7 0%, #7AB8FF 100%));
    border-radius: var(--radius-card, 16rpx);
    padding: 48rpx 36rpx;
    margin-bottom: 32rpx;
}
.shop-name { font-size: 40rpx; font-weight: bold; color: #fff; display: block; }
.shop-desc { font-size: 28rpx; color: rgba(255,255,255,0.85); margin-top: 12rpx; display: block; }

/* ★ 板块通用 */
.section { margin-bottom: 32rpx; }
.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20rpx; }
.section-title { font-size: 34rpx; font-weight: bold; color: var(--text, #1a1a1a); }
.section-more { font-size: 26rpx; color: var(--text-sub, #909399); }

/* ★ 活动大图卡片 */
.act-cards { display: flex; flex-direction: column; gap: 20rpx; }
.act-card {
    position: relative;
    height: 320rpx;
    border-radius: 20rpx;
    overflow: hidden;
}
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
    font-size: 26rpx;
    font-weight: 600;
    padding: 14rpx 32rpx;
    border-radius: 40rpx;
    white-space: nowrap;
    flex-shrink: 0;
    margin-left: 20rpx;
}

/* ★ 自助预约卡片 */
.booking-card {
    display: flex;
    align-items: center;
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
    background: var(--brand, #409eff);
    color: #fff;
    font-size: 28rpx;
    font-weight: 600;
    padding: 18rpx 40rpx;
    border-radius: 40rpx;
    white-space: nowrap;
    flex-shrink: 0;
}

/* ★T22.5 门店排班 */
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

/* ★ 通用 */
.loading { text-align: center; color: var(--text-sub, #909399); padding: 40rpx 0; font-size: 26rpx; }
.empty-tip { text-align: center; color: var(--text-sub, #c0c4cc); padding: 40rpx 0; font-size: 26rpx; }

/* ★手绘主题装饰贴纸 */
.page { position: relative; }
.sticker { position: absolute; opacity: .8; pointer-events: none; z-index: 0; }
.shop-header, .section, .booking-card { position: relative; z-index: 1; }
.sticker-star-tl { top: 16rpx; left: 16rpx; width: 56rpx; height: 56rpx; transform: rotate(-10deg); }
.sticker-sparkle-tl { top: 40rpx; left: 60rpx; width: 36rpx; height: 36rpx; transform: rotate(15deg); }
.sticker-sparkle-tr { top: 20rpx; right: 40rpx; width: 36rpx; height: 36rpx; transform: rotate(-12deg); }
.sticker-bubble-tr { top: 10rpx; right: 10rpx; width: 64rpx; height: 64rpx; transform: rotate(8deg); }
.sticker-bubble-bl { bottom: 160rpx; left: 16rpx; width: 72rpx; height: 72rpx; transform: rotate(-6deg); }
.sticker-sparkle-bl { bottom: 200rpx; left: 80rpx; width: 36rpx; height: 36rpx; transform: rotate(20deg); }
.sticker-cloud-br { bottom: 140rpx; right: 16rpx; width: 90rpx; height: 72rpx; transform: rotate(5deg); }
</style>
