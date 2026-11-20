<template>
    <view class="page" :class="themeClass" :style="pageStyle">
        <!-- ★手绘主题装饰贴纸 -->
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-star-tl" :src="iconUrl('hand-star')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-bubble-tr" :src="iconUrl('hand-bubble')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-sparkle-bl" :src="iconUrl('hand-sparkle')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-cloud-br" :src="iconUrl('hand-cloud')" mode="aspectFit" />

        <!-- ═══ 门店名称 + 地区（渐变头部） ═══ -->
        <view class="shop-header" :style="{ background: headerBg }">
            <text class="shop-name" :style="{ color: headerText }">{{ info.companyName || '门店' }}</text>
            <text v-if="locationText" class="shop-location" :style="{ color: headerSubText }">{{ locationText }}</text>
        </view>

        <!-- ═══ 联系电话（一键拨号 tel:） ═══ -->
        <view class="card">
            <view class="contact-row" @click="callPhone">
                <view class="contact-icon-box phone-box">
                    <text class="contact-icon">📱</text>
                </view>
                <view class="contact-body">
                    <text class="contact-label">联系电话</text>
                    <text class="contact-value">{{ info.contactPhone || '暂无' }}</text>
                </view>
                <text v-if="info.contactPhone" class="contact-action">一键拨号 ›</text>
            </view>
        </view>

        <!-- ═══ 客服微信（长按识别二维码添加） ═══ -->
        <view class="card">
            <view class="contact-row">
                <view class="contact-icon-box wechat-box">
                    <text class="contact-icon">💬</text>
                </view>
                <view class="contact-body">
                    <text class="contact-label">客服微信</text>
                    <text class="contact-value">{{ info.contactWechat || '暂无' }}</text>
                </view>
                <text v-if="info.contactWechat" class="contact-action copy-btn" @click.stop="copyWechat">复制</text>
            </view>
            <text v-if="info.contactWechat" class="wechat-tip">长按微信号复制后，打开微信搜索添加</text>
        </view>

        <!-- ═══ 门店地址 ═══ -->
        <view class="card">
            <view class="contact-row" @click="openMap">
                <view class="contact-icon-box address-box">
                    <text class="contact-icon">📍</text>
                </view>
                <view class="contact-body">
                    <text class="contact-label">门店地址</text>
                    <text class="contact-value">{{ addressText || '暂无地址信息' }}</text>
                </view>
                <text v-if="info.address" class="contact-action">导航 ›</text>
            </view>
        </view>

        <!-- ═══ 服务时间 ═══ -->
        <view class="card">
            <view class="contact-row">
                <view class="contact-icon-box time-box">
                    <text class="contact-icon">🕐</text>
                </view>
                <view class="contact-body">
                    <text class="contact-label">服务时间</text>
                    <text class="contact-value">{{ businessHours || '请致电门店确认营业时间' }}</text>
                </view>
            </view>
        </view>

        <!-- ═══ 投诉入口 ═══ -->
        <view class="card complaint-entry" @click="goComplaint">
            <text class="complaint-text">遇到问题？前往投诉反馈 ›</text>
        </view>
    </view>
</template>

<script>
import { get } from '../../../utils/request'
import config from '../../../config'
import { toHttps } from '../../../utils/format'
import { themeGradient, themeGradientText } from '../../../utils/theme'
import themeMixin from '../../../utils/themeMixin'
import { iconUrl } from '../../../utils/themeIcons'

export default {
    mixins: [themeMixin],
    data() {
        return {
            info: {},
            businessHours: ''
        }
    },
    computed: {
        headerBg() { return themeGradient(this.themeKey) },
        headerText() { return themeGradientText(this.themeKey).text },
        headerSubText() { return themeGradientText(this.themeKey).sub },
        locationText() {
            const parts = []
            if (this.info.city) parts.push(this.info.city)
            return parts.join(' · ')
        },
        addressText() {
            const parts = []
            if (this.info.city) parts.push(this.info.city)
            if (this.info.address) parts.push(this.info.address)
            return parts.join('')
        }
    },
    onShow() {
        this.loadContactInfo()
        this.loadBusinessHours()
    },
    methods: {
        toHttps,
        iconUrl,

        /** ★T32.7 加载门店联系方式（L1 门店信息，登录即可） */
        async loadContactInfo() {
            try {
                const res = await get(config.api.microStoreContactInfo)
                this.info = (res && res.data) || {}
            } catch (e) {
                this.info = {}
            }
        },
        async loadBusinessHours() {
            try {
                const res = await get(config.api.microShopConfig)
                const cfg = (res && res.data) || {}
                this.businessHours = cfg.businessHours || ''
            } catch (e) {
                this.businessHours = ''
            }
        },

        /** ★一键拨号（tel: 链接，零资质） */
        callPhone() {
            if (!this.info.contactPhone) return
            uni.makePhoneCall({
                phoneNumber: this.info.contactPhone,
                fail: () => {
                    // H5 降级：直接打开 tel: 链接
                    window.location.href = 'tel:' + this.info.contactPhone
                }
            })
        },

        /** ★复制微信号 */
        copyWechat() {
            if (!this.info.contactWechat) return
            uni.setClipboardData({
                data: this.info.contactWechat,
                success: () => uni.showToast({ title: '微信号已复制', icon: 'none' })
            })
        },

        /** ★打开地图导航 */
        openMap() {
            if (!this.info.address) return
            uni.openLocation({
                address: this.addressText,
                name: this.info.companyName || '门店',
                fail: () => {
                    uni.showToast({ title: '无法打开地图', icon: 'none' })
                }
            })
        },

        goComplaint() {
            uni.navigateTo({ url: '/pages/micro/customer/complaint' })
        }
    }
}
</script>

<style scoped>
.page { padding-bottom: 40rpx; min-height: 100vh; background: var(--page-bg, #f5f6f8); position: relative; }

/* 门店头部 */
.shop-header { padding: 48rpx 32rpx 36rpx; text-align: center; position: relative; z-index: 1; }
.shop-name { font-size: 36rpx; font-weight: bold; display: block; }
.shop-location { font-size: 26rpx; display: block; margin-top: 8rpx; opacity: .85; }

/* 卡片 */
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 24rpx 28rpx; margin: 0 24rpx 20rpx; box-shadow: var(--shadow-card, none); position: relative; z-index: 1; }

/* 联系行 */
.contact-row { display: flex; align-items: center; }
.contact-icon-box { width: 80rpx; height: 80rpx; border-radius: 16rpx; display: flex; align-items: center; justify-content: center; flex-shrink: 0; }
.phone-box { background: #e8f5e9; }
.wechat-box { background: #e8f5e9; }
.address-box { background: #e3f2fd; }
.time-box { background: #fff3e0; }
.contact-icon { font-size: 36rpx; }
.contact-body { flex: 1; margin-left: 20rpx; }
.contact-label { font-size: 24rpx; color: var(--text-sub, #909399); display: block; }
.contact-value { font-size: 30rpx; color: var(--text, #303133); display: block; margin-top: 4rpx; font-weight: 500; }
.contact-action { font-size: 26rpx; color: var(--brand, #409eff); flex-shrink: 0; margin-left: 12rpx; }
.copy-btn { padding: 6rpx 20rpx; background: var(--brand-light, #ecf5ff); border-radius: var(--radius-tag, 6rpx); }
.wechat-tip { font-size: 22rpx; color: var(--text-sub, #c0c4cc); display: block; margin-top: 12rpx; padding-left: 100rpx; }

/* 投诉入口 */
.complaint-entry { text-align: center; padding: 28rpx; }
.complaint-text { font-size: 28rpx; color: var(--brand, #409eff); }

/* ★手绘主题装饰贴纸 */
.sticker { position: absolute; opacity: .8; pointer-events: none; z-index: 0; }
.sticker-star-tl { top: 16rpx; left: 16rpx; width: 52rpx; height: 52rpx; transform: rotate(-10deg); }
.sticker-bubble-tr { top: 10rpx; right: 16rpx; width: 56rpx; height: 56rpx; transform: rotate(8deg); }
.sticker-sparkle-bl { bottom: 80rpx; left: 16rpx; width: 36rpx; height: 36rpx; transform: rotate(15deg); }
.sticker-cloud-br { bottom: 80rpx; right: 16rpx; width: 72rpx; height: 56rpx; transform: rotate(-5deg); }
</style>
