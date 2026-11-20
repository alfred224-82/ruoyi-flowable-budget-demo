<template>
    <view class="page">
        <!-- 收入汇总 -->
        <view class="card summary">
            <text class="card-title">我的收入</text>
            <view class="grid">
                <view class="stat">
                    <text class="num">¥{{ formatAmount(income.weekAmount || 0) }}</text>
                    <text class="label">本周应发</text>
                </view>
                <view class="stat">
                    <text class="num">¥{{ formatAmount(income.monthAmount || 0) }}</text>
                    <text class="label">本月应发</text>
                </view>
                <view class="stat">
                    <text class="num">{{ income.weekOrders || 0 }}</text>
                    <text class="label">本周单量</text>
                </view>
                <view class="stat">
                    <text class="num">{{ income.monthOrders || 0 }}</text>
                    <text class="label">本月单量</text>
                </view>
            </view>
        </view>

        <!-- 每单明细 -->
        <view class="card">
            <text class="card-title">已完成订单明细</text>
            <view v-if="!detailLoaded" class="loading">加载中…</view>
            <view v-else-if="!details.length" class="empty">暂无已完成订单</view>
            <view v-else>
                <view v-for="item in details" :key="item.id" class="detail-item" @click="goDetail(item.id)">
                    <view class="detail-info">
                        <text class="customer">{{ item.customerName || '顾客' }}</text>
                        <text class="service">{{ item.serviceItemName || '-' }}</text>
                        <text class="time">{{ item.completeTime || item.bookingTime || '-' }}</text>
                    </view>
                    <text class="amount">¥{{ formatAmount(item.amount || 0) }}</text>
                </view>
            </view>
        </view>
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get } from '../../../utils/request'
import config from '../../../config'
import { formatAmount } from '../../../utils/format'

export default {
    data() { return { income: {}, details: [], detailLoaded: false } },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        this.loadData()
    },
    methods: {
        formatAmount,
        goDetail(id) { uni.navigateTo({ url: '/pages/micro/staffIncome/detail?id=' + id }) },
        async loadData() {
            try {
                const [incomeRes, detailRes] = await Promise.all([
                    get(config.api.microStaffIncome),
                    get(config.api.microStaffIncomeDetail)
                ])
                this.income = (incomeRes && incomeRes.data) || {}
                this.details = (detailRes && detailRes.data) || []
            } catch (e) { /* ignore */ } finally { this.detailLoaded = true }
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 32rpx; margin-bottom: 24rpx; }
.card-title { font-size: 30rpx; font-weight: bold; display: block; margin-bottom: 20rpx; }
.grid { display: flex; flex-wrap: wrap; }
.stat { width: 50%; box-sizing: border-box; padding: 16rpx 8rpx; text-align: center; }
.num { display: block; font-size: 36rpx; font-weight: bold; color: #67c23a; }
.label { display: block; font-size: 24rpx; color: #909399; margin-top: 8rpx; }
.detail-item { display: flex; justify-content: space-between; align-items: center; padding: 20rpx 0; border-bottom: 1rpx solid #f0f2f5; }
.detail-item:last-child { border-bottom: none; }
.detail-info { flex: 1; }
.customer { font-size: 28rpx; font-weight: bold; display: block; }
.service { font-size: 24rpx; color: #606266; margin-top: 4rpx; display: block; }
.time { font-size: 22rpx; color: #909399; margin-top: 4rpx; display: block; }
.amount { font-size: 30rpx; font-weight: bold; color: #67c23a; }
.loading, .empty { text-align: center; color: #909399; padding: 60rpx 0; }
</style>
