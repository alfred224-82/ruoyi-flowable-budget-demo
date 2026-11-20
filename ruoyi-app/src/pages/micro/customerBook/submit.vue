<template>
    <view class="page">
        <view class="card">
            <text class="card-title">提交预约</text>
            <view class="selected-service" v-if="serviceName">
                <text class="label">已选服务</text>
                <text class="value">{{ serviceName }} · ¥{{ price }}</text>
            </view>
            <view class="form">
                <view class="form-item">
                    <text class="form-label">您的姓名</text>
                    <input class="form-input" v-model="form.customerName" placeholder="请输入姓名" />
                </view>
                <view class="form-item">
                    <text class="form-label">手机号</text>
                    <input class="form-input" v-model="form.customerPhone" placeholder="请输入手机号" type="number" />
                </view>
                <view class="form-item">
                    <text class="form-label">预约日期</text>
                    <picker mode="date" :start="today" @change="onDateChange">
                        <view class="form-picker">{{ form.bookingTime || '请选择日期' }}</view>
                    </picker>
                </view>
                <view class="form-item">
                    <text class="form-label">备注</text>
                    <input class="form-input" v-model="form.remark" placeholder="可选" />
                </view>
            </view>
            <view class="pay-notice">
                <text class="notice-text">★ 本店支持到店付款，本小程序不提供在线支付</text>
            </view>
            <!-- T15.32 防爽约：黑名单警告横幅（仅 micro_pro 拉黑顾客可见） -->
            <view class="credit-block" v-if="creditBlocked">
                <text class="credit-block-text">⚠ {{ creditMessage }}</text>
            </view>
            <button class="btn-submit" :class="{ 'btn-disabled': creditBlocked }" :disabled="creditBlocked"
                    :loading="submitting" @click="submit">
                {{ creditBlocked ? '已被限制预约' : '提交预约' }}
            </button>
        </view>
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get, post } from '../../../utils/request'
import config from '../../../config'
import { today } from '../../../utils/format'

export default {
    data() {
        return {
            submitting: false,
            today: today(),
            serviceName: '',
            price: 0,
            // T15.32 防爽约：黑名单状态（onLoad 查询 myCredit 回填）
            creditBlocked: false,
            creditMessage: '',
            form: { serviceItemId: null, customerName: '', customerPhone: '', bookingTime: '', remark: '' }
        }
    },
    onLoad(options) {
        // ★ID 保持字符串（雪花ID用 Number() 会丢精度；后端 Jackson 支持 string→Long）
        this.form.serviceItemId = options.serviceId || null
        this.serviceName = decodeURIComponent(options.serviceName || '')
        this.price = options.price || 0
        this.checkCredit()
    },
    methods: {
        onDateChange(e) { this.form.bookingTime = e.detail.value },
        /** T15.32 查询当前顾客信誉状态：拉黑则按钮置灰禁止提交（后端 submitBook 二次校验兼底） */
        async checkCredit() {
            try {
                const res = await get(config.api.microCustomerMyCredit)
                const data = res.data || {}
                this.creditBlocked = !!data.blocked
                this.creditMessage = data.message || ''
            } catch (e) {
                // 查询失败不阻断预约（降级：交由后端 submitBook 校验）
                this.creditBlocked = false
            }
        },
        async submit() {
            // T15.32 黑名单前置拦截（与按钮 disabled 双重保障）
            if (this.creditBlocked) {
                uni.showToast({ title: this.creditMessage || '您已被限制预约', icon: 'none' })
                return
            }
            if (!this.form.customerName) { uni.showToast({ title: '请输入姓名', icon: 'none' }); return }
            if (!this.form.customerPhone) { uni.showToast({ title: '请输入手机号', icon: 'none' }); return }
            if (!this.form.bookingTime) { uni.showToast({ title: '请选择预约日期', icon: 'none' }); return }
            this.submitting = true
            try {
                await post(config.api.microCustomerSubmitBook, this.form)
                uni.showToast({ title: '预约成功', icon: 'success' })
                setTimeout(() => uni.switchTab({ url: '/pages/micro/customerBook/list' }), 1500)
            } catch (e) { /* 已统一提示 */ } finally { this.submitting = false }
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 32rpx; }
.card-title { font-size: 32rpx; font-weight: bold; display: block; margin-bottom: 24rpx; }
.selected-service { background: #ecf5ff; border-radius: 8rpx; padding: 16rpx 20rpx; margin-bottom: 24rpx; display: flex; justify-content: space-between; }
.selected-service .label { font-size: 26rpx; color: #606266; }
.selected-service .value { font-size: 26rpx; color: #409eff; font-weight: bold; }
.form-item { margin-bottom: 28rpx; }
.form-label { font-size: 26rpx; color: #606266; display: block; margin-bottom: 12rpx; }
.form-input { background: #f5f7fa; border-radius: 8rpx; padding: 20rpx 24rpx; font-size: 28rpx; }
.form-picker { background: #f5f7fa; border-radius: 8rpx; padding: 20rpx 24rpx; font-size: 28rpx; color: #606266; }
.pay-notice { background: #fdf6ec; border-radius: 8rpx; padding: 16rpx 20rpx; margin-bottom: 20rpx; }
.notice-text { font-size: 24rpx; color: #e6a23c; }
.btn-submit { background: #409eff; color: #fff; border-radius: 12rpx; margin-top: 12rpx; font-size: 30rpx; }
.btn-submit.btn-disabled { background: #c0c4cc; color: #fff; }
.credit-block { background: #fef0f0; border-radius: 8rpx; padding: 16rpx 20rpx; margin-bottom: 12rpx; }
.credit-block-text { font-size: 24rpx; color: #f56c6c; }
</style>
