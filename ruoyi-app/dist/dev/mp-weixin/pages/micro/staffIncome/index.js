"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_auth = require("../../../utils/auth.js");
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const utils_format = require("../../../utils/format.js");
const _sfc_main = {
  data() {
    return { income: {}, details: [], detailLoaded: false };
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
    goDetail(id) {
      common_vendor.index.navigateTo({ url: "/pages/micro/staffIncome/detail?id=" + id });
    },
    async loadData() {
      try {
        const [incomeRes, detailRes] = await Promise.all([
          utils_request.get(config_index.config.api.microStaffIncome),
          utils_request.get(config_index.config.api.microStaffIncomeDetail)
        ]);
        this.income = incomeRes && incomeRes.data || {};
        this.details = detailRes && detailRes.data || [];
      } catch (e) {
      } finally {
        this.detailLoaded = true;
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.t($options.formatAmount($data.income.weekAmount || 0)),
    b: common_vendor.t($options.formatAmount($data.income.monthAmount || 0)),
    c: common_vendor.t($data.income.weekOrders || 0),
    d: common_vendor.t($data.income.monthOrders || 0),
    e: !$data.detailLoaded
  }, !$data.detailLoaded ? {} : !$data.details.length ? {} : {
    g: common_vendor.f($data.details, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.customerName || "顾客"),
        b: common_vendor.t(item.serviceItemName || "-"),
        c: common_vendor.t(item.completeTime || item.bookingTime || "-"),
        d: common_vendor.t($options.formatAmount(item.amount || 0)),
        e: item.id,
        f: common_vendor.o(($event) => $options.goDetail(item.id), item.id)
      };
    })
  }, {
    f: !$data.details.length
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-63f372ef"]]);
wx.createPage(MiniProgramPage);
