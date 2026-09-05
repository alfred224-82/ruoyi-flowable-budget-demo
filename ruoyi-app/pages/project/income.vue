<template>
    <view class="page">
        <!-- ========== 核验状态筛选（三态，T8.3） ========== -->
        <view class="filter">
            <view v-for="f in filters" :key="f.value" class="filter-item"
                :class="{ active: query.verifyStatus === f.value }" @click="changeStatus(f.value)">
                <text>{{ f.label }}</text>
            </view>
        </view>

        <!-- ========== 收入确认卡片 ========== -->
        <view v-if="!list.length && !loading" class="card"><text class="desc">暂无收入确认记录</text></view>
        <view v-for="item in list" :key="item.id" class="card">
            <view class="item-head">
                <text class="item-title">{{ item.projectName || ('项目#' + item.projectId) }}</text>
                <text class="amount">¥{{ formatAmount(item.confirmedAmount) }}</text>
            </view>
            <view class="item-row">
                <text class="badge" :style="{ color: statusColor(INCOME_STATUS, item.verifyStatus), borderColor: statusColor(INCOME_STATUS, item.verifyStatus) }">
                    {{ statusText(INCOME_STATUS, item.verifyStatus) }}
                </text>
                <text class="desc">{{ basisText(item.basisType) }}{{ item.basisNo ? ' · ' + item.basisNo : '' }}</text>
            </view>
            <view class="item-row">
                <text class="desc">{{ formatDate(item.confirmDate) }} · {{ item.confirmedByName || '项目经理' }}</text>
            </view>
            <view v-if="item.verifyStatus === 'REJECTED' && item.rejectReason" class="reject-tip">
                <text class="desc">驳回原因：{{ item.rejectReason }}</text>
            </view>
        </view>

        <view v-if="list.length" class="load-tip">
            <text class="desc">{{ finished ? '— 已加载全部 —' : (loading ? '加载中…' : '上拉加载更多') }}</text>
        </view>

        <!-- ========== 新建收入确认入口 ========== -->
        <view class="fab" @click="goCreate"><text class="fab-text">＋</text></view>
    </view>
</template>

<script>
import { guardPage } from '../../utils/permission'
import { get } from '../../utils/request'
import config from '../../config'
import { formatAmount, formatDate, INCOME_STATUS, statusText, statusColor } from '../../utils/format'

/** 依据类型（与后端 basis_type 一致） */
const BASIS = { CONTRACT: '合同', INVOICE: '发票', RECEIPT: '回款单', OTHER: '其他' }

export default {
    data() {
        return {
            INCOME_STATUS,
            filters: [
                { label: '全部', value: '' },
                { label: '待核验', value: 'PENDING' },
                { label: '已核验', value: 'VERIFIED' },
                { label: '已驳回', value: 'REJECTED' }
            ],
            query: { verifyStatus: '', pageNum: 1, pageSize: 10 },
            list: [],
            loading: false,
            finished: false
        }
    },
    onLoad() {
        if (!guardPage('/pages/project/income')) return
        this.loadList(true)
    },
    onShow() {
        // 新建成功返回后刷新
        if (this.list.length) this.loadList(true)
    },
    onPullDownRefresh() {
        this.loadList(true).finally(() => uni.stopPullDownRefresh())
    },
    onReachBottom() {
        if (!this.finished && !this.loading) {
            this.query.pageNum += 1
            this.loadList()
        }
    },
    methods: {
        formatAmount, formatDate, statusText, statusColor,
        /** 收入确认列表（后端数据范围：项目经理仅本人参与项目） */
        async loadList(reset = false) {
            if (reset) {
                this.query.pageNum = 1
                this.list = []
                this.finished = false
            }
            this.loading = true
            try {
                const params = { pageNum: this.query.pageNum, pageSize: this.query.pageSize }
                if (this.query.verifyStatus) params.verifyStatus = this.query.verifyStatus
                const res = await get(config.api.incomeList, params)
                const rows = res.rows || []
                this.list = reset ? rows : this.list.concat(rows)
                this.finished = this.list.length >= (res.total || 0)
            } catch (e) { /* 已统一提示 */ }
            this.loading = false
        },
        changeStatus(status) {
            if (this.query.verifyStatus === status) return
            this.query.verifyStatus = status
            this.loadList(true)
        },
        basisText(type) {
            return BASIS[type] || type || '未填依据'
        },
        goCreate() {
            uni.navigateTo({ url: '/pages/project/incomeCreate' })
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 120rpx; }
.filter { display: flex; margin-bottom: 20rpx; }
.filter-item { flex: 1; text-align: center; font-size: 26rpx; color: #606266; padding: 12rpx 0; background: #fff; border-radius: 8rpx; margin-right: 12rpx; }
.filter-item:last-child { margin-right: 0; }
.filter-item.active { color: #fff; background: #409eff; }
.card { background: #fff; border-radius: 16rpx; padding: 28rpx; margin-bottom: 20rpx; }
.desc { font-size: 24rpx; color: #909399; display: block; }
.item-head { display: flex; align-items: center; margin-bottom: 12rpx; }
.item-title { flex: 1; font-size: 30rpx; font-weight: bold; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.amount { font-size: 30rpx; font-weight: bold; color: #303133; margin-left: 16rpx; flex-shrink: 0; }
.item-row { display: flex; align-items: center; margin-top: 8rpx; }
.badge { font-size: 22rpx; border: 1rpx solid; border-radius: 8rpx; padding: 2rpx 12rpx; margin-right: 16rpx; flex-shrink: 0; }
.reject-tip { margin-top: 12rpx; background: rgba(245, 108, 108, .08); border-radius: 8rpx; padding: 12rpx; }
.load-tip { text-align: center; padding: 16rpx 0; }
.fab { position: fixed; right: 40rpx; bottom: calc(60rpx + env(safe-area-inset-bottom)); width: 100rpx; height: 100rpx; border-radius: 50%; background: linear-gradient(135deg, #409eff, #5d7ce6); box-shadow: 0 8rpx 24rpx rgba(64, 158, 255, .4); display: flex; align-items: center; justify-content: center; }
.fab-text { color: #fff; font-size: 56rpx; line-height: 1; margin-top: -6rpx; }
</style>
