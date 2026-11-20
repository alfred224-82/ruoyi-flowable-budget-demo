"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_auth = require("../../../utils/auth.js");
const utils_request = require("../../../utils/request.js");
const utils_format = require("../../../utils/format.js");
const config_index = require("../../../config/index.js");
const utils_themeMixin = require("../../../utils/themeMixin.js");
const utils_themeIcons = require("../../../utils/themeIcons.js");
const MicroTabbar = () => "../../../components/micro-tabbar/micro-tabbar.js";
const _sfc_main = {
  components: { MicroTabbar },
  mixins: [utils_themeMixin.themeMixin],
  data() {
    return { user: {}, payInfo: "" };
  },
  onShow() {
    if (!utils_auth.getToken()) {
      common_vendor.index.reLaunch({ url: "/pages/login/index" });
      return;
    }
    this.user = utils_auth.getUser();
    this.loadProfile();
    this.loadPayInfo();
  },
  methods: {
    toHttps: utils_format.toHttps,
    avatarSize: utils_format.avatarSize,
    iconUrl: utils_themeIcons.iconUrl,
    go(url) {
      common_vendor.index.navigateTo({ url });
    },
    /** ★昵称/头像以 /micro/profile 为准刷新（手机号一键登录流程不写用户缓存，仅读缓存会缺头像） */
    async loadProfile() {
      try {
        const res = await utils_request.get(config_index.config.api.microProfile);
        const data = res && res.data || {};
        this.user = Object.assign({}, this.user, { nickName: data.nickName || this.user.nickName, avatar: data.avatar || "", phonenumber: data.phonenumber || this.user.phonenumber });
        utils_auth.setUser(this.user);
      } catch (e) {
      }
    },
    async loadPayInfo() {
      try {
        const res = await utils_request.get(config_index.config.api.microCustomerPayInfo);
        this.payInfo = res && res.data || "";
      } catch (e) {
      }
    },
    logout() {
      common_vendor.index.showModal({ title: "提示", content: "确认退出登录？", success: (r) => {
        if (r.confirm) utils_auth.clearAuth();
      } });
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
    b: $options.iconUrl("hand-star")
  } : {}, {
    c: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    d: $options.iconUrl("hand-bubble")
  } : {}, {
    e: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    f: $options.iconUrl("hand-sparkle")
  } : {}, {
    g: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    h: $options.iconUrl("hand-cloud")
  } : {}, {
    i: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    j: $options.iconUrl("hand-sparkle")
  } : {}, {
    k: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    l: $options.iconUrl("hand-star")
  } : {}, {
    m: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    n: $options.iconUrl("hand-star")
  } : {}, {
    o: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    p: $options.iconUrl("hand-sparkle")
  } : {}, {
    q: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    r: $options.iconUrl("hand-cloud")
  } : {}, {
    s: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    t: $options.iconUrl("hand-cloud")
  } : {}, {
    v: $data.user.avatar
  }, $data.user.avatar ? {
    w: $options.avatarSize($data.user.avatar, "sm"),
    x: common_vendor.o(($event) => $data.user.avatar = "")
  } : {
    y: common_vendor.t(($data.user.nickName || "客").charAt(0))
  }, {
    z: common_vendor.t($data.user.nickName || "-"),
    A: common_vendor.t($data.payInfo || "本店支持到店付款，服务完成后请向前台或老板付款。本小程序不提供在线支付。"),
    B: common_vendor.o(($event) => $options.go("/pages/micro/customerBook/list")),
    C: common_vendor.o(($event) => $options.go("/pages/micro/account/settings")),
    D: common_vendor.o(($event) => $options.go("/pages/micro/complaint/index")),
    E: common_vendor.o((...args) => $options.logout && $options.logout(...args)),
    F: common_vendor.p({
      current: "/pages/micro/customerMine/index"
    }),
    G: common_vendor.n(_ctx.themeClass),
    H: common_vendor.s(_ctx.pageStyle)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-20c5f713"]]);
wx.createPage(MiniProgramPage);
