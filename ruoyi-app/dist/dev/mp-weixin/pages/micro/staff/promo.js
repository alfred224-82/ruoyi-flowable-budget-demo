"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const utils_format = require("../../../utils/format.js");
const utils_themeMixin = require("../../../utils/themeMixin.js");
const MicroTabbar = () => "../../../components/micro-tabbar/micro-tabbar.js";
const _sfc_main = {
  components: { MicroTabbar },
  mixins: [utils_themeMixin.themeMixin],
  data() {
    return { loaded: false, code: null, broughtCount: 0, rewardRecords: [], qrcodeUrl: "", h5Url: "" };
  },
  onShow() {
    this.loadMyPromotion();
    this.loadQrcode();
  },
  methods: {
    toHttps: utils_format.toHttps,
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
        this.rewardRecords = data.rewardRecords || [];
      } catch (e) {
      }
      this.loaded = true;
    },
    async generate() {
      try {
        const res = await utils_request.post(config_index.config.api.microPromoGenerate + "?codeType=personal");
        this.code = res && res.data || null;
        await this.loadQrcode();
      } catch (e) {
        common_vendor.index.showToast({ title: "生成失败", icon: "none" });
      }
    },
    /** 重新生成推广码（覆盖旧码，修复 t=null 等存量问题） */
    async regenerate() {
      common_vendor.index.showModal({
        title: "重新生成",
        content: "重新生成后旧推广码将失效，确定继续？",
        success: async (res) => {
          if (res.confirm) {
            await this.generate();
            common_vendor.index.showToast({ title: "已重新生成", icon: "success" });
          }
        }
      });
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
    a: !$data.loaded
  }, !$data.loaded ? {} : !$data.code ? {
    c: common_vendor.o((...args) => $options.generate && $options.generate(...args))
  } : common_vendor.e({
    d: $data.qrcodeUrl
  }, $data.qrcodeUrl ? {
    e: $options.toHttps($data.qrcodeUrl)
  } : {}, {
    f: common_vendor.t($data.code.status === "0" ? "启用中" : "已禁用"),
    g: common_vendor.n($data.code.status === "0" ? "on" : "off"),
    h: common_vendor.t($data.code.createTime),
    i: common_vendor.o((...args) => $options.regenerate && $options.regenerate(...args))
  }), {
    b: !$data.code,
    j: $data.code
  }, $data.code ? {
    k: common_vendor.t($data.broughtCount)
  } : {}, {
    l: $data.code && $data.rewardRecords.length
  }, $data.code && $data.rewardRecords.length ? {
    m: common_vendor.f($data.rewardRecords, (r, k0, i0) => {
      return {
        a: common_vendor.t(r.rewardType === "cash" ? "现金" : "优惠券"),
        b: common_vendor.t(r.rewardAmount || "-"),
        c: common_vendor.t(r.issueStatus === "PAID_OFFLINE" ? "已发放" : "待发放"),
        d: r.id
      };
    })
  } : {}, {
    n: common_vendor.p({
      current: "/pages/micro/staff/promo"
    }),
    o: common_vendor.n(_ctx.themeClass),
    p: common_vendor.s(_ctx.pageStyle)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-6133f5f7"]]);
wx.createPage(MiniProgramPage);
