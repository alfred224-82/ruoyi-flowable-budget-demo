<template>
    <view class="page">
        <view v-if="!loaded" class="loading">加载中…</view>
        <view v-else-if="order" class="card">
            <text class="card-title">订单详情</text>
            <view class="info-row"><text class="label">顾客</text><text>{{ order.customerName || '-' }}</text></view>
            <view class="info-row"><text class="label">服务项目</text><text>{{ order.serviceItemName || '-' }}</text></view>
            <view class="info-row"><text class="label">预约时间</text><text>{{ order.bookingTime || '-' }}</text></view>
            <view class="info-row"><text class="label">完成时间</text><text>{{ order.completeTime || '-' }}</text></view>
            <view class="info-row"><text class="label">金额</text><text class="amount">¥{{ formatAmount(order.amount || 0) }}</text></view>
            <view class="info-row"><text class="label">状态</text>
                <text :style="{ color: statusColor(order.orderStatus) }">{{ statusText(order.orderStatus) }}</text></view>
        </view>
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get } from '../../../utils/request'
import config from '../../../config'
import { formatAmount, BOOKING_STATUS } from '../../../utils/format'

export default {
    data() { return { loaded: false, order: null } },
    onLoad(options) { this.id = options.id; this.loadDetail() },
    methods: {
        formatAmount,
        statusText(s) { return (BOOKING_STATUS[s] || {}).label || s || '-' },
        statusColor(s) { return (BOOKING_STATUS[s] || {}).color || '#909399' },
        async loadDetail() {
            try {
                const res = await get(config.api.microStaffSchedule + '/' + this.id)
                this.order = (res && res.data) || null
            } catch (e) { /* 已统一提示 */ } finally { this.loaded = true }
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 32rpx; }
.card-title { font-size: 30rpx; font-weight: bold; display: block; margin-bottom: 24rpx; }
.info-row { display: flex; justify-content: space-between; padding: 16rpx 0; border-bottom: 1rpx solid #f0f2f5; font-size: 28rpx; }
.info-row .label { color: #909399; }
.amount { font-weight: bold; color: #67c23a; }
.loading { text-align: center; color: #909399; padding: 80rpx 0; }
</style>
