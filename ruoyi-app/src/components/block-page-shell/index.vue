<template>
    <view class="page-shell">
        <view v-if="loading" class="shell-loading">
            <text>加载中…</text>
        </view>
        <view v-else-if="!blocks.length" class="shell-empty">
            <text>暂无配置内容</text>
        </view>
        <view v-else class="shell-content">
            <!-- T15.17 + T15.30 第③层最终渲染：
                 后端 /page/config 已做 天花板(block_keys) ∩ page_block_config ∩ perms 三重过滤，
                 此处只需按返回顺序循环渲染即可。 -->
            <block v-for="(blk, idx) in blocks" :key="blk.blockKey + '_' + idx">
                <revenue-week v-if="blk.blockKey === 'revenue_week'" :blockParams="parseParams(blk.blockParams)" />
                <revenue-received v-else-if="blk.blockKey === 'revenue_received'" :blockParams="parseParams(blk.blockParams)" />
                <revenue-pending v-else-if="blk.blockKey === 'revenue_pending'" :blockParams="parseParams(blk.blockParams)" />
                <staff-photo-list v-else-if="blk.blockKey === 'staff_photo_list'" :blockParams="parseParams(blk.blockParams)" />
                <activity-banner v-else-if="blk.blockKey === 'activity_banner'" :blockParams="parseParams(blk.blockParams)" />
                <quick-booking v-else-if="blk.blockKey === 'quick_booking'" :blockParams="parseParams(blk.blockParams)" />
                <my-schedule v-else-if="blk.blockKey === 'my_schedule'" :blockParams="parseParams(blk.blockParams)" />
                <checkin-entry v-else-if="blk.blockKey === 'checkin_entry'" :blockParams="parseParams(blk.blockParams)" />
                <shop-info v-else-if="blk.blockKey === 'shop_info'" :blockParams="parseParams(blk.blockParams)" />
                <notice-bar v-else-if="blk.blockKey === 'notice_bar'" :blockParams="parseParams(blk.blockParams)" />
                <!-- 未注册区块兜底 -->
                <view v-else class="block-fallback">
                    <text>区块 {{ blk.blockKey }} 暂无渲染器</text>
                </view>
            </block>
        </view>
    </view>
</template>

<script>
/**
 * 小程序动态页面壳（T15.17 + T15.30 第③层最终渲染）
 * <p>消费后端 GET /page/config?pageKey=xxx 返回的有序区块清单，
 * 循环渲染对应区块组件。后端已做三层过滤：
 *   ① role_template.block_keys（天花板）
 *   ② page_block_config（排版配置）
 *   ③ 当前角色 perms（权限兜底）
 * 本组件只需按返回顺序逐一渲染即可。
 *
 * 用法：
 *   <block-page-shell pageKey="home" />
 *   <block-page-shell pageKey="mine" :autoLoad="true" />
 *
 * @author ruoyi
 * @date 2026-09-21
 */
import { get } from '../../utils/request'
import config from '../../config'

// T15.16 区块组件库（与 block-registry.js 保持同一份目录，只增不改）
import RevenueWeek from '../blocks/revenue-week.vue'
import RevenueReceived from '../blocks/revenue-received.vue'
import RevenuePending from '../blocks/revenue-pending.vue'
import StaffPhotoList from '../blocks/staff-photo-list.vue'
import ActivityBanner from '../blocks/activity-banner.vue'
import QuickBooking from '../blocks/quick-booking.vue'
import MySchedule from '../blocks/my-schedule.vue'
import CheckinEntry from '../blocks/checkin-entry.vue'
import ShopInfo from '../blocks/shop-info.vue'
import NoticeBar from '../blocks/notice-bar.vue'

export default {
    name: 'BlockPageShell',
    components: {
        RevenueWeek, RevenueReceived, RevenuePending,
        StaffPhotoList, ActivityBanner, QuickBooking,
        MySchedule, CheckinEntry, ShopInfo, NoticeBar
    },
    props: {
        /** 页面标识(home/mine/booking/report) */
        pageKey: { type: String, required: true },
        /** 是否自动加载（默认 true，created 时即请求 /page/config） */
        autoLoad: { type: Boolean, default: true }
    },
    data() {
        return {
            loading: false,
            /** 后端返回的有序区块清单（已做天花板∩配置∩perms 三重过滤） */
            blocks: []
        }
    },
    created() {
        if (this.autoLoad) this.loadConfig()
    },
    watch: {
        pageKey() { if (this.autoLoad) this.loadConfig() }
    },
    methods: {
        /**
         * 调用 /page/config 获取当前角色当前页面的区块配置。
         * 后端 PageBlockConfigServiceImpl#getPageConfig 已做：
         *   1. 按 tenantId + 主角色查 page_block_config
         *   2. role_template.block_keys 天花板过滤
         *   3. block_params.perm ∩ 当前用户 perms 权限过滤
         *   4. 本租户无配置时回落 tenant_id=0 系统默认模板
         */
        async loadConfig() {
            this.loading = true
            try {
                const res = await get(config.api.microPageConfig, { pageKey: this.pageKey })
                this.blocks = (res && res.data) || []
                this.$emit('loaded', this.blocks)
            } catch (e) {
                this.blocks = []
            }
            this.loading = false
        },
        /** 解析 blockParams JSON 字符串为对象 */
        parseParams(raw) {
            if (!raw) return {}
            if (typeof raw === 'object') return raw
            try { return JSON.parse(raw) } catch (e) { return {} }
        },
        /** 外部调用：手动刷新区块配置 */
        refresh() { this.loadConfig() }
    }
}
</script>

<style scoped>
.page-shell { min-height: 200rpx; }
.shell-loading, .shell-empty { text-align: center; color: #909399; padding: 80rpx 0; font-size: 26rpx; }
.shell-content { padding: 0 24rpx; }
.block-fallback { background: #f5f6f8; border-radius: 12rpx; padding: 24rpx; margin-bottom: 20rpx; text-align: center; color: #c0c4cc; font-size: 24rpx; }
</style>
