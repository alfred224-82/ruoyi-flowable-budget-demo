"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_roles = require("../../utils/roles.js");
const _sfc_main = {
  name: "MicroTabbar",
  props: {
    // 当前页面路径（如 /pages/micro/bossHome/index），用于高亮选中 Tab
    current: { type: String, default: "" }
  },
  data() {
    return { tabs: [] };
  },
  created() {
    this.tabs = (utils_roles.getMicroTabConfig().tabs || []).filter((t) => t.page);
  },
  methods: {
    go(tab) {
      if (tab.page === this.current) return;
      common_vendor.index.reLaunch({ url: tab.page });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: common_vendor.f($data.tabs, (tab, k0, i0) => {
      return {
        a: common_vendor.t(tab.icon),
        b: common_vendor.t(tab.text),
        c: tab.index,
        d: tab.page === $props.current ? 1 : "",
        e: common_vendor.o(($event) => $options.go(tab), tab.index)
      };
    })
  };
}
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-30e0307a"]]);
wx.createComponent(Component);
