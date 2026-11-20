<template>
    <view class="page" :style="pageStyle">
        <view class="title">升级为正式顾客</view>
        <view class="subtitle">完善以下信息，享受更多服务</view>

        <view class="card">
            <view class="form">
                <view class="form-item">
                    <text class="label">真实姓名 <text class="required">*</text></text>
                    <input class="input" v-model="form.realName" placeholder="请输入真实姓名" maxlength="50" />
                </view>
                <view class="form-item">
                    <text class="label">身份证号 <text class="required">*</text></text>
                    <input class="input" v-model="form.idCard" placeholder="请输入身份证号" maxlength="18" />
                </view>
                <view class="form-item">
                    <text class="label">生日</text>
                    <picker mode="date" :value="form.birthday" @change="onBirthdayChange">
                        <view class="picker-value">{{ form.birthday || '请选择生日' }}</view>
                    </picker>
                </view>
                <view class="form-item">
                    <text class="label">性别</text>
                    <view class="radio-group">
                        <view class="radio" :class="{ active: form.gender === 1 }" @click="form.gender = 1">男</view>
                        <view class="radio" :class="{ active: form.gender === 2 }" @click="form.gender = 2">女</view>
                    </view>
                </view>
                <view class="form-item">
                    <text class="label">地址</text>
                    <input class="input" v-model="form.address" placeholder="请输入地址（选填）" maxlength="200" />
                </view>
            </view>
        </view>

        <button class="btn" :loading="loading" @click="handleUpgrade">确认升级</button>

        <view class="tip">
            <text>升级后您的账号将转为正式顾客，临时游客信息将保留。</text>
        </view>
    </view>
</template>

<script>
import config from '../../../config'
import { post } from '../../../utils/request'
import themeMixin from '../../../utils/themeMixin'

export default {
    mixins: [themeMixin],
    data() {
        return {
            loading: false,
            form: {
                realName: '',
                idCard: '',
                birthday: '',
                gender: '',
                address: ''
            }
        }
    },
    methods: {
        onBirthdayChange(e) {
            this.form.birthday = e.detail.value
        },
        /** 校验身份证号格式 */
        validateIdCard(idCard) {
            return /(^\d{15}$)|(^\d{18}$)|(^\d{17}(\d|X|x)$)/.test(idCard)
        },
        /** 提交升级 */
        async handleUpgrade() {
            if (!this.form.realName) {
                uni.showToast({ title: '请输入真实姓名', icon: 'none' })
                return
            }
            if (!this.form.idCard) {
                uni.showToast({ title: '请输入身份证号', icon: 'none' })
                return
            }
            if (!this.validateIdCard(this.form.idCard)) {
                uni.showToast({ title: '身份证号格式不正确', icon: 'none' })
                return
            }
            this.loading = true
            try {
                await post(config.api.microVisitorUpgradeFormal, this.form)
                uni.showToast({ title: '升级成功！', icon: 'success' })
                setTimeout(() => {
                    uni.navigateBack()
                }, 1000)
            } catch (e) {
                // 错误已由 request 统一处理
            } finally {
                this.loading = false
            }
        }
    }
}
</script>

<style scoped>
.page { padding: 48rpx 36rpx; min-height: 100vh; background: var(--page-bg, #f5f6f8); }
.title { font-size: 44rpx; font-weight: bold; text-align: center; margin-bottom: 12rpx; color: var(--text, #303133); }
.subtitle { font-size: 26rpx; color: var(--text-sub, #909399); text-align: center; margin-bottom: 48rpx; }
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 36rpx; margin-bottom: 36rpx; box-shadow: var(--shadow-card, none); }
.form { display: flex; flex-direction: column; gap: 28rpx; }
.form-item { display: flex; flex-direction: column; gap: 12rpx; }
.label { font-size: 28rpx; color: var(--text, #303133); font-weight: 500; }
.required { color: #f56c6c; }
.input { background: #f5f7fa; border-radius: 12rpx; padding: 22rpx 20rpx; font-size: 28rpx; color: var(--text, #303133); }
.picker-value { background: #f5f7fa; border-radius: 12rpx; padding: 22rpx 20rpx; font-size: 28rpx; color: var(--text, #303133); }
.radio-group { display: flex; gap: 24rpx; }
.radio { padding: 16rpx 32rpx; border-radius: 12rpx; background: #f5f7fa; font-size: 28rpx; color: var(--text-sub, #909399); }
.radio.active { background: var(--brand, #409eff); color: #fff; }
.btn { background: var(--brand, #409eff); color: #fff; border-radius: 12rpx; font-size: 30rpx; margin-top: 12rpx; border: none; }
.tip { text-align: center; margin-top: 32rpx; font-size: 24rpx; color: var(--text-sub, #909399); }
</style>
