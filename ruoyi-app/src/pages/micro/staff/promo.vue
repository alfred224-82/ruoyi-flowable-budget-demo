<template>
    <view class="page" :class="themeClass" :style="pageStyle">
        <view class="card">
            <text class="card-title">我的推广码</text>
            <view v-if="!loaded" class="loading">加载中…</view>
            <view v-else-if="!code" class="empty">
                <text>暂无推广码</text>
                <button class="btn" @click="generate">生成推广码</button>
            </view>
            <view v-else class="code-area">
                <!-- ★H5推广码：顾客手机浏览器扫码直接进入登录页 -->
                <image v-if="qrcodeUrl" class="qr-img" :src="toHttps(qrcodeUrl)" mode="widthFix" show-menu-by-longpress />
                <text class="qr-tip">顾客手机浏览器扫码直接进入登录页，业绩自动归属您</text>
                <text class="status" :class="code.status === '0' ? 'on' : 'off'">{{ code.status === '0' ? '启用中' : '已禁用' }}</text>
                <text class="time">创建时间：{{ code.createTime }}</text>
                <!-- ★重新生成：修复 t=null 存量码或需要刷新 scene 时使用 -->
                <button class="btn-regen" @click="regenerate">重新生成推广码</button>
            </view>
        </view>

        <view v-if="code" class="card">
            <text class="card-title">推广数据</text>
            <view class="stat-row">
                <text class="stat-num">{{ broughtCount }}</text>
                <text class="stat-label">带来人数</text>
            </view>
        </view>

        <view v-if="code && rewardRecords.length" class="card">
            <text class="card-title">我的奖励</text>
            <view v-for="r in rewardRecords" :key="r.id" class="reward-row">
                <text class="reward-type">{{ r.rewardType === 'cash' ? '现金' : '优惠券' }}</text>
                <text class="reward-amount">¥{{ r.rewardAmount || '-' }}</text>
                <text class="reward-status">{{ r.issueStatus === 'PAID_OFFLINE' ? '已发放' : '待发放' }}</text>
            </view>
        </view>

        <!-- ★V3.7 底部导航 -->
        <micro-tabbar current="/pages/micro/staff/promo" />
    </view>
</template>

<script>
import { get, post } from '../../../utils/request'
import config from '../../../config'
import { toHttps } from '../../../utils/format'
import MicroTabbar from '../../../components/micro-tabbar/micro-tabbar.vue'
import themeMixin from '../../../utils/themeMixin'

export default {
    components: { MicroTabbar },
    mixins: [themeMixin],
    data() {
        return { loaded: false, code: null, broughtCount: 0, rewardRecords: [], qrcodeUrl: '', h5Url: '' }
    },
    onShow() { this.loadMyPromotion(); this.loadQrcode() },
    methods: {
        toHttps,
        /** ★H5推广码：后端生成 H5 URL 的二维码（base64 图片） */
        async loadQrcode() {
            try {
                const res = await get(config.api.microPromoMyQrcode)
                const data = (res && res.data) || {}
                this.qrcodeUrl = data.qrcodeUrl || ''
                this.h5Url = data.h5Url || ''
            } catch (e) { this.qrcodeUrl = '' }
        },
        async loadMyPromotion() {
            this.loaded = false
            try {
                const res = await get(config.api.microPromoMy)
                const data = (res && res.data) || {}
                this.code = data.code || null
                this.broughtCount = data.broughtCount || 0
                this.rewardRecords = data.rewardRecords || []
            } catch (e) { /* ignore */ }
            this.loaded = true
        },
        async generate() {
            try {
                const res = await post(config.api.microPromoGenerate + '?codeType=personal')
                this.code = (res && res.data) || null
                // 生成成功后刷新小程序码
                await this.loadQrcode()
            } catch (e) {
                uni.showToast({ title: '生成失败', icon: 'none' })
            }
        },
        /** 重新生成推广码（覆盖旧码，修复 t=null 等存量问题） */
        async regenerate() {
            uni.showModal({
                title: '重新生成',
                content: '重新生成后旧推广码将失效，确定继续？',
                success: async (res) => {
                    if (res.confirm) {
                        await this.generate()
                        uni.showToast({ title: '已重新生成', icon: 'success' })
                    }
                }
            })
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 140rpx; min-height: 100vh; background: var(--page-bg, #f5f6f8); }
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 28rpx; margin-bottom: 20rpx; box-shadow: var(--shadow-card, none); }
.card-title { font-size: 30rpx; font-weight: bold; display: block; margin-bottom: 20rpx; color: var(--text, #303133); }
.code-area { text-align: center; }
.qr-img { width: 360rpx; height: 360rpx; display: block; margin: 0 auto 16rpx; }
.qr-tip { font-size: 24rpx; color: var(--text-sub, #909399); display: block; text-align: center; margin-bottom: 12rpx; }
.scene { font-size: 26rpx; color: var(--text-sub, #606266); display: block; word-break: break-all; margin-bottom: 12rpx; }
.status { font-size: 24rpx; padding: 4rpx 16rpx; border-radius: var(--radius-tag, 6rpx); }
.status.on { background: #f0f9eb; color: #67c23a; }
.status.off { background: #fef0f0; color: #f56c6c; }
.time { font-size: 22rpx; color: var(--text-sub, #909399); display: block; margin-top: 8rpx; }
.stat-row { text-align: center; }
.stat-num { font-size: 48rpx; font-weight: bold; color: var(--brand, #409eff); display: block; }
.stat-label { font-size: 24rpx; color: var(--text-sub, #909399); display: block; margin-top: 8rpx; }
.reward-row { display: flex; align-items: center; padding: 16rpx 0; border-bottom: 1rpx solid var(--border, #f0f2f5); }
.reward-row:last-child { border-bottom: none; }
.reward-type { flex: 1; font-size: 28rpx; color: var(--text, #303133); }
.reward-amount { width: 160rpx; text-align: right; font-size: 28rpx; color: #f56c6c; font-weight: bold; }
.reward-status { width: 120rpx; text-align: right; font-size: 24rpx; color: var(--text-sub, #909399); }
.btn { background: var(--brand, #409eff); color: #fff; border-radius: var(--radius-btn, 12rpx); font-size: 28rpx; margin-top: 20rpx; }
.btn-regen { background: var(--card-bg, #fff); color: var(--brand, #409eff); border: 2rpx solid var(--brand, #409eff); border-radius: var(--radius-btn, 12rpx); font-size: 26rpx; margin-top: 20rpx; }
.loading, .empty { text-align: center; color: var(--text-sub, #909399); padding: 60rpx 0; }
</style>
