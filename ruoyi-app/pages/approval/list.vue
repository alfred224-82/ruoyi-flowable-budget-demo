<template>
    <view class="page">
        <!-- ========== 单据类型切换（报销/付款/变更/立项，T8.2） ========== -->
        <view class="filter">
            <view v-for="t in types" :key="t.value" class="filter-item"
                :class="{ active: type === t.value }" @click="changeType(t.value)">
                <text>{{ t.label }}</text>
            </view>
        </view>

        <!-- ========== 待审批卡片 ========== -->
        <view v-if="!list.length && !loading" class="card"><text class="desc">🎉 暂无待审批单据</text></view>

        <view v-for="item in list" :key="type + '-' + item.id" class="card item" @click="goDetail(item)">
            <!-- 报销单 -->
            <template v-if="type === 'expense'">
                <view class="item-head">
                    <text class="item-title">{{ item.billNo }}</text>
                    <text class="amount">¥{{ formatAmount(item.amount) }}</text>
                </view>
                <text class="desc">{{ item.applicantName }}（{{ item.deptName || '-' }}）· {{ item.purposeTagName || '未分类' }}</text>
                <view class="tags">
                    <text v-if="item.invoiceDefer === 1" class="tag warn">发票后补</text>
                    <text v-if="showEvidence && item.evidenceLevel" class="tag"
                        :class="evidenceClass(item.evidenceLevel)">{{ evidenceText(item.evidenceLevel) }}</text>
                    <text v-if="item.projectName" class="tag proj">{{ item.projectName }}</text>
                    <text v-else class="tag">公司级</text>
                </view>
            </template>
            <!-- 付款单 -->
            <template v-else-if="type === 'payment'">
                <view class="item-head">
                    <text class="item-title">{{ item.paymentNo }}</text>
                    <text class="amount">¥{{ formatAmount(item.amount) }}</text>
                </view>
                <text class="desc">{{ item.projectName || '-' }} · 收款方：{{ item.payee || '-' }}</text>
                <view class="tags">
                    <text class="tag" :class="item.invoiceStatus === 'HAS' ? 'ok' : 'warn'">
                        {{ item.invoiceStatus === 'HAS' ? '已有发票' : '发票未到' }}
                    </text>
                </view>
            </template>
            <!-- 合同变更 -->
            <template v-else-if="type === 'change'">
                <view class="item-head">
                    <text class="item-title">{{ item.projectName || ('项目#' + item.projectId) }}</text>
                    <text class="amount" :class="Number(item.contractDelta || 0) < 0 ? 'neg' : ''">
                        {{ Number(item.contractDelta || 0) >= 0 ? '+' : '' }}¥{{ formatAmount(item.contractDelta) }}
                    </text>
                </view>
                <text class="desc">{{ item.changeType || '合同变更' }} · {{ statusText(CHANGE_STATUS, item.changeStatus) }}</text>
            </template>
            <!-- 立项审批 -->
            <template v-else>
                <view class="item-head">
                    <text class="item-title">{{ item.projectName }}</text>
                    <text class="amount">¥{{ formatAmount(item.currentContractAmount) }}</text>
                </view>
                <text class="desc">{{ item.clientName || '-' }} · 经理：{{ item.projectManagerName || '-' }}</text>
            </template>
        </view>

        <view v-if="list.length" class="load-tip">
            <text class="desc">{{ finished ? '— 已加载全部 —' : (loading ? '加载中…' : '上拉加载更多') }}</text>
        </view>
    </view>
</template>

<script>
import { guardPage } from '../../utils/permission'
import { get } from '../../utils/request'
import config from '../../config'
import { hasFeature } from '../../utils/features'
import { formatAmount, CHANGE_STATUS, statusText } from '../../utils/format'

/** 各类型待审批数据源 */
const SOURCE = {
    expense: { url: config.api.expensePending, params: {} },
    payment: { url: config.api.paymentList, params: { status: 'PENDING' } },
    change: { url: config.api.changeList, params: { changeStatus: 'PENDING' } },
    project: { url: config.api.projectList, params: { status: 'PENDING_APPROVAL' } }
}

/** 证据等级标记（FEAT_EVIDENCE_CHAIN，T8.5） */
const EVIDENCE = {
    strong: { label: '证据充分', cls: 'ok' },
    medium: { label: '证据一般', cls: 'warn' },
    weak: { label: '证据薄弱', cls: 'danger' }
}

export default {
    data() {
        return {
            CHANGE_STATUS,
            types: [
                { label: '报销', value: 'expense' },
                { label: '付款', value: 'payment' },
                { label: '变更', value: 'change' },
                { label: '立项', value: 'project' }
            ],
            type: 'expense',
            list: [],
            pageNum: 1,
            pageSize: 10,
            finished: false,
            loading: false,
            showEvidence: hasFeature('FEAT_EVIDENCE_CHAIN')
        }
    },
    onLoad(options) {
        if (!guardPage('/pages/approval/list')) return
        if (options.type && SOURCE[options.type]) this.type = options.type
        this.loadList(true)
    },
    onShow() {
        // 详情页审批完成后返回刷新
        if (this.list.length) this.loadList(true)
    },
    onPullDownRefresh() {
        this.loadList(true).finally(() => uni.stopPullDownRefresh())
    },
    onReachBottom() {
        if (!this.finished && !this.loading) {
            this.pageNum += 1
            this.loadList()
        }
    },
    methods: {
        formatAmount, statusText,
        changeType(type) {
            if (this.type === type) return
            this.type = type
            this.loadList(true)
        },
        /** 按当前类型加载待审批列表 */
        async loadList(reset = false) {
            if (reset) {
                this.pageNum = 1
                this.list = []
                this.finished = false
            }
            this.loading = true
            try {
                const src = SOURCE[this.type]
                const res = await get(src.url, { ...src.params, pageNum: this.pageNum, pageSize: this.pageSize })
                const rows = res.rows || []
                this.list = reset ? rows : this.list.concat(rows)
                this.finished = this.list.length >= (res.total || 0)
            } catch (e) { /* 已统一提示 */ }
            this.loading = false
        },
        evidenceText(level) {
            return (EVIDENCE[level] || {}).label || level
        },
        evidenceClass(level) {
            return (EVIDENCE[level] || {}).cls || ''
        },
        goDetail(item) {
            uni.navigateTo({ url: '/pages/approval/detail?type=' + this.type + '&id=' + item.id })
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 60rpx; }
.filter { display: flex; margin-bottom: 20rpx; }
.filter-item { flex: 1; text-align: center; font-size: 26rpx; color: #606266; padding: 12rpx 0; background: #fff; border-radius: 8rpx; margin-right: 12rpx; }
.filter-item:last-child { margin-right: 0; }
.filter-item.active { color: #fff; background: #409eff; }
.card { background: #fff; border-radius: 16rpx; padding: 28rpx; margin-bottom: 20rpx; }
.desc { font-size: 24rpx; color: #909399; display: block; }
.item-head { display: flex; align-items: center; margin-bottom: 8rpx; }
.item-title { flex: 1; font-size: 30rpx; font-weight: bold; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.amount { font-size: 30rpx; font-weight: bold; color: #303133; margin-left: 16rpx; flex-shrink: 0; }
.amount.neg { color: #f56c6c; }
.tags { display: flex; flex-wrap: wrap; margin-top: 12rpx; }
.tag { font-size: 20rpx; border-radius: 6rpx; padding: 2rpx 12rpx; margin-right: 12rpx; background: #f0f2f5; color: #909399; }
.tag.ok { background: rgba(103, 194, 58, .12); color: #67c23a; }
.tag.warn { background: rgba(230, 162, 60, .12); color: #e6a23c; }
.tag.danger { background: rgba(245, 108, 108, .12); color: #f56c6c; }
.tag.proj { background: rgba(64, 158, 255, .1); color: #409eff; max-width: 300rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.load-tip { text-align: center; padding: 16rpx 0; }
</style>
