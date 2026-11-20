<template>
    <view class="page" :class="themeClass" :style="pageStyle">
        <!-- 今日业绩概览 -->
        <view class="card">
            <text class="card-title">今日业绩</text>
            <view class="stat-grid">
                <view class="stat-item">
                    <text class="stat-num">{{ todayOrderCount }}</text>
                    <text class="stat-label">接单数</text>
                </view>
                <view class="stat-item">
                    <text class="stat-num" style="color:#e6a23c;">¥{{ todayRevenue }}</text>
                    <text class="stat-label">营收</text>
                </view>
                <view class="stat-item">
                    <text class="stat-num" style="color:#e64340;">{{ likeCount }}</text>
                    <text class="stat-label">点赞数</text>
                </view>
            </view>
        </view>

        <!-- ★V3.7 我参与的活动（未结束的）——原「今日待办活动」与「我的任务清单」内容一致，合并为一个 -->
        <view class="card">
            <text class="card-title">我参与的活动（未结束的）</text>
            <view class="task-summary">
                <view class="task-item">
                    <text class="task-label">待服务</text>
                    <text class="task-count" style="color:#e6a23c;">{{ pendingCount }}</text>
                </view>
                <view class="task-item">
                    <text class="task-label">已完成</text>
                    <text class="task-count" style="color:#67c23a;">{{ completedCount }}</text>
                </view>
                <view class="task-item">
                    <text class="task-label">本周接单</text>
                    <text class="task-count" style="color:#409eff;">{{ weekOrderCount }}</text>
                </view>
            </view>
            <view v-if="!loaded" class="loading">加载中…</view>
            <view v-else-if="!ongoingList.length" class="empty">暂无未结束的活动</view>
            <view v-else class="todo-list">
                <view v-for="item in ongoingList" :key="item.sourceType + '-' + item.id" class="todo-item">
                    <view class="todo-info">
                        <text class="todo-customer">{{ item.sourceType === 'ACTIVITY' ? (item.title || '活动') : (item.customerName || '顾客') }}</text>
                        <text class="todo-service">{{ item.sourceType === 'ACTIVITY' ? (item.hostNames ? '主持：' + item.hostNames : '推广活动') : (item.serviceName || item.serviceItemName || '-') }}</text>
                        <text class="todo-time">{{ item.sourceType === 'ACTIVITY' ? fmtDateTime(item.startTime) : formatTime(item.bookingTime) }}</text>
                    </view>
                    <view class="todo-action">
                        <text class="status" :style="{ color: item.sourceType === 'ACTIVITY' ? '#e6a23c' : statusColor(item.orderStatus) }">{{ item.sourceType === 'ACTIVITY' ? '待服务' : statusText(item.orderStatus) }}</text>
                        <!-- ★打卡按钮仅预约订单保留；参与活动行纯展示，不需要打卡 -->
                        <template v-if="item.sourceType === 'BOOKING'">
                            <button class="btn-checkin-nav" @click="goCheckIn">打卡</button>
                            <button v-if="item.orderStatus === 'PENDING_SERVICE'" class="btn-checkin" @click="checkinSubmit(item)">打卡完成</button>
                        </template>
                    </view>
                </view>
            </view>
        </view>

        <!-- ★T22.4 我的排班（FEAT_STAFF_SCHEDULE 功能开关，micro_pro 启用） -->
        <view v-if="featStaffSchedule" class="card">
            <view class="card-title-row">
                <text class="card-title">我的排班（本周）</text>
                <text class="card-extra" @click="loadMySchedules">刷新</text>
            </view>
            <view v-if="!scheduleLoaded" class="loading">加载中…</view>
            <view v-else-if="!mySchedules.length" class="empty">本周暂无排班</view>
            <view v-else class="schedule-list">
                <view v-for="(group, date) in groupedSchedules" :key="date" class="schedule-group">
                    <text class="schedule-date">{{ formatDateLabel(date) }}</text>
                    <view class="schedule-items">
                        <view v-for="sch in group" :key="sch.id" class="schedule-item">
                            <view class="shift-tag" :class="'shift-' + sch.shiftType">
                                {{ sch.shiftType === 'morning' ? '早班' : '晚班' }}
                            </view>
                            <text class="shift-time">{{ formatHour(sch.startTime) }} - {{ formatHour(sch.endTime) }}</text>
                            <text v-if="sch.remark" class="shift-remark">{{ sch.remark }}</text>
                        </view>
                    </view>
                </view>
            </view>
        </view>

        <!-- ★V3.7 底部导航 -->
        <micro-tabbar current="/pages/micro/staffHome/index" />
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get, post } from '../../../utils/request'
import config from '../../../config'
import { BOOKING_STATUS } from '../../../utils/format'
import { hasFeature } from '../../../utils/features'
import MicroTabbar from '../../../components/micro-tabbar/micro-tabbar.vue'
import themeMixin from '../../../utils/themeMixin'

export default {
    components: { MicroTabbar },
    mixins: [themeMixin],
    data() {
        return {
            loaded: false,
            schedule: [],
            // ★我参与的活动（推广活动口径，与员工「我的活动」页同源 /micro/staff/myActivities）
            myActivities: [],
            todayOrderCount: 0,
            todayRevenue: '0.00',
            // ★总点赞数（替代原平均评分指标：本人被点赞跨场景汇总）
            likeCount: 0,
            completedCount: 0,
            weekOrderCount: 0,
            // ★T22.4 我的排班
            featStaffSchedule: false,
            scheduleLoaded: false,
            mySchedules: []
        }
    },
    computed: {
        /** ★V3.7 我参与的活动（未结束的）：过滤掉已完成/已取消/已爽约等终态；
         *  ★合并推广活动（被选为主持/助教的未结束活动），与员工「我的活动」页数据同源 */
        ongoingList() {
            const ended = ['COMPLETED', 'CANCELLED', 'NO_SHOW']
            const bookings = this.schedule
                .filter(i => ended.indexOf(i.orderStatus) === -1)
                .map(i => Object.assign({ sourceType: 'BOOKING' }, i))
            const now = Date.now()
            const acts = this.myActivities
                .filter(i => i.sourceType === 'ACTIVITY_SIGNUP'
                    && i.status !== 'ENDED' && i.status !== 'DRAFT'
                    && (!i.endTime || now <= new Date(i.endTime).getTime()))
                .map(i => ({
                    sourceType: 'ACTIVITY',
                    id: i.bizId,
                    title: i.title,
                    hostNames: i.hostNames,
                    startTime: i.startTime,
                    endTime: i.endTime
                }))
            return bookings.concat(acts)
        },
        /** ★待服务 = 待服务预约订单 + 未结束的参与活动 */
        pendingCount() {
            const bookPending = this.schedule.filter(i => i.orderStatus === 'PENDING_SERVICE').length
            const actPending = this.ongoingList.filter(i => i.sourceType === 'ACTIVITY').length
            return bookPending + actPending
        },
        /** ★T22.4 按日期分组排班 */
        groupedSchedules() {
            const groups = {}
            this.mySchedules.forEach(sch => {
                const date = sch.scheduleDate
                if (!groups[date]) groups[date] = []
                groups[date].push(sch)
            })
            // 按日期排序
            const sorted = {}
            Object.keys(groups).sort().forEach(k => { sorted[k] = groups[k] })
            return sorted
        }
    },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        this.loadSchedule()
        this.loadMyActivities()
        this.loadTodayStats()
        this.loadWeekStats()
        this.loadMyLikeCount()
        // ★T22.4 我的排班（FEAT_STAFF_SCHEDULE 功能开关）
        this.featStaffSchedule = hasFeature('FEAT_STAFF_SCHEDULE')
        if (this.featStaffSchedule) {
            this.loadMySchedules()
        }
    },
    methods: {
        statusText(s) { return (BOOKING_STATUS[s] || {}).label || s || '-' },
        statusColor(s) { return (BOOKING_STATUS[s] || {}).color || '#909399' },
        formatTime(t) {
            if (!t) return '-'
            const d = new Date(t)
            if (isNaN(d.getTime())) return String(t).substring(11, 16) || '-'
            return String(d.getHours()).padStart(2, '0') + ':' + String(d.getMinutes()).padStart(2, '0')
        },
        /** 活动行时间：yyyy-MM-dd HH:mm */
        fmtDateTime(t) {
            return t ? String(t).substring(0, 16) : '-'
        },
        async loadSchedule() {
            this.loaded = false
            try {
                const res = await get(config.api.microStaffSchedule)
                this.schedule = (res && res.data) || []
                // ★T18.18 任务清单计数（待服务改由 computed pendingCount 合并活动口径）
                this.completedCount = this.schedule.filter(i => i.orderStatus === 'COMPLETED').length
                this.todayOrderCount = this.completedCount
            } catch (e) { this.schedule = [] } finally { this.loaded = true }
        },
        /** ★我参与的推广活动（与员工「我的活动」页同源接口） */
        async loadMyActivities() {
            try {
                const res = await get(config.api.microStaffMyActivities)
                this.myActivities = (res && res.data) || []
            } catch (e) { this.myActivities = [] }
        },
        async loadTodayStats() {
            try {
                const list = this.schedule
                let revenue = 0
                list.forEach(i => { if (i.orderStatus === 'COMPLETED' && i.amount) revenue += parseFloat(i.amount) })
                this.todayRevenue = revenue.toFixed(2)
            } catch (e) { /* ignore */ }
        },
        async loadWeekStats() {
            try {
                const res = await get(config.api.microStaffIncomeWeek)
                const data = (res && res.data) || {}
                this.weekOrderCount = data.totalOrders || 0
            } catch (e) { /* ignore */ }
        },
        /** ★我的总点赞数（FEAT_RATING 关闭/接口失败降级 0，不阻断首页渲染） */
        async loadMyLikeCount() {
            try {
                const res = await get(config.api.microStaffMyLikeCount)
                this.likeCount = (res && res.data) || 0
            } catch (e) { this.likeCount = 0 }
        },
        goCheckIn() {
            uni.navigateTo({ url: '/pages/micro/checkIn/index' })
        },
        checkinSubmit(item) {
            uni.showModal({
                title: '打卡确认', content: '确认已完成该顾客的服务？',
                success: async (r) => {
                    if (r.confirm) {
                        try {
                            await post(config.api.microStaffCheckinSubmit + '/' + item.id)
                            uni.showToast({ title: '打卡成功', icon: 'success' })
                            this.loadSchedule()
                        } catch (e) { /* 已统一提示 */ }
                    }
                }
            })
        },
        // ===== ★T22.4 我的排班 =====
        async loadMySchedules() {
            this.scheduleLoaded = false
            try {
                // 默认查询本周（week + 当前 ISO 周）
                const now = new Date()
                const iso = this.getIsoWeek(now)
                const periodKey = iso.year + '-W' + String(iso.week).padStart(2, '0')
                const res = await get(config.api.microStaffScheduleMy, {
                    periodType: 'week',
                    periodKey: periodKey
                })
                this.mySchedules = (res && res.data) || []
            } catch (e) {
                this.mySchedules = []
            } finally {
                this.scheduleLoaded = true
            }
        },
        getIsoWeek(date) {
            const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()))
            const dayNum = d.getUTCDay() || 7
            d.setUTCDate(d.getUTCDate() + 4 - dayNum)
            const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1))
            const weekNo = Math.ceil((((d - yearStart) / 86400000) + 1) / 7)
            return { year: d.getUTCFullYear(), week: weekNo }
        },
        formatDateLabel(dateStr) {
            if (!dateStr) return ''
            const d = new Date(dateStr)
            const weekNames = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
            const today = new Date()
            today.setHours(0, 0, 0, 0)
            d.setHours(0, 0, 0, 0)
            const diff = Math.round((d - today) / 86400000)
            let prefix = ''
            if (diff === 0) prefix = '今天 '
            else if (diff === 1) prefix = '明天 '
            else if (diff === -1) prefix = '昨天 '
            return prefix + (d.getMonth() + 1) + '/' + d.getDate() + ' ' + weekNames[d.getDay()]
        },
        formatHour(t) {
            if (!t) return ''
            return t.substring(0, 5)
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 140rpx; min-height: 100vh; background: var(--page-bg, #f5f6f8); }
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 28rpx; margin-bottom: 20rpx; box-shadow: var(--shadow-card, none); }
.card-title { font-size: 30rpx; font-weight: bold; display: block; margin-bottom: 20rpx; color: var(--text, #303133); }
.stat-grid { display: flex; justify-content: space-around; }
.stat-item { text-align: center; }
.stat-num { font-size: 40rpx; font-weight: bold; color: var(--brand, #409eff); display: block; }
.stat-label { font-size: 24rpx; color: var(--text-sub, #909399); display: block; margin-top: 8rpx; }

/* ★T18.18 今日待办 */
.todo-item { display: flex; justify-content: space-between; align-items: center; padding: 20rpx 0; border-bottom: 1rpx solid var(--border, #f0f2f5); }
.todo-item:last-child { border-bottom: none; }
.todo-info { flex: 1; }
.todo-customer { font-size: 28rpx; font-weight: bold; display: block; color: var(--text, #303133); }
.todo-service { font-size: 24rpx; color: var(--text-sub, #606266); margin-top: 6rpx; display: block; }
.todo-time { font-size: 24rpx; color: var(--text-sub, #909399); margin-top: 4rpx; display: block; }
.todo-action { text-align: right; }
.status { font-size: 24rpx; display: block; margin-bottom: 8rpx; }
.btn-checkin-nav { font-size: 24rpx; background: var(--brand, #409eff); color: #fff; border-radius: 8rpx; padding: 8rpx 24rpx; border: none; margin-bottom: 8rpx; }
.btn-checkin { font-size: 24rpx; background: #67c23a; color: #fff; border-radius: 8rpx; padding: 8rpx 24rpx; border: none; }

/* ★T18.18 任务清单 */
.todo-list { margin-top: 8rpx; }
.task-summary { display: flex; justify-content: space-around; text-align: center; padding-bottom: 16rpx; border-bottom: 1rpx solid var(--border, #f0f2f5); }
.task-item { }
.task-label { font-size: 26rpx; color: var(--text-sub, #606266); display: block; }
.task-count { font-size: 48rpx; font-weight: bold; display: block; margin-top: 8rpx; }

/* ★T22.4 我的排班 */
.card-title-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20rpx; }
.card-title-row .card-title { margin-bottom: 0; }
.card-extra { font-size: 24rpx; color: var(--brand, #409eff); }
.schedule-list { }
.schedule-group { padding: 16rpx 0; border-bottom: 1rpx solid var(--border, #f0f2f5); }
.schedule-group:last-child { border-bottom: none; }
.schedule-date { font-size: 26rpx; color: var(--text-sub, #606266); font-weight: bold; display: block; margin-bottom: 12rpx; }
.schedule-items { display: flex; flex-direction: column; gap: 8rpx; }
.schedule-item { display: flex; align-items: center; padding: 12rpx 16rpx; background: var(--brand-weak, #f5f7fa); border-radius: 8rpx; gap: 16rpx; }
.shift-tag { font-size: 22rpx; padding: 4rpx 12rpx; border-radius: 4rpx; color: #fff; }
.shift-morning { background: var(--brand, #409eff); }
.shift-evening { background: #e6a23c; }
.shift-time { font-size: 26rpx; color: var(--text, #303133); font-weight: 500; }
.shift-remark { font-size: 22rpx; color: var(--text-sub, #909399); margin-left: auto; }

.loading, .empty { text-align: center; color: var(--text-sub, #909399); padding: 60rpx 0; }
</style>
