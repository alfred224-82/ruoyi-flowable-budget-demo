"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_auth = require("../../../utils/auth.js");
const utils_request = require("../../../utils/request.js");
const utils_format = require("../../../utils/format.js");
const utils_roles = require("../../../utils/roles.js");
const utils_store = require("../../../utils/store.js");
const config_index = require("../../../config/index.js");
const _sfc_main = {
  data() {
    return { user: {}, messages: [], unreadCount: 0, isMicro: false };
  },
  onShow() {
    if (!utils_auth.getToken()) {
      common_vendor.index.reLaunch({ url: "/pages/login/index" });
      return;
    }
    this.user = utils_auth.getUser();
    this.isMicro = utils_roles.isMicroVersion();
    this.loadProfile();
    this.loadMessages();
  },
  methods: {
    toHttps: utils_format.toHttps,
    avatarSize: utils_format.avatarSize,
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
    /** 切换租户（T29.4）：二次确认 → 清除租户号/门店/门店候选/多租户上下文/登录态 → 回租户号输入页 */
    switchTenantNo() {
      common_vendor.index.showModal({
        title: "切换租户",
        content: "切换租户需清除当前登录并重新输入租户号，是否继续？",
        success: (r) => {
          if (!r.confirm) return;
          utils_store.clearTenantNo();
          utils_store.clearSelectedStore();
          utils_store.clearTenantStoreCandidates();
          utils_roles.clearTenantContext();
          utils_auth.clearAuthTo("/pages/micro/tenant/input");
        }
      });
    },
    async loadMessages() {
      try {
        const [listRes, unreadRes] = await Promise.all([
          utils_request.get(config_index.config.api.microNotifyList, { pageNum: 1, pageSize: 10 }),
          utils_request.get(config_index.config.api.microNotifyUnread)
        ]);
        this.messages = listRes && listRes.rows || [];
        this.unreadCount = unreadRes && unreadRes.data || 0;
      } catch (e) {
      }
    },
    async readMsg(msg) {
      if (!msg.isRead) {
        try {
          await utils_request.put(config_index.config.api.microNotifyRead + "/" + msg.id);
          msg.isRead = 1;
          this.unreadCount = Math.max(0, this.unreadCount - 1);
        } catch (e) {
        }
      }
    },
    logout() {
      common_vendor.index.showModal({
        title: "提示",
        content: "确认退出登录？",
        success: (res) => {
          if (res.confirm) utils_auth.clearAuth();
        }
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: $data.user.avatar
  }, $data.user.avatar ? {
    b: $options.avatarSize($data.user.avatar, "sm"),
    c: common_vendor.o(($event) => $data.user.avatar = "")
  } : {
    d: common_vendor.t(($data.user.nickName || "老").charAt(0))
  }, {
    e: common_vendor.t($data.user.nickName || "-"),
    f: !$data.messages.length
  }, !$data.messages.length ? {} : {
    g: common_vendor.f($data.messages, (msg, k0, i0) => {
      return {
        a: common_vendor.t(msg.title),
        b: common_vendor.t(msg.createTime),
        c: msg.id,
        d: !msg.isRead ? 1 : "",
        e: common_vendor.o(($event) => $options.readMsg(msg), msg.id)
      };
    })
  }, {
    h: $data.unreadCount > 0
  }, $data.unreadCount > 0 ? {
    i: common_vendor.t($data.unreadCount)
  } : {}, {
    j: common_vendor.o(($event) => $options.go("/pages/micro/shop/config")),
    k: $data.isMicro
  }, $data.isMicro ? {
    l: common_vendor.o((...args) => $options.switchTenantNo && $options.switchTenantNo(...args))
  } : {}, {
    m: common_vendor.o(($event) => $options.go("/pages/micro/account/settings")),
    n: common_vendor.o((...args) => $options.logout && $options.logout(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-350930af"]]);
wx.createPage(MiniProgramPage);
