<template>
    <view class="page" :class="'theme-' + themeKey" :style="pageStyle">
        <view class="header">
            <text class="title">{{ mode === 'store' ? '选择门店' : '我的店铺' }}</text>
            <text class="subtitle">{{ mode === 'store' ? '请选择要进入的门店，随后使用该门店账号登录' : '同一手机号可归属多个店铺，请选择本次要进入的店铺' }}</text>
        </view>

        <view v-if="!loaded" class="loading">加载中…</view>
        <view v-else-if="!list.length" class="empty">{{ mode === 'store' ? '该租户号下暂无门店' : '暂无可切换的店铺' }}</view>
        <view v-else class="list">
            <view v-for="item in list" :key="item.tenantId" class="store-card"
                  :class="{ active: selectedId === item.tenantId }" @click="choose(item)">
                <image v-if="item.logoUrl" class="logo" :src="toHttps(item.logoUrl)" mode="aspectFill" />
                <view v-else class="logo logo-ph">{{ (item.tenantName || '店').charAt(0) }}</view>
                <view class="info">
                    <text class="name">{{ item.tenantName || '未命名店铺' }}</text>
                    <view class="tags">
                        <text class="tag role">{{ item.roleName || roleText(item.roleCode) }}</text>
                        <text class="tag edition" :class="item.versionType">{{ editionText(item.versionType) }}</text>
                        <text class="tag industry" v-if="item.industryCode">{{ industryIcon(item.industryCode) }} {{ industryText(item.industryCode) }}</text>
                    </view>
                    <text class="city" v-if="item.city">{{ item.city }}</text>
                </view>
                <text class="check" v-if="selectedId === item.tenantId">✓</text>
            </view>
        </view>

        <view class="remember" v-if="list.length && mode !== 'store'" @click="remember = !remember">
            <text class="box" :class="{ on: remember }">{{ remember ? '✓' : '' }}</text>
            <text class="remember-text">记住选择，下次登录默认进入该店铺</text>
        </view>

        <button class="btn-confirm" :disabled="!selectedId" :loading="loading" @click="confirm">{{ mode === 'store' ? '进入门店' : '进入店铺' }}</button>

        <!-- ★ 备案信息（ICP + 公安备案，取自 config.icp） -->
        <icp-footer />
    </view>
</template>

<script>
/**
 * 我的店铺（多租户选择，T15.36§6.4）
 * 白名单页：登录态存在但 tenant 未锁定时可访问，全屏无 Tab；选定后锁定 tenant_id 并跳对应角色首页。
 * 两种进入场景：
 *  ① 手机号登录跨多租户（/micro/login 返回 needSelectTenant=true）→ 读缓存候选，选定后携 tenantId 二次登录换 token；
 *  ② 已登录「我的 → 切换店铺」→ 调 /micro/tenant/myTenants 拉候选，选定后因后端租户上下文依赖 JWT 需重新登录验证。
 */
import config from '../../../config'
import { get, post } from '../../../utils/request'
import { getToken, setToken, setRoles, setUser, clearAuth, setVersionType } from '../../../utils/auth'
import { getTenantList, setTenantList, selectTenant, getSelectedTenant, isMicroVersion, getMicroTabConfig } from '../../../utils/roles'
import { setSelectedStore, getTenantStoreCandidates, clearTenantStoreCandidates } from '../../../utils/store'
import { toHttps } from '../../../utils/format'
import { getThemeSync, loadTenantTheme, loadThemeByTenantId, resolveThemeKey, themeCssVars } from '../../../utils/theme'
import IcpFooter from '../../../components/icp-footer/icp-footer.vue'

export default {
    components: { IcpFooter },
    data() {
        return { loaded: false, loading: false, list: [], selectedId: null, remember: true, mode: 'tenant', themeKey: resolveThemeKey(null), pinnedTheme: null }
    },
    computed: {
        /** M27 T27.5：页面根节点注入主题 CSS 变量（4 风格变体共用） */
        pageStyle() {
            return themeCssVars(this.themeKey)
        }
    },
    onLoad(options) {
        // mode=store：T29.2 登录前按租户号查得的门店列表点选（主动点选，不自动跳转）；
        // 默认 tenant：原有登录后多租户「我的店铺」切换。
        this.mode = (options && options.mode === 'store') ? 'store' : 'tenant'
        // M27 T27.5：?theme= 显式预览 4 风格变体（供 UI 评审选型），否则按租户主题
        if (options && options.theme) {
            this.pinnedTheme = resolveThemeKey(options.theme)
            this.themeKey = this.pinnedTheme
        } else {
            this.themeKey = (getThemeSync() || {}).key || this.themeKey
        }
        if (this.mode === 'store') {
            this.loadStoreCandidates()
        } else {
            this.loadTenants()
        }
    },
    methods: {
        /**
         * ★T29.5 主题刷新：按「已选中 > 列表首项」的 tenantId 走匿名端点 currentByTenant
         * 拉取最新主题（loadTenantTheme 无 token 时回退旧本地缓存，PC 端新选主题不生效）；
         * 点选门店/店铺时实时预览对应租户主题。
         */
        refreshTheme() {
            if (this.pinnedTheme) return
            const target = this.list.find(t => t.tenantId === this.selectedId) || (this.mode === 'store' ? this.list[0] : null)
            const p = (target && target.tenantId) ? loadThemeByTenantId(target.tenantId) : loadTenantTheme()
            p.then(t => { if (t && !this.pinnedTheme) this.themeKey = t.key })
        },
        /** T29.2 登录前门店点选：读取 input.vue 缓存的门店候选（PortalStoreVo），归一为卡片展示字段 */
        loadStoreCandidates() {
            this.loaded = false
            const raw = getTenantStoreCandidates() || []
            // 展开原始门店字段并补 tenantName 别名，兼容模板展示与 setSelectedStore 入参
            this.list = raw.map(s => ({ ...s, tenantName: s.companyName || s.tenantName }))
            if (this.list.length === 1) {
                this.selectedId = this.list[0].tenantId
            }
            this.loaded = true
            this.refreshTheme()
        },
        /** T29.2 门店点选确认：锁定选定门店（公开信息）后进入登录页（租户以登录后 JWT 为准） */
        confirmStore() {
            const target = this.list.find(t => t.tenantId === this.selectedId)
            if (!target) { uni.showToast({ title: '请选择门店', icon: 'none' }); return }
            setSelectedStore(target)
            clearTenantStoreCandidates()
            // ★T29.5 选定门店后按 tenantId 拉取主题并缓存，登录页即可读取正确主题
            console.log('[select] confirmStore - 加载主题 tenantId:', target.tenantId)
            loadThemeByTenantId(target.tenantId).then(theme => {
                console.log('[select] confirmStore - 主题已缓存:', theme.key)
            })
            uni.reLaunch({ url: '/pages/login/index' })
        },
        async loadTenants() {
            this.loaded = false
            // 优先用登录流程缓存的候选；否则登录态拉「我的店铺」
            let list = getTenantList()
            if ((!list || !list.length) && getToken()) {
                try {
                    const res = await get(config.api.microMyTenants)
                    list = (res && res.data) || []
                    setTenantList(list)
                } catch (e) { list = [] }
            }
            this.list = list || []
            // 默认选中：上次记住的租户 > 唯一租户
            const sel = getSelectedTenant()
            if (sel && this.list.some(t => t.tenantId === sel.tenantId)) {
                this.selectedId = sel.tenantId
            } else if (this.list.length === 1) {
                this.selectedId = this.list[0].tenantId
            }
            this.loaded = true
            this.refreshTheme()
        },
        choose(item) { this.selectedId = item.tenantId; this.refreshTheme() },
        async confirm() {
            // T29.2 登录前门店点选模式：锁定门店后直接进入登录页
            if (this.mode === 'store') {
                this.confirmStore()
                return
            }
            const target = this.list.find(t => t.tenantId === this.selectedId)
            if (!target) { uni.showToast({ title: '请选择店铺', icon: 'none' }); return }
            // 记住选择：持久化选定租户（下次登录默认进入）；不记住则仅本次会话生效
            selectTenant(this.remember ? target : null)
            const ctx = uni.getStorageSync('micro_login_context')
            if (ctx && ctx.phonenumber) {
                // 场景①：登录流程中，携 tenantId 二次登录换取该租户 token
                this.loading = true
                try {
                    const res = await post(config.api.microLogin, { ...ctx, tenantId: target.tenantId })
                    const data = (res && res.data) || {}
                    if (data.needSelectTenant) {
                        setTenantList(data.tenants || [])
                        uni.showToast({ title: '请重新选择店铺', icon: 'none' })
                        return
                    }
                    if (data.token) {
                        setToken(data.token)
                        setRoles(data.roles && data.roles.length ? data.roles : (data.roleCode ? [data.roleCode] : []))
                    }
                    // ★T15.36 存储版本档位（登录响应 versionType）
                    if (data.versionType) {
                        setVersionType(data.versionType)
                    }
                    uni.removeStorageSync('micro_login_context')
                    await this.enterHome()
                } catch (e) {
                    // 错误提示已由 request 封装统一处理
                } finally {
                    this.loading = false
                }
            } else if (getToken()) {
                // 场景②：已登录切换店铺，后端租户上下文依赖 JWT，需重新登录验证身份
                uni.showModal({
                    title: '切换店铺',
                    content: '切换到「' + (target.tenantName || '该店铺') + '」需重新登录验证身份，是否继续？',
                    success: (r) => { if (r.confirm) { clearAuth() } }
                })
            } else {
                await this.enterHome()
            }
        },
        /** 刷新用户信息 + 应用角色 Tab 分流（MICRO_TAB_CONFIG 依赖 role + version_type），进入对应角色首页 */
        async enterHome() {
            try {
                const infoRes = await get(config.api.userInfo)
                const data = infoRes && infoRes.data ? infoRes.data : (infoRes || {})
                if (data.user) setUser(data.user)
                if (data.roles) setRoles(data.roles)
            } catch (e) { /* ignore */ }
            // applyRoleTabs 已移至 tab-home/index 的 onShow 中调用（本页非 TabBar 页）
            if (isMicroVersion()) {
                const conf = getMicroTabConfig()
                const firstPage = conf.tabs.find(t => t.page) || {}
                uni.reLaunch({ url: firstPage.page || '/pages/tab-home/index' })
            } else {
                uni.reLaunch({ url: '/pages/tab-home/index' })
            }
        },
        roleText(code) { return { boss: '老板', salesman: '业务员', customer: '顾客' }[code] || '成员' },
        editionText(vt) { return vt === 'micro_pro' ? '专业版' : (vt === 'micro_lite' ? '免费版' : '标准') },
        industryIcon(code) { return { boardgame: '🎲', fresh_food: '🥬', fund_control: '💰' }[code] || '🏪' },
        industryText(code) { return { boardgame: '桌游', fresh_food: '生鲜', fund_control: '资金' }[code] || code },
        toHttps
    }
}
</script>

<style scoped>
.page { padding: 32rpx 24rpx; min-height: 100vh; background: var(--tbg, #f5f6f8); }
.header { padding: 24rpx 8rpx 40rpx; }
.title { font-size: 44rpx; font-weight: bold; color: var(--ttext, #303133); display: block; }
.subtitle { font-size: 24rpx; color: var(--ttext2, #909399); margin-top: 12rpx; display: block; }
.store-card { display: flex; align-items: center; background: #fff; border-radius: var(--tradius, 16rpx); padding: 28rpx; margin-bottom: 20rpx; border: 2rpx solid transparent; }
.store-card.active { border-color: var(--tp, #409eff); background: var(--tp-soft, #ecf5ff); }
.logo { width: 96rpx; height: 96rpx; border-radius: var(--tradius, 16rpx); flex-shrink: 0; }
.logo-ph { background: var(--tp, #409eff); color: #fff; font-size: 40rpx; display: flex; align-items: center; justify-content: center; }
.info { flex: 1; margin-left: 24rpx; overflow: hidden; }
.name { font-size: 32rpx; font-weight: bold; color: var(--ttext, #303133); display: block; }
.tags { display: flex; flex-wrap: wrap; margin-top: 12rpx; }
.tag { font-size: 22rpx; padding: 4rpx 14rpx; border-radius: 8rpx; margin-right: 12rpx; margin-bottom: 8rpx; }
.tag.role { background: var(--tp-soft, #ecf5ff); color: var(--tp, #409eff); }
.tag.edition { background: #f4f4f5; color: #909399; }
.tag.edition.micro_pro { background: #fdf6ec; color: #e6a23c; }
.tag.edition.micro_lite { background: #f0f9eb; color: #67c23a; }
.tag.industry { background: #f4f4f5; color: #606266; }
.city { font-size: 22rpx; color: #c0c4cc; margin-top: 4rpx; display: block; }
.check { font-size: 40rpx; color: var(--tp, #409eff); font-weight: bold; margin-left: 16rpx; }
.remember { display: flex; align-items: center; padding: 24rpx 8rpx; }
.box { width: 36rpx; height: 36rpx; border: 2rpx solid #dcdfe6; border-radius: 8rpx; display: flex; align-items: center; justify-content: center; font-size: 24rpx; color: #fff; margin-right: 16rpx; }
.box.on { background: var(--tp, #409eff); border-color: var(--tp, #409eff); }
.remember-text { font-size: 26rpx; color: var(--ttext2, #606266); }
.btn-confirm { background: var(--tp, #409eff); color: #fff; border-radius: var(--tradius, 12rpx); margin-top: 32rpx; font-size: 32rpx; }
.btn-confirm[disabled] { background: #a0cfff; color: #fff; }
.loading, .empty { text-align: center; color: var(--ttext2, #909399); padding: 120rpx 0; font-size: 28rpx; }

/* M27 T27.5：4 风格变体——结构文案一致，仅配色/圆角/氛围不同 */
.page.theme-vivid { background: var(--tgrad); }
.page.theme-handdrawn { background: var(--tgrad); }
.page.theme-dark { background: var(--tgrad); }
.page.theme-dark .store-card { background: #16213E; }
.page.theme-dark .name { color: #EAEAEA; }
.page.theme-dark .btn-confirm { box-shadow: 0 0 16rpx var(--tp, #00E5FF); }
</style>
