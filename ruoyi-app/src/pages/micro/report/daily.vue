<template>
    <view class="page" :class="'theme-' + theme.key" :style="rootStyle">
        <!-- 顶部三页签 -->
        <view class="tabbar">
            <view v-for="t in tabs" :key="t.key" class="tab" :class="{ active: t.key === 'daily' }" @click="switchReport(t.key)">
                <text>{{ t.label }}</text>
            </view>
        </view>

        <!-- 日期选择 -->
        <view class="card filter">
            <view class="f-row">
                <text class="f-label">选择日期</text>
                <picker mode="date" :value="date" @change="onDateChange">
                    <view class="f-picker">{{ date }} ▾</view>
                </picker>
            </view>
        </view>

        <view v-if="!loaded" class="loading">加载中…</view>
        <template v-else>
            <!-- 当日汇总 -->
            <view class="card">
                <text class="card-title">当日经营汇总</text>
                <view class="grid">
                    <view class="cell"><text class="c-label">已结算场次</text><text class="c-val">{{ data.sessionCount || 0 }}</text></view>
                    <view class="cell"><text class="c-label">实收</text><text class="c-val income">¥{{ formatAmount(data.revenue) }}</text></view>
                    <view class="cell"><text class="c-label">变动成本</text><text class="c-val expense">¥{{ formatAmount(data.cost) }}</text></view>
                    <view class="cell"><text class="c-label">当日毛利</text><text class="c-val strong">¥{{ formatAmount(data.grossProfit) }}</text></view>
                    <view class="cell"><text class="c-label">日均固定成本<text class="ref">（均摊参考）</text></text><text class="c-val">¥{{ formatAmount(data.dailyFixedCostRef) }}</text></view>
                    <view class="cell"><text class="c-label">预估净利</text>
                        <text class="c-val" :style="{ color: (data.estNetProfit || 0) >= 0 ? '#67c23a' : '#f56c6c' }">¥{{ formatAmount(data.estNetProfit) }}</text>
                    </view>
                </view>
                <text class="foot-note">日均固定成本为月固定成本按天均摊的参考值，非当日实际支出。</text>
            </view>

            <!-- 本月进度 -->
            <view class="card">
                <text class="card-title">本月保本进度</text>
                <view class="prog-line">
                    <text class="p-item">累计毛利 <text class="strong">¥{{ formatAmount(data.monthAccumGrossProfit) }}</text></text>
                    <text class="p-item">月保本毛利 <text class="strong">¥{{ formatAmount(data.breakevenGrossProfit) }}</text></text>
                </view>
                <view class="prog-bar">
                    <view class="prog-inner" :style="{ width: progressWidth(data.breakevenProgress) + '%', background: theme.brand }"></view>
                </view>
                <view class="prog-foot">
                    <text class="prog-pct">{{ formatAmount(data.breakevenProgress) }}%</text>
                    <text class="prog-flag" :class="data.passedBreakeven ? 'safe' : 'unsafe'">
                        {{ data.passedBreakeven ? '✓ 已过安全线' : '尚未保本，继续加油' }}
                    </text>
                </view>
            </view>

            <!-- 当日活动清单 -->
            <view class="card">
                <text class="card-title">当日活动清单（{{ (data.sessions || []).length }}）</text>
                <view v-if="!(data.sessions || []).length" class="empty">当日无活动</view>
                <view v-for="(s, idx) in data.sessions" :key="idx" class="s-row" @click="drillDown(s)">
                    <view class="s-main">
                        <text class="s-title">{{ s.activityTitle || '未命名活动' }}</text>
                        <text class="s-sub">{{ s.staffNames || '—' }} · {{ statusText(s) }}</text>
                    </view>
                    <view class="s-right">
                        <text class="s-profit">¥{{ formatAmount(s.profit) }}</text>
                        <text class="s-diff" :class="diffClass(s.profitDiff)">{{ diffText(s.profitDiff) }}</text>
                    </view>
                </view>
            </view>
        </template>
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get } from '../../../utils/request'
import config from '../../../config'
import { formatAmount, today } from '../../../utils/format'
import { diffClass, diffText, settleStatusText, progressWidth } from '../../../utils/reportFormat'
import { getThemeSync, loadTenantTheme, themeCssVars } from '../../../utils/theme'
import { applyNavBarTitle } from '../../../utils/roles'

export default {
    data() {
        return {
            theme: getThemeSync(),
            loaded: false,
            date: today(),
            data: {},
            tabs: [
                { key: 'detail', label: '活动明细' },
                { key: 'daily', label: '日报' },
                { key: 'monthly', label: '月报' }
            ]
        }
    },
    computed: { rootStyle() { return themeCssVars(this.theme.key) } },
    onLoad(options) {
        if (options && options.date) this.date = options.date
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
        statusText(row) { return settleStatusText(row.settlementStatus, row.settled) },
        switchReport(tab) {
            const map = { detail: '/pages/micro/report/activityDetail', daily: '/pages/micro/report/daily', monthly: '/pages/micro/report/monthly' }
            if (tab === 'daily') return
            uni.redirectTo({ url: map[tab] })
        },
        onDateChange(e) { this.date = e.detail.value; this.loadData() },
        async loadData() {
            this.loaded = false
            try {
                const res = await get(config.api.microOperatingDaily, { date: this.date })
                this.data = (res && res.data) || {}
            } catch (e) { this.data = {} } finally { this.loaded = true }
        },
        drillDown(row) {
            if (!row.activityId) return
            uni.navigateTo({ url: '/pages/micro/report/settlementDetail?activityId=' + row.activityId })
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
.card-title { display: block; font-size: 30rpx; font-weight: bold; color: var(--text, #303133); margin-bottom: 20rpx; }
.filter .f-row { display: flex; align-items: center; justify-content: space-between; }
.f-label { font-size: 28rpx; color: var(--text, #303133); }
.f-picker { font-size: 28rpx; color: var(--brand, #409eff); }
.grid { display: flex; flex-wrap: wrap; }
.cell { width: 50%; box-sizing: border-box; padding: 14rpx 8rpx; }
.c-label { display: block; font-size: 24rpx; color: var(--text-sub, #909399); }
.ref { font-size: 20rpx; }
.c-val { display: block; font-size: 34rpx; font-weight: bold; color: var(--text, #303133); margin-top: 8rpx; }
.c-val.income { color: #67c23a; }
.c-val.expense { color: #f56c6c; }
.c-val.strong { color: var(--brand, #409eff); }
.foot-note { display: block; font-size: 22rpx; color: var(--text-sub, #909399); margin-top: 12rpx; }
.prog-line { display: flex; justify-content: space-between; margin-bottom: 16rpx; }
.p-item { font-size: 24rpx; color: var(--text-sub, #909399); }
.p-item .strong { color: var(--text, #303133); font-size: 28rpx; font-weight: bold; }
.prog-bar { height: 20rpx; background: var(--brand-weak, #f0f2f5); border-radius: 10rpx; overflow: hidden; }
.prog-inner { height: 100%; border-radius: 10rpx; }
.prog-foot { display: flex; justify-content: space-between; align-items: center; margin-top: 14rpx; }
.prog-pct { font-size: 26rpx; font-weight: bold; color: var(--text, #303133); }
.prog-flag { font-size: 24rpx; }
.prog-flag.safe { color: #67c23a; }
.prog-flag.unsafe { color: #e6a23c; }
.s-row { display: flex; align-items: center; justify-content: space-between; padding: 18rpx 0; border-bottom: 1rpx solid var(--border, #f0f2f5); }
.s-row:last-child { border-bottom: none; }
.s-main { flex: 1; overflow: hidden; }
.s-title { display: block; font-size: 28rpx; color: var(--text, #303133); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.s-sub { display: block; font-size: 22rpx; color: var(--text-sub, #909399); margin-top: 6rpx; }
.s-right { text-align: right; margin-left: 16rpx; }
.s-profit { display: block; font-size: 28rpx; font-weight: bold; color: var(--text, #303133); }
.s-diff { display: block; font-size: 22rpx; margin-top: 4rpx; }
.diff-neg { color: #f56c6c; }
.diff-zero { color: #909399; }
.diff-pos { color: #67c23a; }
.loading, .empty { text-align: center; color: var(--text-sub, #909399); font-size: 26rpx; padding: 60rpx 0; }
</style>
