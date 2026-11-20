<template>
    <!-- ★M37 T37.2 老板端门店推广码（§7.10）
         老板生成不带员工 ID 的门店码 → 扫码进来顾客计入门店自然流量，不参与员工拉新业绩
         · 服务端聚合五要素（活动主视觉 + 标题/时间/地点/价格 + 实时剩余名额 + 二维码 + 行动指令）
         · 二维码 URL 仅含 ref_t（门店），不含 ref_u（员工）→ 自然流量归因
         · 老板也可切换到某员工视角预览其一人一码（可选） -->
    <view class="page" :class="'theme-' + theme.key" :style="rootStyle">
        <scroll-view class="scroll" scroll-y>
            <!-- 活动选择 -->
            <view class="card">
                <text class="card-title">选择要推广的活动</text>
                <view v-if="!loaded" class="empty">加载中…</view>
                <block v-else>
                    <view v-if="!activities.length" class="empty">暂无进行中的活动</view>
                    <view v-for="a in activities" :key="a.bizId || a.id" class="act-item"
                          :class="{ active: String(a.bizId || a.id) === String(activityId) }"
                          :style="String(a.bizId || a.id) === String(activityId) ? { borderColor: theme.primaryColor } : {}"
                          @click="selectActivity(a.bizId || a.id)">
                        <view class="act-info">
                            <text class="act-title">{{ a.title || '活动' }}</text>
                            <text class="act-time">{{ fmtTime(a.startTime) }}</text>
                        </view>
                        <text v-if="String(a.bizId || a.id) === String(activityId)" class="act-check" :style="{ color: theme.primaryColor }">✓</text>
                    </view>
                </block>
            </view>

            <!-- 门店码预览 + 生成 -->
            <view v-if="activityId" class="card">
                <view class="card-head">
                    <text class="card-title no-mb">门店推广码</text>
                    <text class="link" :style="{ color: theme.primaryColor }" @click="loadPoster">刷新名额</text>
                </view>
                <!-- 实时剩余名额（T37.3） -->
                <view class="remain-bar" :style="{ background: theme.primaryColor }">
                    <text class="remain-text">{{ poster.remainingText || '名额统计中…' }}</text>
                </view>
                <!-- 二维码直显（长按可识别/保存） -->
                <view v-if="poster.qrcodeUrl" class="qr-area">
                    <image class="qr-img" :src="poster.qrcodeUrl" mode="widthFix" show-menu-by-longpress />
                    <text class="qr-tip">扫码看详情并报名 · 计入门店自然流量</text>
                </view>
                <view class="owner">
                    <text class="owner-item">{{ poster.ownerType || '门店推广码' }}</text>
                    <text class="owner-item">{{ poster.ownerName || '' }}</text>
                </view>
                <text class="note">门店码不带员工 ID，扫码进来的顾客不参与员工拉新业绩统计。如需给员工分码，请让员工在「我的」→「生成我的海报」各自生成。</text>
                <!-- 生成海报（五要素长图，保存到相册/贴门店） -->
                <activity-poster :data="poster" code-type="store" :activity-id="activityId"
                                 btn-text="生成门店海报" @logged="onLogged" />
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
        /** 加载老板的活动列表（选择器） */
        async loadActivities() {
            this.loaded = false
            try {
                const r = await get(config.api.microBossMyActivities)
                const list = (r && r.data) || []
                // 兼容不同字段名：bizId / id / activityId
                this.activities = list.map(x => Object.assign({}, x, {
                    bizId: x.bizId || x.id || x.activityId
                })).filter(x => x.bizId)
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
        /** 加载门店码海报数据（五要素 + 二维码 + 实时剩余名额） */
        async loadPoster() {
            if (!this.activityId) return
            try {
                const r = await get(config.api.microPosterData + this.activityId, { codeType: 'store' })
                this.poster = (r && r.data) || {}
            } catch (e) {
                this.poster = {}
                uni.showToast({ title: '海报数据加载失败', icon: 'none' })
            }
        },
        onLogged(payload) {
            // 生成动作记日志：谁、什么时候、生成了哪个门店码海报
            console.log('[store-poster]', payload)
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
.owner { display: flex; justify-content: space-around; margin: 24rpx 0 16rpx; padding-top: 20rpx; border-top: 1rpx solid var(--border, #f0f2f5); }
.owner-item { font-size: 24rpx; color: var(--text-sub, #606266); }
.note { display: block; font-size: 22rpx; color: var(--text-sub, #909399); line-height: 1.6; background: var(--brand-weak, #f7f9fc); border-radius: 12rpx; padding: 16rpx 20rpx; margin-bottom: 20rpx; }
</style>
