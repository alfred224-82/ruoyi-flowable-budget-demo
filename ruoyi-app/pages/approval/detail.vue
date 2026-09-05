<template>
    <view class="page">
        <!-- ========== 报销单详情 ========== -->
        <template v-if="type === 'expense'">
            <view class="head">
                <text class="head-amount">¥{{ formatAmount(data.amount) }}</text>
                <text class="head-sub">{{ data.billNo }}</text>
                <view class="tags">
                    <text v-if="data.invoiceDefer === 1" class="tag warn">发票后补</text>
                    <text v-if="showEvidence && data.evidenceLevel" class="tag" :class="evidenceClass">
                        {{ evidenceText }}
                    </text>
                    <text v-if="data.isOverBudget === 1" class="tag danger">超预算</text>
                </view>
            </view>
            <view class="card">
                <view class="kv"><text>申请人</text><text>{{ data.applicantName }}（{{ data.deptName || '-' }}）</text></view>
                <view class="kv"><text>用途标签</text><text>{{ data.purposeTagName || '-' }}</text></view>
                <view class="kv"><text>所属项目</text><text>{{ data.projectName || '公司级（未关联项目）' }}</text></view>
                <view class="kv"><text>预算科目</text><text>{{ data.calcBudgetSubjectName || '-' }}</text></view>
                <view class="kv"><text>费用日期</text><text>{{ formatDate(data.expenseDate) }}</text></view>
                <view class="kv"><text>报销年月</text><text>{{ data.expensePeriod || '-' }}</text></view>
                <view class="kv" v-if="data.currencyCode && data.currencyCode !== 'CNY'">
                    <text>原币金额</text><text>{{ data.originalAmount }} {{ data.currencyCode }}（汇率 {{ data.exchangeRate }}）</text>
                </view>
                <view class="kv" v-if="data.creditLevel"><text>信用等级快照</text><text>{{ data.creditLevel }}</text></view>
                <view class="kv" v-if="data.remark"><text>备注</text><text>{{ data.remark }}</text></view>
            </view>
        </template>

        <!-- ========== 付款单详情 ========== -->
        <template v-else-if="type === 'payment'">
            <view class="head">
                <text class="head-amount">¥{{ formatAmount(data.amount) }}</text>
                <text class="head-sub">{{ data.paymentNo }}</text>
            </view>
            <view class="card">
                <view class="kv"><text>所属项目</text><text>{{ data.projectName || '-' }}</text></view>
                <view class="kv"><text>收款方</text><text>{{ data.payee || '-' }}</text></view>
                <view class="kv"><text>付款方</text><text>{{ data.payer || '-' }}</text></view>
                <view class="kv"><text>付款类型</text><text>{{ data.payType || '-' }}</text></view>
                <view class="kv"><text>付款日期</text><text>{{ formatDate(data.paymentDate) }}</text></view>
                <view class="kv"><text>发票状态</text><text>{{ data.invoiceStatus === 'HAS' ? '已有发票' : (data.invoiceStatus === 'PENDING' ? '收集中' : '未到票') }}</text></view>
                <view class="kv" v-if="data.description"><text>说明</text><text>{{ data.description }}</text></view>
            </view>
        </template>

        <!-- ========== 合同变更详情 ========== -->
        <template v-else-if="type === 'change'">
            <view class="head">
                <text class="head-amount" :class="Number(data.contractDelta || 0) < 0 ? 'neg' : ''">
                    {{ Number(data.contractDelta || 0) >= 0 ? '+' : '' }}¥{{ formatAmount(data.contractDelta) }}
                </text>
                <text class="head-sub">{{ data.projectName || ('项目#' + data.projectId) }} · {{ data.changeType || '合同变更' }}</text>
            </view>
            <view class="card">
                <view class="kv"><text>原合同额</text><text>¥{{ formatAmount(data.oldContractAmount) }}</text></view>
                <view class="kv"><text>新合同额</text><text>¥{{ formatAmount(data.newContractAmount) }}</text></view>
                <view class="kv"><text>变更状态</text><text>{{ statusText(CHANGE_STATUS, data.changeStatus) }}</text></view>
                <view class="kv" v-if="data.changeReason"><text>变更原因</text><text>{{ data.changeReason }}</text></view>
            </view>
        </template>

        <!-- ========== 立项详情 ========== -->
        <template v-else>
            <view class="head">
                <text class="head-amount">¥{{ formatAmount(data.currentContractAmount) }}</text>
                <text class="head-sub">{{ data.projectName }}</text>
            </view>
            <view class="card">
                <view class="kv"><text>客户</text><text>{{ data.clientName || '-' }}</text></view>
                <view class="kv"><text>项目经理</text><text>{{ data.projectManagerName || '-' }}</text></view>
                <view class="kv"><text>起止日期</text><text>{{ formatDate(data.startDate) }} ~ {{ formatDate(data.endDate) }}</text></view>
                <view class="kv"><text>项目总预算</text><text>¥{{ formatAmount(data.totalBudget) }}</text></view>
                <view class="kv" v-if="data.remark"><text>备注</text><text>{{ data.remark }}</text></view>
            </view>
            <view class="tip"><text class="desc">💡 立项通过后，可在项目详情中按大类分配预算科目</text></view>
        </template>

        <!-- ========== 审批操作区（仅待审批状态显示） ========== -->
        <view v-if="canAudit" class="approve-box">
            <textarea v-if="showRejectInput" v-model="rejectReason" class="reject-input"
                :placeholder="type === 'expense' ? '请输入驳回原因（5-500字，必填）' : '请输入驳回原因（选填）'" maxlength="500" />
            <view class="approve-bar">
                <template v-if="showRejectInput">
                    <view class="btn cancel" @click="showRejectInput = false">取消</view>
                    <view class="btn reject" @click="doReject">确认驳回</view>
                </template>
                <template v-else>
                    <view class="btn reject" @click="showRejectInput = true">驳回</view>
                    <view class="btn approve" @click="doApprove">审批通过</view>
                </template>
            </view>
        </view>
        <view v-else class="card"><text class="desc">该单据当前不在待审批状态</text></view>
    </view>
</template>

<script>
import { guardPage } from '../../utils/permission'
import { get, post, put } from '../../utils/request'
import config from '../../config'
import { hasFeature } from '../../utils/features'
import { formatAmount, formatDate, CHANGE_STATUS, statusText } from '../../utils/format'

/** 四类单据的详情/审批端点（报销审批为 POST，其余为 PUT） */
const ENDPOINT = {
    expense: {
        detail: '/fee/expense/', approve: config.api.expenseApprove, reject: config.api.expenseReject,
        method: 'POST', pendingStatus: 'SUBMITTED', statusField: 'status'
    },
    payment: {
        detail: '/fee/payment/', approve: config.api.paymentApprove, reject: config.api.paymentReject,
        method: 'PUT', pendingStatus: 'PENDING', statusField: 'status'
    },
    change: {
        detail: '/fee/change/', approve: config.api.changeApprove, reject: config.api.changeReject,
        method: 'PUT', pendingStatus: 'PENDING', statusField: 'changeStatus'
    },
    project: {
        detail: '/fee/project/', approve: config.api.projectApprove, reject: config.api.projectReject,
        method: 'PUT', pendingStatus: 'PENDING_APPROVAL', statusField: 'status'
    }
}

const EVIDENCE = { strong: '证据充分', medium: '证据一般', weak: '证据薄弱' }
const EVIDENCE_CLS = { strong: 'ok', medium: 'warn', weak: 'danger' }

export default {
    data() {
        return {
            CHANGE_STATUS,
            type: 'expense',
            id: null,
            data: {},
            showRejectInput: false,
            rejectReason: '',
            submitting: false,
            showEvidence: hasFeature('FEAT_EVIDENCE_CHAIN')
        }
    },
    computed: {
        /** 仅待审批状态可操作 */
        canAudit() {
            const ep = ENDPOINT[this.type]
            return ep && this.data[ep.statusField] === ep.pendingStatus
        },
        evidenceText() {
            return EVIDENCE[this.data.evidenceLevel] || this.data.evidenceLevel || ''
        },
        evidenceClass() {
            return EVIDENCE_CLS[this.data.evidenceLevel] || ''
        }
    },
    onLoad(options) {
        if (!guardPage('/pages/approval/detail')) return
        if (!options.type || !ENDPOINT[options.type]) {
            uni.showToast({ title: '单据类型错误', icon: 'none' })
            return
        }
        this.type = options.type
        this.id = options.id
        this.loadDetail()
    },
    methods: {
        formatAmount, formatDate, statusText,
        async loadDetail() {
            try {
                const res = await get(ENDPOINT[this.type].detail + this.id)
                this.data = res.data || {}
            } catch (e) { /* 已统一提示 */ }
        },
        /** 审批通过 */
        doApprove() {
            if (this.submitting) return
            uni.showModal({
                title: '审批确认',
                content: '确认通过该单据审批？',
                success: async (res) => {
                    if (!res.confirm) return
                    this.submitting = true
                    try {
                        await this.callAudit(ENDPOINT[this.type].approve)
                        uni.showToast({ title: '审批通过', icon: 'success' })
                        setTimeout(() => uni.navigateBack(), 800)
                    } catch (e) { /* 已统一提示 */ }
                    this.submitting = false
                }
            })
        },
        /** 驳回（报销单原因必填 5-500 字） */
        async doReject() {
            if (this.submitting) return
            const reason = (this.rejectReason || '').trim()
            if (this.type === 'expense' && reason.length < 5) {
                uni.showToast({ title: '驳回原因至少 5 个字', icon: 'none' })
                return
            }
            this.submitting = true
            try {
                const url = ENDPOINT[this.type].reject + '/' + this.id +
                    (reason ? '?reason=' + encodeURIComponent(reason) : '')
                await this.callAudit(url)
                uni.showToast({ title: '已驳回', icon: 'success' })
                setTimeout(() => uni.navigateBack(), 800)
            } catch (e) { /* 已统一提示 */ }
            this.submitting = false
        },
        /** 按单据类型选择 POST/PUT 调用审批端点 */
        callAudit(url) {
            return ENDPOINT[this.type].method === 'POST' ? post(url) : put(url)
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 120rpx; }
.head { background: linear-gradient(135deg, #409eff, #5d7ce6); border-radius: 16rpx; padding: 40rpx 32rpx; margin-bottom: 24rpx; text-align: center; }
.head-amount { display: block; font-size: 56rpx; font-weight: bold; color: #fff; }
.head-amount.neg { color: #ffd7d7; }
.head-sub { display: block; font-size: 24rpx; color: rgba(255, 255, 255, .85); margin-top: 8rpx; }
.tags { display: flex; justify-content: center; flex-wrap: wrap; margin-top: 16rpx; }
.tag { font-size: 20rpx; border-radius: 6rpx; padding: 2rpx 12rpx; margin: 0 8rpx; background: rgba(255, 255, 255, .25); color: #fff; }
.tag.ok { background: rgba(103, 194, 58, .85); }
.tag.warn { background: rgba(230, 162, 60, .85); }
.tag.danger { background: rgba(245, 108, 108, .85); }
.card { background: #fff; border-radius: 16rpx; padding: 28rpx; margin-bottom: 20rpx; }
.desc { font-size: 24rpx; color: #909399; display: block; }
.kv { display: flex; justify-content: space-between; padding: 12rpx 0; font-size: 26rpx; color: #303133; }
.kv text:first-child { color: #909399; flex-shrink: 0; margin-right: 24rpx; }
.kv text:last-child { text-align: right; flex: 1; }
.tip { margin-bottom: 20rpx; }
.approve-box { position: fixed; left: 0; right: 0; bottom: 0; background: #fff; padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom)); box-shadow: 0 -4rpx 16rpx rgba(0, 0, 0, .05); }
.reject-input { width: 100%; box-sizing: border-box; height: 160rpx; background: #f5f7fa; border-radius: 12rpx; padding: 16rpx; font-size: 26rpx; margin-bottom: 16rpx; }
.approve-bar { display: flex; }
.btn { flex: 1; text-align: center; padding: 22rpx 0; border-radius: 12rpx; font-size: 30rpx; font-weight: bold; }
.btn.approve { background: #67c23a; color: #fff; margin-left: 20rpx; }
.btn.reject { background: #fff; color: #f56c6c; border: 1rpx solid #f56c6c; }
.btn.cancel { background: #f0f2f5; color: #606266; margin-right: 20rpx; }
</style>
