"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_auth = require("../../../utils/auth.js");
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const _sfc_main = {
  data() {
    return { saving: false, form: { businessHours: "", payInfo: "" } };
  },
  onShow() {
    if (!utils_auth.getToken()) {
      common_vendor.index.reLaunch({ url: "/pages/login/index" });
      return;
    }
    this.loadConfig();
  },
  methods: {
    async loadConfig() {
      try {
        const res = await utils_request.get(config_index.config.api.microShopConfig);
        const data = res && res.data || {};
        this.form.businessHours = data.businessHours || "";
        this.form.payInfo = data.payInfo || "";
      } catch (e) {
      }
    },
    async save() {
      this.saving = true;
      try {
        await utils_request.put(config_index.config.api.microShopConfig, this.form);
        common_vendor.index.showToast({ title: "保存成功", icon: "success" });
      } catch (e) {
      } finally {
        this.saving = false;
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: $data.form.businessHours,
    b: common_vendor.o(($event) => $data.form.businessHours = $event.detail.value),
    c: $data.form.payInfo,
    d: common_vendor.o(($event) => $data.form.payInfo = $event.detail.value),
    e: $data.saving,
    f: common_vendor.o((...args) => $options.save && $options.save(...args))
  };
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-d6ae4fb5"]]);
wx.createPage(MiniProgramPage);
