"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_auth = require("../../../utils/auth.js");
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const utils_format = require("../../../utils/format.js");
const _sfc_main = {
  data() {
    return { loaded: false, list: [], maxVal: 0 };
  },
  onShow() {
    if (!utils_auth.getToken()) {
      common_vendor.index.reLaunch({ url: "/pages/login/index" });
      return;
    }
    this.loadTrend();
  },
  methods: {
    formatAmount: utils_format.formatAmount,
    barWidth(val, max) {
      if (!max || !val) return "0%";
      return Math.max(5, Math.round(val / max * 100)) + "%";
    },
    async loadTrend() {
      this.loaded = false;
      try {
        const res = await utils_request.post(config_index.config.api.microReportTrend, {});
        this.list = res && res.data || [];
        this.maxVal = Math.max(...this.list.map((i) => Math.max(Math.abs(i.totalIncome || 0), Math.abs(i.totalExpense || 0))), 1);
      } catch (e) {
        this.list = [];
      } finally {
        this.loaded = true;
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: !$data.loaded
  }, !$data.loaded ? {} : !$data.list.length ? {} : {
    c: common_vendor.f($data.list, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.revenueDate),
        b: $options.barWidth(item.totalIncome, $data.maxVal),
        c: $options.barWidth(item.totalExpense, $data.maxVal),
        d: common_vendor.t($options.formatAmount(item.netProfit || 0)),
        e: (item.netProfit || 0) < 0 ? 1 : "",
        f: item.revenueDate
      };
    })
  }, {
    b: !$data.list.length
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-f039a977"]]);
wx.createPage(MiniProgramPage);
