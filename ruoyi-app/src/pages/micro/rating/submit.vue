<template>
    <view class="page">
        <view class="card">
            <text class="card-title">服务评价</text>
            <text class="sub-title">为服务人员打分</text>
            <!-- 星级选择 -->
            <view class="star-row">
                <text v-for="n in 5" :key="n" class="star" :class="{ active: n <= rating }" @click="rating = n">★</text>
            </view>
            <text class="star-label">{{ starLabels[rating - 1] || '请选择评分' }}</text>

            <!-- 评价内容 -->
            <view class="input-area">
                <textarea v-model="content" placeholder="请输入评价内容（至少1字）" :maxlength="500" />
            </view>

            <!-- 场景选择 -->
            <view class="scene-row">
                <text class="scene-label">评价场景：</text>
                <text class="scene-tag" :class="{ active: scene === 'booking_complete' }" @click="scene = 'booking_complete'">预约完成</text>
                <text class="scene-tag" :class="{ active: scene === 'service_done' }" @click="scene = 'service_done'">服务完成</text>
                <text class="scene-tag" :class="{ active: scene === 'visit_done' }" @click="scene = 'visit_done'">到店完成</text>
            </view>

            <button class="btn-submit" :disabled="!canSubmit" @click="submitRating">提交评价</button>
        </view>
    </view>
</template>

<script>
import { post } from '../../../utils/request'
import config from '../../../config'

export default {
    data() {
        return {
            rating: 5,
            content: '',
            scene: 'booking_complete',
            targetUserId: null,
            orderId: null,
            starLabels: ['很差', '较差', '一般', '满意', '非常满意']
        }
    },
    computed: {
        canSubmit() {
            return this.rating >= 1 && this.rating <= 5 && this.content.trim().length > 0 && this.targetUserId
        }
    },
    onLoad(options) {
        // ★雪花ID超过 JS 安全整数精度，保持字符串避免 Number() 丢精度（后端 Jackson 支持 string→Long）
        this.targetUserId = options.targetUserId || null
        this.orderId = options.orderId || null
        if (options.scene) this.scene = options.scene
    },
    methods: {
        async submitRating() {
            if (!this.canSubmit) return
            try {
                await post(config.api.microRatingSubmit, {
                    targetUserId: this.targetUserId,
                    rating: this.rating,
                    content: this.content.trim(),
                    scene: this.scene,
                    orderId: this.orderId
                })
                uni.showToast({ title: '评价成功', icon: 'success' })
                setTimeout(() => uni.navigateBack(), 1500)
            } catch (e) {
                uni.showToast({ title: (e && e.msg) || '评价失败', icon: 'none' })
            }
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; min-height: 100vh; background: #f5f6f8; }
.card { background: #fff; border-radius: 16rpx; padding: 32rpx; }
.card-title { font-size: 32rpx; font-weight: bold; display: block; margin-bottom: 8rpx; }
.sub-title { font-size: 26rpx; color: #909399; display: block; margin-bottom: 24rpx; }
.star-row { display: flex; justify-content: center; margin-bottom: 12rpx; }
.star { font-size: 56rpx; color: #ddd; margin: 0 12rpx; }
.star.active { color: #f7ba2a; }
.star-label { text-align: center; display: block; font-size: 26rpx; color: #606266; margin-bottom: 32rpx; }
.input-area { margin-bottom: 24rpx; }
.input-area textarea { width: 100%; height: 200rpx; border: 1rpx solid #dcdfe6; border-radius: 8rpx; padding: 16rpx; font-size: 28rpx; box-sizing: border-box; }
.scene-row { display: flex; align-items: center; flex-wrap: wrap; margin-bottom: 32rpx; }
.scene-label { font-size: 26rpx; color: #606266; margin-right: 16rpx; }
.scene-tag { font-size: 24rpx; padding: 8rpx 20rpx; border-radius: 8rpx; background: #f0f2f5; color: #606266; margin-right: 12rpx; }
.scene-tag.active { background: #409eff; color: #fff; }
.btn-submit { background: #409eff; color: #fff; border-radius: 12rpx; font-size: 30rpx; margin-top: 16rpx; }
.btn-submit[disabled] { background: #c0c4cc; }
</style>
