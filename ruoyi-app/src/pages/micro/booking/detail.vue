<template>
    <view class="page">
        <view v-if="!loaded" class="loading">加载中…</view>
        <template v-else-if="order">
            <view class="card">
                <view class="status-bar" :style="{ color: statusColor(order.orderStatus) }">
                    {{ statusText(order.orderStatus) }}
                </view>
                <view class="info-row"><text class="label">顾客</text><text>{{ order.customerName || '-' }}</text></view>
                <view class="info-row"><text class="label">手机号</text><text>{{ order.customerPhone || '-' }}</text></view>
                <view class="info-row"><text class="label">服务项目</text><text>{{ order.serviceItemName || '-' }}</text></view>
                <view class="info-row"><text class="label">预约时间</text><text>{{ order.bookingTime || '-' }}</text></view>
                <view class="info-row"><text class="label">金额</text><text class="amount">{{ order.amount != null ? '¥' + formatAmount(order.amount) : '-' }}</text></view>
                <view class="info-row"><text class="label">收款状态</text>
                    <text :style="{ color: payColor(order.payStatus) }">{{ payText(order.payStatus) }}</text></view>
                <view v-if="order.staffName" class="info-row"><text class="label">业务员</text><text>{{ order.staffName }}</text></view>
                <view v-if="order.remark" class="info-row"><text class="label">备注</text><text>{{ order.remark }}</text></view>
            </view>
            <view class="actions">
                <button v-if="order.orderStatus === 'PENDING_SERVICE'" class="btn primary" @click="complete">完成服务</button>
                <button v-if="order.payStatus === 'PENDING_PAY' && order.orderStatus !== 'CANCELLED'" class="btn success" @click="receive">标记收款</button>
                <button v-if="order.orderStatus === 'PENDING_SERVICE'" class="btn warn" @click="cancel">取消预约</button>
            </view>
        </template>
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get, put } from '../../../utils/request'
import config from '../../../config'
import { formatAmount, BOOKING_STATUS, PAY_STATUS } from '../../../utils/format'

export default {
    data() { return { loaded: false, order: null } },
    onLoad(options) { this.id = options.id; this.loadDetail() },
    methods: {
        formatAmount,
        statusText(s) { return (BOOKING_STATUS[s] || {}).label || s || '-' },
        statusColor(s) { return (BOOKING_STATUS[s] || {}).color || '#909399' },
        payText(s) { return (PAY_STATUS[s] || {}).label || s || '-' },
        payColor(s) { return (PAY_STATUS[s] || {}).color || '#909399' },
        async loadDetail() {
            try {
                const res = await get('/micro/booking/' + this.id)
                this.order = (res && res.data) || null
            } catch (e) { /* 已统一提示 */ } finally { this.loaded = true }
        },
        async complete() {
            uni.showModal({ title: '确认', content: '确认已完成服务？', success: async (r) => {
                if (r.confirm) { await put(config.api.microBookingComplete + '/' + this.id); uni.showToast({ title: '已完成' }); this.loadDetail() }
            }})
        },
        async receive() {
            uni.showModal({ title: '确认收款', content: '确认已线下收款？', success: async (r) => {
                if (r.confirm) { await put(config.api.microBookingReceive + '/' + this.id); uni.showToast({ title: '已收款' }); this.loadDetail() }
            }})
        },
        async cancel() {
            uni.showModal({ title: '取消', content: '确认取消该预约？', success: async (r) => {
                if (r.confirm) { await put(config.api.microBookingCancel + '/' + this.id); uni.showToast({ title: '已取消' }); this.loadDetail() }
            }})
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 32rpx; margin-bottom: 24rpx; }
.status-bar { font-size: 36rpx; font-weight: bold; text-align: center; margin-bottom: 32rpx; }
.info-row { display: flex; justify-content: space-between; padding: 16rpx 0; border-bottom: 1rpx solid #f0f2f5; font-size: 28rpx; }
.info-row .label { color: #909399; }
.amount { font-weight: bold; color: #409eff; }
.actions { display: flex; flex-direction: column; gap: 20rpx; }
.btn { border-radius: 12rpx; font-size: 30rpx; border: none; }
.btn.primary { background: #409eff; color: #fff; }
.btn.success { background: #67c23a; color: #fff; }
.btn.warn { background: #f56c6c; color: #fff; }
.loading { text-align: center; color: #909399; padding: 80rpx 0; }
</style>
