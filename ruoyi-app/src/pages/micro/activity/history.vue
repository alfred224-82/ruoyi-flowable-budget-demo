<template>
    <view class="page" :class="themeClass" :style="pageStyle">
        <!-- ★手绘主题装饰贴纸（仅 theme_handdrawn 显示） -->
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-bubble-tr" :src="iconUrl('hand-bubble')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-sparkle-tr" :src="iconUrl('hand-sparkle')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-bubble-tab-l" :src="iconUrl('hand-bubble')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-star-tab-r" :src="iconUrl('hand-star')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-bubble-bl" :src="iconUrl('hand-bubble')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-star-bl" :src="iconUrl('hand-star')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-cloud-br" :src="iconUrl('hand-cloud')" mode="aspectFit" />
        <!-- ★搜索栏：按名称 + 时间范围查询 -->
        <view class="search-bar">
            <input class="search-input" v-model="searchTitle" placeholder="搜索活动名称" confirm-type="search" @confirm="doSearch" />
            <view class="date-row">
                <picker mode="date" :value="searchBegin" @change="onBeginChange">
                    <view class="date-pick">{{ searchBegin || '开始日期' }}</view>
                </picker>
                <text class="date-sep">~</text>
                <picker mode="date" :value="searchEnd" @change="onEndChange">
                    <view class="date-pick">{{ searchEnd || '结束日期' }}</view>
                </picker>
            </view>
            <view class="search-actions">
                <text class="search-btn" @click="doSearch">搜索</text>
                <text class="reset-btn" @click="resetSearch">重置</text>
            </view>
        </view>

        <!-- 历史活动列表 -->
        <view v-if="!loaded" class="loading">加载中…</view>
        <view v-else-if="!list.length" class="empty">
            <text class="empty-text">暂无历史活动</text>
        </view>
        <view v-else class="activity-list">
            <view v-for="item in list" :key="item.id" class="card activity-card" @click="viewDetail(item)">
                <!-- 封面图：优先背景图，其次海报/封面，无图用主题默认背景 -->
                <image v-if="item.bgImageUrl || item.posterUrl || item.coverUrl"
                       class="cover" :src="toHttps(item.bgImageUrl || item.posterUrl || firstImage(item.coverUrl))" mode="aspectFill" />
                <view v-else class="cover cover-default" :style="{ background: themeBg(item.themeKey) }">
                    <text :style="{ color: gradText(item.themeKey), fontSize: '32rpx', fontWeight: 'bold' }">{{ item.title || '活动' }}</text>
                </view>
                <!-- 信息区 -->
                <view class="info">
                    <view class="title-row">
                        <image v-if="item.logoUrl" class="logo" :src="toHttps(item.logoUrl)" mode="aspectFill" />
                        <text class="title">{{ item.title || '-' }}</text>
                        <text class="status-badge" :class="item.status === 'DRAFT' ? 'st-cancel' : 'st-done'">{{ statusLabel(item) }}</text>
                    </view>
                    <text v-if="item.startTime" class="time">{{ item.startTime }} ~ {{ item.endTime || '' }}</text>
                    <text v-else-if="item.publishTime" class="time">{{ item.publishTime }}</text>
                    <text class="content">{{ item.content ? (item.content.length > 60 ? item.content.substring(0, 60) + '...' : item.content) : '' }}</text>
                </view>
            </view>
        </view>

    </view>
</template>

<script>
import { get } from '../../../utils/request'
import config from '../../../config'
import { themeGradient, themeGradientText } from '../../../utils/theme'
import { toHttps, firstImage } from '../../../utils/format'
import themeMixin from '../../../utils/themeMixin'
import { iconUrl } from '../../../utils/themeIcons'

export default {
    mixins: [themeMixin],
    data() {
        return {
            loaded: false,
            list: [],
            // ★搜索条件：名称 + 时间范围
            searchTitle: '',
            searchBegin: '',
            searchEnd: ''
        }
    },
    onLoad() {
        this.loadHistory()
    },
    methods: {
        toHttps,
        firstImage,
        iconUrl,
        themeBg(themeKey) { return themeGradient(themeKey) },
        /** 渐变占位封面标题色：浅色渐变用深色字（写死白色在浅色底不可读） */
        gradText(themeKey) { return themeGradientText(themeKey).text },
        /** DRAFT=已取消（boss 下架）；ENDED / 过期 PUBLISHED=已完成 */
        statusLabel(item) { return item.status === 'DRAFT' ? '已取消' : '已完成' },
        async loadHistory() {
            this.loaded = false
            try {
                const params = { pageNum: 1, pageSize: 50 }
                if (this.searchTitle) { params.title = this.searchTitle }
                if (this.searchBegin) { params.beginTime = this.searchBegin }
                if (this.searchEnd) { params.endTime = this.searchEnd }
                const res = await get(config.api.microActivityHistory, params)
                this.list = (res && res.rows) || []
            } catch (e) {
                this.list = []
            } finally {
                this.loaded = true
            }
        },
        /** ★搜索（名称 + 时间范围） */
        doSearch() { this.loadHistory() },
        /** ★重置搜索条件 */
        resetSearch() {
            this.searchTitle = ''
            this.searchBegin = ''
            this.searchEnd = ''
            this.loadHistory()
        },
        onBeginChange(e) { this.searchBegin = e.detail.value },
        onEndChange(e) { this.searchEnd = e.detail.value },
        /** ★活动详情单页化：所有角色/入口统一跳 pages/micro/activity/detail */
        viewDetail(item) {
            uni.navigateTo({ url: '/pages/micro/activity/detail?id=' + encodeURIComponent(JSON.stringify(item)) })
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 40rpx; min-height: 100vh; background: #f5f6f8; }

/* ★搜索栏 */
.search-bar { background: #fff; border-radius: 16rpx; padding: 20rpx 24rpx; margin-bottom: 24rpx; }
.search-input { height: 68rpx; background: #f5f6f8; border-radius: 34rpx; padding: 0 28rpx; font-size: 28rpx; }
.date-row { display: flex; align-items: center; margin-top: 16rpx; }
.date-pick { flex: 1; height: 60rpx; line-height: 60rpx; text-align: center; background: #f5f6f8; border-radius: 8rpx; font-size: 26rpx; color: #606266; padding: 0 16rpx; }
.date-sep { margin: 0 16rpx; color: #909399; font-size: 26rpx; }
.search-actions { display: flex; margin-top: 16rpx; }
.search-btn { flex: 1; text-align: center; height: 64rpx; line-height: 64rpx; background: #409eff; color: #fff; border-radius: 8rpx; font-size: 28rpx; margin-right: 16rpx; }
.reset-btn { flex: 1; text-align: center; height: 64rpx; line-height: 64rpx; background: #f0f2f5; color: #606266; border-radius: 8rpx; font-size: 28rpx; }

/* 活动卡片 */
.activity-card { display: flex; flex-direction: column; background: #fff; border-radius: 16rpx; padding: 0; margin-bottom: 24rpx; overflow: hidden; }
.cover { width: 100%; height: 320rpx; }
.cover-default { display: flex; align-items: center; justify-content: center; }
.info { padding: 24rpx 28rpx; }
.title-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12rpx; }
.logo { width: 56rpx; height: 56rpx; border-radius: 8rpx; margin-right: 12rpx; flex-shrink: 0; }
.title { font-size: 32rpx; font-weight: bold; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.status-badge { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 6rpx; margin-left: 16rpx; white-space: nowrap; }
.status-badge.st-done { background: #f0f9eb; color: #67c23a; }
.status-badge.st-cancel { background: #fef0f0; color: #f56c6c; }
.time { font-size: 24rpx; color: #909399; display: block; margin-bottom: 8rpx; }
.content { font-size: 26rpx; color: #606266; display: -webkit-box; -webkit-line-clamp: 4; -webkit-box-orient: vertical; overflow: hidden; line-height: 1.6; margin-top: 8rpx; }

/* 通用 */
.loading { text-align: center; color: #909399; padding: 120rpx 0; }
.empty { text-align: center; padding: 120rpx 0; }
.empty-text { color: #909399; font-size: 28rpx; }

/* ★手绘主题装饰贴纸 */
.page { position: relative; }
.sticker { position: absolute; opacity: .8; pointer-events: none; z-index: 0; }
.search-bar, .activity-list { position: relative; z-index: 1; }
.sticker-bubble-tr { top: 16rpx; right: 16rpx; width: 56rpx; height: 56rpx; transform: rotate(8deg); }
.sticker-sparkle-tr { top: 40rpx; right: 60rpx; width: 32rpx; height: 32rpx; transform: rotate(-12deg); }
.sticker-bubble-tab-l { top: 100rpx; left: 10rpx; width: 48rpx; height: 48rpx; transform: rotate(-6deg); }
.sticker-star-tab-r { top: 100rpx; right: 10rpx; width: 44rpx; height: 44rpx; transform: rotate(10deg); }
.sticker-bubble-bl { bottom: 120rpx; left: 16rpx; width: 56rpx; height: 56rpx; transform: rotate(-8deg); }
.sticker-star-bl { bottom: 140rpx; left: 70rpx; width: 40rpx; height: 40rpx; transform: rotate(12deg); }
.sticker-cloud-br { bottom: 100rpx; right: 16rpx; width: 80rpx; height: 64rpx; transform: rotate(5deg); }
</style>
