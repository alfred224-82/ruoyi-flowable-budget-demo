<template>
    <view class="page">
        <!-- ========== 补票入口（有后补单据时提醒） ========== -->
        <view v-if="pendingSupplementCount > 0" class="banner" @click="goSupplement">
            <text class="banner-text">📄 您有 {{ pendingSupplementCount }} 笔发票待补传，点击前往补票 ›</text>
        </view>

        <!-- ========== 状态筛选 ========== -->
        <view class="filter">
            <view v-for="f in filters" :key="f.value" class="filter-item"
                :class="{ active: query.status === f.value }" @click="changeStatus(f.value)">
                <text>{{ f.label }}</text>
            </view>
        </view>

        <!-- ========== 报销单卡片 ========== -->
        <view v-if="!list.length && !loading" class="card"><text class="desc">暂无报销记录</text></view>
        <view v-for="item in list" :key="item.id" class="card">
            <view class="item-head">
                <text class="item-title">{{ item.billNo }}</text>
                <text class="amount">¥{{ formatAmount(item.amount) }}</text>
            </view>
            <view class="item-row">
                <text class="badge" :style="{ color: statusColor(EXPENSE_STATUS, item.status), borderColor: statusColor(EXPENSE_STATUS, item.status) }">
                    {{ statusText(EXPENSE_STATUS, item.status) }}
                </text>
                <text v-if="item.invoiceDefer === 1" class="tag warn">发票后补</text>
                <text class="desc">{{ item.purposeTagName || '未分类' }}</text>
            </view>
            <text class="desc">{{ formatDate(item.expenseDate) }} · {{ item.projectName || '公司级' }} · {{ item.expensePeriod }}</text>
            <view v-if="item.status === 'REJECTED' && item.rejectReason" class="reject-tip">
                <text class="desc">驳回原因：{{ item.rejectReason }}</text>
                <text class="link" @click="goReapply">修改后重新提交 ›</text>
            </view>
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
import { formatAmount, formatDate, EXPENSE_STATUS, statusText, statusColor } from '../../utils/format'

export default {
    data() {
        return {
            EXPENSE_STATUS,
            filters: [
                { label: '全部', value: '' },
                { label: '待审批', value: 'SUBMITTED' },
                { label: '已通过', value: 'APPROVED' },
                { label: '已驳回', value: 'REJECTED' }
            ],
            query: { status: '', pageNum: 1, pageSize: 10 },
            list: [],
            loading: false,
            finished: false,
            pendingSupplementCount: 0
        }
    },
    onLoad() {
        if (!guardPage('/pages/expense/mine')) return
        this.loadList(true)
    },
    onShow() {
        if (this.list.length) this.loadList(true)
        this.loadSupplementCount()
    },
    onPullDownRefresh() {
        Promise.all([this.loadList(true), this.loadSupplementCount()])
            .finally(() => uni.stopPullDownRefresh())
    },
    onReachBottom() {
        if (!this.finished && !this.loading) {
            this.query.pageNum += 1
            this.loadList()
        }
    },
    methods: {
        formatAmount, formatDate, statusText, statusColor,
        /** 我的报销列表（后端已按当前申请人过滤） */
        async loadList(reset = false) {
            if (reset) {
                this.query.pageNum = 1
                this.list = []
                this.finished = false
            }
            this.loading = true
            try {
                const params = { pageNum: this.query.pageNum, pageSize: this.query.pageSize }
                if (this.query.status) params.status = this.query.status
                const res = await get(config.api.expenseList, params)
                const rows = res.rows || []
                this.list = reset ? rows : this.list.concat(rows)
                this.finished = this.list.length >= (res.total || 0)
            } catch (e) { /* 已统一提示 */ }
            this.loading = false
        },
        /** 待补票数量（提醒条） */
        async loadSupplementCount() {
            try {
                const res = await get(config.api.supplementList, { status: 'PENDING', pageNum: 1, pageSize: 50 })
                this.pendingSupplementCount = res.total || 0
            } catch (e) { /* 补票接口异常不打扰主流程 */ }
        },
        changeStatus(status) {
            if (this.query.status === status) return
            this.query.status = status
            this.loadList(true)
        },
        goSupplement() {
            uni.navigateTo({ url: '/pages/expense/supplement' })
        },
        goReapply() {
            uni.switchTab({ url: '/pages/tab-work/index' })
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 60rpx; }
.banner { background: rgba(230, 162, 60, .12); border-radius: 12rpx; padding: 20rpx; margin-bottom: 20rpx; }
.banner-text { font-size: 26rpx; color: #e6a23c; font-weight: bold; }
.filter { display: flex; margin-bottom: 20rpx; }
.filter-item { flex: 1; text-align: center; font-size: 26rpx; color: #606266; padding: 12rpx 0; background: #fff; border-radius: 8rpx; margin-right: 12rpx; }
.filter-item:last-child { margin-right: 0; }
.filter-item.active { color: #fff; background: #409eff; }
.card { background: #fff; border-radius: 16rpx; padding: 28rpx; margin-bottom: 20rpx; }
.desc { font-size: 24rpx; color: #909399; display: block; }
.item-head { display: flex; align-items: center; margin-bottom: 12rpx; }
.item-title { flex: 1; font-size: 30rpx; font-weight: bold; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.amount { font-size: 30rpx; font-weight: bold; color: #303133; margin-left: 16rpx; flex-shrink: 0; }
.item-row { display: flex; align-items: center; margin-bottom: 8rpx; }
.badge { font-size: 22rpx; border: 1rpx solid; border-radius: 8rpx; padding: 2rpx 12rpx; margin-right: 12rpx; flex-shrink: 0; }
.tag { font-size: 20rpx; border-radius: 6rpx; padding: 2rpx 12rpx; margin-right: 12rpx; }
.tag.warn { background: rgba(230, 162, 60, .12); color: #e6a23c; }
.reject-tip { margin-top: 12rpx; background: rgba(245, 108, 108, .08); border-radius: 8rpx; padding: 12rpx; }
.link { color: #409eff; font-size: 24rpx; margin-top: 8rpx; display: inline-block; }
.load-tip { text-align: center; padding: 16rpx 0; }
</style>
