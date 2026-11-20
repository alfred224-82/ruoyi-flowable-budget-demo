"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_auth = require("../../../utils/auth.js");
const utils_request = require("../../../utils/request.js");
const utils_format = require("../../../utils/format.js");
const config_index = require("../../../config/index.js");
const utils_themeMixin = require("../../../utils/themeMixin.js");
const MicroTabbar = () => "../../../components/micro-tabbar/micro-tabbar.js";
const _sfc_main = {
  components: { MicroTabbar },
  mixins: [utils_themeMixin.themeMixin],
  data() {
    return { user: {} };
  },
  onShow() {
    if (!utils_auth.getToken()) {
      common_vendor.index.reLaunch({ url: "/pages/login/index" });
      return;
    }
    this.user = utils_auth.getUser();
    this.loadProfile();
  },
  methods: {
    toHttps: utils_format.toHttps,
    avatarSize: utils_format.avatarSize,
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
    go(url) {
      common_vendor.index.navigateTo({ url });
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
    a: $data.user.avatar
  }, $data.user.avatar ? {
    b: $options.avatarSize($data.user.avatar, "sm"),
    c: common_vendor.o(($event) => $data.user.avatar = "")
  } : {
    d: common_vendor.t(($data.user.nickName || "业").charAt(0))
  }, {
    e: common_vendor.t($data.user.nickName || "-"),
    f: common_vendor.t($data.user.phonenumber || "-"),
    g: common_vendor.o(($event) => $options.go("/pages/micro/account/settings")),
    h: common_vendor.o(($event) => $options.go("/pages/micro/staffBook/list")),
    i: common_vendor.o((...args) => $options.logout && $options.logout(...args)),
    j: common_vendor.p({
      current: "/pages/micro/staffMine/index"
    }),
    k: common_vendor.n(_ctx.themeClass),
    l: common_vendor.s(_ctx.pageStyle)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-020a8211"]]);
wx.createPage(MiniProgramPage);
