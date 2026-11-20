"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_permission = require("../../utils/permission.js");
const utils_request = require("../../utils/request.js");
const config_index = require("../../config/index.js");
const utils_format = require("../../utils/format.js");
const BASIS_OPTIONS = [
  { label: "合同", value: "CONTRACT" },
  { label: "发票", value: "INVOICE" },
  { label: "回款单", value: "RECEIPT" },
  { label: "其他", value: "OTHER" }
];
const _sfc_main = {
  data() {
    return {
      basisOptions: BASIS_OPTIONS,
      projects: [],
      selectedProject: null,
      showPicker: false,
      submitting: false,
      form: {
        projectId: null,
        confirmedAmount: "",
        confirmDate: utils_format.today(),
        basisType: "CONTRACT",
        basisNo: ""
      }
    };
  },
  computed: {
    basisText() {
      const hit = BASIS_OPTIONS.find((b) => b.value === this.form.basisType);
      return hit ? hit.label : "-";
    }
  },
  onLoad() {
    if (!utils_permission.guardPage("/pages/project/incomeCreate")) return;
    this.loadProjects();
  },
  methods: {
    /** 进行中项目（后端按成员过滤，仅本人参与项目可选） */
    async loadProjects() {
      try {
        const res = await utils_request.get(config_index.config.api.projectActiveList);
        this.projects = res.data || [];
      } catch (e) {
      }
    },
    openProjectPicker() {
      this.showPicker = true;
    },
    chooseProject(p) {
      this.form.projectId = p.id;
      this.selectedProject = p;
      this.showPicker = false;
    },
    /** 提交（后端校验：项目必选、金额>0.01） */
    async onSubmit() {
      if (this.submitting) return;
      if (!this.form.projectId) {
        common_vendor.index.showToast({ title: "请选择所属项目", icon: "none" });
        return;
      }
      const amount = Number(this.form.confirmedAmount);
      if (isNaN(amount) || amount <= 0) {
        common_vendor.index.showToast({ title: "请输入大于 0 的金额", icon: "none" });
        return;
      }
      this.submitting = true;
      try {
        await utils_request.post(config_index.config.api.incomeAdd, {
          projectId: this.form.projectId,
          confirmedAmount: amount,
          confirmDate: this.form.confirmDate,
          basisType: this.form.basisType,
          basisNo: this.form.basisNo.trim() || void 0
        });
        common_vendor.index.showToast({ title: "已提交，待财务核验", icon: "success" });
        setTimeout(() => common_vendor.index.navigateBack(), 800);
      } catch (e) {
      }
      this.submitting = false;
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.t($data.selectedProject ? $data.selectedProject.projectName : "请选择进行中项目"),
    b: !$data.selectedProject ? 1 : "",
    c: common_vendor.o((...args) => $options.openProjectPicker && $options.openProjectPicker(...args)),
    d: $data.form.confirmedAmount,
    e: common_vendor.o(($event) => $data.form.confirmedAmount = $event.detail.value),
    f: common_vendor.t($data.form.confirmDate),
    g: $data.form.confirmDate,
    h: common_vendor.o((e) => $data.form.confirmDate = e.detail.value),
    i: common_vendor.t($options.basisText),
    j: $data.basisOptions,
    k: common_vendor.o((e) => $data.form.basisType = $data.basisOptions[e.detail.value].value),
    l: $data.form.basisNo,
    m: common_vendor.o(($event) => $data.form.basisNo = $event.detail.value),
    n: $data.submitting,
    o: common_vendor.o((...args) => $options.onSubmit && $options.onSubmit(...args)),
    p: $data.showPicker
  }, $data.showPicker ? common_vendor.e({
    q: !$data.projects.length
  }, !$data.projects.length ? {} : {}, {
    r: common_vendor.f($data.projects, (p, k0, i0) => {
      return {
        a: common_vendor.t(p.projectName),
        b: common_vendor.t(p.clientName || "-"),
        c: p.id,
        d: $data.form.projectId === p.id ? 1 : "",
        e: common_vendor.o(($event) => $options.chooseProject(p), p.id)
      };
    }),
    s: common_vendor.o(() => {
    }),
    t: common_vendor.o(($event) => $data.showPicker = false)
  }) : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-251e1ab5"]]);
wx.createPage(MiniProgramPage);
