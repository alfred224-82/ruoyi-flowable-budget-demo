<template>
    <view class="block revenue-week">
        <view class="block-header">
            <text class="block-title">本周收入概览</text>
            <text class="block-period">{{ periodLabel }}</text>
        </view>
        <view v-if="loading" class="block-loading">加载中…</view>
        <view v-else-if="!dailyDetails.length" class="block-empty">暂无数据</view>
        <view v-else class="chart-area">
            <view class="total-row">
                <text class="total-label">本周合计</text>
                <text class="total-value">¥{{ formatAmount(weekTotal) }}</text>
            </view>
            <view class="bar-list">
                <view v-for="d in dailyDetails" :key="d.date" class="bar-item">
                    <text class="bar-date">{{ d.date ? d.date.substring(5) : '' }}</text>
                    <view class="bar-track">
                        <view class="bar-fill" :style="{ width: barWidth(d.amount) }" />
                    </view>
                    <text class="bar-value">¥{{ formatAmount(d.amount || 0) }}</text>
                </view>
            </view>
        </view>
    </view>
</template>

<script>
/**
 * 区块组件：本周收入概览（T15.16，blockKey=revenue_week）
 * 数据源：GET /micro/report/weekly
 * 参数：period=week（默认）
 */
import { get } from '../../utils/request'
import config from '../../config'

export default {
    name: 'BlockRevenueWeek',
    props: {
        blockParams: { type: Object, default: () => ({}) }
    },
    data() {
        return { loading: false, dailyDetails: [], weekTotal: 0 }
    },
    computed: {
        periodLabel() {
            const now = new Date()
            const start = new Date(now)
            start.setDate(now.getDate() - now.getDay() + 1)
            return (start.getMonth() + 1) + '/' + start.getDate() + ' - ' + (now.getMonth() + 1) + '/' + now.getDate()
        }
    },
    created() { this.loadData() },
    methods: {
        formatAmount(v) { return (v || 0).toFixed(2) },
        barWidth(amount) {
            if (!this.weekTotal) return '0%'
            return Math.max(4, Math.round((amount / this.weekTotal) * 100 * (this.dailyDetails.length || 1) / 7 * 2.5)) + '%'
        },
        async loadData() {
            this.loading = true
            try {
                const res = await get(config.api.microReportWeekly)
                const data = (res && res.data) || {}
                this.dailyDetails = data.dailyDetails || []
                this.weekTotal = this.dailyDetails.reduce((s, d) => s + (d.amount || 0), 0)
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
.block-period { font-size: 22rpx; color: #909399; }
.block-loading, .block-empty { text-align: center; color: #909399; padding: 30rpx 0; font-size: 24rpx; }
.total-row { display: flex; justify-content: space-between; margin-bottom: 16rpx; }
.total-label { font-size: 26rpx; color: #606266; }
.total-value { font-size: 32rpx; font-weight: bold; color: #409eff; }
.bar-item { display: flex; align-items: center; margin-bottom: 10rpx; }
.bar-date { width: 80rpx; font-size: 22rpx; color: #909399; }
.bar-track { flex: 1; height: 16rpx; background: #f0f2f5; border-radius: 8rpx; margin: 0 12rpx; overflow: hidden; }
.bar-fill { height: 100%; background: linear-gradient(90deg, #409eff, #69c0ff); border-radius: 8rpx; transition: width 0.3s; }
.bar-value { width: 120rpx; font-size: 22rpx; color: #606266; text-align: right; }
</style>
