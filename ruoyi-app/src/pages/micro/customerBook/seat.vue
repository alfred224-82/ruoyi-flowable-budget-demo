<template>
    <view class="page" :class="'theme-' + (theme && theme.key)" :style="pageStyle">
        <!-- ★手绘主题装饰贴纸（仅 theme_handdrawn 显示） -->
        <image v-if="theme && theme.key === 'theme_handdrawn'" class="sticker sticker-star-tr" :src="iconUrl('hand-star')" mode="aspectFit" />
        <image v-if="theme && theme.key === 'theme_handdrawn'" class="sticker sticker-cloud-stats" :src="iconUrl('hand-cloud')" mode="aspectFit" />
        <image v-if="theme && theme.key === 'theme_handdrawn'" class="sticker sticker-star-bl" :src="iconUrl('hand-star')" mode="aspectFit" />
        <image v-if="theme && theme.key === 'theme_handdrawn'" class="sticker sticker-bubble-bl" :src="iconUrl('hand-bubble')" mode="aspectFit" />
        <image v-if="theme && theme.key === 'theme_handdrawn'" class="sticker sticker-sparkle-bl" :src="iconUrl('hand-sparkle')" mode="aspectFit" />
        <image v-if="theme && theme.key === 'theme_handdrawn'" class="sticker sticker-sparkle-br" :src="iconUrl('hand-sparkle')" mode="aspectFit" />
        <image v-if="theme && theme.key === 'theme_handdrawn'" class="sticker sticker-cloud-br" :src="iconUrl('hand-cloud')" mode="aspectFit" />
        <!-- 加载态 -->
        <view v-if="!loaded" class="loading">加载中…</view>

        <template v-else>
            <!-- 活动信息头部 -->
            <view class="header-card">
                <text class="activity-title">{{ seatMap.activityTitle || '活动选座' }}</text>
                <view class="stats-row">
                    <text class="stat">总 {{ seatMap.totalSeats || 0 }}</text>
                    <text class="stat free">空闲 {{ seatMap.freeSeats || 0 }}</text>
                    <text class="stat occupied">已占 {{ seatMap.occupiedSeats || 0 }}</text>
                    <text v-if="seatMap.presetSeats > 0" class="stat preset">预置 {{ seatMap.presetSeats }}</text>
                </view>
                <!-- 锁定提示（M28 T28.1 联动） -->
                <view v-if="seatMap.activityLocked" class="locked-banner">
                    <text class="locked-text">🔒 活动已锁定，座位只读</text>
                </view>
            </view>

            <!-- 座位画布区 -->
            <view class="canvas-wrap">
                <!-- 有背景图：绝对定位渲染（M27 T27.3：PRESET 默认图容器按主题主色 20% 铺底，UPLOAD 自传图不叠加滤镜） -->
                <view v-if="seatMap.bgImageUrl" class="canvas-with-bg"
                      :class="{ 'canvas-preset': seatMap.bgImageSource === 'PRESET' }"
                      :style="canvasBgStyle">
                    <!-- ★背景图流内等比渲染（widthFix）：容器高度=图片高度，座位百分比坐标与图片对齐，与 PC 画布/布局编辑器一致（cover 裁切会偏移） -->
                    <image class="canvas-bg" :src="toHttps(seatMap.bgImageUrl)" mode="widthFix" />
                    <view v-for="seat in seatMap.seats" :key="seat.seatId || seat.seatNo"
                          class="seat-marker"
                          :class="seatClass(seat)"
                          :style="seatStyle(seat)"
                          @click="onSeatTap(seat)">
                        <!-- 空闲：加号 -->
                        <text v-if="seat.status === 'FREE'" class="seat-icon">＋</text>
                        <!-- 已占：头像 -->
                        <image v-else-if="seat.userAvatar" class="seat-avatar" :src="toHttps(seat.userAvatar)" mode="aspectFill" />
                        <text v-else class="seat-icon occupied-icon">👤</text>
                        <!-- 预置：橙色标记 -->
                        <text v-if="seat.status === 'PRESET'" class="preset-badge">预</text>
                        <!-- 座位号小字（圆点随座位等比缩小后空间不足，仅在圆点足够大时显示） -->
                        <text v-if="seatSize >= 32" class="seat-no">{{ seat.seatNo || '-' }}</text>
                    </view>
                </view>

                <!-- 无背景图（T26.1 未落地或房间未配置布局）：网格降级渲染 -->
                <view v-else class="canvas-grid">
                    <view v-if="!seatMap.seats || seatMap.seats.length === 0" class="empty-tip">
                        <text>本活动暂未配置座位布局</text>
                    </view>
                    <view v-for="seat in seatMap.seats" :key="seat.seatId || seat.seatNo"
                          class="grid-seat"
                          :class="seatClass(seat)"
                          @click="onSeatTap(seat)">
                        <text v-if="seat.status === 'FREE'" class="grid-icon">＋</text>
                        <image v-else-if="seat.userAvatar" class="grid-avatar" :src="toHttps(seat.userAvatar)" mode="aspectFill" />
                        <text v-else class="grid-icon occupied-icon">👤</text>
                        <text v-if="seat.status === 'PRESET'" class="preset-badge">预</text>
                        <text class="grid-no">{{ seat.seatNo || '-' }}</text>
                    </view>
                </view>
            </view>

            <!-- 图例 -->
            <view class="legend">
                <view class="legend-item"><view class="legend-dot free"></view><text>空闲</text></view>
                <view class="legend-item"><view class="legend-dot occupied"></view><text>已占</text></view>
                <view class="legend-item"><view class="legend-dot preset"></view><text>预置人员</text></view>
                <view class="legend-item"><view class="legend-dot mine"></view><text>我的座位</text></view>
            </view>

            <!-- 底部操作栏 -->
            <view class="footer">
                <view v-if="mySeat" class="my-seat-info">
                    <text class="my-seat-label">我的座位：</text>
                    <text class="my-seat-no">{{ mySeat.seatNo }}</text>
                    <button class="btn-release" size="mini" :disabled="seatMap.activityLocked" @click="releaseMine">取消选座</button>
                </view>
                <view v-else class="tip">
                    <text class="tip-text">{{ seatMap.activityLocked ? '活动已锁定，无法选座' : '点击空闲座位（＋）即可选座' }}</text>
                </view>
            </view>
        </template>

        <!-- 占座人信息弹窗（点击头像仅显示姓名，不显示手机号） -->
        <view v-if="infoVisible" class="modal-mask" @click="infoVisible = false">
            <view class="modal-card" @click.stop>
                <image v-if="infoSeat.userAvatar" class="modal-avatar" :src="toHttps(infoSeat.userAvatar)" mode="aspectFill" />
                <text class="modal-name">{{ infoSeat.userNickName || '已占座' }}</text>
                <text class="modal-seat-no">座位号：{{ infoSeat.seatNo || '-' }}</text>
                <text v-if="infoSeat.status === 'PRESET'" class="modal-preset">预置人员（{{ infoSeat.roleDisplayName || presetRoleText(infoSeat.presetRole) }}）</text>
                <button class="modal-close" size="mini" @click="infoVisible = false">关闭</button>
            </view>
        </view>
    </view>
</template>

<script>
import { get, post, del } from '../../../utils/request'
import config from '../../../config'
import { toHttps } from '../../../utils/format'
import { getThemeSync, loadTenantTheme, themeCssVars, themeSoftBg } from '../../../utils/theme'
import { iconUrl } from '../../../utils/themeIcons'

// ★圆点直径自适应座位间距：座位点由 Boss 沿背景图座位方块落位，取最小邻距（换算为占画布宽比例）× 0.85 ≈ 图中座位方块大小；
// 背景图 widthFix 随容器等比缩放，圆点按同比例换算，保证任意设备/任意背景图下圆点与座位同大小（固定 rpx 在窄屏会显巨大）
const SEAT_MARKER_GAP_FACTOR = 0.85
// 仅一个座位点时的回落比例 / 单点异常间距上限比例（均相对画布宽度）/ 直径下限
const SEAT_MARKER_FALLBACK_RATIO = 0.04
const SEAT_MARKER_MAX_RATIO = 0.06
const SEAT_MARKER_MIN_PX = 12

export default {
    data() {
        return {
            loaded: false,
            activityId: null,
            // M27 T27.3：租户主题（首屏用缓存同步渲染，onShow 拉取最新）
            theme: getThemeSync(),
            seatMap: {
                activityId: null, activityTitle: '', roomId: null, roomName: '',
                layoutId: null, bgImageUrl: null, bgImageSource: null,
                layoutWidth: 1920, layoutHeight: 1080,
                activityLocked: false,
                totalSeats: 0, occupiedSeats: 0, freeSeats: 0, presetSeats: 0,
                mySeatId: null, seats: []
            },
            infoVisible: false,
            infoSeat: {},
            // ★座位圆点直径（px）：按画布实际宽度等比换算，与背景图座位同大小
            seatSize: 0
        }
    },
    computed: {
        /** 当前登录用户的座位（用于底部操作栏展示） */
        mySeat() {
            if (!this.seatMap.mySeatId) return null
            return (this.seatMap.seats || []).find(s => s.seatId === this.seatMap.mySeatId) || null
        },
        /** M27 T27.3：页面根节点注入主题 CSS 变量（座位边框/按钮/链接跟随主题主色） */
        pageStyle() {
            return themeCssVars(this.theme && this.theme.key)
        },
        /** M27 T27.3：背景图改为 <image widthFix> 流内渲染（与 PC 画布/编辑器几何一致），容器仅保留 PRESET 主题铺底色 */
        canvasBgStyle() {
            // ★注入 --ss 变量：座位圆点直径随画布宽度等比缩放（与背景图座位同大小）
            const style = { '--ss': (this.seatSize || 40) + 'px' }
            if (this.seatMap.bgImageSource === 'PRESET') {
                style.backgroundColor = themeSoftBg(this.theme && this.theme.key, 0.2)
            }
            return style
        }
    },
    onLoad(options) {
        // ★ID 保持字符串（雪花ID用 Number() 会丢精度；后端 Jackson 支持 string→Long）
        this.activityId = options.activityId || options.id || null
        if (!this.activityId) {
            uni.showToast({ title: '缺少活动ID', icon: 'none' })
            return
        }
        // ★首屏先按屏宽估算圆点直径，避免固定 rpx 圆点大于背景图座位
        this.initSeatSize()
        this._resizeHandler = () => this.refineSeatSize()
        uni.onWindowResize(this._resizeHandler)
        this.loadSeatMap()
    },
    onUnload() {
        if (this._resizeHandler) {
            uni.offWindowResize(this._resizeHandler)
            this._resizeHandler = null
        }
    },
    onShow() {
        // M27 T27.3：首屏用缓存主题，随后拉取租户最新主题刷新联动色
        this.theme = getThemeSync()
        loadTenantTheme().then(t => { this.theme = t })
    },
    methods: {
        toHttps,
        iconUrl,
        /** 加载活动座位图 */
        async loadSeatMap() {
            try {
                const res = await get(config.api.microSeatMap + this.activityId)
                this.seatMap = (res && res.data) || this.seatMap
            } catch (e) {
                // 已统一提示
            } finally {
                this.loaded = true
                // ★画布渲染后按实际宽度校准圆点直径（旋转屏/折叠屏由 onWindowResize 兜底）
                this.$nextTick(() => this.refineSeatSize())
            }
        },
        /** 按屏宽估算圆点直径（首屏用；画布宽 = 屏宽 - 页面/卡片内边距共 88rpx） */
        initSeatSize() {
            const windowWidth = (uni.getSystemInfoSync() || {}).windowWidth || 375
            const canvasWidth = windowWidth - 88 * (windowWidth / 750)
            this.seatSize = this.calcSeatSize(canvasWidth)
        },
        /** 按画布实际宽度校准圆点直径，保证圆点与背景图座位正好同大小 */
        refineSeatSize() {
            uni.createSelectorQuery().in(this).select('.canvas-with-bg').boundingClientRect(rect => {
                if (rect && rect.width) {
                    this.seatSize = this.calcSeatSize(rect.width)
                }
            }).exec()
        },
        /** 圆点直径 = 座位点最小邻距（百分比坐标换算为画布宽比例）× 0.85，与背景图座位方块同大小 */
        calcSeatSize(canvasWidth) {
            const seats = this.seatMap.seats || []
            // 百分比坐标各向异性：Y 方向按布局高宽比折算为宽度比例
            const aspect = (this.seatMap.layoutHeight || 1080) / (this.seatMap.layoutWidth || 1920)
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
        /** 座位样式类（空闲/已占/预置/我的） */
        seatClass(seat) {
            const cls = []
            if (seat.status === 'FREE') cls.push('seat-free')
            else if (seat.status === 'OCCUPIED') cls.push('seat-occupied')
            else if (seat.status === 'PRESET') cls.push('seat-preset')
            if (this.seatMap.mySeatId && seat.seatId === this.seatMap.mySeatId) cls.push('seat-mine')
            if (this.seatMap.activityLocked) cls.push('seat-readonly')
            return cls.join(' ')
        },
        /** 座位绝对定位样式（百分比坐标 → 像素） */
        seatStyle(seat) {
            // ★坐标以背景图百分比存储（0~1），前端按容器实际尺寸换算
            const x = parseFloat(seat.posXPercent || 0) * 100
            const y = parseFloat(seat.posYPercent || 0) * 100
            return {
                left: x + '%',
                top: y + '%'
            }
        },
        /** 点击座位 */
        onSeatTap(seat) {
            // 锁定后只读
            if (this.seatMap.activityLocked) {
                uni.showToast({ title: '活动已锁定，座位只读', icon: 'none' })
                return
            }
            // 空闲座位 → 选座
            if (seat.status === 'FREE') {
                this.confirmPick(seat)
                return
            }
            // 已占 / 预置 → 仅显示姓名（不显示手机号）
            if (seat.status === 'OCCUPIED' || seat.status === 'PRESET') {
                this.infoSeat = seat
                this.infoVisible = true
                return
            }
        },
        /** 确认选座弹窗 */
        confirmPick(seat) {
            uni.showModal({
                title: '确认选座',
                content: '您选择座位「' + (seat.seatNo || '-') + '」吗？',
                success: async (r) => {
                    if (r.confirm) {
                        await this.doPick(seat)
                    }
                }
            })
        },
        /** 执行选座 */
        async doPick(seat) {
            try {
                await post(config.api.microSeatPick, {
                    activityId: this.activityId,
                    seatId: seat.seatId,
                    seatNo: seat.seatNo
                })
                uni.showToast({ title: '选座成功', icon: 'success' })
                // 刷新座位图
                this.loadSeatMap()
                // ★通知活动详情聚合刷新（按钮态「已选座·查看座位」）
                uni.$emit('seatAssignmentChanged')
            } catch (e) { /* 已统一提示 */ }
        },
        /** 取消自己的座位 */
        releaseMine() {
            if (!this.mySeat || !this.mySeat.assignmentId) {
                uni.showToast({ title: '未找到占座记录', icon: 'none' })
                return
            }
            uni.showModal({
                title: '取消选座',
                content: '确认取消座位「' + this.mySeat.seatNo + '」吗？',
                success: async (r) => {
                    if (r.confirm) {
                        try {
                            await del(config.api.microSeatRelease + this.mySeat.assignmentId)
                            uni.showToast({ title: '已取消', icon: 'success' })
                            this.loadSeatMap()
                            // ★通知活动详情聚合刷新（按钮态回落「选座报名」）
                            uni.$emit('seatAssignmentChanged')
                        } catch (e) { /* 已统一提示 */ }
                    }
                }
            })
        },
        /** 预置角色中文 */
        presetRoleText(role) {
            const map = { guide: '导游', driver: '司机', assistant: '助教' }
            return map[role] || role || '预置'
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 160rpx; background: var(--page-bg, #f5f6f8); }
.loading { text-align: center; color: var(--text-sub, #909399); padding: 80rpx 0; }

/* 头部卡片 */
.header-card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 24rpx 32rpx; margin-bottom: 24rpx; box-shadow: var(--shadow-card, none); }
.activity-title { font-size: 34rpx; font-weight: bold; display: block; margin-bottom: 16rpx; color: var(--text, #303133); }
.stats-row { display: flex; gap: 24rpx; flex-wrap: wrap; }
.stat { font-size: 24rpx; color: var(--text-sub, #606266); }
.stat.free { color: var(--brand, #409eff); }
.stat.occupied { color: #67c23a; }
.stat.preset { color: #ff9800; }
.locked-banner { background: #fef0f0; border-radius: 8rpx; padding: 12rpx 20rpx; margin-top: 16rpx; }
.locked-text { font-size: 24rpx; color: #f56c6c; }

/* 画布区 */
.canvas-wrap { background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 20rpx; margin-bottom: 24rpx; overflow: hidden; box-shadow: var(--shadow-card, none); }
.canvas-with-bg {
    position: relative;
    width: 100%;
    border-radius: 12rpx;
}
/* ★背景图流内等比缩放（宽撑满、高按比例）：容器高度自适应图片比例，座位百分比坐标精准落在图片上 */
.canvas-bg {
    display: block;
    width: 100%;
    border-radius: 12rpx;
}
/* M27 T27.3：PRESET 默认图（透明底线条 SVG）叠加主题主色内描边，与自传图区分 */
.canvas-preset { box-shadow: inset 0 0 0 3rpx var(--brand-weak, rgba(74,144,226,0.2)); }
.canvas-grid {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;
    padding: 20rpx 0;
    min-height: 200rpx;
}
.empty-tip { width: 100%; text-align: center; color: var(--text-sub, #909399); font-size: 26rpx; padding: 60rpx 0; }

/* 座位标记（绝对定位；★直径取容器 --ss 变量随画布宽度等比缩放，与背景图座位同大小；--ss 未注入时回落 80rpx） */
.seat-marker {
    position: absolute;
    width: var(--ss, 80rpx);
    height: var(--ss, 80rpx);
    margin-left: calc(var(--ss, 80rpx) / -2);
    margin-top: calc(var(--ss, 80rpx) / -2);
    border-radius: 50%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    background: #fff;
    border: 3rpx solid #dcdfe6;
    box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.1);
    transition: transform 0.15s;
}
.seat-marker:active { transform: scale(1.1); }
.seat-icon { font-size: calc(var(--ss, 80rpx) * 0.45); color: var(--brand, #409eff); line-height: 1; }
.seat-icon.occupied-icon { color: #67c23a; }
.seat-avatar { width: calc(var(--ss, 80rpx) * 0.7); height: calc(var(--ss, 80rpx) * 0.7); border-radius: 50%; }
.seat-no { font-size: calc(var(--ss, 80rpx) * 0.24); color: #909399; margin-top: 2rpx; line-height: 1; }
.preset-badge {
    position: absolute;
    top: calc(var(--ss, 80rpx) * -0.1);
    right: calc(var(--ss, 80rpx) * -0.1);
    background: #ff9800;
    color: #fff;
    font-size: calc(var(--ss, 80rpx) * 0.24);
    width: calc(var(--ss, 80rpx) * 0.36);
    height: calc(var(--ss, 80rpx) * 0.36);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    line-height: 1;
}

/* 座位状态色（★不使用颜色区分状态，用图标+文字表达；空闲边框跟随主题主色 M27 T27.3，预置人员固定橙色） */
.seat-free { border-color: var(--brand, #409eff); }
.seat-occupied { border-color: #67c23a; }
.seat-preset { border-color: #ff9800; background: #fff8e7; }
.seat-mine { border-color: #f56c6c; border-width: 4rpx; box-shadow: 0 0 12rpx rgba(245,108,108,0.4); }
.seat-readonly { opacity: 0.7; }

/* 网格降级渲染（无背景图时） */
.grid-seat {
    width: 120rpx;
    height: 120rpx;
    border-radius: 12rpx;
    background: #fff;
    border: 3rpx solid #dcdfe6;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    position: relative;
}
.grid-icon { font-size: 40rpx; color: var(--brand, #409eff); }
.grid-icon.occupied-icon { color: #67c23a; }
.grid-avatar { width: 72rpx; height: 72rpx; border-radius: 50%; }
.grid-no { font-size: 20rpx; color: #909399; margin-top: 4rpx; }
.grid-seat.seat-free { border-color: var(--brand, #409eff); }
.grid-seat.seat-occupied { border-color: #67c23a; }
.grid-seat.seat-preset { border-color: #ff9800; background: #fff8e7; }
.grid-seat.seat-mine { border-color: #f56c6c; border-width: 4rpx; }

/* 图例 */
.legend { display: flex; gap: 24rpx; flex-wrap: wrap; background: var(--card-bg, #fff); border-radius: var(--radius-card, 16rpx); padding: 20rpx 32rpx; margin-bottom: 24rpx; box-shadow: var(--shadow-card, none); }
.legend-item { display: flex; align-items: center; gap: 8rpx; font-size: 24rpx; color: var(--text-sub, #606266); }
.legend-dot { width: 20rpx; height: 20rpx; border-radius: 50%; border: 2rpx solid; }
.legend-dot.free { border-color: var(--brand, #409eff); background: var(--brand-weak, #ecf5ff); }
.legend-dot.occupied { border-color: #67c23a; background: #f0f9eb; }
.legend-dot.preset { border-color: #ff9800; background: #fff8e7; }
.legend-dot.mine { border-color: #f56c6c; background: #fef0f0; }

/* 底部操作栏 */
.footer {
    position: fixed;
    left: 0; right: 0; bottom: 0;
    background: var(--card-bg, #fff);
    padding: 20rpx 32rpx;
    box-shadow: 0 -2rpx 12rpx rgba(0,0,0,0.06);
    z-index: 10;
}
.my-seat-info { display: flex; align-items: center; gap: 16rpx; }
.my-seat-label { font-size: 26rpx; color: var(--text-sub, #606266); }
.my-seat-no { font-size: 30rpx; font-weight: bold; color: #f56c6c; flex: 1; }
.btn-release { background: #fff; color: #f56c6c; border: 2rpx solid #f56c6c; border-radius: 8rpx; font-size: 24rpx; }
.tip { text-align: center; }
.tip-text { font-size: 26rpx; color: var(--text-sub, #909399); }

/* 弹窗 */
.modal-mask {
    position: fixed;
    left: 0; top: 0; right: 0; bottom: 0;
    background: rgba(0,0,0,0.5);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 100;
}
.modal-card {
    background: var(--card-bg, #fff);
    border-radius: var(--radius-card, 16rpx);
    padding: 40rpx;
    width: 560rpx;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 16rpx;
}
.modal-avatar { width: 120rpx; height: 120rpx; border-radius: 50%; }
.modal-name { font-size: 32rpx; font-weight: bold; color: var(--text, #303133); }
.modal-seat-no { font-size: 26rpx; color: var(--text-sub, #606266); }
.modal-preset { font-size: 24rpx; color: #ff9800; }
.modal-close { background: #f5f7fa; color: #606266; border-radius: 8rpx; margin-top: 12rpx; }

/* ★手绘主题装饰贴纸 */
.page { position: relative; }
.sticker { position: absolute; opacity: .8; pointer-events: none; z-index: 0; }
.header-card, .canvas-wrap, .legend, .footer { position: relative; z-index: 1; }
.sticker-star-tr { top: 20rpx; right: 20rpx; width: 48rpx; height: 48rpx; transform: rotate(10deg); }
.sticker-cloud-stats { top: 60rpx; right: 16rpx; width: 52rpx; height: 42rpx; transform: rotate(-5deg); }
.sticker-star-bl { bottom: 200rpx; left: 20rpx; width: 48rpx; height: 48rpx; transform: rotate(-12deg); }
.sticker-bubble-bl { bottom: 160rpx; left: 16rpx; width: 64rpx; height: 64rpx; transform: rotate(6deg); }
.sticker-sparkle-bl { bottom: 180rpx; left: 80rpx; width: 36rpx; height: 36rpx; transform: rotate(20deg); }
.sticker-sparkle-br { bottom: 200rpx; right: 40rpx; width: 36rpx; height: 36rpx; transform: rotate(-15deg); }
.sticker-cloud-br { bottom: 160rpx; right: 16rpx; width: 80rpx; height: 64rpx; transform: rotate(5deg); }
</style>
