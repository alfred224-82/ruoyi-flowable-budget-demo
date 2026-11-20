"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_permission = require("../../utils/permission.js");
const utils_request = require("../../utils/request.js");
const config_index = require("../../config/index.js");
const utils_format = require("../../utils/format.js");
const _sfc_main = {
  data() {
    return {
      data: {},
      // 下钻状态：level=1 大类视图；2=按项目；3=按明细
      drill: { level: 1, category: "", projectId: null, projectName: "", rows: [] }
    };
  },
  computed: {
    categoryList() {
      return this.data.categoryExpenses || [];
    },
    totalCatAmount() {
      return this.categoryList.reduce((s, c) => s + Number(c.amount || 0), 0);
    }
  },
  onLoad() {
    if (!utils_permission.guardPage("/pages/dashboard/category")) return;
    this.loadDashboard();
  },
  onPullDownRefresh() {
    this.loadDashboard().finally(() => common_vendor.index.stopPullDownRefresh());
  },
  methods: {
    formatAmount: utils_format.formatAmount,
    formatWan: utils_format.formatWan,
    /** 看板总数据（与 PC 3215/3288 同源） */
    async loadDashboard() {
      try {
        const res = await utils_request.get(config_index.config.api.dashboard);
        this.data = res.data || {};
      } catch (e) {
      }
    },
    /** 大类风险色：占比<30% 🟢，30%~60% 🟡，>60% 🔴（设计文档 7.3 三秒看懂） */
    riskDot(cat) {
      const ratio = this.totalCatAmount ? Number(cat.amount || 0) / this.totalCatAmount : 0;
      if (ratio > 0.6) return "🔴";
      if (ratio >= 0.3) return "🟡";
      return "🟢";
    },
    ratioText(cat) {
      const ratio = this.totalCatAmount ? Number(cat.amount || 0) / this.totalCatAmount * 100 : 0;
      return ratio.toFixed(1) + "%";
    },
    /** 大类 → 项目分布（下钻第2层） */
    async drillToCategory(cat) {
      try {
        const res = await utils_request.get(config_index.config.api.dashboardDrillDown, { level: 2, category: cat.categoryName });
        this.drill = { level: 2, category: cat.categoryName, projectId: null, projectName: "", rows: res.data || [] };
      } catch (e) {
      }
    },
    /** 项目 → 单据明细（下钻第3层） */
    async drillToRow(row) {
      if (this.drill.level !== 2 || row.projectId === void 0) return;
      try {
        const res = await utils_request.get(config_index.config.api.dashboardDrillDown, {
          level: 3,
          category: this.drill.category,
          projectId: row.projectId
        });
        this.drill.projectId = row.projectId;
        this.drill.projectName = row.name;
        this.drill.rows = res.data || [];
        this.drill.level = 3;
      } catch (e) {
      }
    },
    /** 下钻返回上一层 */
    drillBack() {
      if (this.drill.level === 3) {
        this.drillToCategory({ categoryName: this.drill.category });
      } else {
        this.drill = { level: 1, category: "", projectId: null, projectName: "", rows: [] };
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  var _a, _b, _c, _d;
  return common_vendor.e({
    a: common_vendor.t($options.formatWan($data.data.totalActualProfit)),
    b: common_vendor.t($options.formatWan($data.data.totalContractAmount)),
    c: common_vendor.t($options.formatWan($data.data.totalVerifiedIncome)),
    d: common_vendor.t($options.formatWan($data.data.totalPendingIncome)),
    e: common_vendor.t($options.formatWan($data.data.totalExpense)),
    f: common_vendor.t($data.data.activeProjectCount || 0),
    g: common_vendor.t($data.data.pendingApprovalCount || 0),
    h: !$options.categoryList.length
  }, !$options.categoryList.length ? {} : {}, {
    i: common_vendor.f($options.categoryList, (cat, k0, i0) => {
      return {
        a: common_vendor.t($options.riskDot(cat)),
        b: common_vendor.t(cat.categoryName),
        c: common_vendor.t($options.formatAmount(cat.amount)),
        d: common_vendor.t(cat.billCount || 0),
        e: common_vendor.t($options.ratioText(cat)),
        f: cat.categoryName,
        g: common_vendor.o(($event) => $options.drillToCategory(cat), cat.categoryName)
      };
    }),
    j: $data.drill.level >= 2
  }, $data.drill.level >= 2 ? common_vendor.e({
    k: common_vendor.t($data.drill.level === 3 ? "大类" : ""),
    l: common_vendor.o((...args) => $options.drillBack && $options.drillBack(...args)),
    m: common_vendor.t($data.drill.category),
    n: common_vendor.t($data.drill.projectName ? " / " + $data.drill.projectName : ""),
    o: !$data.drill.rows.length
  }, !$data.drill.rows.length ? {} : {}, {
    p: common_vendor.f($data.drill.rows, (row, idx, i0) => {
      return {
        a: common_vendor.t(row.name || row.no),
        b: common_vendor.t($options.formatAmount(row.amount)),
        c: idx,
        d: common_vendor.o(($event) => $options.drillToRow(row), idx)
      };
    })
  }) : {}, {
    q: common_vendor.t($data.data.accrualMonth || "本月"),
    r: common_vendor.t($options.formatAmount($data.data.monthlySalaryAccrual)),
    s: common_vendor.t($options.formatAmount($data.data.monthlyDepreciation)),
    t: common_vendor.t($options.formatAmount($data.data.monthlyLoanInterest)),
    v: !((_a = $data.data.badDebtWarnings) == null ? void 0 : _a.length) && !((_b = $data.data.changeConfirmWarnings) == null ? void 0 : _b.length)
  }, !((_c = $data.data.badDebtWarnings) == null ? void 0 : _c.length) && !((_d = $data.data.changeConfirmWarnings) == null ? void 0 : _d.length) ? {} : {}, {
    w: common_vendor.f($data.data.badDebtWarnings || [], (bd, k0, i0) => {
      return {
        a: common_vendor.t(bd.projectName || "项目#" + bd.projectId),
        b: common_vendor.t(bd.lossType || "-"),
        c: common_vendor.t($options.formatAmount(bd.badDebtProvision)),
        d: "bd" + bd.id
      };
    }),
    x: common_vendor.f($data.data.changeConfirmWarnings || [], (ch, k0, i0) => {
      return {
        a: common_vendor.t(ch.projectName || "项目#" + ch.projectId),
        b: common_vendor.t($options.formatAmount(ch.contractDelta)),
        c: common_vendor.t(ch.changeStatus),
        d: "ch" + ch.id
      };
    }),
    y: common_vendor.t($data.data.unclassifiedBillCount || 0),
    z: common_vendor.t($options.formatAmount($data.data.unclassifiedBillAmount))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-64094773"]]);
wx.createPage(MiniProgramPage);
