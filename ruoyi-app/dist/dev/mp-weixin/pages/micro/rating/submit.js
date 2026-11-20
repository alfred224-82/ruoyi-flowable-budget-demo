"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const _sfc_main = {
  data() {
    return {
      rating: 5,
      content: "",
      scene: "booking_complete",
      targetUserId: null,
      orderId: null,
      starLabels: ["很差", "较差", "一般", "满意", "非常满意"]
    };
  },
  computed: {
    canSubmit() {
      return this.rating >= 1 && this.rating <= 5 && this.content.trim().length > 0 && this.targetUserId;
    }
  },
  onLoad(options) {
    this.targetUserId = options.targetUserId || null;
    this.orderId = options.orderId || null;
    if (options.scene) this.scene = options.scene;
  },
  methods: {
    async submitRating() {
      if (!this.canSubmit) return;
      try {
        await utils_request.post(config_index.config.api.microRatingSubmit, {
          targetUserId: this.targetUserId,
          rating: this.rating,
          content: this.content.trim(),
          scene: this.scene,
          orderId: this.orderId
        });
        common_vendor.index.showToast({ title: "评价成功", icon: "success" });
        setTimeout(() => common_vendor.index.navigateBack(), 1500);
      } catch (e) {
        common_vendor.index.showToast({ title: e && e.msg || "评价失败", icon: "none" });
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: common_vendor.f(5, (n, k0, i0) => {
      return {
        a: n,
        b: n <= $data.rating ? 1 : "",
        c: common_vendor.o(($event) => $data.rating = n, n)
      };
    }),
    b: common_vendor.t($data.starLabels[$data.rating - 1] || "请选择评分"),
    c: $data.content,
    d: common_vendor.o(($event) => $data.content = $event.detail.value),
    e: $data.scene === "booking_complete" ? 1 : "",
    f: common_vendor.o(($event) => $data.scene = "booking_complete"),
    g: $data.scene === "service_done" ? 1 : "",
    h: common_vendor.o(($event) => $data.scene = "service_done"),
    i: $data.scene === "visit_done" ? 1 : "",
    j: common_vendor.o(($event) => $data.scene = "visit_done"),
    k: !$options.canSubmit,
    l: common_vendor.o((...args) => $options.submitRating && $options.submitRating(...args))
  };
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-e87b8156"]]);
wx.createPage(MiniProgramPage);
