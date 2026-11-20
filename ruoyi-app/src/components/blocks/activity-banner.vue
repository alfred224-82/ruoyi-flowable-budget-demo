<template>
    <view class="block activity-banner">
        <view v-if="loading" class="block-loading">加载中…</view>
        <view v-else-if="!activity" class="block-empty">暂无活动</view>
        <view v-else class="banner-card" @click="goDetail">
            <image v-if="activity.bgImageUrl || activity.logoUrl" class="banner-img"
                :src="toHttps(activity.bgImageUrl || activity.logoUrl)" mode="aspectFill" />
            <view v-else class="banner-placeholder" :style="{ background: themeGradient(activity.themeKey) }">
                <text class="placeholder-icon">🎉</text>
            </view>
            <view class="banner-info">
                <text class="activity-title">{{ activity.activityName || activity.title || '精彩活动' }}</text>
                <text class="activity-desc" v-if="activity.description">{{ activity.description }}</text>
                <text class="activity-time" v-if="activity.startTime">{{ formatTime(activity.startTime) }}</text>
            </view>
        </view>
    </view>
</template>

<script>
/**
 * 区块组件：活动 Banner（T15.16，blockKey=activity_banner）
 * 数据源：GET /micro/activity/browse（取第一条活动）
 */
import { get } from '../../utils/request'
import config from '../../config'
import { themeGradient } from '../../utils/theme'
import { toHttps } from '../../utils/format'

export default {
    name: 'BlockActivityBanner',
    props: { blockParams: { type: Object, default: () => ({}) } },
    data() { return { loading: false, activity: null } },
    created() { this.loadData() },
    methods: {
        themeGradient,
        toHttps,
        formatTime(t) {
            if (!t) return ''
            const d = new Date(t)
            return (d.getMonth() + 1) + '/' + d.getDate() + ' ' + d.getHours() + ':' + String(d.getMinutes()).padStart(2, '0')
        },
        goDetail() {
            if (this.activity && this.activity.id) {
                uni.navigateTo({ url: '/pages/micro/activity/browse?id=' + this.activity.id })
            }
        },
        async loadData() {
            this.loading = true
            try {
                const res = await get(config.api.microActivityBrowse, { pageNum: 1, pageSize: 1 })
                const rows = (res && res.rows) || []
                this.activity = rows[0] || null
            } catch (e) { /* 静默降级 */ }
            this.loading = false
        }
    }
}
</script>

<style scoped>
.block { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 20rpx; }
.block-loading, .block-empty { text-align: center; color: #909399; padding: 30rpx 0; font-size: 24rpx; }
.banner-card { border-radius: 12rpx; overflow: hidden; }
.banner-img { width: 100%; height: 280rpx; }
.banner-placeholder { width: 100%; height: 200rpx; background: linear-gradient(135deg, #409eff, #69c0ff); display: flex; align-items: center; justify-content: center; }
.placeholder-icon { font-size: 64rpx; }
.banner-info { padding: 16rpx 0; }
.activity-title { font-size: 30rpx; font-weight: bold; display: block; }
.activity-desc { font-size: 24rpx; color: #606266; display: block; margin-top: 8rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.activity-time { font-size: 22rpx; color: #909399; display: block; margin-top: 6rpx; }
</style>
