<template>
    <view class="page">
        <!-- ========== 总览 ========== -->
        <view class="overview">
            <view class="ov-item">
                <text class="ov-num">{{ formatWan(data.totalActualProfit) }}</text>
                <text class="ov-label">实际利润</text>
            </view>
            <view class="ov-row">
                <view class="ov-sm"><text class="sm-num">{{ formatWan(data.totalContractAmount) }}</text><text class="sm-label">合同总额</text></view>
                <view class="ov-sm"><text class="sm-num">{{ formatWan(data.totalVerifiedIncome) }}</text><text class="sm-label">已核验收入</text></view>
                <view class="ov-sm"><text class="sm-num">{{ formatWan(data.totalPendingIncome) }}</text><text class="sm-label">待核验收入</text></view>
            </view>
            <view class="ov-row">
                <view class="ov-sm"><text class="sm-num">{{ formatWan(data.totalExpense) }}</text><text class="sm-label">总支出</text></view>
                <view class="ov-sm"><text class="sm-num">{{ data.activeProjectCount || 0 }}</text><text class="sm-label">进行中项目</text></view>
                <view class="ov-sm"><text class="sm-num">{{ data.pendingApprovalCount || 0 }}</text><text class="sm-label">待审批</text></view>
            </view>
        </view>

        <!-- ========== 大类卡片（🟢🟡🔴 按支出占比） ========== -->
        <view class="section-title">大类支出（点击下钻）</view>
        <view v-if="!categoryList.length" class="card"><text class="desc">暂无支出数据</text></view>
        <view v-for="cat in categoryList" :key="cat.categoryName" class="card cat" @click="drillToCategory(cat)">
            <view class="cat-head">
                <text class="dot">{{ riskDot(cat) }}</text>
                <text class="cat-name">{{ cat.categoryName }}</text>
                <text class="cat-amount">¥{{ formatAmount(cat.amount) }}</text>
            </view>
            <view class="cat-foot">
                <text class="desc">{{ cat.billCount || 0 }} 笔　占比 {{ ratioText(cat) }}</text>
            </view>
        </view>

        <!-- ========== 下钻面板（大类→项目→明细） ========== -->
        <view v-if="drill.level >= 2" class="card drill">
            <view class="drill-nav">
                <text class="link" @click="drillBack">← 返回{{ drill.level === 3 ? '大类' : '' }}</text>
                <text class="drill-path">{{ drill.category }}{{ drill.projectName ? ' / ' + drill.projectName : '' }}</text>
            </view>
            <view v-if="!drill.rows.length" class="desc">暂无数据</view>
            <view v-for="(row, idx) in drill.rows" :key="idx" class="drill-row" @click="drillToRow(row)">
                <text class="drill-name">{{ row.name || row.no }}</text>
                <text class="drill-amount">¥{{ formatAmount(row.amount) }}</text>
            </view>
        </view>

        <!-- ========== 固定成本卡片（本月计提） ========== -->
        <view class="section-title">固定成本（{{ data.accrualMonth || '本月' }}计提）</view>
        <view class="card">
            <view class="kv"><text>薪资社保计提</text><text>¥{{ formatAmount(data.monthlySalaryAccrual) }}</text></view>
            <view class="kv"><text>资产折旧计提</text><text>¥{{ formatAmount(data.monthlyDepreciation) }}</text></view>
            <view class="kv"><text>贷款利息计提</text><text>¥{{ formatAmount(data.monthlyLoanInterest) }}</text></view>
        </view>

        <!-- ========== 风险预警区 ========== -->
        <view class="section-title">风险预警</view>
        <view v-if="!data.badDebtWarnings?.length && !data.changeConfirmWarnings?.length" class="card">
            <text class="desc">✅ 暂无风险预警</text>
        </view>
        <view v-for="bd in (data.badDebtWarnings || [])" :key="'bd' + bd.id" class="card risk">
            <text class="title">🔴 烂账风险：{{ bd.projectName || ('项目#' + bd.projectId) }}</text>
            <text class="desc">类型：{{ bd.lossType || '-' }}　计提：¥{{ formatAmount(bd.badDebtProvision) }}</text>
        </view>
        <view v-for="ch in (data.changeConfirmWarnings || [])" :key="'ch' + ch.id" class="card risk">
            <text class="title">🟡 变更待收款确认：{{ ch.projectName || ('项目#' + ch.projectId) }}</text>
            <text class="desc">变更额 ¥{{ formatAmount(ch.contractDelta) }}　状态：{{ ch.changeStatus }}</text>
        </view>

        <!-- ========== 未归类池 ========== -->
        <view class="section-title">未归类池</view>
        <view class="card">
            <text class="desc">未关联项目（公司级）报销 {{ data.unclassifiedBillCount || 0 }} 笔，合计 ¥{{ formatAmount(data.unclassifiedBillAmount) }}</text>
        </view>
    </view>
</template>

<script>
import { guardPage } from '../../utils/permission'
import { get } from '../../utils/request'
import config from '../../config'
import { formatAmount, formatWan } from '../../utils/format'

export default {
    data() {
        return {
            data: {},
            // 下钻状态：level=1 大类视图；2=按项目；3=按明细
            drill: { level: 1, category: '', projectId: null, projectName: '', rows: [] }
        }
    },
    computed: {
        categoryList() {
            return this.data.categoryExpenses || []
        },
        totalCatAmount() {
            return this.categoryList.reduce((s, c) => s + Number(c.amount || 0), 0)
        }
    },
    onLoad() {
        if (!guardPage('/pages/dashboard/category')) return
        this.loadDashboard()
    },
    onPullDownRefresh() {
        this.loadDashboard().finally(() => uni.stopPullDownRefresh())
    },
    methods: {
        formatAmount, formatWan,
        /** 看板总数据（与 PC 3215/3288 同源） */
        async loadDashboard() {
            try {
                const res = await get(config.api.dashboard)
                this.data = res.data || {}
            } catch (e) { /* 已统一提示 */ }
        },
        /** 大类风险色：占比<30% 🟢，30%~60% 🟡，>60% 🔴（设计文档 7.3 三秒看懂） */
        riskDot(cat) {
            const ratio = this.totalCatAmount ? Number(cat.amount || 0) / this.totalCatAmount : 0
            if (ratio > 0.6) return '🔴'
            if (ratio >= 0.3) return '🟡'
            return '🟢'
        },
        ratioText(cat) {
            const ratio = this.totalCatAmount ? (Number(cat.amount || 0) / this.totalCatAmount) * 100 : 0
            return ratio.toFixed(1) + '%'
        },
        /** 大类 → 项目分布（下钻第2层） */
        async drillToCategory(cat) {
            try {
                const res = await get(config.api.dashboardDrillDown, { level: 2, category: cat.categoryName })
                this.drill = { level: 2, category: cat.categoryName, projectId: null, projectName: '', rows: res.data || [] }
            } catch (e) { /* 已统一提示 */ }
        },
        /** 项目 → 单据明细（下钻第3层） */
        async drillToRow(row) {
            if (this.drill.level !== 2 || row.projectId === undefined) return
            try {
                const res = await get(config.api.dashboardDrillDown, {
                    level: 3, category: this.drill.category, projectId: row.projectId
                })
                this.drill.projectId = row.projectId
                this.drill.projectName = row.name
                this.drill.rows = res.data || []
                this.drill.level = 3
            } catch (e) { /* 已统一提示 */ }
        },
        /** 下钻返回上一层 */
        drillBack() {
            if (this.drill.level === 3) {
                this.drillToCategory({ categoryName: this.drill.category })
            } else {
                this.drill = { level: 1, category: '', projectId: null, projectName: '', rows: [] }
            }
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 60rpx; }
.overview { background: linear-gradient(135deg, #409eff, #5d7ce6); border-radius: 16rpx; padding: 32rpx; margin-bottom: 24rpx; }
.ov-item { text-align: center; margin-bottom: 24rpx; }
.ov-num { display: block; font-size: 56rpx; font-weight: bold; color: #fff; }
.ov-label { font-size: 24rpx; color: rgba(255, 255, 255, .8); }
.ov-row { display: flex; }
.ov-sm { flex: 1; text-align: center; }
.sm-num { display: block; font-size: 30rpx; font-weight: bold; color: #fff; }
.sm-label { font-size: 22rpx; color: rgba(255, 255, 255, .8); }
.section-title { font-size: 30rpx; font-weight: bold; margin: 24rpx 0 16rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 28rpx; margin-bottom: 20rpx; }
.card.risk { border-left: 8rpx solid #f56c6c; }
.card.drill { border-left: 8rpx solid #409eff; }
.title { font-size: 30rpx; font-weight: bold; display: block; }
.desc { font-size: 24rpx; color: #909399; display: block; }
.cat-head { display: flex; align-items: center; }
.dot { font-size: 28rpx; margin-right: 12rpx; }
.cat-name { flex: 1; font-size: 30rpx; font-weight: bold; }
.cat-amount { font-size: 30rpx; font-weight: bold; color: #303133; }
.cat-foot { margin-top: 8rpx; }
.drill-nav { display: flex; align-items: center; margin-bottom: 16rpx; }
.link { color: #409eff; font-size: 26rpx; margin-right: 16rpx; }
.drill-path { font-size: 24rpx; color: #606266; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.drill-row { display: flex; justify-content: space-between; padding: 16rpx 0; border-top: 1rpx solid #f0f2f5; }
.drill-name { font-size: 26rpx; color: #303133; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.drill-amount { font-size: 26rpx; color: #303133; margin-left: 16rpx; }
.kv { display: flex; justify-content: space-between; padding: 10rpx 0; font-size: 26rpx; color: #303133; }
</style>
