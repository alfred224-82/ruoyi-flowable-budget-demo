<template>
    <view class="page" :class="themeClass" :style="pageStyle">
        <!-- ★手绘主题装饰贴纸 -->
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-star-tl" :src="iconUrl('hand-star')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-bubble-tr" :src="iconUrl('hand-bubble')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-sparkle-bl" :src="iconUrl('hand-sparkle')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-cloud-br" :src="iconUrl('hand-cloud')" mode="aspectFit" />

        <!-- ═══ 状态过滤 Tab ═══ -->
        <view class="filter-bar">
            <view class="filter-item" :class="{ active: statusFilter === '' }" @click="switchFilter('')">
                <text class="filter-text">全部</text>
            </view>
            <view class="filter-item" :class="{ active: statusFilter === 'ONGOING' }" @click="switchFilter('ONGOING')">
                <text class="filter-text">进行中</text>
            </view>
            <view class="filter-item" :class="{ active: statusFilter === 'ENDED' }" @click="switchFilter('ENDED')">
                <text class="filter-text">已结束</text>
            </view>
        </view>

        <!-- ═══ 活动卡片列表 ═══ -->
        <view v-if="!loaded" class="loading">加载中…</view>
        <view v-else-if="!filteredList.length" class="empty">
            <text class="empty-text">暂无活动记录</text>
            <text class="empty-tip">参与门店活动后将在此显示</text>
        </view>
        <view v-else class="card-list">
            <view v-for="item in filteredList" :key="item.sourceType + '_' + item.bizId"
                  class="card" :class="{ 'card-ended': isEnded(item) }" @click="viewDetail(item)">
                <!-- 封面图 -->
                <view class="card-cover">
                    <image v-if="item.coverUrl && isImageUrl(item.coverUrl)"
                           class="cover-img" :src="toHttps(item.coverUrl)" mode="aspectFill" />
                    <view v-else class="cover-img cover-default" :style="{ background: themeGradient(themeKey) }">
                        <text class="cover-icon">{{ item.sourceType === 'BOOKING' ? '📅' : '🎉' }}</text>
                    </view>
                </view>
                <!-- 信息区 -->
                <view class="card-body">
                    <view class="card-header">
                        <text class="card-title">{{ item.title || '未命名活动' }}</text>
                        <text class="card-status" :class="isEnded(item) ? 'ended' : 'ongoing'">
                            {{ isEnded(item) ? '已结束' : '进行中' }}
                        </text>
                    </view>
                    <text class="card-time">{{ formatTime(item) }}</text>
                    <view class="card-meta">
                        <text v-if="item.perPersonPrice != null" class="meta-item">¥{{ item.perPersonPrice }}/人</text>
                        <text v-if="item.amount != null" class="meta-item">合计 ¥{{ item.amount }}</text>
                        <text v-if="item.participantCount" class="meta-item">{{ item.participantCount }}人参与</text>
                        <text v-if="item.sourceType === 'BOOKING'" class="meta-tag booking">预约</text>
                        <text v-if="item.sourceType === 'ACTIVITY_SIGNUP'" class="meta-tag signup">报名</text>
                    </view>
                    <view class="card-footer">
                        <text class="detail-btn">查看详情 ›</text>
                    </view>
                </view>
            </view>
        </view>

        <!-- ═══ 底部 4 Tab ═══ -->
        <view class="customer-tabbar">
            <view class="tab-item" @click="goHome">
                <text class="tab-icon">🏠</text>
                <text class="tab-text">发现</text>
            </view>
            <view class="tab-item" @click="goPromo">
                <text class="tab-icon">🎫</text>
                <text class="tab-text">我的推广码</text>
            </view>
            <view class="tab-item active">
                <text class="tab-icon">📋</text>
                <text class="tab-text">我的活动</text>
            </view>
            <view class="tab-item" @click="goMine">
                <text class="tab-icon">👤</text>
                <text class="tab-text">我的</text>
            </view>
        </view>
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get } from '../../../utils/request'
import config from '../../../config'
import { toHttps, isImageUrl } from '../../../utils/format'
import { themeGradient } from '../../../utils/theme'
import themeMixin from '../../../utils/themeMixin'
import { iconUrl } from '../../../utils/themeIcons'

export default {
    mixins: [themeMixin],
    data() {
        return {
            loaded: false,
            statusFilter: '',
            allList: []
        }
    },
    computed: {
        /** ★T32.5 本地过滤：全部 / 进行中 / 已结束 */
        filteredList() {
            if (!this.statusFilter) return this.allList
            if (this.statusFilter === 'ENDED') return this.allList.filter(a => this.isEnded(a))
            return this.allList.filter(a => !this.isEnded(a))
        }
    },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        this.loadActivities()
    },
    methods: {
        toHttps,
        isImageUrl,
        iconUrl,
        themeGradient,

        switchFilter(val) {
            this.statusFilter = val
        },

        /** ★T32.5 加载我的活动（合并自助预约 + 活动报名） */
        async loadActivities() {
            this.loaded = false
            try {
                const res = await get(config.api.microCustomerMyActivities)
                this.allList = (res && res.data) || []
            } catch (e) {
                this.allList = []
            }
            this.loaded = true
        },

        isEnded(item) {
            if (!item) return false
            if (item.status === 'ENDED' || item.status === 'COMPLETED' || item.status === 'CANCELLED') return true
            if (item.endTime) return Date.now() > new Date(item.endTime).getTime()
            if (item.startTime) return Date.now() > new Date(item.startTime).getTime()
            return false
        },

        formatTime(item) {
            if (!item.startTime) return ''
            const s = String(item.startTime).replace('T', ' ').substring(0, 16)
            const e = item.endTime ? String(item.endTime).replace('T', ' ').substring(0, 16) : ''
            return e ? s + ' ~ ' + e : s
        },

        /** ★T32.5 查看详情 → 进详情页可改选座或取消报名 */
        viewDetail(item) {
            if (item.sourceType === 'BOOKING') {
                uni.navigateTo({ url: '/pages/micro/customerBook/detail?id=' + item.bizId })
            } else {
                // 活动报名 → 跳顾客端活动详情页
                uni.navigateTo({ url: '/pages/micro/customer/activityDetail?id=' + encodeURIComponent(JSON.stringify({ id: item.bizId })) })
            }
        },

        goHome() { uni.reLaunch({ url: '/pages/micro/customer/home' }) },
        goPromo() { uni.navigateTo({ url: '/pages/micro/customer/myPromoCode' }) },
        goMine() { uni.navigateTo({ url: '/pages/micro/customer/mine' }) }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 140rpx; min-height: 100vh; background: var(--page-bg, #f5f6f8); position: relative; }

/* 过滤 Tab */
.filter-bar { display: flex; background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 8rpx; margin-bottom: 20rpx; position: relative; z-index: 1; }
.filter-item { flex: 1; text-align: center; padding: 14rpx 0; border-radius: var(--radius-btn, 12rpx); }
.filter-item.active { background: var(--brand, #409eff); }
.filter-text { font-size: 26rpx; color: var(--text-sub, #606266); }
.filter-item.active .filter-text { color: #fff; font-weight: bold; }

/* 卡片列表 */
.card-list { position: relative; z-index: 1; }
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); overflow: hidden; margin-bottom: 20rpx; box-shadow: var(--shadow-card, none); display: flex; }
.card-ended { opacity: 0.6; }

/* 封面 */
.card-cover { width: 200rpx; min-height: 240rpx; flex-shrink: 0; }
.cover-img { width: 200rpx; height: 100%; display: block; }
.cover-default { display: flex; align-items: center; justify-content: center; }
.cover-icon { font-size: 64rpx; }

/* 信息区 */
.card-body { flex: 1; padding: 20rpx; display: flex; flex-direction: column; justify-content: space-between; }
.card-header { display: flex; align-items: flex-start; gap: 8rpx; }
.card-title { font-size: 28rpx; font-weight: bold; color: var(--text, #303133); flex: 1; line-height: 1.4; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical; overflow: hidden; }
.card-status { font-size: 22rpx; padding: 4rpx 12rpx; border-radius: var(--radius-tag, 6rpx); flex-shrink: 0; }
.card-status.ongoing { background: #f0f9eb; color: #67c23a; }
.card-status.ended { background: #f4f4f5; color: #909399; }
.card-time { font-size: 24rpx; color: var(--text-sub, #909399); margin-top: 8rpx; }
.card-meta { display: flex; flex-wrap: wrap; gap: 12rpx; margin-top: 8rpx; align-items: center; }
.meta-item { font-size: 24rpx; color: var(--text-sub, #606266); }
.meta-tag { font-size: 20rpx; padding: 2rpx 10rpx; border-radius: var(--radius-tag, 6rpx); }
.meta-tag.booking { background: #ecf5ff; color: #409eff; }
.meta-tag.signup { background: #fdf6ec; color: #e6a23c; }
.card-footer { margin-top: auto; padding-top: 8rpx; }
.detail-btn { font-size: 26rpx; color: var(--brand, #409eff); }

/* 空态/加载 */
.loading, .empty { text-align: center; color: var(--text-sub, #909399); padding: 80rpx 0; position: relative; z-index: 1; }
.empty-text { display: block; font-size: 28rpx; margin-bottom: 12rpx; }
.empty-tip { display: block; font-size: 24rpx; color: var(--text-sub, #c0c4cc); }

/* ★ 底部 4 Tab */
.customer-tabbar {
    position: fixed; left: 0; right: 0; bottom: 0; z-index: 900;
    display: flex;
    background: var(--card-bg, #fff);
    border-top: 1rpx solid var(--border, #e4e7ed);
    padding-bottom: env(safe-area-inset-bottom);
}
.tab-item { flex: 1; text-align: center; padding: 12rpx 0 14rpx; }
.tab-icon { font-size: 40rpx; display: block; line-height: 1.2; }
.tab-text { font-size: 22rpx; color: var(--text-sub, #909399); display: block; margin-top: 4rpx; }
.tab-item.active .tab-text { color: var(--brand, #409eff); font-weight: bold; }

/* ★手绘主题装饰贴纸 */
.sticker { position: absolute; opacity: .8; pointer-events: none; z-index: 0; }
.sticker-star-tl { top: 16rpx; left: 16rpx; width: 52rpx; height: 52rpx; transform: rotate(-10deg); }
.sticker-bubble-tr { top: 10rpx; right: 16rpx; width: 56rpx; height: 56rpx; transform: rotate(8deg); }
.sticker-sparkle-bl { bottom: 160rpx; left: 16rpx; width: 36rpx; height: 36rpx; transform: rotate(15deg); }
.sticker-cloud-br { bottom: 160rpx; right: 16rpx; width: 72rpx; height: 56rpx; transform: rotate(-5deg); }
</style>
