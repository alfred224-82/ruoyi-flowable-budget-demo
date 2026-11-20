<template>
    <view class="page" :class="'theme-' + themeKey" :style="pageStyle">
        <!-- ★手绘主题装饰贴纸（仅 theme_handdrawn 显示） -->
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-sparkle-tl" :src="iconUrl('hand-sparkle')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-star-l" :src="iconUrl('hand-star')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-heart-r" :src="iconUrl('hand-heart')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-sparkle-info" :src="iconUrl('hand-sparkle')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-bubble-info" :src="iconUrl('hand-bubble')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-cloud-info" :src="iconUrl('hand-cloud')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-star-host" :src="iconUrl('hand-star')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-heart-host" :src="iconUrl('hand-heart')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-bubble-host" :src="iconUrl('hand-bubble')" mode="aspectFit" />
        <image v-if="themeKey === 'theme_handdrawn'" class="sticker sticker-cloud-host" :src="iconUrl('hand-cloud')" mode="aspectFit" />

        <scroll-view class="scroll" scroll-y>
            <!-- 头图卡 -->
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

            <!-- 活动信息 -->
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
                <text v-if="(sd.hosts || []).length || roomText" class="info-note" :style="{ color: theme.primaryColor }">主持人与房间已由店长安排</text>
                <text v-if="sd.hasSeatLayout && role !== 'boss' && role !== 'salesman'" class="info-note" :style="{ color: theme.primaryColor }">本活动支持在线选座，请点击底部按钮进入座位图选座</text>
            </view>

            <!-- 本场主持人 -->
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
                        <!-- ★顾客点赞主持人：红心 toggle（红=已点赞 / 白=取消点赞），仅登录顾客且 FEAT_RATING 开启显示 -->
                        <view v-if="canLikeHost(h)" class="host-like" :class="{ 'is-liked': h.liked }" @click.stop="toggleHostLike(h)">
                            <text class="host-like-icon">♥</text>
                            <text class="host-like-count">{{ h.likeCount || 0 }}</text>
                        </view>
                    </view>
                </view>
            </view>

            <!-- 已报名顾客 -->
            <view v-if="sd.projectId" class="card">
                <view class="roster-header">
                    <text class="card-title no-mb">已报名顾客</text>
                    <text class="roster-count">{{ countText }}</text>
                </view>
                <text class="card-sub">{{ minMaxText }}</text>
                <view v-if="(sd.members || []).length || emptySeatCount" class="roster-row">
                    <view v-for="(m, i) in sd.members" :key="i" class="roster-cell">
                        <image v-if="m.avatar" class="roster-avatar" :class="{ 'is-wait': m.isWaitlist }" :src="avatarSize(m.avatar, 'sm')" mode="aspectFill" @error="m.avatar = ''" />
                        <view v-else class="roster-avatar roster-avatar-none" :class="{ 'is-wait': m.isWaitlist }">
                            <text class="avatar-char">{{ (m.name || '?').charAt(0) }}</text>
                        </view>
                        <text class="roster-name" :class="{ 'is-wait': m.isWaitlist }">{{ m.isWaitlist ? m.name + '·候补' : m.name }}</text>
                    </view>
                    <!-- ★boss 视角：空位占位（与原报名详情弹窗口径一致） -->
                    <view v-for="n in emptySeatCount" :key="'empty-' + n" class="roster-cell">
                        <view class="roster-avatar roster-avatar-none is-empty"></view>
                        <text class="roster-name">空位</text>
                    </view>
                </view>
                <view v-else class="roster-empty">暂无顾客报名，快来抢占名额</view>
                <text v-if="gapText" class="roster-gap" :style="{ color: theme.primaryColor }">{{ gapText }}</text>
            </view>

            <!-- 活动介绍 -->
            <view class="card">
                <text class="card-title">活动介绍</text>
                <view v-if="item.voiceUrl" class="voice-player">
                    <text class="voice-play-icon" @click="playVoice">{{ isPlaying ? '⏸' : '▶️' }}</text>
                    <view class="voice-progress">
                        <view class="voice-bar" :style="{ width: voiceProgress + '%' }"></view>
                    </view>
                    <text class="voice-duration">{{ formatDuration(voiceDuration) }}</text>
                </view>
                <text class="intro-content">{{ item.content || '暂无活动说明' }}</text>
                <text v-if="item.remark" class="intro-remark">{{ item.remark }}</text>
                <view v-if="voiceEnabled" class="voice-notice">
                    <text class="voice-icon">️</text>
                    <text class="voice-text">本店服务过程可能进行录音/录像，参与活动即视为知悉并同意</text>
                    <text class="voice-link" @click.stop="goConsent">查看详情</text>
                </view>
            </view>
        </scroll-view>

        <!-- 费用信息 -->
        <view v-if="sd.perPersonPrice != null && role !== 'salesman'" class="fee-bar">
            <text class="fee-text">费用：¥{{ sd.perPersonPrice }}/人（共{{ sd.participantCount || '-' }}人）</text>
            <text v-if="sd.isLocked" class="fee-locked">已锁定</text>
        </view>

        <!-- 底部确认报名（★boss 为管理视角不展示报名按钮） -->
        <view v-if="role !== 'salesman' && role !== 'boss'" class="footer">
            <button class="submit-btn" :class="{ 'is-disabled': submitState.disabled }"
                    :style="{ background: submitState.disabled ? '#c0c4cc' : theme.primaryColor }"
                    @click="confirmSignup">{{ submitState.text }}</button>
            <text class="footer-hint">报名后请按时参加活动</text>
        </view>

        <!-- ★M34 T34.6 顾客点头像弹名片 -->
        <staff-card-popup ref="staffCard" />
    </view>
</template>

<script>
import { get, post } from '../../../utils/request'
import config from '../../../config'
import { themeGradient, themeGradientText, getThemeSync, loadTenantTheme } from '../../../utils/theme'
import { toHttps, splitImages, firstImage, isImageUrl, avatarSize, isActivityEnded } from '../../../utils/format'
import { getToken } from '../../../utils/auth'
import { getPrimaryRole } from '../../../utils/roles'
import { hasFeature } from '../../../utils/features'
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
            // ★FEAT_RATING 版本开关：顾客点赞主持人入口显隐（后端 @RequireFeature 仍为最终防线）
            featRating: false,
            submitting: false,
            voiceEnabled: true,
            innerAudioContext: null,
            isPlaying: false,
            voiceDuration: 0,
            voiceProgress: 0,
            posterIndex: 0,
            statusBarHeight: 0
        }
    },
    computed: {
        cover() {
            // ★防线：过滤未翻译的纯数字 ossId，避免 image src 污染 404 空白
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
            // ★已结束统一口径：status=ENDED / endTime 已过 / endTime 缺失时 startTime 已过
            return isActivityEnded(this.item)
        },
        statusText() {
            if (this.ended) return '已结束'
            // ★锁定活动对客只读：徽标与报名按钮同口径展示「活动已锁定」
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
        countText() {
            const count = this.sd.signedCount || 0
            const wait = this.sd.waitlistCount || 0
            let text = `${count}人已报名`
            if (wait > 0) text += `，${wait}人候补`
            return text
        },
        minMaxText() {
            const quota = this.sd.quota || 0
            return quota ? `名额${quota}人` : ''
        },
        gapText() {
            const remaining = this.sd.remaining || 0
            return remaining > 0 ? `还差${remaining}人成团` : ''
        },
        /** ★boss 视角空位占位数（与原报名详情弹窗口径一致：剩余名额最多展示 10 个） */
        emptySeatCount() {
            if (this.role !== 'boss') return 0
            return Math.min(this.sd.remaining || 0, 10)
        },
        submitState() {
            if (!this.logged) return { disabled: false, text: '请先登录', needLogin: true }
            if (this.ended) return { disabled: true, text: '活动已结束' }
            if (this.sd.isLocked) return { disabled: true, text: '活动已锁定' }
            if (this.submitting) return { disabled: true, text: '报名中...' }
            const isMember = (this.sd.members || []).some(m => m.userId === this.currentUserId)
            if (isMember) return { disabled: true, text: '已报名' }
            // ★座位布局活动 → 选座报名
            if (this.sd.hasSeatLayout) {
                return this.sd.mySeatNo
                    ? { disabled: false, text: '已选座 ' + this.sd.mySeatNo + ' · 查看座位', toSeat: true }
                    : { disabled: false, text: '选座报名', toSeat: true }
            }
            // ★无项目且无座位布局 → 不可报名
            if (!this.sd.projectId) return { disabled: true, text: '未开放线上报名' }
            const max = this.sd.maxParticipants || 0
            const count = (this.sd.members || []).filter(m => !m.isWaitlist).length
            if (max && count >= max) return { disabled: false, text: '加入候补' }
            return { disabled: false, text: '确认报名' }
        },
        currentUserId() {
            return this.$store?.state?.user?.id || ''
        }
    },
    onLoad(options) {
        try {
            this.statusBarHeight = uni.getSystemInfoSync().statusBarHeight || 0
        } catch (e) {}
        this.logged = !!getToken()
        this.role = getPrimaryRole() || ''
        this.featRating = hasFeature('FEAT_RATING')
        // ★支持两种传参：① id=xxx（列表页跳转）② scene=t{tenantId}a{activityId}...（扫码进入）
        this.item = this.parseItemParam(options && (options.id || this.parseScene(options.scene)))
        this.loadSignupDetail()
    },
    onShow() {
        this.onSeatChanged = () => { this.loadSignupDetail() }
        uni.$on('seatAssignmentChanged', this.onSeatChanged)
    },
    beforeUnmount() {
        if (this.innerAudioContext) {
            this.innerAudioContext.destroy()
            this.innerAudioContext = null
        }
        if (this.onSeatChanged) {
            uni.$off('seatAssignmentChanged', this.onSeatChanged)
            this.onSeatChanged = null
        }
    },
    methods: {
        toHttps,
        avatarSize,
        iconUrl,
        /** 格式化语音时长 */
        formatDuration(seconds) {
            if (!seconds) return '0:00'
            const m = Math.floor(seconds / 60)
            const s = Math.floor(seconds % 60)
            return m + ':' + String(s).padStart(2, '0')
        },
        /** ★统一详情入口参数解析：兼容完整活动 JSON（各列表页）、仅 bizId 的薄数据 JSON（myActivities）、纯 id 三种传参 */
        parseItemParam(raw) {
            if (!raw) return {}
            const candidates = [raw]
            try { candidates.push(decodeURIComponent(raw)) } catch (e) { /* 解码失败保留原值继续尝试 */ }
            for (const cand of candidates) {
                try {
                    const parsed = JSON.parse(cand)
                    if (parsed && typeof parsed === 'object') {
                        // ★薄数据兼容：myActivities 的 MyActivityVo 仅 bizId，统一补 id 保证页面按 id 自取
                        if (!parsed.id && parsed.bizId) parsed.id = parsed.bizId
                        return parsed
                    }
                } catch (e) { /* 非 JSON，尝试下一候选 */ }
            }
            return /^\d+$/.test(raw) ? { id: raw } : {}
        },
        /** ★解析扫码 scene 参数（t{tenantId}a{activityId}u{userType}p{phoneLast4}），返回 activityId */
        parseScene(scene) {
            if (!scene) return null
            try {
                const decoded = decodeURIComponent(scene)
                const aIdx = decoded.indexOf('a')
                const uIdx = decoded.indexOf('u')
                if (aIdx < 0 || uIdx < 0) return null
                return decoded.substring(aIdx + 1, uIdx)
            } catch (e) {
                return null
            }
        },
        async loadSignupDetail() {
            if (!this.item.id) return
            try {
                const res = await get(config.api.microActivitySignupDetail, { activityId: this.item.id })
                this.sd = (res && res.data) || {}
                // ★服务端活动基础信息为权威：统一修正列表页透传的薄数据（myActivities）与过期快照，
                //   保证所有入口（我的活动/浏览/首页/历史/boss 待办）渲染同一份完整详情
                if (this.sd.activity && typeof this.sd.activity === 'object') {
                    this.item = Object.assign({}, this.item, this.sd.activity)
                }
                // ★主持人点赞状态回填（红心红/白 + 总点赞数）
                this.loadHostLikes()
            } catch (e) { this.sd = {} }
        },
        /** ★点赞按钮显隐：登录顾客 + FEAT_RATING 开启 + 非本人（自赞由后端拒绝） */
        canLikeHost(h) {
            return this.logged && this.role === 'customer' && this.featRating
                && !!h.userId && String(h.userId) !== String(this.currentUserId || '')
        },
        /** ★加载主持人点赞状态（逐个主持人查询，失败降级未赞不阻断详情渲染） */
        async loadHostLikes() {
            const hosts = this.sd.hosts || []
            for (const h of hosts) {
                if (!this.canLikeHost(h)) continue
                try {
                    const res = await get(config.api.microActivityHostLikeStatus, { hostUserId: h.userId })
                    const d = (res && res.data) || {}
                    this.$set(h, 'liked', !!d.liked)
                    this.$set(h, 'likeCount', d.likeCount || 0)
                } catch (e) {
                    this.$set(h, 'liked', false)
                    this.$set(h, 'likeCount', 0)
                }
            }
        },
        /** ★点赞 toggle：成功红心变红（已赞），再点变白（取消）；每人仅一次由 toggle 天然防重 */
        async toggleHostLike(h) {
            if (!this.logged) {
                this.goPhoneLogin()
                return
            }
            if (h.likeLoading) return
            h.likeLoading = true
            try {
                const res = await post(config.api.microActivityHostLikeToggle + '?hostUserId=' + h.userId, {})
                const liked = !!(res && res.data)
                this.$set(h, 'liked', liked)
                this.$set(h, 'likeCount', Math.max((h.likeCount || 0) + (liked ? 1 : -1), 0))
                uni.showToast({ title: liked ? '点赞成功' : '已取消点赞', icon: 'none' })
            } catch (e) {
                /* 请求层已统一提示 */
            } finally {
                h.likeLoading = false
            }
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
        playVoice() {
            if (!this.item.voiceUrl) return
            if (!this.innerAudioContext) {
                this.innerAudioContext = uni.createInnerAudioContext()
                this.innerAudioContext.src = toHttps(this.item.voiceUrl)
                this.innerAudioContext.onTimeUpdate(() => {
                    const dur = this.innerAudioContext.duration || 0
                    const cur = this.innerAudioContext.currentTime || 0
                    this.voiceDuration = dur
                    this.voiceProgress = dur > 0 ? (cur / dur) * 100 : 0
                })
                this.innerAudioContext.onEnded(() => {
                    this.isPlaying = false
                    this.voiceProgress = 0
                })
            }
            if (this.isPlaying) {
                this.innerAudioContext.pause()
                this.isPlaying = false
            } else {
                this.innerAudioContext.play()
                this.isPlaying = true
            }
        },
        goConsent() {
            uni.navigateTo({ url: '/pages/micro/customer/consent' })
        },
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
        async confirmSignup() {
            const st = this.submitState
            if (st.disabled) return
            // ★未登录 → 手机验证码登录页（按钮可点击）
            if (st.needLogin) {
                this.goPhoneLogin()
                return
            }
            // ★座位布局活动 → 跳转选座页
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
.page { position: fixed; top: 0; left: 0; right: 0; bottom: 0; background-color: #f5f6f8; display: flex; flex-direction: column; }
.scroll { flex: 1; min-height: 0; }

/* 手绘贴纸 */
.page { position: relative; }
.sticker { position: absolute; opacity: .75; pointer-events: none; z-index: 0; }
.hero-card, .card { position: relative; z-index: 1; }
.sticker-sparkle-tl { top: 16rpx; left: 16rpx; width: 40rpx; height: 40rpx; transform: rotate(-15deg); }
.sticker-star-l { top: 80rpx; left: 10rpx; width: 48rpx; height: 48rpx; transform: rotate(-10deg); }
.sticker-heart-r { top: 60rpx; right: 16rpx; width: 44rpx; height: 44rpx; transform: rotate(12deg); }
.sticker-sparkle-info { top: 16rpx; right: 16rpx; width: 36rpx; height: 36rpx; transform: rotate(20deg); }
.sticker-bubble-info { top: 60rpx; right: 10rpx; width: 48rpx; height: 48rpx; transform: rotate(-8deg); }
.sticker-cloud-info { bottom: 16rpx; right: 16rpx; width: 64rpx; height: 52rpx; transform: rotate(5deg); }
.sticker-star-host { bottom: 16rpx; left: 16rpx; width: 44rpx; height: 44rpx; transform: rotate(-12deg); }
.sticker-heart-host { bottom: 40rpx; left: 60rpx; width: 32rpx; height: 32rpx; transform: rotate(10deg); }
.sticker-bubble-host { top: 16rpx; right: 16rpx; width: 48rpx; height: 48rpx; transform: rotate(8deg); }
.sticker-cloud-host { bottom: 16rpx; right: 16rpx; width: 56rpx; height: 44rpx; transform: rotate(-5deg); }

/* 头图卡 */
.hero-card { position: relative; margin: 0 24rpx 24rpx; height: 420rpx; border-radius: 24rpx; overflow: hidden; }
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
.card { background: #fff; border-radius: 24rpx; padding: 32rpx; margin: 0 24rpx 24rpx; }
.card-title { display: block; font-size: 34rpx; font-weight: bold; color: #1a1a1a; margin-bottom: 24rpx; }
.card-title.no-mb { margin-bottom: 0; }
.card-sub { display: block; font-size: 26rpx; color: #909399; margin-bottom: 28rpx; }

/* 活动信息 */
.info-row { display: flex; align-items: flex-start; margin-bottom: 20rpx; }
.info-label { width: 150rpx; flex-shrink: 0; font-size: 28rpx; color: #909399; }
.info-value { flex: 1; font-size: 30rpx; font-weight: 600; color: #1a1a1a; }
.info-note { display: block; font-size: 28rpx; margin-top: 4rpx; }

/* 本场主持人 */
.host-row { display: flex; justify-content: center; gap: 96rpx; }
.host-cell { display: flex; flex-direction: column; align-items: center; }
.host-avatar { width: 128rpx; height: 128rpx; border-radius: 50%; background: #f0f2f5; }
.host-avatar-none { display: flex; align-items: center; justify-content: center; }
.avatar-char { font-size: 44rpx; color: #909399; }
.host-name { font-size: 30rpx; font-weight: bold; color: #1a1a1a; margin-top: 16rpx; }
.host-card-hint { font-size: 22rpx; color: #c0c4cc; margin-top: 6rpx; }

/* ★主持人点赞（红=已赞 / 白=未赞） */
.host-like { display: flex; align-items: center; justify-content: center; gap: 8rpx; margin-top: 16rpx; padding: 6rpx 24rpx; border-radius: 28rpx; background: #c0c4cc; }
.host-like.is-liked { background: #fef0f0; }
.host-like-icon { font-size: 30rpx; line-height: 1.2; color: #fff; }
.host-like.is-liked .host-like-icon { color: #e64340; }
.host-like-count { font-size: 24rpx; line-height: 1.2; color: #fff; }
.host-like.is-liked .host-like-count { color: #e64340; }

/* 已报名顾客 */
.roster-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 12rpx; }
.roster-count { font-size: 36rpx; font-weight: bold; color: #1a1a1a; }
.roster-row { display: flex; flex-wrap: wrap; gap: 32rpx; }
.roster-cell { width: 110rpx; display: flex; flex-direction: column; align-items: center; }
.roster-avatar { width: 110rpx; height: 110rpx; border-radius: 50%; background: #f0f2f5; }
.roster-avatar-none { display: flex; align-items: center; justify-content: center; }
.roster-avatar .avatar-char, .roster-avatar-none .avatar-char { font-size: 40rpx; }
.roster-avatar.is-wait { opacity: 0.55; }
.roster-avatar-none.is-empty { background: transparent; border: 2rpx dashed #c0c4cc; }
.roster-name { font-size: 26rpx; color: #606266; margin-top: 12rpx; max-width: 110rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; text-align: center; }
.roster-name.is-wait { color: #e6a23c; }
.roster-empty { text-align: center; color: #c0c4cc; font-size: 26rpx; padding: 32rpx 0; }
.roster-gap { display: block; text-align: center; font-size: 28rpx; margin-top: 24rpx; }

/* 活动介绍 */
.intro-content { display: block; font-size: 28rpx; color: #606266; line-height: 1.8; white-space: pre-wrap; word-break: break-all; }
.intro-remark { display: block; font-size: 26rpx; color: #e6a23c; margin-top: 16rpx; }

/* 语音播放器 */
.voice-player { display: flex; align-items: center; background: #f5f7fa; border-radius: 12rpx; padding: 16rpx 20rpx; margin-bottom: 20rpx; }
.voice-play-icon { font-size: 32rpx; margin-right: 16rpx; cursor: pointer; }
.voice-progress { flex: 1; height: 8rpx; background: #e4e7ed; border-radius: 4rpx; overflow: hidden; margin-right: 16rpx; }
.voice-bar { height: 100%; background: #409eff; border-radius: 4rpx; transition: width 0.3s; }
.voice-duration { font-size: 22rpx; color: #909399; white-space: nowrap; }

/* 录音知情告知 */
.voice-notice { display: flex; align-items: center; background: #fdf6ec; border-radius: 8rpx; padding: 12rpx 16rpx; margin-top: 20rpx; }
.voice-icon { font-size: 26rpx; margin-right: 10rpx; flex-shrink: 0; }
.voice-text { font-size: 22rpx; color: #e6a23c; flex: 1; }
.voice-link { font-size: 22rpx; color: #409eff; margin-left: 12rpx; flex-shrink: 0; }

/* 底部确认报名 */
.footer { flex-shrink: 0; background: #fff; padding: 16rpx 32rpx calc(env(safe-area-inset-bottom) + 16rpx); box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.06); }
.submit-btn { color: #fff; border-radius: 48rpx; height: 92rpx; line-height: 92rpx; font-size: 32rpx; font-weight: 600; }
.submit-btn::after { border: none; }
.submit-btn.is-disabled { color: #fff; }
.footer-hint { display: block; text-align: center; font-size: 24rpx; color: #909399; margin-top: 12rpx; }

/* 费用信息栏 */
.fee-bar { display: flex; align-items: center; justify-content: space-between; background: #fff; padding: 16rpx 32rpx; border-top: 1rpx solid #f0f0f0; }
.fee-text { font-size: 28rpx; color: #e6a23c; font-weight: 600; }
.fee-locked { font-size: 24rpx; color: #f56c6c; background: #fef0f0; padding: 4rpx 16rpx; border-radius: 8rpx; }
</style>
