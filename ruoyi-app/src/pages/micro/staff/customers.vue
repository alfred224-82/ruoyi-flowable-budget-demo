<template>
    <view class="page">
        <view class="card-header">
            <text class="card-title">我的顾客</text>
        </view>

        <view v-if="!loaded" class="loading">加载中…</view>
        <view v-else-if="!customers.length" class="empty">暂无顾客记录</view>
        <view v-else>
            <view v-for="c in customers" :key="c.id" class="card customer-card">
                <view class="cust-top">
                    <text class="cust-name">{{ c.customerPhone || '顾客' }}</text>
                    <text class="cust-time">{{ formatTime(c.lastScanTime) }}</text>
                </view>
                <view class="cust-bottom">
                    <text class="cust-label">归因时间：{{ formatTime(c.lastScanTime) }}</text>
                    <text class="cust-status" :class="c.status === '0' ? 'active' : 'inactive'">
                        {{ c.status === '0' ? '有效' : '失效' }}
                    </text>
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
        return { loaded: false, customers: [] }
    },
    onShow() { this.loadCustomers() },
    methods: {
        async loadCustomers() {
            this.loaded = false
            try {
                const res = await get(config.api.microStaffCustomers, { pageNum: 1, pageSize: 50 })
                const data = (res && res.data) || {}
                this.customers = data.rows || data.list || []
            } catch (e) { this.customers = [] }
            this.loaded = true
        },
        formatTime(t) {
            if (!t) return '-'
            // T30.1 时间精度统一到秒：后端 DATETIME 串截到秒（yyyy-MM-dd HH:mm:ss，19 位）
            return typeof t === 'string' ? t.substring(0, 19) : '-'
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; min-height: 100vh; background: #f5f6f8; }
.card-header { margin-bottom: 20rpx; }
.card-title { font-size: 34rpx; font-weight: bold; }
.card { background: #fff; border-radius: 16rpx; padding: 28rpx; margin-bottom: 20rpx; }
.cust-top { display: flex; align-items: center; justify-content: space-between; }
.cust-name { font-size: 30rpx; font-weight: bold; flex: 1; }
.cust-time { font-size: 22rpx; color: #c0c4cc; }
.cust-bottom { margin-top: 12rpx; display: flex; align-items: center; justify-content: space-between; }
.cust-label { font-size: 24rpx; color: #909399; }
.cust-status { font-size: 24rpx; padding: 4rpx 16rpx; border-radius: 6rpx; }
.cust-status.active { background: #f0f9eb; color: #67c23a; }
.cust-status.inactive { background: #f0f2f5; color: #909399; }
.loading, .empty { text-align: center; color: #909399; padding: 80rpx 0; }
</style>
