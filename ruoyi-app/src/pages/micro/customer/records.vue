<template>
    <view class="page" :class="themeClass" :style="pageStyle">
        <view class="card-header">
            <text class="card-title">消费记录</text>
        </view>

        <view v-if="!loaded" class="loading">加载中…</view>
        <view v-else-if="!records.length" class="empty">暂无消费记录</view>
        <view v-else>
            <view v-for="r in records" :key="r.id" class="card record-card">
                <view class="record-top">
                    <text class="record-type">{{ r.flowType === 'income' ? '收入' : '支出' }}</text>
                    <text class="record-amount" :class="r.flowType === 'income' ? 'income' : 'expense'">
                        {{ r.flowType === 'income' ? '+' : '-' }}¥{{ formatAmount(r.amount || 0) }}
                    </text>
                </view>
                <view class="record-bottom">
                    <text class="record-desc">{{ r.remark || r.category || '-' }}</text>
                    <text class="record-time">{{ r.createTime || '' }}</text>
                </view>
            </view>
        </view>
    </view>
</template>

<script>
import { get } from '../../../utils/request'
import config from '../../../config'
import { formatAmount } from '../../../utils/format'
import themeMixin from '../../../utils/themeMixin'

export default {
    mixins: [themeMixin],
    data() {
        return { loaded: false, records: [] }
    },
    onShow() { this.loadRecords() },
    methods: {
        formatAmount,
        async loadRecords() {
            this.loaded = false
            try {
                const res = await get(config.api.microCustomerRecords, { pageNum: 1, pageSize: 50 })
                const data = (res && res.data) || {}
                this.records = data.rows || data.list || []
            } catch (e) { this.records = [] }
            this.loaded = true
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; min-height: 100vh; background: var(--page-bg, #f5f6f8); }
.card-header { margin-bottom: 20rpx; }
.card-title { font-size: 34rpx; font-weight: bold; color: var(--text, #303133); }
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 28rpx; margin-bottom: 20rpx; box-shadow: var(--shadow-card, none); }
.record-top { display: flex; align-items: center; justify-content: space-between; }
.record-type { font-size: 28rpx; color: var(--text-sub, #606266); }
.record-amount { font-size: 34rpx; font-weight: bold; }
.record-amount.income { color: #67c23a; }
.record-amount.expense { color: #f56c6c; }
.record-bottom { margin-top: 12rpx; display: flex; justify-content: space-between; align-items: center; }
.record-desc { font-size: 24rpx; color: var(--text-sub, #909399); flex: 1; }
.record-time { font-size: 22rpx; color: var(--text-sub, #c0c4cc); }
.loading, .empty { text-align: center; color: var(--text-sub, #909399); padding: 80rpx 0; }
</style>
