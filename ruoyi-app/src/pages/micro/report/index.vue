<template>
    <view class="page">
        <view class="tab-bar">
            <view v-for="t in tabs" :key="t.key" class="tab-item" :class="{ active: currentTab === t.key }" @click="switchTab(t.key)">
                <text>{{ t.label }}</text>
            </view>
        </view>
        <view v-if="!loaded" class="loading">加载中…</view>
        <view v-else class="card">
            <view class="grid">
                <view class="stat"><text class="num income">¥{{ formatAmount(data.totalIncome || 0) }}</text><text class="label">收入</text></view>
                <view class="stat"><text class="num expense">¥{{ formatAmount(data.totalExpense || 0) }}</text><text class="label">支出</text></view>
                <view class="stat"><text class="num profit">¥{{ formatAmount(data.netProfit || 0) }}</text><text class="label">净利</text></view>
                <view class="stat"><text class="num">{{ data.orderCount || 0 }}</text><text class="label">订单数</text></view>
            </view>
            <view v-if="data.period" class="period">统计周期：{{ data.period }}</view>
        </view>
        <view class="card" @click="go('/pages/micro/report/trend')">
            <text class="link-text">查看营收趋势（按日快照） ›</text>
        </view>
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get } from '../../../utils/request'
import config from '../../../config'
import { formatAmount } from '../../../utils/format'

export default {
    data() {
        return {
            loaded: false, currentTab: 'weekly', data: {},
            tabs: [
                { key: 'weekly', label: '周报' },
                { key: 'monthly', label: '月报' },
                { key: 'cashFlow', label: '资金水位' }
            ]
        }
    },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        this.loadData()
    },
    methods: {
        formatAmount,
        go(url) { uni.navigateTo({ url }) },
        switchTab(key) { this.currentTab = key; this.loadData() },
        async loadData() {
            this.loaded = false
            try {
                const apiMap = { weekly: config.api.microReportWeekly, monthly: config.api.microReportMonthly, cashFlow: config.api.microReportCashFlow }
                const res = await get(apiMap[this.currentTab])
                this.data = (res && res.data) || {}
            } catch (e) { this.data = {} } finally { this.loaded = true }
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; }
.tab-bar { display: flex; background: #fff; border-radius: 12rpx; padding: 12rpx; margin-bottom: 24rpx; }
.tab-item { flex: 1; text-align: center; padding: 16rpx 0; font-size: 26rpx; color: #606266; border-radius: 8rpx; }
.tab-item.active { background: #409eff; color: #fff; }
.card { background: #fff; border-radius: 16rpx; padding: 32rpx; margin-bottom: 24rpx; }
.grid { display: flex; flex-wrap: wrap; }
.stat { width: 50%; box-sizing: border-box; padding: 16rpx 8rpx; text-align: center; }
.num { display: block; font-size: 36rpx; font-weight: bold; }
.num.income { color: #67c23a; }
.num.expense { color: #f56c6c; }
.num.profit { color: #409eff; }
.label { display: block; font-size: 24rpx; color: #909399; margin-top: 8rpx; }
.period { font-size: 24rpx; color: #909399; text-align: center; margin-top: 16rpx; }
.link-text { font-size: 28rpx; color: #409eff; }
.loading { text-align: center; color: #909399; padding: 80rpx 0; }
</style>
