<template>
    <view class="page" :class="'theme-' + themeKey" :style="pageStyle">
        <!-- ★手绘主题装饰贴纸 -->
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-sparkle-tl" :src="iconUrl('hand-sparkle')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-star-l" :src="iconUrl('hand-star')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-heart-r" :src="iconUrl('hand-heart')" mode="aspectFit" />

        <scroll-view class="scroll" scroll-y>
            <!-- ═══ 宣传图（头图卡） ═══ -->
            <view class="hero-card">
                <swiper v-if="posters.length > 1" class="hero-swiper" @change="onSwiperChange">
                    <swiper-item v-for="(url, i) in posters" :key="i">
                        <image class="hero-img" :src="toHttps(url)" mode="aspectFill" @click="previewImage(i)" />
                    </swiper-item>
                </swiper>
                <image v-else-if="cover" class="hero-img" :src="toHttps(cover)" mode="aspectFill" @click="previewImage(0)" />
                <view v-else class="hero-img hero-img-default" :style="{ background: heroGradient }">
                    <text class="hero-default-title" :style="{ color: heroDefaultText }">{{ item.title || '活动' }}</text>
                </view>
                <view v-if="posters.length > 1" class="hero-pager">{{ posterIndex + 1 }}/{{ posters.length }}</view>
                <view class="hero-shade"></view>
                <view class="hero-info">
                    <text class="hero-title">{{ item.title || '-' }}</text>
                    <text class="hero-status">{{ statusText }}</text>
                </view>
            </view>

            <!-- ═══ 活动时间 / 房间 ═══ -->
            <view class="card">
                <text class="card-title">活动信息</text>
                <view class="info-row">
                    <text class="info-label">活动时间</text>
                    <text class="info-value">{{ timeText }}</text>
                </view>
                <view v-if="roomText" class="info-row">
                    <text class="info-label">指定房间</text>
                    <text class="info-value">{{ roomText }}</text>
                </view>
                <text v-if="sd.hasSeatLayout && (role === 'customer' || !role)" class="info-note" :style="{ color: theme.primaryColor }">本活动支持在线选座，请点击底部按钮进入座位图选座</text>
            </view>

            <!-- ═══ 本场主持人（姓名 + 不计入名额提示） ═══ -->
            <view v-if="(sd.hosts || []).length" class="card">
                <text class="card-title">本场主持人</text>
                <text class="card-sub">主持已安排，不计入顾客名额</text>
                <view class="host-row">
                    <view v-for="(h, i) in sd.hosts" :key="i" class="host-cell" @click="openStaffCard(h)">
                        <image v-if="h.avatar" class="host-avatar" :src="avatarSize(h.avatar, 'sm')" mode="aspectFill" @error="h.avatar = ''" />
                        <view v-else class="host-avatar host-avatar-none">
                            <text class="avatar-char">{{ (h.name || '?').charAt(0) }}</text>
                        </view>
                        <text class="host-name">{{ h.name || '-' }}</text>
                        <text class="host-card-hint">点头像看名片</text>
                    </view>
                </view>
            </view>

            <!-- ═══ 费用（¥/人 + 共 N 人） ═══ -->
            <view v-if="sd.perPersonPrice != null" class="card fee-card">
                <view class="fee-row">
                    <text class="fee-label">费用</text>
                    <text class="fee-value">¥{{ sd.perPersonPrice }}/人</text>
                </view>
                <view class="fee-row">
                    <text class="fee-label">参与人数</text>
                    <text class="fee-value">共 {{ sd.participantCount || sd.signedCount || '-' }} 人</text>
                </view>
                <text v-if="sd.isLocked" class="fee-locked">活动已锁定</text>
            </view>

            <!-- ═══ 活动介绍 ═══ -->
            <view class="card">
                <text class="card-title">活动介绍</text>
                <text class="intro-content">{{ item.content || '暂无活动说明' }}</text>
                <text v-if="item.remark" class="intro-remark">{{ item.remark }}</text>
            </view>
        </scroll-view>

        <!-- ═══ 底部「查看座位」按钮（显示已选座位） ═══ -->
        <view v-if="role === 'customer' || !role" class="footer">
            <button class="submit-btn" :class="{ 'is-disabled': submitState.disabled }"
                    :style="{ background: submitState.disabled ? '#c0c4cc' : theme.primaryColor }"
                    @click="onFooterClick">{{ submitState.text }}</button>
            <text class="footer-hint">报名后请按时参加活动</text>
        </view>

        <!-- ★M34 T34.6 顾客点头像弹名片 -->
        <staff-card-popup ref="staffCard" />
    </view>
</template>

<script>
import { get, post } from '../../../utils/request'
import config from '../../../config'
import { themeGradient, themeGradientText, getThemeSync } from '../../../utils/theme'
import { toHttps, splitImages, firstImage, isImageUrl, avatarSize, isActivityEnded } from '../../../utils/format'
import { getToken } from '../../../utils/auth'
import { getPrimaryRole } from '../../../utils/roles'
import { iconUrl } from '../../../utils/themeIcons'
import themeMixin from '../../../utils/themeMixin'
import StaffCardPopup from '../../../components/staff-card-popup/staff-card-popup.vue'

export default {
    components: { StaffCardPopup },
    mixins: [themeMixin],
    data() {
        return {
            theme: getThemeSync(),
            item: {},
            sd: {},
            role: '',
            logged: false,
            submitting: false,
            posterIndex: 0
        }
    },
    computed: {
        cover() {
            return [this.item.bgImageUrl, this.item.posterUrl, firstImage(this.item.coverUrl), this.sd.roomBgImageUrl]
                .find(isImageUrl) || ''
        },
        posters() {
            const multi = splitImages(this.item.coverUrl)
            return multi.length > 1 ? multi : (this.cover ? [this.cover] : [])
        },
        heroGradient() {
            return themeGradient(this.item.themeKey || this.theme.key)
        },
        heroDefaultText() {
            return themeGradientText(this.item.themeKey || this.theme.key).text
        },
        ended() {
            return isActivityEnded(this.item)
        },
        statusText() {
            if (this.ended) return '已结束'
            if (this.sd.isLocked) return '活动已锁定'
            return '报名进行中'
        },
        timeText() {
            if (!this.item.startTime && !this.item.endTime) return '-'
            const s = this.item.startTime ? this.item.startTime.replace('T', ' ').substring(0, 16) : ''
            const e = this.item.endTime ? this.item.endTime.replace('T', ' ').substring(0, 16) : ''
            return s && e ? `${s} ~ ${e}` : (s || e || '-')
        },
        roomText() {
            return (this.sd.roomNames || []).join('、')
        },
        /** ★T32.2 底部按钮状态：未登录可点击（跳手机验证码登录）；选座活动 → 查看座位；非选座 → 确认报名 */
        submitState() {
            if (!this.logged) return { disabled: false, text: '请先登录', needLogin: true }
            if (this.ended) return { disabled: true, text: '活动已结束' }
            if (this.sd.isLocked) return { disabled: true, text: '活动已锁定' }
            if (this.submitting) return { disabled: true, text: '报名中...' }
            const isMember = (this.sd.members || []).some(m => m.userId === this.currentUserId)
            // ★选座活动：显示已选座位
            if (this.sd.hasSeatLayout) {
                if (this.sd.mySeatNo) {
                    return { disabled: false, text: '已选座 ' + this.sd.mySeatNo + ' · 查看座位', toSeat: true }
                }
                return { disabled: false, text: '查看座位', toSeat: true }
            }
            if (isMember) return { disabled: true, text: '已报名' }
            if (!this.sd.projectId) return { disabled: true, text: '未开放线上报名' }
            const max = this.sd.maxParticipants || 0
            const count = (this.sd.members || []).filter(m => !m.isWaitlist).length
            if (max && count >= max) return { disabled: false, text: '加入候补' }
            return { disabled: false, text: '确认报名' }
        },
        currentUserId() {
            try {
                const info = uni.getStorageSync('app_user_info') || {}
                return info.userId || ''
            } catch (e) { return '' }
        }
    },
    onLoad(options) {
        this.logged = !!getToken()
        this.role = getPrimaryRole() || ''
        this.item = this.parseItemParam(options && options.id)
        this.loadSignupDetail()
    },
    onShow() {
        this.onSeatChanged = () => { this.loadSignupDetail() }
        uni.$on('seatAssignmentChanged', this.onSeatChanged)
    },
    beforeUnmount() {
        if (this.onSeatChanged) {
            uni.$off('seatAssignmentChanged', this.onSeatChanged)
            this.onSeatChanged = null
        }
    },
    methods: {
        toHttps,
        avatarSize,
        iconUrl,
        /** ★M34 T34.6 点击主持人头像 → 未登录仅提示不点开，登录后弹名片 */
        openStaffCard(h) {
            if (!h) return
            if (!this.logged) {
                // ★未登录：不弹名片也不跳登录页（登录入口由底部「请先登录」按钮提供）
                uni.showToast({ title: '请先登录后再查看主持人名片', icon: 'none' })
                return
            }
            this.$refs.staffCard.open({
                staffId: h.staffId || h.userId || h.id,
                name: h.name || h.nickName,
                avatar: h.avatar,
                role: h.role || 'salesman',
                intro: h.intro || h.remark || '',
                phone: h.phone || h.phoneNumber || ''
            })
        },
        /** ★未登录时跳转手机验证码登录页（携带活动ID，登录后返回本页） */
        goPhoneLogin() {
            const tid = (this.sd.activity && this.sd.activity.tenantId) || (this.item && this.item.tenantId) || ''
            uni.navigateTo({
                url: '/pages/login/phoneLogin?activityId=' + (this.item.id || '') + '&tenantId=' + tid
            })
        },
        parseItemParam(raw) {
            if (!raw) return {}
            const candidates = [raw]
            try { candidates.push(decodeURIComponent(raw)) } catch (e) { /* ignore */ }
            for (const cand of candidates) {
                try {
                    const parsed = JSON.parse(cand)
                    if (parsed && typeof parsed === 'object') {
                        if (!parsed.id && parsed.bizId) parsed.id = parsed.bizId
                        return parsed
                    }
                } catch (e) { /* ignore */ }
            }
            return /^\d+$/.test(raw) ? { id: raw } : {}
        },
        async loadSignupDetail() {
            if (!this.item.id) return
            try {
                const res = await get(config.api.microActivitySignupDetail, { activityId: this.item.id })
                this.sd = (res && res.data) || {}
                if (this.sd.activity && typeof this.sd.activity === 'object') {
                    this.item = Object.assign({}, this.item, this.sd.activity)
                }
            } catch (e) { this.sd = {} }
        },
        onSwiperChange(e) {
            this.posterIndex = e.detail.current
        },
        previewImage(index) {
            if (!this.posters.length) return
            uni.previewImage({
                urls: this.posters.map(u => toHttps(u)),
                current: index
            })
        },
        /** ★底部按钮点击：未登录 → 手机验证码登录；选座 → 跳 M26 选座页；非选座 → 确认报名 */
        async onFooterClick() {
            const st = this.submitState
            if (st.disabled) return
            // ★未登录 → 手机验证码登录页（按钮可点击）
            if (st.needLogin) {
                this.goPhoneLogin()
                return
            }
            if (st.toSeat) {
                uni.navigateTo({ url: '/pages/micro/customerBook/seat?activityId=' + this.item.id })
                return
            }
            this.submitting = true
            try {
                await post(config.api.microActivitySignup + this.item.id, {})
                uni.showToast({ title: '报名成功', icon: 'success' })
                this.loadSignupDetail()
            } catch (e) {
                uni.showToast({ title: e.message || '报名失败', icon: 'none' })
            } finally {
                this.submitting = false
            }
        }
    }
}
</script>

<style scoped>
.page { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background-color: var(--page-bg, #f5f6f8); display: flex; flex-direction: column; }
.scroll { flex: 1; min-height: 0; }

/* 手绘贴纸 */
.sticker { position: absolute; opacity: .75; pointer-events: none; z-index: 0; }
.hero-card, .card { position: relative; z-index: 1; }
.sticker-sparkle-tl { top: 16rpx; left: 16rpx; width: 40rpx; height: 40rpx; transform: rotate(-15deg); }
.sticker-star-l { top: 80rpx; left: 10rpx; width: 48rpx; height: 48rpx; transform: rotate(-10deg); }
.sticker-heart-r { top: 60rpx; right: 16rpx; width: 44rpx; height: 44rpx; transform: rotate(12deg); }

/* 头图卡 */
.hero-card { position: relative; margin: 0 24rpx 24rpx; height: 420rpx; border-radius: var(--radius-card, 24rpx); overflow: hidden; }
.hero-img { width: 100%; height: 420rpx; display: block; }
.hero-img-default { display: flex; align-items: center; justify-content: center; }
.hero-default-title { font-size: 40rpx; font-weight: bold; text-align: center; padding: 0 48rpx; }
.hero-shade { position: absolute; left: 0; right: 0; bottom: 0; height: 60%; background: linear-gradient(to top, rgba(0, 0, 0, 0.65) 0%, transparent 100%); }
.hero-swiper { width: 100%; height: 420rpx; }
.hero-pager { position: absolute; right: 20rpx; top: 20rpx; z-index: 3; background: rgba(0, 0, 0, 0.35); color: #fff; font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 20rpx; }
.hero-info { position: absolute; left: 32rpx; right: 32rpx; bottom: 24rpx; }
.hero-title { display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden; font-size: 40rpx; font-weight: bold; color: #fff; line-height: 1.35; }
.hero-status { display: block; font-size: 26rpx; color: rgba(255, 255, 255, 0.85); margin-top: 12rpx; }

/* 通用卡片 */
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 24rpx); padding: 32rpx; margin: 0 24rpx 24rpx; box-shadow: var(--shadow-card, none); }
.card-title { display: block; font-size: 34rpx; font-weight: bold; color: var(--text, #1a1a1a); margin-bottom: 24rpx; }
.card-sub { display: block; font-size: 26rpx; color: var(--text-sub, #909399); margin-bottom: 28rpx; }

/* 活动信息 */
.info-row { display: flex; align-items: flex-start; margin-bottom: 20rpx; }
.info-label { width: 150rpx; flex-shrink: 0; font-size: 28rpx; color: var(--text-sub, #909399); }
.info-value { flex: 1; font-size: 30rpx; font-weight: 600; color: var(--text, #1a1a1a); }
.info-note { display: block; font-size: 28rpx; margin-top: 4rpx; }

/* 本场主持人 */
.host-row { display: flex; justify-content: center; gap: 96rpx; flex-wrap: wrap; }
.host-cell { display: flex; flex-direction: column; align-items: center; }
.host-avatar { width: 128rpx; height: 128rpx; border-radius: 50%; background: var(--brand-weak, #f0f2f5); }
.host-avatar-none { display: flex; align-items: center; justify-content: center; }
.avatar-char { font-size: 44rpx; color: var(--text-sub, #909399); }
.host-name { font-size: 30rpx; font-weight: bold; color: var(--text, #1a1a1a); margin-top: 16rpx; }
.host-card-hint { font-size: 22rpx; color: var(--text-sub, #c0c4cc); margin-top: 6rpx; }

/* 费用卡 */
.fee-card { background: var(--card-bg, #fff); }
.fee-row { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16rpx; }
.fee-row:last-child { margin-bottom: 0; }
.fee-label { font-size: 28rpx; color: var(--text-sub, #606266); }
.fee-value { font-size: 32rpx; font-weight: bold; color: var(--brand, #e6a23c); }
.fee-locked { display: block; text-align: center; font-size: 24rpx; color: var(--danger, #f56c6c); background: #fef0f0; padding: 8rpx 16rpx; border-radius: 8rpx; margin-top: 16rpx; }

/* 活动介绍 */
.intro-content { display: block; font-size: 28rpx; color: var(--text-sub, #606266); line-height: 1.8; white-space: pre-wrap; word-break: break-all; }
.intro-remark { display: block; font-size: 26rpx; color: var(--brand, #e6a23c); margin-top: 16rpx; }

/* 底部按钮 */
.footer { flex-shrink: 0; background: var(--card-bg, #fff); padding: 16rpx 32rpx calc(env(safe-area-inset-bottom) + 16rpx); box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.06); }
.submit-btn { color: #fff; border-radius: 48rpx; height: 92rpx; line-height: 92rpx; font-size: 32rpx; font-weight: 600; }
.submit-btn::after { border: none; }
.submit-btn.is-disabled { color: #fff; }
.footer-hint { display: block; text-align: center; font-size: 24rpx; color: var(--text-sub, #909399); margin-top: 12rpx; }
</style>
