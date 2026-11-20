<template>
    <view class="login" :class="'theme-' + themeKey" :style="pageStyle">
        <!-- ★手绘主题装饰贴纸（仅 theme_handdrawn 显示） -->
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-cloud-tl" :src="iconUrl('hand-cloud')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-star-tr" :src="iconUrl('hand-star')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-sparkle-l" :src="iconUrl('hand-sparkle')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-heart-r" :src="iconUrl('hand-heart')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-star-bl" :src="iconUrl('hand-star')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-cloud-bl" :src="iconUrl('hand-cloud')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-sparkle-bl" :src="iconUrl('hand-sparkle')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-bubble-br" :src="iconUrl('hand-bubble')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-bubble-br-sm" :src="iconUrl('hand-bubble')" mode="aspectFit" />

        <!-- T29.1 分层：系统名「阿杰小账本」固定小字 + 租户名大字号展示（M27 T27.5：4 风格变体仅配色/圆角/氛围不同，结构文案一致） -->
        <view class="brand">阿杰小账本</view>
        <view class="logo">{{ store ? store.companyName : '欢迎登录' }}</view>
        <view class="store-banner" v-if="store">
            <view class="store-info">
                <text class="store-name">{{ store.companyName }}</text>
                <text class="store-city" v-if="store.city">{{ store.city }}</text>
            </view>
            <text class="store-switch" @click="switchStore">切换门店</text>
        </view>
        <view class="form">
            <input class="input" v-model="form.username" placeholder="请输入账号" />
            <input class="input" v-model="form.password" type="password" placeholder="请输入密码" />
            <button class="btn" :loading="loading" @click="handleLogin">登 录</button>
            <!-- #ifdef MP-WEIXIN -->
            <button class="btn-wx" :loading="loading" @click="handleWxLogin">微信code登录</button>
            <!-- T15.8 手机号一键登录（wx.getPhoneNumber → /micro/login，后端解密 phoneCode 直取手机号） -->
            <button class="btn-phone" :loading="loading" open-type="getPhoneNumber" @getphonenumber="handlePhoneLogin">手机号一键登录</button>
            <!-- #endif -->
        </view>
        <!-- ★ 备案信息（ICP + 公安备案，取自 config.icp） -->
        <icp-footer />
    </view>
</template>

<script>
import config from '../../config'
import { post, get } from '../../utils/request'
import { getToken, setToken, setRoles, setUser, setOrg } from '../../utils/auth'
import { getSelectedTenant } from '../../utils/roles'
import { isMicroVersion, getMicroTabConfig, applyNavBarTitle } from '../../utils/roles'
import { setEditionFeatures, loadEditionFeatures, hasFeature } from '../../utils/features'
import { setVersionType } from '../../utils/auth'
import { getSelectedStore, getTenantStoreCandidates, clearSelectedStore, clearTenantStoreCandidates } from '../../utils/store'
import { getThemeSync, loadTenantTheme, loadThemeByTenantId, resolveThemeKey, themeCssVars } from '../../utils/theme'
import { iconUrl } from '../../utils/themeIcons'
import IcpFooter from '../../components/icp-footer/icp-footer.vue'

export default {
    components: { IcpFooter },
    data() {
        return {
            loading: false,
            store: null,
            // M27 T27.5：当前主题 key（?theme= 显式预览优先，否则租户主题）
            themeKey: resolveThemeKey(null),
            pinnedTheme: null,
            form: { username: '', password: '', code: '', uuid: '' },
            // ★H5推广码归因：顾客扫码进入登录页时携带 ref_t(租户ID) + ref_u(推广人用户ID)
            refTenantId: null,
            refUserId: null
        }
    },
    computed: {
        /** M27 T27.5：页面根节点注入主题 CSS 变量（4 风格变体共用） */
        pageStyle() {
            return themeCssVars(this.themeKey)
        }
    },
    /** ★H5推广码：从 URL 参数提取 ref_t / ref_u（顾客手机浏览器扫码进入）；★M27 T27.5：?theme= 预览 4 风格变体 */
    onLoad(options) {
        if (options) {
            if (options.ref_t) {
                this.refTenantId = options.ref_t
                try { uni.setStorageSync('promo_ref_t', options.ref_t) } catch (e) {}
            }
            if (options.ref_u) {
                this.refUserId = options.ref_u
                try { uni.setStorageSync('promo_ref_u', options.ref_u) } catch (e) {}
            }
            // ?theme=theme_fresh|theme_vivid|theme_dark|theme_handdrawn 显式预览（供 UI 评审选型）
            if (options.theme) {
                this.pinnedTheme = resolveThemeKey(options.theme)
                this.themeKey = this.pinnedTheme
            }
        }
    },
    onShow() {
        // 引导态：未显式选定门店时回退到文档 M29 流程（租户号输入 → 门店点选）
        this.store = getSelectedStore()
        console.log('[login] onShow - store:', this.store ? this.store.companyName : 'null', '| themeKey:', this.themeKey, '| pinnedTheme:', this.pinnedTheme)
        if (!this.store) {
            this.goStorePick()
            return
        }
        // M27 T27.5：无 ?theme= 显式预览时，按租户主题渲染（首屏缓存 + 拉取最新）
        // ★置于门店校验之后：避免未登录时白跑一次请求（401 会触发 clearAuth 跳转，与本页跳转竞争）
        if (!this.pinnedTheme) {
            const cachedTheme = getThemeSync()
            console.log('[login] onShow - getThemeSync() 缓存主题:', JSON.stringify(cachedTheme))
            this.themeKey = (cachedTheme || {}).key || this.themeKey
            console.log('[login] onShow - 缓存赋值后 themeKey:', this.themeKey)
            // ★T29.5 有门店但未登录时，按 tenantId 拉取主题（匿名接口）；已登录则走原逻辑
            if (this.store && this.store.tenantId && !getToken()) {
                console.log('[login] onShow - 按 tenantId 加载主题:', this.store.tenantId)
                loadThemeByTenantId(this.store.tenantId).then(t => {
                    console.log('[login] onShow - loadThemeByTenantId() 返回:', JSON.stringify(t))
                    if (t && !this.pinnedTheme) {
                        this.themeKey = t.key
                        console.log('[login] onShow - tenantId 赋值后 themeKey:', this.themeKey)
                    }
                }).catch(e => {
                    console.error('[login] onShow - loadThemeByTenantId() 异常:', e)
                })
            } else {
                loadTenantTheme().then(t => {
                    console.log('[login] onShow - loadTenantTheme() 返回:', JSON.stringify(t))
                    if (t && !this.pinnedTheme) {
                        this.themeKey = t.key
                        console.log('[login] onShow - API 赋值后 themeKey:', this.themeKey)
                    }
                }).catch(e => {
                    console.error('[login] onShow - loadTenantTheme() 异常:', e)
                })
            }
        } else {
            console.log('[login] onShow - pinnedTheme 存在，跳过主题加载，pinnedTheme:', this.pinnedTheme)
        }
        // 导航栏标题固定「阿杰小账本」（T29.1），租户名已在页内大字号展示
        applyNavBarTitle()
    },
    methods: {
        iconUrl,
        /**
         * ★门店点选回退（M29 文档流程：租户号输入 → 门店点选 → 凭证登录）：
         *   有门店候选 → 门店点选页；无候选 → 租户号输入页。
         *   绝不使用旧版 /pages/store/select（旧引导流程，与文档不一致）。
         */
        goStorePick() {
            if (getTenantStoreCandidates().length) {
                uni.reLaunch({ url: '/pages/micro/tenant/select?mode=store' })
            } else {
                uni.reLaunch({ url: '/pages/micro/tenant/input' })
            }
        },
        /** 切换门店：清除已选门店与候选，避免 input.vue routeByLoginState 检测到旧门店弹回登录页 */
        switchStore() {
            clearSelectedStore()
            clearTenantStoreCandidates()
            this.store = null
            uni.reLaunch({ url: '/pages/micro/tenant/input' })
        },
        async handleLogin() {
            if (!this.form.username || !this.form.password) {
                uni.showToast({ title: '请输入账号和密码', icon: 'none' })
                return
            }
            this.loading = true
            try {
                // 登录拿 token（携带选定门店 tenantId，后端校验账号归属防跨门店串号 T13.1；租户到期/停用会被后端拦截 T3.4）
                // 切换店铺重新登录时优先带入「我的店铺」选定租户（T15.36§6.2），否则用引导态选定门店
                const selTenant = getSelectedTenant()
                const loginTenantId = selTenant ? selTenant.tenantId : (this.store ? this.store.tenantId : null)
                const loginBody = { ...this.form, tenantId: loginTenantId }
                const loginRes = await post(config.api.login, loginBody)
                const token = (loginRes.data && loginRes.data.token) || loginRes.data
                await this.afterLogin(token)
            } catch (e) {
                // 错误提示已由 request 封装统一处理
            } finally {
                this.loading = false
            }
        },
        // #ifdef MP-WEIXIN
        /** 微信一键登录（T13.1 code2session）：wx.login 拿 code + 选定门店 tenantId → /xcxLogin。
         *  需后端配置 wx.miniapp.appid/secret，且该微信已绑定门店账号（首次请先用账号密码登录回填 userId） */
        async handleWxLogin() {
            if (!this.store) {
                this.goStorePick()
                return
            }
            this.loading = true
            try {
                const code = await this.wxLoginCode()
                if (!code) {
                    uni.showToast({ title: '微信登录失败，请重试', icon: 'none' })
                    return
                }
                const res = await post(config.api.xcxLogin, { xcxCode: code, tenantId: this.store.tenantId })
                const token = (res.data && res.data.token) || res.data
                await this.afterLogin(token)
            } catch (e) {
                // 错误提示已由 request 封装统一处理（未配置 appid/未绑定账号时后端返回明确提示）
            } finally {
                this.loading = false
            }
        },
        /** 封装 uni.login 获取微信临时登录凭证 code */
        wxLoginCode() {
            return new Promise((resolve) => {
                uni.login({
                    provider: 'weixin',
                    success: (r) => resolve(r.code),
                    fail: () => resolve(null)
                })
            })
        },
        // #endif
        // #ifdef MP-WEIXIN
        /**
         * T15.8 手机号一键登录（小程序专属）：
         * 用户点击 → wx.getPhoneNumber 授权弹窗 → 后端 /micro/login 解密 phoneCode 直取手机号。
         * 多租户时返回 needSelectTenant + tenants 候选，前端弹「我的店铺」选择页。
         */
        async handlePhoneLogin(e) {
            const phoneCode = (e.detail && e.detail.code) || ''
            if (!phoneCode) {
                // 用户拒绝授权或授权失败
                uni.showToast({ title: '手机号授权失败，请重试', icon: 'none' })
                return
            }
            this.loading = true
            try {
                const selTenant = getSelectedTenant()
                const loginTenantId = selTenant ? selTenant.tenantId : (this.store ? this.store.tenantId : null)
                const loginRes = await post(config.api.microLogin, {
                    phoneCode: phoneCode,
                    clientType: 'mini',
                    tenantId: loginTenantId
                })
                const data = loginRes.data || loginRes || {}
                // 多租户候选：缓存列表 → 引导选择页
                if (data.needSelectTenant && data.tenants && data.tenants.length > 0) {
                    const { setTenantList } = await import('../../utils/roles')
                    setTenantList(data.tenants)
                    uni.reLaunch({ url: '/pages/micro/tenant/select' })
                    return
                }
                if (!data.token) {
                    uni.showToast({ title: '登录失败，请重试', icon: 'none' })
                    return
                }
                // ★T15.36 直接使用登录响应中的 roleCode（boss/salesman/customer），避免 /getInfo 返回系统角色导致 isMicroVersion() 误判
                const loginRole = data.roleCode || ''
                if (loginRole) {
                    setRoles([loginRole])
                }
                // ★T15.36 存储版本档位（micro_pro/micro_lite），前端据此条件渲染 UI
                if (data.versionType) {
                    setVersionType(data.versionType)
                }
                await this.afterLogin(data.token, loginRole)
            } catch (e) {
                // 错误提示已由 request 封装统一处理
            } finally {
                this.loading = false
            }
        },
        // #endif
        /** 登录成功后置流程（账号密码 / 微信一键登录 / 手机号一键登录共用）：用户信息→能力开关→多组织→角色分流 */
        async afterLogin(token, loginRole) {
            setToken(token)
            // ★H5推广码归因：登录成功后调用归因接口，记录顾客是扫描了谁的二维码登录的
            await this.doPromoAttribution()
            // ★小微版优先使用登录响应传入的 roleCode；否则调 /getInfo 获取角色
            let roles = []
            if (loginRole) {
                roles = [loginRole]
            } else {
                try {
                    const infoRes = await get(config.api.userInfo)
                    const data = infoRes && infoRes.data ? infoRes.data : (infoRes || {})
                    setUser(data.user || {})
                    // ★账号密码/微信登录：优先用 /getInfo 返回的小微 roleCode（boss/salesman/customer），
                    //   确保 isMicroVersion() 正确判断并路由到对应角色首页
                    if (data.roleCode) {
                        roles = [data.roleCode]
                    } else {
                        roles = data.roles || []
                    }
                    // ★T15.36 从 /getInfo 响应提取 versionType（账号密码/微信登录时 phoneLogin 未设置）
                    if (data.versionType) {
                        setVersionType(data.versionType)
                    }
                } catch (e) { /* ignore */ }
            }
            setRoles(roles)
            // 平台管理员不使用小程序（权限文档 3.5/5.1）
            if (roles.includes('platform_admin')) {
                uni.showModal({ title: '提示', content: '平台管理员请使用 PC 端租户管理', showCancel: false })
                return
            }
            // 版本能力开关（防串第③层）：优先用 getInfo 下发，缺失时再调专用接口兜底
            if (loginRole) {
                // 小微版：调专用接口获取版本能力开关
                await loadEditionFeatures()
            } else {
                try {
                    const infoRes = await get(config.api.userInfo)
                    const infoData = infoRes && infoRes.data ? infoRes.data : (infoRes || {})
                    if (Array.isArray(infoData.features) && infoData.edition) {
                        setEditionFeatures(infoData.features, infoData.edition)
                    } else {
                        await loadEditionFeatures()
                    }
                } catch (e) { await loadEditionFeatures() }
            }
            // 专业版多组织用户登录后选择组织（5.3，FEAT_MULTI_ORG，T8.5；后端未就绪时静默跳过）
            if (hasFeature('FEAT_MULTI_ORG')) {
                await this.chooseOrg()
            }
            // applyRoleTabs 已移至 tab-home/index 的 onShow 中调用（登录页非 TabBar 页，setTabBarItem 会报错）
            if (isMicroVersion()) {
                const conf = getMicroTabConfig()
                const firstPage = conf.tabs.find(t => t.page) || {}
                uni.reLaunch({ url: firstPage.page || '/pages/tab-home/index' })
            } else {
                uni.switchTab({ url: '/pages/tab-home/index' })
            }
        },
        /** ★H5推广码归因：登录成功后调用，记录顾客是扫描了谁的二维码登录的（用于后续奖励发放） */
        async doPromoAttribution() {
            let refT = this.refTenantId
            let refU = this.refUserId
            // 兼容：从 data() 或 localStorage 读取
            if (!refT) { try { refT = uni.getStorageSync('promo_ref_t') } catch (e) {} }
            if (!refU) { try { refU = uni.getStorageSync('promo_ref_u') } catch (e) {} }
            if (!refT || !refU) return
            try {
                const profile = await get(config.api.microProfile)
                const customerUserId = (profile && profile.data && profile.data.userId) || null
                if (!customerUserId) return
                await post(config.api.microPromoScan, {
                    sceneParam: 't=' + refT + '&u=' + refU,
                    customerUserId: customerUserId,
                    customerPhone: (profile && profile.data && profile.data.phonenumber) || ''
                })
                // 归因成功后清除缓存，避免重复归因
                try { uni.removeStorageSync('promo_ref_t'); uni.removeStorageSync('promo_ref_u') } catch (e) {}
            } catch (e) {
                // 归因失败不阻塞登录流程
            }
        },
        /** 多组织选择：拉取本人可选组织，多个时弹窗选择（后端接口未就绪时优雅降级） */
        async chooseOrg() {
            try {
                const res = await get(config.api.orgMyList)
                const orgs = (res && res.data) || []
                if (orgs.length > 1) {
                    const names = orgs.map(o => o.orgName || o.name)
                    uni.showActionSheet({
                        itemList: names,
                        success: (r) => setOrg(orgs[r.tapIndex])
                    })
                } else if (orgs.length === 1) {
                    setOrg(orgs[0])
                }
            } catch (e) {
                // 组织接口未就绪/请求失败：不阻断登录（后端 @RequireFeature 为最终防线）
            }
        }
    }
}
</script>

<style scoped>
/* M27 T27.5：4 风格变体——页面结构/元素位置/中文文案完全一致，仅配色/圆角/图标风格/动效氛围不同 */
.login { padding: 80rpx 60rpx; min-height: 100vh; box-sizing: border-box; background: var(--tbg, #f5f6f8); }
.brand { font-size: 30rpx; color: var(--ttext2, #909399); text-align: center; letter-spacing: 4rpx; margin-bottom: 16rpx; }
.logo { font-size: 56rpx; font-weight: bold; text-align: center; margin-bottom: 40rpx; color: var(--ttext, #303133); }
.store-banner { display: flex; align-items: center; justify-content: space-between; background: var(--tp-soft, #ecf5ff); border-radius: var(--tradius, 12rpx); padding: 20rpx 24rpx; margin-bottom: 40rpx; }
.store-info { display: flex; flex-direction: column; }
.store-name { font-size: 28rpx; color: var(--ttext, #303133); font-weight: bold; }
.store-city { font-size: 22rpx; color: var(--ttext2, #909399); margin-top: 6rpx; }
.store-switch { font-size: 24rpx; color: var(--tp, #409eff); font-weight: bold; }
.input { background: #fff; border-radius: var(--tradius, 12rpx); padding: 24rpx; margin-bottom: 30rpx; color: var(--ttext, #303133); }
.btn { background: var(--tp, #409eff); color: #fff; border-radius: var(--tradius, 12rpx); margin-top: 20rpx; }
.btn-wx { background: #07c160; color: #fff; border-radius: var(--tradius, 12rpx); margin-top: 20rpx; }
.btn-phone { background: #ff6a00; color: #fff; border-radius: var(--tradius, 12rpx); margin-top: 20rpx; }

/* 活力渐变风：渐变背景 + 圆润图标 */
.login.theme-vivid { background: var(--tgrad); }
/* 温暖手绘风：暖色底 */
.login.theme-handdrawn { background: var(--tgrad); }
/* 暗黑科技风：深色底 + 霓虹边 + 文字反色 */
.login.theme-dark { background: var(--tgrad); }
.login.theme-dark .input { background: #16213E; color: #EAEAEA; border: 1rpx solid var(--tp, #00E5FF); }
.login.theme-dark .store-banner { background: rgba(0, 229, 255, 0.12); }
.login.theme-dark .btn { box-shadow: 0 0 16rpx var(--tp, #00E5FF); }

/* ★手绘主题装饰贴纸 */
.login { position: relative; }
.sticker { position: absolute; opacity: .8; pointer-events: none; z-index: 0; }
.brand, .logo, .store-banner, .form { position: relative; z-index: 1; }
.sticker-cloud-tl { top: 120rpx; left: 20rpx; width: 100rpx; height: 80rpx; transform: rotate(-8deg); }
.sticker-star-tr { top: 140rpx; right: 30rpx; width: 60rpx; height: 60rpx; transform: rotate(12deg); }
.sticker-sparkle-l { top: 200rpx; left: 40rpx; width: 44rpx; height: 44rpx; transform: rotate(-15deg); }
.sticker-heart-r { top: 190rpx; right: 20rpx; width: 64rpx; height: 64rpx; transform: rotate(10deg); }
.sticker-star-bl { bottom: 260rpx; left: 20rpx; width: 52rpx; height: 52rpx; transform: rotate(-12deg); }
.sticker-cloud-bl { bottom: 180rpx; left: 40rpx; width: 90rpx; height: 72rpx; transform: rotate(6deg); }
.sticker-sparkle-bl { bottom: 220rpx; left: 120rpx; width: 40rpx; height: 40rpx; transform: rotate(20deg); }
.sticker-bubble-br { bottom: 180rpx; right: 20rpx; width: 100rpx; height: 100rpx; transform: rotate(-5deg); }
.sticker-bubble-br-sm { bottom: 240rpx; right: 60rpx; width: 56rpx; height: 56rpx; transform: rotate(8deg); }
</style>
