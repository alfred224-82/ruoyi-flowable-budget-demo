"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_roles = require("../../utils/roles.js");
const utils_request = require("../../utils/request.js");
const config_index = require("../../config/index.js");
const utils_format = require("../../utils/format.js");
const _sfc_main = {
  data() {
    return { role: "", ROLES: utils_roles.ROLES, messages: [] };
  },
  onShow() {
    this.role = utils_roles.getPrimaryRole();
    if (this.role === utils_roles.ROLES.FINANCE || !this.role) {
      this.loadWarnings();
    }
  },
  methods: {
    formatDateTime: utils_format.formatDateTime,
    go(url) {
      common_vendor.index.navigateTo({ url });
    },
    /** 财务：未读预警/通知（消息中心 3114 同源） */
    async loadWarnings() {
      try {
        const res = await utils_request.get(config_index.config.api.messageList, { isRead: 0, pageNum: 1, pageSize: 20 });
        this.messages = res && res.rows || [];
      } catch (e) {
      }
    },
    async readMsg(msg) {
      try {
        await utils_request.put(config_index.config.api.messageRead + "/" + msg.id);
        this.messages = this.messages.filter((m) => m.id !== msg.id);
      } catch (e) {
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: $data.role === $data.ROLES.BOSS
  }, $data.role === $data.ROLES.BOSS ? {
    b: common_vendor.o(($event) => $options.go("/pages/project/list")),
    c: common_vendor.o(($event) => $options.go("/pages/approval/list"))
  } : $data.role === $data.ROLES.MANAGER ? {
    e: common_vendor.o(($event) => $options.go("/pages/project/income")),
    f: common_vendor.o(($event) => $options.go("/pages/expense/create"))
  } : $data.role === $data.ROLES.EMPLOYEE ? {
    h: common_vendor.o(($event) => $options.go("/pages/expense/create")),
    i: common_vendor.o(($event) => $options.go("/pages/expense/supplement"))
  } : common_vendor.e({
    j: !$data.messages.length
  }, !$data.messages.length ? {} : {}, {
    k: common_vendor.f($data.messages, (msg, k0, i0) => {
      return {
        a: common_vendor.t(msg.title),
        b: common_vendor.t(msg.content),
        c: common_vendor.t($options.formatDateTime(msg.createTime)),
        d: msg.id,
        e: common_vendor.o(($event) => $options.readMsg(msg), msg.id)
      };
    })
  }), {
    d: $data.role === $data.ROLES.MANAGER,
    g: $data.role === $data.ROLES.EMPLOYEE
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-bf33f812"]]);
wx.createPage(MiniProgramPage);
