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
    payText(s) {
      return (utils_format.PAY_STATUS[s] || {}).label || s || "-";
    },
    payColor(s) {
      return (utils_format.PAY_STATUS[s] || {}).color || "#909399";
    },
    async loadDetail() {
      try {
        const res = await utils_request.get("/micro/booking/" + this.id);
        this.order = res && res.data || null;
      } catch (e) {
      } finally {
        this.loaded = true;
      }
    },
    async complete() {
      common_vendor.index.showModal({ title: "确认", content: "确认已完成服务？", success: async (r) => {
        if (r.confirm) {
          await utils_request.put(config_index.config.api.microBookingComplete + "/" + this.id);
          common_vendor.index.showToast({ title: "已完成" });
          this.loadDetail();
        }
      } });
    },
    async receive() {
      common_vendor.index.showModal({ title: "确认收款", content: "确认已线下收款？", success: async (r) => {
        if (r.confirm) {
          await utils_request.put(config_index.config.api.microBookingReceive + "/" + this.id);
          common_vendor.index.showToast({ title: "已收款" });
          this.loadDetail();
        }
      } });
    },
    async cancel() {
      common_vendor.index.showModal({ title: "取消", content: "确认取消该预约？", success: async (r) => {
        if (r.confirm) {
          await utils_request.put(config_index.config.api.microBookingCancel + "/" + this.id);
          common_vendor.index.showToast({ title: "已取消" });
          this.loadDetail();
        }
      } });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: !$data.loaded
  }, !$data.loaded ? {} : $data.order ? common_vendor.e({
    c: common_vendor.t($options.statusText($data.order.orderStatus)),
    d: $options.statusColor($data.order.orderStatus),
    e: common_vendor.t($data.order.customerName || "-"),
    f: common_vendor.t($data.order.customerPhone || "-"),
    g: common_vendor.t($data.order.serviceItemName || "-"),
    h: common_vendor.t($data.order.bookingTime || "-"),
    i: common_vendor.t($data.order.amount != null ? "¥" + $options.formatAmount($data.order.amount) : "-"),
    j: common_vendor.t($options.payText($data.order.payStatus)),
    k: $options.payColor($data.order.payStatus),
    l: $data.order.staffName
  }, $data.order.staffName ? {
    m: common_vendor.t($data.order.staffName)
  } : {}, {
    n: $data.order.remark
  }, $data.order.remark ? {
    o: common_vendor.t($data.order.remark)
  } : {}, {
    p: $data.order.orderStatus === "PENDING_SERVICE"
  }, $data.order.orderStatus === "PENDING_SERVICE" ? {
    q: common_vendor.o((...args) => $options.complete && $options.complete(...args))
  } : {}, {
    r: $data.order.payStatus === "PENDING_PAY" && $data.order.orderStatus !== "CANCELLED"
  }, $data.order.payStatus === "PENDING_PAY" && $data.order.orderStatus !== "CANCELLED" ? {
    s: common_vendor.o((...args) => $options.receive && $options.receive(...args))
  } : {}, {
    t: $data.order.orderStatus === "PENDING_SERVICE"
  }, $data.order.orderStatus === "PENDING_SERVICE" ? {
    v: common_vendor.o((...args) => $options.cancel && $options.cancel(...args))
  } : {}) : {}, {
    b: $data.order
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-b7d30e50"]]);
wx.createPage(MiniProgramPage);
