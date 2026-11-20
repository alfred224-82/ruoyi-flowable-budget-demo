<template>
  <view class="container">
    <view class="header">
      <text class="title">投诉/反馈</text>
      <text class="subtitle">选择渠道提交您的投诉或反馈</text>
    </view>

    <!-- 选择渠道 -->
    <view class="section">
      <text class="section-title">投诉渠道</text>
      <view class="channel-tabs">
        <view class="channel-tab" :class="{ active: channel === 'BOSS' }" @click="channel = 'BOSS'">
          <text class="tab-icon">🏪</text>
          <text class="tab-label">Boss 门店</text>
          <text class="tab-desc">门店内部问题反馈</text>
        </view>
        <view class="channel-tab" :class="{ active: channel === 'PLATFORM' }" @click="channel = 'PLATFORM'">
          <text class="tab-icon">️</text>
          <text class="tab-label">平台 Admin</text>
          <text class="tab-desc">违法违规举报</text>
        </view>
      </view>
    </view>

    <!-- 渠道二额外字段 -->
    <view class="section" v-if="channel === 'PLATFORM'">
      <text class="section-title">投诉类别 <text class="required">*</text></text>
      <picker :range="categories" @change="onCategoryChange">
        <view class="picker-value">
          <text>{{ category || '请选择类别' }}</text>
          <text class="arrow">▼</text>
        </view>
      </picker>
    </view>

    <!-- 关联活动 -->
    <view class="section">
      <text class="section-title">关联活动（选填）</text>
      <picker :range="activityNames" @change="onActivityChange" v-if="activityList.length > 0">
        <view class="picker-value">
          <text>{{ selectedActivityId ? activityNames[activityIndex] : '不关联活动' }}</text>
          <text class="arrow">▼</text>
        </view>
      </picker>
      <text v-else class="hint-text">暂无可关联活动</text>
    </view>

    <!-- 投诉员工 -->
    <view class="section">
      <text class="section-title">投诉员工（选填）</text>
      <picker :range="staffNames" @change="onStaffChange" v-if="staffList.length > 0">
        <view class="picker-value">
          <text>{{ selectedStaffId ? staffNames[staffIndex] : '不指定员工' }}</text>
          <text class="arrow">▼</text>
        </view>
      </picker>
      <text v-else class="hint-text">暂无员工数据</text>
    </view>

    <!-- 投诉人信息 -->
    <view class="section">
      <text class="section-title">您的信息（选填）</text>
      <input class="input-field" v-model="complainantName" placeholder="姓名" />
      <input class="input-field" v-model="complainantPhone" placeholder="手机号" type="number" maxlength="11" />
    </view>

    <!-- 投诉内容 -->
    <view class="section">
      <text class="section-title">投诉内容 <text class="required">*</text></text>
      <textarea class="textarea-field" v-model="contentText" :placeholder="contentPlaceholder" :maxlength="500" />
      <text class="char-count" v-if="channel === 'PLATFORM'">{{ contentText.length }}/20字（最少）</text>
    </view>

    <!-- 渠道一：附加图片（可选，≤3张，可相册） -->
    <view class="section" v-if="channel === 'BOSS'">
      <text class="section-title">附加图片（选填，最多3张）</text>
      <view class="photo-grid">
        <view class="photo-item" v-for="(photo, idx) in photoList" :key="idx">
          <image :src="photo" mode="aspectFill" class="photo-img" />
          <view class="photo-remove" @click="removePhoto(idx)">×</view>
        </view>
        <view class="photo-add" v-if="photoList.length < 3" @click="addPhoto">
          <text class="add-icon">+</text>
        </view>
      </view>
    </view>

    <!-- 渠道二：取证照片（必填≥1张，仅摄像头） -->
    <view class="section" v-if="channel === 'PLATFORM'">
      <text class="section-title">取证照片 <text class="required">*</text>（仅摄像头拍摄）</text>
      <view class="photo-grid">
        <view class="photo-item" v-for="(photo, idx) in evidenceList" :key="idx">
          <image :src="photo" mode="aspectFill" class="photo-img" />
          <view class="photo-remove" @click="removeEvidence(idx)">×</view>
        </view>
        <view class="photo-add camera-only" @click="addEvidence">
          <text class="add-icon">📷</text>
          <text class="add-hint">拍照</text>
        </view>
      </view>
    </view>

    <!-- 提交按钮 -->
    <view class="submit-area">
      <button class="submit-btn" :disabled="!canSubmit" :loading="submitting" @click="handleSubmit">
        {{ submitting ? '提交中...' : '提交投诉' }}
      </button>
    </view>

    <!-- 我的投诉记录 -->
    <view class="section">
      <text class="section-title">我的投诉记录</text>
      <view v-if="myComplaints.length > 0">
        <view class="complaint-item" v-for="(item, idx) in myComplaints" :key="idx">
          <view class="complaint-top">
            <text class="complaint-channel">{{ item.complaintChannel === 'BOSS' ? 'Boss门店' : '平台Admin' }}</text>
            <text class="complaint-status" :class="'status-' + item.status">{{ statusLabel(item.status) }}</text>
          </view>
          <text class="complaint-content">{{ item.contentText }}</text>
          <text class="complaint-time">{{ item.submitTime }}</text>
          <view v-if="item.handlerReply" class="complaint-reply">
            <text class="reply-label">处理回复：</text>
            <text class="reply-text">{{ item.handlerReply }}</text>
          </view>
        </view>
      </view>
      <view v-else class="empty-hint">暂无投诉记录</view>
    </view>
  </view>
</template>

<script>
import config from '@/config/index.js'

export default {
  data() {
    return {
      channel: 'BOSS',
      category: '',
      categories: ['黄赌毒', '违法宣传', '违规活动', '其他'],
      activityList: [],
      activityNames: [],
      activityIndex: 0,
      selectedActivityId: null,
      staffList: [],
      staffNames: [],
      staffIndex: 0,
      selectedStaffId: null,
      selectedStaffName: '',
      complainantName: '',
      complainantPhone: '',
      contentText: '',
      photoList: [],
      evidenceList: [],
      submitting: false,
      myComplaints: []
    }
  },
  computed: {
    canSubmit() {
      if (!this.contentText.trim()) return false
      if (this.channel === 'PLATFORM') {
        if (this.contentText.trim().length < 20) return false
        if (!this.category) return false
        if (this.evidenceList.length === 0) return false
      }
      return true
    },
    contentPlaceholder() {
      return this.channel === 'PLATFORM'
        ? '请详细描述投诉内容（不少于20字）...'
        : '请描述您的问题或反馈...'
    }
  },
  onShow() {
    this.loadMyComplaints()
    this.loadActivities()
    this.loadStaff()
  },
  methods: {
    loadActivities() {
      // ★改用 API 加载顾客参与的全部活动（不限状态），不再依赖本地缓存
      uni.request({
        url: getApp().globalData.baseUrl + config.api.microCustomerMyActivities,
        header: { 'Authorization': 'Bearer ' + uni.getStorageSync('token') },
        success: (res) => {
          if (res.data && res.data.code === 200) {
            const list = res.data.data || []
            // 过滤：仅展示 ACTIVITY_SIGNUP 类型的活动（排除自助预约 BOOKING）
            this.activityList = list.filter(a => a.sourceType === 'ACTIVITY_SIGNUP')
            this.activityNames = this.activityList.map(a => a.title || '活动#' + a.bizId)
          }
        },
        fail: () => { this.activityList = [] }
      })
    },
    loadStaff() {
      uni.request({
        url: getApp().globalData.baseUrl + config.api.microComplaintStaffOptions,
        header: { 'Authorization': 'Bearer ' + uni.getStorageSync('token') },
        success: (res) => {
          if (res.data && res.data.code === 200) {
            this.staffList = res.data.data || []
            this.staffNames = this.staffList.map(s => s.name || '员工#' + s.id)
          }
        },
        fail: () => { this.staffList = [] }
      })
    },
    onCategoryChange(e) {
      this.category = this.categories[parseInt(e.detail.value)]
    },
    onActivityChange(e) {
      this.activityIndex = parseInt(e.detail.value)
      this.selectedActivityId = this.activityList[this.activityIndex].bizId
    },
    onStaffChange(e) {
      this.staffIndex = parseInt(e.detail.value)
      this.selectedStaffId = this.staffList[this.staffIndex].id
      this.selectedStaffName = this.staffList[this.staffIndex].name
    },
    addPhoto() {
      uni.chooseImage({
        count: 3 - this.photoList.length,
        sourceType: ['album', 'camera'],
        success: (res) => {
          this.photoList = this.photoList.concat(res.tempFilePaths)
        }
      })
    },
    removePhoto(idx) {
      this.photoList.splice(idx, 1)
    },
    addEvidence() {
      uni.chooseImage({
        count: 1,
        sourceType: ['camera'],
        success: (res) => {
          this.evidenceList = this.evidenceList.concat(res.tempFilePaths)
        }
      })
    },
    removeEvidence(idx) {
      this.evidenceList.splice(idx, 1)
    },
    handleSubmit() {
      if (!this.canSubmit) return
      this.submitting = true
      const data = {
        complaintChannel: this.channel,
        contentText: this.contentText,
        complainantName: this.complainantName,
        complainantPhone: this.complainantPhone,
        activityId: this.selectedActivityId,
        staffId: this.selectedStaffId,
        staffName: this.selectedStaffName
      }
      if (this.channel === 'PLATFORM') {
        data.category = this.category
        data.evidencePhotos = JSON.stringify(this.evidenceList)
      } else {
        if (this.photoList.length > 0) {
          data.photoUrls = JSON.stringify(this.photoList)
        }
      }
      uni.request({
        url: getApp().globalData.baseUrl + config.api.microComplaintSubmit,
        method: 'POST',
        header: {
          'Authorization': 'Bearer ' + uni.getStorageSync('token'),
          'Content-Type': 'application/json'
        },
        data: data,
        success: (res) => {
          if (res.data && res.data.code === 200) {
            uni.showToast({ title: '投诉提交成功', icon: 'success' })
            this.resetForm()
            this.loadMyComplaints()
          } else {
            uni.showToast({ title: res.data.msg || '提交失败', icon: 'none' })
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
    resetForm() {
      this.contentText = ''
      this.category = ''
      this.photoList = []
      this.evidenceList = []
      this.complainantName = ''
      this.complainantPhone = ''
      this.selectedActivityId = null
      this.selectedStaffId = null
      this.selectedStaffName = ''
    },
    loadMyComplaints() {
      uni.request({
        url: getApp().globalData.baseUrl + config.api.microComplaintMy,
        header: { 'Authorization': 'Bearer ' + uni.getStorageSync('token') },
        success: (res) => {
          if (res.data && res.data.code === 200) {
            this.myComplaints = res.data.data || []
          }
        }
      })
    },
    statusLabel(status) {
      const map = { PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已解决', CLOSED: '已关闭' }
      return map[status] || status
    }
  }
}
</script>

<style scoped>
.container { padding: 24rpx; background: #f5f6f8; min-height: 100vh; }
.header { text-align: center; padding: 30rpx 0; }
.title { font-size: 40rpx; font-weight: bold; display: block; }
.subtitle { font-size: 26rpx; color: #999; display: block; margin-top: 8rpx; }
.section { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 24rpx; }
.section-title { font-size: 28rpx; font-weight: bold; display: block; margin-bottom: 16rpx; }
.required { color: #f56c6c; }
.channel-tabs { display: flex; gap: 16rpx; }
.channel-tab { flex: 1; padding: 24rpx; border: 2rpx solid #e4e7ed; border-radius: 12rpx; text-align: center; }
.channel-tab.active { border-color: #409eff; background: #ecf5ff; }
.tab-icon { font-size: 48rpx; display: block; }
.tab-label { font-size: 28rpx; font-weight: bold; display: block; margin-top: 8rpx; }
.tab-desc { font-size: 22rpx; color: #999; display: block; margin-top: 4rpx; }
.picker-value { display: flex; justify-content: space-between; align-items: center; padding: 16rpx; background: #f5f6f8; border-radius: 8rpx; }
.arrow { font-size: 24rpx; color: #999; }
.input-field { padding: 16rpx; background: #f5f6f8; border-radius: 8rpx; margin-bottom: 12rpx; font-size: 28rpx; }
.textarea-field { width: 100%; min-height: 200rpx; padding: 16rpx; background: #f5f6f8; border-radius: 8rpx; font-size: 28rpx; box-sizing: border-box; }
.char-count { font-size: 22rpx; color: #999; display: block; text-align: right; margin-top: 8rpx; }
.photo-grid { display: flex; flex-wrap: wrap; gap: 16rpx; }
.photo-item { position: relative; width: 160rpx; height: 160rpx; }
.photo-img { width: 160rpx; height: 160rpx; border-radius: 8rpx; }
.photo-remove { position: absolute; top: -10rpx; right: -10rpx; width: 36rpx; height: 36rpx; background: #f56c6c; color: #fff; border-radius: 50%; text-align: center; line-height: 36rpx; font-size: 24rpx; }
.photo-add { width: 160rpx; height: 160rpx; background: #f5f6f8; border-radius: 8rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; border: 2rpx dashed #dcdfe6; }
.add-icon { font-size: 48rpx; }
.add-hint { font-size: 22rpx; color: #999; margin-top: 4rpx; }
.submit-area { margin-bottom: 24rpx; }
.submit-btn { background: #409eff; color: #fff; border-radius: 12rpx; font-size: 32rpx; height: 88rpx; line-height: 88rpx; }
.submit-btn[disabled] { background: #c0c4cc; }
.hint-text { font-size: 24rpx; color: #999; }
.complaint-item { padding: 16rpx 0; border-bottom: 1rpx solid #f0f0f0; }
.complaint-top { display: flex; justify-content: space-between; align-items: center; }
.complaint-channel { font-size: 24rpx; color: #409eff; }
.complaint-status { font-size: 22rpx; padding: 2rpx 12rpx; border-radius: 6rpx; }
.status-PENDING { color: #909399; background: #f4f4f5; }
.status-PROCESSING { color: #e6a23c; background: #fdf6ec; }
.status-RESOLVED { color: #67c23a; background: #f0f9eb; }
.status-CLOSED { color: #909399; background: #f4f4f5; }
.complaint-content { font-size: 26rpx; color: #333; display: block; margin: 8rpx 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.complaint-time { font-size: 22rpx; color: #999; display: block; }
.complaint-reply { background: #f5f6f8; border-radius: 8rpx; padding: 12rpx; margin-top: 8rpx; }
.reply-label { font-size: 22rpx; color: #999; }
.reply-text { font-size: 24rpx; color: #333; }
.empty-hint { text-align: center; color: #999; font-size: 26rpx; padding: 30rpx; }
</style>
