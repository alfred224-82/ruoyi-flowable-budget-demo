<template>
    <view class="page">
        <!-- 用户信息卡片 -->
        <view class="card user-card">
            <!-- ★头像：有照片展示照片，无照片降级为首字 -->
            <image v-if="profile.avatar" class="avatar-img" :src="avatarSize(profile.avatar, 'sm')" mode="aspectFill" @error="profile.avatar = ''" />
            <view v-else class="avatar">{{ (profile.nickName || '用').charAt(0) }}</view>
            <view class="user-info">
                <text class="name">{{ profile.nickName || '-' }}</text>
                <text class="role">{{ roleLabel }}</text>
            </view>
        </view>

        <!-- 基本信息编辑 -->
        <view class="card">
            <text class="card-title">基本信息</text>
            <view class="form-item">
                <text class="label">昵称</text>
                <input class="input" v-model="form.nickName" :maxlength="30" placeholder="请输入昵称" />
            </view>
            <view class="form-item">
                <text class="label">手机号</text>
                <input class="input" v-model="form.phonenumber" :maxlength="11" type="number" placeholder="请输入手机号" />
            </view>
            <view class="form-item">
                <text class="label">邮箱</text>
                <input class="input" v-model="form.email" :maxlength="50" placeholder="请输入邮箱" />
            </view>
            <button class="btn-save" @click="saveProfile">保存修改</button>
        </view>

        <!-- 修改密码 -->
        <view class="card">
            <text class="card-title">修改密码</text>
            <view class="form-item">
                <text class="label">旧密码</text>
                <input class="input" v-model="pwdForm.oldPassword" type="password" placeholder="请输入旧密码" />
            </view>
            <view class="form-item">
                <text class="label">新密码</text>
                <input class="input" v-model="pwdForm.newPassword" type="password" placeholder="4~20位新密码" />
            </view>
            <view class="form-item">
                <text class="label">确认密码</text>
                <input class="input" v-model="pwdForm.confirmPassword" type="password" placeholder="再次输入新密码" />
            </view>
            <button class="btn-pwd" @click="changePassword">修改密码</button>
        </view>

        <!-- 切换租户（T29.4：清除租户号 + 登录态 → 回租户号输入页重新登录）-->
        <button class="btn-switch-tenant" @click="switchTenantNo">切换租户</button>

        <!-- 退出登录 -->
        <button class="btn-logout" @click="logout">退出登录</button>
    </view>
</template>

<script>
import { getToken, getUser, clearAuth, clearAuthTo, getRoles } from '../../../utils/auth'
import { get, put } from '../../../utils/request'
import { toHttps, avatarSize } from '../../../utils/format'
import { clearTenantContext } from '../../../utils/roles'
import { clearTenantNo, clearSelectedStore, clearTenantStoreCandidates } from '../../../utils/store'
import config from '../../../config'

export default {
    data() {
        return {
            profile: {},
            form: { nickName: '', phonenumber: '', email: '' },
            pwdForm: { oldPassword: '', newPassword: '', confirmPassword: '' }
        }
    },
    computed: {
        roleLabel() {
            // ★优先后端下发的角色显示名（租户角色映射动态名，如司机/导游），未配置回退通用名
            if (this.profile && this.profile.roleDisplayName) return this.profile.roleDisplayName
            const roles = getRoles()
            if (!roles || !roles.length) return '用户'
            if (roles.includes('boss')) return '老板'
            if (roles.includes('employee') || roles.includes('salesman')) return '员工'
            if (roles.includes('customer')) return '顾客'
            return '用户'
        }
    },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        this.loadProfile()
    },
    methods: {
        toHttps,
        avatarSize,
        async loadProfile() {
            try {
                const res = await get(config.api.microProfile)
                const data = (res && res.data) || {}
                this.profile = data
                this.form.nickName = data.nickName || ''
                this.form.phonenumber = data.phonenumber || ''
                this.form.email = data.email || ''
            } catch (e) { /* ignore */ }
        },
        async saveProfile() {
            // ★前端校验（第七章：提交前校验）
            if (!this.form.nickName || !this.form.nickName.trim()) {
                uni.showToast({ title: '昵称不能为空', icon: 'none' }); return
            }
            if (this.form.nickName.length > 30) {
                uni.showToast({ title: '昵称不能超过30个字符', icon: 'none' }); return
            }
            if (this.form.phonenumber && !/^1[3-9]\d{9}$/.test(this.form.phonenumber)) {
                uni.showToast({ title: '手机号格式不正确', icon: 'none' }); return
            }
            if (this.form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.form.email)) {
                uni.showToast({ title: '邮箱格式不正确', icon: 'none' }); return
            }
            try {
                await put(config.api.microUpdateProfile, {
                    nickName: this.form.nickName.trim(),
                    phonenumber: this.form.phonenumber || '',
                    email: this.form.email || ''
                })
                uni.showToast({ title: '保存成功', icon: 'success' })
                // 更新本地缓存
                const localUser = getUser()
                localUser.nickName = this.form.nickName.trim()
                localUser.phonenumber = this.form.phonenumber || ''
                localUser.email = this.form.email || ''
                uni.setStorageSync('app_user', localUser)
            } catch (e) {
                uni.showToast({ title: '保存失败', icon: 'none' })
            }
        },
        async changePassword() {
            // ★前端校验
            if (!this.pwdForm.oldPassword) {
                uni.showToast({ title: '请输入旧密码', icon: 'none' }); return
            }
            if (!this.pwdForm.newPassword) {
                uni.showToast({ title: '请输入新密码', icon: 'none' }); return
            }
            if (this.pwdForm.newPassword.length < 4 || this.pwdForm.newPassword.length > 20) {
                uni.showToast({ title: '新密码长度需4~20位', icon: 'none' }); return
            }
            if (this.pwdForm.newPassword !== this.pwdForm.confirmPassword) {
                uni.showToast({ title: '两次密码输入不一致', icon: 'none' }); return
            }
            try {
                await put(config.api.microUpdatePwd, {
                    oldPassword: this.pwdForm.oldPassword,
                    newPassword: this.pwdForm.newPassword
                })
                uni.showToast({ title: '密码修改成功', icon: 'success' })
                this.pwdForm = { oldPassword: '', newPassword: '', confirmPassword: '' }
            } catch (e) {
                // 后端返回具体错误（旧密码错误/新密码不能与旧密码相同等）
                const msg = (e && e.message) || '修改密码失败'
                uni.showToast({ title: msg, icon: 'none' })
            }
        },
        logout() {
            uni.showModal({
                title: '提示',
                content: '确认退出登录？',
                success: (r) => { if (r.confirm) clearAuth() }
            })
        },
        /** 切换租户（T29.4）：二次确认 → 清除租户号/门店/门店候选/多租户上下文/登录态 → 回租户号输入页 */
        switchTenantNo() {
            uni.showModal({
                title: '切换租户',
                content: '切换租户需清除当前登录并重新输入租户号，是否继续？',
                success: (r) => {
                    if (!r.confirm) return
                    clearTenantNo()
                    clearSelectedStore()
                    // ★同时清门店候选：否则入口分流会把用户从租户号输入页弹回旧门店列表
                    clearTenantStoreCandidates()
                    clearTenantContext()
                    clearAuthTo('/pages/micro/tenant/input')
                }
            })
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; min-height: 100vh; background: #f5f6f8; }
.card { background: #fff; border-radius: 16rpx; padding: 28rpx; margin-bottom: 20rpx; }
.user-card { display: flex; align-items: center; }
.avatar-img { width: 96rpx; height: 96rpx; border-radius: 50%; }
.avatar { width: 96rpx; height: 96rpx; border-radius: 50%; background: #409eff; color: #fff; font-size: 40rpx; display: flex; align-items: center; justify-content: center; }
.user-info { margin-left: 24rpx; }
.name { font-size: 32rpx; font-weight: bold; display: block; }
.role { font-size: 24rpx; color: #409eff; margin-top: 8rpx; display: block; }
.card-title { font-size: 30rpx; font-weight: bold; display: block; margin-bottom: 20rpx; color: #303133; }
.form-item { display: flex; align-items: center; padding: 20rpx 0; border-bottom: 1rpx solid #f0f2f5; }
.form-item:last-of-type { border-bottom: none; }
.label { width: 160rpx; font-size: 28rpx; color: #606266; flex-shrink: 0; }
.input { flex: 1; font-size: 28rpx; color: #303133; }
.btn-save { background: #409eff; color: #fff; border-radius: 12rpx; font-size: 28rpx; margin-top: 24rpx; border: none; }
.btn-pwd { background: #e6a23c; color: #fff; border-radius: 12rpx; font-size: 28rpx; margin-top: 24rpx; border: none; }
.btn-switch-tenant { background: #fff; color: #409eff; border: 2rpx solid #409eff; border-radius: 12rpx; margin-top: 24rpx; font-size: 30rpx; }
.btn-logout { background: #fff; color: #f56c6c; border: 2rpx solid #f56c6c; border-radius: 12rpx; margin-top: 24rpx; font-size: 30rpx; }
</style>
