<template>
    <view class="page" :class="'theme-' + theme.key" :style="rootStyle">
        <!-- ★H5推广码：顾客手机浏览器扫码直接进入登录页（带 ref_t/ref_u 参数） -->
        <view class="card qr-card">
            <text class="card-title">我的推广码</text>
            <view v-if="!loaded" class="loading">加载中…</view>
            <block v-else>
                <!-- H5二维码：顾客扫码直接进入登录页 -->
                <view v-if="qrcodeUrl" class="qr-area">
                    <image class="qr-img" :src="toHttps(qrcodeUrl)" mode="widthFix" show-menu-by-longpress />
                    <text class="qr-tip">顾客手机浏览器扫码直接进入登录页，业绩自动归属您</text>
                </view>
                <!-- 生成失败：降级展示 H5 链接 + 重新生成按钮 -->
                <view v-else class="qr-fallback">
                    <text class="scene">{{ h5Url || '暂无推广码' }}</text>
                    <text class="qr-tip">H5推广码生成失败，请点击重新生成</text>
                    <button class="btn" @click="loadQrcode">重新生成</button>
                </view>
                <!-- 归属信息：租户 / 人员 / 角色 -->
                <view class="owner">
                    <text class="owner-item">租户：{{ tenantName || '-' }}</text>
                    <text class="owner-item">人员：{{ userName || '-' }}</text>
                    <text class="owner-item">角色：{{ roleText }}</text>
                </view>
            </block>
        </view>

        <!-- 推广数据 -->
        <view class="card">
            <text class="card-title">推广数据</text>
            <view class="stat-grid">
                <view class="stat">
                    <text class="stat-num">{{ broughtCount }}</text>
                    <text class="stat-label">带来人数</text>
                </view>
                <view class="stat">
                    <text class="stat-num">{{ consumedCount }}</text>
                    <text class="stat-label">已消费</text>
                </view>
                <view class="stat">
                    <text class="stat-num">{{ newCustomerCount }}</text>
                    <text class="stat-label">拉新成功</text>
                </view>
            </view>
        </view>

        <!-- ★V3.7 底部导航 -->
        <micro-tabbar current="/pages/micro/bossPromo/index" />

        <!-- 登出按钮 -->
        <view class="logout-area">
            <button class="logout-btn" @click="handleLogout">退出登录</button>
        </view>
    </view>
</template>

<script>
import { getToken, removeToken } from '../../../utils/auth'
import { get } from '../../../utils/request'
import config from '../../../config'
import { applyNavBarTitle, getTenantName } from '../../../utils/roles'
import { getThemeSync, loadTenantTheme, themeCssVars } from '../../../utils/theme'
import { toHttps } from '../../../utils/format'
import MicroTabbar from '../../../components/micro-tabbar/micro-tabbar.vue'

const ROLE_MAP = {
    boss: '老板',
    salesman: '业务员',
    staff: '员工',
    customer: '顾客'
}

export default {
    components: { MicroTabbar },
    data() {
        return {
            theme: getThemeSync(),
            loaded: false,
            qrcodeUrl: '',
            h5Url: '',
            scene: '',
            userName: '',
            roleKey: '',
            tenantName: '',
            // 推广数据（复用 microPromoMy 聚合）
            broughtCount: 0,
            consumedCount: 0,
            newCustomerCount: 0
        }
    },
    computed: {
        /** ★20261016 四主题重设计：根节点注入完整主题令牌（沿 DOM 继承到全部子元素/子组件） */
        rootStyle() { return themeCssVars(this.theme.key) },
        roleText() { return ROLE_MAP[this.roleKey] || this.roleKey || '-' }
    },
    onShow() {
        if (!getToken()) {
            uni.reLaunch({ url: '/pages/login/index' })
            return
        }
        applyNavBarTitle()
        this.theme = getThemeSync()
        loadTenantTheme().then(t => { this.theme = t })
        this.loadQrcode()
        this.loadPromotion()
    },
    methods: {
        toHttps,
        /** ★H5推广码：后端生成 H5 URL 的二维码（base64 图片），顾客扫码直接进入登录页 */
        async loadQrcode() {
            this.loaded = false
            try {
                const res = await get(config.api.microPromoMyQrcode)
                const data = (res && res.data) || {}
                this.qrcodeUrl = data.qrcodeUrl || ''
                this.h5Url = data.h5Url || ''
                this.scene = data.scene || ''
                this.userName = data.userName || ''
                this.roleKey = data.roleKey || ''
                this.tenantName = data.tenantName || getTenantName() || ''
            } catch (e) {
                this.qrcodeUrl = ''
            } finally {
                this.loaded = true
            }
        },
        /** 推广数据汇总（带来人数 / 已消费 / 拉新成功） */
        async loadPromotion() {
            try {
                const res = await get(config.api.microPromoMy)
                const data = (res && res.data) || {}
                this.broughtCount = data.broughtCount || 0
                this.consumedCount = data.consumedCount || 0
                this.newCustomerCount = data.newCustomerCount || 0
            } catch (e) { /* 静默降级 */ }
        },
        /** 退出登录：清除 token 后返回登录页 */
        handleLogout() {
            uni.showModal({
                title: '提示',
                content: '确认退出登录？',
                success: (r) => {
                    if (r.confirm) {
                        removeToken()
                        uni.reLaunch({ url: '/pages/login/index' })
                    }
                }
            })
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 140rpx; min-height: 100vh; background: var(--page-bg, #f5f6f8); }
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 32rpx; margin-bottom: 24rpx; box-shadow: var(--shadow-card, 0 2rpx 12rpx rgba(0, 0, 0, 0.04)); }
.card-title { font-size: 30rpx; font-weight: bold; display: block; margin-bottom: 24rpx; text-align: center; color: var(--text, #303133); }
.qr-area { text-align: center; }
.qr-img { width: 400rpx; height: 400rpx; display: block; margin: 0 auto; }
.qr-tip { font-size: 24rpx; color: var(--text-sub, #909399); display: block; margin-top: 16rpx; text-align: center; }
.qr-fallback { text-align: center; }
.scene { font-size: 26rpx; color: var(--text-sub, #606266); display: block; word-break: break-all; margin-bottom: 12rpx; }
.btn { background: var(--brand, #409eff); color: #fff; border-radius: var(--radius-btn, 12rpx); font-size: 28rpx; margin-top: 20rpx; }
.owner { display: flex; justify-content: space-around; margin-top: 28rpx; padding-top: 24rpx; border-top: 1rpx solid var(--border, #f0f2f5); }
.owner-item { font-size: 24rpx; color: var(--text-sub, #606266); }
.stat-grid { display: flex; }
.stat { flex: 1; text-align: center; }
.stat-num { font-size: 44rpx; font-weight: bold; color: var(--brand, #409eff); display: block; }
.stat-label { font-size: 24rpx; color: var(--text-sub, #909399); display: block; margin-top: 8rpx; }
.loading { text-align: center; color: var(--text-sub, #909399); font-size: 26rpx; padding: 60rpx 0; }
.logout-area { padding: 40rpx 24rpx 20rpx; }
.logout-btn { background: var(--card-bg, #fff); color: #f56c6c; border: 1rpx solid #f56c6c; border-radius: var(--radius-btn, 12rpx); font-size: 28rpx; }
.logout-btn::after { border: none; }
</style>
