<template>
    <!-- ★M36 T36.4 员工端结算单只读页面（§8.5）
         与老板端共用数据结构，权限收窄：
         · 收入项：可见不可编辑
         · 成本项：仅可见自己那条员工成本行（后端已过滤）
         · 合计利润区：不可见（后端已置空汇总字段）
         · 无保存 / 提交 / 导出操作 -->
    <view class="page" :class="'theme-' + theme.key" :style="rootStyle">
        <scroll-view class="scroll" scroll-y>
            <view v-if="!loaded" class="empty">加载中…</view>
            <block v-else>
                <!-- 活动选择器 -->
                <view class="card">
                    <text class="card-title">选择活动</text>
                    <view v-if="!activities.length" class="empty-sm">暂无参与的活动</view>
                    <view v-for="a in activities" :key="a.bizId" class="act-item"
                          :class="{ active: String(a.bizId) === String(activityId) }"
                          :style="String(a.bizId) === String(activityId) ? { borderColor: theme.primaryColor } : {}"
                          @click="selectActivity(a.bizId)">
                        <view class="act-info">
                            <text class="act-title">{{ a.title || '活动' }}</text>
                            <text class="act-time">{{ fmtTime(a.startTime) }}</text>
                        </view>
                        <text v-if="String(a.bizId) === String(activityId)" class="act-check" :style="{ color: theme.primaryColor }">✓</text>
                    </view>
                </view>

                <block v-if="sd.id">
                    <!-- ① 活动信息（只读） -->
                    <view class="card head-card">
                        <view class="head-top">
                            <text class="title">{{ sd.activityTitle || '活动结算' }}</text>
                            <text class="status-tag" :class="'st-' + (sd.status || 'DRAFT').toLowerCase()">{{ statusText(sd.status) }}</text>
                        </view>
                        <text class="sub">{{ timeText }}</text>
                        <text class="sub">结算单号：{{ sd.settlementNo || '（未提交）' }}</text>
                    </view>

                    <!-- ② 收入项（可见不可编辑） -->
                    <view class="card">
                        <text class="card-title">收入项</text>
                        <view v-for="(it, i) in incomeItems" :key="'in' + i" class="ro-row">
                            <view class="ro-left">
                                <text class="ro-name">{{ it.itemName }}</text>
                                <text class="type-tag" :class="'tp-' + (it.itemType || 'TICKET').toLowerCase()">{{ incomeTypeText(it.itemType) }}</text>
                            </view>
                            <text class="ro-amount">¥{{ fmtMoney(it.amount) }}</text>
                        </view>
                        <view v-if="!incomeItems.length" class="empty-sm">暂无收入项</view>
                    </view>

                    <!-- ③ 我的成本（仅自己那条员工成本行） -->
                    <view class="card">
                        <text class="card-title">我的分成成本</text>
                        <view v-for="(c, i) in myCosts" :key="'mc' + i" class="ro-row">
                            <view class="ro-left">
                                <text class="ro-name">{{ c.itemName }}</text>
                                <text v-if="c.staffName" class="staff-tag">{{ c.staffName }}</text>
                            </view>
                            <text class="ro-amount cost">¥{{ fmtMoney(c.amount) }}</text>
                        </view>
                        <view v-if="!myCosts.length" class="empty-sm">本活动暂无你的分成成本记录</view>
                        <text class="privacy-note">出于隐私，员工仅可见自己的分成成本；收入合计与利润为门店经营数据，不对员工展示。</text>
                    </view>
                </block>

                <view v-else-if="loaded" class="empty">该活动尚未生成结算单</view>
            </block>
        </scroll-view>
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get } from '../../../utils/request'
import config from '../../../config'
import { applyNavBarTitle } from '../../../utils/roles'
import { getThemeSync, loadTenantTheme, themeCssVars } from '../../../utils/theme'

const INCOME_TYPES = [
    { value: 'TICKET', label: '票面收入' },
    { value: 'ADDON', label: '追加消费' },
    { value: 'COLLECTED', label: '代收金额' }
]

export default {
    data() {
        return {
            theme: getThemeSync(),
            loaded: false,
            activityId: '',
            activities: [],
            sd: {},
            incomeItems: [],
            myCosts: []
        }
    },
    computed: {
        rootStyle() { return themeCssVars(this.theme.key) },
        timeText() {
            const s = this.sd.activityStartTime ? String(this.sd.activityStartTime).replace('T', ' ').substring(0, 16) : ''
            const e = this.sd.activityEndTime ? String(this.sd.activityEndTime).replace('T', ' ').substring(0, 16) : ''
            return s && e ? s + ' ~ ' + e : (s || e || '')
        }
    },
    onLoad(options) {
        this.activityId = options.activityId || options.id || ''
    },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        applyNavBarTitle()
        this.theme = getThemeSync()
        loadTenantTheme().then(t => { this.theme = t })
        this.loadActivities()
    },
    methods: {
        fmtTime(t) { return t ? String(t).replace('T', ' ').substring(0, 16) : '' },
        fmtMoney(v) { const n = Number(v) || 0; return n.toFixed(2) },
        statusText(s) { return { DRAFT: '草稿', SUBMITTED: '已提交', LOCKED: '已锁定' }[s] || '草稿' },
        incomeTypeText(t) { const f = INCOME_TYPES.find(x => x.value === t); return f ? f.label : '票面收入' },

        async loadActivities() {
            try {
                const r = await get(config.api.microStaffMyActivities)
                const list = (r && r.data) || []
                this.activities = list.map(x => Object.assign({}, x, {
                    bizId: x.bizId || x.id || x.activityId, title: x.title || x.activityTitle
                })).filter(x => x.bizId)
                if (!this.activityId && this.activities.length) this.activityId = this.activities[0].bizId
            } catch (e) { this.activities = [] }
            if (this.activityId) this.loadSettlement()
            else this.loaded = true
        },
        selectActivity(id) { this.activityId = id; this.loadSettlement() },

        /** 加载员工只读结算视图 */
        async loadSettlement() {
            if (!this.activityId) return
            this.loaded = false
            try {
                const r = await get(config.api.microSettlementStaffView + this.activityId)
                const vo = (r && r.data) || {}
                this.sd = vo
                this.incomeItems = vo.incomeItems || []
                this.myCosts = vo.costItems || []
            } catch (e) {
                this.sd = {}; this.incomeItems = []; this.myCosts = []
            } finally { this.loaded = true }
        }
    }
}
</script>

<style scoped>
.page { position: fixed; inset: 0; background: var(--page-bg, #f5f6f8); display: flex; flex-direction: column; }
.scroll { flex: 1; min-height: 0; padding: 24rpx; box-sizing: border-box; }
.empty { text-align: center; color: var(--text-sub, #909399); font-size: 26rpx; padding: 80rpx 0; }
.empty-sm { text-align: center; color: #c0c4cc; font-size: 24rpx; padding: 24rpx 0; }
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 20rpx); padding: 28rpx 32rpx; margin-bottom: 20rpx; box-shadow: var(--shadow-card, 0 2rpx 12rpx rgba(0,0,0,.04)); }
.card-title { font-size: 30rpx; font-weight: bold; color: var(--text, #303133); display: block; margin-bottom: 16rpx; }

.act-item { display: flex; align-items: center; padding: 18rpx; border: 2rpx solid var(--border, #f0f2f5); border-radius: 16rpx; margin-bottom: 12rpx; }
.act-item.active { background: var(--brand-weak, #ecf5ff); }
.act-info { flex: 1; overflow: hidden; }
.act-title { display: block; font-size: 28rpx; color: var(--text, #1a1a1a); font-weight: 600; }
.act-time { display: block; font-size: 22rpx; color: var(--text-sub, #909399); margin-top: 6rpx; }
.act-check { font-size: 34rpx; font-weight: bold; }

.head-card { padding: 24rpx 32rpx; }
.head-top { display: flex; align-items: center; justify-content: space-between; }
.title { font-size: 32rpx; font-weight: bold; color: var(--text, #1a1a1a); }
.sub { display: block; font-size: 23rpx; color: var(--text-sub, #909399); margin-top: 8rpx; }
.status-tag { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 20rpx; color: #fff; }
.st-draft { background: #909399; }
.st-submitted { background: #4F8FF7; }
.st-locked { background: #67c23a; }

.ro-row { display: flex; align-items: center; justify-content: space-between; padding: 16rpx 0; border-bottom: 1rpx solid var(--border, #f5f6f8); }
.ro-row:last-child { border-bottom: none; }
.ro-left { display: flex; align-items: center; gap: 12rpx; flex: 1; overflow: hidden; }
.ro-name { font-size: 27rpx; color: var(--text, #303133); }
.ro-amount { font-size: 27rpx; font-weight: 600; color: var(--text, #1a1a1a); }
.ro-amount.cost { color: #f56c6c; }
.type-tag { font-size: 20rpx; padding: 3rpx 12rpx; border-radius: 16rpx; color: #fff; flex-shrink: 0; }
.tp-ticket { background: #4F8FF7; }
.tp-addon { background: #FFB347; }
.tp-collected { background: #909399; }
.staff-tag { font-size: 20rpx; color: var(--brand, #4F8FF7); background: var(--brand-weak, #ecf5ff); padding: 3rpx 12rpx; border-radius: 16rpx; }
.privacy-note { display: block; font-size: 21rpx; color: var(--text-sub, #909399); line-height: 1.6; margin-top: 16rpx; padding-top: 16rpx; border-top: 1rpx dashed var(--border, #e4e7ed); }
</style>
