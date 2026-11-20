"use strict";
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const common_vendor = require("../../../common/vendor.js");
const _sfc_main = {
  data() {
    return { loaded: false, customers: [] };
  },
  onShow() {
    this.loadCustomers();
  },
  methods: {
    async loadCustomers() {
      this.loaded = false;
      try {
        const res = await utils_request.get(config_index.config.api.microStaffCustomers, { pageNum: 1, pageSize: 50 });
        const data = res && res.data || {};
        this.customers = data.rows || data.list || [];
      } catch (e) {
        this.customers = [];
      }
      this.loaded = true;
    },
    formatTime(t) {
      if (!t) return "-";
      return typeof t === "string" ? t.substring(0, 19) : "-";
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: !$data.loaded
  }, !$data.loaded ? {} : !$data.customers.length ? {} : {
    c: common_vendor.f($data.customers, (c, k0, i0) => {
      return {
        a: common_vendor.t(c.customerPhone || "顾客"),
        b: common_vendor.t($options.formatTime(c.lastScanTime)),
        c: common_vendor.t($options.formatTime(c.lastScanTime)),
        d: common_vendor.t(c.status === "0" ? "有效" : "失效"),
        e: common_vendor.n(c.status === "0" ? "active" : "inactive"),
        f: c.id
      };
    })
  }, {
    b: !$data.customers.length
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-f2a68407"]]);
wx.createPage(MiniProgramPage);
