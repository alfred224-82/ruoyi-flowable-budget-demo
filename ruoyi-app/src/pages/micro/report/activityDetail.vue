<template>
    <view class="page" :class="'theme-' + theme.key" :style="rootStyle">
        <!-- 顶部三页签 -->
        <view class="tabbar">
            <view v-for="t in tabs" :key="t.key" class="tab" :class="{ active: t.key === 'detail' }" @click="switchReport(t.key)">
                <text>{{ t.label }}</text>
            </view>
        </view>

        <!-- 共用筛选条 -->
        <view class="card filter">
            <view class="f-row">
                <text class="f-label">日期</text>
                <picker mode="date" :value="query.beginDate" @change="e => onDate('beginDate', e)">
                    <view class="f-picker">{{ query.beginDate || '开始' }}</view>
                </picker>
                <text class="f-sep">至</text>
                <picker mode="date" :value="query.endDate" @change="e => onDate('endDate', e)">
                    <view class="f-picker">{{ query.endDate || '结束' }}</view>
                </picker>
            </view>
            <view class="f-row">
                <picker :range="settleOpts" range-key="label" @change="e => onPick('settleStatus', settleOpts[e.detail.value].value)">
                    <view class="f-chip">结算：{{ settleLabel }} ▾</view>
                </picker>
                <picker :range="actOpts" range-key="label" @change="e => onPick('activityStatus', actOpts[e.detail.value].value)">
                    <view class="f-chip">活动：{{ actLabel }} ▾</view>
                </picker>
                <picker :range="staffOpts" range-key="label" @change="e => onStaff(e)">
                    <view class="f-chip">员工：{{ staffLabel }} ▾</view>
                </picker>
            </view>
            <view class="f-row">
                <input class="f-input" v-model="query.title" placeholder="活动名称搜索" confirm-type="search" @confirm="loadData" />
                <view class="f-btn" :style="{ background: theme.brand }" @click="loadData">查询</view>
                <view class="f-btn ghost" @click="resetFilter">重置</view>
            </view>
        </view>

        <!-- 未结算提醒 -->
        <view v-if="report.unsettledCount > 0" class="notice">
            <text>⚠️ 有 {{ report.unsettledCount }} 场活动尚未结算，未计入合计。请先在活动结算单完成结算。</text>
        </view>

        <!-- 明细列表 -->
        <view v-if="!loaded" class="loading">加载中…</view>
        <view v-else-if="!report.rows || !report.rows.length" class="empty">该筛选条件下暂无活动</view>
        <view v-else class="list">
            <view v-for="(row, idx) in report.rows" :key="idx" class="card row-card" @click="drillDown(row)">
                <view class="r-head">
                    <text class="r-title">{{ row.activityTitle || '未命名活动' }}</text>
                    <text class="r-status" :class="row.settled ? 'ok' : 'warn'">{{ statusText(row) }}</text>
                </view>
                <view class="r-sub">
                    <text>{{ formatDate(row.endTime) }}</text>
                    <text v-if="row.staffNames"> · {{ row.staffNames }}</text>
                    <text v-if="row.participantCount"> · {{ row.participantCount }}人</text>
                </view>
                <view class="r-nums">
                    <view class="r-cell"><text class="c-label">实收</text><text class="c-val">¥{{ formatAmount(row.totalRevenue) }}</text></view>
                    <view class="r-cell"><text class="c-label">成本</text><text class="c-val">¥{{ formatAmount(row.totalCost) }}</text></view>
                    <view class="r-cell"><text class="c-label">毛利</text><text class="c-val strong">¥{{ formatAmount(row.profit) }}</text></view>
                    <view class="r-cell"><text class="c-label">差额</text>
                        <text class="c-val" :class="diffClass(row.profitDiff)">{{ diffText(row.profitDiff) }}</text>
                    </view>
                </view>
                <view v-if="row.collectedAmount > 0" class="r-collect">代收 ¥{{ formatAmount(row.collectedAmount) }}（不计入收入/利润）</view>
            </view>
        </view>

        <!-- 底部固定合计行 -->
        <view v-if="report.totalRow" class="total-bar">
            <view class="t-title">{{ report.totalRow.activityTitle }}</view>
            <view class="t-nums">
                <text>实收 ¥{{ formatAmount(report.totalRow.totalRevenue) }}</text>
                <text>成本 ¥{{ formatAmount(report.totalRow.totalCost) }}</text>
                <text class="strong">毛利 ¥{{ formatAmount(report.totalRow.profit) }}</text>
                <text :class="diffClass(report.totalRow.profitDiff)">差额 {{ diffText(report.totalRow.profitDiff) }}</text>
            </view>
            <view class="t-actions">
                <view class="f-btn" :style="{ background: theme.brand }" @click="onExport">导出 CSV</view>
            </view>
        </view>
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get } from '../../../utils/request'
import config from '../../../config'
import { formatAmount, formatDate, currentPeriod } from '../../../utils/format'
import { diffClass, diffText, settleStatusText, exportCsv, toCsv } from '../../../utils/reportFormat'
import { getThemeSync, loadTenantTheme, themeCssVars } from '../../../utils/theme'
import { applyNavBarTitle } from '../../../utils/roles'

export default {
    data() {
        return {
            theme: getThemeSync(),
            loaded: false,
            tabs: [
                { key: 'detail', label: '活动明细' },
                { key: 'daily', label: '日报' },
                { key: 'monthly', label: '月报' }
            ],
            query: { beginDate: '', endDate: '', activityStatus: '', settleStatus: '', staffId: '', title: '' },
            report: { rows: [], totalRow: null, unsettledCount: 0 },
            settleOpts: [
                { label: '全部', value: '' },
                { label: '已结算', value: 'SUBMITTED' },
                { label: '已锁定', value: 'LOCKED' },
                { label: '未结算', value: 'DRAFT' }
            ],
            actOpts: [
                { label: '全部', value: '' },
                { label: '已发布', value: 'PUBLISHED' },
                { label: '已结束', value: 'ENDED' },
                { label: '草稿', value: 'DRAFT' }
            ],
            staffOpts: [{ label: '全部', value: '' }]
        }
    },
    computed: {
        rootStyle() { return themeCssVars(this.theme.key) },
        settleLabel() { return (this.settleOpts.find(o => o.value === this.query.settleStatus) || {}).label || '全部' },
        actLabel() { return (this.actOpts.find(o => o.value === this.query.activityStatus) || {}).label || '全部' },
        staffLabel() {
            const o = this.staffOpts.find(x => String(x.value) === String(this.query.staffId))
            return o ? o.label : '全部'
        }
    },
    onLoad(options) {
        const month = (options && options.month) || currentPeriod()
        this.query.beginDate = month + '-01'
        this.query.endDate = this.monthEnd(month)
    },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        applyNavBarTitle()
        this.theme = getThemeSync()
        loadTenantTheme().then(t => { this.theme = t })
        this.loadStaffOptions()
        this.loadData()
    },
    methods: {
        formatAmount, formatDate, diffClass, diffText,
        statusText(row) { return settleStatusText(row.settlementStatus, row.settled) },
        monthEnd(month) {
            const parts = month.split('-')
            const y = Number(parts[0]), m = Number(parts[1])
            const last = new Date(y, m, 0).getDate()
            return month + '-' + String(last).padStart(2, '0')
        },
        switchReport(tab) {
            const map = { detail: '/pages/micro/report/activityDetail', daily: '/pages/micro/report/daily', monthly: '/pages/micro/report/monthly' }
            if (tab === 'detail') return
            uni.redirectTo({ url: map[tab] })
        },
        onDate(field, e) { this.query[field] = e.detail.value; this.loadData() },
        onPick(field, value) { this.query[field] = value; this.loadData() },
        onStaff(e) { this.query.staffId = this.staffOpts[e.detail.value].value; this.loadData() },
        resetFilter() {
            const month = (this.query.beginDate || currentPeriod()).substring(0, 7)
            this.query = { beginDate: month + '-01', endDate: this.monthEnd(month), activityStatus: '', settleStatus: '', staffId: '', title: '' }
            this.loadData()
        },
        async loadStaffOptions() {
            try {
                const res = await get(config.api.microComplaintStaffOptions)
                const list = (res && res.data) || []
                const opts = [{ label: '全部', value: '' }]
                list.forEach(s => {
                    const id = s.userId != null ? s.userId : (s.value != null ? s.value : s.id)
                    const name = s.nickName || s.label || s.name || ('员工' + id)
                    if (id != null) opts.push({ label: name, value: id })
                })
                this.staffOpts = opts
            } catch (e) { /* 员工筛选降级为不可用，不阻塞报表 */ }
        },
        async loadData() {
            this.loaded = false
            try {
                const params = {}
                Object.keys(this.query).forEach(k => { if (this.query[k] !== '' && this.query[k] != null) params[k] = this.query[k] })
                const res = await get(config.api.microOperatingActivityDetail, params)
                this.report = (res && res.data) || { rows: [], totalRow: null, unsettledCount: 0 }
            } catch (e) {
                this.report = { rows: [], totalRow: null, unsettledCount: 0 }
            } finally { this.loaded = true }
        },
        drillDown(row) {
            if (!row.activityId) return
            uni.navigateTo({ url: '/pages/micro/report/settlementDetail?activityId=' + row.activityId })
        },
        onExport() {
            const rows = [['活动名称', '本场员工', '参与人数', '实收', '代收', '成本', '毛利', '预估毛利', '差额', '结算状态', '结束时间']]
            ;(this.report.rows || []).forEach(r => {
                rows.push([r.activityTitle, r.staffNames, r.participantCount, r.totalRevenue, r.collectedAmount,
                    r.totalCost, r.profit, r.estimatedProfit, r.profitDiff, this.statusText(r), formatDate(r.endTime)])
            })
            const t = this.report.totalRow
            if (t) rows.push([t.activityTitle, '', t.participantCount, t.totalRevenue, t.collectedAmount, t.totalCost, t.profit, t.estimatedProfit, t.profitDiff, '', ''])
            exportCsv('活动明细表_' + (this.query.beginDate || '') + '_' + (this.query.endDate || '') + '.csv', toCsv(rows))
        }
    }
}
</script>

<style scoped>
.page { min-height: 100vh; box-sizing: border-box; padding: 20rpx 24rpx 200rpx; background: var(--page-bg, #f5f6f8); }
.tabbar { display: flex; background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 8rpx; margin-bottom: 20rpx; box-shadow: var(--shadow-card, 0 2rpx 12rpx rgba(0,0,0,.04)); }
.tab { flex: 1; text-align: center; padding: 16rpx 0; font-size: 28rpx; color: var(--text-sub, #606266); border-radius: 12rpx; }
.tab.active { background: var(--brand, #409eff); color: #fff; font-weight: bold; }
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 24rpx; margin-bottom: 20rpx; box-shadow: var(--shadow-card, 0 2rpx 12rpx rgba(0,0,0,.04)); }
.filter .f-row { display: flex; align-items: center; flex-wrap: wrap; margin-bottom: 14rpx; }
.filter .f-row:last-child { margin-bottom: 0; }
.f-label { font-size: 26rpx; color: var(--text-sub, #909399); margin-right: 12rpx; }
.f-picker { font-size: 26rpx; color: var(--brand, #409eff); padding: 6rpx 16rpx; background: var(--brand-weak, #f0f2f5); border-radius: 10rpx; }
.f-sep { font-size: 24rpx; color: var(--text-sub, #909399); margin: 0 12rpx; }
.f-chip { font-size: 24rpx; color: var(--text, #303133); padding: 8rpx 18rpx; background: var(--brand-weak, #f0f2f5); border-radius: 20rpx; margin-right: 14rpx; margin-bottom: 6rpx; }
.f-input { flex: 1; font-size: 26rpx; padding: 10rpx 18rpx; background: var(--page-bg, #f5f6f8); border-radius: 12rpx; color: var(--text, #303133); }
.f-btn { font-size: 26rpx; color: #fff; padding: 12rpx 28rpx; border-radius: 24rpx; margin-left: 14rpx; }
.f-btn.ghost { background: var(--brand-weak, #f0f2f5); color: var(--brand, #409eff); }
.notice { background: rgba(230,162,60,.12); border-left: 6rpx solid #e6a23c; border-radius: 10rpx; padding: 16rpx 20rpx; margin-bottom: 20rpx; }
.notice text { font-size: 24rpx; color: #b88230; line-height: 1.5; }
.row-card { padding: 22rpx 24rpx; }
.r-head { display: flex; align-items: center; justify-content: space-between; }
.r-title { flex: 1; font-size: 30rpx; font-weight: bold; color: var(--text, #303133); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.r-status { font-size: 22rpx; padding: 4rpx 14rpx; border-radius: 8rpx; margin-left: 12rpx; }
.r-status.ok { background: rgba(103,194,58,.14); color: #67c23a; }
.r-status.warn { background: rgba(230,162,60,.14); color: #e6a23c; }
.r-sub { font-size: 22rpx; color: var(--text-sub, #909399); margin-top: 8rpx; }
.r-nums { display: flex; margin-top: 16rpx; }
.r-cell { flex: 1; }
.c-label { display: block; font-size: 22rpx; color: var(--text-sub, #909399); }
.c-val { display: block; font-size: 26rpx; color: var(--text, #303133); margin-top: 4rpx; }
.c-val.strong { font-weight: bold; font-size: 28rpx; }
.r-collect { font-size: 22rpx; color: var(--text-sub, #909399); margin-top: 12rpx; }
.diff-neg { color: #f56c6c; }
.diff-zero { color: #909399; }
.diff-pos { color: #67c23a; }
.total-bar { position: fixed; left: 0; right: 0; bottom: 0; background: var(--card-bg, #fff); border-top: 1rpx solid var(--border, #ebeef5); padding: 16rpx 24rpx calc(16rpx + env(safe-area-inset-bottom)); box-shadow: 0 -2rpx 12rpx rgba(0,0,0,.06); z-index: 10; }
.t-title { font-size: 26rpx; font-weight: bold; color: var(--text, #303133); }
.t-nums { display: flex; flex-wrap: wrap; margin-top: 8rpx; }
.t-nums text { font-size: 24rpx; color: var(--text, #303133); margin-right: 24rpx; }
.t-nums .strong { font-weight: bold; }
.t-actions { display: flex; justify-content: flex-end; margin-top: 10rpx; }
.loading, .empty { text-align: center; color: var(--text-sub, #909399); font-size: 26rpx; padding: 100rpx 0; }
</style>
