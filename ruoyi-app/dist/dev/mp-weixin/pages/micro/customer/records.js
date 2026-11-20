"use strict";
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const utils_format = require("../../../utils/format.js");
const utils_themeMixin = require("../../../utils/themeMixin.js");
const common_vendor = require("../../../common/vendor.js");
const _sfc_main = {
  mixins: [utils_themeMixin.themeMixin],
  data() {
    return { loaded: false, records: [] };
  },
  onShow() {
    this.loadRecords();
  },
  methods: {
    formatAmount: utils_format.formatAmount,
    async loadRecords() {
      this.loaded = false;
      try {
        const res = await utils_request.get(config_index.config.api.microCustomerRecords, { pageNum: 1, pageSize: 50 });
        const data = res && res.data || {};
        this.records = data.rows || data.list || [];
      } catch (e) {
        this.records = [];
      }
      this.loaded = true;
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: !$data.loaded
  }, !$data.loaded ? {} : !$data.records.length ? {} : {
    c: common_vendor.f($data.records, (r, k0, i0) => {
      return {
        a: common_vendor.t(r.flowType === "income" ? "收入" : "支出"),
        b: common_vendor.t(r.flowType === "income" ? "+" : "-"),
        c: common_vendor.t($options.formatAmount(r.amount || 0)),
        d: common_vendor.n(r.flowType === "income" ? "income" : "expense"),
        e: common_vendor.t(r.remark || r.category || "-"),
        f: common_vendor.t(r.createTime || ""),
        g: r.id
      };
    })
  }, {
    b: !$data.records.length,
    d: common_vendor.n(_ctx.themeClass),
    e: common_vendor.s(_ctx.pageStyle)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-0a819b20"]]);
wx.createPage(MiniProgramPage);
