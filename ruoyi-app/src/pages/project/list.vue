<template>
    <view class="page">
        <!-- ========== 状态筛选 ========== -->
        <view class="filter">
            <view v-for="f in filters" :key="f.value" class="filter-item"
                :class="{ active: query.status === f.value }" @click="changeStatus(f.value)">
                <text>{{ f.label }}</text>
            </view>
        </view>

        <!-- ========== 项目卡片 ========== -->
        <view v-if="!list.length && !loading" class="card"><text class="desc">暂无项目数据</text></view>
        <view v-for="p in list" :key="p.id" class="card proj" @click="goDetail(p)">
            <view class="proj-head">
                <text class="proj-name">{{ p.projectName }}</text>
                <text class="badge" :style="{ color: statusColor(PROJECT_STATUS, p.status), borderColor: statusColor(PROJECT_STATUS, p.status) }">
                    {{ statusText(PROJECT_STATUS, p.status) }}
                </text>
            </view>
            <text class="desc">{{ p.clientName || '-' }}　经理：{{ p.projectManagerName || '-' }}</text>

            <!-- 预算使用进度（🟢🟡🔴 按使用率，设计文档 7.3） -->
            <view class="budget">
                <view class="budget-bar">
                    <view class="budget-fill" :style="{ width: usedPercent(p) + '%', background: usedColor(p) }"></view>
                </view>
                <text class="budget-text">{{ usedPercent(p) }}%　¥{{ formatWan(p.usedBudget) }} / ¥{{ formatWan(p.totalBudget) }}</text>
            </view>

            <view class="proj-foot">
                <view class="pf-item"><text class="pf-num">¥{{ formatWan(p.currentContractAmount) }}</text><text class="pf-label">当前合同额</text></view>
                <view class="pf-item"><text class="pf-num">¥{{ formatWan(p.verifiedIncome) }}</text><text class="pf-label">已核验收入</text></view>
                <view class="pf-item"><text class="pf-num">¥{{ formatWan(p.totalExpense) }}</text><text class="pf-label">总支出</text></view>
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
import { formatWan, PROJECT_STATUS, statusText, statusColor } from '../../utils/format'

export default {
    data() {
        return {
            PROJECT_STATUS,
            filters: [
                { label: '全部', value: '' },
                { label: '进行中', value: 'ACTIVE' },
                { label: '待审批', value: 'PENDING_APPROVAL' },
                { label: '已完成', value: 'COMPLETED' },
                { label: '烂账', value: 'LOST' }
            ],
            query: { status: '', pageNum: 1, pageSize: 10 },
            list: [],
            total: 0,
            loading: false,
            finished: false
        }
    },
    onLoad() {
        if (!guardPage('/pages/project/list')) return
        this.loadList(true)
    },
    onShow() {
        // 详情页审批/关闭后返回刷新
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
        formatWan, statusText, statusColor,
        /** 项目列表（后端按角色控制数据范围：项目经理仅本人参与项目，fee_project_member） */
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
                const res = await get(config.api.projectList, params)
                const rows = res.rows || []
                this.list = reset ? rows : this.list.concat(rows)
                this.total = res.total || 0
                this.finished = this.list.length >= this.total
            } catch (e) { /* 已统一提示 */ }
            this.loading = false
        },
        changeStatus(status) {
            if (this.query.status === status) return
            this.query.status = status
            this.loadList(true)
        },
        /** 预算使用率 */
        usedPercent(p) {
            const total = Number(p.totalBudget || 0)
            if (!total) return 0
            return Math.min(100, Math.round((Number(p.usedBudget || 0) / total) * 100))
        },
        /** 使用率色：<70% 绿，70%~90% 黄，>90% 红 */
        usedColor(p) {
            const percent = this.usedPercent(p)
            if (percent > 90) return '#f56c6c'
            if (percent >= 70) return '#e6a23c'
            return '#67c23a'
        },
        goDetail(p) {
            uni.navigateTo({ url: '/pages/project/detail?id=' + p.id })
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
.proj-head { display: flex; align-items: center; margin-bottom: 8rpx; }
.proj-name { flex: 1; font-size: 32rpx; font-weight: bold; color: #303133; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.badge { font-size: 22rpx; border: 1rpx solid; border-radius: 8rpx; padding: 2rpx 12rpx; margin-left: 12rpx; flex-shrink: 0; }
.budget { margin-top: 16rpx; }
.budget-bar { height: 12rpx; background: #f0f2f5; border-radius: 6rpx; overflow: hidden; }
.budget-fill { height: 100%; border-radius: 6rpx; transition: width .3s; }
.budget-text { font-size: 22rpx; color: #909399; margin-top: 8rpx; display: block; }
.proj-foot { display: flex; margin-top: 20rpx; padding-top: 20rpx; border-top: 1rpx solid #f0f2f5; }
.pf-item { flex: 1; text-align: center; }
.pf-num { display: block; font-size: 28rpx; font-weight: bold; color: #303133; }
.pf-label { font-size: 22rpx; color: #909399; }
.load-tip { text-align: center; padding: 16rpx 0; }
</style>
