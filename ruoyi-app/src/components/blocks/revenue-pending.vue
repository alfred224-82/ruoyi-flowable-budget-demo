<template>
    <view class="block revenue-pending">
        <view class="block-header">
            <text class="block-title">待结算收入</text>
            <text class="block-sub">待服务订单</text>
        </view>
        <view v-if="loading" class="block-loading">加载中…</view>
        <view v-else-if="!pendingCount" class="block-empty">暂无待结算订单</view>
        <view v-else class="pending-card">
            <text class="pending-count">{{ pendingCount }}</text>
            <text class="pending-label">笔待结算</text>
            <text class="pending-amount">¥{{ formatAmount(pendingAmount) }}</text>
        </view>
    </view>
</template>

<script>
/**
 * 区块组件：待结算收入（T15.16，blockKey=revenue_pending）
 * 数据源：GET /micro/booking/list?orderStatus=PENDING_SERVICE
 */
import { get } from '../../utils/request'
import config from '../../config'

export default {
    name: 'BlockRevenuePending',
    props: { blockParams: { type: Object, default: () => ({}) } },
    data() { return { loading: false, pendingCount: 0, pendingAmount: 0 } },
    created() { this.loadData() },
    methods: {
        formatAmount(v) { return (v || 0).toFixed(2) },
        async loadData() {
            this.loading = true
            try {
                const res = await get(config.api.microBookingList, { orderStatus: 'PENDING_SERVICE', pageNum: 1, pageSize: 100 })
                const rows = (res && res.rows) || []
                this.pendingCount = (res && res.total) || rows.length
                this.pendingAmount = rows.reduce((s, r) => s + (r.amount || 0), 0)
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
.block-loading, .block-empty { text-align: center; color: #909399; padding: 30rpx 0; font-size: 24rpx; }
.pending-card { text-align: center; padding: 16rpx 0; }
.pending-count { font-size: 56rpx; font-weight: bold; color: #e6a23c; display: block; }
.pending-label { font-size: 24rpx; color: #909399; display: block; margin-top: 4rpx; }
.pending-amount { font-size: 28rpx; color: #e6a23c; display: block; margin-top: 8rpx; }
</style>
