<template>
    <view class="page">
        <!-- 缺少 tenantId 提示 -->
        <view v-if="!tenantId" class="empty">
            <text class="empty-text">缺少门店信息，无法加载活动</text>
        </view>
        <template v-else>
            <!-- 活动列表（大图卡片风格） -->
            <view v-if="!loaded" class="loading">加载中…</view>
            <view v-else-if="!list.length" class="empty">
                <text class="empty-text">暂无活动</text>
            </view>
            <view v-else class="act-cards">
                <view v-for="item in list" :key="item.id" class="act-card" @click="viewDetail(item)">
                    <!-- 背景图 -->
                    <image v-if="item.bgImageUrl || item.coverUrl"
                           class="act-bg" :src="toHttps(item.bgImageUrl || firstImage(item.coverUrl))" mode="aspectFill" />
                    <view v-else class="act-bg act-bg-default" :style="{ background: themeBg(item.themeKey) }"></view>
                    <!-- 底部渐变遮罩 -->
                    <view class="act-overlay"></view>
                    <!-- 文字 + 按钮 -->
                    <view class="act-bottom">
                        <view class="act-text">
                            <text class="act-title">{{ item.title || '-' }}</text>
                            <text class="act-status">{{ activityStatusText(item) }}</text>
                        </view>
                        <view class="act-btn">{{ activityBtnText(item) }}</view>
                    </view>
                </view>
            </view>
        </template>

        <!-- ★ 备案信息（ICP + 公安备案，取自 config.icp；匿名可访问页，页尾展示） -->
        <icp-footer />

        <!-- 底部导航 -->
        <micro-tabbar v-if="showTabbar" current="/pages/micro/activity/browse" />
    </view>
</template>

<script>
import { get } from '../../../utils/request'
import config from '../../../config'
import { themeGradient } from '../../../utils/theme'
import { getMicroTabConfig, getTenantId } from '../../../utils/roles'
import { toHttps, firstImage, isActivityEnded, isActivityLocked } from '../../../utils/format'
import MicroTabbar from '../../../components/micro-tabbar/micro-tabbar.vue'
import IcpFooter from '../../../components/icp-footer/icp-footer.vue'

export default {
    components: { MicroTabbar, IcpFooter },
    data() {
        return {
            tenantId: null,
            loaded: false,
            list: []
        }
    },
    computed: {
        showTabbar() {
            const tabs = (getMicroTabConfig().tabs || [])
            return tabs.some(t => t.page === '/pages/micro/activity/browse')
        }
    },
    onLoad(options) {
        if (options.tenantId) {
            this.tenantId = options.tenantId
        } else {
            const tid = getTenantId()
            if (tid) {
                this.tenantId = String(tid)
            }
        }
        if (this.tenantId) {
            this.loadActivities()
        }
        this.checkFirstVisit()
    },
    methods: {
        toHttps,
        firstImage,
        themeBg(themeKey) { return themeGradient(themeKey) },

        /** 活动状态文案：已结束 > 已锁定 > 报名进行中 */
        activityStatusText(item) {
            if (isActivityEnded(item)) return '已结束'
            if (isActivityLocked(item)) return '活动已锁定'
            return '报名进行中'
        },
        /** 活动按钮文案：锁定/结束活动对客只读 → 查看活动 */
        activityBtnText(item) {
            return (isActivityEnded(item) || isActivityLocked(item)) ? '查看活动' : '参与活动'
        },

        async loadActivities() {
            this.loaded = false
            try {
                const params = { tenantId: this.tenantId }
                const res = await get(config.api.microActivityBrowse, params)
                const list = (res && res.data) || []
                // ★防线：已结束活动不进浏览列表（后端 listPublished 已过滤，前端兼容旧后端快照）
                this.list = list.filter(a => !isActivityEnded(a))
            } catch (e) {
                this.list = []
            } finally {
                this.loaded = true
            }
        },
        /** ★活动详情单页化：所有角色/入口统一跳 pages/micro/activity/detail，页面按 id 自取全量数据 */
        viewDetail(item) {
            uni.navigateTo({ url: '/pages/micro/activity/detail?id=' + encodeURIComponent(JSON.stringify(item)) })
        },
        /** T18.17 首次进入活动页弹窗确认录音知情 */
        checkFirstVisit() {
            const agreed = uni.getStorageSync('voice_consent_agreed')
            const visited = uni.getStorageSync('voice_browse_visited')
            if (!visited && !agreed) {
                uni.setStorageSync('voice_browse_visited', true)
                uni.showModal({
                    title: '录音知情告知',
                    content: '本店服务过程可能进行录音/录像，参与活动即视为知悉并同意。点击「确认」查看详情。',
                    confirmText: '确认',
                    cancelText: '稍后再看',
                    success: (res) => {
                        if (res.confirm) {
                            uni.navigateTo({ url: '/pages/micro/customer/consent' })
                        }
                    }
                })
            }
        }
    }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 140rpx; min-height: 100vh; background: #f5f6f8; }

/* ★ 活动大图卡片 */
.act-cards { display: flex; flex-direction: column; gap: 20rpx; }
.act-card {
    position: relative;
    height: 320rpx;
    border-radius: 20rpx;
    overflow: hidden;
}
.act-card:active { transform: scale(0.98); }
.act-bg { position: absolute; top: 0; left: 0; width: 100%; height: 100%; }
.act-bg-default { background: linear-gradient(135deg, #409eff, #66b1ff); }
.act-overlay {
    position: absolute; bottom: 0; left: 0; right: 0; height: 60%;
    background: linear-gradient(to top, rgba(0,0,0,0.75) 0%, transparent 100%);
}
.act-bottom {
    position: absolute; bottom: 0; left: 0; right: 0;
    display: flex; align-items: flex-end; justify-content: space-between;
    padding: 28rpx 32rpx;
}
.act-text { flex: 1; }
.act-title { font-size: 36rpx; font-weight: bold; color: #fff; display: block; line-height: 1.3; }
.act-status { font-size: 26rpx; color: rgba(255,255,255,0.8); margin-top: 8rpx; display: block; }
.act-btn {
    background: #fff;
    color: #1a1a1a;
    font-size: 26rpx;
    font-weight: 600;
    padding: 14rpx 32rpx;
    border-radius: 40rpx;
    white-space: nowrap;
    flex-shrink: 0;
    margin-left: 20rpx;
}

/* ★ 通用 */
.loading { text-align: center; color: #909399; padding: 120rpx 0; font-size: 28rpx; }
.empty { text-align: center; padding: 120rpx 0; }
.empty-text { color: #909399; font-size: 28rpx; }
</style>
