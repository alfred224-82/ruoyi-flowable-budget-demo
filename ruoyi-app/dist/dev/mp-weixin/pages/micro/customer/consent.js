"use strict";
const common_vendor = require("../../../common/vendor.js");
const _sfc_main = {
  data() {
    return {
      agreed: false
    };
  },
  onLoad() {
    this.agreed = !!common_vendor.index.getStorageSync("voice_consent_agreed");
  },
  methods: {
    handleAgree() {
      common_vendor.index.setStorageSync("voice_consent_agreed", true);
      this.agreed = true;
      common_vendor.index.showToast({ title: "已确认知情同意", icon: "success" });
      setTimeout(() => {
        common_vendor.index.navigateBack();
      }, 800);
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: !$data.agreed
  }, !$data.agreed ? {
    b: common_vendor.o((...args) => $options.handleAgree && $options.handleAgree(...args))
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-94f8bbd4"]]);
wx.createPage(MiniProgramPage);
