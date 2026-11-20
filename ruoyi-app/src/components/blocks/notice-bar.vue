<template>
    <view class="block notice-bar" v-if="noticeText" @click="goNotice">
        <text class="notice-icon">📢</text>
        <view class="notice-content">
            <text class="notice-text">{{ noticeText }}</text>
        </view>
        <text class="notice-badge" v-if="unreadCount > 0">{{ unreadCount > 99 ? '99+' : unreadCount }}</text>
        <text class="notice-arrow" v-else>›</text>
    </view>
</template>

<script>
/**
 * 区块组件：通知栏（T15.16，blockKey=notice_bar）
 * 数据源：GET /micro/notify/unreadCount + 最新一条通知
 */
import { get } from '../../utils/request'
import config from '../../config'

export default {
    name: 'BlockNoticeBar',
    props: { blockParams: { type: Object, default: () => ({}) } },
    data() { return { unreadCount: 0, latestNotice: '' } },
    computed: {
        noticeText() {
            if (this.unreadCount > 0) {
                return (this.latestNotice || '您有新的消息') + '（' + this.unreadCount + '条未读）'
            }
            return this.latestNotice || ''
        }
    },
    created() { this.loadData() },
    methods: {
        goNotice() {
            uni.navigateTo({ url: '/pages/micro/mine/index' })
        },
        async loadData() {
            try {
                const [unreadRes, listRes] = await Promise.all([
                    get(config.api.microNotifyUnread),
                    get(config.api.microNotifyList, { pageNum: 1, pageSize: 1 })
                ])
                this.unreadCount = (unreadRes && unreadRes.data) || 0
                const rows = (listRes && listRes.rows) || []
                this.latestNotice = rows.length ? (rows[0].messageContent || rows[0].title || '') : ''
            } catch (e) { /* 静默降级 */ }
        }
    }
}
</script>

<style scoped>
.block { background: #fff; border-radius: 16rpx; padding: 18rpx 24rpx; margin-bottom: 20rpx; display: flex; align-items: center; }
.notice-icon { font-size: 32rpx; margin-right: 12rpx; }
.notice-content { flex: 1; overflow: hidden; }
.notice-text { font-size: 26rpx; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: block; }
.notice-badge { background: #f56c6c; color: #fff; font-size: 20rpx; min-width: 36rpx; height: 36rpx; line-height: 36rpx; text-align: center; border-radius: 18rpx; padding: 0 10rpx; margin-left: 12rpx; }
.notice-arrow { font-size: 32rpx; color: #c0c4cc; margin-left: 12rpx; }
</style>
