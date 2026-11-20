"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_permission = require("../../utils/permission.js");
const utils_request = require("../../utils/request.js");
const config_index = require("../../config/index.js");
const utils_features = require("../../utils/features.js");
const utils_format = require("../../utils/format.js");
const FACE_THRESHOLD = 2e3;
const CLOCK_AMOUNT = 500;
const _sfc_main = {
  data() {
    return {
      itemCategories: [],
      purposeTags: [],
      itemCategoryText: "",
      purposeTagText: "",
      projects: [],
      selectedProject: null,
      showProjectPicker: false,
      submitting: false,
      clockInfo: null,
      // 关联规则：REQUIRED=硬规则 / OPTIONAL=软提示 / NO_PROMPT=不打扰
      linkRule: null,
      featClock: utils_features.hasFeature("FEAT_TIME_CLOCK"),
      featFace: utils_features.hasFeature("FEAT_FACE_RECOGNITION"),
      form: {
        amount: "",
        expenseDate: utils_format.today(),
        expensePeriod: utils_format.currentPeriod(),
        itemCategory: "",
        purposeTag: "",
        projectId: 0,
        invoiceDefer: 0,
        remark: ""
      }
    };
  },
  computed: {
    /** 硬规则：必须关联项目（角色级 REQUIRED 或用途标签要求项目） */
    projectRequired() {
      const tag = this.purposeTags.find((t) => t.tagCode === this.form.purposeTag);
      return this.linkRule && this.linkRule.rule === "REQUIRED" || !!tag && tag.requireProject === 1;
    },
    /** 软提示：建议关联项目（大类级 OPTIONAL） */
    projectOptional() {
      return !this.projectRequired && !!this.linkRule && this.linkRule.rule === "OPTIONAL";
    },
    /** 打卡必要性预判：金额≥500（完整规则含差旅招待/历史异常，后端提交时兜底校验） */
    clockRequired() {
      return Number(this.form.amount || 0) >= CLOCK_AMOUNT;
    },
    /** 大额人脸触发（≥2000，T7.2） */
    faceTriggered() {
      return Number(this.form.amount || 0) >= FACE_THRESHOLD;
    },
    deferDeadline() {
      return utils_format.addDays(utils_format.today(), 7);
    }
  },
  onLoad() {
    if (!utils_permission.guardPage("/pages/expense/create")) return;
    this.loadOptions();
  },
  methods: {
    /** 并行加载：物资小类字典 / 用途标签 / 进行中项目 / 项目关联规则 */
    async loadOptions() {
      try {
        const [dict, tags, projs, rule] = await Promise.all([
          utils_request.get(config_index.config.api.dictData + "/fee_item_category").catch(() => ({ data: [] })),
          utils_request.get(config_index.config.api.purposeTagAll).catch(() => ({ data: [] })),
          utils_request.get(config_index.config.api.projectActiveList).catch(() => ({ data: [] })),
          utils_request.get(config_index.config.api.linkRuleMatch).catch(() => ({ data: null }))
        ]);
        this.itemCategories = dict.data || [];
        this.purposeTags = tags.data || [];
        this.projects = projs.data || [];
        this.linkRule = rule.data || null;
        if (this.linkRule && this.linkRule.rule === "REQUIRED" && this.linkRule.scope === "PROJECT" && this.linkRule.target) {
          const hit = this.projects.find((p) => String(p.id) === String(this.linkRule.target));
          if (hit) this.chooseProject(hit);
        }
      } catch (e) {
      }
    },
    onAmountChange() {
      const num = Number(this.form.amount);
      this.form.amount = isNaN(num) ? "" : num.toFixed(2);
    },
    /** 费用日期变更联动报销年月（对应预算编制期间） */
    onDateChange(date) {
      this.form.expenseDate = date;
      this.form.expensePeriod = date.substring(0, 7);
    },
    onItemCategory(index) {
      const hit = this.itemCategories[index];
      if (hit) {
        this.form.itemCategory = hit.dictValue;
        this.itemCategoryText = hit.dictLabel;
      }
    },
    onPurposeTag(index) {
      const hit = this.purposeTags[index];
      if (hit) {
        this.form.purposeTag = hit.tagCode;
        this.purposeTagText = hit.tagName;
      }
    },
    chooseProject(p) {
      if (!p && this.projectRequired) {
        common_vendor.index.showToast({ title: "本单必须关联项目", icon: "none" });
        return;
      }
      this.form.projectId = p ? p.id : 0;
      this.selectedProject = p;
      this.showProjectPicker = false;
    },
    /** 现场打卡：获取定位（高级版，打卡记录随报销单上报） */
    doClock() {
      common_vendor.index.getLocation({
        type: "gcj02",
        success: (res) => {
          this.clockInfo = {
            longitude: res.longitude,
            latitude: res.latitude,
            accuracy: res.accuracy,
            address: "经度 " + res.longitude.toFixed(4) + " / 纬度 " + res.latitude.toFixed(4)
          };
          common_vendor.index.showToast({ title: "打卡成功", icon: "success" });
        },
        fail: () => {
          common_vendor.index.showToast({ title: "定位失败，请检查小程序位置权限", icon: "none" });
        }
      });
    },
    /** 提交前校验 */
    validate() {
      const amount = Number(this.form.amount);
      if (isNaN(amount) || amount <= 0) {
        common_vendor.index.showToast({ title: "请输入大于 0 的报销金额", icon: "none" });
        return false;
      }
      if (!this.form.itemCategory) {
        common_vendor.index.showToast({ title: "请选择物资小类", icon: "none" });
        return false;
      }
      if (!this.form.purposeTag) {
        common_vendor.index.showToast({ title: "请选择用途标签", icon: "none" });
        return false;
      }
      if (this.projectRequired && !this.form.projectId) {
        common_vendor.index.showToast({ title: "按规则必须选择所属项目", icon: "none" });
        return false;
      }
      if (this.featClock && this.clockRequired && !this.clockInfo) {
        common_vendor.index.showToast({ title: "本单需先完成现场打卡", icon: "none" });
        return false;
      }
      return true;
    },
    /** 大额人脸核身（专业版）：不一致仅标记不拦截，采集流程未就绪时提示后可继续 */
    confirmFace() {
      return new Promise((resolve) => {
        if (!this.featFace || !this.faceTriggered) return resolve(true);
        common_vendor.index.showModal({
          title: "人脸核身",
          content: "大额报销（≥2000元）需人脸核身，采集功能部署中，是否先提交？",
          confirmText: "继续提交",
          success: (res) => resolve(!!res.confirm)
        });
      });
    },
    /** 提交报销单（创建即提交，后端映射规则自动定位预算科目） */
    async onSubmit() {
      if (this.submitting || !this.validate()) return;
      if (!await this.confirmFace()) return;
      this.submitting = true;
      try {
        const body = {
          itemCategory: this.form.itemCategory,
          purposeTag: this.form.purposeTag,
          amount: Number(this.form.amount),
          expenseDate: this.form.expenseDate,
          expensePeriod: this.form.expensePeriod,
          projectId: this.form.projectId || 0,
          invoiceDefer: this.form.invoiceDefer,
          remark: this.form.remark.trim() || void 0
        };
        if (this.clockInfo) body.clockInfo = this.clockInfo;
        const res = await utils_request.post(config_index.config.api.expenseAdd, body);
        if (this.form.invoiceDefer === 1 && res.data && res.data.id) {
          try {
            await utils_request.post(config_index.config.api.supplementCreate + "?billId=" + res.data.id + "&estimatedDate=" + this.deferDeadline);
          } catch (e) {
            common_vendor.index.showToast({ title: "补票记录创建失败，请在补票页手动发起", icon: "none" });
          }
        }
        const msg = this.form.invoiceDefer === 1 ? "已提交（发票后补）" : "提交成功";
        common_vendor.index.showToast({ title: msg, icon: "success" });
        setTimeout(() => {
          common_vendor.index.switchTab({ url: "/pages/tab-work/index" });
        }, 800);
      } catch (e) {
      }
      this.submitting = false;
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.o((...args) => $options.onAmountChange && $options.onAmountChange(...args)),
    b: $data.form.amount,
    c: common_vendor.o(($event) => $data.form.amount = $event.detail.value),
    d: common_vendor.t($data.form.expenseDate),
    e: $data.form.expenseDate,
    f: common_vendor.o((e) => $options.onDateChange(e.detail.value)),
    g: common_vendor.t($data.form.expensePeriod),
    h: common_vendor.t($data.itemCategoryText || "请选择"),
    i: !$data.itemCategoryText ? 1 : "",
    j: $data.itemCategories,
    k: common_vendor.o((e) => $options.onItemCategory(e.detail.value)),
    l: common_vendor.t($data.purposeTagText || "请选择"),
    m: !$data.purposeTagText ? 1 : "",
    n: $data.purposeTags,
    o: common_vendor.o((e) => $options.onPurposeTag(e.detail.value)),
    p: $options.projectRequired
  }, $options.projectRequired ? {} : $options.projectOptional ? {} : {}, {
    q: $options.projectOptional,
    r: common_vendor.t($options.projectRequired ? "所属项目" : "所属项目"),
    s: common_vendor.t($data.selectedProject ? $data.selectedProject.projectName : "公司级（未关联项目）"),
    t: !$data.selectedProject ? 1 : "",
    v: common_vendor.o(($event) => $data.showProjectPicker = true),
    w: $data.form.invoiceDefer === 1,
    x: common_vendor.o((e) => $data.form.invoiceDefer = e.detail.value ? 1 : 0),
    y: $data.form.invoiceDefer === 1
  }, $data.form.invoiceDefer === 1 ? {
    z: common_vendor.t($options.deferDeadline)
  } : {}, {
    A: $data.featClock
  }, $data.featClock ? {} : {}, {
    B: $data.featClock
  }, $data.featClock ? common_vendor.e({
    C: !$data.clockInfo
  }, !$data.clockInfo ? {
    D: common_vendor.t($options.clockRequired ? "本单命中打卡触发规则，提交前需完成打卡" : "差旅招待/大额报销建议打卡（证据等级 medium）"),
    E: common_vendor.o((...args) => $options.doClock && $options.doClock(...args))
  } : {
    F: common_vendor.t($data.clockInfo.address || "定位成功"),
    G: common_vendor.o((...args) => $options.doClock && $options.doClock(...args))
  }) : {}, {
    H: $data.featFace && $options.faceTriggered
  }, $data.featFace && $options.faceTriggered ? {} : {}, {
    I: $data.form.remark,
    J: common_vendor.o(($event) => $data.form.remark = $event.detail.value),
    K: $data.submitting,
    L: common_vendor.o((...args) => $options.onSubmit && $options.onSubmit(...args)),
    M: $data.showProjectPicker
  }, $data.showProjectPicker ? common_vendor.e({
    N: $options.projectRequired
  }, $options.projectRequired ? {} : {}, {
    O: $data.form.projectId === 0 ? 1 : "",
    P: common_vendor.o(($event) => $options.chooseProject(null)),
    Q: !$data.projects.length
  }, !$data.projects.length ? {} : {}, {
    R: common_vendor.f($data.projects, (p, k0, i0) => {
      return {
        a: common_vendor.t(p.projectName),
        b: common_vendor.t(p.clientName || "-"),
        c: p.id,
        d: $data.form.projectId === p.id ? 1 : "",
        e: common_vendor.o(($event) => $options.chooseProject(p), p.id)
      };
    }),
    S: common_vendor.o(() => {
    }),
    T: common_vendor.o(($event) => $data.showProjectPicker = false)
  }) : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-f6a26cb5"]]);
wx.createPage(MiniProgramPage);
