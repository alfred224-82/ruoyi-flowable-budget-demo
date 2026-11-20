<template>
    <view class="page">
        <!-- 状态筛选 -->
        <view class="filter-bar">
            <view v-for="s in statusOptions" :key="s.value" class="filter-item"
                  :class="{ active: currentStatus === s.value }" @click="changeStatus(s.value)">
                <text>{{ s.label }}</text>
            </view>
        </view>

        <!-- 预约列表 -->
        <view v-if="!loaded" class="loading">加载中…</view>
        <view v-else-if="!list.length" class="empty">暂无预约订单</view>
        <view v-else class="list">
            <view v-for="item in list" :key="(item.sourceType || 'B') + '-' + item.id" class="card" @click="goDetail(item)">
                <view class="card-header">
                    <text class="order-no">{{ item.sourceType === 'ACTIVITY' ? (item.serviceName || '活动') : (item.customerName || '顾客') }}</text>
                    <view class="header-right">
                        <text v-if="item.sourceType === 'ACTIVITY'" class="tag-activity">活动</text>
                        <text class="status" :style="{ color: statusColor(item.orderStatus) }">{{ statusText(item.orderStatus) }}</text>
                    </view>
                </view>
                <view class="card-body">
                    <text class="info">服务项目：{{ item.serviceItemName || item.serviceName || '-' }}</text>
                    <text class="info">预约时间：{{ item.bookingTime || '-' }}</text>
                    <text v-if="item.amount != null" class="info">金额：¥{{ formatAmount(item.amount) }}</text>
                    <text v-if="item.staffName" class="info">业务员：{{ item.staffName }}</text>
                </view>
                <view v-if="item.sourceType !== 'ACTIVITY'" class="card-actions">
                    <button v-if="item.orderStatus === 'PENDING_SERVICE'" class="btn-sm primary"
                            @click.stop="completeOrder(item.id)">完成服务</button>
                    <button v-if="item.payStatus === 'PENDING_PAY' && item.orderStatus !== 'CANCELLED'" class="btn-sm success"
                            @click.stop="receiveOrder(item.id)">标记收款</button>
                    <button v-if="item.orderStatus === 'PENDING_SERVICE'" class="btn-sm warn"
                            @click.stop="cancelOrder(item.id)">取消</button>
                </view>
            </view>
        </view>

        <!-- 新建预约浮动按钮 -->
        <view class="fab" @click="go('/pages/micro/booking/add')">+</view>
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get, put } from '../../../utils/request'
import config from '../../../config'
import { formatAmount, BOOKING_STATUS } from '../../../utils/format'

export default {
    data() {
        return {
            loaded: false,
            list: [],
            currentStatus: '',
            statusOptions: [
                { label: '全部', value: '' },
                { label: '待服务', value: 'PENDING_SERVICE' },
                { label: '未收款', value: 'PENDING_PAY' },
                { label: '已完成', value: 'COMPLETED' },
                { label: '已取消', value: 'CANCELLED' }
            ]
        }
    },
    onShow() {
        if (!getToken()) {
            uni.reLaunch({ url: '/pages/login/index' })
            return
        }
        this.loadList()
    },
    methods: {
        formatAmount,
        statusText(status) { return (BOOKING_STATUS[status] || {}).label || status || '-' },
        statusColor(status) { return (BOOKING_STATUS[status] || {}).color || '#909399' },
        go(url) { uni.navigateTo({ url }) },
        goDetail(item) {
            // 活动行（boss 后台创建）不跳转订单详情，仅顾客预约订单可进详情
            if (item.sourceType === 'ACTIVITY') return
            uni.navigateTo({ url: '/pages/micro/booking/detail?id=' + item.id })
        },
        changeStatus(status) {
            this.currentStatus = status
            this.loadList()
        },
        async loadList() {
            this.loaded = false
            try {
                const params = { pageNum: 1, pageSize: 50 }
                // ★「未收款」为收款状态筛选，其余为订单状态筛选
                if (this.currentStatus === 'PENDING_PAY') params.payStatus = this.currentStatus
                else if (this.currentStatus) params.orderStatus = this.currentStatus
                const res = await get(config.api.microBookingList, params)
                this.list = (res && res.rows) || []
            } catch (e) {
                this.list = []
            } finally {
                this.loaded = true
            }
        },
        async completeOrder(id) {
            uni.showModal({
                title: '确认', content: '确认该预约已完成服务？',
                success: async (res) => {
                    if (res.confirm) {
                        try {
                            await put(config.api.microBookingComplete + '/' + id)
                            uni.showToast({ title: '已完成', icon: 'success' })
                            this.loadList()
                        } catch (e) { /* 已统一提示 */ }
                    }
                }
            })
        },
        async receiveOrder(id) {
            uni.showModal({
                title: '确认收款', content: '确认已线下收到该笔款项？',
                success: async (res) => {
                    if (res.confirm) {
                        try {
                            await put(config.api.microBookingReceive + '/' + id)
                            uni.showToast({ title: '已收款', icon: 'success' })
                            this.loadList()
                        } catch (e) { /* 已统一提示 */ }
                    }
                }
            })
        },
        async cancelOrder(id) {
            uni.showModal({
                title: '确认取消', content: '确认取消该预约？',
                success: async (res) => {
                    if (res.confirm) {
                        try {
                            await put(config.api.microBookingCancel + '/' + id)
                            uni.showToast({ title: '已取消', icon: 'success' })
                            this.loadList()
                        } catch (e) { /* 已统一提示 */ }
                    }
                }
            })
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 120rpx; }
.filter-bar { display: flex; background: #fff; border-radius: 12rpx; padding: 12rpx; margin-bottom: 24rpx; }
.filter-item { flex: 1; text-align: center; padding: 16rpx 0; font-size: 26rpx; color: #606266; border-radius: 8rpx; }
.filter-item.active { background: #409eff; color: #fff; }
.card { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 20rpx; }
.card-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.header-right { display: flex; align-items: center; gap: 12rpx; }
.tag-activity { font-size: 22rpx; color: #409eff; background: rgba(64,158,255,0.1); padding: 2rpx 12rpx; border-radius: 6rpx; }
.order-no { font-size: 30rpx; font-weight: bold; }
.status { font-size: 26rpx; font-weight: bold; }
.card-body { }
.info { display: block; font-size: 26rpx; color: #606266; margin-bottom: 8rpx; }
.card-actions { display: flex; justify-content: flex-end; margin-top: 16rpx; gap: 16rpx; }
.btn-sm { font-size: 24rpx; padding: 8rpx 24rpx; border-radius: 8rpx; border: none; }
.btn-sm.primary { background: #409eff; color: #fff; }
.btn-sm.success { background: #67c23a; color: #fff; }
.btn-sm.warn { background: #f56c6c; color: #fff; }
.fab { position: fixed; right: 40rpx; bottom: 120rpx; width: 100rpx; height: 100rpx; background: #409eff; color: #fff; font-size: 56rpx; border-radius: 50%; display: flex; align-items: center; justify-content: center; box-shadow: 0 4rpx 16rpx rgba(64,158,255,0.4); }
.loading, .empty { text-align: center; color: #909399; font-size: 26rpx; padding: 80rpx 0; }
</style>
