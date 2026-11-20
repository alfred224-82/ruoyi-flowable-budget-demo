<template>
    <!-- ★M33 T33.8 报名名单页（§7.8，按设计图 v2 重排）
         头部副标题（活动 · 时间）→ 座位分布卡（★复用活动座位布局图 bgImageUrl，与选座页同源一致；
         橙框=主持 / 绿框=已确认 / 灰框=未确认；点击放大）→ 报名名单卡（头像/姓名/座位号/脱敏手机号/确认按钮）
         → 本场主持（不计入名额）→ 底部「全部确认」+「导出名单」
         ★手机号 DOM 只存脱敏值（138****4521），完整号码仅拨号时临时拼 tel: -->
    <view class="page" :class="'theme-' + theme.key" :style="rootStyle">
        <scroll-view class="scroll" scroll-y>
            <view v-if="!loaded" class="empty">加载中…</view>
            <template v-else>
                <!-- 头部副标题：活动 · 时间 -->
                <view class="sub-head">
                    <text class="sub-title">{{ activityTitle }}</text>
                    <text v-if="timeText" class="sub-time">{{ timeText }}</text>
                </view>

                <!-- 座位分布：★座位图片使用活动里的座位布局图（与选座页同一张 bgImageUrl），纯展示不可编辑 -->
                <view class="card">
                    <view class="card-head">
                        <text class="card-title no-mb">座位分布</text>
                        <text v-if="seats.length" class="link" :style="{ color: theme.primaryColor }" @click="zoomSeat = true">🔍 点击放大</text>
                    </view>
                    <view v-if="!seats.length" class="empty-sm">本活动暂未配置座位布局</view>
                    <view v-else-if="seatMap.bgImageUrl" class="canvas-with-bg" :style="canvasStyle(seatSize)">
                        <image class="canvas-bg" :src="toHttps(seatMap.bgImageUrl)" mode="widthFix" />
                        <view v-for="(s, i) in seats" :key="s.seatId || s.seatNo || i"
                              class="seat-box" :class="seatClass(s)" :style="seatStyle(s)">
                            <text class="seat-box-no">{{ s.seatNo || (i + 1) }}</text>
                            <image v-if="s.userId && s.userAvatar" class="seat-box-avatar" :src="toHttps(s.userAvatar)" mode="aspectFill" />
                            <view v-else-if="s.userId" class="seat-box-avatar avatar-none"><text>{{ (s.userNickName || '?').charAt(0) }}</text></view>
                        </view>
                    </view>
                    <!-- 无背景图降级：网格排布（同样式座位块） -->
                    <view v-else class="canvas-grid">
                        <view v-for="(s, i) in seats" :key="'g' + (s.seatId || s.seatNo || i)"
                              class="seat-box grid-box" :class="seatClass(s)">
                            <text class="seat-box-no">{{ s.seatNo || (i + 1) }}</text>
                            <image v-if="s.userId && s.userAvatar" class="seat-box-avatar" :src="toHttps(s.userAvatar)" mode="aspectFill" />
                            <view v-else-if="s.userId" class="seat-box-avatar avatar-none"><text>{{ (s.userNickName || '?').charAt(0) }}</text></view>
                        </view>
                    </view>
                    <view class="legend">
                        <view class="legend-item"><view class="legend-dot host"></view><text>主持</text></view>
                        <view class="legend-item"><view class="legend-dot confirmed"></view><text>已确认</text></view>
                        <view class="legend-item"><view class="legend-dot pending"></view><text>未确认</text></view>
                    </view>
                </view>

                <!-- 报名名单 -->
                <view class="card">
                    <view class="card-head">
                        <text class="card-title no-mb">报名名单</text>
                        <!-- ★T33.10 分享座位表到群（链接态触发） -->
                        <view class="share-wrap">
                            <share-image-generator
                                :activity-title="activityTitle"
                                :activity-time="timeText"
                                :activity-id="activityId"
                                :hosts="shareHosts"
                                :members="shareMembers"
                                :seat-map-bg-url="seatMap.bgImageUrl || ''"
                                :seats="shareSeats"
                                :seat-layout-width="seatMap.layoutWidth || 1920"
                                :seat-layout-height="seatMap.layoutHeight || 1080"
                                btn-text="分享截图 ›"
                                @logged="onShareLogged" />
                        </view>
                    </view>
                    <!-- 顾客行：头像 / 姓名 / 座位号 / 脱敏手机号 / 确认按钮 -->
                    <view v-if="!members.length" class="empty-sm">暂无顾客报名</view>
                    <view v-for="(m, i) in members" :key="'m' + i" class="row" :class="{ 'is-absent': m.absent }">
                        <image v-if="m.avatar" class="avatar" :src="m.avatar" mode="aspectFill" />
                        <view v-else class="avatar avatar-none"><text>{{ (m.name || '?').charAt(0) }}</text></view>
                        <view class="info">
                            <view class="name-line">
                                <text class="name">{{ m.name || '-' }}</text>
                                <text v-if="m.seatNo" class="seat-no">{{ m.seatNo }}</text>
                                <text v-if="m.isWaitlist" class="tag wait-tag">候补</text>
                                <text v-if="m.absent" class="tag absent-tag">不参加</text>
                            </view>
                            <view class="phone-line">
                                <text class="phone">{{ m.phoneMasked || '-' }}</text>
                                <text v-if="m.showPhoneIcon && m.fullPhone" class="dial-sm" @click.stop="dial(m.fullPhone)">📞</text>
                            </view>
                        </view>
                        <view class="actions">
                            <button v-if="!m.absent" class="btn-pill" :class="m.confirmed ? 'confirmed' : 'pending'"
                                    @click.stop="toggleConfirm(m)">
                                {{ m.confirmed ? '已确认' : '确认' }}
                            </button>
                            <button v-else class="btn-pill restore" @click.stop="restore(m)">恢复</button>
                            <text v-if="!m.absent" class="absent-link" @click.stop="markAbsent(m)">不参加</text>
                        </view>
                    </view>

                    <!-- 本场主持（不计入名额） -->
                    <view v-if="hosts.length" class="host-block">
                        <text class="host-block-title">本场主持（不计入名额）</text>
                        <view v-for="(h, i) in hosts" :key="'h' + i" class="row">
                            <image v-if="h.avatar" class="avatar" :src="h.avatar" mode="aspectFill" />
                            <view v-else class="avatar avatar-none"><text>{{ (h.name || '?').charAt(0) }}</text></view>
                            <view class="info">
                                <view class="name-line">
                                    <text class="name">{{ h.name || '-' }}</text>
                                    <text v-if="h.seatNo" class="seat-no">{{ h.seatNo }}</text>
                                </view>
                                <text class="phone">{{ h.phoneMasked || '-' }}</text>
                            </view>
                            <text v-if="h.showPhoneIcon && h.fullPhone" class="dial" @click="dial(h.fullPhone)">📞</text>
                        </view>
                    </view>
                </view>
            </template>
        </scroll-view>

        <!-- 底部操作区：全部确认（主）+ 导出名单（描边） -->
        <view v-if="loaded" class="footer">
            <button class="btn-main" :disabled="!members.length" @click="confirmAll">
                全部确认（{{ confirmedCount }}/{{ customerCount }}）
            </button>
            <button class="btn-outline" @click="exportCsv">导出名单</button>
        </view>

        <!-- 座位图放大遮罩（点击空白关闭） -->
        <view v-if="zoomSeat" class="zoom-mask" @click="zoomSeat = false">
            <scroll-view class="zoom-scroll" :scroll-x="true" :scroll-y="true" @click.stop>
                <view class="canvas-with-bg" :style="[canvasStyle(zoomSeatSize), { width: zoomWidth + 'px' }]">
                    <image class="canvas-bg" :src="toHttps(seatMap.bgImageUrl)" mode="widthFix" />
                    <view v-for="(s, i) in seats" :key="'z' + (s.seatId || s.seatNo || i)"
                          class="seat-box" :class="seatClass(s)" :style="seatStyle(s)">
                        <text class="seat-box-no">{{ s.seatNo || (i + 1) }}</text>
                        <image v-if="s.userId && s.userAvatar" class="seat-box-avatar" :src="toHttps(s.userAvatar)" mode="aspectFill" />
                        <view v-else-if="s.userId" class="seat-box-avatar avatar-none"><text>{{ (s.userNickName || '?').charAt(0) }}</text></view>
                    </view>
                </view>
            </scroll-view>
            <text class="zoom-tip">点击空白处关闭</text>
        </view>
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get, post } from '../../../utils/request'
import config from '../../../config'
import { toHttps } from '../../../utils/format'
import { getThemeSync, loadTenantTheme, themeCssVars } from '../../../utils/theme'
import { getPrimaryRole } from '../../../utils/roles'
import { desensitizePhone } from '../../../utils/privacy'
import ShareImageGenerator from '../../../components/share-image-generator/share-image-generator.vue'

// ★座位块尺寸自适应（与选座页 seat.vue 同口径）：取最小邻距（百分比坐标换算为画布宽比例）× 0.85 ≈ 图中座位方块大小
const SEAT_MARKER_GAP_FACTOR = 0.85
const SEAT_MARKER_FALLBACK_RATIO = 0.04
const SEAT_MARKER_MAX_RATIO = 0.06
const SEAT_MARKER_MIN_PX = 16

export default {
    components: { ShareImageGenerator },
    data() {
        return {
            theme: getThemeSync(),
            loaded: false,
            activityId: '',
            activityTitle: '',
            timeText: '',
            item: {},
            sd: {},
            seatMap: {},
            zoomSeat: false,
            role: '',
            // ★座位块边长（px）：按画布实际宽度等比换算，与背景图座位同大小
            seatSize: 40
        }
    },
    computed: {
        rootStyle() { return themeCssVars(this.theme.key) },
        hosts() {
            return (this.sd.hosts || []).map(h => this.decorate(h, 'salesman'))
        },
        members() {
            return (this.sd.members || []).map(m => this.decorate(m, 'customer'))
        },
        seats() { return (this.seatMap && this.seatMap.seats) || [] },
        customerCount() {
            const c = this.sd.signedCount != null ? this.sd.signedCount : this.members.filter(m => !m.isWaitlist && !m.absent).length
            return c || 0
        },
        confirmedCount() {
            return this.members.filter(m => m.confirmed && !m.absent).length
        },
        /** 顾客确认状态映射（userId → confirmed），供座位块着色（绿=已确认 / 灰=未确认） */
        confirmedMap() {
            const map = {}
            this.members.forEach(m => { if (m.userId != null) map[m.userId] = m.confirmed !== false })
            return map
        },
        /** 放大遮罩画布宽度（屏宽 1.7 倍，可横向滚动查看） */
        zoomWidth() {
            const w = (uni.getSystemInfoSync() || {}).windowWidth || 375
            return Math.round(w * 1.7)
        },
        zoomSeatSize() { return this.calcSeatSize(this.zoomWidth) },
        /** 分享组件入参（主持 + 顾客，手机号只传脱敏值） */
        shareHosts() {
            return this.hosts.map(h => ({ name: h.name, avatar: h.avatar, phoneMasked: h.phoneMasked }))
        },
        shareMembers() {
            return this.members.filter(m => !m.absent).map(m => ({
                name: m.name, avatar: m.avatar, phoneMasked: m.phoneMasked,
                seatNo: m.seatNo, confirmed: m.confirmed
            }))
        },
        /** ★座位分布数据（含确认状态 + 头像/昵称），传给截图组件 */
        shareSeats() {
            return this.seats.map(s => ({
                seatNo: s.seatNo || '',
                posXPercent: s.posXPercent || 0,
                posYPercent: s.posYPercent || 0,
                userId: s.userId || null,
                status: s.status || '',
                presetRole: s.presetRole || '',
                // ★T33.10 修复：分享截图座位块需要显示占座人头像与昵称首字
                userAvatar: s.userAvatar ? toHttps(s.userAvatar) : '',
                userNickName: s.userNickName || '',
                confirmed: s.userId ? (this.confirmedMap[s.userId] !== false) : false
            }))
        }
    },
    onLoad(options) {
        this.activityId = options.activityId || options.id || ''
        if (!this.activityId) {
            uni.showToast({ title: '缺少活动ID', icon: 'none' })
            return
        }
        this.initSeatSize()
    },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        this.theme = getThemeSync()
        loadTenantTheme().then(t => { this.theme = t })
        this.role = getPrimaryRole() || ''
        if (this.activityId) this.load()
    },
    methods: {
        toHttps,
        /** 统一装饰行数据：头像 HTTPS / 手机号脱敏（DOM 只存脱敏值）/ 电话图标可见性 */
        decorate(raw, targetRole) {
            const fullPhone = raw.phone || raw.phoneNumber || raw.phonenumber || ''
            const isSelf = raw.isSelf || false
            return Object.assign({}, raw, {
                name: raw.name || raw.nickName || '',
                avatar: toHttps(raw.avatar || ''),
                // ★DOM 只存脱敏值，完整号码仅拨号时临时用
                phoneMasked: raw.phoneMasked || desensitizePhone(fullPhone),
                fullPhone,
                // 电话图标：老板自己不显示；顾客与员工行显示
                showPhoneIcon: !isSelf && !!fullPhone && this.role !== 'customer',
                confirmed: raw.confirmed !== false && raw.isConfirmed !== false,
                absent: !!raw.absent || raw.status === 'ABSENT',
                seatNo: raw.seatNo || raw.seat || ''
            })
        },
        /** 画布注入 --ss（座位块边长）/ --sa（块内头像直径）变量 */
        canvasStyle(size) {
            return { '--ss': size + 'px', '--sa': Math.max(10, size - 16) + 'px' }
        },
        /** 座位块样式类：橙=主持（预置）/ 绿=已确认 / 灰=未确认 / 浅=空闲 */
        seatClass(s) {
            if (s.status === 'PRESET' || s.presetRole) return 'host'
            if (s.status === 'FREE' || !s.userId) return 'free'
            return this.confirmedMap[s.userId] === false ? 'pending' : 'confirmed'
        },
        /** 座位绝对定位样式（百分比坐标 → left/top，与选座页一致） */
        seatStyle(seat) {
            const x = parseFloat(seat.posXPercent || 0) * 100
            const y = parseFloat(seat.posYPercent || 0) * 100
            return { left: x + '%', top: y + '%' }
        },
        /** 按屏宽估算座位块边长（首屏用；画布宽 = 屏宽 - 页面/卡片内边距共 88rpx） */
        initSeatSize() {
            const windowWidth = (uni.getSystemInfoSync() || {}).windowWidth || 375
            const canvasWidth = windowWidth - 88 * (windowWidth / 750)
            this.seatSize = this.calcSeatSize(canvasWidth)
        },
        /** 座位块边长 = 座位点最小邻距（百分比坐标换算为画布宽比例）× 0.85，与背景图座位方块同大小 */
        calcSeatSize(canvasWidth) {
            const seats = this.seats
            // 百分比坐标各向异性：Y 方向按布局高宽比折算为宽度比例
            const aspect = ((this.seatMap && this.seatMap.layoutHeight) || 1080) / ((this.seatMap && this.seatMap.layoutWidth) || 1920)
            let minDist = 0
            for (let i = 0; i < seats.length; i++) {
                for (let j = i + 1; j < seats.length; j++) {
                    const dx = parseFloat(seats[i].posXPercent || 0) - parseFloat(seats[j].posXPercent || 0)
                    const dy = (parseFloat(seats[i].posYPercent || 0) - parseFloat(seats[j].posYPercent || 0)) * aspect
                    const dist = Math.sqrt(dx * dx + dy * dy)
                    if (dist > 0 && (minDist === 0 || dist < minDist)) minDist = dist
                }
            }
            const ratio = minDist > 0 ? minDist * SEAT_MARKER_GAP_FACTOR : SEAT_MARKER_FALLBACK_RATIO
            return Math.max(SEAT_MARKER_MIN_PX, Math.round(canvasWidth * Math.min(ratio, SEAT_MARKER_MAX_RATIO)))
        },
        async load() {
            this.loaded = false
            try {
                // 报名详情（含 members/hosts/quota；★座位布局活动 BE 已按座位占用记录补全名单）
                const r = await get(config.api.microActivitySignupDetail, { activityId: this.activityId })
                this.sd = (r && r.data) || {}
                const act = this.sd.activity || {}
                this.item = act
                this.activityTitle = act.title || this.sd.activityTitle || '报名名单'
                const s = act.startTime ? String(act.startTime).replace('T', ' ').substring(0, 16) : ''
                const e = act.endTime ? String(act.endTime).replace('T', ' ').substring(0, 16) : ''
                this.timeText = s && e ? s + ' ~ ' + e : (s || e || '')
                // ★座位分布图：复用活动座位布局（与选座页同源 /micro/seat/map/）
                try {
                    const r2 = await get(config.api.microSeatMap + this.activityId)
                    this.seatMap = (r2 && r2.data) || {}
                } catch (e2) { this.seatMap = {} }
            } catch (e) { this.sd = {} } finally {
                this.loaded = true
                this.$nextTick(() => this.initSeatSize())
            }
        },
        /** 拨号：完整号码仅此时临时拼 tel: */
        dial(phone) {
            if (!phone) return
            // #ifdef H5
            window.location.href = 'tel:' + phone
            // #endif
            // #ifndef H5
            uni.makePhoneCall({ phoneNumber: String(phone) })
            // #endif
        },
        /** 逐人确认/取消确认 */
        async toggleConfirm(m) {
            const target = !m.confirmed
            try {
                await post(config.api.microBossRosterConfirm, {
                    activityId: this.activityId,
                    userId: m.userId,
                    confirmed: target
                })
                uni.showToast({ title: target ? '已确认' : '已取消确认', icon: 'none' })
                this.load()
            } catch (e) {
                // BE 未就绪时本地乐观更新，保证页面可用
                m.confirmed = target
            }
        },
        /** 全部确认 */
        confirmAll() {
            uni.showModal({
                title: '全部确认',
                content: '确定将全部 ' + this.customerCount + ' 人标记为已确认吗？',
                success: async (r) => {
                    if (!r.confirm) return
                    try {
                        await post(config.api.microBossRosterConfirmAll, { activityId: this.activityId })
                        uni.showToast({ title: '已全部确认', icon: 'success' })
                        this.load()
                    } catch (e) {
                        this.members.forEach(m => { m.confirmed = true })
                        uni.showToast({ title: '已全部确认', icon: 'success' })
                    }
                }
            })
        },
        /** 标记不参加 */
        markAbsent(m) {
            uni.showModal({
                title: '标记不参加',
                content: '确定将「' + (m.name || '') + '」标记为不参加吗？',
                success: async (r) => {
                    if (!r.confirm) return
                    try {
                        await post(config.api.microBossRosterMarkAbsent, { activityId: this.activityId, userId: m.userId, absent: true })
                        this.load()
                    } catch (e) { m.absent = true }
                }
            })
        },
        /** 恢复（取消不参加标记） */
        async restore(m) {
            try {
                await post(config.api.microBossRosterMarkAbsent, { activityId: this.activityId, userId: m.userId, absent: false })
                this.load()
            } catch (e) { m.absent = false }
        },
        /** 导出名单 CSV */
        async exportCsv() {
            try {
                // ★第三个参数 silent=true：BE 未就绪（404/无权限）时不弹错误提示，静默走本地兜底
                const r = await get(config.api.microBossRosterExport + this.activityId, {}, true)
                const data = (r && r.data) || {}
                const rows = data.rows || this.buildCsvRows()
                this.downloadCsv(rows, data.filename || (this.activityTitle + '_报名名单.csv'))
            } catch (e) {
                // BE 未就绪时前端本地生成 CSV
                this.downloadCsv(this.buildCsvRows(), this.activityTitle + '_报名名单.csv')
            }
        },
        buildCsvRows() {
            const head = ['座位号', '姓名', '手机号(脱敏)', '角色', '确认状态']
            const hostRows = this.hosts.map(h => [h.seatNo || '', h.name, h.phoneMasked, '主持', '-'])
            const memberRows = this.members.filter(m => !m.absent).map(m => [
                m.seatNo || '', m.name, m.phoneMasked, m.isWaitlist ? '候补' : '顾客', m.confirmed ? '已确认' : '待确认'
            ])
            return [head].concat(hostRows, memberRows)
        },
        downloadCsv(rows, filename) {
            const csv = rows.map(r => r.map(c => '"' + String(c == null ? '' : c).replace(/"/g, '""') + '"').join(',')).join('\n')
            // #ifdef H5
            const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8' })
            const url = URL.createObjectURL(blob)
            const a = document.createElement('a')
            a.href = url
            a.download = filename
            document.body.appendChild(a)
            a.click()
            document.body.removeChild(a)
            URL.revokeObjectURL(url)
            uni.showToast({ title: '已导出', icon: 'success' })
            // #endif
            // #ifndef H5
            uni.setClipboardData({ data: csv, success: () => uni.showToast({ title: 'CSV 已复制到剪贴板', icon: 'none' }) })
            // #endif
        },
        /** 分享生成日志（外部可挂接后端留痕接口） */
        onShareLogged(payload) {
            // 生成动作记日志：谁、什么时候、导出了哪个活动
            console.log('[roster-share]', payload)
        }
    }
}
</script>

<style scoped>
.page { position: fixed; inset: 0; background: var(--page-bg, #f5f6f8); display: flex; flex-direction: column; }
.scroll { flex: 1; min-height: 0; padding: 80rpx 24rpx 0; box-sizing: border-box; }
.empty { text-align: center; color: var(--text-sub, #909399); font-size: 26rpx; padding: 100rpx 0; }
.empty-sm { text-align: center; color: #c0c4cc; font-size: 24rpx; padding: 32rpx 0; }

/* 头部副标题（设计稿：活动 · 时间） */
.sub-head { text-align: center; padding: 4rpx 0 20rpx; }
.sub-title { display: block; font-size: 32rpx; font-weight: bold; color: var(--text, #1a1a1a); }
.sub-time { display: block; font-size: 24rpx; color: var(--text-sub, #909399); margin-top: 8rpx; }

.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 20rpx); padding: 28rpx 32rpx; margin-bottom: 20rpx; box-shadow: var(--shadow-card, 0 2rpx 12rpx rgba(0,0,0,.04)); }
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16rpx; }
.card-title { font-size: 30rpx; font-weight: bold; color: var(--text, #303133); display: block; margin-bottom: 16rpx; }
.card-title.no-mb { margin-bottom: 0; }
.link { font-size: 24rpx; }

/* ---------- 座位分布：活动座位布局图（与选座页同源） ---------- */
.canvas-with-bg { position: relative; }
.canvas-bg { width: 100%; display: block; }
.seat-box {
    position: absolute; transform: translate(-50%, -50%);
    width: var(--ss, 40px); height: calc(var(--ss, 40px) * 1.2);
    border-radius: 8px; border: 2px solid #e4e7ed; background: #fafafa;
    display: flex; flex-direction: column; align-items: center; justify-content: center;
    gap: 2px; box-sizing: border-box; overflow: hidden;
}
.seat-box-no { font-size: 10px; color: #909399; line-height: 1; }
.seat-box-avatar { width: var(--sa, 24px); height: var(--sa, 24px); border-radius: 50%; background: #f0f2f5; }
.avatar-none { display: flex; align-items: center; justify-content: center; font-size: 12px; color: #909399; }
.seat-box.host { border-color: #e6a23c; background: #fdf6ec; }
.seat-box.host .seat-box-no { color: #e6a23c; }
.seat-box.confirmed { border-color: #67c23a; background: #f0f9eb; }
.seat-box.confirmed .seat-box-no { color: #67c23a; }
.seat-box.pending { border-color: #909399; background: #f4f4f5; }
.seat-box.pending .seat-box-no { color: #909399; }
.seat-box.free { border-color: #e4e7ed; background: #fafafa; }
/* 无背景图降级：网格排布 */
.canvas-grid { display: flex; flex-wrap: wrap; gap: 12rpx; }
.grid-box { position: static; transform: none; width: 96rpx; height: 116rpx; }
.grid-box .seat-box-no { font-size: 20rpx; }
.grid-box .seat-box-avatar { width: 56rpx; height: 56rpx; }

.legend { display: flex; flex-wrap: wrap; gap: 32rpx; justify-content: center; margin-top: 20rpx; }
.legend-item { display: flex; align-items: center; gap: 8rpx; font-size: 22rpx; color: var(--text-sub, #606266); }
.legend-dot { width: 20rpx; height: 20rpx; border-radius: 50%; }
.legend-dot.host { background: #e6a23c; }
.legend-dot.confirmed { background: #67c23a; }
.legend-dot.pending { background: #c0c4cc; }

/* ---------- 报名名单 ---------- */
.share-wrap >>> .sig-wrap { display: inline-block; }
.share-wrap >>> .sig-btn { background: transparent; color: var(--brand, #409eff); font-size: 24rpx; padding: 0; height: auto; line-height: 1.4; border-radius: 0; }
.share-wrap >>> .sig-btn::after { border: none; }

.row { display: flex; align-items: center; padding: 18rpx 0; border-bottom: 1rpx solid var(--border, #f5f6f8); }
.row:last-child { border-bottom: none; }
.row.is-absent { opacity: 0.5; }
.avatar { width: 80rpx; height: 80rpx; border-radius: 50%; background: #f0f2f5; margin-right: 16rpx; flex-shrink: 0; }
.info { flex: 1; overflow: hidden; }
.name-line { display: flex; align-items: center; gap: 10rpx; flex-wrap: wrap; }
.name { font-size: 28rpx; color: var(--text, #1a1a1a); font-weight: 600; }
.seat-no { font-size: 22rpx; color: var(--text-sub, #606266); }
.phone-line { display: flex; align-items: center; gap: 8rpx; margin-top: 6rpx; }
.phone { font-size: 24rpx; color: var(--text-sub, #909399); }
.dial-sm { font-size: 26rpx; }
.tag { font-size: 20rpx; padding: 2rpx 12rpx; border-radius: 8rpx; }
.wait-tag { background: #909399; color: #fff; }
.absent-tag { background: #f56c6c; color: #fff; }
.dial { font-size: 36rpx; margin-left: 16rpx; flex-shrink: 0; }
.actions { display: flex; flex-direction: column; align-items: center; gap: 6rpx; flex-shrink: 0; margin-left: 16rpx; }
.btn-pill { min-width: 108rpx; height: 52rpx; line-height: 48rpx; padding: 0 20rpx; border-radius: 26rpx; font-size: 22rpx; margin: 0; background: #fff; }
.btn-pill::after { border: none; }
.btn-pill.confirmed { background: #67c23a; color: #fff; }
.btn-pill.pending { background: #fff; color: #e6a23c; border: 1rpx solid #e6a23c; line-height: 46rpx; }
.btn-pill.restore { background: #f0f9eb; color: #67c23a; border: 1rpx solid #c2e7b0; line-height: 46rpx; }
.absent-link { font-size: 20rpx; color: #c0c4cc; }

/* 本场主持（不计入名额） */
.host-block { margin-top: 16rpx; padding-top: 16rpx; border-top: 1rpx dashed #e4e7ed; }
.host-block-title { display: block; font-size: 26rpx; color: var(--text, #303133); margin-bottom: 8rpx; font-weight: 600; }

/* ---------- 底部操作区 ---------- */
.footer { flex-shrink: 0; background: #fff; padding: 16rpx 32rpx calc(env(safe-area-inset-bottom) + 16rpx); box-shadow: 0 -2rpx 12rpx rgba(0,0,0,.06); }
.btn-main { background: #67c23a; color: #fff; border-radius: 44rpx; height: 88rpx; line-height: 88rpx; font-size: 30rpx; font-weight: 600; border: none; }
.btn-main::after { border: none; }
.btn-main[disabled] { opacity: 0.5; }
.btn-outline { margin-top: 16rpx; height: 80rpx; line-height: 76rpx; border-radius: 40rpx; background: #fff; color: #e6a23c; border: 1rpx solid #e6a23c; font-size: 28rpx; }
.btn-outline::after { border: none; }

/* ---------- 座位图放大遮罩 ---------- */
.zoom-mask { position: fixed; inset: 0; background: rgba(0,0,0,.78); z-index: 99; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.zoom-scroll { max-width: 96vw; max-height: 78vh; background: #fff; border-radius: 16rpx; padding: 12rpx; }
.zoom-tip { color: #fff; font-size: 24rpx; margin-top: 24rpx; }
</style>
