<template>
    <view class="block my-schedule">
        <view class="block-header">
            <text class="block-title">我的排班</text>
            <text class="block-sub">今日</text>
        </view>
        <view v-if="loading" class="block-loading">加载中…</view>
        <view v-else-if="!scheduleList.length" class="block-empty">今日暂无排班</view>
        <view v-else class="schedule-list">
            <view v-for="(item, idx) in scheduleList" :key="idx" class="schedule-item">
                <text class="schedule-time">{{ item.startTime || '' }} - {{ item.endTime || '' }}</text>
                <text class="schedule-content">{{ item.serviceName || item.remark || '排班' }}</text>
            </view>
        </view>
    </view>
</template>

<script>
/**
 * 区块组件：我的排班（T15.16，blockKey=my_schedule）
 * 数据源：GET /micro/staff/schedule
 */
import { get } from '../../utils/request'
import config from '../../config'

export default {
    name: 'BlockMySchedule',
    props: { blockParams: { type: Object, default: () => ({}) } },
    data() { return { loading: false, scheduleList: [] } },
    created() { this.loadData() },
    methods: {
        async loadData() {
            this.loading = true
            try {
                const res = await get(config.api.microStaffSchedule)
                this.scheduleList = (res && res.data) || (res && res.rows) || []
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
.schedule-item { display: flex; align-items: center; padding: 12rpx 0; border-bottom: 1rpx solid #f0f2f5; }
.schedule-item:last-child { border-bottom: none; }
.schedule-time { font-size: 24rpx; color: #409eff; width: 200rpx; }
.schedule-content { font-size: 26rpx; color: #303133; flex: 1; }
</style>
