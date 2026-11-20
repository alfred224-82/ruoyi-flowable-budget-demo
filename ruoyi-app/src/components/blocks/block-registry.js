/**
 * 小程序区块组件注册表（T15.16 区块组件库）
 * <p>与 PC 端 pageDecorate/index.vue 的 blockCatalog 一一对应，只增不改。
 * <p>三层控制模型（T15.30）：
 *   第①层 role_template.block_keys 定天花板 → 第②层 page_block_config 定排版 → 第③层本注册表渲染。
 * <p>新增区块只需：① 在此注册 key→组件映射 ② 创建对应 .vue 组件 ③ PC blockCatalog 同步加条目。
 *
 * @author ruoyi
 * @date 2026-09-21
 */

// 直接导入全部区块组件（uni-app 不支持 require.context，须逐个声明）
import RevenueWeek from './revenue-week.vue'
import RevenueReceived from './revenue-received.vue'
import RevenuePending from './revenue-pending.vue'
import StaffPhotoList from './staff-photo-list.vue'
import ActivityBanner from './activity-banner.vue'
import QuickBooking from './quick-booking.vue'
import MySchedule from './my-schedule.vue'
import CheckinEntry from './checkin-entry.vue'
import ShopInfo from './shop-info.vue'
import NoticeBar from './notice-bar.vue'

/**
 * blockKey → 组件映射表（只增不改）
 * <p>key 与 page_block_config.block_key 一致，组件为小程序 .vue 文件。
 */
const BLOCK_REGISTRY = {
    revenue_week: RevenueWeek,
    revenue_received: RevenueReceived,
    revenue_pending: RevenuePending,
    staff_photo_list: StaffPhotoList,
    activity_banner: ActivityBanner,
    quick_booking: QuickBooking,
    my_schedule: MySchedule,
    checkin_entry: CheckinEntry,
    shop_info: ShopInfo,
    notice_bar: NoticeBar
}

/**
 * 按 blockKey 查找组件（找不到返回 null，上层兜底渲染占位）
 */
export function resolveBlock(blockKey) {
    return BLOCK_REGISTRY[blockKey] || null
}

/**
 * 获取全部已注册 blockKey 列表（调试/验收用）
 */
export function listRegisteredBlocks() {
    return Object.keys(BLOCK_REGISTRY)
}

export default BLOCK_REGISTRY
