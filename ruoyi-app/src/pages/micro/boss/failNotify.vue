<template>
    <!-- ★M33 T33.9 预约失败通知（§7.9）
         失败记录列表 → 无记录显示空态「暂无预约失败通知」
         ★数据源：/micro/notify/list?messageType=RESERVATION_FAILED -->
    <view class="page" :class="'theme-' + theme.key" :style="rootStyle">
        <view class="card">
            <view class="head">
                <text class="title">预约失败通知</text>
                <text class="sub">共 {{ total }} 条 · 仅自助预约开通后产生</text>
            </view>
            <view v-if="!selfBookingOn" class="empty">自助预约功能未开通，升级版本后可查看预约失败通知</view>
            <view v-else-if="!loaded" class="empty">加载中…</view>
            <view v-else-if="!list.length" class="empty">暂无预约失败通知</view>
            <view v-else class="list">
                <view v-for="msg in list" :key="msg.id" class="row">
                    <view class="main">
                        <text class="name">{{ msg.title || '预约失败' }}</text>
                        <text class="content">{{ msg.content || '' }}</text>
                        <text class="time">{{ formatTime(msg.createTime) }}</text>
                    </view>
                    <button class="btn-sm success" @click="rebook(msg)">重新预约</button>
                </view>
            </view>
            <view v-if="hasMore" class="more" @click="loadMore">加载更多</view>
        </view>
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get } from '../../../utils/request'
import config from '../../../config'
import { getThemeSync, loadTenantTheme, themeCssVars } from '../../../utils/theme'
import { hasFeature } from '../../../utils/features'

export default {
    data() {
        return {
            theme: getThemeSync(),
            selfBookingOn: false,
            loaded: false,
            list: [],
            total: 0,
            pageNum: 1,
            pageSize: 20
        }
    },
    computed: {
        rootStyle() { return themeCssVars(this.theme.key) },
        hasMore() { return this.list.length < this.total }
    },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        this.theme = getThemeSync()
        loadTenantTheme().then(t => { this.theme = t })
        this.selfBookingOn = hasFeature('FEAT_SELF_BOOKING')
        this.pageNum = 1
        this.load()
    },
    methods: {
        formatTime(t) {
            if (!t) return ''
            const d = new Date(typeof t === 'string' ? t.replace(/-/g, '/') : t)
            if (isNaN(d.getTime())) return String(t).substring(0, 16)
            const p = n => String(n).padStart(2, '0')
            return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate()) + ' ' + p(d.getHours()) + ':' + p(d.getMinutes())
        },
        async load() {
            if (!this.selfBookingOn) { this.loaded = true; return }
            try {
                const r = await get(config.api.microNotifyList, {
                    messageType: 'RESERVATION_FAILED',
                    pageNum: this.pageNum,
                    pageSize: this.pageSize
                })
                const rows = (r && r.rows) || []
                this.total = (r && r.total) || 0
                this.list = this.pageNum === 1 ? rows : this.list.concat(rows)
            } catch (e) { if (this.pageNum === 1) this.list = [] }
            this.loaded = true
        },
        loadMore() { this.pageNum += 1; this.load() },
        rebook(msg) {
            const bizId = msg.bizId || ''
            uni.navigateTo({ url: '/pages/micro/booking/add?rebookId=' + bizId })
        }
    }
}
</script>

<style scoped>
.page { min-height: 100vh; padding: 24rpx; background: var(--page-bg, #f5f6f8); box-sizing: border-box; }
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 24rpx); padding: 32rpx; box-shadow: var(--shadow-card, 0 4rpx 20rpx rgba(0, 0, 0, 0.05)); }
.head { margin-bottom: 24rpx; }
.title { display: block; font-size: 34rpx; font-weight: bold; color: var(--text, #1a1a1a); }
.sub { display: block; font-size: 24rpx; color: var(--text-sub, #909399); margin-top: 8rpx; }
.empty { text-align: center; color: var(--text-sub, #909399); font-size: 26rpx; padding: 60rpx 0; }
.list { display: flex; flex-direction: column; }
.row { display: flex; align-items: flex-end; justify-content: space-between; padding: 24rpx 0; border-bottom: 1rpx solid var(--border, #f5f6f8); }
.row:last-child { border-bottom: none; }
.main { flex: 1; display: flex; flex-direction: column; overflow: hidden; margin-right: 16rpx; }
.name { font-size: 28rpx; color: var(--text, #303133); font-weight: 600; margin-bottom: 8rpx; }
.content { font-size: 24rpx; color: var(--text-sub, #606266); margin-bottom: 6rpx; }
.time { font-size: 22rpx; color: var(--text-sub, #909399); }
.btn-sm { font-size: 24rpx; height: 56rpx; line-height: 56rpx; padding: 0 24rpx; border-radius: 28rpx; border: none; margin: 0; flex-shrink: 0; }
.btn-sm::after { border: none; }
.btn-sm.success { background: #67c23a; color: #fff; }
.more { text-align: center; font-size: 24rpx; color: var(--brand, #409eff); padding: 24rpx 0 8rpx; }
</style>
