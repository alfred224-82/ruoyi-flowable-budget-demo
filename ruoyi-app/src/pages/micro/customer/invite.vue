<template>
    <view class="page">
        <!-- 顶部 Banner -->
        <view class="banner">
            <text class="banner-title">邀请有礼</text>
            <text class="banner-desc">邀请好友注册，即享专属奖励！</text>
        </view>

        <!-- 推广码区 -->
        <view class="card">
            <text class="card-title">我的邀请码</text>
            <view v-if="!loaded" class="loading">加载中…</view>
            <view v-else-if="!code" class="empty">
                <text>您还没有邀请码，点击下方生成</text>
                <button class="btn-primary" @click="generate">生成邀请码</button>
            </view>
            <view v-else class="code-area">
                <view class="code-box">
                    <text class="code-text">{{ code.sceneParam || '-' }}</text>
                </view>
                <text class="code-tip">分享此邀请码给好友，好友注册后即算您邀请成功</text>
                <button class="btn-share" open-type="share" v-if="code">分享给好友</button>
            </view>
        </view>

        <!-- 推广数据 -->
        <view v-if="code" class="card">
            <text class="card-title">我的推广数据</text>
            <view class="stat-grid">
                <view class="stat-item">
                    <text class="stat-num">{{ broughtCount }}</text>
                    <text class="stat-label">邀请人数</text>
                </view>
                <view class="stat-item">
                    <text class="stat-num">{{ consumedCount }}</text>
                    <text class="stat-label">已消费</text>
                </view>
                <view class="stat-item">
                    <text class="stat-num">{{ rewardRecords.length }}</text>
                    <text class="stat-label">获得奖励</text>
                </view>
            </view>
        </view>

        <!-- ★T18.16 邀请好友列表 -->
        <view v-if="code && friends.length" class="card">
            <text class="card-title">邀请好友列表</text>
            <view v-for="(f, idx) in friends" :key="idx" class="friend-row">
                <view class="friend-left">
                    <view class="friend-avatar">{{ (f.nickName || '顾').charAt(0) }}</view>
                    <view class="friend-info">
                        <text class="friend-name">{{ f.nickName || '顾客' }}</text>
                        <text class="friend-time">{{ formatTime(f.lastScanTime) }}</text>
                    </view>
                </view>
                <text class="friend-status" :class="f.consumed ? 'yes' : 'no'">{{ f.consumed ? '已消费' : '未消费' }}</text>
            </view>
        </view>

        <!-- 无好友提示 -->
        <view v-if="code && !friends.length" class="card">
            <text class="card-title">邀请好友列表</text>
            <view class="empty-reward">
                <text>还没有好友记录，快去分享邀请码吧！</text>
            </view>
        </view>

        <!-- 奖励记录 -->
        <view v-if="code && rewardRecords.length" class="card">
            <text class="card-title">我的奖励</text>
            <view v-for="r in rewardRecords" :key="r.id" class="reward-row">
                <view class="reward-left">
                    <text class="reward-icon">{{ r.rewardType === 'cash' ? '💰' : '🎫' }}</text>
                    <view>
                        <text class="reward-type">{{ r.rewardType === 'cash' ? '现金奖励' : r.rewardType === 'points' ? '积分奖励' : '优惠券' }}</text>
                        <text class="reward-status">{{ r.issueStatus === 'PAID_OFFLINE' || r.issueStatus === 'ISSUED' ? '已发放' : '待发放' }}</text>
                    </view>
                </view>
                <text class="reward-amount" v-if="r.rewardAmount">¥{{ r.rewardAmount }}</text>
            </view>
        </view>

        <!-- 无奖励提示 -->
        <view v-if="code && !rewardRecords.length" class="card">
            <text class="card-title">我的奖励</text>
            <view class="empty-reward">
                <text>暂无奖励，快去邀请好友吧！</text>
            </view>
        </view>

        <!-- 规则说明 -->
        <view class="card">
            <text class="card-title">活动规则</text>
            <view class="rule-list">
                <text class="rule-item">1. 分享您的邀请码给好友</text>
                <text class="rule-item">2. 好友通过邀请码注册即视为邀请成功</text>
                <text class="rule-item">3. 邀请成功后，好友首次消费后您将获得奖励</text>
                <text class="rule-item">4. 奖励形式以店铺配置为准（优惠券/积分）</text>
                <text class="rule-item">5. 每位好友仅计算一次邀请</text>
            </view>
        </view>
    </view>
</template>

<script>
import { get, post } from '../../../utils/request'
import config from '../../../config'

export default {
    data() {
        return { loaded: false, code: null, broughtCount: 0, consumedCount: 0, rewardRecords: [], friends: [] }
    },
    onShow() { this.loadMyPromotion() },
    onShareAppMessage() {
        return {
            title: '邀请你一起来，有惊喜奖励！',
            path: '/pages/login/index?promoter=' + (this.code ? this.code.sceneParam : '')
        }
    },
    methods: {
        async loadMyPromotion() {
            this.loaded = false
            try {
                const res = await get(config.api.microPromoMy)
                const data = (res && res.data) || {}
                this.code = data.code || null
                this.broughtCount = data.broughtCount || 0
                this.consumedCount = data.consumedCount || 0
                this.rewardRecords = data.rewardRecords || []
                this.friends = data.friends || []
            } catch (e) { /* ignore */ }
            this.loaded = true
        },
        async generate() {
            try {
                const res = await post(config.api.microPromoGenerate + '?codeType=personal')
                this.code = (res && res.data) || null
            } catch (e) {
                uni.showToast({ title: '生成失败', icon: 'none' })
            }
        },
        formatTime(t) {
            if (!t) return '-'
            const d = new Date(t)
            if (isNaN(d.getTime())) return String(t).substring(0, 10)
            return d.getFullYear() + '-' + String(d.getMonth() + 1).padStart(2, '0') + '-' + String(d.getDate()).padStart(2, '0')
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; min-height: 100vh; background: #f5f6f8; }
.banner { background: linear-gradient(135deg, #f56c6c, #e6a23c); border-radius: 16rpx; padding: 48rpx 32rpx; text-align: center; margin-bottom: 24rpx; }
.banner-title { font-size: 40rpx; font-weight: bold; color: #fff; display: block; }
.banner-desc { font-size: 26rpx; color: rgba(255,255,255,0.85); display: block; margin-top: 12rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 28rpx; margin-bottom: 20rpx; }
.card-title { font-size: 30rpx; font-weight: bold; display: block; margin-bottom: 20rpx; color: #303133; }
.code-area { text-align: center; }
.code-box { background: #f5f7fa; border-radius: 12rpx; padding: 24rpx; margin-bottom: 16rpx; }
.code-text { font-size: 32rpx; font-weight: bold; color: #409eff; word-break: break-all; }
.code-tip { font-size: 24rpx; color: #909399; display: block; margin-bottom: 20rpx; }
.btn-primary { background: #409eff; color: #fff; border-radius: 12rpx; font-size: 28rpx; margin-top: 20rpx; border: none; }
.btn-share { background: #67c23a; color: #fff; border-radius: 12rpx; font-size: 28rpx; border: none; }
.stat-grid { display: flex; justify-content: space-around; }
.stat-item { text-align: center; }
.stat-num { font-size: 44rpx; font-weight: bold; color: #409eff; display: block; }
.stat-label { font-size: 24rpx; color: #909399; display: block; margin-top: 8rpx; }
.reward-row { display: flex; align-items: center; justify-content: space-between; padding: 20rpx 0; border-bottom: 1rpx solid #f0f2f5; }
.reward-row:last-child { border-bottom: none; }
.reward-left { display: flex; align-items: center; }
.reward-icon { font-size: 36rpx; margin-right: 16rpx; }
.reward-type { font-size: 28rpx; color: #303133; display: block; }
.reward-status { font-size: 22rpx; color: #909399; display: block; margin-top: 4rpx; }
.reward-amount { font-size: 30rpx; color: #f56c6c; font-weight: bold; }
.rule-list { }
.rule-item { font-size: 26rpx; color: #606266; display: block; line-height: 2; }
.empty-reward { text-align: center; color: #909399; padding: 32rpx 0; font-size: 26rpx; }
.friend-row { display: flex; align-items: center; justify-content: space-between; padding: 20rpx 0; border-bottom: 1rpx solid #f0f2f5; }
.friend-row:last-child { border-bottom: none; }
.friend-left { display: flex; align-items: center; }
.friend-avatar { width: 64rpx; height: 64rpx; border-radius: 50%; background: #ecf5ff; color: #409eff; font-size: 28rpx; display: flex; align-items: center; justify-content: center; margin-right: 16rpx; flex-shrink: 0; }
.friend-info { }
.friend-name { font-size: 28rpx; color: #303133; display: block; }
.friend-time { font-size: 22rpx; color: #c0c4cc; display: block; margin-top: 4rpx; }
.friend-status { font-size: 24rpx; padding: 4rpx 16rpx; border-radius: 6rpx; }
.friend-status.yes { background: #f0f9eb; color: #67c23a; }
.friend-status.no { background: #f5f7fa; color: #909399; }
.loading, .empty { text-align: center; color: #909399; padding: 60rpx 0; font-size: 26rpx; }
</style>
