"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const utils_format = require("../../../utils/format.js");
const utils_themeMixin = require("../../../utils/themeMixin.js");
const _sfc_main = {
  mixins: [utils_themeMixin.themeMixin],
  data() {
    return { loaded: false, order: null, payInfoText: "" };
  },
  computed: {
    // ★解析 remark 中的费用明细/合计/备注文本
    cost() {
      return this.order ? utils_format.parseBookingCost(this.order.remark) : { details: [], total: 0, hours: 0, peopleCount: 0, remarkText: "" };
    }
  },
  onLoad(options) {
    this.id = options.id;
    this.loadDetail();
    this.loadPayInfo();
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
        const res = await utils_request.get(config_index.config.api.microCustomerMyBookDetail + this.id);
        this.order = res && res.data || null;
      } catch (e) {
      } finally {
        this.loaded = true;
      }
    },
    async loadPayInfo() {
      try {
        const res = await utils_request.get(config_index.config.api.microCustomerPayInfo);
        this.payInfoText = res && res.data || "";
      } catch (e) {
      }
    },
    cancel() {
      common_vendor.index.showModal({
        title: "取消预约",
        content: "确认取消该预约？",
        success: async (r) => {
          if (r.confirm) {
            try {
              await utils_request.put(config_index.config.api.microCustomerCancelBook + "/" + this.id);
              common_vendor.index.showToast({ title: "已取消", icon: "success" });
              this.loadDetail();
            } catch (e) {
            }
          }
        }
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: !$data.loaded
  }, !$data.loaded ? {} : $data.order ? common_vendor.e({
    c: common_vendor.t($options.statusText($data.order.orderStatus)),
    d: $options.statusColor($data.order.orderStatus),
    e: common_vendor.t($data.order.serviceName || $data.order.serviceItemName || "-"),
    f: common_vendor.t($data.order.bookingTime || "-"),
    g: $options.cost.details.length
  }, $options.cost.details.length ? {
    h: common_vendor.f($options.cost.details, (cd, ci, i0) => {
      return {
        a: common_vendor.t(cd.name),
        b: common_vendor.t(cd.type),
        c: common_vendor.t(cd.unitPrice),
        d: common_vendor.t(cd.hours),
        e: common_vendor.t(cd.subtotal),
        f: ci
      };
    }),
    i: common_vendor.t($options.formatAmount($options.cost.total))
  } : {
    j: common_vendor.t($options.formatAmount($data.order.amount || 0))
  }, {
    k: $data.order.staffName
  }, $data.order.staffName ? {
    l: common_vendor.t($data.order.staffName)
  } : {}, {
    m: $options.cost.remarkText
  }, $options.cost.remarkText ? {
    n: common_vendor.t($options.cost.remarkText)
  } : {}, {
    o: common_vendor.t($data.payInfoText || "本店支持到店付款，服务完成后请向前台付款。"),
    p: $data.order.orderStatus === "PENDING_SERVICE"
  }, $data.order.orderStatus === "PENDING_SERVICE" ? {
    q: common_vendor.o((...args) => $options.cancel && $options.cancel(...args))
  } : {}) : {}, {
    b: $data.order,
    r: common_vendor.n(_ctx.themeClass),
    s: common_vendor.s(_ctx.pageStyle)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-214052cb"]]);
wx.createPage(MiniProgramPage);
