<template>
    <view class="page" :style="pageStyle">
        <view class="brand">阿杰小账本</view>
        <view class="title">游客登录</view>
        <view class="subtitle">扫码参与活动，请先登录</view>

        <view class="card">
            <!-- ★登录方式切换：短信验证码 / 密码登录（短信通道不可用时备用） -->
            <view class="tab-bar">
                <view class="tab-item" :class="{ active: loginMode === 'sms' }" @click="loginMode = 'sms'">验证码登录</view>
                <view class="tab-item" :class="{ active: loginMode === 'password' }" @click="loginMode = 'password'">密码登录</view>
            </view>

            <view class="form">
                <input class="input" v-model="phone" type="number" maxlength="11" placeholder="请输入手机号" />

                <!-- 短信验证码模式 -->
                <template v-if="loginMode === 'sms'">
                    <view class="sms-row">
                        <input class="input sms-input" v-model="smsCode" type="number" maxlength="6" placeholder="短信验证码" />
                        <button class="sms-btn" :disabled="smsCooldown > 0" @click="sendSmsCode">{{ smsBtnText }}</button>
                    </view>
                    <button class="btn" :loading="loading" @click="handleLogin">登 录</button>
                </template>

                <!-- 密码登录模式 -->
                <template v-else>
                    <input class="input" v-model="password" type="password" placeholder="请输入密码" />
                    <button class="btn" :loading="loading" @click="handlePasswordLogin">登 录</button>
                    <view class="mode-tip">短信通道不可用时可使用密码登录</view>
                </template>
            </view>
        </view>

        <view class="tip">
            <text>登录即表示同意《用户服务协议》</text>
        </view>

        <!-- ★ 备案信息（ICP + 公安备案，取自 config.icp） -->
        <icp-footer />
    </view>
</template>

<script>
import config from '../../config'
import { get, post } from '../../utils/request'
import { setToken, setRoles, setVersionType } from '../../utils/auth'
import { setEditionFeatures, loadEditionFeatures } from '../../utils/features'
import themeMixin from '../../utils/themeMixin'
import IcpFooter from '../../components/icp-footer/icp-footer.vue'

export default {
    mixins: [themeMixin],
    components: { IcpFooter },
    data() {
        return {
            phone: '',
            smsCode: '',
            password: '',
            loading: false,
            smsCooldown: 0,
            smsTimer: null,
            // 登录模式：sms=短信验证码，password=密码登录
            loginMode: 'sms',
            // scene 参数解析结果
            sceneParam: null
        }
    },
    computed: {
        smsBtnText() {
            return this.smsCooldown > 0 ? this.smsCooldown + 's后重发' : '获取验证码'
        }
    },
    onLoad(options) {
        // 解析 scene 参数（微信小程序通过 options.scene 传入，H5 通过 URL query 传入）
        let scene = ''
        if (options && options.scene) {
            scene = decodeURIComponent(options.scene)
        } else if (options && options.t && options.a) {
            // H5 直传参数
            scene = 't' + options.t + 'a' + options.a + 'u' + (options.u || 'visitor') + 'p' + (options.p || '0000')
        }
        this.sceneParam = this.parseScene(scene)
        if (!this.sceneParam) {
            uni.showModal({ title: '提示', content: '二维码参数无效，请重新扫码', showCancel: false })
        }
    },
    onUnload() {
        if (this.smsTimer) clearInterval(this.smsTimer)
    },
    methods: {
        /** 解析 scene 参数（t/a/u/p 编码） */
        parseScene(scene) {
            if (!scene) return null
            try {
                const tIdx = scene.indexOf('t')
                const aIdx = scene.indexOf('a')
                const uIdx = scene.indexOf('u')
                const pIdx = scene.indexOf('p')
                if (tIdx < 0 || aIdx < 0 || uIdx < 0 || pIdx < 0) return null
                return {
                    tenantId: scene.substring(tIdx + 1, aIdx),
                    activityId: scene.substring(aIdx + 1, uIdx),
                    userType: scene.substring(uIdx + 1, pIdx),
                    phoneLast4: scene.substring(pIdx + 1)
                }
            } catch (e) {
                return null
            }
        },
        /** 发送短信验证码（后端限流：同一手机号每日最多 10 次） */
        async sendSmsCode() {
            if (!this.phone || this.phone.length !== 11) {
                uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
                return
            }
            if (this.smsCooldown > 0) return
            try {
                const res = await get(config.api.microVisitorSmsCode, { phone: this.phone })
                const data = (res && res.data) || {}
                if (data.devCode) {
                    // ★短信云通道未开启（未注册阿里云/腾讯云）：开发模式自动回填验证码
                    this.smsCode = String(data.devCode)
                    uni.showToast({ title: '短信未开通，验证码已填入', icon: 'none', duration: 3000 })
                } else {
                    uni.showToast({ title: '验证码已发送', icon: 'success' })
                }
                this.smsCooldown = 60
                this.smsTimer = setInterval(() => {
                    this.smsCooldown--
                    if (this.smsCooldown <= 0) {
                        clearInterval(this.smsTimer)
                        this.smsTimer = null
                    }
                }, 1000)
            } catch (e) {
                // 错误已由 request 统一处理
            }
        },
        /** 游客短信验证码登录 */
        async handleLogin() {
            if (!this.phone || this.phone.length !== 11) {
                uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
                return
            }
            if (!this.smsCode || this.smsCode.length !== 6) {
                uni.showToast({ title: '请输入6位短信验证码', icon: 'none' })
                return
            }
            if (!this.sceneParam) {
                uni.showToast({ title: '二维码参数无效', icon: 'none' })
                return
            }
            this.loading = true
            try {
                const scene = 't' + this.sceneParam.tenantId + 'a' + this.sceneParam.activityId + 'u' + this.sceneParam.userType + 'p' + this.sceneParam.phoneLast4
                const res = await post(config.api.microVisitorLogin, {
                    phone: this.phone,
                    smsCode: this.smsCode,
                    scene: scene
                })
                await this.afterLoginSuccess((res && res.data) || {})
            } catch (e) {
                // 错误已由 request 统一处理
            } finally {
                this.loading = false
            }
        },
        /** ★游客密码登录（备用方案：短信通道不可用时使用） */
        async handlePasswordLogin() {
            if (!this.phone || this.phone.length !== 11) {
                uni.showToast({ title: '请输入正确的手机号', icon: 'none' })
                return
            }
            if (!this.password) {
                uni.showToast({ title: '请输入密码', icon: 'none' })
                return
            }
            if (!this.sceneParam) {
                uni.showToast({ title: '二维码参数无效', icon: 'none' })
                return
            }
            this.loading = true
            try {
                const scene = 't' + this.sceneParam.tenantId + 'a' + this.sceneParam.activityId + 'u' + this.sceneParam.userType + 'p' + this.sceneParam.phoneLast4
                const res = await post(config.api.microVisitorPasswordLogin, {
                    phone: this.phone,
                    password: this.password,
                    scene: scene
                })
                await this.afterLoginSuccess((res && res.data) || {})
            } catch (e) {
                // 错误已由 request 统一处理
            } finally {
                this.loading = false
            }
        },
        /** 登录成功统一处理：存登录态 → 加载能力开关 → 跳转活动详情页 */
        async afterLoginSuccess(data) {
            if (!data || !data.token) {
                uni.showToast({ title: '登录失败，请重试', icon: 'none' })
                return
            }
            // 存储登录态
            setToken(data.token)
            setRoles(['customer'])
            setVersionType('micro_lite')
            // 加载版本能力开关
            await loadEditionFeatures()
            uni.showToast({ title: data.isNewAccount ? '欢迎新朋友！' : '登录成功', icon: 'success' })
            // ★跳转到活动详情页
            setTimeout(() => {
                uni.reLaunch({
                    url: '/pages/micro/activity/detail?id=' + this.sceneParam.activityId
                })
            }, 500)
        }
    }
}
</script>

<style scoped>
.page { padding: 80rpx 60rpx; min-height: 100vh; background: var(--page-bg, #f5f6f8); }
.brand { font-size: 30rpx; color: var(--text-sub, #909399); text-align: center; letter-spacing: 4rpx; margin-bottom: 16rpx; }
.title { font-size: 52rpx; font-weight: bold; text-align: center; margin-bottom: 16rpx; color: var(--text, #303133); }
.subtitle { font-size: 28rpx; color: var(--text-sub, #909399); text-align: center; margin-bottom: 60rpx; }
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 48rpx 36rpx; box-shadow: var(--shadow-card, none); }
/* ★登录方式切换 Tab */
.tab-bar { display: flex; justify-content: center; gap: 40rpx; margin-bottom: 36rpx; border-bottom: 1rpx solid #ebeef5; padding-bottom: 24rpx; }
.tab-item { font-size: 28rpx; color: var(--text-sub, #909399); padding-bottom: 12rpx; position: relative; cursor: pointer; }
.tab-item.active { color: var(--brand, #409eff); font-weight: bold; }
.tab-item.active::after { content: ''; position: absolute; bottom: -25rpx; left: 50%; transform: translateX(-50%); width: 48rpx; height: 4rpx; background: var(--brand, #409eff); border-radius: 2rpx; }
.form { display: flex; flex-direction: column; gap: 24rpx; }
.input { background: #f5f7fa; border-radius: 12rpx; padding: 24rpx; font-size: 28rpx; color: var(--text, #303133); }
.sms-row { display: flex; gap: 16rpx; align-items: center; }
.sms-input { flex: 1; }
.sms-btn { flex-shrink: 0; background: var(--brand, #409eff); color: #fff; border-radius: 12rpx; font-size: 24rpx; padding: 0 24rpx; height: 72rpx; line-height: 72rpx; border: none; }
.sms-btn[disabled] { background: #c0c4cc; }
.btn { background: var(--brand, #409eff); color: #fff; border-radius: 12rpx; font-size: 30rpx; margin-top: 16rpx; border: none; }
/* ★密码登录提示文案 */
.mode-tip { text-align: center; font-size: 24rpx; color: var(--text-sub, #909399); margin-top: -8rpx; }
.tip { text-align: center; margin-top: 40rpx; font-size: 24rpx; color: var(--text-sub, #909399); }
/* ★ ICP 备案信息由 components/icp-footer 统一渲染（样式随组件） */
</style>
