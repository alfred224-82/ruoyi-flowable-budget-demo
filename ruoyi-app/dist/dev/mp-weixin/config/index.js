"use strict";
const config = {
  // 后端接口地址（本地开发环境，使用相对路径走 vite 代理）
  baseUrl: "/prod-api",
  // baseUrl: 'http://localhost:8080/prod-api',
  // baseUrl: 'https://xiaozhangben.com.cn/prod-api',
  api: {
    // 登录/登出/用户信息（RuoYi 框架：/login 返回 data.token；/getInfo 返回 roles/permissions/features/edition）
    login: "/login",
    // 微信小程序一键登录（T13.1 code2session；wx.login code + 选定门店 tenantId）
    xcxLogin: "/xcxLogin",
    logout: "/logout",
    userInfo: "/getInfo",
    // 手机号统一登录（T15.8）：多租户时返回 needSelectTenant + tenants 候选，前端弹「我的店铺」选择页
    microLogin: "/micro/login",
    // 我的店铺（多租户切换，T15.36§6.2）：登录态查当前用户可切换的租户列表
    microMyTenants: "/micro/tenant/myTenants",
    // ========== M29 手机端登录流程增强（T29.2/T29.3，匿名端点，登录前调用） ==========
    // Boss 手动输租户号 → 按租户号查门店列表（校验存在/未停用/未过期）
    microTenantStores: "/micro/tenant/stores",
    // 租户号合法性校验（存在/未停用/未过期）
    microTenantResolveNo: "/micro/tenant/resolveNo",
    // 员工/顾客扫码 scene 解析 + 校验（t=tenantId&s=storeId&r=role）
    microLoginResolveScene: "/micro/login/resolveScene",
    // 版本能力开关下发（防串第③层：前端动态渲染，T4.3/T8.5）
    editionFeatures: "/system/edition/features",
    // 字典数据（物资小类 fee_item_category，登录即可）
    dictData: "/system/dict/data/type",
    // 文件上传（补票发票图片，需 system:oss:upload 权限）
    upload: "/system/oss/upload",
    // ========== M8 引导态门户（/portal/**，安全白名单，无需登录，T8.10/T13.1） ==========
    // 门店搜索/选择首页：关键字 + 城市 + 用户经纬度就近排序，仅返回公开展示字段
    portalStores: "/portal/stores",
    // 按 openid 反查已绑定门店列表（公众号/小程序授权后可用）
    portalTenants: "/portal/tenants",
    // 公众号总入口：按公众号 AppID 解析归属主体
    portalResolve: "/portal/tenant/resolve",
    // 企微自建应用网页授权链接 / OAuth 回调换取成员身份
    portalWecomAuthUrl: "/portal/wecom/authUrl",
    portalWecomUserInfo: "/portal/wecom/userInfo",
    // 引导态绑定：openid→门店（用户选定门店后落库，登录后由 /login 回填 userId）
    portalBind: "/portal/tenant/bind",
    // ========== 老板看板（/fee/dashboard，与 PC 3215 同源） ==========
    dashboard: "/fee/dashboard",
    dashboardProjects: "/fee/dashboard/projects",
    dashboardDrillDown: "/fee/dashboard/drillDown",
    // ========== 项目管理（/fee/project，与 PC 3201 同源） ==========
    projectList: "/fee/project/list",
    projectActiveList: "/fee/project/activeList",
    projectApprove: "/fee/project/approve",
    projectReject: "/fee/project/reject",
    projectProfit: "/fee/project/profit",
    projectSubjects: "/fee/project/subjects",
    projectMembers: "/fee/project/members",
    // ========== 收入确认（/fee/income，与 PC 3202/3210 同源） ==========
    incomeList: "/fee/income/list",
    incomeAdd: "/fee/income",
    // ========== 报销单（/fee/expense，与 PC 3004 同源；审批为 POST） ==========
    expenseList: "/fee/expense/list",
    expenseAdd: "/fee/expense",
    expensePending: "/fee/expense/pendingApproval",
    expenseApprove: "/fee/expense/approve",
    expenseReject: "/fee/expense/reject",
    // ========== 付款单 / 合同变更（审批为 PUT） ==========
    paymentList: "/fee/payment/list",
    paymentApprove: "/fee/payment/approve",
    paymentReject: "/fee/payment/reject",
    changeList: "/fee/change/list",
    changeApprove: "/fee/change/approve",
    changeReject: "/fee/change/reject",
    // ========== 补票记录（/fee/invoiceSupplement，T5.8 后补通道） ==========
    supplementList: "/fee/invoiceSupplement/list",
    supplementCreate: "/fee/invoiceSupplement",
    supplementSubmit: "/fee/invoiceSupplement/supplement",
    // ========== 项目关联规则（报销表单硬/软规则，登录即可） ==========
    linkRuleMatch: "/fee/linkRule/match",
    // ========== 用途标签（登录即可） ==========
    purposeTagAll: "/fee/purposeTag/allEnabled",
    // ========== 消息中心（3114） ==========
    messageList: "/system/message/list",
    messageUnread: "/system/message/unreadCount",
    messageRead: "/system/message/read",
    messageReadAll: "/system/message/readAll",
    // ========== 高级/专业版增量（T8.5，后端未就绪时前端优雅降级） ==========
    creditMine: "/fee/credit/mine",
    orgMyList: "/fee/org/myList",
    // ========== 小微版（/micro/**，T15.15~T15.20 小程序端） ==========
    // 店铺配置
    microShopConfig: "/micro/shop/config",
    // 预约管理（老板）
    microBookingList: "/micro/booking/list",
    microBookingAdd: "/micro/booking",
    microBookingCancel: "/micro/booking/cancel",
    microBookingComplete: "/micro/booking/complete",
    microBookingReceive: "/micro/booking/receive",
    // 业务员
    microStaffSchedule: "/micro/staff/schedule",
    microStaffIncome: "/micro/staff/income",
    microStaffIncomeWeek: "/micro/staff/income/week",
    microStaffIncomeMonth: "/micro/staff/income/month",
    microStaffIncomeDetail: "/micro/staff/income/detail",
    microStaffCheckin: "/micro/staff/checkin",
    microStaffCheckinSubmit: "/micro/staff/checkinSubmit",
    // 顾客
    microCustomerShop: "/micro/customer/shop",
    // ★V3.7 发现页数据源（项目分类/房间/助教）
    microCustomerCategories: "/micro/customer/categories",
    microCustomerRooms: "/micro/customer/rooms",
    microCustomerStaff: "/micro/customer/staff",
    microCustomerSubmitBook: "/micro/customer/submitBook",
    microCustomerMyBook: "/micro/customer/myBook",
    microCustomerMyBookDetail: "/micro/customer/myBook/",
    microCustomerCancelBook: "/micro/customer/cancelBook",
    microCustomerPayInfo: "/micro/customer/payInfo",
    // 顾客自查信誉状态（T15.32 防爽约按钮置灰）
    microCustomerMyCredit: "/micro/customer/myCredit",
    // ★M22 顾客我的活动（合并自助预约+活动报名）
    microCustomerMyActivities: "/micro/customer/myActivities",
    // ★Boss 我的活动（本店全部活动）
    microBossMyActivities: "/micro/activity/bossActivities",
    // ★M22 助教我的活动（合并服务预约+被选中活动）
    microStaffMyActivities: "/micro/staff/myActivities",
    // 报表
    microReportToday: "/micro/report/today",
    microReportWeekly: "/micro/report/weekly",
    microReportMonthly: "/micro/report/monthly",
    microReportProfit: "/micro/report/profit",
    microReportCashFlow: "/micro/report/cashFlow",
    microReportTrend: "/micro/report/trend",
    // 服务
    microServiceOnSale: "/micro/service/onSale",
    microServiceChangePrice: "/micro/service/changePrice",
    // 消息
    microNotifyList: "/micro/notify/list",
    microNotifyUnread: "/micro/notify/unreadCount",
    microNotifyRead: "/micro/notify/read",
    microNotifyReadAll: "/micro/notify/readAll",
    // 模块化页面配置
    microPageConfig: "/page/config",
    // 活动浏览（匿名可访问）
    microActivityBrowse: "/micro/activity/browse",
    // 历史活动（boss 登录态，已取消/已完成，支持名称+时间搜索分页）
    microActivityHistory: "/micro/activity/history",
    // ★boss 最新活动→报名详情聚合（主持/名单/人数/名额，micro:promo:list）
    microActivitySignupDetail: "/micro/activity/signupDetail",
    // ★顾客端活动自助报名（活动详情页「确认报名」，登录态）
    microActivitySignup: "/micro/activity/signup/",
    // ★boss「我的待处理→状态设置中心」：房间/助教资源状态与手动设置（FEAT_SELF_BOOKING）
    microResourceRoomList: "/micro/resourceStatus/room/list",
    microResourceStaffList: "/micro/resourceStatus/staff/list",
    microResourceSet: "/micro/resourceStatus/set",
    // 活动发布/下架/结束（/micro/promo/status/{id}/{status}）
    microPromoStatus: "/micro/promo/status",
    // ========== T15.37 盈亏三指标 ==========
    microProfitSummary: "/micro/report/profitSummary",
    // ========== T15.37 固定支出 ==========
    microCostList: "/micro/cost/all",
    microCostMonthSum: "/micro/cost/monthSum",
    microCostAdd: "/micro/cost",
    microCostUpdate: "/micro/cost",
    microCostDelete: "/micro/cost",
    // ========== T16.11 推广码 ==========
    microPromoMy: "/micro/promo/code/my",
    microPromoGenerate: "/micro/promo/code/generate",
    // V3.7 我的微信小程序码（scene=租户+用户名+角色，返回 base64 图片）
    microPromoMyQrcode: "/micro/promo/code/myQrcode",
    // ========== V3.7 老板经营驾驶舱 + 待办与异常处理中心 ==========
    microBossDashboard: "/micro/bossDashboard",
    microComplianceRecords: "/micro/compliance/records/list",
    // ========== T16.6 扫码归因 ==========
    microPromoScan: "/micro/promo/attribution/scan",
    // ========== T16.3 优惠券 ==========
    microCustomerCoupon: "/micro/coupon/my",
    // ========== 顾客消费记录 ==========
    microCustomerRecords: "/micro/cashflow/my",
    // ========== boss lite 活动统计 ==========
    microActivityStats: "/micro/activity/stats",
    // ========== v1.43 租户主题下发（登录可读） ==========
    microThemeCurrent: "/micro/platform/theme/current",
    // ========== T29.5 匿名主题接口（登录前按 tenantId 获取主题） ==========
    microThemeByTenant: "/micro/platform/theme/currentByTenant",
    // ========== 业务员我的顾客 ==========
    microStaffCustomers: "/micro/promo/attribution/list",
    // ========== T10.5/T10.6 文案映射 + Schema 渲染 ==========
    microBizCopyAll: "/system/bizCopy/all",
    microRenderSchema: "/system/tenantBizConfig/render",
    // ========== T17 评价与点赞 ==========
    microRatingSubmit: "/micro/rating/submit",
    microRatingSummary: "/micro/rating/summary",
    microRatingLikeToggle: "/micro/rating/like/toggle",
    microRatingLikeCount: "/micro/rating/like/count",
    microRatingIsLiked: "/micro/rating/like/isLiked",
    // ========== 业务员个人中心 ==========
    microStaffMine: "/micro/staff/mine",
    // ========== T18.17 录音知情告知 ==========
    microVoiceRetention: "/micro/compliance/retention",
    // ========== T18.19 账号设置（老板/员工/顾客通用） ==========
    microProfile: "/micro/profile",
    // 获取个人信息
    microUpdateProfile: "/micro/profile",
    // PUT 更新个人信息（昵称/手机/邮箱）
    microUpdatePwd: "/micro/profile/updatePwd",
    // ========== T21 自助预约（顾客小程序，★FEAT_SELF_BOOKING 开关） ==========
    microSelfBookingItems: "/micro/selfBooking/items",
    microSelfBookingRooms: "/micro/selfBooking/rooms",
    microSelfBookingStaff: "/micro/selfBooking/staff",
    microSelfBookingSubmit: "/micro/selfBooking/submit",
    // ★预约策略顾客端视图（登录可读）：顾客自助预约开关，首页/自助预约页据此隐藏入口
    microBookingPolicyCustomerView: "/micro/bookingPolicy/customerView",
    // ★角色显示名映射（登录可读）：行业术语动态化（助教/导游/司机），页面文案不硬编码
    microRoleDisplayNames: "/micro/tenantRole/displayNames",
    // ========== T22 员工排班（★FEAT_STAFF_SCHEDULE 开关，micro_pro 启用） ==========
    // 员工端：查看本人排班（仅 published）
    microStaffScheduleMy: "/micro/staffSchedule/my",
    // 顾客端：查看门店最新排班（仅 published，按日期分组）
    microStaffScheduleStore: "/micro/staffSchedule/store",
    // ========== T24 员工打卡（★FEAT_CHECK_IN 开关，micro_pro 启用） ==========
    microCheckIn: "/micro/checkIn",
    microCheckInMy: "/micro/checkIn/my",
    microCheckInActivity: "/micro/checkIn/activity/",
    microCheckInEnabled: "/micro/checkIn/enabled",
    // ========== T25 投诉与合规（★FEAT_COMPLAINT 开关，全版本启用） ==========
    microComplaintSubmit: "/micro/complaint/submit",
    microComplaintMy: "/micro/complaint/my",
    microComplaintStaffOptions: "/micro/complaint/staffOptions",
    microComplianceAgreementCurrent: "/micro/complianceAgreement/current",
    microComplianceAgreementSign: "/micro/complianceAgreement/sign",
    microComplianceAgreementCheck: "/micro/complianceAgreement/check",
    // ========== T26.7 顾客选座（★M26 房间座位布局与选座，FEAT_ROOM_SEAT_LAYOUT 开关） ==========
    // 活动座位图查询（登录即可）
    microSeatMap: "/micro/seat/map/",
    // 顾客选座（perms=micro:booking:pickSeat）
    microSeatPick: "/micro/seat/pick",
    // 顾客取消自己的座位（perms=micro:booking:pickSeat）
    microSeatRelease: "/micro/seat/release/",
    // 活动座位占用列表（登录即可）
    microSeatAssignments: "/micro/seat/assignments/",
    // ========== M28 T28.1/T28.2/T28.3 活动锁定 + Boss 编排 + 顾客移座（★FEAT_ACTIVITY_LOCK 开关） ==========
    // 活动锁定/解锁基址（PUT {base}{id}/lock 或 {base}{id}/unlock，perms micro:activity:lock / micro:activity:unlock）
    microActivityLockBase: "/micro/activity/",
    // Boss 加人到座位（perms=micro:seat:assign）
    microSeatAssign: "/micro/seat/assign",
    // Boss 移动座位，含预置人员（perms=micro:seat:assign）
    microSeatMove: "/micro/seat/move",
    // Boss 两人互换座位（perms=micro:seat:assign）
    microSeatSwap: "/micro/seat/swap",
    // 顾客移动自己的座位（perms=micro:booking:pickSeat）
    microSeatMoveMine: "/micro/seat/moveMine",
    // 座位编排操作日志回放（perms=micro:seat:assign）
    microSeatOperationLogs: "/micro/seat/operationLogs/"
  },
  // 请求超时（毫秒）
  timeout: 3e4
};
exports.config = config;
