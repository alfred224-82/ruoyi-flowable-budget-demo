"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const _sfc_main = {
  data() {
    return {
      isEdit: false,
      form: { name: "", category: "other", categoryName: "其他", amount: "", isRecurring: 1, effectiveMonth: "", remark: "" },
      categories: [
        { label: "房租", value: "rent" },
        { label: "水电", value: "utility" },
        { label: "员工工资", value: "staff_salary" },
        { label: "员工提成", value: "staff_reward" },
        { label: "其他", value: "other" }
      ]
    };
  },
  onLoad(options) {
    if (options.id) {
      this.isEdit = true;
      this.loadDetail(options.id);
    }
  },
  methods: {
    async loadDetail(id) {
      try {
        const res = await utils_request.get(config_index.config.api.microCostList + "/" + id);
        if (res && res.data) {
          this.form = { ...res.data };
        }
      } catch (e) {
      }
    },
    onCategoryChange(e) {
      const item = this.categories[e.detail.value];
      this.form.category = item.value;
      this.form.categoryName = item.label;
    },
    onMonthChange(e) {
      this.form.effectiveMonth = e.detail.value;
    },
    onRecurringChange(e) {
      this.form.isRecurring = e.detail.value ? 1 : 0;
    },
    async handleSave() {
      if (!this.form.name) return common_vendor.index.showToast({ title: "请输入名称", icon: "none" });
      if (!this.form.amount) return common_vendor.index.showToast({ title: "请输入金额", icon: "none" });
      if (!this.form.effectiveMonth) return common_vendor.index.showToast({ title: "请选择月份", icon: "none" });
      try {
        if (this.isEdit) {
          await utils_request.put(config_index.config.api.microCostUpdate, this.form);
        } else {
          await utils_request.post(config_index.config.api.microCostAdd, this.form);
        }
        common_vendor.index.showToast({ title: "保存成功" });
        setTimeout(() => common_vendor.index.navigateBack(), 500);
      } catch (e) {
        common_vendor.index.showToast({ title: "保存失败", icon: "none" });
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: $data.form.name,
    b: common_vendor.o(($event) => $data.form.name = $event.detail.value),
    c: common_vendor.t($data.form.categoryName || "请选择分类"),
    d: $data.categories,
    e: common_vendor.o((...args) => $options.onCategoryChange && $options.onCategoryChange(...args)),
    f: $data.form.amount,
    g: common_vendor.o(($event) => $data.form.amount = $event.detail.value),
    h: common_vendor.t($data.form.effectiveMonth || "请选择月份"),
    i: $data.form.effectiveMonth,
    j: common_vendor.o((...args) => $options.onMonthChange && $options.onMonthChange(...args)),
    k: $data.form.isRecurring === 1,
    l: common_vendor.o((...args) => $options.onRecurringChange && $options.onRecurringChange(...args)),
    m: $data.form.remark,
    n: common_vendor.o(($event) => $data.form.remark = $event.detail.value),
    o: common_vendor.o((...args) => $options.handleSave && $options.handleSave(...args))
  };
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-156d4bcc"]]);
wx.createPage(MiniProgramPage);
