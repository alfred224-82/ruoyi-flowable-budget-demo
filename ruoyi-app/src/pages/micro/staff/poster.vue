<template>
    <!-- ★M37 T37.1 员工端一人一码海报（§8.4）
         员工打开自己参与的活动 → 选择活动 → 「生成我的海报」
         · 服务端聚合五要素（活动主视觉 + 标题/时间/地点/价格 + 实时剩余名额 + 二维码 + 行动指令）
         · 二维码 URL 含隐形员工推广码（ref_u=当前员工）→ 扫码自动归因到本人拉新业绩
         · 三名员工各生成各的海报：图面一致，仅二维码参数不同 -->
    <view class="page" :class="'theme-' + theme.key" :style="rootStyle">
        <scroll-view class="scroll" scroll-y>
            <!-- 活动选择 -->
            <view class="card">
                <text class="card-title">选择我参与的活动</text>
                <view v-if="!loaded" class="empty">加载中…</view>
                <block v-else>
                    <view v-if="!activities.length" class="empty">暂无参与的活动</view>
                    <view v-for="a in activities" :key="a.bizId" class="act-item"
                          :class="{ active: String(a.bizId) === String(activityId) }"
                          :style="String(a.bizId) === String(activityId) ? { borderColor: theme.primaryColor } : {}"
                          @click="selectActivity(a.bizId)">
                        <view class="act-info">
                            <text class="act-title">{{ a.title || '活动' }}</text>
                            <text class="act-time">{{ fmtTime(a.startTime) }}</text>
                        </view>
                        <text v-if="String(a.bizId) === String(activityId)" class="act-check" :style="{ color: theme.primaryColor }">✓</text>
                    </view>
                </block>
            </view>

            <!-- 海报预览 + 生成 -->
            <view v-if="activityId" class="card">
                <view class="card-head">
                    <text class="card-title no-mb">我的推广海报</text>
                    <text class="link" :style="{ color: theme.primaryColor }" @click="loadPoster">刷新名额</text>
                </view>
                <!-- 实时剩余名额（T37.3） -->
                <view class="remain-bar" :style="{ background: theme.primaryColor }">
                    <text class="remain-text">{{ poster.remainingText || '名额统计中…' }}</text>
                </view>
                <!-- 二维码直显（长按可识别/保存） -->
                <view v-if="poster.qrcodeUrl" class="qr-area">
                    <image class="qr-img" :src="poster.qrcodeUrl" mode="widthFix" show-menu-by-longpress />
                    <text class="qr-tip">扫码看详情并报名 · 业绩自动归属您</text>
                </view>
                <view class="owner">
                    <text class="owner-item">{{ poster.ownerType || '员工推广码' }}</text>
                    <text class="owner-item">{{ poster.ownerName || '' }}</text>
                </view>
                <!-- 生成海报（五要素长图，保存到相册发群） -->
                <activity-poster :data="poster" code-type="staff" :activity-id="activityId"
                                 btn-text="生成我的海报" @logged="onLogged" />
            </view>
        </scroll-view>
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get } from '../../../utils/request'
import config from '../../../config'
import { applyNavBarTitle } from '../../../utils/roles'
import { getThemeSync, loadTenantTheme, themeCssVars } from '../../../utils/theme'
import ActivityPoster from '../../../components/activity-poster/activity-poster.vue'

export default {
    components: { ActivityPoster },
    data() {
        return {
            theme: getThemeSync(),
            loaded: false,
            activityId: '',
            activities: [],
            poster: {}
        }
    },
    computed: {
        rootStyle() { return themeCssVars(this.theme.key) }
    },
    onLoad(options) {
        this.activityId = options.activityId || options.id || ''
    },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        applyNavBarTitle()
        this.theme = getThemeSync()
        loadTenantTheme().then(t => { this.theme = t })
        this.loadActivities()
    },
    methods: {
        fmtTime(t) { return t ? String(t).replace('T', ' ').substring(0, 16) : '' },
        /** 加载我参与的活动（选择器） */
        async loadActivities() {
            this.loaded = false
            try {
                const r = await get(config.api.microStaffMyActivities)
                const list = (r && r.data) || []
                // 仅保留活动类（ACTIVITY_SIGNUP），排除自助预约订单
                this.activities = list.filter(x => x.sourceType === 'ACTIVITY_SIGNUP' || x.bizId)
                // 无指定活动时默认选中第一个
                if (!this.activityId && this.activities.length) {
                    this.activityId = this.activities[0].bizId
                }
            } catch (e) {
                this.activities = []
            } finally {
                this.loaded = true
            }
            if (this.activityId) this.loadPoster()
        },
        selectActivity(id) {
            this.activityId = id
            this.loadPoster()
        },
        /** 加载海报数据（五要素 + 二维码 + 实时剩余名额） */
        async loadPoster() {
            if (!this.activityId) return
            try {
                const r = await get(config.api.microPosterData + this.activityId, { codeType: 'staff' })
                this.poster = (r && r.data) || {}
            } catch (e) {
                this.poster = {}
                uni.showToast({ title: '海报数据加载失败', icon: 'none' })
            }
        },
        onLogged(payload) {
            // 生成动作记日志：谁、什么时候、生成了哪个活动海报
            console.log('[staff-poster]', payload)
        }
    }
}
</script>

<style scoped>
.page { position: fixed; inset: 0; background: var(--page-bg, #f5f6f8); display: flex; flex-direction: column; }
.scroll { flex: 1; min-height: 0; padding: 24rpx; box-sizing: border-box; }
.empty { text-align: center; color: var(--text-sub, #909399); font-size: 26rpx; padding: 60rpx 0; }
.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 20rpx); padding: 28rpx 32rpx; margin-bottom: 20rpx; box-shadow: var(--shadow-card, 0 2rpx 12rpx rgba(0,0,0,.04)); }
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16rpx; }
.card-title { font-size: 30rpx; font-weight: bold; color: var(--text, #303133); display: block; margin-bottom: 16rpx; }
.card-title.no-mb { margin-bottom: 0; }
.link { font-size: 24rpx; }

.act-item { display: flex; align-items: center; padding: 20rpx; border: 2rpx solid var(--border, #f0f2f5); border-radius: 16rpx; margin-bottom: 14rpx; }
.act-item.active { background: var(--brand-weak, #ecf5ff); }
.act-info { flex: 1; overflow: hidden; }
.act-title { display: block; font-size: 28rpx; color: var(--text, #1a1a1a); font-weight: 600; }
.act-time { display: block; font-size: 22rpx; color: var(--text-sub, #909399); margin-top: 6rpx; }
.act-check { font-size: 34rpx; font-weight: bold; }

.remain-bar { display: inline-block; padding: 10rpx 28rpx; border-radius: 30rpx; margin-bottom: 20rpx; }
.remain-text { color: #fff; font-size: 26rpx; font-weight: bold; }
.qr-area { text-align: center; }
.qr-img { width: 360rpx; height: 360rpx; display: block; margin: 0 auto; }
.qr-tip { font-size: 24rpx; color: var(--text-sub, #909399); display: block; margin-top: 12rpx; text-align: center; }
.owner { display: flex; justify-content: space-around; margin: 24rpx 0; padding-top: 20rpx; border-top: 1rpx solid var(--border, #f0f2f5); }
.owner-item { font-size: 24rpx; color: var(--text-sub, #606266); }
</style>
