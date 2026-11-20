<template>
    <view class="page">
        <view class="card">
            <view class="header">
                <text class="header-icon">🎙️</text>
                <text class="header-title">录音/录像知情告知书</text>
            </view>

            <view class="section">
                <text class="section-title">一、告知内容</text>
                <text class="section-body">尊敬的客户，感谢您光临本店。为了保障服务质量、维护双方合法权益，本店在服务过程中可能会进行录音和/或录像（以下简称"录音录像"）。请您仔细阅读以下内容，了解您的权利与义务。</text>
            </view>

            <view class="section">
                <text class="section-title">二、录音录像的目的</text>
                <text class="section-item">1. 服务质量监控与改进；</text>
                <text class="section-item">2. 纠纷处理与事实还原；</text>
                <text class="section-item">3. 员工培训与考核；</text>
                <text class="section-item">4. 法律法规要求的合规留存。</text>
            </view>

            <view class="section">
                <text class="section-title">三、信息保管与使用</text>
                <text class="section-item">1. 录音录像资料由本店专人保管，未经授权不得对外披露；</text>
                <text class="section-item">2. 保存期限不超过法律法规要求的必要期限；</text>
                <text class="section-item">3. 不会将录音录像用于与上述目的无关的商业用途。</text>
            </view>

            <view class="section">
                <text class="section-title">四、您的权利</text>
                <text class="section-item">1. 您有权知悉录音录像的存在及目的；</text>
                <text class="section-item">2. 您有权要求查阅涉及您本人的录音录像资料；</text>
                <text class="section-item">3. 您有权要求删除与您相关且超出保存期限的资料；</text>
                <text class="section-item">4. 如您不同意录音录像，可提前告知店员，我们将尊重您的意愿。</text>
            </view>

            <view class="section">
                <text class="section-title">五、同意与确认</text>
                <text class="section-body">您继续参与本店活动或接受服务，即视为您已知悉并同意上述录音录像安排。如您不同意，请在服务开始前明确告知工作人员。</text>
            </view>
        </view>

        <view class="agree-area">
            <button v-if="!agreed" class="btn-agree" @click="handleAgree">我已阅读并同意</button>
            <view v-else class="agreed-tip">
                <text class="agreed-icon">✅</text>
                <text class="agreed-text">您已确认知情同意</text>
            </view>
        </view>
    </view>
</template>

<script>
export default {
    data() {
        return {
            agreed: false
        }
    },
    onLoad() {
        this.agreed = !!uni.getStorageSync('voice_consent_agreed')
    },
    methods: {
        handleAgree() {
            uni.setStorageSync('voice_consent_agreed', true)
            this.agreed = true
            uni.showToast({ title: '已确认知情同意', icon: 'success' })
            // 延迟返回上一页
            setTimeout(() => { uni.navigateBack() }, 800)
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; min-height: 100vh; background: #f5f6f8; }
.card { background: #fff; border-radius: 16rpx; padding: 32rpx 28rpx; margin-bottom: 24rpx; }
.header { display: flex; align-items: center; justify-content: center; margin-bottom: 32rpx; padding-bottom: 24rpx; border-bottom: 1rpx solid #f0f2f5; }
.header-icon { font-size: 40rpx; margin-right: 12rpx; }
.header-title { font-size: 34rpx; font-weight: bold; color: #303133; }
.section { margin-bottom: 28rpx; }
.section-title { font-size: 28rpx; font-weight: bold; color: #303133; display: block; margin-bottom: 12rpx; }
.section-body { font-size: 26rpx; color: #606266; line-height: 1.8; display: block; }
.section-item { font-size: 26rpx; color: #606266; line-height: 1.8; display: block; padding-left: 16rpx; }
.agree-area { padding: 20rpx 0 60rpx; }
.btn-agree { background: #409eff; color: #fff; border-radius: 12rpx; font-size: 30rpx; height: 88rpx; line-height: 88rpx; }
.agreed-tip { display: flex; align-items: center; justify-content: center; padding: 24rpx 0; }
.agreed-icon { font-size: 32rpx; margin-right: 12rpx; }
.agreed-text { font-size: 30rpx; color: #67c23a; font-weight: bold; }
</style>
