"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_roles = require("../../utils/roles.js");
const utils_request = require("../../utils/request.js");
const config_index = require("../../config/index.js");
const _sfc_main = {
  data() {
    return { role: "", ROLES: utils_roles.ROLES, pendingCount: 0 };
  },
  onShow() {
    this.role = utils_roles.getPrimaryRole();
    if (this.role === utils_roles.ROLES.BOSS) {
      this.loadPending();
    }
  },
  methods: {
    go(url) {
      common_vendor.index.navigateTo({ url });
    },
    /** 老板：待审批报销单数量 */
    async loadPending() {
      try {
        const res = await utils_request.get(config_index.config.api.expensePending, { pageNum: 1, pageSize: 1 });
        this.pendingCount = res && res.total || 0;
      } catch (e) {
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: $data.role === $data.ROLES.BOSS
  }, $data.role === $data.ROLES.BOSS ? common_vendor.e({
    b: common_vendor.o(($event) => $options.go("/pages/approval/list")),
    c: $data.pendingCount > 0
  }, $data.pendingCount > 0 ? {
    d: common_vendor.t($data.pendingCount),
    e: common_vendor.o(($event) => $options.go("/pages/approval/list"))
  } : {}) : $data.role === $data.ROLES.EMPLOYEE || $data.role === $data.ROLES.MANAGER ? {
    g: common_vendor.o(($event) => $options.go("/pages/expense/mine")),
    h: common_vendor.o(($event) => $options.go("/pages/expense/supplement"))
  } : {}, {
    f: $data.role === $data.ROLES.EMPLOYEE || $data.role === $data.ROLES.MANAGER
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-bddbb054"]]);
wx.createPage(MiniProgramPage);
