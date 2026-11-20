"use strict";
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const common_vendor = require("../../../common/vendor.js");
const _sfc_main = {
  data() {
    return { loaded: false, coupons: [] };
  },
  onShow() {
    this.loadCoupons();
  },
  methods: {
    async loadCoupons() {
      this.loaded = false;
      try {
        const res = await utils_request.get(config_index.config.api.microCustomerCoupon);
        this.coupons = res && res.data || [];
      } catch (e) {
        this.coupons = [];
      }
      this.loaded = true;
    },
    statusText(s) {
      const map = { UNUSED: "未使用", USED: "已使用", EXPIRED: "已过期" };
      return map[s] || s || "-";
    },
    statusClass(s) {
      if (s === "UNUSED") return "active";
      if (s === "USED") return "used";
      return "expired";
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: !$data.loaded
  }, !$data.loaded ? {} : !$data.coupons.length ? {} : {
    c: common_vendor.f($data.coupons, (c, k0, i0) => {
      return common_vendor.e({
        a: common_vendor.t(c.couponName || "优惠券"),
        b: common_vendor.t($options.statusText(c.status)),
        c: common_vendor.n($options.statusClass(c.status)),
        d: c.receiveTime
      }, c.receiveTime ? {
        e: common_vendor.t(c.receiveTime)
      } : {}, {
        f: c.expireTime
      }, c.expireTime ? {
        g: common_vendor.t(c.expireTime)
      } : {}, {
        h: c.id
      });
    })
  }, {
    b: !$data.coupons.length
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-cedcd47e"]]);
wx.createPage(MiniProgramPage);
