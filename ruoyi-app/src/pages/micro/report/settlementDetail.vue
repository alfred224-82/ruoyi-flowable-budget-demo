<template>
    <!-- ★M39 T39.4 报表下钻终点：只读单场结算单（复用 GET /micro/settlement/activity/{activityId}）
         三区：① 活动信息（只读）② 收入项 / 成本项列表（只读）③ 合计与利润 + 差额着色
         约束：代收(COLLECTED)不计入收入/利润，单独标注；只读不可编辑（编辑属 M36 结算页范畴） -->
    <view class="page" :class="'theme-' + theme.key" :style="rootStyle">
        <view v-if="!loaded" class="loading">加载中…</view>
        <block v-else-if="sd.id">
            <!-- ① 活动信息（只读） -->
            <view class="card head-card">
                <view class="head-top">
                    <text class="title">{{ sd.activityTitle || '活动结算单' }}</text>
                    <text class="status-tag" :class="'st-' + (sd.status || 'DRAFT').toLowerCase()">{{ statusText(sd.status) }}</text>
                </view>
                <text class="sub">{{ timeText }}</text>
                <text class="sub">结算单号：{{ sd.settlementNo || '（草稿未生成）' }} · 参与人数：{{ sd.participantCount || 0 }}</text>
                <text v-if="sd.isAdjustment" class="sub adjust-note">📌 本单为调整单（原单已锁定）</text>
            </view>

            <!-- ② 收入项（只读） -->
            <view class="card">
                <text class="card-title">收入项</text>
                <view v-for="(it, i) in incomeItems" :key="'in' + i" class="item-row">
                    <view class="item-line1">
                        <text class="item-name">{{ it.itemName }}<text v-if="it.isDefaultTicket" class="default-tag">默认票面</text></text>
                        <text class="type-tag" :class="'tp-' + (it.itemType || 'TICKET').toLowerCase()">{{ incomeTypeText(it.itemType) }}</text>
                    </view>
                    <view class="item-line2">
                        <text v-if="it.quantity" class="qty">数量 {{ it.quantity }}<text v-if="it.unitPrice"> · 单价 ¥{{ fmtMoney(it.unitPrice) }}</text></text>
                        <text class="amount">¥{{ fmtMoney(it.amount) }}</text>
                    </view>
                    <text v-if="it.itemType === 'COLLECTED'" class="collected-hint">代收金额不计入收入 / 利润 / 分账基数（仅记现金流）</text>
                </view>
                <view v-if="!incomeItems.length" class="empty-sm">暂无收入项</view>
            </view>

            <!-- ② 成本项（只读） -->
            <view class="card">
                <text class="card-title">成本项</text>
                <view v-if="employeeCosts.length" class="cost-group">
                    <text class="group-title">员工成本（快照自动生成）</text>
                    <view v-for="(c, i) in employeeCosts" :key="'ec' + i" class="cost-row">
                        <text class="cost-name">{{ c.itemName }}<text v-if="c.staffName" class="staff-name">· {{ c.staffName }}</text></text>
                        <text class="cost-amount">¥{{ fmtMoney(c.amount) }}</text>
                    </view>
                </view>
                <view v-for="(c, i) in otherCosts" :key="'oc' + i" class="cost-row">
                    <text class="cost-name">{{ c.itemName }}</text>
                    <text class="cost-amount">¥{{ fmtMoney(c.amount) }}</text>
                </view>
                <view v-if="!employeeCosts.length && !otherCosts.length" class="empty-sm">暂无成本项</view>
            </view>

            <!-- ③ 合计与利润 -->
            <view class="card summary-card">
                <text class="card-title">合计与利润</text>
                <view class="sum-grid">
                    <view class="sum-cell"><text class="sum-k">票面收入</text><text class="sum-v">¥{{ fmtMoney(sum.ticket) }}</text></view>
                    <view class="sum-cell"><text class="sum-k">追加收入</text><text class="sum-v">¥{{ fmtMoney(sum.addon) }}</text></view>
                    <view class="sum-cell"><text class="sum-k">代收金额</text><text class="sum-v muted">¥{{ fmtMoney(sum.collected) }}</text></view>
                    <view class="sum-cell"><text class="sum-k">收入合计</text><text class="sum-v strong">¥{{ fmtMoney(sum.totalRevenue) }}</text></view>
                    <view class="sum-cell"><text class="sum-k">员工成本</text><text class="sum-v cost">¥{{ fmtMoney(sum.employee) }}</text></view>
                    <view class="sum-cell"><text class="sum-k">其他成本</text><text class="sum-v cost">¥{{ fmtMoney(sum.other) }}</text></view>
                    <view class="sum-cell"><text class="sum-k">成本合计</text><text class="sum-v cost strong">¥{{ fmtMoney(sum.totalCost) }}</text></view>
                </view>
                <view class="profit-line">
                    <text class="profit-label">利润</text>
                    <text class="profit-value" :class="{ negative: sum.profit < 0 }" :style="sum.profit >= 0 ? { color: theme.brand } : {}">¥{{ fmtMoney(sum.profit) }}</text>
                </view>
                <view v-if="sd.estimatedProfit != null" class="diff-line">
                    <text class="diff-label">预估利润 ¥{{ fmtMoney(sd.estimatedProfit) }} · 差额</text>
                    <text class="diff-value" :class="diffClass(sum.profitDiff)">{{ diffText(sum.profitDiff) }}</text>
                </view>
                <text v-if="sd.settledByName || sd.settledTime" class="settle-meta">
                    结算人：{{ sd.settledByName || '—' }} · {{ fmtTime(sd.settledTime) }}
                </text>
                <text v-if="sd.remark" class="remark-text">备注：{{ sd.remark }}</text>
            </view>
        </block>
        <view v-else class="empty">未找到该活动的结算单</view>
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get } from '../../../utils/request'
import config from '../../../config'
import { applyNavBarTitle } from '../../../utils/roles'
import { getThemeSync, loadTenantTheme, themeCssVars } from '../../../utils/theme'
import { diffClass, diffText } from '../../../utils/reportFormat'

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
            sd: {},
            incomeItems: [],
            employeeCosts: [],
            otherCosts: []
        }
    },
    computed: {
        rootStyle() { return themeCssVars(this.theme.key) },
        timeText() {
            const s = this.sd.activityStartTime ? String(this.sd.activityStartTime).replace('T', ' ').substring(0, 16) : ''
            const e = this.sd.activityEndTime ? String(this.sd.activityEndTime).replace('T', ' ').substring(0, 16) : ''
            return s && e ? s + ' ~ ' + e : (s || e || '')
        },
        /** 合计与利润（镜像后端 recalculateSummary：代收不计入收入合计） */
        sum() {
            let ticket = 0, addon = 0, collected = 0
            this.incomeItems.forEach(it => {
                const amt = Number(it.amount) || 0
                if (it.itemType === 'TICKET') ticket += amt
                else if (it.itemType === 'ADDON') addon += amt
                else if (it.itemType === 'COLLECTED') collected += amt
            })
            let employee = 0, other = 0
            this.employeeCosts.forEach(c => { employee += Number(c.amount) || 0 })
            this.otherCosts.forEach(c => { other += Number(c.amount) || 0 })
            const totalRevenue = ticket + addon
            const totalCost = employee + other
            const profit = totalRevenue - totalCost
            const est = this.sd.estimatedProfit != null ? Number(this.sd.estimatedProfit) : null
            return {
                ticket, addon, collected, totalRevenue, employee, other, totalCost, profit,
                profitDiff: est != null ? profit - est : null
            }
        }
    },
    onLoad(options) {
        this.activityId = (options && (options.activityId || options.id)) || ''
    },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        applyNavBarTitle()
        this.theme = getThemeSync()
        loadTenantTheme().then(t => { this.theme = t })
        this.loadSettlement()
    },
    methods: {
        fmtTime(t) { return t ? String(t).replace('T', ' ').substring(0, 16) : '' },
        fmtMoney(v) { const n = Number(v) || 0; return n.toFixed(2) },
        statusText(s) { return { DRAFT: '草稿', SUBMITTED: '已提交', LOCKED: '已锁定' }[s] || '草稿' },
        incomeTypeText(t) { const f = INCOME_TYPES.find(x => x.value === t); return f ? f.label : '票面收入' },
        diffClass, diffText,
        async loadSettlement() {
            if (!this.activityId) { this.loaded = true; return }
            this.loaded = false
            try {
                const r = await get(config.api.microSettlementByActivity + this.activityId)
                this.applyVo((r && r.data) || {})
            } catch (e) {
                this.sd = {}
            } finally { this.loaded = true }
        },
        applyVo(vo) {
            this.sd = vo || {}
            this.incomeItems = vo.incomeItems || []
            const costs = vo.costItems || []
            this.employeeCosts = costs.filter(c => c.costType === 'EMPLOYEE')
            this.otherCosts = costs.filter(c => c.costType !== 'EMPLOYEE')
        }
    }
}
</script>

<style scoped>
.page { min-height: 100vh; box-sizing: border-box; padding: 24rpx 24rpx 60rpx; background: var(--page-bg, #f5f6f8); }
.loading, .empty { text-align: center; color: var(--text-sub, #909399); font-size: 26rpx; padding: 100rpx 0; }
.empty-sm { text-align: center; color: #c0c4cc; font-size: 24rpx; padding: 24rpx 0; }
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 20rpx); padding: 28rpx 32rpx; margin-bottom: 20rpx; box-shadow: var(--shadow-card, 0 2rpx 12rpx rgba(0,0,0,.04)); }
.card-title { font-size: 30rpx; font-weight: bold; color: var(--text, #303133); display: block; margin-bottom: 16rpx; }
.head-card { padding: 24rpx 32rpx; }
.head-top { display: flex; align-items: center; justify-content: space-between; }
.title { font-size: 32rpx; font-weight: bold; color: var(--text, #1a1a1a); }
.sub { display: block; font-size: 23rpx; color: var(--text-sub, #909399); margin-top: 8rpx; }
.adjust-note { color: #e6a23c; }
.status-tag { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 20rpx; color: #fff; }
.st-draft { background: #909399; }
.st-submitted { background: #4F8FF7; }
.st-locked { background: #67c23a; }
.item-row { padding: 18rpx 0; border-bottom: 1rpx solid var(--border, #f5f6f8); }
.item-row:last-child { border-bottom: none; }
.item-line1 { display: flex; align-items: center; justify-content: space-between; }
.item-name { font-size: 28rpx; color: var(--text, #1a1a1a); font-weight: 600; }
.default-tag { font-size: 20rpx; color: #4F8FF7; background: var(--brand-weak, #ecf5ff); padding: 2rpx 10rpx; border-radius: 8rpx; margin-left: 10rpx; }
.type-tag { font-size: 22rpx; padding: 6rpx 16rpx; border-radius: 20rpx; color: #fff; flex-shrink: 0; }
.tp-ticket { background: #4F8FF7; }
.tp-addon { background: #FFB347; }
.tp-collected { background: #909399; }
.item-line2 { display: flex; align-items: center; justify-content: space-between; margin-top: 10rpx; }
.qty { font-size: 22rpx; color: var(--text-sub, #909399); }
.amount { font-size: 28rpx; font-weight: 600; color: var(--text, #1a1a1a); }
.collected-hint { display: block; font-size: 21rpx; color: #e6a23c; margin-top: 10rpx; }
.cost-group { margin-bottom: 16rpx; }
.group-title { display: block; font-size: 22rpx; color: #e6a23c; margin-bottom: 10rpx; font-weight: 600; }
.cost-row { display: flex; align-items: center; justify-content: space-between; padding: 14rpx 0; border-bottom: 1rpx solid var(--border, #f5f6f8); }
.cost-name { flex: 1; font-size: 26rpx; color: var(--text, #303133); }
.staff-name { font-size: 22rpx; color: var(--text-sub, #909399); margin-left: 8rpx; }
.cost-amount { font-size: 26rpx; font-weight: 600; color: #f56c6c; }
.summary-card { background: linear-gradient(180deg, var(--card-bg, #fff), var(--brand-weak, #f7faff)); }
.sum-grid { display: flex; flex-wrap: wrap; }
.sum-cell { width: 50%; display: flex; justify-content: space-between; padding: 10rpx 0; box-sizing: border-box; }
.sum-cell:nth-child(odd) { padding-right: 20rpx; }
.sum-cell:nth-child(even) { padding-left: 20rpx; }
.sum-k { font-size: 24rpx; color: var(--text-sub, #606266); }
.sum-v { font-size: 24rpx; color: var(--text, #303133); }
.sum-v.muted { color: #909399; }
.sum-v.cost { color: #f56c6c; }
.sum-v.strong { font-weight: bold; }
.profit-line { display: flex; align-items: center; justify-content: space-between; margin-top: 16rpx; padding-top: 16rpx; border-top: 2rpx dashed var(--border, #e4e7ed); }
.profit-label { font-size: 28rpx; font-weight: bold; color: var(--text, #303133); }
.profit-value { font-size: 40rpx; font-weight: bold; }
.profit-value.negative { color: #f56c6c !important; }
.diff-line { display: flex; align-items: center; justify-content: space-between; margin-top: 8rpx; }
.diff-label { font-size: 22rpx; color: var(--text-sub, #909399); }
.diff-value { font-size: 24rpx; font-weight: 600; }
.diff-neg { color: #f56c6c; }
.diff-zero { color: #909399; }
.diff-pos { color: #67c23a; }
.settle-meta { display: block; font-size: 22rpx; color: var(--text-sub, #909399); margin-top: 14rpx; }
.remark-text { display: block; font-size: 22rpx; color: var(--text-sub, #606266); margin-top: 8rpx; line-height: 1.5; }
</style>
