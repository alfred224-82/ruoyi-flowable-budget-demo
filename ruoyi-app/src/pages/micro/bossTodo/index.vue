<template>
    <view class="page" :class="'theme-' + theme.key" :style="rootStyle">
        <!-- ★V3.8 状态设置中心：一整屏固定布局（页面不滚动）
             枢纽态=四卡均分屏高；点「去设置/去处理」整屏切换到设置列表（带返回） -->

        <!-- 枢纽态：四张状态卡 -->
        <view v-if="!panel" class="hub-wrap">
            <!-- ① 空间资源状态设置（★BUG-03 行业术语动态化：房间/车辆/场地） -->
            <view class="hub-card">
                <view class="hub-row">
                    <view class="hub-icon bg-blue">🖥️</view>
                    <view class="hub-main">
                        <text class="hub-title">{{ spaceLabel }}状态设置</text>
                        <text class="hub-desc">管理当前{{ spaceLabel }}开放、占用与维护状态</text>
                        <text class="hub-pill" :class="'t-' + roomPill.type" :style="pillStyle(roomPill.type)">{{ roomPill.label }}</text>
                    </view>
                    <button class="hub-btn solid-primary" :style="{ background: theme.primaryColor }" @click="openPanel('room')">去设置</button>
                </view>
            </view>

            <!-- ② 员工状态设置（★BUG-03 行业术语动态化：助教/导游/司机/教练） -->
            <view class="hub-card">
                <view class="hub-row">
                    <view class="hub-icon bg-blue">👤</view>
                    <view class="hub-main">
                        <text class="hub-title">{{ staffLabel }}状态设置</text>
                        <text class="hub-desc">设置{{ staffLabel }}在线、忙碌与休息状态</text>
                        <text class="hub-pill" :class="'t-' + staffPill.type" :style="pillStyle(staffPill.type)">{{ staffPill.label }}</text>
                    </view>
                    <button class="hub-btn soft-warn" @click="openPanel('staff')">去设置</button>
                </view>
            </view>

            <!-- ③ 活动状态设置 -->
            <view class="hub-card">
                <view class="hub-row">
                    <view class="hub-icon bg-blue">📅</view>
                    <view class="hub-main">
                        <text class="hub-title">活动状态设置</text>
                        <text class="hub-desc">控制活动报名、进行与结束状态</text>
                        <text class="hub-pill" :class="'t-' + actPill.type" :style="pillStyle(actPill.type)">{{ actPill.label }}</text>
                    </view>
                    <button class="hub-btn soft-primary" :style="pillStyle('primary-soft')" @click="openPanel('activity')">去设置</button>
                </view>
            </view>

            <!-- ④ 预约失败通知（右上角计数徽章） -->
            <view class="hub-card">
                <text v-if="failedNotices.length" class="hub-badge">{{ failedNotices.length }}条</text>
                <view class="hub-row">
                    <view class="hub-icon bg-warn">⚠️</view>
                    <view class="hub-main">
                        <text class="hub-title">预约失败通知</text>
                        <text class="hub-desc">查看未成功预约记录与原因</text>
                    </view>
                    <button class="hub-btn solid-warn" @click="openPanel('notify')">去处理</button>
                </view>
            </view>

            <!-- ★业务待办/异常：仅有数据时展示（压缩枢纽卡高度，仍保持一屏） -->
            <!-- 预约冲突处理：待服务预约 -->
            <view v-if="pendingBookings.length" class="card shrink">
                <view class="card-head">
                    <text class="card-title">预约冲突处理</text>
                    <text class="link" :style="{ color: theme.primaryColor }" @click="goBookingAll">全部预约 ›</text>
                </view>
                <view>
                    <view v-for="item in pendingBookings" :key="item.id" class="row">
                        <view class="row-main">
                            <text class="row-title">{{ item.customerName || '顾客' }} · {{ item.serviceName || '-' }}</text>
                            <text class="row-sub">{{ item.bookingTime || '-' }}</text>
                        </view>
                        <view class="row-action">
                            <text class="status" :style="{ color: statusColor(item.orderStatus) }">{{ statusText(item.orderStatus) }}</text>
                            <button v-if="item.orderStatus === 'PENDING_SERVICE'" class="btn-sm solid-primary"
                                    :style="{ background: theme.primaryColor }" @click="goBookingDetail(item.id)">处理</button>
                        </view>
                    </view>
                </view>
            </view>

            <!-- 固定支出异常提醒（仅 micro_pro，有异常才展示） -->
            <view v-if="isPro && costAnomalyCount > 0" class="card shrink warn" @click="goCost">
                <view class="anomaly">
                    <text class="anomaly-num" style="color: #f56c6c;">{{ costAnomalyCount }}</text>
                    <text class="anomaly-label">项支出异常，请及时核对</text>
                </view>
            </view>

            <!-- 免费版：升级引导 -->
            <view v-if="!isPro" class="card shrink upgrade" :style="{ borderLeftColor: theme.primaryColor }">
                <text class="card-title">升级解锁更多异常处理</text>
                <text class="desc">固定支出异常提醒等付费档功能，升级后可在此统一处理</text>
            </view>
        </view>

        <!-- 设置态：整屏列表（仅列表区可滚动，页面仍一屏） -->
        <view v-else class="panel-view">
            <view class="panel-card">
                <view class="panel-head">
                    <text class="panel-title">{{ panelTitle }}</text>
                    <text class="panel-back" :style="{ color: theme.primaryColor }" @click="closePanel">‹ 返回</text>
                </view>
                <scroll-view class="panel-scroll" scroll-y>
                    <!-- 空间资源状态列表（★BUG-03 动态行业术语） -->
                    <view v-if="panel === 'room'">
                        <view v-if="!selfBookingOn" class="empty">自助预约功能未开通，升级版本后可管理{{ spaceLabel }}状态</view>
                        <view v-else-if="!rooms.length" class="empty">暂无{{ spaceLabel }}数据</view>
                        <view v-else>
                            <view v-for="r in rooms" :key="r.id" class="p-row">
                                <view class="p-main">
                                    <text class="p-name">{{ r.name }}</text>
                                    <text class="hub-pill sm" :class="'t-' + roomStatus(r.resourceStatus).type"
                                          :style="pillStyle(roomStatus(r.resourceStatus).type)">{{ roomStatus(r.resourceStatus).label }}</text>
                                </view>
                                <view class="p-actions">
                                    <button v-if="r.resourceStatus !== 'idle'" class="btn-sm solid-primary"
                                            :style="{ background: theme.primaryColor }" @click="setRoom(r, 'idle')">设空闲</button>
                                    <button v-if="r.resourceStatus !== 'in_use'" class="btn-sm soft-warn" @click="setRoom(r, 'in_use')">设使用</button>
                                </view>
                            </view>
                        </view>
                    </view>
                    <!-- 员工状态列表（★BUG-03 动态行业术语） -->
                    <view v-else-if="panel === 'staff'">
                        <view v-if="!selfBookingOn" class="empty">自助预约功能未开通，升级版本后可管理{{ staffLabel }}状态</view>
                        <view v-else-if="!staff.length" class="empty">暂无{{ staffLabel }}数据</view>
                        <view v-else>
                            <view v-for="u in staff" :key="u.userId" class="p-row">
                                <view class="p-main">
                                    <text class="p-name">{{ u.nickName }}</text>
                                    <text class="hub-pill sm" :class="'t-' + staffStatus(u.resourceStatus).type"
                                          :style="pillStyle(staffStatus(u.resourceStatus).type)">{{ staffStatus(u.resourceStatus).label }}</text>
                                </view>
                                <view class="p-actions">
                                    <button v-if="u.resourceStatus !== 'idle'" class="btn-sm soft-primary"
                                            :style="pillStyle('primary-soft')" @click="setStaff(u, 'idle')">在岗</button>
                                    <button v-if="u.resourceStatus !== 'working'" class="btn-sm soft-warn" @click="setStaff(u, 'working')">忙碌</button>
                                    <button v-if="u.resourceStatus !== 'resting'" class="btn-sm gray" @click="setStaff(u, 'resting')">休息</button>
                                </view>
                            </view>
                        </view>
                    </view>
                    <!-- 活动状态列表（★纯列表，操作按钮在详情页） -->
                    <view v-else-if="panel === 'activity'">
                        <view v-if="!activities.length" class="empty">暂无活动</view>
                        <view v-else>
                            <view v-for="a in activities" :key="a.id" class="p-row" @click="viewActivity(a)">
                                <view class="p-main">
                                    <text class="p-name">{{ a.title || '-' }}</text>
                                    <text class="hub-pill sm" :class="'t-' + actStatus(a).type"
                                          :style="pillStyle(actStatus(a).type)">{{ actStatus(a).label }}</text>
                                </view>
                                <text class="row-arrow">›</text>
                            </view>
                        </view>
                        <view class="panel-link" :style="{ color: theme.primaryColor }" @click="goHistoryAll">查看历史活动 ›</view>
                    </view>
                    <!-- 预约失败通知列表 -->
                    <view v-else>
                        <view v-if="!failedNotices.length" class="empty">暂无预约失败通知</view>
                        <view v-else>
                            <view v-for="msg in failedNotices" :key="msg.id" class="p-row col">
                                <view class="p-main col-main">
                                    <text class="p-name">{{ msg.title || '预约失败' }}</text>
                                    <text class="p-sub">{{ msg.content || '' }}</text>
                                    <text class="p-sub time">{{ formatTime(msg.createTime) }}</text>
                                </view>
                                <button class="btn-sm success" @click="rebook(msg)">重新预约</button>
                            </view>
                        </view>
                    </view>
                </scroll-view>
            </view>
        </view>

        <!-- ★V3.7 底部导航 -->
        <micro-tabbar current="/pages/micro/bossTodo/index" />
    </view>
</template>

<script>
import { getToken, getVersionType } from '../../../utils/auth'
import { get, put } from '../../../utils/request'
import config from '../../../config'
import { BOOKING_STATUS } from '../../../utils/format'
import { getSelectedTenant } from '../../../utils/roles'
import { getThemeSync, loadTenantTheme, themeCssVars } from '../../../utils/theme'
import { hasFeature } from '../../../utils/features'
// ★BUG-03 行业术语动态化：员工名（助教/导游/司机）+ 空间名（房间/车辆/场地）读后端行业映射
import { getRoleDisplayName, getSpaceDisplayName } from '../../../utils/roleDisplay'
import MicroTabbar from '../../../components/micro-tabbar/micro-tabbar.vue'

// ★BUG-03 PANEL_TITLE 中固定项仅保留活动/预约失败通知；房间/员工面板标题改为 computed 拼接
const PANEL_TITLE_STATIC = {
    activity: '活动状态设置',
    notify: '预约失败通知'
}
/** 房间资源状态（/micro/resourceStatus：idle/reserved/in_use） */
const ROOM_STATUS = {
    idle: { label: '空闲', type: 'primary-solid' },
    reserved: { label: '预约中', type: 'warn-soft' },
    in_use: { label: '使用中', type: 'warn-solid' }
}
/** 助教资源状态（idle/working/resting/reserved） */
const STAFF_STATUS = {
    idle: { label: '在岗', type: 'primary-soft' },
    working: { label: '忙碌', type: 'warn-soft' },
    resting: { label: '休息', type: 'gray' },
    reserved: { label: '服务中', type: 'warn-solid' }
}

export default {
    components: { MicroTabbar },
    data() {
        return {
            theme: getThemeSync(),
            isPro: false,
            // 自助预约功能开关（FEAT_SELF_BOOKING，资源状态接口功能门槛）
            selfBookingOn: false,
            // ★M28 活动锁定功能开关（FEAT_ACTIVITY_LOCK，锁定/解锁按钮门槛）
            activityLockOn: false,
            // 当前设置面板（room/staff/activity/notify，空=枢纽态）
            panel: '',
            // 枢纽卡数据源
            rooms: [],
            staff: [],
            activities: [],
            failedNotices: [],
            // 业务待办/异常（有数据才展示）
            pendingBookings: [],
            costAnomalyCount: 0,
            // ★BUG-03 行业术语动态化：默认回退名，onShow 后异步替换为租户行业映射名
            staffLabel: '员工',
            spaceLabel: '房间'
        }
    },
    computed: {
        /** ★20261016 四主题重设计：根节点注入完整主题令牌 */
        rootStyle() {
            return themeCssVars(this.theme.key)
        },
        /** 设置态标题（★BUG-03 房间/员工面板标题拼接行业名） */
        panelTitle() {
            if (this.panel === 'room') return this.spaceLabel + '状态设置'
            if (this.panel === 'staff') return this.staffLabel + '状态设置'
            return PANEL_TITLE_STATIC[this.panel] || ''
        },
        /** 房间枢纽 pill：任一占用/预约中=占用中，否则空闲 */
        roomPill() {
            if (!this.selfBookingOn) return { label: '未开通', type: 'gray' }
            if (!this.rooms.length) return { label: '暂无' + this.spaceLabel, type: 'gray' }
            const busy = this.rooms.some(r => r.resourceStatus === 'in_use' || r.resourceStatus === 'reserved')
            return busy ? { label: '占用中', type: 'warn-solid' } : { label: '空闲', type: 'primary-solid' }
        },
        /** 员工枢纽 pill：任一在岗/忙碌=在岗，否则休息中 */
        staffPill() {
            if (!this.selfBookingOn) return { label: '未开通', type: 'gray' }
            if (!this.staff.length) return { label: '暂无' + this.staffLabel, type: 'gray' }
            const onDuty = this.staff.some(u => u.resourceStatus === 'idle' || u.resourceStatus === 'working')
            return onDuty ? { label: '在岗', type: 'primary-soft' } : { label: '休息中', type: 'gray' }
        },
        /** 活动枢纽 pill：取最新一条活动状态 */
        actPill() {
            if (!this.activities.length) return { label: '暂无活动', type: 'gray' }
            return this.actStatus(this.activities[0])
        }
    },
    onShow() {
        if (!getToken()) {
            uni.reLaunch({ url: '/pages/login/index' })
            return
        }
        this.theme = getThemeSync()
        loadTenantTheme().then(t => { this.theme = t })
        // ★T15.36 版本档位判断（versionType 由登录响应缓存，兜底从已选租户取）
        let vt = getVersionType()
        if (!vt) {
            const sel = getSelectedTenant()
            vt = (sel && sel.versionType) || ''
        }
        this.isPro = vt === 'micro_pro'
        this.selfBookingOn = hasFeature('FEAT_SELF_BOOKING')
        this.activityLockOn = hasFeature('FEAT_ACTIVITY_LOCK')
        // ★BUG-03 行业术语异步加载：salesman→员工名（助教/导游/司机），_space_→空间名（房间/车辆/场地）
        //   回退名：员工/房间（无映射时展示通用名，不阻断页面渲染）
        getRoleDisplayName('salesman', '员工').then(name => { this.staffLabel = name || '员工' })
        getSpaceDisplayName('房间').then(name => { this.spaceLabel = name || '房间' })
        this.loadSummary()
        // 预约失败通知仅自助预约开通时才会产生，未开通不发请求（避免无权限/无意义调用）
        if (this.selfBookingOn) {
            this.loadFailedNotices()
        }
        this.loadPendingBookings()
        if (this.isPro) {
            this.loadCostAnomaly()
        }
    },
    methods: {
        statusText(s) { return (BOOKING_STATUS[s] || {}).label || s || '-' },
        statusColor(s) { return (BOOKING_STATUS[s] || {}).color || '#909399' },
        formatTime(t) {
            if (!t) return ''
            const d = new Date(typeof t === 'string' ? t.replace(/-/g, '/') : t)
            if (isNaN(d.getTime())) return String(t).substring(0, 16)
            const p = n => String(n).padStart(2, '0')
            return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`
        },
        /** 状态映射取值（房间/助教），未知状态兜底灰 pill */
        roomStatus(s) { return ROOM_STATUS[s] || { label: s || '空闲', type: 'gray' } },
        staffStatus(s) { return STAFF_STATUS[s] || { label: s || '在岗', type: 'gray' } },
        /** 活动状态文案：PUBLISHED 未过结束时间=报名中，过时/ENDED=已结束，DRAFT=已下架；★锁定优先展示已锁定 */
        actStatus(a) {
            if (a.status === 'DRAFT') return { label: '已下架', type: 'gray' }
            if (a.status === 'ENDED') return { label: '已结束', type: 'gray' }
            if (a.endTime && Date.now() > new Date(a.endTime).getTime()) return { label: '已结束', type: 'gray' }
            if (a.isLocked) return { label: '已锁定', type: 'warn-solid' }
            return { label: '报名中', type: 'success-soft' }
        },
        /** 主题色 pill/按钮内联样式（色板取租户主题下发，不写死） */
        pillStyle(type) {
            const primary = this.theme.primaryColor || '#409eff'
            if (type === 'primary-solid') return { background: primary, color: '#fff' }
            if (type === 'primary-soft') return { color: primary, background: primary + '1a' }
            return {}
        },
        /** 进入设置态（整屏切换，保持页面一屏不滚动） */
        openPanel(key) {
            this.panel = key
        },
        closePanel() {
            this.panel = ''
        },
        /** 枢纽卡汇总数据（pill 与设置列表共用） */
        loadSummary() {
            if (this.selfBookingOn) {
                this.loadRooms()
                this.loadStaff()
            }
            this.loadActivities()
        },
        async loadRooms() {
            try {
                const res = await get(config.api.microResourceRoomList)
                this.rooms = (res && res.data) || []
            } catch (e) { this.rooms = [] }
        },
        async loadStaff() {
            try {
                const res = await get(config.api.microResourceStaffList)
                this.staff = (res && res.data) || []
            } catch (e) { this.staff = [] }
        },
        /** 活动列表（/micro/activity/stats，取前 5 条供状态管理） */
        async loadActivities() {
            try {
                const res = await get(config.api.microActivityStats)
                this.activities = ((res && res.data) || []).slice(0, 5)
            } catch (e) { this.activities = [] }
        },
        /** 设置房间状态（idle/in_use） */
        async setRoom(r, targetStatus) {
            try {
                await put(config.api.microResourceSet + '?resourceType=room&resourceId=' + r.id + '&targetStatus=' + targetStatus)
                uni.showToast({ title: '设置成功', icon: 'success' })
                this.loadRooms()
            } catch (e) { /* 失败提示由请求层统一 toast */ }
        },
        /** 设置助教状态（idle/working/resting） */
        async setStaff(u, targetStatus) {
            try {
                await put(config.api.microResourceSet + '?resourceType=staff&resourceId=' + u.userId + '&targetStatus=' + targetStatus)
                uni.showToast({ title: '设置成功', icon: 'success' })
                this.loadStaff()
            } catch (e) { /* 失败提示由请求层统一 toast */ }
        },
        /** 活动发布/结束/下架（PUBLISHED/ENDED/DRAFT） */
        async setAct(a, status) {
            try {
                await put(config.api.microPromoStatus + '/' + a.id + '/' + status)
                uni.showToast({ title: '操作成功', icon: 'success' })
                this.loadActivities()
            } catch (e) { /* 失败提示由请求层统一 toast */ }
        },
        /** ★M28 T28.1/T28.5 活动锁定/解锁（解锁需二次确认：解锁后座位可调整） */
        setLock(a, locked) {
            const title = a.title || ''
            if (locked) {
                uni.showModal({
                    title: '锁定活动',
                    content: '锁定后顾客不可报名/选座，座位变只读。确定锁定「' + title + '」吗？',
                    success: async (res) => {
                        if (!res.confirm) return
                        try {
                            await put(config.api.microActivityLockBase + a.id + '/lock')
                            uni.showToast({ title: '锁定成功', icon: 'success' })
                            this.loadActivities()
                        } catch (e) { /* 失败提示由请求层统一 toast */ }
                    }
                })
            } else {
                // ★解锁二次确认弹窗（T28.5）——解锁后座位可调整
                uni.showModal({
                    title: '解锁活动',
                    content: '解锁后座位可调整，确定解锁吗？',
                    success: async (res) => {
                        if (!res.confirm) return
                        try {
                            await put(config.api.microActivityLockBase + a.id + '/unlock')
                            uni.showToast({ title: '解锁成功', icon: 'success' })
                            this.loadActivities()
                        } catch (e) { /* 失败提示由请求层统一 toast */ }
                    }
                })
            }
        },
        /** 预约失败通知：按 messageType=RESERVATION_FAILED 过滤 */
        async loadFailedNotices() {
            try {
                const res = await get(config.api.microNotifyList, { messageType: 'RESERVATION_FAILED', pageNum: 1, pageSize: 10 })
                this.failedNotices = (res && res.rows) || []
            } catch (e) {
                this.failedNotices = []
            }
        },
        /** 一键重新预约：带原预约 bizId 进入新建预约页回填原参数 */
        rebook(msg) {
            const bizId = msg.bizId || ''
            uni.navigateTo({ url: '/pages/micro/booking/add?rebookId=' + bizId })
        },
        /** 预约冲突：仅拉取 CONFLICT 状态订单（正常 PENDING_SERVICE 不应出现在冲突区） */
        async loadPendingBookings() {
            try {
                const res = await get(config.api.microBookingList, { orderStatus: 'CONFLICT', pageNum: 1, pageSize: 20 })
                this.pendingBookings = (res && res.rows) || []
            } catch (e) {
                this.pendingBookings = []
            }
        },
        /** 固定支出异常计数：取经营驾驶舱聚合的 costAnomalyCount */
        async loadCostAnomaly() {
            try {
                const res = await get(config.api.microBossDashboard)
                const dash = (res && res.data) || {}
                this.costAnomalyCount = dash.costAnomalyCount || 0
            } catch (e) {
                this.costAnomalyCount = 0
            }
        },
        goBookingAll() { uni.navigateTo({ url: '/pages/micro/booking/list' }) },
        goBookingDetail(id) { uni.navigateTo({ url: '/pages/micro/booking/detail?id=' + id }) },
        goCost() { uni.navigateTo({ url: '/pages/micro/cost/list' }) },
        /** 跳转历史活动全部列表页（支持名称/时间查询） */
        goHistoryAll() { uni.navigateTo({ url: '/pages/micro/activity/history' }) },
        /** ★活动详情：跳老板端操作详情页（含结束/下架/锁定按钮） */
        viewActivity(item) {
            uni.navigateTo({ url: '/pages/micro/boss/activityDetail?id=' + (item.id || item.bizId || '') })
        }
    }
}
</script>

<style scoped>
/* ★一整屏固定布局：页面不滚动，枢纽卡均分剩余高度（与 bossHome 一屏方案同口径） */
.page { height: 100vh; box-sizing: border-box; padding: 20rpx 24rpx 120rpx; overflow: hidden; display: flex; flex-direction: column; background: var(--page-bg, #f5f6f8); }
.hub-wrap { flex: 1; min-height: 0; display: flex; flex-direction: column; gap: 24rpx; }

/* ★枢纽卡（状态设置中心） */
.hub-card { position: relative; flex: 1; min-height: 0; background: var(--card-bg, #fff); border-radius: var(--radius-card, 24rpx); padding: 24rpx 32rpx; box-shadow: var(--shadow-card, 0 4rpx 20rpx rgba(0, 0, 0, 0.05)); overflow: hidden; display: flex; flex-direction: column; justify-content: center; }
.hub-row { display: flex; align-items: center; }
.hub-icon { width: 88rpx; height: 88rpx; border-radius: 20rpx; display: flex; align-items: center; justify-content: center; font-size: 44rpx; margin-right: 24rpx; flex-shrink: 0; }
.bg-blue { background: var(--brand-weak, #e8f1fb); }
.bg-warn { background: #fdf6ec; }
.hub-main { flex: 1; overflow: hidden; }
.hub-title { display: block; font-size: 34rpx; font-weight: bold; color: var(--text, #1a1a1a); margin-bottom: 10rpx; }
.hub-desc { display: block; font-size: 26rpx; color: var(--text-sub, #909399); margin-bottom: 14rpx; }
.hub-pill { display: inline-block; font-size: 24rpx; padding: 6rpx 24rpx; border-radius: 10rpx; }
.hub-pill.sm { font-size: 22rpx; padding: 4rpx 18rpx; }
/* pill 配色（warn/success/gray 固定语义色；primary 系由主题内联下发） */
.t-warn-solid { background: #e6a23c; color: #fff; }
.t-warn-soft { background: #fdf6ec; color: #e6a23c; }
.t-success-soft { background: #e8f8e0; color: #52b647; }
.t-gray { background: var(--brand-weak, #f0f2f5); color: var(--text-sub, #909399); }
.hub-btn { min-width: 160rpx; height: 72rpx; line-height: 72rpx; font-size: 28rpx; font-weight: 600; border-radius: 40rpx; border: none; margin: 0; flex-shrink: 0; padding: 0 32rpx; }
.hub-btn::after { border: none; }
.solid-primary { color: #fff; }
.soft-warn { background: #fdf6ec; color: #e6a23c; border: 1rpx solid #f5dab1; }
.solid-warn { background: #e6a23c; color: #fff; }
.soft-primary { border: 1rpx solid transparent; }
/* 失败通知计数徽章（右上角） */
.hub-badge { position: absolute; top: 0; right: 0; background: var(--brand, #8ba3c7); color: #fff; font-size: 22rpx; padding: 8rpx 24rpx; border-radius: 0 24rpx 0 20rpx; z-index: 1; }

/* ★设置态：整屏列表卡（仅列表区内部可滚动） */
.panel-view { flex: 1; min-height: 0; display: flex; flex-direction: column; }
.panel-card { flex: 1; min-height: 0; display: flex; flex-direction: column; background: var(--card-bg, #fff); border-radius: var(--radius-card, 24rpx); padding: 32rpx; box-shadow: var(--shadow-card, 0 4rpx 20rpx rgba(0, 0, 0, 0.05)); }
.panel-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; flex-shrink: 0; }
.panel-title { font-size: 34rpx; font-weight: bold; color: var(--text, #1a1a1a); }
.panel-back { font-size: 26rpx; }
.panel-scroll { flex: 1; min-height: 0; }
.p-row { display: flex; align-items: center; justify-content: space-between; padding: 20rpx 0; border-bottom: 1rpx solid var(--border, #f5f6f8); }
.p-row:last-child { border-bottom: none; }
.p-row.col { align-items: flex-end; }
.p-main { flex: 1; display: flex; align-items: center; overflow: hidden; }
.col-main { flex-direction: column; align-items: flex-start; }
.p-name { font-size: 28rpx; color: var(--text, #303133); font-weight: 600; margin-right: 16rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.col-main .p-name { margin-right: 0; margin-bottom: 6rpx; }
.p-sub { font-size: 24rpx; color: var(--text-sub, #606266); display: block; }
.p-sub.time { color: var(--text-sub, #909399); font-size: 22rpx; margin-top: 4rpx; }
.p-actions { display: flex; gap: 12rpx; flex-shrink: 0; margin-left: 16rpx; }
.row-arrow { font-size: 32rpx; color: var(--text-sub, #c0c4cc); flex-shrink: 0; margin-left: 8rpx; }
.btn-sm { font-size: 24rpx; height: 56rpx; line-height: 56rpx; padding: 0 24rpx; border-radius: 28rpx; border: none; margin: 0; }
.btn-sm::after { border: none; }
.btn-sm.gray { background: var(--brand-weak, #f0f2f5); color: var(--text-sub, #606266); }
.btn-sm.success { background: #67c23a; color: #fff; }
.panel-link { font-size: 24rpx; text-align: right; padding: 16rpx 0 4rpx; }

/* ★业务待办/异常卡（有数据才展示，不伸展挤压枢纽卡） */
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 24rpx 32rpx; box-shadow: var(--shadow-card, 0 2rpx 12rpx rgba(0, 0, 0, 0.04)); flex-shrink: 0; max-height: 40%; overflow: hidden; }
.card.warn { border-left: 8rpx solid #e6a23c; }
.card.upgrade { border-left: 8rpx solid var(--brand, #409eff); }
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12rpx; }
.card-head .card-title { margin-bottom: 0; }
.card-title { font-size: 30rpx; font-weight: bold; display: block; margin-bottom: 12rpx; color: var(--text, #303133); }
.link { font-size: 24rpx; color: var(--brand, #409eff); }
.desc { font-size: 26rpx; color: var(--text-sub, #909399); }
.row { display: flex; justify-content: space-between; align-items: center; padding: 12rpx 0; border-bottom: 1rpx solid var(--border, #f0f2f5); }
.row:last-child { border-bottom: none; }
.row-main { flex: 1; overflow: hidden; }
.row-title { font-size: 28rpx; font-weight: bold; color: var(--text, #303133); display: block; }
.row-sub { font-size: 24rpx; color: var(--text-sub, #606266); margin-top: 6rpx; display: block; }
.row-action { text-align: right; flex-shrink: 0; margin-left: 16rpx; }
.status { font-size: 24rpx; display: block; margin-bottom: 8rpx; }
.anomaly { display: flex; align-items: baseline; }
.anomaly-num { font-size: 48rpx; font-weight: bold; margin-right: 16rpx; }
.anomaly-label { font-size: 26rpx; color: var(--text-sub, #606266); }
.empty { text-align: center; color: var(--text-sub, #909399); font-size: 26rpx; padding: 40rpx 0; }
</style>
