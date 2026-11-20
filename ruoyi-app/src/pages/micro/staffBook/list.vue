<template>
    <view class="page" :class="themeClass" :style="pageStyle">
        <!-- Tab 切换 -->
        <view class="tab-bar">
            <view v-for="tab in tabs" :key="tab.key" class="tab-item" :class="{ active: activeTab === tab.key }" @click="switchTab(tab.key)">
                <text class="tab-text">{{ tab.label }}</text>
                <view v-if="activeTab === tab.key" class="tab-indicator"></view>
            </view>
        </view>

        <!-- 列表 -->
        <scroll-view class="list-scroll" scroll-y @scrolltolower="loadMore">
            <view v-if="!loaded" class="loading">
                <text class="loading-text">加载中…</text>
            </view>
            <view v-else-if="!filteredList.length" class="empty">
                <text class="empty-text">暂无活动记录</text>
            </view>
            <view v-else class="act-list">
                <view v-for="item in filteredList" :key="item.sourceType + '-' + item.bizId" class="act-card">
                    <!-- ★M34 T34.2 活动报名卡片可点进报名确认页 -->
                    <view v-if="item.sourceType === 'ACTIVITY_SIGNUP'" class="confirm-entry" @click.stop="goConfirm(item)">
                        <text class="confirm-entry-text">报名确认 ›</text>
                    </view>
                    <!-- 封面 Banner（纯图，无文字覆盖，与 bossBook/list 一致） -->
                    <view class="card-banner" v-if="item.sourceType === 'ACTIVITY_SIGNUP' && coverUrl(item)">
                        <!-- ★H5 用原生 img（uni-app image 组件在此场景加载失败），与 bossBook/list 修正一致 -->
                        <img class="banner-img" :src="toHttps(coverUrl(item))" style="object-fit:cover;" />
                    </view>

                    <!-- 无封面时的标题 -->
                    <view v-else class="card-title-bar">
                        <text class="card-title-text">{{ item.title || item.serviceName || '-' }}</text>
                        <view class="tag-row" v-if="item.sourceType === 'BOOKING'">
                            <text class="tag" :class="statusTagClass(item)">{{ statusLabel(item) }}</text>
                        </view>
                    </view>

                    <!-- 已报名人数（仅活动报名） -->
                    <view class="section-block" v-if="item.sourceType === 'ACTIVITY_SIGNUP'">
                        <text class="section-label">已报名人数</text>
                        <text class="section-count">{{ item.participantCount || 0 }}</text>
                    </view>

                    <!-- 报名名单（主持人，★头像优先取 hostAvatars，与 bossBook/list 一致） -->
                    <view class="section-block" v-if="item.hostNames">
                        <text class="section-label">报名名单</text>
                        <view class="host-row" v-for="(name, idx) in hostList(item)" :key="idx">
                            <view class="host-avatar">
                                <image v-if="item.hostAvatars && item.hostAvatars[idx]" class="host-avatar-img" :src="toHttps(item.hostAvatars[idx])" mode="aspectFill" />
                                <text v-else class="host-avatar-text">{{ name.charAt(0) }}</text>
                            </view>
                            <view class="host-info">
                                <view class="host-role-tag"><text class="host-role-text">{{ item.roleLabel || '主持' }}</text></view>
                                <text class="host-name">{{ name }}</text>
                            </view>
                        </view>
                    </view>

                    <!-- 预约信息（BOOKING 类型） -->
                    <view class="section-block" v-if="item.sourceType === 'BOOKING'">
                        <view class="info-line">
                            <text class="info-label">时间：</text>
                            <text class="info-value">{{ item.startTime || '-' }}</text>
                        </view>
                        <view v-if="item.amount" class="info-line">
                            <text class="info-label">金额：</text>
                            <text class="info-price">¥{{ formatAmount(item.amount) }}</text>
                        </view>
                    </view>

                    <!-- 名额信息 -->
                    <view class="section-block" v-if="item.totalSeats != null && item.totalSeats > 0">
                        <view class="quota-line">
                            <text class="quota-icon"></text>
                            <text class="quota-text">名额上限：{{ item.totalSeats }}人</text>
                        </view>
                        <view class="quota-line">
                            <text class="quota-icon">🎟️</text>
                            <text class="quota-text">剩余名额：{{ item.totalSeats - (item.currentSeats || 0) }}个</text>
                        </view>
                    </view>

                    <!-- 活动介绍 -->
                    <view class="section-block">
                        <text class="section-label">活动介绍</text>
                        <text class="section-desc">{{ item.startTime || '' }} ~ {{ item.endTime || '' }}</text>
                    </view>
                </view>
            </view>
        </scroll-view>

        <micro-tabbar current="/pages/micro/staffMine/index" />
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get } from '../../../utils/request'
import config from '../../../config'
import { formatAmount, toHttps, firstImage, isActivityEnded } from '../../../utils/format'
import MicroTabbar from '../../../components/micro-tabbar/micro-tabbar.vue'
import themeMixin from '../../../utils/themeMixin'

export default {
    components: { MicroTabbar },
    mixins: [themeMixin],
    data() {
        return {
            loaded: false,
            list: [],
            activeTab: 'ALL',
            tabs: [
                { key: 'ALL', label: '全部' },
                { key: 'ONGOING', label: '进行中' },
                { key: 'ENDED', label: '已结束' }
            ]
        }
    },
    computed: {
        filteredList() {
            if (this.activeTab === 'ALL') return this.list
            return this.list.filter(item => {
                if (this.activeTab === 'ONGOING') {
                    if (item.sourceType === 'BOOKING') {
                        return item.status !== 'COMPLETED' && item.status !== 'CANCELLED'
                    }
                    return !isActivityEnded(item)
                }
                if (this.activeTab === 'ENDED') {
                    if (item.sourceType === 'BOOKING') {
                        return item.status === 'COMPLETED'
                    }
                    return isActivityEnded(item)
                }
                return true
            })
        }
    },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        this.loadList()
    },
    methods: {
        formatAmount,
        toHttps,
        switchTab(key) {
            this.activeTab = key
        },
        /** ★M34 T34.2 跳转员工报名确认页 */
        goConfirm(item) {
            const id = item.bizId || item.activityId || item.id
            if (!id) { uni.showToast({ title: '缺少活动ID', icon: 'none' }); return }
            uni.navigateTo({ url: '/pages/micro/staffBook/confirm?activityId=' + id })
        },
        /** 取 coverUrl 的首张图（正则提取，兼容任意分隔符），与 bossBook/list 一致 */
        coverUrl(item) {
            if (!item.coverUrl) return ''
            return firstImage(item.coverUrl)
        },
        /** 将 hostNames 拆分为数组（支持 "A、B" 格式），与 bossBook/list 一致 */
        hostList(item) {
            if (!item.hostNames) return []
            return item.hostNames.split('、').map(s => s.trim()).filter(Boolean)
        },
        /** 状态标签文案 */
        statusLabel(item) {
            if (item.sourceType === 'BOOKING') {
                const map = { PENDING: '待确认', CONFIRMED: '已确认', IN_PROGRESS: '进行中', COMPLETED: '已完成', CANCELLED: '已取消' }
                return map[item.status] || item.status || '-'
            }
            const map = { DRAFT: '草稿', PUBLISHED: '进行中', ENDED: '已结束' }
            // ★已结束统一口径：status=ENDED / endTime 已过 / endTime 缺失时 startTime 已过
            if (isActivityEnded(item)) {
                return '已结束'
            }
            return map[item.status] || item.status || '-'
        },
        /** 状态标签样式类 */
        statusTagClass(item) {
            const label = this.statusLabel(item)
            if (label === '进行中' || label === '已确认' || label === '待确认') return 'tag-ongoing'
            if (label === '已结束' || label === '已完成') return 'tag-ended'
            if (label === '已取消' || label === '草稿') return 'tag-draft'
            return 'tag-ongoing'
        },
        async loadList() {
            this.loaded = false
            try {
                const res = await get(config.api.microStaffMyActivities)
                this.list = (res && res.data) || []
            } catch (e) { this.list = [] } finally { this.loaded = true }
        },
        loadMore() {}
    }
}
</script>

<style scoped>
/* ★ 页面整体 */
.page {
    min-height: 100vh;
    padding: 0 24rpx 140rpx;
    background: var(--page-bg, #FFF9F0);
    display: flex;
    flex-direction: column;
}

/* ★ Tab 切换栏 */
.tab-bar {
    display: flex;
    align-items: center;
    gap: 48rpx;
    padding: 24rpx 0 20rpx;
    border-bottom: 1rpx solid var(--border, rgba(58,58,58,.08));
    margin-bottom: 24rpx;
}
.tab-item {
    position: relative;
    padding: 8rpx 0 16rpx;
}
.tab-text {
    font-size: 30rpx;
    color: var(--text-sub, #9E9E9E);
    transition: color 0.2s;
}
.tab-item.active .tab-text {
    font-size: 32rpx;
    font-weight: bold;
    color: var(--brand, #FFB347);
}
.tab-indicator {
    position: absolute;
    bottom: 0;
    left: 50%;
    transform: translateX(-50%);
    width: 48rpx;
    height: 6rpx;
    border-radius: 3rpx;
    background: var(--brand, #FFB347);
}

/* ★ 列表滚动区 */
.list-scroll {
    flex: 1;
    min-height: 0;
}

/* ★ 活动卡片 */
.act-list {
    display: flex;
    flex-direction: column;
    gap: 24rpx;
}
.act-card {
    background: var(--card-bg, #FFFFFF);
    border-radius: var(--radius-card, 18px);
    overflow: hidden;
    box-shadow: var(--shadow-card, 0 3px 10px rgba(255,179,71,.14));
    position: relative;
}
/* ★M34 T34.2 报名确认入口角标 */
.confirm-entry {
    position: absolute;
    top: 16rpx;
    right: 16rpx;
    z-index: 2;
    background: rgba(0,0,0,.45);
    border-radius: 24rpx;
    padding: 8rpx 20rpx;
}
.confirm-entry-text {
    font-size: 24rpx;
    color: #FFFFFF;
    font-weight: 600;
}

/* ★ 封面 Banner */
.card-banner {
    position: relative;
    width: 100%;
    height: 360rpx;
}
.banner-img {
    width: 100%;
    height: 100%;
    display: block;
}
/* 无封面时的标题栏 */
.card-title-bar {
    padding: 24rpx 28rpx 16rpx;
    display: flex;
    justify-content: space-between;
    align-items: center;
}
.card-title-text {
    flex: 1;
    font-size: 34rpx;
    font-weight: bold;
    color: var(--text, #3A3A3A);
    margin-right: 12rpx;
}

/* 标签 */
.tag-row {
    flex-shrink: 0;
}
.tag {
    font-size: 22rpx;
    padding: 4rpx 16rpx;
    border-radius: var(--radius-tag, 14rpx);
    white-space: nowrap;
}
.tag-ongoing {
    background: rgba(76,175,80,.12);
    color: #4CAF50;
    font-weight: bold;
}
.tag-ended {
    background: rgba(158,158,158,.12);
    color: #9E9E9E;
}
.tag-draft {
    background: rgba(244,67,54,.10);
    color: #E5544B;
}

/* ★ 内容区块 */
.section-block {
    padding: 20rpx 28rpx;
    border-top: 1rpx solid var(--border, rgba(58,58,58,.06));
}
.section-label {
    font-size: 28rpx;
    font-weight: 600;
    color: var(--text, #3A3A3A);
    display: block;
    margin-bottom: 12rpx;
}
.section-count {
    font-size: 72rpx;
    font-weight: bold;
    color: var(--brand, #FFB347);
    display: block;
    line-height: 1.2;
}
.section-desc {
    font-size: 26rpx;
    color: var(--text-sub, #9E9E9E);
    display: block;
    line-height: 1.6;
}

/* ★ 主持人行 */
.host-row {
    display: flex;
    align-items: center;
    gap: 16rpx;
    margin-top: 8rpx;
}
.host-avatar {
    width: 72rpx;
    height: 72rpx;
    border-radius: 50%;
    background: linear-gradient(135deg, #FFB347, #FF8C42);
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
}
.host-avatar-text {
    font-size: 28rpx;
    color: #FFFFFF;
    font-weight: bold;
}
.host-avatar-img {
    width: 100%;
    height: 100%;
    border-radius: 50%;
}
.host-info {
    display: flex;
    flex-direction: column;
    gap: 6rpx;
}
.host-role-tag {
    background: var(--brand, #FFB347);
    border-radius: 8rpx;
    padding: 2rpx 14rpx;
    display: inline-flex;
    align-self: flex-start;
}
.host-role-text {
    font-size: 20rpx;
    color: #FFFFFF;
    font-weight: bold;
}
.host-name {
    font-size: 26rpx;
    color: var(--text, #3A3A3A);
}

/* ★ BOOKING 信息行 */
.info-line {
    display: flex;
    align-items: center;
    margin-bottom: 10rpx;
}
.info-line:last-child {
    margin-bottom: 0;
}
.info-label {
    font-size: 26rpx;
    color: var(--text-sub, #9E9E9E);
    flex-shrink: 0;
}
.info-value {
    font-size: 26rpx;
    color: var(--text, #3A3A3A);
}
.info-price {
    font-size: 30rpx;
    font-weight: bold;
    color: var(--brand, #FFB347);
}

/* ★ 名额信息 */
.quota-line {
    display: flex;
    align-items: center;
    gap: 10rpx;
    margin-bottom: 10rpx;
}
.quota-line:last-child {
    margin-bottom: 0;
}
.quota-icon {
    font-size: 28rpx;
}
.quota-text {
    font-size: 26rpx;
    color: var(--text, #3A3A3A);
}

/* ★ 加载 / 空状态 */
.loading, .empty {
    text-align: center;
    padding: 120rpx 0;
}
.loading-text, .empty-text {
    color: var(--text-sub, #9E9E9E);
    font-size: 28rpx;
}
</style>
