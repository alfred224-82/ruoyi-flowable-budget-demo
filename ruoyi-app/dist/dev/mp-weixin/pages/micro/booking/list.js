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
      list: [],
      currentStatus: "",
      statusOptions: [
        { label: "全部", value: "" },
        { label: "待服务", value: "PENDING_SERVICE" },
        { label: "未收款", value: "PENDING_PAY" },
        { label: "已完成", value: "COMPLETED" },
        { label: "已取消", value: "CANCELLED" }
      ]
    };
  },
  onShow() {
    if (!utils_auth.getToken()) {
      common_vendor.index.reLaunch({ url: "/pages/login/index" });
      return;
    }
    this.loadList();
  },
  methods: {
    formatAmount: utils_format.formatAmount,
    statusText(status) {
      return (utils_format.BOOKING_STATUS[status] || {}).label || status || "-";
    },
    statusColor(status) {
      return (utils_format.BOOKING_STATUS[status] || {}).color || "#909399";
    },
    go(url) {
      common_vendor.index.navigateTo({ url });
    },
    goDetail(item) {
      if (item.sourceType === "ACTIVITY") return;
      common_vendor.index.navigateTo({ url: "/pages/micro/booking/detail?id=" + item.id });
    },
    changeStatus(status) {
      this.currentStatus = status;
      this.loadList();
    },
    async loadList() {
      this.loaded = false;
      try {
        const params = { pageNum: 1, pageSize: 50 };
        if (this.currentStatus === "PENDING_PAY") params.payStatus = this.currentStatus;
        else if (this.currentStatus) params.orderStatus = this.currentStatus;
        const res = await utils_request.get(config_index.config.api.microBookingList, params);
        this.list = res && res.rows || [];
      } catch (e) {
        this.list = [];
      } finally {
        this.loaded = true;
      }
    },
    async completeOrder(id) {
      common_vendor.index.showModal({
        title: "确认",
        content: "确认该预约已完成服务？",
        success: async (res) => {
          if (res.confirm) {
            try {
              await utils_request.put(config_index.config.api.microBookingComplete + "/" + id);
              common_vendor.index.showToast({ title: "已完成", icon: "success" });
              this.loadList();
            } catch (e) {
            }
          }
        }
      });
    },
    async receiveOrder(id) {
      common_vendor.index.showModal({
        title: "确认收款",
        content: "确认已线下收到该笔款项？",
        success: async (res) => {
          if (res.confirm) {
            try {
              await utils_request.put(config_index.config.api.microBookingReceive + "/" + id);
              common_vendor.index.showToast({ title: "已收款", icon: "success" });
              this.loadList();
            } catch (e) {
            }
          }
        }
      });
    },
    async cancelOrder(id) {
      common_vendor.index.showModal({
        title: "确认取消",
        content: "确认取消该预约？",
        success: async (res) => {
          if (res.confirm) {
            try {
              await utils_request.put(config_index.config.api.microBookingCancel + "/" + id);
              common_vendor.index.showToast({ title: "已取消", icon: "success" });
              this.loadList();
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
    a: common_vendor.f($data.statusOptions, (s, k0, i0) => {
      return {
        a: common_vendor.t(s.label),
        b: s.value,
        c: $data.currentStatus === s.value ? 1 : "",
        d: common_vendor.o(($event) => $options.changeStatus(s.value), s.value)
      };
    }),
    b: !$data.loaded
  }, !$data.loaded ? {} : !$data.list.length ? {} : {
    d: common_vendor.f($data.list, (item, k0, i0) => {
      return common_vendor.e({
        a: common_vendor.t(item.sourceType === "ACTIVITY" ? item.serviceName || "活动" : item.customerName || "顾客"),
        b: item.sourceType === "ACTIVITY"
      }, item.sourceType === "ACTIVITY" ? {} : {}, {
        c: common_vendor.t($options.statusText(item.orderStatus)),
        d: $options.statusColor(item.orderStatus),
        e: common_vendor.t(item.serviceItemName || item.serviceName || "-"),
        f: common_vendor.t(item.bookingTime || "-"),
        g: item.amount != null
      }, item.amount != null ? {
        h: common_vendor.t($options.formatAmount(item.amount))
      } : {}, {
        i: item.staffName
      }, item.staffName ? {
        j: common_vendor.t(item.staffName)
      } : {}, {
        k: item.sourceType !== "ACTIVITY"
      }, item.sourceType !== "ACTIVITY" ? common_vendor.e({
        l: item.orderStatus === "PENDING_SERVICE"
      }, item.orderStatus === "PENDING_SERVICE" ? {
        m: common_vendor.o(($event) => $options.completeOrder(item.id), (item.sourceType || "B") + "-" + item.id)
      } : {}, {
        n: item.payStatus === "PENDING_PAY" && item.orderStatus !== "CANCELLED"
      }, item.payStatus === "PENDING_PAY" && item.orderStatus !== "CANCELLED" ? {
        o: common_vendor.o(($event) => $options.receiveOrder(item.id), (item.sourceType || "B") + "-" + item.id)
      } : {}, {
        p: item.orderStatus === "PENDING_SERVICE"
      }, item.orderStatus === "PENDING_SERVICE" ? {
        q: common_vendor.o(($event) => $options.cancelOrder(item.id), (item.sourceType || "B") + "-" + item.id)
      } : {}) : {}, {
        r: (item.sourceType || "B") + "-" + item.id,
        s: common_vendor.o(($event) => $options.goDetail(item), (item.sourceType || "B") + "-" + item.id)
      });
    })
  }, {
    c: !$data.list.length,
    e: common_vendor.o(($event) => $options.go("/pages/micro/booking/add"))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-225a5936"]]);
wx.createPage(MiniProgramPage);
