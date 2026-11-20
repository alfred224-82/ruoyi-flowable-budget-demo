"use strict";
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const utils_format = require("../../../utils/format.js");
const common_vendor = require("../../../common/vendor.js");
const _sfc_main = {
  data() {
    return { loaded: false, list: [], monthSum: 0 };
  },
  onShow() {
    this.loadData();
  },
  methods: {
    formatAmount: utils_format.formatAmount,
    async loadData() {
      this.loaded = false;
      try {
        const [listRes, sumRes] = await Promise.all([
          utils_request.get(config_index.config.api.microCostList),
          utils_request.get(config_index.config.api.microCostMonthSum)
        ]);
        this.list = listRes && listRes.data || [];
        this.monthSum = sumRes && sumRes.data || 0;
      } catch (e) {
      }
      this.loaded = true;
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.t($options.formatAmount($data.monthSum)),
    b: !$data.loaded
  }, !$data.loaded ? {} : !$data.list.length ? {} : {
    d: common_vendor.f($data.list, (item, k0, i0) => {
      return common_vendor.e({
        a: common_vendor.t(item.name),
        b: common_vendor.t($options.formatAmount(item.amount)),
        c: common_vendor.t(item.categoryName),
        d: common_vendor.t(item.effectiveMonth),
        e: item.amortizeMonths > 0
      }, item.amortizeMonths > 0 ? {
        f: common_vendor.t($options.formatAmount(item.monthlyAmount)),
        g: common_vendor.t(item.amortizeMonths)
      } : {}, {
        h: common_vendor.t(item.enabled === 1 ? "启用" : "停用"),
        i: common_vendor.n(item.enabled === 1 ? "on" : "off"),
        j: item.id
      });
    })
  }, {
    c: !$data.list.length
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-d16800bc"]]);
wx.createPage(MiniProgramPage);
