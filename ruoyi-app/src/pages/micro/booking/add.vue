<template>
    <view class="page">
        <view class="card">
            <text class="card-title">新建预约</text>
            <view class="form">
                <view class="form-item">
                    <text class="form-label">顾客姓名</text>
                    <input class="form-input" v-model="form.customerName" placeholder="请输入顾客姓名" />
                </view>
                <view class="form-item">
                    <text class="form-label">顾客手机号</text>
                    <input class="form-input" v-model="form.customerPhone" placeholder="请输入手机号" type="number" />
                </view>
                <view class="form-item">
                    <text class="form-label">服务项目</text>
                    <picker :range="serviceNames" @change="onServiceChange">
                        <view class="form-picker">{{ form.serviceItemId ? serviceNames[serviceIndex] : '请选择服务项目' }}</view>
                    </picker>
                </view>
                <view class="form-item">
                    <text class="form-label">预约时间</text>
                    <picker mode="date" :start="today" @change="onDateChange">
                        <view class="form-picker">{{ form.bookingTime || '请选择日期' }}</view>
                    </picker>
                </view>
                <view class="form-item">
                    <text class="form-label">金额（元）</text>
                    <input class="form-input" v-model="form.amount" placeholder="0.00" type="digit" />
                </view>
                <view class="form-item">
                    <text class="form-label">备注</text>
                    <input class="form-input" v-model="form.remark" placeholder="可选" />
                </view>
            </view>
            <button class="btn-submit" :loading="submitting" @click="submit">提交预约</button>
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
            form: { customerName: '', customerPhone: '', serviceItemId: null, bookingTime: '', amount: '', remark: '' },
            services: [],
            serviceIndex: 0
        }
    },
    computed: {
        serviceNames() { return this.services.map(s => s.itemName || s.name || '-') }
    },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        this.loadServices()
    },
    methods: {
        async loadServices() {
            try {
                const res = await get(config.api.microServiceOnSale)
                this.services = (res && res.data) || []
            } catch (e) { this.services = [] }
        },
        onServiceChange(e) {
            this.serviceIndex = e.detail.value
            const s = this.services[this.serviceIndex]
            if (s) {
                this.form.serviceItemId = s.id
                if (!this.form.amount) this.form.amount = s.price || ''
            }
        },
        onDateChange(e) { this.form.bookingTime = e.detail.value },
        async submit() {
            if (!this.form.customerName) { uni.showToast({ title: '请输入顾客姓名', icon: 'none' }); return }
            if (!this.form.bookingTime) { uni.showToast({ title: '请选择预约时间', icon: 'none' }); return }
            this.submitting = true
            try {
                await post(config.api.microBookingAdd, this.form)
                uni.showToast({ title: '预约成功', icon: 'success' })
                setTimeout(() => uni.navigateBack(), 1500)
            } catch (e) { /* 已统一提示 */ } finally { this.submitting = false }
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 32rpx; }
.card-title { font-size: 32rpx; font-weight: bold; display: block; margin-bottom: 32rpx; }
.form-item { margin-bottom: 28rpx; }
.form-label { font-size: 26rpx; color: #606266; display: block; margin-bottom: 12rpx; }
.form-input { background: #f5f7fa; border-radius: 8rpx; padding: 20rpx 24rpx; font-size: 28rpx; }
.form-picker { background: #f5f7fa; border-radius: 8rpx; padding: 20rpx 24rpx; font-size: 28rpx; color: #606266; }
.btn-submit { background: #409eff; color: #fff; border-radius: 12rpx; margin-top: 20rpx; font-size: 30rpx; }
</style>
