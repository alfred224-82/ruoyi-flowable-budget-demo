<template>
    <view class="store-select">
        <!-- 顶部品牌 -->
        <view class="hero">
            <view class="hero-title">选择门店</view>
            <view class="hero-sub">请选择您要进入的企业/门店，随后使用该门店账号登录</view>
        </view>

        <!-- 定位状态条 -->
        <view class="loc-bar">
            <view class="loc-text">
                <text v-if="locating">正在定位…</text>
                <text v-else-if="located">已定位{{ locatedCity ? '：' + locatedCity : '' }}（按距离就近排序）</text>
                <text v-else class="loc-warn">未获取定位，可手动输入城市或按名称搜索</text>
            </view>
            <text class="loc-btn" @click="reLocate">{{ located ? '重新定位' : '开启定位' }}</text>
        </view>

        <!-- 搜索区 -->
        <view class="search-box">
            <input class="search-input" v-model="keyword" placeholder="搜索门店名称/编码" confirm-type="search" @confirm="onSearch" />
            <input class="search-input city" v-model="city" placeholder="城市（可选）" confirm-type="search" @confirm="onSearch" />
            <button class="search-btn" size="mini" @click="onSearch">搜索</button>
        </view>

        <!-- 公众号入口解析出的归属主体（mpAppId） -->
        <view v-if="pinnedStore" class="section">
            <view class="section-title">归属门店</view>
            <view class="card pinned" @click="selectStore(pinnedStore)">
                <view class="card-main">
                    <view class="card-name">{{ pinnedStore.companyName }}</view>
                    <view class="card-meta">
                        <text v-if="pinnedStore.tenantCode" class="code">{{ pinnedStore.tenantCode }}</text>
                        <text v-if="channelText(pinnedStore.accessChannel)" class="tag">{{ channelText(pinnedStore.accessChannel) }}</text>
                    </view>
                </view>
                <text class="arrow">›</text>
            </view>
        </view>

        <!-- 已绑定门店（openid 反查） -->
        <view v-if="boundTenants.length" class="section">
            <view class="section-title">已绑定门店</view>
            <view class="card" v-for="item in boundTenants" :key="'b' + item.tenantId" @click="selectStore(item)">
                <view class="logo-placeholder">{{ initial(item.companyName) }}</view>
                <view class="card-main">
                    <view class="card-name">
                        {{ item.companyName }}
                        <text v-if="item.isDefault === 1" class="tag default">默认</text>
                    </view>
                    <view class="card-meta">
                        <text v-if="item.tenantCode" class="code">{{ item.tenantCode }}</text>
                        <text v-if="channelText(item.accessChannel)" class="tag">{{ channelText(item.accessChannel) }}</text>
                    </view>
                </view>
                <text class="arrow">›</text>
            </view>
        </view>

        <!-- 门店搜索结果 -->
        <view class="section">
            <view class="section-title">{{ located ? '附近门店' : '全部门店' }}</view>
            <view v-if="loading" class="tip">加载中…</view>
            <block v-else>
                <view class="card" v-for="item in stores" :key="'s' + item.tenantId" @click="selectStore(item)">
                    <image v-if="item.logoUrl" class="logo" :src="item.logoUrl" mode="aspectFill" />
                    <view v-else class="logo-placeholder">{{ initial(item.companyName) }}</view>
                    <view class="card-main">
                        <view class="card-name">{{ item.companyName }}</view>
                        <view class="card-addr">
                            <text v-if="item.city" class="city-name">{{ item.city }}</text>
                            <text v-if="item.address"> {{ item.address }}</text>
                        </view>
                        <view v-if="item.storeIntro" class="card-intro">{{ item.storeIntro }}</view>
                        <view class="card-meta">
                            <text v-if="item.bizCode" class="tag biz">{{ item.bizCode }}</text>
                            <text v-if="channelText(item.accessChannel)" class="tag">{{ channelText(item.accessChannel) }}</text>
                        </view>
                    </view>
                    <view class="card-right">
                        <text v-if="item.distanceKm != null" class="distance">{{ formatDistance(item.distanceKm) }}</text>
                        <text class="arrow">›</text>
                    </view>
                </view>
                <view v-if="!stores.length" class="empty">
                    <view class="empty-text">未找到匹配门店</view>
                    <view class="empty-sub">请调整关键字/城市，或开启定位后重试</view>
                </view>
            </block>
        </view>
    </view>
</template>

<script>
import config from '../../config'
import { get } from '../../utils/request'
import { setSelectedStore } from '../../utils/store'

export default {
    data() {
        return {
            keyword: '',
            city: '',
            stores: [],
            loading: false,
            locating: false,
            located: false,
            locatedCity: '',
            userLng: null,
            userLat: null,
            boundTenants: [],
            pinnedStore: null
        }
    },
    onLoad(options) {
        const opt = options || {}
        // 公众号总入口：携带 mpAppId 时解析归属主体并置顶（仍需用户显式点选，绝不自动登录）
        if (opt.mpAppId) {
            this.resolveByMpAppId(opt.mpAppId)
        }
        // 携带 openid（如公众号网页授权回传）时反查已绑定门店
        if (opt.openid) {
            this.loadBoundTenants(opt.openid)
        }
        // 主流程：尝试定位后加载门店（定位失败自动降级为名称/城市搜索）
        this.tryLocation()
    },
    methods: {
        /** 尝试获取定位（gcj02），成功则按距离就近排序，失败降级 */
        tryLocation() {
            this.locating = true
            uni.getLocation({
                type: 'gcj02',
                success: (res) => {
                    this.located = true
                    this.userLng = res.longitude
                    this.userLat = res.latitude
                    this.locating = false
                    this.loadStores()
                },
                fail: () => {
                    this.located = false
                    this.userLng = null
                    this.userLat = null
                    this.locating = false
                    this.loadStores()
                }
            })
        },
        reLocate() {
            this.tryLocation()
        },
        /** 加载门店列表（/portal/stores，仅返回公开展示字段） */
        loadStores() {
            this.loading = true
            const params = { limit: 50 }
            if (this.keyword && this.keyword.trim()) params.keyword = this.keyword.trim()
            if (this.city && this.city.trim()) params.city = this.city.trim()
            if (this.located && this.userLng != null && this.userLat != null) {
                params.longitude = this.userLng
                params.latitude = this.userLat
            }
            get(config.api.portalStores, params).then(res => {
                this.stores = (res && res.data) || []
                this.loading = false
            }).catch(() => {
                this.stores = []
                this.loading = false
            })
        },
        /** 按 openid 反查已绑定门店（引导态只读，不写会话） */
        loadBoundTenants(openid) {
            get(config.api.portalTenants, { openid }).then(res => {
                this.boundTenants = (res && res.data) || []
            }).catch(() => { this.boundTenants = [] })
        },
        /** 公众号 AppID 解析归属主体 */
        resolveByMpAppId(mpAppId) {
            get(config.api.portalResolve, { mpAppId }).then(res => {
                this.pinnedStore = (res && res.data) || null
            }).catch(() => { this.pinnedStore = null })
        },
        onSearch() {
            this.loadStores()
        },
        /** 用户显式选定门店：缓存公开信息后进入登录页（鉴权态由登录签发 JWT 决定租户） */
        selectStore(store) {
            if (!store || store.tenantId == null) {
                uni.showToast({ title: '门店信息不完整', icon: 'none' })
                return
            }
            setSelectedStore(store)
            uni.reLaunch({ url: '/pages/login/index' })
        },
        channelText(channel) {
            const map = { wechat: '微信', wecom: '企业微信' }
            return map[channel] || ''
        },
        initial(name) {
            return name ? String(name).charAt(0) : '店'
        },
        formatDistance(km) {
            const d = Number(km)
            if (isNaN(d)) return ''
            if (d < 1) return Math.round(d * 1000) + ' m'
            return d.toFixed(1) + ' km'
        }
    }
}
</script>

<style scoped>
.store-select { padding: 24rpx 24rpx 60rpx; }
.hero { padding: 30rpx 10rpx 20rpx; }
.hero-title { font-size: 44rpx; font-weight: bold; color: #303133; }
.hero-sub { font-size: 24rpx; color: #909399; margin-top: 10rpx; }

.loc-bar { display: flex; align-items: center; justify-content: space-between; background: #ecf5ff; border-radius: 12rpx; padding: 18rpx 24rpx; margin-bottom: 20rpx; }
.loc-text { font-size: 24rpx; color: #409eff; flex: 1; }
.loc-warn { color: #e6a23c; }
.loc-btn { font-size: 24rpx; color: #409eff; font-weight: bold; margin-left: 16rpx; }

.search-box { display: flex; align-items: center; margin-bottom: 16rpx; }
.search-input { flex: 1; background: #fff; border-radius: 12rpx; padding: 18rpx 24rpx; font-size: 26rpx; margin-right: 16rpx; }
.search-input.city { flex: 0 0 200rpx; }
.search-btn { background: #409eff; color: #fff; border-radius: 12rpx; margin: 0; }

.section { margin-top: 20rpx; }
.section-title { font-size: 26rpx; color: #606266; font-weight: bold; margin: 16rpx 4rpx 12rpx; }

.card { display: flex; align-items: center; background: #fff; border-radius: 16rpx; padding: 24rpx; margin-bottom: 16rpx; }
.card.pinned { border: 2rpx solid #409eff; }
.logo { width: 88rpx; height: 88rpx; border-radius: 12rpx; background: #f0f2f5; }
.logo-placeholder { width: 88rpx; height: 88rpx; border-radius: 12rpx; background: #409eff; color: #fff; font-size: 40rpx; display: flex; align-items: center; justify-content: center; }
.card-main { flex: 1; margin-left: 20rpx; overflow: hidden; }
.card-name { font-size: 30rpx; font-weight: bold; color: #303133; }
.card-addr { font-size: 24rpx; color: #909399; margin-top: 8rpx; }
.city-name { color: #606266; }
.card-intro { font-size: 22rpx; color: #c0c4cc; margin-top: 6rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.card-meta { margin-top: 10rpx; }
.code { font-size: 22rpx; color: #909399; margin-right: 12rpx; }
.tag { font-size: 20rpx; color: #409eff; background: #ecf5ff; border-radius: 6rpx; padding: 2rpx 12rpx; margin-right: 10rpx; }
.tag.biz { color: #67c23a; background: #f0f9eb; }
.tag.default { color: #e6a23c; background: #fdf6ec; }
.card-right { display: flex; flex-direction: column; align-items: flex-end; justify-content: center; }
.distance { font-size: 24rpx; color: #409eff; font-weight: bold; }
.arrow { font-size: 40rpx; color: #c0c4cc; margin-left: 12rpx; }

.tip { text-align: center; color: #909399; font-size: 26rpx; padding: 60rpx 0; }
.empty { text-align: center; padding: 80rpx 0; }
.empty-text { font-size: 28rpx; color: #606266; }
.empty-sub { font-size: 24rpx; color: #c0c4cc; margin-top: 12rpx; }
</style>
