/**
 * 全局配置
 */
export default {
    // 后端接口地址（开发联调时改为本机网关地址）
    baseUrl: 'http://localhost:8080',
    api: {
        // 登录/登出/用户信息（RuoYi 框架：/login 返回 data.token；/getInfo 返回 roles/permissions/features/edition）
        login: '/login',
        // 微信小程序一键登录（T13.1 code2session；wx.login code + 选定门店 tenantId）
        xcxLogin: '/xcxLogin',
        logout: '/logout',
        userInfo: '/getInfo',
        // 版本能力开关下发（防串第③层：前端动态渲染，T4.3/T8.5）
        editionFeatures: '/system/edition/features',
        // 字典数据（物资小类 fee_item_category，登录即可）
        dictData: '/system/dict/data/type',
        // 文件上传（补票发票图片，需 system:oss:upload 权限）
        upload: '/system/oss/upload',

        // ========== M8 引导态门户（/portal/**，安全白名单，无需登录，T8.10/T13.1） ==========
        // 门店搜索/选择首页：关键字 + 城市 + 用户经纬度就近排序，仅返回公开展示字段
        portalStores: '/portal/stores',
        // 按 openid 反查已绑定门店列表（公众号/小程序授权后可用）
        portalTenants: '/portal/tenants',
        // 公众号总入口：按公众号 AppID 解析归属主体
        portalResolve: '/portal/tenant/resolve',
        // 企微自建应用网页授权链接 / OAuth 回调换取成员身份
        portalWecomAuthUrl: '/portal/wecom/authUrl',
        portalWecomUserInfo: '/portal/wecom/userInfo',
        // 引导态绑定：openid→门店（用户选定门店后落库，登录后由 /login 回填 userId）
        portalBind: '/portal/tenant/bind',

        // ========== 老板看板（/fee/dashboard，与 PC 3215 同源） ==========
        dashboard: '/fee/dashboard',
        dashboardProjects: '/fee/dashboard/projects',
        dashboardDrillDown: '/fee/dashboard/drillDown',

        // ========== 项目管理（/fee/project，与 PC 3201 同源） ==========
        projectList: '/fee/project/list',
        projectActiveList: '/fee/project/activeList',
        projectApprove: '/fee/project/approve',
        projectReject: '/fee/project/reject',
        projectProfit: '/fee/project/profit',
        projectSubjects: '/fee/project/subjects',
        projectMembers: '/fee/project/members',

        // ========== 收入确认（/fee/income，与 PC 3202/3210 同源） ==========
        incomeList: '/fee/income/list',
        incomeAdd: '/fee/income',

        // ========== 报销单（/fee/expense，与 PC 3004 同源；审批为 POST） ==========
        expenseList: '/fee/expense/list',
        expenseAdd: '/fee/expense',
        expensePending: '/fee/expense/pendingApproval',
        expenseApprove: '/fee/expense/approve',
        expenseReject: '/fee/expense/reject',

        // ========== 付款单 / 合同变更（审批为 PUT） ==========
        paymentList: '/fee/payment/list',
        paymentApprove: '/fee/payment/approve',
        paymentReject: '/fee/payment/reject',
        changeList: '/fee/change/list',
        changeApprove: '/fee/change/approve',
        changeReject: '/fee/change/reject',

        // ========== 补票记录（/fee/invoiceSupplement，T5.8 后补通道） ==========
        supplementList: '/fee/invoiceSupplement/list',
        supplementCreate: '/fee/invoiceSupplement',
        supplementSubmit: '/fee/invoiceSupplement/supplement',

        // ========== 项目关联规则（报销表单硬/软规则，登录即可） ==========
        linkRuleMatch: '/fee/linkRule/match',
        // ========== 用途标签（登录即可） ==========
        purposeTagAll: '/fee/purposeTag/allEnabled',

        // ========== 消息中心（3114） ==========
        messageList: '/system/message/list',
        messageUnread: '/system/message/unreadCount',
        messageRead: '/system/message/read',
        messageReadAll: '/system/message/readAll',

        // ========== 高级/专业版增量（T8.5，后端未就绪时前端优雅降级） ==========
        creditMine: '/fee/credit/mine',
        orgMyList: '/fee/org/myList'
    },
    // 请求超时（毫秒）
    timeout: 30000
}
