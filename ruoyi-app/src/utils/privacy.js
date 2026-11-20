/**
 * 隐私分级可见性工具（T31.5，设计方案 §四 / §八之二）
 * ------------------------------------------------------------------
 * 前端只做「展示层过滤」——最终数据可见性以后端 PrivacyVisibilityUtil 为准；
 * 本模块用于：
 *   1. 列表/名片渲染时决定是否显示「📞 打电话」/「💬 加微信」入口；
 *   2. 顾客端「联系我们」页拉取门店 L1 公开信息；
 *   3. 脱敏展示（如姓名「李*明」、手机「138****4521」）——服务端已脱敏的字段
 *      不再二次处理，本模块仅补齐前端本地渲染场景。
 *
 * 分级说明（与 BE `PrivacyLevel` 枚举一致）：
 *   L1_STORE           门店信息（名称/地址/电话/客服微信）——登录即可见
 *   L2_BUSINESS        业务信息（活动/订单/结算/收入）    ——按角色与租户可见
 *   L3_PERSONAL_CONTACT 个人联系方式（员工/顾客手机号/微信号）——按开关与角色可见
 *   L4_CUSTOMER_INFO   顾客个人信息（姓名/头像/手机号）    ——本人 + 老板 + 服务员工可见
 *
 * 核心规则（§四 三原则）：
 *   ① 顾客之间互相不可见（顾客 A 看顾客 B → 电话/姓名全隐藏）
 *   ② 不给自己打电话（当前登录者本人行 → 跳过电话图标）
 *   ③ 员工联系方式按开关（`staff_contact_public.show_phone/show_wechat`）
 */
import { getPrimaryRole, ROLES } from './roles'

/** 隐私级别常量（与 BE `PrivacyLevel` 枚举 code 对齐） */
export const PRIVACY_LEVEL = {
    L1_STORE: 'L1',
    L2_BUSINESS: 'L2',
    L3_PERSONAL_CONTACT: 'L3',
    L4_CUSTOMER_INFO: 'L4'
}

/** 可见性枚举（与 BE `PrivacyVisibilityUtil.Visibility` 对齐） */
export const VISIBILITY = {
    VISIBLE: 'VISIBLE',
    HIDDEN: 'HIDDEN',
    DESENSITIZED: 'DESENSITIZED'
}

/**
 * 判断电话图标是否可见（§四 三原则 + §八之二 员工开关）
 *
 * @param {Object} opts
 * @param {String} opts.currentRole   当前登录人角色（boss/salesman/customer），缺省取 getPrimaryRole()
 * @param {String} opts.targetRole    目标对象角色
 * @param {Boolean} opts.isSelf       是否本人（本人行一律不显示电话图标）
 * @param {Boolean} opts.staffShowPhone 员工联系方式开关（仅 target=salesman 时生效）
 * @returns {Boolean} true=显示电话图标
 */
export function getPhoneIconVisibility(opts = {}) {
    const currentRole = opts.currentRole || getPrimaryRole()
    const targetRole = opts.targetRole
    const isSelf = !!opts.isSelf

    // 规则②：本人行不显示电话图标（不给自己打电话）
    if (isSelf) return false

    // 老板视角：对全部员工/顾客可见（用于店内管理与回访）
    if (currentRole === ROLES.BOSS) return true

    // 员工（salesman）视角：对顾客可见（服务对接需要）
    if (currentRole === ROLES.SALESMAN && targetRole === ROLES.CUSTOMER) return true

    // 顾客视角：看员工按开关；看其他顾客一律隐藏
    if (currentRole === ROLES.CUSTOMER) {
        if (targetRole === ROLES.SALESMAN) return !!opts.staffShowPhone
        if (targetRole === ROLES.CUSTOMER) return false
        // 顾客看老板：门店公开电话（L1），可见
        if (targetRole === ROLES.BOSS) return true
    }

    return false
}

/**
 * 判断微信图标是否可见（规则与电话一致，仅开关字段换成 show_wechat）
 */
export function getWechatIconVisibility(opts = {}) {
    const currentRole = opts.currentRole || getPrimaryRole()
    const targetRole = opts.targetRole
    const isSelf = !!opts.isSelf

    if (isSelf) return false
    if (currentRole === ROLES.BOSS) return true
    if (currentRole === ROLES.SALESMAN && targetRole === ROLES.CUSTOMER) return true
    if (currentRole === ROLES.CUSTOMER) {
        if (targetRole === ROLES.SALESMAN) return !!opts.staffShowWechat
        if (targetRole === ROLES.CUSTOMER) return false
        if (targetRole === ROLES.BOSS) return true
    }
    return false
}

/**
 * 判断姓名/头像是否可见（L4 顾客个人信息）
 * <p>顾客之间互相不可见；老板/服务员工对顾客可见；员工之间互相可见（同事）。
 */
export function getNameVisibility(opts = {}) {
    const currentRole = opts.currentRole || getPrimaryRole()
    const targetRole = opts.targetRole
    const isSelf = !!opts.isSelf

    if (isSelf) return VISIBILITY.VISIBLE
    if (currentRole === ROLES.BOSS) return VISIBILITY.VISIBLE
    if (currentRole === ROLES.SALESMAN) {
        // 员工看顾客：可见（服务对接）
        if (targetRole === ROLES.CUSTOMER) return VISIBILITY.VISIBLE
        // 员工看员工：可见（同事协作）
        if (targetRole === ROLES.SALESMAN) return VISIBILITY.VISIBLE
        return VISIBILITY.VISIBLE
    }
    if (currentRole === ROLES.CUSTOMER) {
        // 顾客看顾客：一律脱敏（规则①）
        if (targetRole === ROLES.CUSTOMER) return VISIBILITY.DESENSITIZED
        // 顾客看员工/老板：可见
        return VISIBILITY.VISIBLE
    }
    return VISIBILITY.HIDDEN
}

/**
 * 前端本地脱敏（服务端已脱敏字段无需再调用）
 * <p>手机号：138****4521（前三后四）
 * <p>姓名：李*明 / 欧阳**（复姓保留两字）
 */
export function desensitizePhone(phone) {
    if (!phone) return ''
    const s = String(phone)
    if (s.length < 7) return s.replace(/./g, '*')
    return s.slice(0, 3) + '****' + s.slice(-4)
}

const COMPOUND_SURNAMES = [
    '欧阳', '司马', '上官', '诸葛', '东方', '独孤', '南宫', '万俟',
    '闻人', '夏侯', '皇甫', '尉迟', '公羊', '赫连', '澹台', '轩辕'
]

export function desensitizeName(name) {
    if (!name) return ''
    const s = String(name)
    if (s.length <= 1) return s
    // 复姓优先
    for (const cs of COMPOUND_SURNAMES) {
        if (s.startsWith(cs)) {
            if (s.length === 2) return cs
            return cs + '*'.repeat(s.length - cs.length - 1) + s.slice(-1)
        }
    }
    if (s.length === 2) return s.slice(0, 1) + '*'
    return s.slice(0, 1) + '*'.repeat(s.length - 2) + s.slice(-1)
}

/**
 * 过滤联系人列表：只保留当前登录人可见的项，并按规则处理脱敏。
 * <p>典型用法：老板端「员工列表」/ 顾客端「我的活动 · 同场顾客」。
 *
 * @param {Array} list 原始联系人列表，每项需含 { userId, role, name, phone, wechat, staffShowPhone, staffShowWechat }
 * @returns {Array} 过滤后的列表，字段已按可见性处理
 */
export function getVisibleContacts(list) {
    if (!Array.isArray(list)) return []
    const currentRole = getPrimaryRole()
    const currentUserId = getCurrentUserId()
    return list.map(item => {
        const isSelf = item.userId != null && item.userId === currentUserId
        const phoneVisible = getPhoneIconVisibility({
            currentRole,
            targetRole: item.role,
            isSelf,
            staffShowPhone: item.staffShowPhone
        })
        const wechatVisible = getWechatIconVisibility({
            currentRole,
            targetRole: item.role,
            isSelf,
            staffShowWechat: item.staffShowWechat
        })
        const nameVis = getNameVisibility({
            currentRole,
            targetRole: item.role,
            isSelf
        })
        return {
            ...item,
            // 姓名按可见性处理
            displayName: nameVis === VISIBILITY.VISIBLE
                ? item.name
                : (nameVis === VISIBILITY.DESENSITIZED ? desensitizeName(item.name) : ''),
            // 电话/微信按开关与规则处理
            displayPhone: phoneVisible ? item.phone : '',
            displayWechat: wechatVisible ? item.wechat : '',
            // 图标可见性（供模板层直接绑定 v-if）
            showPhoneIcon: phoneVisible && !!item.phone,
            showWechatIcon: wechatVisible && !!item.wechat
        }
    })
}

/** 从本地缓存获取当前登录人 userId（uni-app 端） */
function getCurrentUserId() {
    try {
        const info = uni.getStorageSync('app_user_info') || {}
        return info.userId || null
    } catch (e) {
        return null
    }
}
