"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_auth = require("../../../utils/auth.js");
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const utils_format = require("../../../utils/format.js");
const _sfc_main = {
  data() {
    return {
      loaded: false,
      currentTab: "weekly",
      data: {},
      tabs: [
        { key: "weekly", label: "周报" },
        { key: "monthly", label: "月报" },
        { key: "cashFlow", label: "资金水位" }
      ]
    };
  },
  onShow() {
    if (!utils_auth.getToken()) {
      common_vendor.index.reLaunch({ url: "/pages/login/index" });
      return;
    }
    this.loadData();
  },
  methods: {
    formatAmount: utils_format.formatAmount,
    go(url) {
      common_vendor.index.navigateTo({ url });
    },
    switchTab(key) {
      this.currentTab = key;
      this.loadData();
    },
    async loadData() {
      this.loaded = false;
      try {
        const apiMap = { weekly: config_index.config.api.microReportWeekly, monthly: config_index.config.api.microReportMonthly, cashFlow: config_index.config.api.microReportCashFlow };
        const res = await utils_request.get(apiMap[this.currentTab]);
        this.data = res && res.data || {};
      } catch (e) {
        this.data = {};
      } finally {
        this.loaded = true;
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.f($data.tabs, (t, k0, i0) => {
      return {
        a: common_vendor.t(t.label),
        b: t.key,
        c: $data.currentTab === t.key ? 1 : "",
        d: common_vendor.o(($event) => $options.switchTab(t.key), t.key)
      };
    }),
    b: !$data.loaded
  }, !$data.loaded ? {} : common_vendor.e({
    c: common_vendor.t($options.formatAmount($data.data.totalIncome || 0)),
    d: common_vendor.t($options.formatAmount($data.data.totalExpense || 0)),
    e: common_vendor.t($options.formatAmount($data.data.netProfit || 0)),
    f: common_vendor.t($data.data.orderCount || 0),
    g: $data.data.period
  }, $data.data.period ? {
    h: common_vendor.t($data.data.period)
  } : {}), {
    i: common_vendor.o(($event) => $options.go("/pages/micro/report/trend"))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-bdf2289f"]]);
wx.createPage(MiniProgramPage);
