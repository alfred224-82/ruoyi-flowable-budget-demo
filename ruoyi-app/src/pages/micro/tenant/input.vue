<template>
    <view class="page" :class="'theme-' + themeKey" :style="pageStyle">
        <view class="brand">
            <text class="brand-name">阿杰小账本</text>
            <text class="brand-sub">请输入平台提供的租户号开始使用</text>
        </view>

        <view class="form-card">
            <text class="label">租户号</text>
            <input class="input" v-model="tenantNo" :maxlength="50" placeholder="请输入租户号" confirm-type="done" @confirm="confirm" />
        </view>

        <button class="btn-confirm" :loading="loading" @click="confirm">下一步</button>

        <!-- ★ 备案信息（ICP + 公安备案，取自 config.icp） -->
        <icp-footer />
    </view>
</template>

<script>
/**
 * Boss 手动输租户号页（T29.2）
 * 小微版 Boss 首次登录第一步：手动输入平台提供的租户号 → 校验合法性 → 拉取门店列表 → 门店列表页点选 → 登录页。
 * 匿名端点：GET /micro/tenant/stores?tenantNo=xxx（校验存在/未停用/未过期，非法由 request 封装统一提示）。
 * 「后续免输」：租户号持久化到 storage（tenantNo），下次冷启动回填并直接分流到门店点选/登录页（T29.4）。
 *
 * ★本页同时承担「入口路由」职责（原 App.vue onLaunch 的分流逻辑已移至此）：
 *   pages.json 第一页 = 本页，H5 框架启动时会自动导航到本页；在其 onLoad/onShow（路由已就绪）中
 *   再决定是否需要跳转，避免与框架初始导航并发导致
 *   「reLaunch:fail Navigation cancelled ... with a new navigation」。
 */
import config from '../../../config'
import { get, post } from '../../../utils/request'
import {
    setTenantNo, getTenantNo,
    setSelectedStore, getSelectedStore,
    setTenantStoreCandidates, getTenantStoreCandidates
} from '../../../utils/store'
import { getToken } from '../../../utils/auth'
import { applyNavBarTitle, isMicroVersion, getMicroTabConfig, getTenantList, getSelectedTenant } from '../../../utils/roles'
import { getThemeSync, loadTenantTheme, loadThemeByTenantId, resolveThemeKey, themeCssVars } from '../../../utils/theme'
import IcpFooter from '../../../components/icp-footer/icp-footer.vue'

export default {
    components: { IcpFooter },
    data() {
        return { tenantNo: '', loading: false, scenePending: false, themeKey: resolveThemeKey(null), pinnedTheme: null }
    },
    computed: {
        /** M27 T27.5：页面根节点注入主题 CSS 变量 */
        pageStyle() {
            return themeCssVars(this.themeKey)
        }
    },
    onShow() {
        applyNavBarTitle()
        // 扫码 scene 处理中：由 resolveSceneLogin 决定去向，此处不再分流
        if (this.scenePending) return
        this.routeByLoginState()
    },
    onLoad(options) {
        // 定位日志：页面源 + 接口前缀（排查「请求未走代理/访问地址错配」）
        console.log('[tenant-input] onLoad page=', typeof window !== 'undefined' ? window.location.href : 'mp', '| baseUrl=', config.baseUrl, '| api=', config.api.microTenantStores)
        // ★T29.3 扫码带租户（t=tenantId&s=storeId&r=role）：校验合法 → 直接进登录页（跳过租户号输入 + 门店点选）
        const scene = this.extractScene(options)
        if (scene) {
            this.scenePending = true
            this.resolveSceneLogin(scene)
            return
        }
        if (options && options.theme) {
            this.pinnedTheme = resolveThemeKey(options.theme)
            this.themeKey = this.pinnedTheme
        } else {
            this.themeKey = (getThemeSync() || {}).key || this.themeKey
            this.refreshTheme()
        }
        // ★T29.4 后续免输：回填上次记住的租户号
        if (!this.tenantNo) this.tenantNo = getTenantNo() || ''
    },
    methods: {
        /**
         * ★T29.5 主题刷新：登录前 tenantId 已知时（门店候选/门店列表响应）走匿名端点
         * currentByTenant 拉取最新主题（loadTenantTheme 无 token 时回退旧本地缓存，
         * 会导致 PC 端新选主题不生效）；拉到后同步写缓存并刷新本页 themeKey。
         */
        refreshTheme(tenantId) {
            if (this.pinnedTheme) return
            const tid = tenantId || (getTenantStoreCandidates()[0] || {}).tenantId
            const p = tid ? loadThemeByTenantId(tid) : loadTenantTheme()
            p.then(t => { if (t && !this.pinnedTheme) this.themeKey = t.key })
        },
        /**
         * ★入口分流（替代原 App.vue onLaunch 跳转，在路由就绪后单点执行，避免导航竞争）
         * 文档 M29 流程：输入租户号（本页）→ 门店点选 → 凭证登录。
         * ★不按「门店候选」自动跳转：本页 → navigateTo 门店点选页后，用户返回本页属正常操作，
         *   若此处再弹回门店页会形成回退死循环；改为回填租户号（T29.4 免输）让用户自行点「下一步」。
         */
        routeByLoginState() {
            // 1) 已登录 → 按角色/租户状态分流（登录态优先于角色缓存：token 失效时 roles 可能残留）
            if (getToken()) {
                if (isMicroVersion()) {
                    const conf = getMicroTabConfig()
                    const firstPage = conf.tabs.find(t => t.page) || {}
                    uni.reLaunch({ url: firstPage.page || '/pages/tab-home/index' })
                } else if (!getSelectedTenant() && getTenantList().length > 1) {
                    // 手机号登录返回候选但尚未选择 →「我的店铺」选择页（T15.36§6.5）
                    uni.reLaunch({ url: '/pages/micro/tenant/select' })
                } else {
                    uni.switchTab({ url: '/pages/tab-home/index' })
                }
                return
            }
            // 2) 未登录 + 已选定门店 → 直接进登录页（T29.4 后续免输）
            if (getSelectedStore()) {
                uni.reLaunch({ url: '/pages/login/index' })
                return
            }
            // 3) 未登录且未选定门店 → 停留本页输入/确认租户号（T29.2 第一步）
        },
        /**
         * T29.3 提取扫码 scene：小程序端启动参数在 options.query.scene，H5/首页 onLoad 直接在 options.scene（需 decodeURIComponent）；
         * 微信启动场景值 scene 为纯数字，非本业务的租户 scene，需排除。
         */
        extractScene(options) {
            const query = (options && (options.query || options)) || {}
            let raw = query.scene || ''
            if (!raw) return ''
            // 纯数字为微信启动场景值（如 1047 扫码进入），不是我们的 t=&s=&r= 参数
            if (/^\d+$/.test(String(raw))) return ''
            try { raw = decodeURIComponent(raw) } catch (e) { /* 已解码则忽略 */ }
            // 仅当含租户标识 t= 时才视为有效 scene
            return /(^|&)t=/.test(raw) ? raw : ''
        },
        /**
         * T29.3 扫码登录：调 /micro/login/resolveScene 校验 scene 合法性（租户存在/未停用/未过期、门店归属），
         * 通过则锁定门店上下文 + 记住租户号 → 直接进登录页；非法则提示「二维码无效」并停留本页。
         */
        async resolveSceneLogin(scene) {
            try {
                const res = await post(config.api.microLoginResolveScene, { scene })
                const data = (res && res.data) || {}
                if (!data.tenantId) {
                    throw new Error('invalid scene')
                }
                setSelectedStore({
                    tenantId: data.tenantId,
                    tenantCode: data.tenantCode,
                    companyName: data.tenantName,
                    city: data.city,
                    logoUrl: data.logoUrl
                })
                // 记住租户号：后续冷启动免输（T29.4）
                if (data.tenantCode) setTenantNo(data.tenantCode)
                uni.reLaunch({ url: '/pages/login/index' })
            } catch (e) {
                uni.showToast({ title: '二维码无效', icon: 'none' })
                this.scenePending = false
            }
        },
        async confirm() {
            const no = (this.tenantNo || '').trim()
            if (!no) {
                uni.showToast({ title: '请输入租户号', icon: 'none' })
                return
            }
            this.loading = true
            try {
                const res = await get(config.api.microTenantStores, { tenantNo: no })
                const list = (res && res.data) || []
                // 定位日志：门店列表响应形状
                console.log('[tenant-input] stores 响应 code=', res && res.code, '| listLen=', list.length, '| raw=', JSON.stringify(res).slice(0, 300))
                if (!list.length) {
                    uni.showToast({ title: '该租户号下暂无门店', icon: 'none' })
                    return
                }
                // 记住租户号（后续免输）：优先存后端回传的标准 tenant_code（用户输门店名称时不回填名称到「租户号」框）
                // + 缓存门店候选，进入门店列表页主动点选（不自动跳转）
                setTenantNo(list[0].tenantCode || no)
                setTenantStoreCandidates(list)
                // ★门店列表已带 tenantId：立即按匿名端点刷新主题，本页与后续门店点选页同步生效
                this.refreshTheme(list[0].tenantId)
                uni.navigateTo({ url: '/pages/micro/tenant/select?mode=store' })
            } catch (e) {
                // 租户号非法/停用/到期：错误提示已由 request 封装统一处理
            } finally {
                this.loading = false
            }
        },
    }
}
</script>

<style scoped>
.page { padding: 60rpx 48rpx; min-height: 100vh; background: var(--tbg, #f5f6f8); }
.brand { padding: 40rpx 8rpx 60rpx; text-align: center; }
.brand-name { font-size: 64rpx; font-weight: bold; color: var(--ttext, #303133); display: block; letter-spacing: 4rpx; }
.brand-sub { font-size: 26rpx; color: var(--ttext2, #909399); margin-top: 20rpx; display: block; }
.form-card { background: var(--tbg, #fff); border-radius: 16rpx; padding: 32rpx 28rpx; }
.label { font-size: 28rpx; color: var(--ttext2, #606266); font-weight: bold; display: block; margin-bottom: 16rpx; }
.input { background: var(--tp-soft, #f5f6f8); border-radius: 12rpx; padding: 24rpx; font-size: 30rpx; color: var(--ttext, #303133); }
.hint { font-size: 22rpx; color: var(--ttext2, #c0c4cc); margin-top: 20rpx; line-height: 1.6; display: block; }
.btn-confirm { background: var(--tp, #409eff); color: #fff; border-radius: var(--tradius, 12rpx); margin-top: 48rpx; font-size: 32rpx; }
</style>
