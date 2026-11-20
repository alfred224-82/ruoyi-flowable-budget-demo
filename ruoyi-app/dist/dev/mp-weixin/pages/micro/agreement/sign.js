"use strict";
const common_vendor = require("../../../common/vendor.js");
const config_index = require("../../../config/index.js");
const _sfc_main = {
  data() {
    return {
      agreementInfo: null,
      agreed: false,
      submitting: false
    };
  },
  onLoad() {
    this.loadCurrent();
  },
  methods: {
    loadCurrent() {
      common_vendor.index.request({
        url: getApp().globalData.baseUrl + config_index.config.api.microComplianceAgreementCurrent,
        header: { "Authorization": "Bearer " + common_vendor.index.getStorageSync("token") },
        success: (res) => {
          if (res.data && res.data.code === 200) {
            this.agreementInfo = res.data.data;
            if (!this.agreementInfo.needSign) {
              setTimeout(() => {
                common_vendor.index.navigateBack();
              }, 2e3);
            }
          }
        }
      });
    },
    viewAgreement() {
      if (this.agreementInfo && this.agreementInfo.agreementContentUrl) {
        const raw = this.agreementInfo.agreementContentUrl;
        const full = /^https?:\/\//.test(raw) ? raw : getApp().globalData.baseUrl + raw;
        common_vendor.index.navigateTo({
          url: "/pages/webview/index?url=" + encodeURIComponent(full)
        });
      }
    },
    handleSign() {
      if (!this.agreed) return;
      this.submitting = true;
      common_vendor.index.request({
        url: getApp().globalData.baseUrl + config_index.config.api.microComplianceAgreementSign + "?agreementVersion=" + encodeURIComponent(this.agreementInfo.currentVersion),
        method: "POST",
        header: {
          "Authorization": "Bearer " + common_vendor.index.getStorageSync("token")
        },
        success: (res) => {
          if (res.data && res.data.code === 200) {
            common_vendor.index.showToast({ title: "签署成功", icon: "success" });
            setTimeout(() => {
              common_vendor.index.navigateBack();
            }, 1500);
          } else {
            common_vendor.index.showToast({ title: res.data.msg || "签署失败", icon: "none" });
          }
        },
        fail: () => {
          common_vendor.index.showToast({ title: "网络错误", icon: "none" });
        },
        complete: () => {
          this.submitting = false;
        }
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: $data.agreementInfo
  }, $data.agreementInfo ? common_vendor.e({
    b: common_vendor.t($data.agreementInfo.currentVersion),
    c: $data.agreementInfo.agreementContentUrl
  }, $data.agreementInfo.agreementContentUrl ? {
    d: common_vendor.o((...args) => $options.viewAgreement && $options.viewAgreement(...args))
  } : {}) : {}, {
    e: $data.agreed,
    f: common_vendor.o(($event) => $data.agreed = !$data.agreed),
    g: common_vendor.t($data.submitting ? "签署中..." : "确认签署"),
    h: !$data.agreed,
    i: $data.submitting,
    j: common_vendor.o((...args) => $options.handleSign && $options.handleSign(...args)),
    k: !$data.agreementInfo.needSign && $data.agreementInfo.signTime
  }, !$data.agreementInfo.needSign && $data.agreementInfo.signTime ? {
    l: common_vendor.t($data.agreementInfo.signTime),
    m: common_vendor.t($data.agreementInfo.signerName)
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-9524760e"]]);
wx.createPage(MiniProgramPage);
