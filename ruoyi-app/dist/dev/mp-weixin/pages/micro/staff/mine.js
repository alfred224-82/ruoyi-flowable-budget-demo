"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_auth = require("../../../utils/auth.js");
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const _sfc_main = {
  data() {
    return { nickName: "", roleLabel: "业务员", unreadCount: 0 };
  },
  computed: {
    initials() {
      return (this.nickName || "业").substring(0, 1);
    }
  },
  onShow() {
    if (!utils_auth.getToken()) {
      common_vendor.index.reLaunch({ url: "/pages/login/index" });
      return;
    }
    this.loadInfo();
  },
  methods: {
    async loadInfo() {
      try {
        const res = await utils_request.get(config_index.config.api.microProfile);
        const data = res && res.data || {};
        this.nickName = data.nickName || "";
      } catch (e) {
      }
      try {
        const res = await utils_request.get(config_index.config.api.messageUnread);
        this.unreadCount = res && res.data || 0;
      } catch (e) {
      }
    },
    goIncome() {
      common_vendor.index.navigateTo({ url: "/pages/micro/staffIncome/index" });
    },
    goPromo() {
      common_vendor.index.navigateTo({ url: "/pages/micro/staff/promo" });
    },
    goCustomers() {
      common_vendor.index.navigateTo({ url: "/pages/micro/staff/customers" });
    },
    goMessages() {
      common_vendor.index.showToast({ title: "消息中心", icon: "none" });
    },
    handleLogout() {
      common_vendor.index.showModal({
        title: "提示",
        content: "确定退出登录？",
        success: (res) => {
          if (res.confirm) {
            utils_auth.removeToken();
            common_vendor.index.reLaunch({ url: "/pages/login/index" });
          }
        }
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.t($options.initials),
    b: common_vendor.t($data.nickName || "业务员"),
    c: common_vendor.t($data.roleLabel),
    d: common_vendor.o((...args) => $options.goIncome && $options.goIncome(...args)),
    e: common_vendor.o((...args) => $options.goPromo && $options.goPromo(...args)),
    f: common_vendor.o((...args) => $options.goCustomers && $options.goCustomers(...args)),
    g: $data.unreadCount > 0
  }, $data.unreadCount > 0 ? {
    h: common_vendor.t($data.unreadCount)
  } : {}, {
    i: common_vendor.o((...args) => $options.goMessages && $options.goMessages(...args)),
    j: common_vendor.o((...args) => $options.handleLogout && $options.handleLogout(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-3aa6ae23"]]);
wx.createPage(MiniProgramPage);
