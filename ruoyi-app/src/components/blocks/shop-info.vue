<template>
    <view class="block shop-info">
        <view v-if="loading" class="block-loading">加载中…</view>
        <view v-else class="info-card">
            <view class="shop-header">
                <view class="shop-avatar" :style="{ background: avatarBg }">
                    {{ (shop.companyName || '店').substring(0, 1) }}
                </view>
                <view class="shop-meta">
                    <text class="shop-name">{{ shop.companyName || '门店' }}</text>
                    <text class="shop-city" v-if="shop.city">{{ shop.city }}</text>
                </view>
            </view>
            <view class="info-row" v-if="shop.address">
                <text class="info-label">地址</text>
                <text class="info-value">{{ shop.address }}</text>
            </view>
            <view class="info-row" v-if="shop.contactPhone">
                <text class="info-label">电话</text>
                <text class="info-value link" @click="callPhone(shop.contactPhone)">{{ shop.contactPhone }}</text>
            </view>
            <view class="info-row" v-if="shop.businessHours">
                <text class="info-label">营业时间</text>
                <text class="info-value">{{ shop.businessHours }}</text>
            </view>
        </view>
    </view>
</template>

<script>
/**
 * 区块组件：店铺信息（T15.16，blockKey=shop_info）
 * 数据源：GET /micro/shop/config
 */
import { get } from '../../utils/request'
import config from '../../config'

export default {
    name: 'BlockShopInfo',
    props: { blockParams: { type: Object, default: () => ({}) } },
    data() { return { loading: false, shop: {} } },
    computed: {
        avatarBg() {
            const name = this.shop.companyName || '店'
            const colors = ['#409eff', '#67c23a', '#e6a23c', '#f56c6c']
            let hash = 0
            for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash)
            return colors[Math.abs(hash) % colors.length]
        }
    },
    created() { this.loadData() },
    methods: {
        callPhone(phone) {
            uni.makePhoneCall({ phoneNumber: phone })
        },
        async loadData() {
            this.loading = true
            try {
                const res = await get(config.api.microShopConfig)
                this.shop = (res && res.data) || {}
            } catch (e) { /* 静默降级 */ }
            this.loading = false
        }
    }
}
</script>

<style scoped>
.block { background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 20rpx; }
.block-loading { text-align: center; color: #909399; padding: 30rpx 0; font-size: 24rpx; }
.shop-header { display: flex; align-items: center; margin-bottom: 16rpx; }
.shop-avatar { width: 72rpx; height: 72rpx; border-radius: 12rpx; color: #fff; font-size: 32rpx; display: flex; align-items: center; justify-content: center; margin-right: 16rpx; }
.shop-meta { flex: 1; }
.shop-name { font-size: 30rpx; font-weight: bold; display: block; }
.shop-city { font-size: 22rpx; color: #909399; display: block; margin-top: 4rpx; }
.info-row { display: flex; padding: 8rpx 0; }
.info-label { width: 140rpx; font-size: 24rpx; color: #909399; flex-shrink: 0; }
.info-value { font-size: 24rpx; color: #303133; flex: 1; }
.info-value.link { color: #409eff; }
</style>
