<template>
    <view class="block quick-booking">
        <view class="booking-entry" @click="goBooking">
            <view class="entry-icon">📅</view>
            <view class="entry-text">
                <text class="entry-title">快速预约</text>
                <text class="entry-desc">一键预约服务</text>
            </view>
            <text class="entry-arrow">›</text>
        </view>
    </view>
</template>

<script>
/**
 * 区块组件：快速预约入口（T15.16，blockKey=quick_booking）
 * 纯导航组件，不加载数据。
 */
export default {
    name: 'BlockQuickBooking',
    props: { blockParams: { type: Object, default: () => ({}) } },
    methods: {
        goBooking() {
            uni.navigateTo({ url: '/pages/micro/customerBook/submit' })
        }
    }
}
</script>

<style scoped>
.block { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 20rpx; }
.booking-entry { display: flex; align-items: center; }
.entry-icon { font-size: 48rpx; margin-right: 20rpx; }
.entry-text { flex: 1; }
.entry-title { font-size: 30rpx; font-weight: bold; display: block; }
.entry-desc { font-size: 24rpx; color: #909399; display: block; margin-top: 4rpx; }
.entry-arrow { font-size: 40rpx; color: #c0c4cc; }
</style>
