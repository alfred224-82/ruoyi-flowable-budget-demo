<template>
    <view class="page" :class="themeClass" :style="pageStyle">
        <!-- 页面标题 -->
        <view class="page-header">
            <text class="page-title">我的活动</text>
        </view>

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
                <view v-for="item in filteredList" :key="item.sourceType + '-' + item.bizId" class="act-card" @click="goDetail(item)">
                    <!-- 卡片头部：标题 + 标签 -->
                    <view class="card-top">
                        <text class="act-title">{{ buildTitle(item) }}</text>
                        <view class="tag-row">
                            <text v-if="item.sourceType === 'ACTIVITY_SIGNUP'" class="tag tag-activity">活动报名</text>
                            <text v-if="item.isLocked" class="tag tag-locked">已锁定</text>
                            <text class="tag" :class="statusTagClass(item)">{{ statusLabel(item) }}</text>
                        </view>
                    </view>

                    <!-- 卡片信息 -->
                    <view class="card-info">
                        <view class="info-line">
                            <text class="info-label">时间：</text>
                            <text class="info-value">{{ item.startTime || '-' }}</text>
                        </view>
                        <!-- 活动报名：人均费用 -->
                        <view v-if="item.sourceType === 'ACTIVITY_SIGNUP' && item.perPersonPrice != null" class="info-line">
                            <text class="info-label">人均：</text>
                            <text class="info-price">¥{{ item.perPersonPrice }}</text>
                        </view>
                        <!-- 活动报名：参与人数 -->
                        <view v-if="item.sourceType === 'ACTIVITY_SIGNUP' && item.participantCount" class="info-line">
                            <text class="info-label">参与人数：</text>
                            <text class="info-value">{{ item.participantCount }}人</text>
                        </view>
                        <!-- 自助预约：金额 -->
                        <view v-if="item.sourceType === 'BOOKING' && item.amount" class="info-line">
                            <text class="info-label">金额：</text>
                            <text class="info-price">¥{{ formatAmount(item.amount) }}</text>
                        </view>
                        <!-- 座位信息 -->
                        <view v-if="item.totalSeats != null && item.totalSeats > 0" class="info-line">
                            <text class="info-label">座位：</text>
                            <text class="info-value">{{ item.currentSeats || 0 }}/{{ item.totalSeats }}</text>
                            <text v-if="item.currentSeats != null" class="info-remain">（剩余 {{ item.totalSeats - item.currentSeats }} 座）</text>
                        </view>
                    </view>

                    <!-- 卡片底部 -->
                    <view class="card-footer">
                        <view v-if="item.sourceType === 'BOOKING' && item.status === 'PENDING_SERVICE'" class="cancel-wrap" @click.stop="cancelBook(item.bizId)">
                            <text class="cancel-btn">取消预约</text>
                        </view>
                        <view class="detail-wrap">
                            <text class="detail-btn">查看详情</text>
                            <text class="detail-arrow">›</text>
                        </view>
                    </view>
                </view>
            </view>
        </scroll-view>

        <micro-tabbar current="/pages/micro/customerBook/list" />
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get, put } from '../../../utils/request'
import config from '../../../config'
import { formatAmount, BOOKING_STATUS, isActivityEnded } from '../../../utils/format'
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
        switchTab(key) {
            this.activeTab = key
        },
        /** 构建标题：活动标题 + 主持人信息 */
        buildTitle(item) {
            if (item.sourceType === 'ACTIVITY_SIGNUP' && item.hostNames) {
                return `${item.title || '-'}，由${item.hostNames}主持`
            }
            return item.title || item.serviceName || '-'
        },
        /** 状态标签文案 */
        statusLabel(item) {
            if (item.sourceType === 'BOOKING') {
                const map = { PENDING: '待确认', CONFIRMED: '已确认', IN_PROGRESS: '进行中', COMPLETED: '已完成', CANCELLED: '已取消', PENDING_SERVICE: '待服务' }
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
            if (label === '进行中' || label === '已确认' || label === '待确认' || label === '待服务') return 'tag-ongoing'
            if (label === '已结束' || label === '已完成') return 'tag-ended'
            if (label === '已取消' || label === '草稿') return 'tag-draft'
            return 'tag-ongoing'
        },
        async loadList() {
            this.loaded = false
            try {
                const res = await get(config.api.microCustomerMyActivities)
                this.list = ((res && res.data) || []).map(x => {
                    if (x.sourceType === 'BOOKING') {
                        x.orderStatus = x.status
                        x.cost = null
                    }
                    return x
                })
            } catch (e) { this.list = [] } finally { this.loaded = true }
        },
        loadMore() {},
        goDetail(item) {
            if (item.sourceType === 'BOOKING') {
                uni.navigateTo({ url: '/pages/micro/customerBook/detail?id=' + item.bizId })
            } else {
                // ★不要手动 encodeURIComponent，uni-app navigateTo 会自动编码
                uni.navigateTo({ url: '/pages/micro/activity/detail?id=' + JSON.stringify(item) })
            }
        },
        cancelBook(id) {
            uni.showModal({
                title: '取消预约', content: '确认取消该预约？',
                success: async (r) => {
                    if (r.confirm) {
                        try {
                            await put(config.api.microCustomerCancelBook + '/' + id)
                            uni.showToast({ title: '已取消', icon: 'success' })
                            this.loadList()
                        } catch (e) { /* 已统一提示 */ }
                    }
                }
            })
        }
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

/* ★ 页面标题 */
.page-header {
    padding: 32rpx 0 16rpx;
}
.page-title {
    font-size: 44rpx;
    font-weight: bold;
    color: var(--text, #3A3A3A);
}

/* ★ Tab 切换栏 */
.tab-bar {
    display: flex;
    align-items: center;
    gap: 48rpx;
    padding: 16rpx 0 20rpx;
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
    padding: 32rpx;
    box-shadow: var(--shadow-card, 0 3px 10px rgba(255,179,71,.14));
    transition: transform 0.15s;
}
.act-card:active {
    transform: scale(0.98);
}

/* 卡片头部 */
.card-top {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    margin-bottom: 20rpx;
}
.act-title {
    flex: 1;
    font-size: 32rpx;
    font-weight: bold;
    color: var(--text, #3A3A3A);
    line-height: 1.4;
    margin-right: 16rpx;
}

/* 标签行 */
.tag-row {
    display: flex;
    align-items: center;
    gap: 12rpx;
    flex-shrink: 0;
}
.tag {
    font-size: 22rpx;
    padding: 4rpx 16rpx;
    border-radius: var(--radius-tag, 14rpx);
    white-space: nowrap;
}
.tag-activity {
    background: rgba(255,179,71,.12);
    color: #E09A30;
}
.tag-locked {
    background: rgba(244,67,54,.10);
    color: #E5544B;
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

/* 卡片信息区 */
.card-info {
    padding: 16rpx 0;
    border-top: 1rpx solid var(--border, rgba(58,58,58,.06));
    border-bottom: 1rpx solid var(--border, rgba(58,58,58,.06));
}
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
.info-remain {
    font-size: 24rpx;
    color: var(--brand, #FFB347);
    margin-left: 8rpx;
}

/* 卡片底部 */
.card-footer {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-top: 16rpx;
}
.cancel-wrap {
    flex: 1;
}
.cancel-btn {
    font-size: 24rpx;
    color: #f56c6c;
    background: #fff;
    border: 2rpx solid #f56c6c;
    border-radius: 8rpx;
    padding: 8rpx 24rpx;
}
.detail-wrap {
    display: flex;
    align-items: center;
    gap: 4rpx;
    flex-shrink: 0;
}
.detail-btn {
    font-size: 28rpx;
    color: var(--brand, #FFB347);
    font-weight: 600;
}
.detail-arrow {
    font-size: 32rpx;
    color: var(--brand, #FFB347);
    font-weight: bold;
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
