<template>
  <view class="container">
    <view class="header">
      <text class="title">合规协议签署</text>
      <text class="subtitle">请阅读并签署合规协议</text>
    </view>

    <view class="section" v-if="agreementInfo">
      <view class="info-row">
        <text class="info-label">协议版本</text>
        <text class="info-value">{{ agreementInfo.currentVersion }}</text>
      </view>
      <view class="info-row" v-if="agreementInfo.agreementContentUrl">
        <text class="info-label">协议内容</text>
        <text class="info-link" @click="viewAgreement">查看协议全文</text>
      </view>
    </view>

    <!-- 勾选同意 -->
    <view class="agree-row">
      <checkbox :checked="agreed" @click="agreed = !agreed" />
      <text class="agree-text">我已阅读并同意以上合规协议</text>
    </view>

    <!-- 签署按钮 -->
    <view class="submit-area">
      <button class="submit-btn" :disabled="!agreed" :loading="submitting" @click="handleSign">
        {{ submitting ? '签署中...' : '确认签署' }}
      </button>
    </view>

    <!-- 已签署状态 -->
    <view class="signed-card" v-if="!agreementInfo.needSign && agreementInfo.signTime">
      <text class="signed-icon">✅</text>
      <text class="signed-text">您已签署本协议</text>
      <text class="signed-info">签署时间：{{ agreementInfo.signTime }}</text>
      <text class="signed-info">签署人：{{ agreementInfo.signerName }}</text>
    </view>
  </view>
</template>

<script>
import config from '@/config/index.js'

export default {
  data() {
    return {
      agreementInfo: null,
      agreed: false,
      submitting: false
    }
  },
  onLoad() {
    this.loadCurrent()
  },
  methods: {
    loadCurrent() {
      uni.request({
        url: getApp().globalData.baseUrl + config.api.microComplianceAgreementCurrent,
        header: { 'Authorization': 'Bearer ' + uni.getStorageSync('token') },
        success: (res) => {
          if (res.data && res.data.code === 200) {
            this.agreementInfo = res.data.data
            if (!this.agreementInfo.needSign) {
              // 已签署，2秒后自动返回
              setTimeout(() => {
                uni.navigateBack()
              }, 2000)
            }
          }
        }
      })
    },
    viewAgreement() {
      if (this.agreementInfo && this.agreementInfo.agreementContentUrl) {
        // OSS 绝对地址直接用，相对路径才补 baseUrl
        const raw = this.agreementInfo.agreementContentUrl
        const full = /^https?:\/\//.test(raw) ? raw : getApp().globalData.baseUrl + raw
        uni.navigateTo({
          url: '/pages/webview/index?url=' + encodeURIComponent(full)
        })
      }
    },
    handleSign() {
      if (!this.agreed) return
      this.submitting = true
      // 后端 @RequestParam 接收：版本号走 query 参数，不能放 JSON body
      uni.request({
        url: getApp().globalData.baseUrl + config.api.microComplianceAgreementSign +
          '?agreementVersion=' + encodeURIComponent(this.agreementInfo.currentVersion),
        method: 'POST',
        header: {
          'Authorization': 'Bearer ' + uni.getStorageSync('token')
        },
        success: (res) => {
          if (res.data && res.data.code === 200) {
            uni.showToast({ title: '签署成功', icon: 'success' })
            setTimeout(() => {
              uni.navigateBack()
            }, 1500)
          } else {
            uni.showToast({ title: res.data.msg || '签署失败', icon: 'none' })
          }
        },
        fail: () => {
          uni.showToast({ title: '网络错误', icon: 'none' })
        },
        complete: () => {
          this.submitting = false
        }
      })
    }
  }
}
</script>

<style scoped>
.container { padding: 24rpx; background: #f5f6f8; min-height: 100vh; }
.header { text-align: center; padding: 40rpx 0; }
.title { font-size: 40rpx; font-weight: bold; display: block; }
.subtitle { font-size: 26rpx; color: #999; display: block; margin-top: 8rpx; }
.section { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 24rpx; }
.info-row { display: flex; justify-content: space-between; align-items: center; padding: 16rpx 0; border-bottom: 1rpx solid #f0f0f0; }
.info-label { font-size: 28rpx; color: #666; }
.info-value { font-size: 28rpx; color: #333; font-weight: bold; }
.info-link { font-size: 26rpx; color: #409eff; }
.agree-row { display: flex; align-items: center; padding: 24rpx; }
.agree-text { font-size: 28rpx; color: #333; margin-left: 12rpx; }
.submit-area { margin-bottom: 24rpx; padding: 0 24rpx; }
.submit-btn { background: #409eff; color: #fff; border-radius: 12rpx; font-size: 32rpx; height: 88rpx; line-height: 88rpx; }
.submit-btn[disabled] { background: #c0c4cc; }
.signed-card { background: #f0f9eb; border-radius: 16rpx; padding: 30rpx; text-align: center; margin: 24rpx; }
.signed-icon { font-size: 60rpx; display: block; }
.signed-text { font-size: 30rpx; font-weight: bold; color: #67c23a; display: block; margin-top: 12rpx; }
.signed-info { font-size: 24rpx; color: #999; display: block; margin-top: 8rpx; }
</style>
