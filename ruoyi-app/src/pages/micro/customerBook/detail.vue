<template>
    <view class="page" :class="themeClass" :style="pageStyle">
        <view v-if="!loaded" class="loading">加载中…</view>
        <template v-else-if="order">
            <view class="card">
                <view class="status-bar" :style="{ color: statusColor(order.orderStatus) }">{{ statusText(order.orderStatus) }}</view>
                <view class="info-row"><text class="label">服务项目</text><text>{{ order.serviceName || order.serviceItemName || '-' }}</text></view>
                <view class="info-row"><text class="label">预约时间</text><text>{{ order.bookingTime || '-' }}</text></view>
                <!-- ★费用明细 + 合计（remark.costDetails）；历史订单无明细则回退展示 amount -->
                <template v-if="cost.details.length">
                    <view v-for="(cd, ci) in cost.details" :key="ci" class="info-row">
                        <text class="label">{{ cd.name }}（{{ cd.type }}）</text>
                        <text>{{ cd.unitPrice }}元/小时 × {{ cd.hours }}小时 = {{ cd.subtotal }}元</text>
                    </view>
                    <view class="info-row"><text class="label">合计</text><text class="amount">¥{{ formatAmount(cost.total) }}</text></view>
                </template>
                <view v-else class="info-row"><text class="label">金额</text><text class="amount">¥{{ formatAmount(order.amount || 0) }}</text></view>
                <view v-if="order.staffName" class="info-row"><text class="label">服务员</text><text>{{ order.staffName }}</text></view>
                <view v-if="cost.remarkText" class="info-row"><text class="label">备注</text><text>{{ cost.remarkText }}</text></view>
            </view>
            <!-- 收款方式提示 -->
            <view class="card pay-info">
                <text class="pay-title">收款方式</text>
                <text class="pay-desc">{{ payInfoText || '本店支持到店付款，服务完成后请向前台付款。' }}</text>
            </view>
            <view v-if="order.orderStatus === 'PENDING_SERVICE'" class="actions">
                <button class="btn-cancel" @click="cancel">取消预约</button>
            </view>
        </template>
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get, put } from '../../../utils/request'
import config from '../../../config'
import { formatAmount, BOOKING_STATUS, parseBookingCost } from '../../../utils/format'
import themeMixin from '../../../utils/themeMixin'

export default {
    mixins: [themeMixin],
    data() { return { loaded: false, order: null, payInfoText: '' } },
    computed: {
        // ★解析 remark 中的费用明细/合计/备注文本
        cost() {
            return this.order ? parseBookingCost(this.order.remark)
                : { details: [], total: 0, hours: 0, peopleCount: 0, remarkText: '' }
        }
    },
    onLoad(options) { this.id = options.id; this.loadDetail(); this.loadPayInfo() },
    methods: {
        formatAmount,
        statusText(s) { return (BOOKING_STATUS[s] || {}).label || s || '-' },
        statusColor(s) { return (BOOKING_STATUS[s] || {}).color || '#909399' },
        async loadDetail() {
            try {
                // ★顾客域详情接口（含本人归属校验），boss 域 /micro/booking/{id} 顾客无权限
                const res = await get(config.api.microCustomerMyBookDetail + this.id)
                this.order = (res && res.data) || null
            } catch (e) { /* 已统一提示 */ } finally { this.loaded = true }
        },
        async loadPayInfo() {
            try {
                const res = await get(config.api.microCustomerPayInfo)
                this.payInfoText = (res && res.data) || ''
            } catch (e) { /* ignore */ }
        },
        cancel() {
            uni.showModal({
                title: '取消预约', content: '确认取消该预约？',
                success: async (r) => {
                    if (r.confirm) {
                        try {
                            await put(config.api.microCustomerCancelBook + '/' + this.id)
                            uni.showToast({ title: '已取消', icon: 'success' })
                            this.loadDetail()
                        } catch (e) { /* 已统一提示 */ }
                    }
                }
            })
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; background: var(--page-bg, #f5f6f8); }
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 32rpx; margin-bottom: 24rpx; box-shadow: var(--shadow-card, none); }
.status-bar { font-size: 36rpx; font-weight: bold; text-align: center; margin-bottom: 32rpx; }
.info-row { display: flex; justify-content: space-between; padding: 16rpx 0; border-bottom: 1rpx solid var(--border, #f0f2f5); font-size: 28rpx; color: var(--text, #303133); }
.info-row .label { color: var(--text-sub, #909399); }
.amount { font-weight: bold; color: var(--brand, #409eff); }
.pay-info { background: #fdf6ec; }
.pay-title { font-size: 28rpx; font-weight: bold; color: #e6a23c; display: block; margin-bottom: 12rpx; }
.pay-desc { font-size: 26rpx; color: #606266; }
.actions { margin-top: 20rpx; }
.btn-cancel { background: var(--card-bg, #fff); color: #f56c6c; border: 2rpx solid #f56c6c; border-radius: var(--radius-btn, 12rpx); font-size: 30rpx; }
.loading { text-align: center; color: var(--text-sub, #909399); padding: 80rpx 0; }
</style>
