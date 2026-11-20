<template>
    <!-- ★M33 T33.6 活动列表（§7.6，纯列表）
         每项：活动标题 + 主持人 → 人数信息（「8 人已报名 · 5 人待确认」）→ 状态标签（报名中绿/已锁定橙/已结束灰）→ 右箭头提示可点击
         ★不放操作按钮，整行点击进入详情；长按/点「详情」弹出 T33.2 报名详情弹窗 -->
    <view class="page" :class="'theme-' + theme.key" :style="rootStyle">
        <!-- Tab 切换 -->
        <view class="tab-bar">
            <view v-for="tab in tabs" :key="tab.key" class="tab-item" :class="{ active: activeTab === tab.key }" @click="switchTab(tab.key)">
                <text class="tab-text">{{ tab.label }}</text>
                <view v-if="activeTab === tab.key" class="tab-indicator" :style="{ background: theme.primaryColor }"></view>
            </view>
        </view>

        <!-- 纯列表 -->
        <scroll-view class="list-scroll" scroll-y>
            <view v-if="!loaded" class="empty">加载中…</view>
            <view v-else-if="!filteredList.length" class="empty">暂无活动</view>
            <view v-else class="list">
                <view v-for="item in filteredList" :key="item.id || item.bizId" class="row" @click="goDetail(item)">
                    <view class="row-main">
                        <view class="title-line">
                            <text class="title">{{ item.title || '-' }}</text>
                            <text class="pill" :class="'t-' + statusOf(item).type" :style="pillStyle(statusOf(item).type)">{{ statusOf(item).label }}</text>
                        </view>
                        <text v-if="hostText(item)" class="host">主持：{{ hostText(item) }}</text>
                        <text class="count">{{ countText(item) }}</text>
                        <text class="time">{{ timeText(item) }}</text>
                    </view>
                    <view class="row-side">
                        <text class="arrow">›</text>
                        <text class="detail-link" :style="{ color: theme.primaryColor }" @click.stop="openModal(item)">详情</text>
                    </view>
                </view>
            </view>
        </scroll-view>

        <!-- ★T33.2 报名详情弹窗 -->
        <activity-detail-modal v-model="modalVisible" :activity-id="modalActivityId" :title="modalTitle" />

        <micro-tabbar current="/pages/micro/boss/activityList" />
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get } from '../../../utils/request'
import config from '../../../config'
import { isActivityEnded } from '../../../utils/format'
import { getThemeSync, loadTenantTheme, themeCssVars } from '../../../utils/theme'
import MicroTabbar from '../../../components/micro-tabbar/micro-tabbar.vue'
import ActivityDetailModal from '../../../components/activity-detail-modal/activity-detail-modal.vue'

export default {
    components: { MicroTabbar, ActivityDetailModal },
    data() {
        return {
            theme: getThemeSync(),
            loaded: false,
            list: [],
            activeTab: 'ALL',
            tabs: [
                { key: 'ALL', label: '全部' },
                { key: 'ONGOING', label: '进行中' },
                { key: 'LOCKED', label: '已锁定' },
                { key: 'ENDED', label: '已结束' }
            ],
            modalVisible: false,
            modalActivityId: '',
            modalTitle: ''
        }
    },
    computed: {
        rootStyle() { return themeCssVars(this.theme.key) },
        filteredList() {
            if (this.activeTab === 'ALL') return this.list
            return this.list.filter(item => {
                if (this.activeTab === 'ONGOING') return !isActivityEnded(item) && !item.isLocked && item.status !== 'DRAFT'
                if (this.activeTab === 'LOCKED') return !!item.isLocked
                if (this.activeTab === 'ENDED') return isActivityEnded(item) || item.status === 'ENDED'
                return true
            })
        }
    },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        this.theme = getThemeSync()
        loadTenantTheme().then(t => { this.theme = t })
        this.loadList()
    },
    methods: {
        switchTab(key) { this.activeTab = key },
        /** 活动状态文案：报名中绿 / 已锁定橙 / 已结束灰 / 已下架灰 */
        statusOf(a) {
            if (a.status === 'DRAFT') return { label: '已下架', type: 'gray' }
            if (a.status === 'ENDED' || isActivityEnded(a)) return { label: '已结束', type: 'gray' }
            if (a.isLocked) return { label: '已锁定', type: 'warn-solid' }
            return { label: '报名中', type: 'success-soft' }
        },
        pillStyle(type) {
            const primary = this.theme.primaryColor || '#409eff'
            if (type === 'primary-solid') return { background: primary, color: '#fff' }
            if (type === 'primary-soft') return { color: primary, background: primary + '1a' }
            return {}
        },
        hostText(item) {
            if (item.hostNames) return item.hostNames
            const hosts = item.hosts || []
            return hosts.map(h => h.name || h.nickName || '').filter(Boolean).join('、')
        },
        /** 人数信息：「8 人已报名 · 5 人待确认」 */
        countText(item) {
            const signed = item.signedCount != null ? item.signedCount : (item.participantCount || 0)
            const pending = item.pendingCount != null ? item.pendingCount : (item.unconfirmedCount || 0)
            let text = signed + ' 人已报名'
            if (pending > 0) text += ' · ' + pending + ' 人待确认'
            return text
        },
        timeText(item) {
            const s = item.startTime ? String(item.startTime).replace('T', ' ').substring(0, 16) : ''
            return s || ''
        },
        async loadList() {
            this.loaded = false
            try {
                // 优先用 M33 老板端活动列表接口，失败降级到既有 bossActivities
                let res
                try {
                    res = await get(config.api.microBossActivityList)
                } catch (e) {
                    res = await get(config.api.microBossMyActivities)
                }
                this.list = (res && (res.data || res.rows)) || []
            } catch (e) { this.list = [] } finally { this.loaded = true }
        },
        /** 整行点击 → 进入活动详情（操作视图） */
        goDetail(item) {
            const id = item.id || item.bizId
            uni.navigateTo({ url: '/pages/micro/boss/activityDetail?id=' + id })
        },
        /** 「详情」链接 → 弹出报名详情弹窗（T33.2） */
        openModal(item) {
            this.modalActivityId = item.id || item.bizId
            this.modalTitle = item.title || '报名详情'
            this.modalVisible = true
        }
    }
}
</script>

<style scoped>
.page { min-height: 100vh; padding: 0 24rpx 140rpx; background: var(--page-bg, #f5f6f8); display: flex; flex-direction: column; box-sizing: border-box; }
.tab-bar { display: flex; align-items: center; gap: 40rpx; padding: 24rpx 0 16rpx; border-bottom: 1rpx solid var(--border, rgba(58,58,58,.08)); margin-bottom: 16rpx; flex-shrink: 0; }
.tab-item { position: relative; padding: 8rpx 0 14rpx; }
.tab-text { font-size: 28rpx; color: var(--text-sub, #9E9E9E); }
.tab-item.active .tab-text { font-size: 30rpx; font-weight: bold; color: var(--brand, #409eff); }
.tab-indicator { position: absolute; bottom: 0; left: 50%; transform: translateX(-50%); width: 44rpx; height: 6rpx; border-radius: 3rpx; }

.list-scroll { flex: 1; min-height: 0; }
.list { display: flex; flex-direction: column; gap: 16rpx; }
.row { display: flex; align-items: center; background: var(--card-bg, #fff); border-radius: var(--radius-card, 18rpx); padding: 24rpx 28rpx; box-shadow: var(--shadow-card, 0 3px 10px rgba(0,0,0,.05)); }
.row-main { flex: 1; overflow: hidden; }
.title-line { display: flex; align-items: center; gap: 12rpx; margin-bottom: 10rpx; }
.title { font-size: 30rpx; font-weight: bold; color: var(--text, #1a1a1a); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; flex: 1; }
.pill { display: inline-block; font-size: 20rpx; padding: 4rpx 16rpx; border-radius: 8rpx; flex-shrink: 0; }
.t-warn-solid { background: #e6a23c; color: #fff; }
.t-success-soft { background: #e8f8e0; color: #52b647; }
.t-gray { background: var(--brand-weak, #f0f2f5); color: var(--text-sub, #909399); }
.host { display: block; font-size: 24rpx; color: var(--text-sub, #606266); margin-bottom: 6rpx; }
.count { display: block; font-size: 24rpx; color: var(--text-sub, #606266); margin-bottom: 6rpx; }
.time { display: block; font-size: 22rpx; color: var(--text-sub, #909399); }
.row-side { display: flex; flex-direction: column; align-items: center; gap: 12rpx; flex-shrink: 0; margin-left: 16rpx; }
.arrow { font-size: 40rpx; color: #c0c4cc; line-height: 1; }
.detail-link { font-size: 22rpx; }
.empty { text-align: center; color: var(--text-sub, #909399); font-size: 26rpx; padding: 100rpx 0; }
</style>
