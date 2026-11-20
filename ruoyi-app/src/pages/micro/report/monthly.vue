<template>
    <view class="page" :class="'theme-' + theme.key" :style="rootStyle">
        <!-- 顶部三页签 -->
        <view class="tabbar">
            <view v-for="t in tabs" :key="t.key" class="tab" :class="{ active: t.key === 'monthly' }" @click="switchReport(t.key)">
                <text>{{ t.label }}</text>
            </view>
        </view>

        <!-- 月份选择 + 锁定标记 -->
        <view class="card filter">
            <view class="f-row">
                <picker mode="date" fields="month" :value="month" @change="onMonthChange">
                    <view class="f-picker">{{ month }} ▾</view>
                </picker>
                <text v-if="locked" class="lock-badge">🔒 已月结锁定</text>
                <text v-else class="open-badge">未月结</text>
            </view>
        </view>

        <view v-if="!loaded" class="loading">加载中…</view>
        <template v-else>
            <!-- 未结算提醒 -->
            <view v-if="op.unsettledCount > 0" class="notice">
                <text>⚠️ 本月有 {{ op.unsettledCount }} 场活动尚未结算，未计入月毛利。建议先完成结算再月结锁定。</text>
            </view>

            <!-- 月经营汇总 -->
            <view class="card">
                <text class="card-title">月经营汇总</text>
                <view class="grid">
                    <view class="cell"><text class="c-label">月实收</text><text class="c-val income">¥{{ formatAmount(op.monthRevenue) }}</text></view>
                    <view class="cell"><text class="c-label">月变动成本</text><text class="c-val expense">¥{{ formatAmount(op.monthCost) }}</text></view>
                    <view class="cell"><text class="c-label">月毛利</text><text class="c-val strong">¥{{ formatAmount(op.monthGrossProfit) }}</text></view>
                    <view class="cell"><text class="c-label">已结算场次</text><text class="c-val">{{ op.activityCount || 0 }}</text></view>
                    <view class="cell"><text class="c-label">平均单场毛利</text><text class="c-val">¥{{ formatAmount(op.avgSessionProfit) }}</text></view>
                    <view class="cell"><text class="c-label">固定成本合计</text><text class="c-val expense">¥{{ formatAmount(op.fixedCostTotal) }}</text></view>
                </view>
                <!-- 成本构成 -->
                <view v-if="(op.fixedCostComposition || []).length" class="comp">
                    <view v-for="item in op.fixedCostComposition" :key="item.category" class="comp-row">
                        <text class="comp-name">{{ item.name }}</text>
                        <view class="comp-bar"><view class="comp-inner" :style="{ width: (item.percent || 0) + '%', background: theme.brand }"></view></view>
                        <text class="comp-amt">¥{{ formatAmount(item.amount) }}（{{ item.percent || 0 }}%）</text>
                    </view>
                </view>
            </view>

            <!-- 月净利润（含税费） -->
            <view class="card">
                <text class="card-title">月净利润</text>
                <view class="line"><text class="l-name">税前利润</text><text class="l-val">¥{{ formatAmount(op.pretaxProfit) }}</text></view>
                <!-- 税费录入 -->
                <view class="tax-box">
                    <view class="tax-mode">
                        <text class="t-radio" :class="{ on: taxForm.taxMode === 'MANUAL' }" @click="setTaxMode('MANUAL')">手填</text>
                        <text class="t-radio" :class="{ on: taxForm.taxMode === 'RATE' }" @click="setTaxMode('RATE')">按率估算</text>
                    </view>
                    <view v-if="taxForm.taxMode === 'MANUAL'" class="tax-input-row">
                        <text class="t-label">税费金额</text>
                        <input class="t-input" type="digit" v-model="taxForm.taxAmount" :disabled="locked" placeholder="0.00" />
                    </view>
                    <view v-else class="tax-input-row">
                        <text class="t-label">税率（%）</text>
                        <input class="t-input" type="digit" v-model="taxForm.taxRate" :disabled="locked" placeholder="如 3" />
                    </view>
                    <text class="tax-est">估算税费 ¥{{ formatAmount(op.taxAmount) }}</text>
                    <text class="tax-note">税费为估算，非申报依据；本页数据不作为税务申报依据。</text>
                </view>
                <view class="line total"><text class="l-name">月净利润</text>
                    <text class="l-val" :style="{ color: (op.netProfit || 0) >= 0 ? '#67c23a' : '#f56c6c' }">¥{{ formatAmount(op.netProfit) }}</text>
                </view>
                <view v-if="!locked" class="btn-row">
                    <view class="btn" :style="{ background: theme.brand }" @click="saveTax">保存税费</view>
                </view>
            </view>

            <!-- 保本线 -->
            <view class="card">
                <text class="card-title">月保本线</text>
                <view class="prog-line">
                    <text class="p-item">月毛利 <text class="strong">¥{{ formatAmount(op.monthGrossProfit) }}</text></text>
                    <text class="p-item">保本毛利 <text class="strong">¥{{ formatAmount(op.breakevenGrossProfit) }}</text></text>
                </view>
                <view class="prog-bar"><view class="prog-inner" :style="{ width: progressWidth(op.breakevenProgress) + '%', background: theme.brand }"></view></view>
                <view class="prog-foot">
                    <text class="prog-pct">{{ formatAmount(op.breakevenProgress) }}%</text>
                    <text class="prog-flag" :class="op.passedBreakeven ? 'safe' : 'unsafe'">{{ op.passedBreakeven ? '✓ 已过保本线' : '尚未保本' }}</text>
                </view>
                <text class="be-note">月保本场次约 {{ op.breakevenSessions || 0 }} 场（保本毛利 ÷ 平均单场毛利，向上取整）</text>
            </view>

            <!-- 对比上月 -->
            <view class="card">
                <text class="card-title">对比上月</text>
                <view class="line"><text class="l-name">上月毛利</text><text class="l-val">¥{{ formatAmount(op.prevMonthGrossProfit) }}</text></view>
                <view class="line"><text class="l-name">毛利环比</text><text class="l-val" :class="diffClass(monthGrossDelta)">{{ diffText(monthGrossDelta) }}</text></view>
                <view class="line"><text class="l-name">上月净利</text><text class="l-val">¥{{ formatAmount(op.prevNetProfit) }}</text></view>
                <view class="line"><text class="l-name">净利环比</text><text class="l-val" :class="diffClass(netDelta)">{{ diffText(netDelta) }}</text></view>
            </view>

            <!-- 对账校验（T39.5） -->
            <view class="card">
                <view class="card-head">
                    <text class="card-title">三张报表对账</text>
                    <text class="recon-flag" :class="reconcile.overallPassed ? 'safe' : 'unsafe'">{{ reconcile.overallPassed ? '✓ 全部一致' : '✗ 存在差异' }}</text>
                </view>
                <view v-for="(r, idx) in (reconcile.rules || [])" :key="idx" class="recon-row">
                    <text class="recon-name">{{ r.name }}</text>
                    <text class="recon-val" :class="r.passed ? 'safe' : 'unsafe'">{{ formatAmount(r.leftValue) }} / {{ formatAmount(r.rightValue) }} {{ r.passed ? '✓' : '✗' }}</text>
                </view>
            </view>

            <!-- 操作区 -->
            <view class="card actions">
                <view class="btn ghost" @click="exportMonthly">导出月报 CSV</view>
                <view v-if="!locked" class="btn danger" @click="lockMonth">月结锁定</view>
                <text v-else class="lock-tip">本月已月结锁定，数据只读。锁定人：{{ closing.lockByName || '—' }}</text>
            </view>
        </template>
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get, post } from '../../../utils/request'
import config from '../../../config'
import { formatAmount, currentPeriod } from '../../../utils/format'
import { diffClass, diffText, progressWidth, exportCsv, toCsv } from '../../../utils/reportFormat'
import { getThemeSync, loadTenantTheme, themeCssVars } from '../../../utils/theme'
import { applyNavBarTitle } from '../../../utils/roles'

export default {
    data() {
        return {
            theme: getThemeSync(),
            loaded: false,
            month: currentPeriod(),
            op: {},
            closing: {},
            reconcile: {},
            taxForm: { taxMode: 'MANUAL', taxAmount: '', taxRate: '' },
            tabs: [
                { key: 'detail', label: '活动明细' },
                { key: 'daily', label: '日报' },
                { key: 'monthly', label: '月报' }
            ]
        }
    },
    computed: {
        rootStyle() { return themeCssVars(this.theme.key) },
        locked() { return this.op.closingStatus === 'LOCKED' || this.closing.locked === true },
        monthGrossDelta() { return Number(this.op.monthGrossProfit || 0) - Number(this.op.prevMonthGrossProfit || 0) },
        netDelta() { return Number(this.op.netProfit || 0) - Number(this.op.prevNetProfit || 0) }
    },
    onLoad(options) {
        if (options && options.month) this.month = options.month
    },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        applyNavBarTitle()
        this.theme = getThemeSync()
        loadTenantTheme().then(t => { this.theme = t })
        this.loadData()
    },
    methods: {
        formatAmount, diffClass, diffText, progressWidth,
        switchReport(tab) {
            const map = { detail: '/pages/micro/report/activityDetail', daily: '/pages/micro/report/daily', monthly: '/pages/micro/report/monthly' }
            if (tab === 'monthly') return
            uni.redirectTo({ url: map[tab] })
        },
        onMonthChange(e) { this.month = e.detail.value; this.loadData() },
        setTaxMode(mode) { if (!this.locked) this.taxForm.taxMode = mode },
        async loadData() {
            this.loaded = false
            try {
                const [opRes, closingRes, reconRes] = await Promise.all([
                    get(config.api.microOperatingMonthly, { month: this.month }),
                    get(config.api.microOperatingClosing, { month: this.month }),
                    get(config.api.microOperatingReconcile, { month: this.month })
                ])
                this.op = (opRes && opRes.data) || {}
                this.closing = (closingRes && closingRes.data) || {}
                this.reconcile = (reconRes && reconRes.data) || {}
                this.taxForm.taxMode = this.closing.taxMode || 'MANUAL'
                this.taxForm.taxAmount = this.closing.taxAmount != null ? String(this.closing.taxAmount) : ''
                this.taxForm.taxRate = this.closing.taxRate != null ? String(this.closing.taxRate) : ''
            } catch (e) {
                this.op = {}; this.closing = {}; this.reconcile = {}
            } finally { this.loaded = true }
        },
        async saveTax() {
            if (this.locked) { uni.showToast({ title: '已月结锁定，不可修改', icon: 'none' }); return }
            const payload = { month: this.month, taxMode: this.taxForm.taxMode }
            if (this.taxForm.taxMode === 'MANUAL') payload.taxAmount = Number(this.taxForm.taxAmount || 0)
            else payload.taxRate = Number(this.taxForm.taxRate || 0)
            try {
                await post(config.api.microOperatingTax, payload)
                uni.showToast({ title: '税费已保存', icon: 'success' })
                this.loadData()
            } catch (e) { /* request 已提示 */ }
        },
        lockMonth() {
            uni.showModal({
                title: '月结锁定',
                content: '锁定后本月税费与报表将只读，不可再修改。确认锁定 ' + this.month + ' ？',
                success: async (r) => {
                    if (!r.confirm) return
                    try {
                        await post(config.api.microOperatingLock + this.month)
                        uni.showToast({ title: '已月结锁定', icon: 'success' })
                        this.loadData()
                    } catch (e) { /* request 已提示 */ }
                }
            })
        },
        exportMonthly() {
            const o = this.op
            const rows = [
                ['月度经营报表'],
                ['月份', this.month],
                [],
                ['月实收', o.monthRevenue], ['月变动成本', o.monthCost], ['月毛利', o.monthGrossProfit],
                ['已结算场次', o.activityCount], ['平均单场毛利', o.avgSessionProfit], ['未结算场次', o.unsettledCount],
                [],
                ['固定成本合计', o.fixedCostTotal], ['税前利润', o.pretaxProfit], ['税费', o.taxAmount], ['月净利润', o.netProfit],
                [],
                ['月保本毛利', o.breakevenGrossProfit], ['月保本场次', o.breakevenSessions], ['保本进度(%)', o.breakevenProgress],
                [],
                ['上月毛利', o.prevMonthGrossProfit], ['上月净利', o.prevNetProfit]
            ]
            exportCsv('月度经营报表_' + this.month + '.csv', toCsv(rows))
        }
    }
}
</script>

<style scoped>
.page { min-height: 100vh; box-sizing: border-box; padding: 20rpx 24rpx 60rpx; background: var(--page-bg, #f5f6f8); }
.tabbar { display: flex; background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 8rpx; margin-bottom: 20rpx; box-shadow: var(--shadow-card, 0 2rpx 12rpx rgba(0,0,0,.04)); }
.tab { flex: 1; text-align: center; padding: 16rpx 0; font-size: 28rpx; color: var(--text-sub, #606266); border-radius: 12rpx; }
.tab.active { background: var(--brand, #409eff); color: #fff; font-weight: bold; }
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 28rpx; margin-bottom: 20rpx; box-shadow: var(--shadow-card, 0 2rpx 12rpx rgba(0,0,0,.04)); }
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16rpx; }
.card-head .card-title { margin-bottom: 0; }
.card-title { display: block; font-size: 30rpx; font-weight: bold; color: var(--text, #303133); margin-bottom: 20rpx; }
.filter .f-row { display: flex; align-items: center; justify-content: space-between; }
.f-picker { font-size: 30rpx; font-weight: bold; color: var(--brand, #409eff); }
.lock-badge { font-size: 24rpx; color: #67c23a; background: rgba(103,194,58,.14); padding: 6rpx 16rpx; border-radius: 20rpx; }
.open-badge { font-size: 24rpx; color: #e6a23c; background: rgba(230,162,60,.14); padding: 6rpx 16rpx; border-radius: 20rpx; }
.notice { background: rgba(230,162,60,.12); border-left: 6rpx solid #e6a23c; border-radius: 10rpx; padding: 16rpx 20rpx; margin-bottom: 20rpx; }
.notice text { font-size: 24rpx; color: #b88230; line-height: 1.5; }
.grid { display: flex; flex-wrap: wrap; }
.cell { width: 50%; box-sizing: border-box; padding: 14rpx 8rpx; }
.c-label { display: block; font-size: 24rpx; color: var(--text-sub, #909399); }
.c-val { display: block; font-size: 34rpx; font-weight: bold; color: var(--text, #303133); margin-top: 8rpx; }
.c-val.income { color: #67c23a; }
.c-val.expense { color: #f56c6c; }
.c-val.strong { color: var(--brand, #409eff); }
.comp { margin-top: 16rpx; border-top: 1rpx solid var(--border, #f0f2f5); padding-top: 16rpx; }
.comp-row { display: flex; align-items: center; padding: 8rpx 0; }
.comp-name { width: 130rpx; font-size: 24rpx; color: var(--text-sub, #606266); flex-shrink: 0; }
.comp-bar { flex: 1; height: 14rpx; background: var(--brand-weak, #f0f2f5); border-radius: 7rpx; overflow: hidden; margin: 0 14rpx; }
.comp-inner { height: 100%; border-radius: 7rpx; }
.comp-amt { width: 210rpx; text-align: right; font-size: 22rpx; color: var(--text-sub, #909399); flex-shrink: 0; }
.line { display: flex; align-items: center; justify-content: space-between; padding: 12rpx 0; }
.line.total { border-top: 1rpx solid var(--border, #f0f2f5); margin-top: 8rpx; padding-top: 18rpx; }
.l-name { font-size: 26rpx; color: var(--text-sub, #606266); }
.l-val { font-size: 28rpx; font-weight: bold; color: var(--text, #303133); }
.tax-box { background: var(--page-bg, #f5f6f8); border-radius: 12rpx; padding: 20rpx; margin: 8rpx 0; }
.tax-mode { display: flex; margin-bottom: 14rpx; }
.t-radio { font-size: 24rpx; color: var(--text-sub, #909399); padding: 8rpx 24rpx; border-radius: 20rpx; background: var(--card-bg, #fff); margin-right: 14rpx; }
.t-radio.on { background: var(--brand, #409eff); color: #fff; }
.tax-input-row { display: flex; align-items: center; }
.t-label { font-size: 26rpx; color: var(--text, #303133); width: 160rpx; }
.t-input { flex: 1; font-size: 28rpx; padding: 10rpx 18rpx; background: var(--card-bg, #fff); border-radius: 10rpx; color: var(--text, #303133); }
.tax-est { display: block; font-size: 24rpx; color: var(--brand, #409eff); margin-top: 12rpx; }
.tax-note { display: block; font-size: 20rpx; color: var(--text-sub, #909399); margin-top: 8rpx; line-height: 1.5; }
.prog-line { display: flex; justify-content: space-between; margin-bottom: 16rpx; }
.p-item { font-size: 24rpx; color: var(--text-sub, #909399); }
.p-item .strong { color: var(--text, #303133); font-size: 28rpx; font-weight: bold; }
.prog-bar { height: 20rpx; background: var(--brand-weak, #f0f2f5); border-radius: 10rpx; overflow: hidden; }
.prog-inner { height: 100%; border-radius: 10rpx; }
.prog-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 14rpx; }
.prog-pct { font-size: 26rpx; font-weight: bold; color: var(--text, #303133); }
.prog-flag { font-size: 24rpx; }
.prog-flag.safe, .recon-flag.safe, .recon-val.safe { color: #67c23a; }
.prog-flag.unsafe, .recon-flag.unsafe, .recon-val.unsafe { color: #f56c6c; }
.be-note { display: block; font-size: 22rpx; color: var(--text-sub, #909399); margin-top: 12rpx; }
.recon-flag { font-size: 24rpx; font-weight: bold; }
.recon-row { display: flex; align-items: center; justify-content: space-between; padding: 10rpx 0; border-bottom: 1rpx solid var(--border, #f0f2f5); }
.recon-row:last-child { border-bottom: none; }
.recon-name { flex: 1; font-size: 24rpx; color: var(--text-sub, #606266); }
.recon-val { font-size: 24rpx; margin-left: 16rpx; }
.diff-neg { color: #f56c6c; }
.diff-zero { color: #909399; }
.diff-pos { color: #67c23a; }
.actions { display: flex; flex-direction: column; align-items: stretch; }
.btn-row { display: flex; justify-content: flex-end; margin-top: 12rpx; }
.btn { text-align: center; font-size: 28rpx; color: #fff; padding: 18rpx 0; border-radius: 24rpx; margin-top: 14rpx; }
.btn.ghost { background: var(--brand-weak, #f0f2f5); color: var(--brand, #409eff); }
.btn.danger { background: #f56c6c; }
.btn-row .btn { margin-left: 16rpx; padding: 14rpx 40rpx; }
.lock-tip { font-size: 24rpx; color: var(--text-sub, #909399); text-align: center; margin-top: 14rpx; }
.loading { text-align: center; color: var(--text-sub, #909399); font-size: 26rpx; padding: 100rpx 0; }
</style>
