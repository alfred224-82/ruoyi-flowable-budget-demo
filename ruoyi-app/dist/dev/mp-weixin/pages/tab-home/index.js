"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_roles = require("../../utils/roles.js");
const utils_auth = require("../../utils/auth.js");
const utils_request = require("../../utils/request.js");
const config_index = require("../../config/index.js");
const utils_format = require("../../utils/format.js");
const _sfc_main = {
  data() {
    return {
      role: "",
      ROLES: utils_roles.ROLES,
      // 老板看板摘要
      dash: {},
      // 项目经理：待核验收入数
      pendingIncome: 0,
      // 报销人/项目经理：驳回单据 + 待补票
      rejectedBills: [],
      pendingSupplements: [],
      unreadCount: 0,
      todoLoaded: false,
      // 财务：通知列表
      messages: []
    };
  },
  onShow() {
    if (!utils_auth.getToken()) {
      common_vendor.index.reLaunch({ url: "/pages/login/index" });
      return;
    }
    utils_roles.applyRoleTabs();
    this.role = utils_roles.getPrimaryRole();
    this.loadData();
  },
  methods: {
    formatAmount: utils_format.formatAmount,
    formatWan: utils_format.formatWan,
    formatDate: utils_format.formatDate,
    formatDateTime: utils_format.formatDateTime,
    go(url) {
      common_vendor.index.navigateTo({ url });
    },
    goMessages() {
      this.loadMessages();
    },
    async loadData() {
      if (this.role === utils_roles.ROLES.BOSS) {
        this.loadDashboard();
      } else if (this.role === utils_roles.ROLES.MANAGER) {
        this.loadManagerTodo();
      } else if (this.role === utils_roles.ROLES.EMPLOYEE) {
        await this.loadEmployeeTodo();
      } else {
        this.loadMessages();
      }
    },
    /** 老板：看板摘要（与 /fee/dashboard 同源，与 PC 看板数据一致） */
    async loadDashboard() {
      try {
        const res = await utils_request.get(config_index.config.api.dashboard);
        this.dash = res.data || {};
      } catch (e) {
      }
    },
    /** 项目经理：待核验收入 + 本人驳回单 */
    async loadManagerTodo() {
      try {
        const [incomeRes, billRes] = await Promise.all([
          utils_request.get(config_index.config.api.incomeList, { verifyStatus: "PENDING", pageNum: 1, pageSize: 1 }),
          utils_request.get(config_index.config.api.expenseList, { status: "REJECTED", pageNum: 1, pageSize: 5 })
        ]);
        this.pendingIncome = incomeRes && incomeRes.total || 0;
        this.rejectedBills = billRes && billRes.rows || [];
      } catch (e) {
      }
    },
    /** 报销人：首页待办（未读通知 + 驳回单 + 待补票，T8.4） */
    async loadEmployeeTodo() {
      this.todoLoaded = false;
      try {
        const [unreadRes, billRes, supRes] = await Promise.all([
          utils_request.get(config_index.config.api.messageUnread),
          utils_request.get(config_index.config.api.expenseList, { status: "REJECTED", pageNum: 1, pageSize: 5 }),
          utils_request.get(config_index.config.api.supplementList, { status: "PENDING", pageNum: 1, pageSize: 5 })
        ]);
        this.unreadCount = unreadRes && unreadRes.data || 0;
        this.rejectedBills = billRes && billRes.rows || [];
        this.pendingSupplements = supRes && supRes.rows || [];
      } catch (e) {
      } finally {
        this.todoLoaded = true;
      }
    },
    /** 财务：通知预警列表（消息中心 3114 同源，T8.6） */
    async loadMessages() {
      try {
        const res = await utils_request.get(config_index.config.api.messageList, { pageNum: 1, pageSize: 20 });
        this.messages = res && res.rows || [];
      } catch (e) {
      }
    },
    async readMsg(msg) {
      if (!msg.isRead) {
        try {
          await utils_request.put(config_index.config.api.messageRead + "/" + msg.id);
          msg.isRead = 1;
        } catch (e) {
        }
      }
    },
    async readAll() {
      try {
        await utils_request.put(config_index.config.api.messageReadAll);
        this.messages.forEach((m) => {
          m.isRead = 1;
        });
        common_vendor.index.showToast({ title: "已全部标记为已读", icon: "none" });
      } catch (e) {
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: $data.role === $data.ROLES.BOSS
  }, $data.role === $data.ROLES.BOSS ? common_vendor.e({
    b: common_vendor.o(($event) => $options.go("/pages/dashboard/category")),
    c: common_vendor.t($data.dash.pendingApprovalCount || 0),
    d: common_vendor.t($data.dash.activeProjectCount || 0),
    e: common_vendor.t($options.formatWan($data.dash.totalActualProfit)),
    f: common_vendor.t($options.formatWan($data.dash.totalExpense)),
    g: $data.dash.pendingApprovalCount > 0
  }, $data.dash.pendingApprovalCount > 0 ? {
    h: common_vendor.t($data.dash.pendingApprovalCount),
    i: common_vendor.o(($event) => $options.go("/pages/approval/list"))
  } : {}) : $data.role === $data.ROLES.MANAGER ? common_vendor.e({
    k: common_vendor.o(($event) => $options.go("/pages/project/list")),
    l: common_vendor.o(($event) => $options.go("/pages/project/income")),
    m: $data.pendingIncome > 0
  }, $data.pendingIncome > 0 ? {
    n: common_vendor.t($data.pendingIncome),
    o: common_vendor.o(($event) => $options.go("/pages/project/income"))
  } : {}, {
    p: $data.rejectedBills.length
  }, $data.rejectedBills.length ? {
    q: common_vendor.t($data.rejectedBills.length),
    r: common_vendor.o(($event) => $options.go("/pages/expense/mine"))
  } : {}) : $data.role === $data.ROLES.EMPLOYEE ? common_vendor.e({
    t: !$data.todoLoaded
  }, !$data.todoLoaded ? {} : common_vendor.e({
    v: $data.unreadCount > 0
  }, $data.unreadCount > 0 ? {
    w: common_vendor.t($data.unreadCount),
    x: common_vendor.o((...args) => $options.goMessages && $options.goMessages(...args))
  } : {}, {
    y: common_vendor.f($data.rejectedBills, (bill, k0, i0) => {
      return {
        a: common_vendor.t(bill.billNo),
        b: common_vendor.t($options.formatAmount(bill.amount)),
        c: common_vendor.t(bill.rejectReason || "-"),
        d: bill.id,
        e: common_vendor.o(($event) => $options.go("/pages/expense/mine"), bill.id)
      };
    }),
    z: common_vendor.f($data.pendingSupplements, (sup, k0, i0) => {
      return {
        a: common_vendor.t(sup.billNo),
        b: common_vendor.t($options.formatAmount(sup.reservedAmount)),
        c: common_vendor.t($options.formatDate(sup.estimatedDate)),
        d: sup.id,
        e: common_vendor.o(($event) => $options.go("/pages/expense/supplement"), sup.id)
      };
    }),
    A: $data.unreadCount === 0 && !$data.rejectedBills.length && !$data.pendingSupplements.length
  }, $data.unreadCount === 0 && !$data.rejectedBills.length && !$data.pendingSupplements.length ? {} : {})) : common_vendor.e({
    B: common_vendor.o((...args) => $options.readAll && $options.readAll(...args)),
    C: !$data.messages.length
  }, !$data.messages.length ? {} : {}, {
    D: common_vendor.f($data.messages, (msg, k0, i0) => {
      return {
        a: common_vendor.t(msg.title),
        b: common_vendor.t(msg.content),
        c: common_vendor.t($options.formatDateTime(msg.createTime)),
        d: msg.id,
        e: !msg.isRead ? 1 : "",
        f: common_vendor.o(($event) => $options.readMsg(msg), msg.id)
      };
    })
  }), {
    j: $data.role === $data.ROLES.MANAGER,
    s: $data.role === $data.ROLES.EMPLOYEE
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-b153d3ad"]]);
wx.createPage(MiniProgramPage);
