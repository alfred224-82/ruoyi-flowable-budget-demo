<template>
    <view class="page">
        <view class="card">
            <text class="card-title">营业时间</text>
            <view class="form-item">
                <text class="form-label">营业时间配置</text>
                <textarea class="form-textarea" v-model="form.businessHours" placeholder="如：周一至周日 9:00-21:00" />
            </view>
        </view>
        <view class="card">
            <text class="card-title">收款方式说明</text>
            <view class="form-item">
                <text class="form-label">到店付款提示文案</text>
                <textarea class="form-textarea" v-model="form.payInfo" placeholder="如：支持现金/微信/支付宝付款" />
            </view>
        </view>
        <button class="btn-save" :loading="saving" @click="save">保存配置</button>
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get, put } from '../../../utils/request'
import config from '../../../config'

export default {
    data() { return { saving: false, form: { businessHours: '', payInfo: '' } } },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        this.loadConfig()
    },
    methods: {
        async loadConfig() {
            try {
                const res = await get(config.api.microShopConfig)
                const data = (res && res.data) || {}
                this.form.businessHours = data.businessHours || ''
                this.form.payInfo = data.payInfo || ''
            } catch (e) { /* 已统一提示 */ }
        },
        async save() {
            this.saving = true
            try {
                await put(config.api.microShopConfig, this.form)
                uni.showToast({ title: '保存成功', icon: 'success' })
            } catch (e) { /* 已统一提示 */ } finally { this.saving = false }
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 32rpx; margin-bottom: 24rpx; }
.card-title { font-size: 30rpx; font-weight: bold; display: block; margin-bottom: 20rpx; }
.form-item { margin-bottom: 16rpx; }
.form-label { font-size: 26rpx; color: #606266; display: block; margin-bottom: 12rpx; }
.form-textarea { background: #f5f7fa; border-radius: 8rpx; padding: 20rpx; font-size: 28rpx; width: 100%; height: 160rpx; box-sizing: border-box; }
.btn-save { background: #409eff; color: #fff; border-radius: 12rpx; margin-top: 20rpx; font-size: 30rpx; }
</style>
