<template>
    <view class="page">
        <view class="card-header">
            <text class="card-title">我的优惠券</text>
        </view>

        <view v-if="!loaded" class="loading">加载中…</view>
        <view v-else-if="!coupons.length" class="empty">暂无优惠券</view>
        <view v-else>
            <view v-for="c in coupons" :key="c.id" class="card coupon-card">
                <view class="coupon-top">
                    <text class="coupon-name">{{ c.couponName || '优惠券' }}</text>
                    <text class="coupon-status" :class="statusClass(c.status)">{{ statusText(c.status) }}</text>
                </view>
                <view class="coupon-bottom">
                    <text class="coupon-time" v-if="c.receiveTime">领取：{{ c.receiveTime }}</text>
                    <text class="coupon-time" v-if="c.expireTime">有效期至：{{ c.expireTime }}</text>
                </view>
            </view>
        </view>
    </view>
</template>

<script>
import { get } from '../../../utils/request'
import config from '../../../config'

export default {
    data() {
        return { loaded: false, coupons: [] }
    },
    onShow() { this.loadCoupons() },
    methods: {
        async loadCoupons() {
            this.loaded = false
            try {
                const res = await get(config.api.microCustomerCoupon)
                this.coupons = (res && res.data) || []
            } catch (e) { this.coupons = [] }
            this.loaded = true
        },
        statusText(s) {
            const map = { UNUSED: '未使用', USED: '已使用', EXPIRED: '已过期' }
            return map[s] || s || '-'
        },
        statusClass(s) {
            if (s === 'UNUSED') return 'active'
            if (s === 'USED') return 'used'
            return 'expired'
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; min-height: 100vh; background: #f5f6f8; }
.card-header { margin-bottom: 20rpx; }
.card-title { font-size: 34rpx; font-weight: bold; }
.card { background: #fff; border-radius: 16rpx; padding: 28rpx; margin-bottom: 20rpx; }
.coupon-card { border-left: 8rpx solid #409eff; }
.coupon-top { display: flex; align-items: center; justify-content: space-between; }
.coupon-name { font-size: 30rpx; font-weight: bold; flex: 1; }
.coupon-status { font-size: 24rpx; padding: 4rpx 16rpx; border-radius: 6rpx; }
.coupon-status.active { background: #f0f9eb; color: #67c23a; }
.coupon-status.used { background: #f0f2f5; color: #909399; }
.coupon-status.expired { background: #fef0f0; color: #f56c6c; }
.coupon-bottom { margin-top: 16rpx; }
.coupon-time { font-size: 22rpx; color: #909399; display: block; margin-top: 4rpx; }
.loading, .empty { text-align: center; color: #909399; padding: 80rpx 0; }
</style>
