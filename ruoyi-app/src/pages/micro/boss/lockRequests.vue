<template>
    <!-- ★M33 §九 老板端·锁定申请审批（老板把关：员工申请锁定 → 老板一键审批）
         · 三状态页签：待审批（PENDING）/ 已通过（APPROVED）/ 已驳回（REJECTED）
         · 待审批卡片：活动标题 + 申请人 + 申请时间 + 申请说明 → 通过 / 驳回
         · 通过 → 委派 BE 触发活动真正锁定；驳回 → 可附原因，活动保持未锁定
         · 接口：microBossLockRequestList / Approve / Reject（perms micro:activity:lock） -->
    <view class="page" :class="'theme-' + theme.key" :style="rootStyle">
        <!-- 状态页签 -->
        <view class="tabs">
            <view v-for="t in tabs" :key="t.value" class="tab" :class="{ on: status === t.value }"
                :style="status === t.value ? { color: theme.primaryColor, borderBottomColor: theme.primaryColor } : {}"
                @click="switchTab(t.value)">
                {{ t.label }}
            </view>
        </view>

        <scroll-view class="scroll" scroll-y>
            <view v-if="!loaded" class="empty">加载中…</view>
            <view v-else-if="!list.length" class="empty">{{ emptyText }}</view>
            <template v-else>
                <view v-for="r in list" :key="r.id" class="card">
                    <view class="card-head">
                        <text class="act-title">{{ r.activityTitle || ('活动 #' + r.activityId) }}</text>
                        <text class="badge" :class="'b-' + (r.status || '').toLowerCase()">{{ statusLabel(r.status) }}</text>
                    </view>
                    <view class="meta-row"><text class="meta-label">申请人</text><text class="meta-value">{{ r.applyStaffName || '员工' }}</text></view>
                    <view class="meta-row"><text class="meta-label">申请时间</text><text class="meta-value">{{ timeText(r.createTime) }}</text></view>
                    <view v-if="r.reason" class="meta-row"><text class="meta-label">申请说明</text><text class="meta-value">{{ r.reason }}</text></view>
                    <!-- 已处理：展示审批人与结果 -->
                    <template v-if="r.status !== 'PENDING'">
                        <view class="meta-row"><text class="meta-label">审批人</text><text class="meta-value">{{ r.approveByName || '-' }}</text></view>
                        <view class="meta-row"><text class="meta-label">审批时间</text><text class="meta-value">{{ timeText(r.approveTime) }}</text></view>
                        <view v-if="r.status === 'REJECTED' && r.rejectReason" class="meta-row">
                            <text class="meta-label">驳回原因</text><text class="meta-value reject">{{ r.rejectReason }}</text>
                        </view>
                    </template>
                    <!-- 待审批：通过 / 驳回 -->
                    <view v-if="r.status === 'PENDING'" class="btn-row">
                        <button class="btn-soft reject-btn" @click="doReject(r)">驳回</button>
                        <button class="btn-soft approve-btn" :style="{ background: theme.primaryColor }" @click="doApprove(r)">通过并锁定</button>
                    </view>
                </view>
            </template>
        </scroll-view>
    </view>
</template>

<script>
import { getToken } from '../../../utils/auth'
import { get, post } from '../../../utils/request'
import config from '../../../config'
import { getThemeSync, loadTenantTheme, themeCssVars } from '../../../utils/theme'

export default {
    data() {
        return {
            theme: getThemeSync(),
            loaded: false,
            status: 'PENDING',
            list: [],
            tabs: [
                { label: '待审批', value: 'PENDING' },
                { label: '已通过', value: 'APPROVED' },
                { label: '已驳回', value: 'REJECTED' }
            ]
        }
    },
    computed: {
        rootStyle() { return themeCssVars(this.theme.key) },
        emptyText() {
            if (this.status === 'PENDING') return '🎉 暂无待审批的锁定申请'
            return '暂无记录'
        }
    },
    onShow() {
        if (!getToken()) { uni.reLaunch({ url: '/pages/login/index' }); return }
        this.theme = getThemeSync()
        loadTenantTheme().then(t => { this.theme = t })
        this.load()
    },
    methods: {
        statusLabel(s) {
            if (s === 'APPROVED') return '已通过'
            if (s === 'REJECTED') return '已驳回'
            return '待审批'
        },
        timeText(t) {
            return t ? String(t).replace('T', ' ').substring(0, 16) : '-'
        },
        switchTab(v) {
            if (this.status === v) return
            this.status = v
            this.load()
        },
        async load() {
            this.loaded = false
            try {
                const r = await get(config.api.microBossLockRequestList, { status: this.status })
                this.list = (r && r.data) || []
            } catch (e) {
                this.list = []
            } finally {
                this.loaded = true
            }
        },
        /** 审批通过：委派 BE 触发活动真正锁定 */
        doApprove(r) {
            uni.showModal({
                title: '通过锁定申请',
                content: '通过后「' + (r.activityTitle || '该活动') + '」将立即锁定，名单与座位变只读。确定通过吗？',
                success: async (res) => {
                    if (!res.confirm) return
                    try {
                        await post(config.api.microBossLockRequestApprove + r.id)
                        uni.showToast({ title: '已通过并锁定', icon: 'success' })
                        this.load()
                    } catch (e) { /* 请求层统一提示 */ }
                }
            })
        },
        /** 驳回：可附驳回原因，活动保持未锁定 */
        doReject(r) {
            uni.showModal({
                title: '驳回锁定申请',
                editable: true,
                placeholderText: '请输入驳回原因（可选）',
                success: async (res) => {
                    if (!res.confirm) return
                    try {
                        await post(config.api.microBossLockRequestReject + r.id, { reason: res.content || '' })
                        uni.showToast({ title: '已驳回', icon: 'success' })
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
.tabs { flex-shrink: 0; display: flex; background: var(--card-bg, #fff); border-bottom: 1rpx solid var(--border, #ebeef5); }
.tab { flex: 1; text-align: center; font-size: 28rpx; color: var(--text-sub, #909399); padding: 24rpx 0; border-bottom: 4rpx solid transparent; }
.tab.on { font-weight: 600; }
.scroll { flex: 1; min-height: 0; padding: 24rpx 24rpx 0; box-sizing: border-box; }
.empty { text-align: center; color: var(--text-sub, #909399); font-size: 26rpx; padding: 120rpx 0; }

.card { background: var(--card-bg, #fff); border-radius: var(--radius-card, 20rpx); padding: 28rpx 32rpx; margin-bottom: 20rpx; box-shadow: var(--shadow-card, 0 2rpx 12rpx rgba(0,0,0,.04)); }
.card-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 16rpx; }
.act-title { flex: 1; font-size: 30rpx; font-weight: bold; color: var(--text, #1a1a1a); overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.badge { flex-shrink: 0; margin-left: 12rpx; font-size: 22rpx; padding: 4rpx 16rpx; border-radius: 10rpx; }
.badge.b-pending { background: #fdf6ec; color: #e6a23c; }
.badge.b-approved { background: #f0f9eb; color: #67c23a; }
.badge.b-rejected { background: #fef0f0; color: #f56c6c; }

.meta-row { display: flex; align-items: flex-start; padding: 6rpx 0; }
.meta-label { width: 140rpx; flex-shrink: 0; font-size: 26rpx; color: var(--text-sub, #909399); }
.meta-value { flex: 1; font-size: 26rpx; color: var(--text, #303133); }
.meta-value.reject { color: #f56c6c; }

.btn-row { display: flex; gap: 16rpx; margin-top: 20rpx; }
.btn-soft { flex: 1; height: 76rpx; line-height: 76rpx; font-size: 28rpx; border-radius: 38rpx; border: none; margin: 0; }
.btn-soft::after { border: none; }
.btn-soft.reject-btn { background: #fef0f0; color: #f56c6c; border: 1rpx solid #fbc4c4; }
.btn-soft.approve-btn { color: #fff; }
</style>
