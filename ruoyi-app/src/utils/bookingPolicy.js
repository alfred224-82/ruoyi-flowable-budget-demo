/**
 * 预约策略工具（T18.10 策略开关顾客端联动）
 * ★顾客自助预约开关（booking_policy.customer_self_booking）由租户管理员在 PC「策略开关」维护；
 *   顾客端首页/自助预约页据此隐藏入口（第一层防线），后端提交接口复核为最终防线。
 */
import { get } from './request'
import config from '../config'

/**
 * 查询当前租户是否开启顾客自助预约
 * @returns {Promise<boolean>} fail-soft：接口异常视为未开启（入口隐藏，后端提交仍兜底拒绝）
 */
export function isSelfBookingEnabled() {
    return get(config.api.microBookingPolicyCustomerView)
        .then(res => !!(res && res.data && res.data.customerSelfBooking === 1))
        .catch(() => false)
}

/**
 * ★REQ-01 查询顾客端预约策略完整视图（含三类资源开关 + 行业编码）
 * <p>enabled=是否对客户开放（0 隐藏对应资源入口）；required=是否必填（1 提交前强制校验）。
 * <p>fail-soft：接口异常时回退为「全部开放、全部非必填」，后端提交接口仍为最终防线。
 * @returns {Promise<Object>} { customerSelfBooking, projectBookingEnabled/Required, roomBookingEnabled/Required, staffBookingEnabled/Required, industryCode }
 */
export function getBookingResourcePolicy() {
    return get(config.api.microBookingPolicyCustomerView)
        .then(res => {
            const d = (res && res.data) || {}
            return {
                customerSelfBooking: d.customerSelfBooking === 1,
                projectBookingEnabled: d.projectBookingEnabled === undefined ? 1 : d.projectBookingEnabled,
                projectBookingRequired: d.projectBookingRequired === undefined ? 0 : d.projectBookingRequired,
                roomBookingEnabled: d.roomBookingEnabled === undefined ? 1 : d.roomBookingEnabled,
                roomBookingRequired: d.roomBookingRequired === undefined ? 0 : d.roomBookingRequired,
                staffBookingEnabled: d.staffBookingEnabled === undefined ? 1 : d.staffBookingEnabled,
                staffBookingRequired: d.staffBookingRequired === undefined ? 0 : d.staffBookingRequired,
                industryCode: d.industryCode || null
            }
        })
        .catch(() => ({
            customerSelfBooking: false,
            projectBookingEnabled: 1, projectBookingRequired: 0,
            roomBookingEnabled: 1, roomBookingRequired: 0,
            staffBookingEnabled: 1, staffBookingRequired: 0,
            industryCode: null
        }))
}
