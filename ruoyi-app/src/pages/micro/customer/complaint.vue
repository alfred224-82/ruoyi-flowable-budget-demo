<template>
    <view class="page" :class="themeClass" :style="pageStyle">
        <!-- ★手绘主题装饰贴纸 -->
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-star-tl" :src="iconUrl('hand-star')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-bubble-tr" :src="iconUrl('hand-bubble')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-sparkle-bl" :src="iconUrl('hand-sparkle')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-cloud-br" :src="iconUrl('hand-cloud')" mode="aspectFit" />

        <!-- ═══ 投诉渠道选择 ═══ -->
        <view class="card">
            <text class="card-title">投诉渠道</text>
            <view class="channel-tabs">
                <view class="channel-tab" :class="{ active: channel === 'BOSS' }" @click="channel = 'BOSS'">
                    <text class="ch-icon">🏪</text>
                    <text class="ch-label">门店反馈</text>
                    <text class="ch-desc">门店内部问题</text>
                </view>
                <view class="channel-tab" :class="{ active: channel === 'PLATFORM' }" @click="channel = 'PLATFORM'">
                    <text class="ch-icon">⚖️</text>
                    <text class="ch-label">平台举报</text>
                    <text class="ch-desc">违法违规</text>
                </view>
            </view>
        </view>

        <!-- ═══ 投诉类型（★T32.8 服务态度/行程不符/收费争议/其他） ═══ -->
        <view class="card">
            <text class="card-title">投诉类型 <text class="required">*</text></text>
            <view class="type-grid">
                <view v-for="t in complaintTypes" :key="t" class="type-item" :class="{ active: complaintType === t }" @click="complaintType = t">
                    <text class="type-text">{{ t }}</text>
                </view>
            </view>
        </view>

        <!-- ═══ 关联活动（选填） ═══ -->
        <view class="card">
            <text class="card-title">关联活动（选填）</text>
            <picker v-if="activityList.length" :range="activityNames" @change="onActivityChange">
                <view class="picker-value">
                    <text>{{ selectedActivityId ? activityNames[activityIndex] : '不关联活动' }}</text>
                    <text class="picker-arrow">▼</text>
                </view>
            </picker>
            <text v-else class="hint-text">暂无可关联活动</text>
        </view>

        <!-- ═══ 投诉内容 + ★敏感词预检 ═══ -->
        <view class="card">
            <text class="card-title">投诉内容 <text class="required">*</text></text>
            <textarea class="textarea-field" v-model="contentText" :placeholder="contentPlaceholder"
                      :maxlength="500" @input="onContentInput" />
            <view class="char-row">
                <text class="char-count">{{ contentText.length }}/500</text>
                <text v-if="channel === 'PLATFORM'" class="char-min">最少20字</text>
            </view>
            <!-- ★敏感词命中提示（二档：命中即禁止提交） -->
            <view v-if="sensitiveHit" class="sensitive-alert">
                <text class="sensitive-icon">⚠️</text>
                <text class="sensitive-msg">{{ sensitiveMsg }}</text>
            </view>
        </view>

        <!-- ═══ 附加图片（BOSS 渠道可选，≤3张） ═══ -->
        <view v-if="channel === 'BOSS'" class="card">
            <text class="card-title">附加图片（选填，最多3张）</text>
            <view class="photo-grid">
                <view v-for="(photo, idx) in photoList" :key="idx" class="photo-item">
                    <image :src="photo" mode="aspectFill" class="photo-img" />
                    <view class="photo-remove" @click="removePhoto(idx)">×</view>
                </view>
                <view v-if="photoList.length < 3" class="photo-add" @click="addPhoto">
                    <text class="add-icon">+</text>
                </view>
            </view>
        </view>

        <!-- ═══ 取证照片（PLATFORM 渠道必填≥1张，仅摄像头） ═══ -->
        <view v-if="channel === 'PLATFORM'" class="card">
            <text class="card-title">取证照片 <text class="required">*</text>（仅摄像头拍摄）</text>
            <view class="photo-grid">
                <view v-for="(photo, idx) in evidenceList" :key="idx" class="photo-item">
                    <image :src="photo" mode="aspectFill" class="photo-img" />
                    <view class="photo-remove" @click="removeEvidence(idx)">×</view>
                </view>
                <view class="photo-add camera-only" @click="addEvidence">
                    <text class="add-icon">📷</text>
                    <text class="add-hint">拍照</text>
                </view>
            </view>
        </view>

        <!-- ═══ 提交按钮（★敏感词命中时置灰） ═══ -->
        <view class="submit-area">
            <button class="submit-btn" :class="{ 'is-disabled': !canSubmit }"
                    :disabled="!canSubmit || submitting" :loading="submitting" @click="handleSubmit">
                {{ submitting ? '提交中...' : '提交投诉' }}
            </button>
            <text v-if="sensitiveHit" class="submit-hint">请修改文案后再提交</text>
        </view>

        <!-- ═══ 我的投诉记录 ═══ -->
        <view class="card">
            <text class="card-title">我的投诉记录</text>
            <!-- 分类 Tab -->
            <view class="record-tabs">
                <view class="record-tab" :class="{ active: recordTab === 'pending' }" @click="recordTab = 'pending'">
                    <text>未处理</text>
                    <text v-if="pendingCount" class="tab-badge">{{ pendingCount }}</text>
                </view>
                <view class="record-tab" :class="{ active: recordTab === 'resolved' }" @click="recordTab = 'resolved'">
                    <text>已解决</text>
                    <text v-if="resolvedCount" class="tab-badge">{{ resolvedCount }}</text>
                </view>
            </view>
            <!-- 未处理列表 -->
            <view v-if="recordTab === 'pending'">
                <view v-if="pendingComplaints.length">
                    <view v-for="(item, idx) in pendingComplaints" :key="idx" class="complaint-item">
                        <view class="complaint-top">
                            <text class="complaint-channel">{{ item.complaintChannel === 'BOSS' ? '门店反馈' : '平台举报' }}</text>
                            <text class="complaint-status" :class="'status-' + item.status">{{ statusLabel(item.status) }}</text>
                        </view>
                        <text class="complaint-content">{{ item.contentText }}</text>
                        <text class="complaint-time">{{ item.submitTime || item.createTime }}</text>
                        <view v-if="item.handlerReply" class="complaint-reply">
                            <text class="reply-label">处理回复：</text>
                            <text class="reply-text">{{ item.handlerReply }}</text>
                        </view>
                    </view>
                </view>
                <view v-else class="empty-hint">暂无未处理的投诉</view>
            </view>
            <!-- 已解决列表 -->
            <view v-if="recordTab === 'resolved'">
                <view v-if="resolvedComplaints.length">
                    <view v-for="(item, idx) in resolvedComplaints" :key="idx" class="complaint-item">
                        <view class="complaint-top">
                            <text class="complaint-channel">{{ item.complaintChannel === 'BOSS' ? '门店反馈' : '平台举报' }}</text>
                            <text class="complaint-status" :class="'status-' + item.status">{{ statusLabel(item.status) }}</text>
                        </view>
                        <text class="complaint-content">{{ item.contentText }}</text>
                        <text class="complaint-time">{{ item.submitTime || item.createTime }}</text>
                        <view v-if="item.handlerReply" class="complaint-reply">
                            <text class="reply-label">处理回复：</text>
                            <text class="reply-text">{{ item.handlerReply }}</text>
                        </view>
                    </view>
                </view>
                <view v-else class="empty-hint">暂无已解决的投诉</view>
            </view>
        </view>
    </view>
</template>

<script>
import { get, post, upload } from '../../../utils/request'
import config from '../../../config'
import { checkSensitive, buildHitMessage, clearSensitiveDebounce } from '../../../utils/sensitive'
import themeMixin from '../../../utils/themeMixin'
import { iconUrl } from '../../../utils/themeIcons'
import { getUser } from '../../../utils/auth'

export default {
    mixins: [themeMixin],
    data() {
        return {
            channel: 'BOSS',
            complaintType: '',
            complaintTypes: ['服务态度', '行程不符', '收费争议', '其他'],
            contentText: '',
            activityList: [],
            activityNames: [],
            activityIndex: 0,
            selectedActivityId: null,
            photoList: [],
            evidenceList: [],
            photoUrls: [],       // ★已上传 OSS 的 URL 列表（BOSS 渠道附加图片）
            evidenceUrls: [],    // ★已上传 OSS 的 URL 列表（PLATFORM 渠道取证照片）
            uploading: false,    // ★图片上传中
            submitting: false,
            myComplaints: [],
            recordTab: 'pending',
            // ★T32.8 敏感词预检状态
            sensitiveHit: false,
            sensitiveMsg: ''
        }
    },
    computed: {
        canSubmit() {
            if (this.sensitiveHit) return false
            if (!this.complaintType) return false
            if (!this.contentText.trim()) return false
            if (this.channel === 'PLATFORM') {
                if (this.contentText.trim().length < 20) return false
                if (this.evidenceList.length === 0) return false
            }
            return true
        },
        contentPlaceholder() {
            return this.channel === 'PLATFORM'
                ? '请详细描述投诉内容（不少于20字）...'
                : '请描述您的问题或反馈...'
        },
        /** 未处理投诉（PENDING / PROCESSING） */
        pendingComplaints() {
            return this.myComplaints.filter(c => c.status === 'PENDING' || c.status === 'PROCESSING')
        },
        /** 已解决投诉（RESOLVED / CLOSED） */
        resolvedComplaints() {
            return this.myComplaints.filter(c => c.status === 'RESOLVED' || c.status === 'CLOSED')
        },
        pendingCount() { return this.pendingComplaints.length },
        resolvedCount() { return this.resolvedComplaints.length }
    },
    onShow() {
        this.loadMyComplaints()
        this.loadActivities()
    },
    beforeUnmount() {
        clearSensitiveDebounce('complaint')
    },
    methods: {
        iconUrl,

        /** ★T32.8 敏感词前端预检（防抖 400ms，命中即按钮置灰） */
        async onContentInput() {
            const text = this.contentText
            if (!text || !text.trim()) {
                this.sensitiveHit = false
                this.sensitiveMsg = ''
                return
            }
            const res = await checkSensitive(text, {
                bizType: 'complaint',
                key: 'complaint',
                debounce: 400
            })
            if (!res.pass) {
                this.sensitiveHit = true
                this.sensitiveMsg = buildHitMessage(res.hitWords)
            } else {
                this.sensitiveHit = false
                this.sensitiveMsg = ''
            }
        },

        async loadActivities() {
            try {
                const res = await get(config.api.microCustomerMyActivities)
                const list = (res && res.data) || []
                this.activityList = list.filter(a => a.sourceType === 'ACTIVITY_SIGNUP')
                this.activityNames = this.activityList.map(a => a.title || '活动#' + a.bizId)
            } catch (e) {
                this.activityList = []
                this.activityNames = []
            }
        },

        onActivityChange(e) {
            this.activityIndex = parseInt(e.detail.value)
            this.selectedActivityId = this.activityList[this.activityIndex].bizId
        },

        addPhoto() {
            uni.chooseImage({
                count: 3 - this.photoList.length,
                sourceType: ['album', 'camera'],
                success: async (res) => {
                    this.uploading = true
                    for (const path of res.tempFilePaths) {
                        try {
                            const r = await upload(config.api.microComplaintUpload, path)
                            const url = (r.data || {}).url
                            if (url) {
                                this.photoList.push(path)
                                this.photoUrls.push(url)
                            }
                        } catch (e) {
                            uni.showToast({ title: '图片上传失败', icon: 'none' })
                        }
                    }
                    this.uploading = false
                }
            })
        },
        removePhoto(idx) {
            this.photoList.splice(idx, 1)
            this.photoUrls.splice(idx, 1)
        },
        addEvidence() {
            uni.chooseImage({
                count: 1,
                sourceType: ['camera'],
                success: async (res) => {
                    this.uploading = true
                    for (const path of res.tempFilePaths) {
                        try {
                            const r = await upload(config.api.microComplaintUpload, path)
                            const url = (r.data || {}).url
                            if (url) {
                                this.evidenceList.push(path)
                                this.evidenceUrls.push(url)
                            }
                        } catch (e) {
                            uni.showToast({ title: '照片上传失败', icon: 'none' })
                        }
                    }
                    this.uploading = false
                }
            })
        },
        removeEvidence(idx) {
            this.evidenceList.splice(idx, 1)
            this.evidenceUrls.splice(idx, 1)
        },

        /** ★提交投诉（服务端二次终检兜底） */
        async handleSubmit() {
            if (!this.canSubmit || this.submitting || this.uploading) return
            this.submitting = true
            try {
                const user = getUser() || {}
                const data = {
                    complaintChannel: this.channel,
                    category: this.complaintType,
                    contentText: this.contentText,
                    activityId: this.selectedActivityId,
                    complainantPhone: user.phonenumber || '',
                    complainantName: user.nickName || ''
                }
                if (this.channel === 'PLATFORM' && this.evidenceUrls.length) {
                    data.evidencePhotos = JSON.stringify(this.evidenceUrls)
                }
                if (this.channel === 'BOSS' && this.photoUrls.length) {
                    data.photoUrls = JSON.stringify(this.photoUrls)
                }
                const res = await post(config.api.microComplaintSubmit, data)
                if (res && res.code === 200) {
                    uni.showToast({ title: '投诉提交成功', icon: 'success' })
                    this.resetForm()
                    this.loadMyComplaints()
                } else {
                    uni.showToast({ title: (res && res.msg) || '提交失败', icon: 'none' })
                }
            } catch (e) {
                const msg = (e && e.message) || (e && e.msg) || '网络错误'
                uni.showToast({ title: msg, icon: 'none' })
            }
            this.submitting = false
        },

        resetForm() {
            this.contentText = ''
            this.complaintType = ''
            this.photoList = []
            this.photoUrls = []
            this.evidenceList = []
            this.evidenceUrls = []
            this.selectedActivityId = null
            this.sensitiveHit = false
            this.sensitiveMsg = ''
        },

        async loadMyComplaints() {
            try {
                const res = await get(config.api.microComplaintMy)
                this.myComplaints = (res && res.data) || []
            } catch (e) {
                this.myComplaints = []
            }
        },

        statusLabel(status) {
            const map = { PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已解决', CLOSED: '已关闭' }
            return map[status] || status
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 40rpx; min-height: 100vh; background: var(--page-bg, #f5f6f8); position: relative; }
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 24rpx 28rpx; margin-bottom: 20rpx; position: relative; z-index: 1; box-shadow: var(--shadow-card, none); }
.card-title { font-size: 28rpx; font-weight: bold; display: block; margin-bottom: 16rpx; color: var(--text, #303133); }
.required { color: #f56c6c; }

/* 渠道选择 */
.channel-tabs { display: flex; gap: 16rpx; }
.channel-tab { flex: 1; padding: 20rpx 12rpx; border: 2rpx solid var(--border, #e4e7ed); border-radius: var(--radius-card, 12rpx); text-align: center; }
.channel-tab.active { border-color: var(--brand, #409eff); background: var(--brand-light, #ecf5ff); }
.ch-icon { font-size: 40rpx; display: block; }
.ch-label { font-size: 26rpx; font-weight: bold; display: block; margin-top: 6rpx; color: var(--text, #303133); }
.ch-desc { font-size: 20rpx; color: var(--text-sub, #909399); display: block; margin-top: 4rpx; }

/* 投诉类型 */
.type-grid { display: flex; flex-wrap: wrap; gap: 12rpx; }
.type-item { padding: 12rpx 24rpx; border: 2rpx solid var(--border, #e4e7ed); border-radius: var(--radius-btn, 32rpx); }
.type-item.active { border-color: var(--brand, #409eff); background: var(--brand-light, #ecf5ff); }
.type-text { font-size: 26rpx; color: var(--text, #606266); }
.type-item.active .type-text { color: var(--brand, #409eff); font-weight: bold; }

/* Picker */
.picker-value { display: flex; justify-content: space-between; align-items: center; padding: 16rpx; background: var(--page-bg, #f5f6f8); border-radius: var(--radius-btn, 8rpx); font-size: 28rpx; color: var(--text, #303133); }
.picker-arrow { font-size: 22rpx; color: var(--text-sub, #909399); }
.hint-text { font-size: 24rpx; color: var(--text-sub, #c0c4cc); }

/* 文本域 */
.textarea-field { width: 100%; min-height: 200rpx; padding: 16rpx; background: var(--page-bg, #f5f6f8); border-radius: var(--radius-btn, 8rpx); font-size: 28rpx; box-sizing: border-box; color: var(--text, #303133); }
.char-row { display: flex; justify-content: space-between; margin-top: 8rpx; }
.char-count { font-size: 22rpx; color: var(--text-sub, #c0c4cc); }
.char-min { font-size: 22rpx; color: #e6a23c; }

/* ★敏感词命中提示 */
.sensitive-alert { display: flex; align-items: flex-start; gap: 8rpx; margin-top: 12rpx; padding: 16rpx; background: #fef0f0; border-radius: var(--radius-btn, 8rpx); border: 1rpx solid #fde2e2; }
.sensitive-icon { font-size: 28rpx; flex-shrink: 0; }
.sensitive-msg { font-size: 24rpx; color: #f56c6c; line-height: 1.5; }

/* 图片上传 */
.photo-grid { display: flex; flex-wrap: wrap; gap: 16rpx; }
.photo-item { position: relative; width: 160rpx; height: 160rpx; }
.photo-img { width: 160rpx; height: 160rpx; border-radius: var(--radius-btn, 8rpx); }
.photo-remove { position: absolute; top: -10rpx; right: -10rpx; width: 36rpx; height: 36rpx; background: #f56c6c; color: #fff; border-radius: 50%; text-align: center; line-height: 36rpx; font-size: 24rpx; }
.photo-add { width: 160rpx; height: 160rpx; background: var(--page-bg, #f5f6f8); border-radius: var(--radius-btn, 8rpx); display: flex; flex-direction: column; align-items: center; justify-content: center; border: 2rpx dashed var(--border, #dcdfe6); }
.add-icon { font-size: 48rpx; color: var(--text-sub, #c0c4cc); }
.add-hint { font-size: 22rpx; color: var(--text-sub, #909399); margin-top: 4rpx; }

/* 提交 */
.submit-area { margin: 0 0 24rpx; position: relative; z-index: 1; }
.submit-btn { background: var(--brand, #409eff); color: #fff; border-radius: var(--radius-btn, 12rpx); font-size: 32rpx; height: 88rpx; line-height: 88rpx; }
.submit-btn.is-disabled, .submit-btn[disabled] { background: #c0c4cc; color: #fff; }
.submit-hint { display: block; text-align: center; font-size: 24rpx; color: #f56c6c; margin-top: 8rpx; }

/* 投诉记录 Tab */
.record-tabs { display: flex; gap: 16rpx; margin-bottom: 16rpx; }
.record-tab { flex: 1; text-align: center; padding: 12rpx 0; border-radius: var(--radius-btn, 8rpx); font-size: 26rpx; color: var(--text-sub, #909399); background: var(--page-bg, #f5f6f8); display: flex; align-items: center; justify-content: center; gap: 8rpx; }
.record-tab.active { color: var(--brand, #409eff); background: var(--brand-light, #ecf5ff); font-weight: bold; }
.tab-badge { font-size: 20rpx; padding: 0 10rpx; border-radius: 20rpx; background: #f0f0f0; color: #909399; line-height: 32rpx; min-width: 32rpx; }
.record-tab.active .tab-badge { background: var(--brand, #409eff); color: #fff; }

/* 投诉记录 */
.complaint-item { padding: 16rpx 0; border-bottom: 1rpx solid var(--border, #f0f0f0); }
.complaint-item:last-child { border-bottom: none; }
.complaint-top { display: flex; justify-content: space-between; align-items: center; }
.complaint-channel { font-size: 24rpx; color: var(--brand, #409eff); }
.complaint-status { font-size: 22rpx; padding: 2rpx 12rpx; border-radius: var(--radius-tag, 6rpx); }
.status-PENDING { color: #909399; background: #f4f4f5; }
.status-PROCESSING { color: #e6a23c; background: #fdf6ec; }
.status-RESOLVED { color: #67c23a; background: #f0f9eb; }
.status-CLOSED { color: #909399; background: #f4f4f5; }
.complaint-content { font-size: 26rpx; color: var(--text, #333); display: block; margin: 8rpx 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.complaint-time { font-size: 22rpx; color: var(--text-sub, #999); display: block; }
.complaint-reply { background: var(--page-bg, #f5f6f8); border-radius: var(--radius-btn, 8rpx); padding: 12rpx; margin-top: 8rpx; }
.reply-label { font-size: 22rpx; color: var(--text-sub, #999); }
.reply-text { font-size: 24rpx; color: var(--text, #333); }
.empty-hint { text-align: center; color: var(--text-sub, #999); font-size: 26rpx; padding: 30rpx; }

/* ★手绘主题装饰贴纸 */
.sticker { position: absolute; opacity: .8; pointer-events: none; z-index: 0; }
.sticker-star-tl { top: 16rpx; left: 16rpx; width: 52rpx; height: 52rpx; transform: rotate(-10deg); }
.sticker-bubble-tr { top: 10rpx; right: 16rpx; width: 56rpx; height: 56rpx; transform: rotate(8deg); }
.sticker-sparkle-bl { bottom: 80rpx; left: 16rpx; width: 36rpx; height: 36rpx; transform: rotate(15deg); }
.sticker-cloud-br { bottom: 80rpx; right: 16rpx; width: 72rpx; height: 56rpx; transform: rotate(-5deg); }
</style>
