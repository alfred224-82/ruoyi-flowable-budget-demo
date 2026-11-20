"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const utils_format = require("../../../utils/format.js");
const _sfc_main = {
  data() {
    return { loaded: false, order: null };
  },
  onLoad(options) {
    this.id = options.id;
    this.loadDetail();
  },
  methods: {
    formatAmount: utils_format.formatAmount,
    statusText(s) {
      return (utils_format.BOOKING_STATUS[s] || {}).label || s || "-";
    },
    statusColor(s) {
      return (utils_format.BOOKING_STATUS[s] || {}).color || "#909399";
    },
    async loadDetail() {
      try {
        const res = await utils_request.get(config_index.config.api.microStaffSchedule + "/" + this.id);
        this.order = res && res.data || null;
      } catch (e) {
      } finally {
        this.loaded = true;
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: !$data.loaded
  }, !$data.loaded ? {} : $data.order ? {
    c: common_vendor.t($data.order.customerName || "-"),
    d: common_vendor.t($data.order.serviceItemName || "-"),
    e: common_vendor.t($data.order.bookingTime || "-"),
    f: common_vendor.t($data.order.completeTime || "-"),
    g: common_vendor.t($options.formatAmount($data.order.amount || 0)),
    h: common_vendor.t($options.statusText($data.order.orderStatus)),
    i: $options.statusColor($data.order.orderStatus)
  } : {}, {
    b: $data.order
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-801a1cbe"]]);
wx.createPage(MiniProgramPage);
