<template>
    <view class="block revenue-received">
        <view class="block-header">
            <text class="block-title">已结算收入</text>
            <text class="block-sub">今日</text>
        </view>
        <view v-if="loading" class="block-loading">加载中…</view>
        <view v-else class="amount-card">
            <text class="amount-value">¥{{ formatAmount(todayData.receivedAmount || todayData.todayRevenue || 0) }}</text>
            <text class="amount-label">已结算金额</text>
            <view class="count-row">
                <text class="count-item">笔数：{{ todayData.receivedCount || todayData.todayOrderCount || 0 }}</text>
            </view>
        </view>
    </view>
</template>

<script>
/**
 * 区块组件：已结算收入（T15.16，blockKey=revenue_received）
 * 数据源：GET /micro/report/today
 */
import { get } from '../../utils/request'
import config from '../../config'

export default {
    name: 'BlockRevenueReceived',
    props: { blockParams: { type: Object, default: () => ({}) } },
    data() { return { loading: false, todayData: {} } },
    created() { this.loadData() },
    methods: {
        formatAmount(v) { return (v || 0).toFixed(2) },
        async loadData() {
            this.loading = true
            try {
                const res = await get(config.api.microReportToday)
                this.todayData = (res && res.data) || {}
            } catch (e) { /* 静默降级 */ }
            this.loading = false
        }
    }
}
</script>

<style scoped>
.block { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 20rpx; }
.block-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.block-title { font-size: 30rpx; font-weight: bold; }
.block-sub { font-size: 22rpx; color: #909399; }
.block-loading { text-align: center; color: #909399; padding: 30rpx 0; font-size: 24rpx; }
.amount-card { text-align: center; padding: 16rpx 0; }
.amount-value { font-size: 48rpx; font-weight: bold; color: #67c23a; display: block; }
.amount-label { font-size: 24rpx; color: #909399; display: block; margin-top: 8rpx; }
.count-row { margin-top: 12rpx; }
.count-item { font-size: 24rpx; color: #606266; }
</style>
