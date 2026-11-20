<template>
    <!-- ★M33 T33.2 报名详情弹窗（§7.2）
         · 从活动列表点开 → 弹窗展示已报名人数（只计顾客，主持不计入）
         · 头像列表（主持带「主持」标签）
         · 底部「关闭」收起
         · 点击「查看完整名单」跳转 T33.8 报名名单页 -->
    <view v-if="visible" class="adm-mask" @click="close">
        <view class="adm-panel" @click.stop>
            <view class="adm-head">
                <text class="adm-title">{{ title || '报名详情' }}</text>
                <text class="adm-close" @click="close">×</text>
            </view>
            <scroll-view class="adm-body" scroll-y>
                <view v-if="loading" class="adm-loading">加载中…</view>
                <template v-else>
                    <!-- 已报名人数（只计顾客，主持不计入） -->
                    <view class="adm-count">
                        <text class="adm-count-num">{{ customerCount }}</text>
                        <text class="adm-count-label">人已报名</text>
                        <text v-if="quota" class="adm-count-quota">/ 名额 {{ quota }}</text>
                    </view>
                    <text class="adm-note">主持不计入名额</text>

                    <!-- 头像列表（主持带「主持」标签） -->
                    <view v-if="allRows.length" class="adm-grid">
                        <view v-for="(r, i) in allRows" :key="i" class="adm-cell">
                            <view class="adm-avatar-wrap">
                                <image v-if="r.avatar" class="adm-avatar" :class="{ 'is-host': r.isHost }" :src="r.avatar" mode="aspectFill" @error="r.avatar = ''" />
                                <view v-else class="adm-avatar adm-avatar-none" :class="{ 'is-host': r.isHost }">
                                    <text>{{ (r.name || '?').charAt(0) }}</text>
                                </view>
                                <text v-if="r.isHost" class="adm-tag-host">主持</text>
                                <text v-else-if="r.isWaitlist" class="adm-tag-wait">候补</text>
                            </view>
                            <text class="adm-name">{{ r.name || '-' }}</text>
                        </view>
                        <!-- 空位占位（剩余名额，最多展示 10 个） -->
                        <view v-for="n in emptyCount" :key="'e' + n" class="adm-cell">
                            <view class="adm-avatar adm-avatar-empty"></view>
                            <text class="adm-name adm-name-empty">空位</text>
                        </view>
                    </view>
                    <view v-else class="adm-empty">暂无顾客报名</view>

                    <!-- 成团提示 -->
                    <text v-if="gapText" class="adm-gap">{{ gapText }}</text>
                </template>
            </scroll-view>
            <view class="adm-foot">
                <button v-if="showRosterEntry && activityId" class="adm-btn secondary" @click="goRoster">查看完整名单</button>
                <button class="adm-btn primary" @click="close">关闭</button>
            </view>
        </view>
    </view>
</template>

<script>
import { get } from '../../utils/request'
import config from '../../config'
import { toHttps } from '../../utils/format'

export default {
    name: 'ActivityDetailModal',
    props: {
        // 是否显示
        value: { type: Boolean, default: false },
        // 活动 ID（传入后自动拉取报名详情）
        activityId: { type: [String, Number], default: '' },
        // 活动标题（可选，未传时取后端返回值）
        title: { type: String, default: '' },
        // 直接注入数据（外部已加载，跳过接口调用）：{ hosts, members, quota, signedCount, waitlistCount, remaining }
        data: { type: Object, default: null },
        // 是否显示「查看完整名单」入口
        showRosterEntry: { type: Boolean, default: true }
    },
    data() {
        return {
            innerVisible: this.value,
            loading: false,
            sd: {}
        }
    },
    computed: {
        visible: {
            get() { return this.innerVisible },
            set(v) { this.innerVisible = v; this.$emit('input', v); this.$emit('update:value', v) }
        },
        /** 数据源：外部注入优先，否则用接口返回 */
        source() { return this.data || this.sd || {} },
        hosts() { return this.source.hosts || [] },
        members() { return this.source.members || [] },
        quota() { return this.source.quota || this.source.maxParticipants || 0 },
        /** 顾客数（只计顾客，主持不计入） */
        customerCount() {
            const c = this.source.signedCount != null ? this.source.signedCount : this.members.filter(m => !m.isWaitlist).length
            return c || 0
        },
        /** 头像列表：主持在前（带「主持」标签）+ 顾客 */
        allRows() {
            const hosts = (this.hosts || []).map(h => ({
                name: h.name || h.nickName || '',
                avatar: toHttps(h.avatar || ''),
                isHost: true,
                isWaitlist: false
            }))
            const members = (this.members || []).map(m => ({
                name: m.name || m.nickName || '',
                avatar: toHttps(m.avatar || ''),
                isHost: false,
                isWaitlist: !!m.isWaitlist
            }))
            return hosts.concat(members)
        },
        /** 空位占位数（剩余名额最多展示 10 个，与原 boss 详情页口径一致） */
        emptyCount() {
            const remaining = this.source.remaining != null
                ? this.source.remaining
                : Math.max((this.quota || 0) - this.customerCount, 0)
            return Math.min(remaining || 0, 10)
        },
        gapText() {
            const remaining = this.source.remaining
            return remaining > 0 ? '还差 ' + remaining + ' 人成团' : ''
        }
    },
    watch: {
        value(v) {
            this.innerVisible = v
            if (v) this.ensureData()
        },
        activityId() { if (this.innerVisible) this.ensureData() }
    },
    mounted() { if (this.innerVisible) this.ensureData() },
    methods: {
        close() { this.visible = false; this.$emit('close') },
        /** 确保数据已加载（外部注入 data 时跳过） */
        ensureData() {
            if (this.data) return
            if (!this.activityId) return
            this.loadData()
        },
        async loadData() {
            this.loading = true
            try {
                const res = await get(config.api.microActivitySignupDetail, { activityId: this.activityId })
                this.sd = (res && res.data) || {}
            } catch (e) { this.sd = {} } finally { this.loading = false }
        },
        goRoster() {
            this.close()
            uni.navigateTo({ url: '/pages/micro/boss/roster?activityId=' + this.activityId })
        }
    }
}
</script>

<style scoped>
.adm-mask { position: fixed; inset: 0; background: rgba(0, 0, 0, 0.5); z-index: 9998; display: flex; align-items: flex-end; justify-content: center; }
.adm-panel { width: 100%; max-width: 720rpx; max-height: 82vh; background: #fff; border-radius: 32rpx 32rpx 0 0; display: flex; flex-direction: column; overflow: hidden; animation: adm-up 0.24s ease-out; }
@keyframes adm-up { from { transform: translateY(100%); } to { transform: translateY(0); } }

.adm-head { display: flex; justify-content: space-between; align-items: center; padding: 28rpx 32rpx; border-bottom: 1rpx solid #f0f2f5; flex-shrink: 0; }
.adm-title { font-size: 32rpx; font-weight: bold; color: #1a1a1a; }
.adm-close { font-size: 44rpx; color: #909399; line-height: 1; padding: 0 8rpx; }

.adm-body { flex: 1; min-height: 0; padding: 24rpx 32rpx; }
.adm-loading, .adm-empty { text-align: center; color: #909399; font-size: 26rpx; padding: 60rpx 0; }

.adm-count { display: flex; align-items: baseline; justify-content: center; gap: 8rpx; padding: 16rpx 0 8rpx; }
.adm-count-num { font-size: 60rpx; font-weight: bold; color: var(--brand, #409eff); line-height: 1; }
.adm-count-label { font-size: 28rpx; color: #303133; }
.adm-count-quota { font-size: 24rpx; color: #909399; margin-left: 12rpx; }
.adm-note { display: block; text-align: center; font-size: 22rpx; color: #909399; margin-bottom: 20rpx; }

.adm-grid { display: flex; flex-wrap: wrap; gap: 24rpx; justify-content: flex-start; }
.adm-cell { width: 110rpx; display: flex; flex-direction: column; align-items: center; }
.adm-avatar-wrap { position: relative; width: 96rpx; height: 96rpx; }
.adm-avatar { width: 96rpx; height: 96rpx; border-radius: 50%; background: #f0f2f5; }
.adm-avatar.is-host { border: 4rpx solid #e6a23c; box-sizing: border-box; }
.adm-avatar-none { display: flex; align-items: center; justify-content: center; font-size: 32rpx; color: #909399; }
.adm-avatar-empty { background: transparent; border: 2rpx dashed #c0c4cc; box-sizing: border-box; }
.adm-tag-host { position: absolute; bottom: -6rpx; left: 50%; transform: translateX(-50%); background: #e6a23c; color: #fff; font-size: 18rpx; padding: 2rpx 10rpx; border-radius: 8rpx; white-space: nowrap; }
.adm-tag-wait { position: absolute; bottom: -6rpx; left: 50%; transform: translateX(-50%); background: #909399; color: #fff; font-size: 18rpx; padding: 2rpx 10rpx; border-radius: 8rpx; white-space: nowrap; }
.adm-name { font-size: 22rpx; color: #606266; margin-top: 12rpx; max-width: 110rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; text-align: center; }
.adm-name-empty { color: #c0c4cc; }

.adm-gap { display: block; text-align: center; font-size: 26rpx; color: var(--brand, #409eff); margin-top: 24rpx; }

.adm-foot { display: flex; gap: 16rpx; padding: 20rpx 32rpx calc(env(safe-area-inset-bottom) + 20rpx); border-top: 1rpx solid #f0f2f5; flex-shrink: 0; }
.adm-btn { flex: 1; height: 84rpx; line-height: 84rpx; font-size: 28rpx; border-radius: 44rpx; border: none; }
.adm-btn::after { border: none; }
.adm-btn.primary { background: var(--brand, #409eff); color: #fff; }
.adm-btn.secondary { background: #f0f2f5; color: #606266; }
</style>
