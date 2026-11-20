<template>
    <!-- ★M33 T33.1 经营首页（§7.1）
         问候语 → 今日收入/支出/净利（实时汇总，净利=收入-支出）
         → 本月成本构成（按月分摊后占比，读取 M38 固定成本设置按类别分组）
         → 「月度报表」入口（M38 上线后）
         ★数据源：/micro/bossDashboard（与 bossHome/index.vue 同源，本页按 §7.1 文档口径独立路由） -->
    <view class="page" :class="'theme-' + theme.key" :style="rootStyle">
        <!-- 手绘主题装饰贴纸 -->
        <image v-if="theme.key === 'theme_handdrawn'" class="sticker sticker-star-tl" :src="iconUrl('hand-star')" mode="aspectFit" />
        <image v-if="theme.key === 'theme_handdrawn'" class="sticker sticker-bubble-welcome" :src="iconUrl('hand-bubble')" mode="aspectFit" />

        <!-- 欢迎区 + 主题联动背景 -->
        <view class="welcome-bar" :style="{ background: welcomeGradient }">
            <view class="welcome-deco deco-1"></view>
            <view class="welcome-deco deco-2"></view>
            <view class="welcome-content">
                <text class="welcome-text" :style="{ color: welcomeText.text }">今天也是生意兴隆的一天哦</text>
                <text class="welcome-sub" :style="{ color: welcomeText.sub }">{{ greeting }}，{{ tenantName }} {{ userName }}</text>
            </view>
        </view>

        <template v-if="isPro">
            <!-- 今日经营数据：收入/支出 并列 + 净利 通栏 -->
            <text class="section-title">今日经营数据</text>
            <view class="stat-grid">
                <view class="stat-card">
                    <text class="stat-label">收入</text>
                    <text class="stat-num c-income">¥{{ formatAmount(dash.todayReceived != null ? dash.todayReceived : dash.todayReceivable || 0) }}</text>
                    <text v-if="dash.todayReceivable != null" class="stat-sub">应收 ¥{{ formatAmount(dash.todayReceivable || 0) }}</text>
                </view>
                <view class="stat-card">
                    <text class="stat-label">支出</text>
                    <text class="stat-num c-expense">¥{{ formatAmount(dash.todayExpense || 0) }}</text>
                </view>
                <view class="stat-card full">
                    <text class="stat-label">净利（收入 − 支出）</text>
                    <text class="stat-num" :style="{ color: (dash.todayNetProfit || 0) >= 0 ? theme.primaryColor : '#f56c6c' }">¥{{ formatAmount(dash.todayNetProfit || 0) }}</text>
                </view>
            </view>

            <!-- 本月成本构成（按月分摊后占比，读取 M38 固定成本设置按类别分组） -->
            <view class="card cost-card">
                <view class="card-head">
                    <text class="card-title no-mb">本月成本构成（月分摊）</text>
                    <text class="link" :style="{ color: theme.primaryColor }" @click="goMonthlyReport">月度报表 ›</text>
                </view>
                <view v-if="!costComposition.length" class="empty">暂无成本数据</view>
                <view v-for="item in costComposition" :key="item.category" class="cost-row">
                    <text class="cost-name">{{ item.name || item.category }}</text>
                    <view class="cost-bar">
                        <view class="cost-bar-inner" :style="{ width: (item.percent || 0) + '%', background: theme.primaryColor || '#409eff' }"></view>
                    </view>
                    <text class="cost-amount">¥{{ formatAmount(item.amount || 0) }}（{{ item.percent || 0 }}%）</text>
                </view>
            </view>

            <!-- ★M38 上线后：月度报表入口卡片（固定成本设置 + 月报） -->
            <view class="card entry-card" @click="goMonthlyReport">
                <view class="entry-row">
                    <view class="entry-icon">📊</view>
                    <view class="entry-main">
                        <text class="entry-title">月度报表</text>
                        <text class="entry-desc">月毛利 / 固定成本 / 月净利润 / 保本线</text>
                    </view>
                    <text class="entry-arrow">›</text>
                </view>
            </view>

            <!-- ★M33 §九 锁定申请审批入口（FEAT_ACTIVITY_LOCK 开启时展示，含待审批数量徽标） -->
            <view v-if="activityLockOn" class="card entry-card" @click="goLockRequests">
                <view class="entry-row">
                    <view class="entry-icon">🔒</view>
                    <view class="entry-main">
                        <text class="entry-title">锁定申请审批</text>
                        <text class="entry-desc">员工申请锁定活动，由老板把关审批</text>
                    </view>
                    <view v-if="lockPendingCount > 0" class="entry-badge" :style="{ background: theme.primaryColor }">{{ lockPendingCount > 99 ? '99+' : lockPendingCount }}</view>
                    <text class="entry-arrow">›</text>
                </view>
            </view>
        </template>

        <!-- 免费版升级引导 -->
        <view v-if="!isPro" class="card upgrade" :style="{ borderLeftColor: theme.primaryColor }">
            <text class="card-title">当前版本：免费档</text>
            <text class="desc">支持活动发布与推广管理，升级付费档可解锁经营驾驶舱、月度报表与异常处理等全经营功能</text>
        </view>

        <micro-tabbar current="/pages/micro/boss/dashboard" />
    </view>
</template>

<script>
import { getToken, getVersionType } from '../../../utils/auth'
import { get } from '../../../utils/request'
import config from '../../../config'
import { formatAmount } from '../../../utils/format'
import { getTenantName, applyNavBarTitle, getSelectedTenant } from '../../../utils/roles'
import { hasFeature } from '../../../utils/features'
import { getThemeSync, loadTenantTheme, themeGradient, themeGradientText, themeCssVars } from '../../../utils/theme'
import { iconUrl } from '../../../utils/themeIcons'
import MicroTabbar from '../../../components/micro-tabbar/micro-tabbar.vue'

export default {
    components: { MicroTabbar },
    data() {
        return {
            tenantName: '',
            userName: '',
            theme: getThemeSync(),
            isPro: false,
            dash: {},
            costComposition: [],
            activityLockOn: false,
            lockPendingCount: 0
        }
    },
    computed: {
        rootStyle() { return themeCssVars(this.theme.key) },
        welcomeGradient() { return themeGradient(this.theme.key) },
        welcomeText() { return themeGradientText(this.theme.key) },
        greeting() {
            const h = new Date().getHours()
            if (h < 6) return '夜深了'
            if (h < 9) return '早上好'
            if (h < 12) return '上午好'
            if (h < 14) return '中午好'
            if (h < 18) return '下午好'
            return '晚上好'
        }
    },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        applyNavBarTitle()
        this.loadData()
    },
    methods: {
        formatAmount,
        iconUrl,
        async loadData() {
            this.tenantName = getTenantName()
            let vt = getVersionType()
            if (!vt) {
                const sel = getSelectedTenant()
                vt = (sel && sel.versionType) || ''
            }
            this.isPro = vt === 'micro_pro'
            this.theme = getThemeSync()
            loadTenantTheme().then(t => { this.theme = t })
            this.loadProfile()
            if (this.isPro) this.loadDashboard()
            // ★M33 §九 锁定申请审批入口（开关开启才展示 + 拉待审批数量）
            this.activityLockOn = hasFeature('FEAT_ACTIVITY_LOCK')
            if (this.activityLockOn) this.loadLockPending()
        },
        async loadDashboard() {
            try {
                const res = await get(config.api.microBossDashboard)
                this.dash = (res && res.data) || {}
                this.costComposition = this.dash.costComposition || []
            } catch (e) {
                this.dash = {}
                this.costComposition = []
            }
        },
        async loadProfile() {
            try {
                const res = await get(config.api.microProfile)
                this.userName = ((res && res.data) || {}).nickName || ''
            } catch (e) { /* ignore */ }
        },
        /** ★月度报表入口：M38 上线后跳转月报页；未上线时提示 */
        goMonthlyReport() {
            uni.navigateTo({
                url: '/pages/micro/report/index',
                fail: () => uni.showToast({ title: '月度报表建设中', icon: 'none' })
            })
        },
        /** ★M33 §九 锁定申请审批入口 */
        goLockRequests() {
            uni.navigateTo({ url: '/pages/micro/boss/lockRequests' })
        },
        /** 拉取待审批锁定申请数量（入口徽标，失败静默归零） */
        async loadLockPending() {
            try {
                const res = await get(config.api.microBossLockRequestList, { status: 'PENDING' })
                const rows = (res && res.data) || []
                this.lockPendingCount = rows.length
            } catch (e) {
                this.lockPendingCount = 0
            }
        }
    }
}
</script>

<style scoped>
.page { min-height: 100vh; box-sizing: border-box; padding: 20rpx 24rpx 140rpx; display: flex; flex-direction: column; background: var(--page-bg, #f5f6f8); position: relative; }
.welcome-bar { flex-shrink: 0; position: relative; overflow: hidden; padding: 28rpx 32rpx; border-radius: 24rpx; margin-bottom: 16rpx; box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.08); z-index: 1; }
.welcome-deco { position: absolute; border-radius: 50%; background: rgba(255, 255, 255, 0.14); }
.welcome-deco.deco-1 { width: 220rpx; height: 220rpx; top: -80rpx; right: -60rpx; }
.welcome-deco.deco-2 { width: 140rpx; height: 140rpx; bottom: -50rpx; right: 120rpx; background: rgba(255, 255, 255, 0.1); }
.welcome-content { position: relative; }
.welcome-text { font-size: 34rpx; font-weight: bold; display: block; }
.welcome-sub { font-size: 24rpx; display: block; margin-top: 8rpx; }

.section-title { flex-shrink: 0; display: block; font-size: 30rpx; font-weight: bold; color: var(--text, #303133); margin: 4rpx 0 12rpx; }
.stat-grid { flex-shrink: 0; display: flex; flex-wrap: wrap; justify-content: space-between; }
.stat-card { width: 48.5%; box-sizing: border-box; background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 20rpx 24rpx; margin-bottom: 14rpx; box-shadow: var(--shadow-card, 0 2rpx 12rpx rgba(0, 0, 0, 0.04)); position: relative; z-index: 1; }
.stat-card.full { width: 100%; }
.stat-label { display: block; font-size: 24rpx; color: var(--text-sub, #606266); }
.stat-num { display: block; font-size: 40rpx; font-weight: bold; margin-top: 8rpx; }
.stat-num.c-income { color: #67c23a; }
.stat-num.c-expense { color: #f56c6c; }
.stat-sub { display: block; font-size: 24rpx; color: var(--brand, #409eff); margin-top: 6rpx; }

.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 24rpx 32rpx; margin-bottom: 20rpx; box-shadow: var(--shadow-card, 0 2rpx 12rpx rgba(0, 0, 0, 0.04)); position: relative; z-index: 1; }
.card.upgrade { border-left: 8rpx solid var(--brand, #409eff); }
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16rpx; }
.card-title { font-size: 30rpx; font-weight: bold; display: block; margin-bottom: 16rpx; color: var(--text, #303133); }
.card-title.no-mb { margin-bottom: 0; }
.link { font-size: 24rpx; }
.desc { font-size: 26rpx; color: #909399; }

.cost-row { display: flex; align-items: center; padding: 10rpx 0; }
.cost-name { width: 140rpx; font-size: 24rpx; color: var(--text-sub, #606266); flex-shrink: 0; }
.cost-bar { flex: 1; height: 16rpx; background: var(--brand-weak, #f0f2f5); border-radius: 8rpx; overflow: hidden; margin: 0 16rpx; }
.cost-bar-inner { height: 100%; border-radius: 8rpx; }
.cost-amount { width: 220rpx; text-align: right; font-size: 22rpx; color: var(--text-sub, #909399); flex-shrink: 0; }
.empty { text-align: center; color: var(--text-sub, #909399); font-size: 26rpx; padding: 32rpx 0; }

.entry-card { padding: 24rpx 32rpx; }
.entry-row { display: flex; align-items: center; }
.entry-icon { width: 72rpx; height: 72rpx; border-radius: 18rpx; background: var(--brand-weak, #e8f1fb); display: flex; align-items: center; justify-content: center; font-size: 36rpx; margin-right: 20rpx; flex-shrink: 0; }
.entry-main { flex: 1; overflow: hidden; }
.entry-title { display: block; font-size: 30rpx; font-weight: bold; color: var(--text, #1a1a1a); }
.entry-desc { display: block; font-size: 24rpx; color: var(--text-sub, #909399); margin-top: 6rpx; }
.entry-arrow { font-size: 40rpx; color: #c0c4cc; flex-shrink: 0; }
.entry-badge { flex-shrink: 0; min-width: 36rpx; height: 36rpx; line-height: 36rpx; padding: 0 10rpx; box-sizing: border-box; border-radius: 18rpx; color: #fff; font-size: 22rpx; text-align: center; margin-right: 12rpx; }

.sticker { position: absolute; opacity: .8; pointer-events: none; z-index: 0; }
.sticker-star-tl { top: 10rpx; left: 10rpx; width: 48rpx; height: 48rpx; transform: rotate(-10deg); }
.sticker-bubble-welcome { top: 16rpx; right: 16rpx; width: 52rpx; height: 52rpx; transform: rotate(8deg); }
</style>
