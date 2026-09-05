<template>
    <view class="page">
        <!-- ========== 项目头部 ========== -->
        <view class="head">
            <view class="head-row">
                <text class="head-name">{{ project.projectName }}</text>
                <text class="badge" :style="{ color: statusColor(PROJECT_STATUS, project.status), borderColor: statusColor(PROJECT_STATUS, project.status) }">
                    {{ statusText(PROJECT_STATUS, project.status) }}
                </text>
            </view>
            <text class="head-desc">{{ project.clientName || '暂无客户信息' }}</text>
            <view class="head-kv">
                <view class="kv"><text>项目经理</text><text>{{ project.projectManagerName || '-' }}</text></view>
                <view class="kv"><text>起止日期</text><text>{{ formatDate(project.startDate) }} ~ {{ formatDate(project.endDate) }}</text></view>
                <view class="kv"><text>当前合同额</text><text>¥{{ formatAmount(project.currentContractAmount) }}</text></view>
                <view class="kv" v-if="project.changeCount"><text>合同变更</text><text>{{ project.changeCount }} 次</text></view>
            </view>
        </view>

        <!-- ========== 老板审批立项（T8.2 立项审批） ========== -->
        <view v-if="canApprove" class="card approve-bar">
            <view class="btn reject" @click="onReject">驳回</view>
            <view class="btn approve" @click="onApprove">审批通过</view>
        </view>

        <!-- ========== 利润拆解（纸面/实际分离，设计文档 27.2） ========== -->
        <view class="section-title">利润拆解</view>
        <view class="card">
            <view class="profit-main">
                <view class="pm-item">
                    <text class="pm-num" :class="Number(profit.actualProfit || 0) < 0 ? 'neg' : ''">¥{{ formatAmount(profit.actualProfit) }}</text>
                    <text class="pm-label">实际利润</text>
                </view>
                <view class="pm-item">
                    <text class="pm-num">¥{{ formatAmount(profit.paperProfit) }}</text>
                    <text class="pm-label">纸面利润</text>
                </view>
                <view class="pm-item">
                    <text class="pm-num">{{ profitRateText }}</text>
                    <text class="pm-label">利润率</text>
                </view>
            </view>
            <view class="kv"><text>已核验收入</text><text>¥{{ formatAmount(profit.verifiedIncome) }}</text></view>
            <view class="kv"><text>待核验收入（不计利润）</text><text>¥{{ formatAmount(profit.pendingIncome) }}</text></view>
            <view class="kv"><text>总支出（报销+付款）</text><text>-¥{{ formatAmount(profit.totalExpense) }}</text></view>
            <view class="kv"><text>折旧分摊</text><text>-¥{{ formatAmount(profit.depreciationShare) }}</text></view>
            <view class="kv"><text>项目分成</text><text>-¥{{ formatAmount(profit.sharingAmount) }}</text></view>
            <view class="kv" v-if="Number(profit.badDebtProvision || 0) > 0"><text>坏账计提</text><text class="neg">-¥{{ formatAmount(profit.badDebtProvision) }}</text></view>
        </view>

        <!-- ========== 预算科目（按大类分配） ========== -->
        <view class="section-title">预算科目</view>
        <view v-if="!subjects.length" class="card"><text class="desc">暂未分配预算科目</text></view>
        <view class="card">
            <view v-for="s in subjects" :key="s.id" class="subject">
                <view class="subject-head">
                    <text class="subject-name">{{ s.subjectName }}</text>
                    <text class="subject-cat">{{ s.categoryName }}</text>
                </view>
                <view class="budget-bar"><view class="budget-fill" :style="{ width: subjectPercent(s) + '%', background: subjectColor(s) }"></view></view>
                <view class="subject-foot">
                    <text class="desc">已用 ¥{{ formatAmount(s.usedAmount) }}</text>
                    <text class="desc">剩余 ¥{{ formatAmount(s.remainAmount) }} / 预算 ¥{{ formatAmount(s.budgetAmount) }}</text>
                </view>
            </view>
        </view>

        <!-- ========== 项目成员 ========== -->
        <view class="section-title">项目成员</view>
        <view v-if="!members.length" class="card"><text class="desc">暂无成员</text></view>
        <view class="card">
            <view v-for="m in members" :key="m.id" class="member">
                <text class="member-name">{{ m.userName }}</text>
                <text class="member-role">{{ memberRoleText(m.role) }}</text>
                <text class="member-ratio" v-if="m.investRatio">投入 {{ m.investRatio }}%</text>
            </view>
        </view>

        <!-- ========== 烂账信息 ========== -->
        <view v-if="project.status === 'LOST'" class="card risk">
            <text class="title">🔴 烂账登记</text>
            <view class="kv"><text>烂账类型</text><text>{{ project.lossType || '-' }}</text></view>
            <view class="kv"><text>烂账原因</text><text>{{ project.lossReason || '-' }}</text></view>
            <view class="kv"><text>坏账计提</text><text>¥{{ formatAmount(project.badDebtProvision) }}</text></view>
        </view>
    </view>
</template>

<script>
import { guardPage } from '../../utils/permission'
import { get, put } from '../../utils/request'
import config from '../../config'
import { getRoles } from '../../utils/auth'
import { ROLES } from '../../utils/roles'
import { formatAmount, formatDate, PROJECT_STATUS, statusText, statusColor } from '../../utils/format'

const MEMBER_ROLE = { MANAGER: '项目经理', MEMBER: '成员', VIEWER: '观察者' }

export default {
    data() {
        return {
            PROJECT_STATUS,
            projectId: null,
            project: {},
            profit: {},
            subjects: [],
            members: [],
            canApprove: false
        }
    },
    computed: {
        profitRateText() {
            const rate = Number(this.profit.profitRate || 0)
            // 后端按小数返回（如 0.3521 → 35.21%）
            return (rate > 1 ? rate : rate * 100).toFixed(2) + '%'
        }
    },
    onLoad(options) {
        if (!guardPage('/pages/project/detail')) return
        this.projectId = options.id
        this.loadAll()
    },
    methods: {
        formatAmount, formatDate, statusText, statusColor,
        async loadAll() {
            try {
                // 详情 + 利润 + 科目 + 成员并行加载（与 PC 3231/3251 同源）
                const [detail, profit, subjects, members] = await Promise.all([
                    get('/fee/project/' + this.projectId),
                    get(config.api.projectProfit + '/' + this.projectId).catch(() => ({ data: {} })),
                    get(config.api.projectSubjects + '/' + this.projectId).catch(() => ({ data: [] })),
                    get(config.api.projectMembers + '/' + this.projectId).catch(() => ({ data: [] }))
                ])
                this.project = detail.data || {}
                this.profit = profit.data || {}
                this.subjects = subjects.data || []
                this.members = members.data || []
                // 立项审批仅老板可操作，且项目处于待审批状态（T8.2）
                this.canApprove = this.project.status === 'PENDING_APPROVAL' && getRoles().includes(ROLES.BOSS)
            } catch (e) { /* 已统一提示 */ }
        },
        subjectPercent(s) {
            const budget = Number(s.budgetAmount || 0)
            if (!budget) return 0
            return Math.min(100, Math.round((Number(s.usedAmount || 0) / budget) * 100))
        },
        subjectColor(s) {
            const percent = this.subjectPercent(s)
            if (percent > 90) return '#f56c6c'
            if (percent >= 70) return '#e6a23c'
            return '#67c23a'
        },
        memberRoleText(role) {
            return MEMBER_ROLE[role] || role || '-'
        },
        /** 老板审批通过立项 */
        onApprove() {
            uni.showModal({
                title: '审批确认',
                content: '确认通过该项目的立项审批？',
                success: async (res) => {
                    if (!res.confirm) return
                    try {
                        await put(config.api.projectApprove + '/' + this.projectId)
                        uni.showToast({ title: '审批通过', icon: 'success' })
                        this.loadAll()
                    } catch (e) { /* 已统一提示 */ }
                }
            })
        },
        /** 老板驳回立项（原因可选填） */
        onReject() {
            uni.showModal({
                title: '驳回立项',
                content: '确认驳回该项目的立项申请？',
                editable: true,
                placeholderText: '驳回原因（选填）',
                success: async (res) => {
                    if (!res.confirm) return
                    try {
                        const reason = encodeURIComponent(res.content || '')
                        await put(config.api.projectReject + '/' + this.projectId + (reason ? '?reason=' + reason : ''))
                        uni.showToast({ title: '已驳回', icon: 'success' })
                        this.loadAll()
                    } catch (e) { /* 已统一提示 */ }
                }
            })
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 60rpx; }
.head { background: linear-gradient(135deg, #409eff, #5d7ce6); border-radius: 16rpx; padding: 32rpx; margin-bottom: 24rpx; }
.head-row { display: flex; align-items: center; }
.head-name { flex: 1; font-size: 36rpx; font-weight: bold; color: #fff; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.badge { font-size: 22rpx; border: 1rpx solid; border-radius: 8rpx; padding: 2rpx 12rpx; margin-left: 12rpx; background: #fff; flex-shrink: 0; }
.head-desc { font-size: 24rpx; color: rgba(255, 255, 255, .85); margin-top: 8rpx; display: block; }
.head-kv { margin-top: 16rpx; }
.head-kv .kv { color: rgba(255, 255, 255, .9); border-top: 1rpx solid rgba(255, 255, 255, .2); }
.section-title { font-size: 30rpx; font-weight: bold; margin: 24rpx 0 16rpx; }
.card { background: #fff; border-radius: 16rpx; padding: 28rpx; margin-bottom: 20rpx; }
.card.risk { border-left: 8rpx solid #f56c6c; }
.title { font-size: 30rpx; font-weight: bold; display: block; margin-bottom: 8rpx; }
.desc { font-size: 24rpx; color: #909399; display: block; }
.kv { display: flex; justify-content: space-between; padding: 10rpx 0; font-size: 26rpx; color: #303133; }
.profit-main { display: flex; margin-bottom: 16rpx; padding-bottom: 16rpx; border-bottom: 1rpx solid #f0f2f5; }
.pm-item { flex: 1; text-align: center; }
.pm-num { display: block; font-size: 30rpx; font-weight: bold; color: #303133; }
.pm-num.neg { color: #f56c6c; }
.pm-label { font-size: 22rpx; color: #909399; }
.neg { color: #f56c6c; }
.approve-bar { display: flex; }
.btn { flex: 1; text-align: center; padding: 20rpx 0; border-radius: 12rpx; font-size: 28rpx; font-weight: bold; }
.btn.approve { background: #67c23a; color: #fff; margin-left: 20rpx; }
.btn.reject { background: #fff; color: #f56c6c; border: 1rpx solid #f56c6c; }
.subject { padding: 16rpx 0; border-bottom: 1rpx solid #f0f2f5; }
.subject:last-child { border-bottom: none; }
.subject-head { display: flex; align-items: center; margin-bottom: 10rpx; }
.subject-name { flex: 1; font-size: 28rpx; color: #303133; font-weight: bold; }
.subject-cat { font-size: 22rpx; color: #909399; }
.budget-bar { height: 10rpx; background: #f0f2f5; border-radius: 5rpx; overflow: hidden; }
.budget-fill { height: 100%; border-radius: 5rpx; }
.subject-foot { display: flex; justify-content: space-between; margin-top: 8rpx; }
.member { display: flex; align-items: center; padding: 14rpx 0; border-bottom: 1rpx solid #f0f2f5; }
.member:last-child { border-bottom: none; }
.member-name { flex: 1; font-size: 28rpx; color: #303133; }
.member-role { font-size: 22rpx; color: #409eff; border: 1rpx solid #409eff; border-radius: 8rpx; padding: 2rpx 12rpx; margin-right: 16rpx; }
.member-ratio { font-size: 22rpx; color: #909399; }
</style>
