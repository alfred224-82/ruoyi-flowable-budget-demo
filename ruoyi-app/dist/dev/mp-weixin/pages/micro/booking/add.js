"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_auth = require("../../../utils/auth.js");
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const utils_format = require("../../../utils/format.js");
const _sfc_main = {
  data() {
    return {
      submitting: false,
      today: utils_format.today(),
      form: { customerName: "", customerPhone: "", serviceItemId: null, bookingTime: "", amount: "", remark: "" },
      services: [],
      serviceIndex: 0
    };
  },
  computed: {
    serviceNames() {
      return this.services.map((s) => s.itemName || s.name || "-");
    }
  },
  onShow() {
    if (!utils_auth.getToken()) {
      common_vendor.index.reLaunch({ url: "/pages/login/index" });
      return;
    }
    this.loadServices();
  },
  methods: {
    async loadServices() {
      try {
        const res = await utils_request.get(config_index.config.api.microServiceOnSale);
        this.services = res && res.data || [];
      } catch (e) {
        this.services = [];
      }
    },
    onServiceChange(e) {
      this.serviceIndex = e.detail.value;
      const s = this.services[this.serviceIndex];
      if (s) {
        this.form.serviceItemId = s.id;
        if (!this.form.amount) this.form.amount = s.price || "";
      }
    },
    onDateChange(e) {
      this.form.bookingTime = e.detail.value;
    },
    async submit() {
      if (!this.form.customerName) {
        common_vendor.index.showToast({ title: "请输入顾客姓名", icon: "none" });
        return;
      }
      if (!this.form.bookingTime) {
        common_vendor.index.showToast({ title: "请选择预约时间", icon: "none" });
        return;
      }
      this.submitting = true;
      try {
        await utils_request.post(config_index.config.api.microBookingAdd, this.form);
        common_vendor.index.showToast({ title: "预约成功", icon: "success" });
        setTimeout(() => common_vendor.index.navigateBack(), 1500);
      } catch (e) {
      } finally {
        this.submitting = false;
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: $data.form.customerName,
    b: common_vendor.o(($event) => $data.form.customerName = $event.detail.value),
    c: $data.form.customerPhone,
    d: common_vendor.o(($event) => $data.form.customerPhone = $event.detail.value),
    e: common_vendor.t($data.form.serviceItemId ? $options.serviceNames[$data.serviceIndex] : "请选择服务项目"),
    f: $options.serviceNames,
    g: common_vendor.o((...args) => $options.onServiceChange && $options.onServiceChange(...args)),
    h: common_vendor.t($data.form.bookingTime || "请选择日期"),
    i: $data.today,
    j: common_vendor.o((...args) => $options.onDateChange && $options.onDateChange(...args)),
    k: $data.form.amount,
    l: common_vendor.o(($event) => $data.form.amount = $event.detail.value),
    m: $data.form.remark,
    n: common_vendor.o(($event) => $data.form.remark = $event.detail.value),
    o: $data.submitting,
    p: common_vendor.o((...args) => $options.submit && $options.submit(...args))
  };
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-6ab23ceb"]]);
wx.createPage(MiniProgramPage);
