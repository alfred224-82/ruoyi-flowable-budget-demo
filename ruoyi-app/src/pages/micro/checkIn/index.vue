<template>
  <view class="container">
    <view class="header">
      <text class="title">员工打卡</text>
      <text class="subtitle">拍照 + 定位 + 水印</text>
    </view>

    <!-- 打卡状态 -->
    <view class="status-card" v-if="checkInEnabled !== null">
      <view v-if="!checkInEnabled" class="status-disabled">
        <text class="status-icon">🔒</text>
        <text class="status-text">打卡功能未开启</text>
        <text class="status-hint">请联系管理员开启打卡功能</text>
      </view>
      <view v-else class="status-enabled">
        <text class="status-icon">✅</text>
        <text class="status-text">打卡功能已开启</text>
      </view>
    </view>

    <!-- 选择活动 -->
    <view class="section" v-if="checkInEnabled">
      <text class="section-title">选择活动</text>
      <picker :range="activityNames" @change="onActivityChange" v-if="activityList.length > 0">
        <view class="picker-value">
          <text>{{ activityList.length > 0 ? activityNames[activityIndex] : '暂无活动' }}</text>
          <text class="arrow">▼</text>
        </view>
      </picker>
      <view v-else class="empty-hint">暂无可打卡的活动</view>
    </view>

    <!-- 拍照区域 -->
    <view class="section" v-if="checkInEnabled && activityList.length > 0">
      <text class="section-title">打卡照片</text>
      <view class="photo-area">
        <image v-if="photoUrl" :src="photoUrl" mode="aspectFill" class="photo-preview" @click="choosePhoto" />
        <view v-else class="photo-placeholder" @click="choosePhoto">
          <text class="photo-icon">📷</text>
          <text class="photo-hint">点击拍照</text>
        </view>
      </view>
    </view>

    <!-- 位置信息 -->
    <view class="section" v-if="checkInEnabled && activityList.length > 0">
      <text class="section-title">打卡位置</text>
      <view class="location-info" v-if="locationName">
        <text class="location-icon">📍</text>
        <text class="location-text">{{ locationName }}</text>
      </view>
      <view v-else class="location-btn" @click="getLocation">
        <text>获取当前位置</text>
      </view>
    </view>

    <!-- 提交按钮 -->
    <view class="submit-area" v-if="checkInEnabled && activityList.length > 0">
      <button class="submit-btn" :disabled="!canSubmit" :loading="submitting" @click="handleSubmit">
        {{ submitting ? '提交中...' : '打卡' }}
      </button>
    </view>

    <!-- 我的打卡记录 -->
    <view class="section" v-if="checkInEnabled">
      <text class="section-title">我的打卡记录</text>
      <view v-if="myRecords.length > 0">
        <view class="record-item" v-for="(record, idx) in myRecords" :key="idx">
          <view class="record-top">
            <text class="record-time">{{ record.checkInTime }}</text>
            <text class="record-late" :class="{ 'late': record.isLate === 1 }">
              {{ record.isLate === 1 ? '迟到' : '准时' }}
            </text>
            <text class="record-source" v-if="record.source === 'SUPPLEMENT'">补录</text>
          </view>
          <text class="record-location">{{ record.locationName || '未知位置' }}</text>
        </view>
      </view>
      <view v-else class="empty-hint">暂无打卡记录</view>
    </view>

    <!-- 打卡成功弹窗 -->
    <view class="modal-mask" v-if="showSuccess" @click="showSuccess = false">
      <view class="modal-content" @click.stop>
        <text class="modal-icon">🎉</text>
        <text class="modal-title">打卡成功</text>
        <text class="modal-info">时间：{{ lastResult.checkInTime }}</text>
        <text class="modal-info">地点：{{ lastResult.locationName }}</text>
        <text class="modal-info" :class="{ 'late-text': lastResult.isLate }">
          {{ lastResult.isLate ? '迟到' : '准时到岗' }}
        </text>
        <button class="modal-btn" @click="showSuccess = false">确定</button>
      </view>
    </view>
  </view>
</template>

<script>
import config from '@/config/index.js'
import { formatDateTime } from '@/utils/format.js'

export default {
  data() {
    return {
      checkInEnabled: null,
      activityList: [],
      activityNames: [],
      activityIndex: 0,
      photoUrl: '',
      locationName: '',
      locationLat: null,
      locationLng: null,
      submitting: false,
      myRecords: [],
      showSuccess: false,
      lastResult: {}
    }
  },
  computed: {
    canSubmit() {
      return this.activityList.length > 0 && this.photoUrl && this.locationName
    }
  },
  onShow() {
    this.checkEnabled()
    this.loadMyRecords()
  },
  methods: {
    checkEnabled() {
      uni.request({
        url: getApp().globalData.baseUrl + config.api.microCheckInEnabled,
        header: { 'Authorization': 'Bearer ' + uni.getStorageSync('token') },
        success: (res) => {
          if (res.data && res.data.code === 200) {
            this.checkInEnabled = res.data.data
            if (this.checkInEnabled) {
              this.loadActivities()
            }
          }
        },
        fail: () => {
          this.checkInEnabled = false
        }
      })
    },
    loadActivities() {
      // 加载我参与的活动（复用已有 API）
      const store = uni.getStorageSync('microActivities')
      if (store) {
        try {
          const list = JSON.parse(store)
          this.activityList = list.filter(a => a.status !== 'stopped')
          this.activityNames = this.activityList.map(a => a.title || '活动#' + a.id)
        } catch (e) {
          this.activityList = []
          this.activityNames = []
        }
      }
    },
    onActivityChange(e) {
      this.activityIndex = parseInt(e.detail.value)
    },
    choosePhoto() {
      uni.chooseImage({
        count: 1,
        sourceType: ['camera'],
        success: (res) => {
          const tempPath = res.tempFilePaths[0]
          // 上传到服务器
          uni.uploadFile({
            url: getApp().globalData.baseUrl + '/resource/upload',
            filePath: tempPath,
            name: 'file',
            header: { 'Authorization': 'Bearer ' + uni.getStorageSync('token') },
            success: (uploadRes) => {
              const data = JSON.parse(uploadRes.data)
              if (data.code === 200) {
                this.photoUrl = data.data.url
              } else {
                uni.showToast({ title: '照片上传失败', icon: 'none' })
              }
            },
            fail: () => {
              uni.showToast({ title: '照片上传失败', icon: 'none' })
            }
          })
        }
      })
    },
    getLocation() {
      uni.getLocation({
        type: 'gcj02',
        success: (res) => {
          this.locationLat = res.latitude
          this.locationLng = res.longitude
          // 逆地理编码（简化处理，使用坐标作为位置名）
          this.locationName = `纬度${res.latitude.toFixed(6)}, 经度${res.longitude.toFixed(6)}`
        },
        fail: () => {
          uni.showToast({ title: '获取位置失败，请授权位置权限', icon: 'none' })
        }
      })
    },
    handleSubmit() {
      if (!this.canSubmit) return
      this.submitting = true
      const activity = this.activityList[this.activityIndex]
      const now = new Date()
      const watermarkText = formatDateTime(now) + ' | ' + this.locationName

      uni.request({
        url: getApp().globalData.baseUrl + config.api.microCheckIn,
        method: 'POST',
        header: {
          'Authorization': 'Bearer ' + uni.getStorageSync('token'),
          'Content-Type': 'application/json'
        },
        data: {
          activityId: activity.id,
          photoUrl: this.photoUrl,
          locationName: this.locationName,
          locationLat: this.locationLat,
          locationLng: this.locationLng,
          watermarkText: watermarkText
        },
        success: (res) => {
          if (res.data && res.data.code === 200) {
            this.lastResult = {
              checkInTime: formatDateTime(now),
              locationName: this.locationName,
              isLate: 0
            }
            this.showSuccess = true
            // 重置表单
            this.photoUrl = ''
            this.locationName = ''
            this.loadMyRecords()
          } else {
            uni.showToast({ title: res.data.msg || '打卡失败', icon: 'none' })
          }
        },
        fail: () => {
          uni.showToast({ title: '网络错误', icon: 'none' })
        },
        complete: () => {
          this.submitting = false
        }
      })
    },
    loadMyRecords() {
      uni.request({
        url: getApp().globalData.baseUrl + config.api.microCheckInMy,
        header: { 'Authorization': 'Bearer ' + uni.getStorageSync('token') },
        success: (res) => {
          if (res.data && res.data.code === 200) {
            this.myRecords = (res.data.data || []).slice(0, 10)
          }
        }
      })
    }
  }
}
</script>

<style scoped>
.container { padding: 24rpx; background: #f5f6f8; min-height: 100vh; }
.header { text-align: center; padding: 30rpx 0; }
.title { font-size: 40rpx; font-weight: bold; display: block; }
.subtitle { font-size: 26rpx; color: #999; display: block; margin-top: 8rpx; }
.status-card { background: #fff; border-radius: 16rpx; padding: 30rpx; margin-bottom: 24rpx; text-align: center; }
.status-disabled { color: #e6a23c; }
.status-enabled { color: #67c23a; }
.status-icon { font-size: 60rpx; display: block; }
.status-text { font-size: 30rpx; font-weight: bold; display: block; margin-top: 12rpx; }
.status-hint { font-size: 24rpx; color: #999; display: block; margin-top: 8rpx; }
.section { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 24rpx; }
.section-title { font-size: 28rpx; font-weight: bold; display: block; margin-bottom: 16rpx; }
.picker-value { display: flex; justify-content: space-between; align-items: center; padding: 16rpx; background: #f5f6f8; border-radius: 8rpx; }
.arrow { font-size: 24rpx; color: #999; }
.photo-area { text-align: center; }
.photo-preview { width: 300rpx; height: 300rpx; border-radius: 12rpx; }
.photo-placeholder { width: 300rpx; height: 300rpx; background: #f5f6f8; border-radius: 12rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; margin: 0 auto; }
.photo-icon { font-size: 80rpx; }
.photo-hint { font-size: 24rpx; color: #999; margin-top: 12rpx; }
.location-info { display: flex; align-items: center; padding: 16rpx; background: #f0f9eb; border-radius: 8rpx; }
.location-icon { margin-right: 8rpx; }
.location-text { font-size: 26rpx; color: #67c23a; }
.location-btn { padding: 16rpx; background: #ecf5ff; border-radius: 8rpx; text-align: center; color: #409eff; font-size: 26rpx; }
.submit-area { margin-bottom: 24rpx; }
.submit-btn { background: #409eff; color: #fff; border-radius: 12rpx; font-size: 32rpx; height: 88rpx; line-height: 88rpx; }
.submit-btn[disabled] { background: #c0c4cc; }
.record-item { padding: 16rpx 0; border-bottom: 1rpx solid #f0f0f0; }
.record-top { display: flex; align-items: center; }
.record-time { font-size: 26rpx; color: #333; flex: 1; }
.record-late { font-size: 22rpx; color: #67c23a; background: #f0f9eb; padding: 2rpx 12rpx; border-radius: 6rpx; }
.record-late.late { color: #f56c6c; background: #fef0f0; }
.record-source { font-size: 22rpx; color: #e6a23c; background: #fdf6ec; padding: 2rpx 12rpx; border-radius: 6rpx; margin-left: 8rpx; }
.record-location { font-size: 24rpx; color: #999; display: block; margin-top: 6rpx; }
.empty-hint { text-align: center; color: #999; font-size: 26rpx; padding: 30rpx; }
.modal-mask { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center; z-index: 999; }
.modal-content { background: #fff; border-radius: 20rpx; padding: 40rpx; width: 560rpx; text-align: center; }
.modal-icon { font-size: 80rpx; display: block; }
.modal-title { font-size: 34rpx; font-weight: bold; display: block; margin: 16rpx 0; }
.modal-info { font-size: 26rpx; color: #666; display: block; margin: 8rpx 0; }
.late-text { color: #f56c6c; }
.modal-btn { margin-top: 24rpx; background: #409eff; color: #fff; border-radius: 8rpx; font-size: 28rpx; }
</style>
