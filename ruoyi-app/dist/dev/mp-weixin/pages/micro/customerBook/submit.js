"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const utils_format = require("../../../utils/format.js");
const _sfc_main = {
  data() {
    return {
      submitting: false,
      today: utils_format.today(),
      serviceName: "",
      price: 0,
      // T15.32 防爽约：黑名单状态（onLoad 查询 myCredit 回填）
      creditBlocked: false,
      creditMessage: "",
      form: { serviceItemId: null, customerName: "", customerPhone: "", bookingTime: "", remark: "" }
    };
  },
  onLoad(options) {
    this.form.serviceItemId = options.serviceId || null;
    this.serviceName = decodeURIComponent(options.serviceName || "");
    this.price = options.price || 0;
    this.checkCredit();
  },
  methods: {
    onDateChange(e) {
      this.form.bookingTime = e.detail.value;
    },
    /** T15.32 查询当前顾客信誉状态：拉黑则按钮置灰禁止提交（后端 submitBook 二次校验兼底） */
    async checkCredit() {
      try {
        const res = await utils_request.get(config_index.config.api.microCustomerMyCredit);
        const data = res.data || {};
        this.creditBlocked = !!data.blocked;
        this.creditMessage = data.message || "";
      } catch (e) {
        this.creditBlocked = false;
      }
    },
    async submit() {
      if (this.creditBlocked) {
        common_vendor.index.showToast({ title: this.creditMessage || "您已被限制预约", icon: "none" });
        return;
      }
      if (!this.form.customerName) {
        common_vendor.index.showToast({ title: "请输入姓名", icon: "none" });
        return;
      }
      if (!this.form.customerPhone) {
        common_vendor.index.showToast({ title: "请输入手机号", icon: "none" });
        return;
      }
      if (!this.form.bookingTime) {
        common_vendor.index.showToast({ title: "请选择预约日期", icon: "none" });
        return;
      }
      this.submitting = true;
      try {
        await utils_request.post(config_index.config.api.microCustomerSubmitBook, this.form);
        common_vendor.index.showToast({ title: "预约成功", icon: "success" });
        setTimeout(() => common_vendor.index.switchTab({ url: "/pages/micro/customerBook/list" }), 1500);
      } catch (e) {
      } finally {
        this.submitting = false;
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: $data.serviceName
  }, $data.serviceName ? {
    b: common_vendor.t($data.serviceName),
    c: common_vendor.t($data.price)
  } : {}, {
    d: $data.form.customerName,
    e: common_vendor.o(($event) => $data.form.customerName = $event.detail.value),
    f: $data.form.customerPhone,
    g: common_vendor.o(($event) => $data.form.customerPhone = $event.detail.value),
    h: common_vendor.t($data.form.bookingTime || "请选择日期"),
    i: $data.today,
    j: common_vendor.o((...args) => $options.onDateChange && $options.onDateChange(...args)),
    k: $data.form.remark,
    l: common_vendor.o(($event) => $data.form.remark = $event.detail.value),
    m: $data.creditBlocked
  }, $data.creditBlocked ? {
    n: common_vendor.t($data.creditMessage)
  } : {}, {
    o: common_vendor.t($data.creditBlocked ? "已被限制预约" : "提交预约"),
    p: $data.creditBlocked ? 1 : "",
    q: $data.creditBlocked,
    r: $data.submitting,
    s: common_vendor.o((...args) => $options.submit && $options.submit(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-703f425f"]]);
wx.createPage(MiniProgramPage);
