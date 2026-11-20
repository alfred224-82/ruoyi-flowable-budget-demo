<template>
    <!-- ★M38 T38.2 固定支出管理（§7.12）：月份切换 + 沿用上月一键复制 + 分摊预览 + 合计实时计算 + 编辑入口
         口径：列表按「覆盖到该月」展示（含每月重复/周期分摊项）；合计 = Σ 启用项每月分摊额（monthlyAmount） -->
    <view class="page" :class="'theme-' + theme.key" :style="rootStyle">
        <!-- 月份切换 + 合计 -->
        <view class="card summary">
            <view class="sum-head">
                <picker mode="date" fields="month" :value="month" @change="onMonthChange">
                    <view class="month-pick">{{ month }} ▾</view>
                </picker>
                <text class="sum-label">当月固定支出合计（月分摊）</text>
            </view>
            <text class="sum">¥{{ formatAmount(totalAmount) }}</text>
            <view class="sum-actions">
                <view class="btn ghost" @click="copyLastMonth">沿用上月</view>
                <view class="btn solid" :style="{ background: theme.brand }" @click="goAdd">+ 新增支出</view>
            </view>
        </view>

        <view v-if="!loaded" class="loading">加载中…</view>
        <view v-else-if="!list.length" class="empty">该月暂无固定支出项，可点「沿用上月」快速复制</view>
        <view v-else class="list">
            <view v-for="item in list" :key="item.id" class="card item">
                <view class="row">
                    <text class="name">{{ item.name }}</text>
                    <text class="amount">¥{{ formatAmount(item.monthlyAmount != null ? item.monthlyAmount : item.amount) }}</text>
                </view>
                <view class="row sub">
                    <text class="category">{{ item.categoryName || item.category }}</text>
                    <text class="tag" :class="item.enabled === 1 ? 'on' : 'off'">{{ item.enabled === 1 ? '启用' : '停用' }}</text>
                </view>
                <!-- 分摊预览 -->
                <view v-if="item.amortizeMonths > 0" class="amort">
                    <text class="amort-txt">分摊：总额 ¥{{ formatAmount(item.amount) }} ÷ {{ item.amortizeMonths }}月 = ¥{{ formatAmount(item.monthlyAmount) }}/月</text>
                    <text v-if="item.periodRuleName" class="amort-rule">（{{ item.periodRuleName }}）</text>
                </view>
                <view v-else class="amort">
                    <text class="amort-txt">{{ item.isRecurring === 1 ? '每月重复全额计入' : '仅生效月计入' }} · 生效 {{ item.effectiveMonth }}</text>
                </view>
                <view class="row ops">
                    <text class="op-link" :style="{ color: theme.brand }" @click="goEdit(item)">编辑</text>
                </view>
            </view>
        </view>
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get, post } from '../../../utils/request'
import config from '../../../config'
import { formatAmount, currentPeriod } from '../../../utils/format'
import { getThemeSync, loadTenantTheme, themeCssVars } from '../../../utils/theme'
import { applyNavBarTitle } from '../../../utils/roles'

export default {
    data() {
        return {
            theme: getThemeSync(),
            loaded: false,
            month: currentPeriod(),
            list: []
        }
    },
    computed: {
        rootStyle() { return themeCssVars(this.theme.key) },
        /** 合计实时计算：Σ 启用项每月分摊额 */
        totalAmount() {
            return this.list.reduce((s, it) => {
                if (it.enabled !== 1) return s
                const v = it.monthlyAmount != null ? it.monthlyAmount : it.amount
                return s + (Number(v) || 0)
            }, 0)
        }
    },
    onLoad(options) {
        if (options && options.month) this.month = options.month
    },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        applyNavBarTitle()
        this.theme = getThemeSync()
        loadTenantTheme().then(t => { this.theme = t })
        this.loadData()
    },
    methods: {
        formatAmount,
        onMonthChange(e) { this.month = e.detail.value; this.loadData() },
        async loadData() {
            this.loaded = false
            try {
                const res = await get(config.api.microCostActiveList, { month: this.month })
                this.list = (res && res.data) || []
            } catch (e) { this.list = [] } finally { this.loaded = true }
        },
        copyLastMonth() {
            uni.showModal({
                title: '沿用上月',
                content: '将把上月生效的固定支出项复制到 ' + this.month + '（已存在或已自动覆盖的项会跳过）。确认继续？',
                success: async (r) => {
                    if (!r.confirm) return
                    try {
                        const res = await post(config.api.microCostCopyLastMonth + '?month=' + this.month)
                        const n = (res && res.data) || 0
                        uni.showToast({ title: n > 0 ? ('已复制 ' + n + ' 项') : '无需复制', icon: 'none' })
                        this.loadData()
                    } catch (e) { /* request 已提示 */ }
                }
            })
        },
        goAdd() { uni.navigateTo({ url: '/pages/micro/cost/edit?month=' + this.month }) },
        goEdit(item) { uni.navigateTo({ url: '/pages/micro/cost/edit?id=' + item.id }) }
    }
}
</script>

<style scoped>
.page { min-height: 100vh; box-sizing: border-box; padding: 24rpx; background: var(--page-bg, #f5f6f8); }
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 28rpx; margin-bottom: 20rpx; box-shadow: var(--shadow-card, 0 2rpx 12rpx rgba(0,0,0,.04)); }
.summary { text-align: center; }
.sum-head { display: flex; align-items: center; justify-content: center; flex-wrap: wrap; }
.month-pick { font-size: 30rpx; font-weight: bold; color: var(--brand, #409eff); margin-right: 16rpx; }
.sum-label { font-size: 24rpx; color: var(--text-sub, #909399); }
.sum { font-size: 52rpx; font-weight: bold; color: #f56c6c; display: block; margin: 12rpx 0 20rpx; }
.sum-actions { display: flex; justify-content: center; }
.btn { font-size: 26rpx; padding: 14rpx 34rpx; border-radius: 30rpx; margin: 0 12rpx; }
.btn.solid { color: #fff; }
.btn.ghost { background: var(--brand-weak, #f0f2f5); color: var(--brand, #409eff); }
.item .row { display: flex; align-items: center; justify-content: space-between; }
.name { font-size: 30rpx; font-weight: bold; color: var(--text, #303133); flex: 1; }
.amount { font-size: 30rpx; color: #f56c6c; font-weight: bold; }
.sub { margin-top: 12rpx; }
.category { font-size: 24rpx; color: var(--text-sub, #909399); }
.tag { font-size: 22rpx; padding: 2rpx 14rpx; border-radius: 8rpx; }
.tag.on { background: rgba(103,194,58,.14); color: #67c23a; }
.tag.off { background: rgba(245,108,108,.14); color: #f56c6c; }
.amort { margin-top: 12rpx; background: var(--page-bg, #f5f6f8); border-radius: 10rpx; padding: 12rpx 16rpx; }
.amort-txt { font-size: 22rpx; color: var(--text-sub, #606266); }
.amort-rule { font-size: 20rpx; color: var(--text-sub, #909399); }
.ops { margin-top: 14rpx; justify-content: flex-end; }
.op-link { font-size: 26rpx; }
.loading, .empty { text-align: center; color: var(--text-sub, #909399); font-size: 26rpx; padding: 100rpx 40rpx; line-height: 1.6; }
</style>
