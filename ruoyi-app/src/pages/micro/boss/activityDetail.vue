<template>
    <!-- ★M33 T33.7 活动详情页（§7.7，操作视图）
         上半部：报名详情（复用 T33.2 弹窗内容内嵌展示）
         底部操作区：「确认报名」（主按钮 + 人数统计）/ 结束 / 下架 / 锁定（三浅色按钮并排）/ 关闭
         锁定条件：全部人员已确认且达成团门槛，否则置灰提示原因 -->
    <view class="page" :class="'theme-' + theme.key" :style="rootStyle">
        <scroll-view class="scroll" scroll-y>
            <view v-if="!loaded" class="empty">加载中…</view>
            <template v-else>
                <!-- 活动信息头 -->
                <view class="card">
                    <view class="head">
                        <text class="title">{{ item.title || '-' }}</text>
                        <text class="pill" :class="'t-' + statusPill.type" :style="pillStyle(statusPill.type)">{{ statusPill.label }}</text>
                    </view>
                    <view class="info-row"><text class="info-label">活动时间</text><text class="info-value">{{ timeText }}</text></view>
                    <view v-if="roomText" class="info-row"><text class="info-label">指定房间</text><text class="info-value">{{ roomText }}</text></view>
                    <view class="info-row"><text class="info-label">费用</text><text class="info-value">¥{{ sd.perPersonPrice != null ? sd.perPersonPrice : (item.price != null ? item.price : '-') }}/人（共{{ sd.participantCount || item.participantCount || '-' }}人）</text></view>
                </view>

                <!-- 报名详情（内嵌，与 T33.2 弹窗口径一致） -->
                <view class="card">
                    <view class="card-head">
                        <text class="card-title no-mb">报名详情</text>
                        <text class="link" :style="{ color: theme.primaryColor }" @click="goRoster">查看完整名单 ›</text>
                    </view>
                    <view class="count-line">
                        <text class="count-num">{{ customerCount }}</text>
                        <text class="count-label">人已报名</text>
                        <text v-if="quota" class="count-quota">/ 名额 {{ quota }}</text>
                        <text v-if="pendingCount > 0" class="count-pending">· {{ pendingCount }} 人待确认</text>
                    </view>
                    <text class="note">主持不计入名额</text>
                    <view v-if="allRows.length" class="grid">
                        <view v-for="(r, i) in allRows" :key="i" class="cell">
                            <view class="avatar-wrap">
                                <image v-if="r.avatar" class="avatar" :class="{ 'is-host': r.isHost }" :src="r.avatar" mode="aspectFill" />
                                <view v-else class="avatar avatar-none" :class="{ 'is-host': r.isHost }"><text>{{ (r.name || '?').charAt(0) }}</text></view>
                                <text v-if="r.isHost" class="tag-host">主持</text>
                                <text v-else-if="r.isWaitlist" class="tag-wait">候补</text>
                            </view>
                            <text class="name">{{ r.name || '-' }}</text>
                        </view>
                        <view v-for="n in emptyCount" :key="'e' + n" class="cell">
                            <view class="avatar avatar-empty"></view>
                            <text class="name name-empty">空位</text>
                        </view>
                    </view>
                    <view v-else class="empty-sm">暂无顾客报名</view>
                    <text v-if="gapText" class="gap">{{ gapText }}</text>
                </view>

                <!-- 锁定条件提示 -->
                <view v-if="lockHint" class="card hint-card">
                    <text class="hint-text">🔒 {{ lockHint }}</text>
                </view>
            </template>
        </scroll-view>

        <!-- 底部操作区 -->
        <view v-if="loaded" class="footer">
            <!-- 主按钮：确认报名（进入名单页逐人确认） -->
            <button class="btn-main" :style="{ background: theme.primaryColor }" @click="goRoster">
                确认报名（{{ confirmedCount }}/{{ customerCount }}）
            </button>
            <!-- 三浅色按钮并排：结束 / 下架 / 锁定 -->
            <view class="btn-row">
                <button class="btn-soft warn" :disabled="!canEnd" @click="doEnd">结束</button>
                <button class="btn-soft gray" :disabled="!canShelf" @click="doShelf">{{ item.status === 'DRAFT' ? '上架' : '下架' }}</button>
                <button class="btn-soft lock" :disabled="!canLock" @click="doLock">{{ sd.isLocked || item.isLocked ? '已锁定' : '锁定' }}</button>
            </view>
            <button class="btn-close" @click="close">关闭</button>
        </view>
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get, put } from '../../../utils/request'
import config from '../../../config'
import { toHttps, isActivityEnded } from '../../../utils/format'
import { getThemeSync, loadTenantTheme, themeCssVars } from '../../../utils/theme'
import { hasFeature } from '../../../utils/features'

export default {
    data() {
        return {
            theme: getThemeSync(),
            loaded: false,
            activityId: '',
            item: {},
            sd: {},
            activityLockOn: false
        }
    },
    computed: {
        rootStyle() { return themeCssVars(this.theme.key) },
        hosts() { return this.sd.hosts || [] },
        members() { return this.sd.members || [] },
        quota() { return this.sd.quota || this.sd.maxParticipants || this.item.totalSeats || 0 },
        customerCount() {
            const c = this.sd.signedCount != null ? this.sd.signedCount : this.members.filter(m => !m.isWaitlist).length
            return c || 0
        },
        /** 已确认人数（confirmed 字段，缺省视为已确认以兼容旧数据） */
        confirmedCount() {
            return this.members.filter(m => m.confirmed !== false && m.isConfirmed !== false).length
        },
        pendingCount() { return Math.max(this.customerCount - this.confirmedCount, 0) },
        /** 达成团门槛（remaining<=0 或 confirmed>=quota） */
        reachedThreshold() {
            if (!this.quota) return this.confirmedCount > 0
            return this.confirmedCount >= this.quota || (this.sd.remaining != null && this.sd.remaining <= 0)
        },
        allRows() {
            const hosts = this.hosts.map(h => ({ name: h.name || h.nickName || '', avatar: toHttps(h.avatar || ''), isHost: true, isWaitlist: false }))
            const members = this.members.map(m => ({ name: m.name || m.nickName || '', avatar: toHttps(m.avatar || ''), isHost: false, isWaitlist: !!m.isWaitlist }))
            return hosts.concat(members)
        },
        emptyCount() {
            const remaining = this.sd.remaining != null ? this.sd.remaining : Math.max((this.quota || 0) - this.customerCount, 0)
            return Math.min(remaining || 0, 10)
        },
        gapText() {
            const remaining = this.sd.remaining
            return remaining > 0 ? '还差 ' + remaining + ' 人成团' : ''
        },
        timeText() {
            const s = this.item.startTime ? String(this.item.startTime).replace('T', ' ').substring(0, 16) : ''
            const e = this.item.endTime ? String(this.item.endTime).replace('T', ' ').substring(0, 16) : ''
            return s && e ? s + ' ~ ' + e : (s || e || '-')
        },
        roomText() { return (this.sd.roomNames || []).join('、') },
        ended() { return isActivityEnded(this.item) || this.item.status === 'ENDED' },
        locked() { return !!(this.sd.isLocked || this.item.isLocked) },
        statusPill() {
            if (this.item.status === 'DRAFT') return { label: '已下架', type: 'gray' }
            if (this.ended) return { label: '已结束', type: 'gray' }
            if (this.locked) return { label: '已锁定', type: 'warn-solid' }
            return { label: '报名中', type: 'success-soft' }
        },
        canEnd() { return !this.ended && this.item.status !== 'DRAFT' },
        canShelf() { return !this.ended },
        /** 锁定条件：全部人员已确认且达成团门槛，且功能开关开启 */
        canLock() { return this.activityLockOn && !this.locked && !this.ended && this.pendingCount === 0 && this.reachedThreshold },
        lockHint() {
            if (this.locked) return '活动已锁定，座位与名单只读'
            if (!this.activityLockOn) return '锁定功能未开通（FEAT_ACTIVITY_LOCK）'
            if (this.pendingCount > 0) return '还有 ' + this.pendingCount + ' 人未确认，全部确认后可锁定'
            if (!this.reachedThreshold) return '未达成团门槛，暂不可锁定'
            return ''
        }
    },
    onLoad(options) {
        this.activityId = options.id || options.activityId || ''
        if (!this.activityId) {
            uni.showToast({ title: '缺少活动ID', icon: 'none' })
            return
        }
    },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        this.theme = getThemeSync()
        loadTenantTheme().then(t => { this.theme = t })
        this.activityLockOn = hasFeature('FEAT_ACTIVITY_LOCK')
        if (this.activityId) this.load()
    },
    methods: {
        pillStyle(type) {
            const primary = this.theme.primaryColor || '#409eff'
            if (type === 'primary-solid') return { background: primary, color: '#fff' }
            if (type === 'primary-soft') return { color: primary, background: primary + '1a' }
            return {}
        },
        async load() {
            this.loaded = false
            try {
                // 优先 M33 老板端详情接口，降级到既有 signupDetail
                let detail
                try {
                    const r = await get(config.api.microBossActivityDetail + this.activityId, null, true)  // silent=true：降级请求不弹 toast
                    detail = (r && r.data) || {}
                    this.item = detail.activity || detail || {}
                } catch (e) {
                    this.item = {}
                }
                const r2 = await get(config.api.microActivitySignupDetail, { activityId: this.activityId })
                this.sd = (r2 && r2.data) || {}
                if (this.sd.activity && typeof this.sd.activity === 'object') {
                    this.item = Object.assign({}, this.item, this.sd.activity)
                }
            } catch (e) { this.sd = {} } finally { this.loaded = true }
        },
        goRoster() {
            uni.navigateTo({ url: '/pages/micro/boss/roster?activityId=' + this.activityId })
        },
        /** 关闭：★修复「点关闭没作用」——H5 无历史栈时 navigateBack 静默失效（fail 不回调），
         *  改为显式判断页面栈深度：有上一页则返回，否则 reLaunch 回活动列表 */
        close() {
            const pages = getCurrentPages() || []
            if (pages.length > 1) {
                uni.navigateBack()
            } else {
                uni.reLaunch({ url: '/pages/micro/boss/activityList' })
            }
        },
        /** 结束活动 */
        doEnd() {
            uni.showModal({
                title: '结束活动',
                content: '结束后活动不可再报名，确定结束「' + (this.item.title || '') + '」吗？',
                success: async (r) => {
                    if (!r.confirm) return
                    try {
                        await put(config.api.microPromoStatus + '/' + this.activityId + '/ENDED')
                        uni.showToast({ title: '已结束', icon: 'success' })
                        this.load()
                    } catch (e) { /* 请求层统一提示 */ }
                }
            })
        },
        /** 上架/下架 */
        doShelf() {
            const target = this.item.status === 'DRAFT' ? 'PUBLISHED' : 'DRAFT'
            const label = target === 'DRAFT' ? '下架' : '上架'
            uni.showModal({
                title: label + '活动',
                content: '确定' + label + '「' + (this.item.title || '') + '」吗？',
                success: async (r) => {
                    if (!r.confirm) return
                    try {
                        await put(config.api.microPromoStatus + '/' + this.activityId + '/' + target)
                        uni.showToast({ title: label + '成功', icon: 'success' })
                        this.load()
                    } catch (e) { /* 请求层统一提示 */ }
                }
            })
        },
        /** 锁定活动（条件不满足时按钮已置灰） */
        doLock() {
            if (!this.canLock) {
                uni.showToast({ title: this.lockHint || '当前不可锁定', icon: 'none' })
                return
            }
            uni.showModal({
                title: '锁定活动',
                content: '锁定后顾客不可报名/选座，座位变只读。确定锁定「' + (this.item.title || '') + '」吗？',
                success: async (r) => {
                    if (!r.confirm) return
                    try {
                        await put(config.api.microActivityLockBase + this.activityId + '/lock')
                        uni.showToast({ title: '锁定成功', icon: 'success' })
                        this.load()
                    } catch (e) { /* 请求层统一提示 */ }
                }
            })
        }
    }
}
</script>

<style scoped>
.page { position: fixed; inset: 0; background: var(--page-bg, #f5f6f8); display: flex; flex-direction: column; }
.scroll { flex: 1; min-height: 0; padding: 80rpx 24rpx 0; box-sizing: border-box; }
.empty { text-align: center; color: var(--text-sub, #909399); font-size: 26rpx; padding: 100rpx 0; }
.empty-sm { text-align: center; color: #c0c4cc; font-size: 24rpx; padding: 32rpx 0; }

.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 20rpx); padding: 28rpx 32rpx; margin-bottom: 20rpx; box-shadow: var(--shadow-card, 0 2rpx 12rpx rgba(0,0,0,.04)); }
.head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16rpx; }
.title { font-size: 34rpx; font-weight: bold; color: var(--text, #1a1a1a); flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.pill { display: inline-block; font-size: 22rpx; padding: 4rpx 18rpx; border-radius: 10rpx; flex-shrink: 0; margin-left: 12rpx; }
.t-warn-solid { background: #e6a23c; color: #fff; }
.t-success-soft { background: #e8f8e0; color: #52b647; }
.t-gray { background: var(--brand-weak, #f0f2f5); color: var(--text-sub, #909399); }

.info-row { display: flex; align-items: flex-start; margin-bottom: 12rpx; }
.info-label { width: 140rpx; flex-shrink: 0; font-size: 26rpx; color: var(--text-sub, #909399); }
.info-value { flex: 1; font-size: 28rpx; font-weight: 600; color: var(--text, #1a1a1a); }

.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16rpx; }
.card-title { font-size: 30rpx; font-weight: bold; color: var(--text, #303133); display: block; margin-bottom: 16rpx; }
.card-title.no-mb { margin-bottom: 0; }
.link { font-size: 24rpx; }

.count-line { display: flex; align-items: baseline; justify-content: center; gap: 8rpx; padding: 8rpx 0; }
.count-num { font-size: 52rpx; font-weight: bold; color: var(--brand, #409eff); line-height: 1; }
.count-label { font-size: 26rpx; color: #303133; }
.count-quota { font-size: 22rpx; color: #909399; margin-left: 8rpx; }
.count-pending { font-size: 22rpx; color: #e6a23c; margin-left: 8rpx; }
.note { display: block; text-align: center; font-size: 22rpx; color: #909399; margin-bottom: 20rpx; }

.grid { display: flex; flex-wrap: wrap; gap: 24rpx; }
.cell { width: 110rpx; display: flex; flex-direction: column; align-items: center; }
.avatar-wrap { position: relative; width: 96rpx; height: 96rpx; }
.avatar { width: 96rpx; height: 96rpx; border-radius: 50%; background: #f0f2f5; }
.avatar.is-host { border: 4rpx solid #e6a23c; box-sizing: border-box; }
.avatar-none { display: flex; align-items: center; justify-content: center; font-size: 32rpx; color: #909399; }
.avatar-empty { background: transparent; border: 2rpx dashed #c0c4cc; box-sizing: border-box; }
.tag-host { position: absolute; bottom: -6rpx; left: 50%; transform: translateX(-50%); background: #e6a23c; color: #fff; font-size: 18rpx; padding: 2rpx 10rpx; border-radius: 8rpx; white-space: nowrap; }
.tag-wait { position: absolute; bottom: -6rpx; left: 50%; transform: translateX(-50%); background: #909399; color: #fff; font-size: 18rpx; padding: 2rpx 10rpx; border-radius: 8rpx; white-space: nowrap; }
.name { font-size: 22rpx; color: #606266; margin-top: 12rpx; max-width: 110rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; text-align: center; }
.name-empty { color: #c0c4cc; }
.gap { display: block; text-align: center; font-size: 26rpx; color: var(--brand, #409eff); margin-top: 20rpx; }

.hint-card { background: #fdf6ec; }
.hint-text { font-size: 24rpx; color: #e6a23c; line-height: 1.6; }

.footer { flex-shrink: 0; background: #fff; padding: 16rpx 32rpx calc(env(safe-area-inset-bottom) + 16rpx); box-shadow: 0 -2rpx 12rpx rgba(0,0,0,.06); }
.btn-main { color: #fff; border-radius: 44rpx; height: 88rpx; line-height: 88rpx; font-size: 30rpx; font-weight: 600; border: none; }
.btn-main::after { border: none; }
.btn-row { display: flex; gap: 16rpx; margin-top: 16rpx; }
.btn-soft { flex: 1; height: 72rpx; line-height: 72rpx; font-size: 26rpx; border-radius: 36rpx; border: none; margin: 0; }
.btn-soft::after { border: none; }
.btn-soft[disabled] { opacity: 0.5; }
.btn-soft.warn { background: #fdf6ec; color: #e6a23c; border: 1rpx solid #f5dab1; }
.btn-soft.gray { background: #f0f2f5; color: #606266; }
.btn-soft.lock { background: #fef0f0; color: #f56c6c; border: 1rpx solid #fbc4c4; }
.btn-close { margin-top: 16rpx; height: 72rpx; line-height: 72rpx; font-size: 26rpx; border-radius: 36rpx; background: transparent; color: #909399; border: 1rpx solid #dcdfe6; }
.btn-close::after { border: none; }
</style>
