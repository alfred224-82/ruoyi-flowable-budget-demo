<template>
    <view class="block checkin-entry">
        <view class="checkin-card" @click="goCheckin">
            <view class="checkin-left">
                <text class="checkin-icon">✅</text>
                <view class="checkin-text">
                    <text class="checkin-title">打卡签到</text>
                    <text class="checkin-desc" v-if="checkedIn">今日已打卡</text>
                    <text class="checkin-desc" v-else>点击签到</text>
                </view>
            </view>
            <view class="checkin-btn" :class="{ done: checkedIn }">
                {{ checkedIn ? '已签' : '签到' }}
            </view>
        </view>
    </view>
</template>

<script>
/**
 * 区块组件：打卡入口（T15.16，blockKey=checkin_entry）
 * 导航到打卡页面，展示当日打卡状态。
 */
export default {
    name: 'BlockCheckinEntry',
    props: { blockParams: { type: Object, default: () => ({}) } },
    data() { return { checkedIn: false } },
    methods: {
        goCheckin() {
            uni.navigateTo({ url: '/pages/micro/staffHome/index?tab=checkin' })
        }
    }
}
</script>

<style scoped>
.block { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 20rpx; }
.checkin-card { display: flex; align-items: center; justify-content: space-between; }
.checkin-left { display: flex; align-items: center; }
.checkin-icon { font-size: 40rpx; margin-right: 16rpx; }
.checkin-text { display: flex; flex-direction: column; }
.checkin-title { font-size: 30rpx; font-weight: bold; }
.checkin-desc { font-size: 22rpx; color: #909399; margin-top: 4rpx; }
.checkin-btn { background: #409eff; color: #fff; font-size: 24rpx; padding: 10rpx 28rpx; border-radius: 24rpx; }
.checkin-btn.done { background: #67c23a; }
</style>
