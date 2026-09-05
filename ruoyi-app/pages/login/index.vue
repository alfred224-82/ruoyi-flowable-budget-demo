<template>
    <view class="login">
        <view class="logo">资金管控</view>
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
            <button class="btn-wx" :loading="loading" @click="handleWxLogin">微信一键登录</button>
            <!-- #endif -->
        </view>
    </view>
</template>

<script>
import config from '../../config'
import { post, get } from '../../utils/request'
import { setToken, setRoles, setUser, setOrg } from '../../utils/auth'
import { applyRoleTabs } from '../../utils/roles'
import { setEditionFeatures, loadEditionFeatures, hasFeature } from '../../utils/features'
import { getSelectedStore } from '../../utils/store'

export default {
    data() {
        return {
            loading: false,
            store: null,
            form: { username: '', password: '', code: '', uuid: '' }
        }
    },
    onShow() {
        // 引导态：未显式选定门店时先回到门店选择页（双阶段租户识别，绝不自动猜测租户）
        this.store = getSelectedStore()
        if (!this.store) {
            uni.reLaunch({ url: '/pages/store/select' })
        }
    },
    methods: {
        /** 切换门店：回到引导态门店选择页重新选定 */
        switchStore() {
            uni.reLaunch({ url: '/pages/store/select' })
        },
        async handleLogin() {
            if (!this.form.username || !this.form.password) {
                uni.showToast({ title: '请输入账号和密码', icon: 'none' })
                return
            }
            this.loading = true
            try {
                // 登录拿 token（携带选定门店 tenantId，后端校验账号归属防跨门店串号 T13.1；租户到期/停用会被后端拦截 T3.4）
                const loginBody = { ...this.form, tenantId: this.store ? this.store.tenantId : null }
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
                uni.reLaunch({ url: '/pages/store/select' })
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
        /** 登录成功后置流程（账号密码 / 微信一键登录共用）：用户信息→能力开关→多组织→角色分流 */
        async afterLogin(token) {
            setToken(token)
            // 获取用户信息：/getInfo 直接返回 roles(角色标识集合)/features/edition
            const infoRes = await get(config.api.userInfo)
            const data = infoRes && infoRes.data ? infoRes.data : (infoRes || {})
            setUser(data.user || {})
            setRoles(data.roles || [])
            // 平台管理员不使用小程序（权限文档 3.5/5.1）
            if ((data.roles || []).includes('platform_admin')) {
                uni.showModal({ title: '提示', content: '平台管理员请使用 PC 端租户管理', showCancel: false })
                return
            }
            // 版本能力开关（防串第③层）：优先用 getInfo 下发，缺失时再调专用接口兜底
            if (Array.isArray(data.features) && data.edition) {
                setEditionFeatures(data.features, data.edition)
            } else {
                await loadEditionFeatures()
            }
            // 专业版多组织用户登录后选择组织（5.3，FEAT_MULTI_ORG，T8.5；后端未就绪时静默跳过）
            if (hasFeature('FEAT_MULTI_ORG')) {
                await this.chooseOrg()
            }
            // 应用角色 Tab 分流（5.1）
            applyRoleTabs()
            uni.switchTab({ url: '/pages/tab-home/index' })
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
.login { padding: 80rpx 60rpx; }
.logo { font-size: 56rpx; font-weight: bold; text-align: center; margin-bottom: 40rpx; }
.store-banner { display: flex; align-items: center; justify-content: space-between; background: #ecf5ff; border-radius: 12rpx; padding: 20rpx 24rpx; margin-bottom: 40rpx; }
.store-info { display: flex; flex-direction: column; }
.store-name { font-size: 28rpx; color: #303133; font-weight: bold; }
.store-city { font-size: 22rpx; color: #909399; margin-top: 6rpx; }
.store-switch { font-size: 24rpx; color: #409eff; font-weight: bold; }
.input { background: #fff; border-radius: 12rpx; padding: 24rpx; margin-bottom: 30rpx; }
.btn { background: #409eff; color: #fff; border-radius: 12rpx; margin-top: 20rpx; }
.btn-wx { background: #07c160; color: #fff; border-radius: 12rpx; margin-top: 20rpx; }
</style>
