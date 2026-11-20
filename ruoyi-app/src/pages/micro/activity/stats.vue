<template>
    <view class="page" :style="{ background: theme.bgColor }">
        <view class="card-header">
            <text class="card-title">活动情况</text>
            <text v-if="loaded && activities.length" class="card-sub">共 {{ activities.length }} 个活动</text>
        </view>

        <view v-if="!loaded" class="loading">加载中…</view>
        <view v-else-if="!activities.length" class="empty">暂无活动数据</view>
        <view v-else>
            <view v-for="a in activities" :key="a.id" class="card activity-card">
                <!-- ★v1.43 活动封面：背景图 > 海报 > 封面图；无图按活动自身主题渐变占位 -->
                <image v-if="a.bgImageUrl || a.posterUrl || a.coverUrl" class="cover"
                       :src="toHttps(a.bgImageUrl || a.posterUrl || firstImage(a.coverUrl))" mode="aspectFill" />
                <view v-else class="cover cover-default" :style="{ background: activityGradient(a.themeKey) }">
                    <text class="cover-title" :style="{ color: gradText(a.themeKey) }">{{ a.title || '活动' }}</text>
                </view>
                <view class="info">
                    <view class="act-top">
                        <image v-if="a.logoUrl" class="logo" :src="toHttps(a.logoUrl)" mode="aspectFill" />
                        <text class="act-name">{{ a.title || a.name || '活动' }}</text>
                        <!-- ★v1.43 状态修正：后端 status 为 PUBLISHED/ENDED/DRAFT（原 '1' 判断恒不命中） -->
                        <text class="act-status" :class="statusClass(a)" :style="statusStyle(a)">{{ statusLabel(a) }}</text>
                    </view>
                    <view class="act-stats">
                        <view class="stat-item">
                            <text class="stat-num" :style="{ color: theme.primaryColor }">{{ a.viewCount || 0 }}</text>
                            <text class="stat-label">浏览量</text>
                        </view>
                        <view class="stat-item">
                            <text class="stat-num" :style="{ color: theme.primaryColor }">{{ a.participantCount || 0 }}</text>
                            <text class="stat-label">参与人数</text>
                        </view>
                        <view class="stat-item">
                            <text class="stat-num" :style="{ color: theme.primaryColor }">{{ a.shareCount || 0 }}</text>
                            <text class="stat-label">分享次数</text>
                        </view>
                    </view>
                    <view class="act-time" v-if="a.startTime || a.endTime">
                        <text class="time-text">{{ a.startTime || '' }} ~ {{ a.endTime || '' }}</text>
                    </view>
                </view>
            </view>
        </view>
    </view>
</template>

<script>
import { get } from '../../../utils/request'
import config from '../../../config'
import { getThemeSync, loadTenantTheme, themeGradient, themeGradientText } from '../../../utils/theme'
import { toHttps, firstImage } from '../../../utils/format'

const STATUS_LABEL = { PUBLISHED: '进行中', ENDED: '已结束', DRAFT: '草稿' }
const STATUS_CLASS = { PUBLISHED: 'st-on', ENDED: 'st-off', DRAFT: 'st-draft' }

export default {
    data() {
        return {
            loaded: false,
            activities: [],
            // ★v1.43 租户主题色板（缓存先渲染，接口回包后刷新）
            theme: getThemeSync()
        }
    },
    onShow() {
        this.theme = getThemeSync()
        loadTenantTheme().then(t => { this.theme = t })
        this.loadStats()
    },
    methods: {
        toHttps,
        firstImage,
        statusLabel(a) { return STATUS_LABEL[a.status] || '已结束' },
        statusClass(a) { return STATUS_CLASS[a.status] || 'st-off' },
        /** 进行中徽标按主题主色淡底渲染（8 位 hex 透明度） */
        statusStyle(a) {
            if (a.status !== 'PUBLISHED') return {}
            return { background: this.theme.primaryColor + '1a', color: this.theme.primaryColor }
        },
        /** 无封面图时按活动自身 theme_key 渲染渐变占位 */
        activityGradient(themeKey) { return themeGradient(themeKey) },
        /** 渐变占位封面标题色：浅色渐变用深色字（写死白色在浅色底不可读） */
        gradText(themeKey) { return themeGradientText(themeKey).text },
        async loadStats() {
            this.loaded = false
            try {
                const res = await get(config.api.microActivityStats, { pageNum: 1, pageSize: 20 })
                const data = (res && res.data) || {}
                this.activities = data.rows || data.list || (Array.isArray(data) ? data : [])
            } catch (e) { this.activities = [] }
            this.loaded = true
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; min-height: 100vh; }
.card-header { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 20rpx; }
.card-title { font-size: 34rpx; font-weight: bold; }
.card-sub { font-size: 24rpx; color: #909399; }
/* ★v1.43 活动卡片：封面大图 + 圆角 + 轻投影 */
.card { background: #fff; border-radius: 16rpx; margin-bottom: 20rpx; overflow: hidden; box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04); }
.cover { width: 100%; height: 280rpx; display: block; }
.cover-default { display: flex; align-items: center; justify-content: center; }
.cover-title { font-size: 32rpx; font-weight: bold; padding: 0 32rpx; }
.info { padding: 24rpx 28rpx; }
.act-top { display: flex; align-items: center; margin-bottom: 20rpx; }
.logo { width: 56rpx; height: 56rpx; border-radius: 8rpx; margin-right: 12rpx; flex-shrink: 0; }
.act-name { font-size: 30rpx; font-weight: bold; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.act-status { font-size: 24rpx; padding: 4rpx 16rpx; border-radius: 6rpx; margin-left: 16rpx; white-space: nowrap; }
.act-status.st-on { background: #f0f9eb; color: #67c23a; }
.act-status.st-off { background: #f0f2f5; color: #909399; }
.act-status.st-draft { background: #fdf6ec; color: #e6a23c; }
.act-stats { display: flex; justify-content: space-around; padding: 20rpx 0; border-top: 1rpx solid #f0f2f5; border-bottom: 1rpx solid #f0f2f5; }
.stat-item { text-align: center; }
.stat-num { font-size: 40rpx; font-weight: bold; color: #409eff; display: block; }
.stat-label { font-size: 22rpx; color: #909399; display: block; margin-top: 4rpx; }
.act-time { margin-top: 16rpx; }
.time-text { font-size: 22rpx; color: #c0c4cc; }
.loading, .empty { text-align: center; color: #909399; padding: 80rpx 0; }
</style>
