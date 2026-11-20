<template>
    <view class="page" :class="'theme-' + theme.key" :style="rootStyle">
        <view class="hero" :style="{ background: heroGradient }">
            <text class="hero-title" :style="{ color: heroText.text }">报表中心</text>
            <text class="hero-sub" :style="{ color: heroText.sub }">活动明细 · 经营日报 · 月度核算，一处看清每场赚多少</text>
        </view>

        <!-- 共用月份筛选（下钻时带入目标页） -->
        <view class="card filter-card">
            <view class="filter-row">
                <text class="filter-label">统计月份</text>
                <picker mode="date" fields="month" :value="month" @change="onMonthChange">
                    <view class="picker">{{ month }} ▾</view>
                </picker>
            </view>
        </view>

        <!-- 三张报表入口 -->
        <view class="entry" @click="goReport('detail')">
            <view class="entry-icon" :style="{ background: theme.brandWeak }">📋</view>
            <view class="entry-main">
                <text class="entry-title">活动明细表</text>
                <text class="entry-desc">一行一场，收入/成本/毛利/差额，底部固定合计</text>
            </view>
            <text class="entry-arrow">›</text>
        </view>
        <view class="entry" @click="goReport('daily')">
            <view class="entry-icon" :style="{ background: theme.brandWeak }">📅</view>
            <view class="entry-main">
                <text class="entry-title">经营日报</text>
                <text class="entry-desc">当日汇总 + 本月保本进度，天天心里有数</text>
            </view>
            <text class="entry-arrow">›</text>
        </view>
        <view class="entry" @click="goReport('monthly')">
            <view class="entry-icon" :style="{ background: theme.brandWeak }">📊</view>
            <view class="entry-main">
                <text class="entry-title">月度经营报表</text>
                <text class="entry-desc">月毛利/净利/税费/保本线，支持月结锁定</text>
            </view>
            <text class="entry-arrow">›</text>
        </view>

        <text class="tip">数据源为已结算活动，代收金额不计入收入与利润。报表仅供经营参考，不作为税务申报依据。</text>
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { currentPeriod } from '../../../utils/format'
import { getThemeSync, loadTenantTheme, themeGradient, themeGradientText, themeCssVars } from '../../../utils/theme'
import { applyNavBarTitle } from '../../../utils/roles'

const REPORT_PATH = {
    detail: '/pages/micro/report/activityDetail',
    daily: '/pages/micro/report/daily',
    monthly: '/pages/micro/report/monthly'
}

export default {
    data() {
        return {
            theme: getThemeSync(),
            month: currentPeriod()
        }
    },
    computed: {
        rootStyle() { return themeCssVars(this.theme.key) },
        heroGradient() { return themeGradient(this.theme.key) },
        heroText() { return themeGradientText(this.theme.key) }
    },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        applyNavBarTitle()
        this.theme = getThemeSync()
        loadTenantTheme().then(t => { this.theme = t })
    },
    methods: {
        onMonthChange(e) { this.month = e.detail.value },
        goReport(tab) {
            const url = REPORT_PATH[tab]
            if (!url) return
            // 日报按日、明细/月报按月带入筛选
            const q = tab === 'daily' ? ('?date=' + this.month + '-01') : ('?month=' + this.month)
            uni.navigateTo({ url: url + q })
        }
    }
}
</script>

<style scoped>
.page { min-height: 100vh; box-sizing: border-box; padding: 24rpx 24rpx 60rpx; background: var(--page-bg, #f5f6f8); }
.hero { position: relative; overflow: hidden; padding: 40rpx 36rpx; border-radius: var(--radius-card, 20rpx); margin-bottom: 24rpx; box-shadow: var(--shadow-card, 0 4rpx 16rpx rgba(0,0,0,.08)); }
.hero-title { display: block; font-size: 40rpx; font-weight: bold; }
.hero-sub { display: block; font-size: 24rpx; margin-top: 12rpx; }
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 24rpx 28rpx; margin-bottom: 20rpx; box-shadow: var(--shadow-card, 0 2rpx 12rpx rgba(0,0,0,.04)); }
.filter-row { display: flex; align-items: center; justify-content: space-between; }
.filter-label { font-size: 28rpx; color: var(--text, #303133); }
.picker { font-size: 28rpx; color: var(--brand, #409eff); }
.entry { display: flex; align-items: center; background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 28rpx; margin-bottom: 20rpx; box-shadow: var(--shadow-card, 0 2rpx 12rpx rgba(0,0,0,.04)); }
.entry-icon { width: 88rpx; height: 88rpx; border-radius: 20rpx; display: flex; align-items: center; justify-content: center; font-size: 44rpx; margin-right: 24rpx; flex-shrink: 0; }
.entry-main { flex: 1; overflow: hidden; }
.entry-title { display: block; font-size: 32rpx; font-weight: bold; color: var(--text, #303133); }
.entry-desc { display: block; font-size: 24rpx; color: var(--text-sub, #909399); margin-top: 8rpx; }
.entry-arrow { font-size: 44rpx; color: var(--text-sub, #c0c4cc); margin-left: 12rpx; }
.tip { display: block; font-size: 22rpx; color: var(--text-sub, #909399); line-height: 1.6; padding: 12rpx 8rpx; }
</style>
