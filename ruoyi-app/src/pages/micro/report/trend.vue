<template>
    <view class="page">
        <view class="card">
            <text class="card-title">营收趋势（近30天）</text>
            <view v-if="!loaded" class="loading">加载中…</view>
            <view v-else-if="!list.length" class="empty">暂无趋势数据</view>
            <view v-else class="trend-list">
                <view v-for="item in list" :key="item.revenueDate" class="trend-row">
                    <text class="date">{{ item.revenueDate }}</text>
                    <view class="bars">
                        <text class="bar income" :style="{ width: barWidth(item.totalIncome, maxVal) }">收</text>
                        <text class="bar expense" :style="{ width: barWidth(item.totalExpense, maxVal) }">支</text>
                    </view>
                    <text class="profit" :class="{ negative: (item.netProfit || 0) < 0 }">¥{{ formatAmount(item.netProfit || 0) }}</text>
                </view>
            </view>
        </view>
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { post } from '../../../utils/request'
import config from '../../../config'
import { formatAmount } from '../../../utils/format'

export default {
    data() { return { loaded: false, list: [], maxVal: 0 } },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        this.loadTrend()
    },
    methods: {
        formatAmount,
        barWidth(val, max) {
            if (!max || !val) return '0%'
            return Math.max(5, Math.round((val / max) * 100)) + '%'
        },
        async loadTrend() {
            this.loaded = false
            try {
                const res = await post(config.api.microReportTrend, {})
                this.list = (res && res.data) || []
                this.maxVal = Math.max(...this.list.map(i => Math.max(Math.abs(i.totalIncome || 0), Math.abs(i.totalExpense || 0))), 1)
            } catch (e) { this.list = [] } finally { this.loaded = true }
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 32rpx; }
.card-title { font-size: 30rpx; font-weight: bold; display: block; margin-bottom: 24rpx; }
.trend-row { display: flex; align-items: center; padding: 16rpx 0; border-bottom: 1rpx solid #f0f2f5; }
.trend-row:last-child { border-bottom: none; }
.date { width: 180rpx; font-size: 24rpx; color: #606266; }
.bars { flex: 1; display: flex; flex-direction: column; gap: 4rpx; margin: 0 16rpx; }
.bar { height: 20rpx; border-radius: 4rpx; font-size: 16rpx; color: #fff; text-align: center; line-height: 20rpx; min-width: 30rpx; }
.bar.income { background: #67c23a; }
.bar.expense { background: #f56c6c; }
.profit { width: 160rpx; text-align: right; font-size: 26rpx; font-weight: bold; color: #409eff; }
.profit.negative { color: #f56c6c; }
.loading, .empty { text-align: center; color: #909399; padding: 60rpx 0; }
</style>
