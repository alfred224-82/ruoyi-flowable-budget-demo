"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const utils_format = require("../../../utils/format.js");
const utils_themeMixin = require("../../../utils/themeMixin.js");
const utils_themeIcons = require("../../../utils/themeIcons.js");
const MicroTabbar = () => "../../../components/micro-tabbar/micro-tabbar.js";
const _sfc_main = {
  components: { MicroTabbar },
  mixins: [utils_themeMixin.themeMixin],
  data() {
    return { loaded: false, code: null, broughtCount: 0, myCoupons: [], qrcodeUrl: "", h5Url: "" };
  },
  onShow() {
    this.loadMyPromotion();
    this.loadQrcode();
  },
  methods: {
    toHttps: utils_format.toHttps,
    iconUrl: utils_themeIcons.iconUrl,
    /** ★H5推广码：后端生成 H5 URL 的二维码（base64 图片） */
    async loadQrcode() {
      try {
        const res = await utils_request.get(config_index.config.api.microPromoMyQrcode);
        const data = res && res.data || {};
        this.qrcodeUrl = data.qrcodeUrl || "";
        this.h5Url = data.h5Url || "";
      } catch (e) {
        this.qrcodeUrl = "";
      }
    },
    async loadMyPromotion() {
      this.loaded = false;
      try {
        const res = await utils_request.get(config_index.config.api.microPromoMy);
        const data = res && res.data || {};
        this.code = data.code || null;
        this.broughtCount = data.broughtCount || 0;
        this.myCoupons = data.myCoupons || [];
      } catch (e) {
      }
      this.loaded = true;
    },
    async generate() {
      try {
        const res = await utils_request.post(config_index.config.api.microPromoGenerate + "?codeType=personal");
        this.code = res && res.data || null;
      } catch (e) {
        common_vendor.index.showToast({ title: "生成失败", icon: "none" });
      }
    }
  }
};
if (!Array) {
  const _easycom_micro_tabbar2 = common_vendor.resolveComponent("micro-tabbar");
  _easycom_micro_tabbar2();
}
const _easycom_micro_tabbar = () => "../../../components/micro-tabbar/micro-tabbar.js";
if (!Math) {
  _easycom_micro_tabbar();
}
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    b: $options.iconUrl("hand-bubble")
  } : {}, {
    c: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    d: $options.iconUrl("hand-sparkle")
  } : {}, {
    e: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    f: $options.iconUrl("hand-bubble")
  } : {}, {
    g: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    h: $options.iconUrl("hand-star")
  } : {}, {
    i: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    j: $options.iconUrl("hand-cloud")
  } : {}, {
    k: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    l: $options.iconUrl("hand-star")
  } : {}, {
    m: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    n: $options.iconUrl("hand-sparkle")
  } : {}, {
    o: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    p: $options.iconUrl("hand-cloud")
  } : {}, {
    q: !$data.loaded
  }, !$data.loaded ? {} : !$data.code ? {
    s: common_vendor.o((...args) => $options.generate && $options.generate(...args))
  } : common_vendor.e({
    t: $data.qrcodeUrl
  }, $data.qrcodeUrl ? {
    v: $options.toHttps($data.qrcodeUrl)
  } : {}, {
    w: common_vendor.t($data.code.status === "0" ? "启用中" : "已禁用"),
    x: common_vendor.n($data.code.status === "0" ? "on" : "off")
  }), {
    r: !$data.code,
    y: $data.code
  }, $data.code ? {
    z: common_vendor.t($data.broughtCount)
  } : {}, {
    A: $data.code && $data.myCoupons.length
  }, $data.code && $data.myCoupons.length ? {
    B: common_vendor.f($data.myCoupons, (c, k0, i0) => {
      return {
        a: common_vendor.t(c.couponName),
        b: common_vendor.t(c.status === "USED" ? "已使用" : "未使用"),
        c: c.id
      };
    })
  } : {}, {
    C: common_vendor.p({
      current: "/pages/micro/customer/promo"
    }),
    D: common_vendor.n(_ctx.themeClass),
    E: common_vendor.s(_ctx.pageStyle)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-b2d4fa10"]]);
wx.createPage(MiniProgramPage);
