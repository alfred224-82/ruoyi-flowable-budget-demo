<template>
    <view class="page" :class="'theme-' + theme.key" :style="rootStyle">
        <!-- ★手绘主题装饰贴纸（仅 theme_handdrawn 显示） -->
        <image v-if="theme.key === 'theme_handdrawn'" class="sticker sticker-star-tl" :src="iconUrl('hand-star')" mode="aspectFit" />
        <image v-if="theme.key === 'theme_handdrawn'" class="sticker sticker-sparkle-tl" :src="iconUrl('hand-sparkle')" mode="aspectFit" />
        <image v-if="theme.key === 'theme_handdrawn'" class="sticker sticker-bubble-welcome" :src="iconUrl('hand-bubble')" mode="aspectFit" />
        <image v-if="theme.key === 'theme_handdrawn'" class="sticker sticker-cloud-welcome" :src="iconUrl('hand-cloud')" mode="aspectFit" />
        <image v-if="theme.key === 'theme_handdrawn'" class="sticker sticker-cloud-profit" :src="iconUrl('hand-cloud')" mode="aspectFit" />
        <!-- T15.36 欢迎区 + 主题联动背景（★v1.43 美化：租户主题渐变 + 装饰光斑 + 双行文案） -->
        <view class="welcome-bar" :style="{ background: welcomeGradient }">
            <view class="welcome-deco deco-1"></view>
            <view class="welcome-deco deco-2"></view>
            <view class="welcome-content">
                <text class="welcome-text" :style="{ color: welcomeText.text }">今天也是生意兴隆的一天哦</text>
                <text class="welcome-sub" :style="{ color: welcomeText.sub }">{{ greeting }}，{{ tenantName }} {{ userName }}</text>
            </view>
        </view>

        <!-- ★V3.7 经营驾驶舱（/micro/bossDashboard 聚合，仅 micro_pro；lite 显示升级引导） -->
        <template v-if="isPro">
            <!-- 今日经营数据：收入/支出 并列 + 净利 通栏 -->
            <text class="section-title">今日经营数据</text>
            <view class="stat-grid">
                <view class="stat-card">
                    <text class="stat-label">应收 / 实收</text>
                    <text class="stat-num c-income">¥{{ formatAmount(dash.todayReceivable || 0) }}</text>
                    <text class="stat-sub">实收 ¥{{ formatAmount(dash.todayReceived || 0) }}</text>
                </view>
                <view class="stat-card">
                    <text class="stat-label">支出</text>
                    <text class="stat-num c-expense">¥{{ formatAmount(dash.todayExpense || 0) }}</text>
                </view>
                <view class="stat-card full">
                    <text class="stat-label">净利</text>
                    <text class="stat-num" :style="{ color: (dash.todayNetProfit || 0) >= 0 ? theme.primaryColor : '#f56c6c' }">¥{{ formatAmount(dash.todayNetProfit || 0) }}</text>
                </view>
            </view>

            <!-- 本月经营数据：收入/支出/本月净利/本月订单 2×2 -->
            <text class="section-title">本月经营数据</text>
            <view class="stat-grid">
                <view class="stat-card">
                    <text class="stat-label">应收 / 实收</text>
                    <text class="stat-num c-dark">¥{{ formatAmount(dash.monthReceivable || 0) }}</text>
                    <text class="stat-sub">实收 ¥{{ formatAmount(dash.monthReceived || 0) }}</text>
                </view>
                <view class="stat-card">
                    <text class="stat-label">支出</text>
                    <text class="stat-num c-dark">¥{{ formatAmount(dash.monthExpense || 0) }}</text>
                </view>
                <view class="stat-card">
                    <text class="stat-label">本月净利</text>
                    <text class="stat-num" :style="{ color: (dash.monthNetProfit || 0) >= 0 ? theme.primaryColor : '#f56c6c' }">¥{{ formatAmount(dash.monthNetProfit || 0) }}</text>
                </view>
                <view class="stat-card">
                    <text class="stat-label">本月订单</text>
                    <text class="stat-num c-dark">{{ dash.monthOrderCount || 0 }}</text>
                </view>
            </view>

            <!-- ★T38.5 月度经营核算（结算口径：源自活动结算单 activity_settlement + 固定成本 + 税费） -->
            <text class="section-title">月度经营核算（结算口径）</text>
            <view class="stat-grid">
                <view class="stat-card">
                    <text class="stat-label">月毛利</text>
                    <text class="stat-num c-income">¥{{ formatAmount(dash.monthGrossProfit || 0) }}</text>
                </view>
                <view class="stat-card">
                    <text class="stat-label">固定成本合计</text>
                    <text class="stat-num c-expense">¥{{ formatAmount(dash.breakevenGrossProfit || 0) }}</text>
                </view>
                <view class="stat-card full">
                    <text class="stat-label">月净利润（结算口径，含税费）</text>
                    <text class="stat-num" :style="{ color: (dash.monthOperatingNetProfit || 0) >= 0 ? theme.primaryColor : '#f56c6c' }">¥{{ formatAmount(dash.monthOperatingNetProfit || 0) }}</text>
                </view>
            </view>

            <!-- ★T38.5 月保本线进度 + 月度报表入口 -->
            <view class="card be-card">
                <view class="card-head">
                    <text class="card-title no-mb">月保本线</text>
                    <text class="link" @click="goReport">月度报表 ›</text>
                </view>
                <view class="be-bar">
                    <view class="be-inner" :style="{ width: beProgressWidth + '%', background: theme.primaryColor || '#409eff' }"></view>
                </view>
                <view class="be-foot">
                    <text class="be-pct">{{ formatAmount(dash.breakevenProgress || 0) }}%</text>
                    <text class="be-flag" :class="(dash.breakevenProgress || 0) >= 100 ? 'safe' : 'unsafe'">{{ (dash.breakevenProgress || 0) >= 100 ? '✓ 已过保本线' : '尚未保本，继续加油' }}</text>
                </view>
            </view>

            <!-- 成本构成：按标签占比 -->
            <view v-if="costComposition.length" class="card cost-card">
                <text class="card-title">本月成本构成（月分摊）</text>
                <view v-for="item in costComposition" :key="item.category" class="cost-row">
                    <text class="cost-name">{{ item.name || item.category }}</text>
                    <view class="cost-bar">
                        <view class="cost-bar-inner" :style="{ width: (item.percent || 0) + '%', background: theme.primaryColor || '#409eff' }"></view>
                    </view>
                    <text class="cost-amount">¥{{ formatAmount(item.amount || 0) }}（{{ item.percent || 0 }}%）</text>
                </view>
            </view>

        </template>

        <!-- 免费版升级引导（与 PC lite 首页口径一致） -->
        <view v-if="!isPro" class="card upgrade" :style="{ borderLeftColor: theme.primaryColor }">
            <text class="card-title">当前版本：免费档</text>
            <text class="desc">支持活动发布与推广管理，升级付费档可解锁经营驾驶舱、待办与异常处理等全经营功能</text>
        </view>

        <!-- ★V3.7 底部导航（首页 / 最新活动 / 我的待处理 / 我的推广码） -->
        <micro-tabbar current="/pages/micro/bossHome/index" />
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get } from '../../../utils/request'
import config from '../../../config'
import { formatAmount, toHttps } from '../../../utils/format'
import { getTenantName, getTenantId, applyNavBarTitle, getSelectedTenant } from '../../../utils/roles'
import { getVersionType } from '../../../utils/auth'
import { getThemeSync, loadTenantTheme, themeGradient, themeGradientText, themeCssVars } from '../../../utils/theme'
import { iconUrl } from '../../../utils/themeIcons'
import MicroTabbar from '../../../components/micro-tabbar/micro-tabbar.vue'

const TYPE_MAP = {
    activity: '活动',
    notice: '预告',
    poster: '海报'
}

export default {
    components: { MicroTabbar },
    data() {
        return {
            // ★v1.42 租户名称/用户昵称（导航栏标题同源）
            tenantName: '',
            userName: '',
            // ★v1.43 租户主题色板（/micro/platform/theme/current 下发，首屏先用缓存同步渲染）
            theme: getThemeSync(),
            // 版本档位（登录响应 versionType：micro_pro/micro_lite）
            isPro: false,
            // ★V3.7 经营驾驶舱聚合数据（/micro/bossDashboard）
            dash: {},
            costComposition: []
        }
    },
    computed: {
        /** ★20261016 四主题重设计：根节点注入完整主题令牌（沿 DOM 继承到全部子元素/子组件） */
        rootStyle() {
            return themeCssVars(this.theme.key)
        },
        /** ★v1.43 欢迎区渐变：按租户主题渲染 */
        welcomeGradient() {
            return themeGradient(this.theme.key)
        },
        /** ★欢迎区前景文字色：浅色渐变用同系深色字、深色渐变用白字（禁止写死白色，浅色底不可读） */
        welcomeText() {
            return themeGradientText(this.theme.key)
        },
        /** ★v1.43 时段问候语 */
        greeting() {
            const h = new Date().getHours()
            if (h < 6) return '夜深了'
            if (h < 9) return '早上好'
            if (h < 12) return '上午好'
            if (h < 14) return '中午好'
            if (h < 18) return '下午好'
            return '晚上好'
        },
        /** ★T38.5 月保本进度条宽度（%，封顶 100） */
        beProgressWidth() {
            const p = Number(this.dash.breakevenProgress || 0)
            if (isNaN(p) || p < 0) return 0
            return p > 100 ? 100 : p
        }
    },
    onShow() {
        if (!getToken()) {
            uni.reLaunch({ url: '/pages/login/index' })
            return
        }
        applyNavBarTitle()
        this.loadData()
    },
    methods: {
        formatAmount,
        toHttps,
        iconUrl,
        typeLabel(type) { return TYPE_MAP[type] || '活动' },
        /** ★v1.43 活动封面占位渐变：按活动自身 theme_key */
        activityGradient(themeKey) { return themeGradient(themeKey) },
        go(url) { uni.navigateTo({ url }) },
        /** ★T38.5 月度报表入口（三张报表中心） */
        goReport() { uni.navigateTo({ url: '/pages/micro/report/center' }) },
        async loadData() {
            // ★v1.42 租户名称 + 版本档位（versionType 由登录响应缓存，兜底从已选租户取）
            this.tenantName = getTenantName()
            let vt = getVersionType()
            if (!vt) {
                const sel = getSelectedTenant()
                vt = (sel && sel.versionType) || ''
            }
            this.isPro = vt === 'micro_pro'
            // ★v1.43 租户主题：缓存先渲染，接口回包后刷新
            this.theme = getThemeSync()
            loadTenantTheme().then(t => { this.theme = t })
            // 用户昵称（T18.19 个人信息接口，失败不阻塞首页）
            this.loadProfile()
            // ★V3.7 经营驾驶舱仅付费版加载（免费版无权限，避免 403 噪音）
            if (this.isPro) {
                this.loadDashboard()
            }
        },
        /** ★V3.7 经营驾驶舱聚合数据（核心经营数据 + 成本构成 + 业务待办计数） */
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
                const data = (res && res.data) || {}
                this.userName = data.nickName || ''
            } catch (e) { /* ignore */ }
        }
    }
}
</script>

<style scoped>
/* ★首页改版（T38.5：新增月度经营核算区后内容超一屏，改为可滚动） */
.page { min-height: 100vh; box-sizing: border-box; padding: 20rpx 24rpx 120rpx; display: flex; flex-direction: column; background: var(--page-bg, #f5f6f8); }
/* ★v1.43 欢迎区：主题渐变 + 半透明装饰光斑 + 投影 */
.welcome-bar { flex-shrink: 0; position: relative; overflow: hidden; padding: 28rpx 32rpx; border-radius: 24rpx; margin-bottom: 16rpx; box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.08); }
.welcome-deco { position: absolute; border-radius: 50%; background: rgba(255, 255, 255, 0.14); }
.welcome-deco.deco-1 { width: 220rpx; height: 220rpx; top: -80rpx; right: -60rpx; }
.welcome-deco.deco-2 { width: 140rpx; height: 140rpx; bottom: -50rpx; right: 120rpx; background: rgba(255, 255, 255, 0.1); }
.welcome-content { position: relative; }
.welcome-text { font-size: 34rpx; font-weight: bold; display: block; }
.welcome-sub { font-size: 24rpx; display: block; margin-top: 8rpx; }
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 32rpx; margin-bottom: 24rpx; box-shadow: var(--shadow-card, 0 2rpx 12rpx rgba(0, 0, 0, 0.04)); }
.card.upgrade { border-left: 8rpx solid var(--brand, #409eff); }
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20rpx; }
.card-head .card-title { margin-bottom: 0; }
.card-title { font-size: 30rpx; font-weight: bold; display: block; margin-bottom: 20rpx; color: var(--text, #303133); }
.card-title.no-mb { margin-bottom: 0; }
.link { font-size: 24rpx; color: var(--brand, #409eff); }
.desc { font-size: 26rpx; color: #e6a23c; }
.card.upgrade .desc { color: #909399; }
/* 最新活动（★v1.43 带封面缩略图） */
.activity-row { display: flex; align-items: center; padding: 16rpx 0; border-bottom: 1rpx solid #f0f2f5; }
.activity-row:last-child { border-bottom: none; }
.activity-thumb { width: 96rpx; height: 96rpx; border-radius: 12rpx; margin-right: 20rpx; flex-shrink: 0; }
.thumb-default { display: flex; align-items: center; justify-content: center; }
.thumb-icon { font-size: 40rpx; }
.activity-main { flex: 1; overflow: hidden; }
.activity-title { display: block; font-size: 28rpx; color: var(--text, #303133); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.activity-time { display: block; font-size: 22rpx; color: var(--text-sub, #909399); margin-top: 6rpx; }
.type-badge { font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 6rpx; margin-left: 16rpx; white-space: nowrap; }
.type-activity { background: #ecf5ff; color: #409eff; }
.type-notice { background: #fdf6ec; color: #e6a23c; }
.type-poster { background: #f0f9eb; color: #67c23a; }
/* ★首页改版：分区标题 + 独立数据卡片（页面/横幅背景色由租户主题下发，此处仅中性卡片面） */
.section-title { flex-shrink: 0; display: block; font-size: 30rpx; font-weight: bold; color: var(--text, #303133); margin: 4rpx 0 12rpx; }
.stat-grid { flex-shrink: 0; display: flex; flex-wrap: wrap; justify-content: space-between; }
.stat-card { width: 48.5%; box-sizing: border-box; background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 20rpx 24rpx; margin-bottom: 14rpx; box-shadow: var(--shadow-card, 0 2rpx 12rpx rgba(0, 0, 0, 0.04)); }
.stat-card.full { width: 100%; }
.stat-label { display: block; font-size: 24rpx; color: var(--text-sub, #606266); }
.stat-num { display: block; font-size: 40rpx; font-weight: bold; margin-top: 8rpx; }
.stat-num.c-income { color: #67c23a; }
.stat-num.c-expense { color: #f56c6c; }
.stat-num.c-dark { color: var(--text, #303133); }
.stat-sub { display: block; font-size: 24rpx; color: var(--brand, #409eff); margin-top: 6rpx; }
/* ★T38.5 月保本线进度卡 */
.card.be-card { flex-shrink: 0; padding: 24rpx 32rpx; }
.be-bar { height: 20rpx; background: var(--brand-weak, #f0f2f5); border-radius: 10rpx; overflow: hidden; }
.be-inner { height: 100%; border-radius: 10rpx; }
.be-foot { display: flex; align-items: center; justify-content: space-between; margin-top: 14rpx; }
.be-pct { font-size: 26rpx; font-weight: bold; color: var(--text, #303133); }
.be-flag { font-size: 24rpx; }
.be-flag.safe { color: #67c23a; }
.be-flag.unsafe { color: #e6a23c; }
/* ★V3.7 成本构成（卡片吸收一屏剩余高度，超出隐藏不滚动） */
.card.cost-card { flex: 1; overflow: hidden; margin-bottom: 0; padding: 24rpx 32rpx; }
.cost-row { display: flex; align-items: center; padding: 10rpx 0; }
.cost-name { width: 140rpx; font-size: 24rpx; color: var(--text-sub, #606266); flex-shrink: 0; }
.cost-bar { flex: 1; height: 16rpx; background: var(--brand-weak, #f0f2f5); border-radius: 8rpx; overflow: hidden; margin: 0 16rpx; }
.cost-bar-inner { height: 100%; border-radius: 8rpx; }
.cost-amount { width: 220rpx; text-align: right; font-size: 22rpx; color: var(--text-sub, #909399); flex-shrink: 0; }
.loading, .empty { text-align: center; color: var(--text-sub, #909399); font-size: 26rpx; padding: 40rpx 0; }

/* ★手绘主题装饰贴纸 */
.page { position: relative; }
.sticker { position: absolute; opacity: .8; pointer-events: none; z-index: 0; }
.welcome-bar, .stat-card, .cost-card { position: relative; z-index: 1; }
.sticker-star-tl { top: 10rpx; left: 10rpx; width: 48rpx; height: 48rpx; transform: rotate(-10deg); }
.sticker-sparkle-tl { top: 30rpx; left: 50rpx; width: 32rpx; height: 32rpx; transform: rotate(15deg); }
.sticker-bubble-welcome { top: 16rpx; right: 16rpx; width: 52rpx; height: 52rpx; transform: rotate(8deg); }
.sticker-cloud-welcome { top: 40rpx; right: 6rpx; width: 64rpx; height: 52rpx; transform: rotate(-5deg); }
.sticker-cloud-profit { top: 16rpx; right: 16rpx; width: 56rpx; height: 44rpx; transform: rotate(5deg); }
</style>
