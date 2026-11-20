"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_permission = require("../../utils/permission.js");
const utils_request = require("../../utils/request.js");
const config_index = require("../../config/index.js");
const utils_features = require("../../utils/features.js");
const utils_format = require("../../utils/format.js");
const ENDPOINT = {
  expense: {
    detail: "/fee/expense/",
    approve: config_index.config.api.expenseApprove,
    reject: config_index.config.api.expenseReject,
    method: "POST",
    pendingStatus: "SUBMITTED",
    statusField: "status"
  },
  payment: {
    detail: "/fee/payment/",
    approve: config_index.config.api.paymentApprove,
    reject: config_index.config.api.paymentReject,
    method: "PUT",
    pendingStatus: "PENDING",
    statusField: "status"
  },
  change: {
    detail: "/fee/change/",
    approve: config_index.config.api.changeApprove,
    reject: config_index.config.api.changeReject,
    method: "PUT",
    pendingStatus: "PENDING",
    statusField: "changeStatus"
  },
  project: {
    detail: "/fee/project/",
    approve: config_index.config.api.projectApprove,
    reject: config_index.config.api.projectReject,
    method: "PUT",
    pendingStatus: "PENDING_APPROVAL",
    statusField: "status"
  }
};
const EVIDENCE = { strong: "证据充分", medium: "证据一般", weak: "证据薄弱" };
const EVIDENCE_CLS = { strong: "ok", medium: "warn", weak: "danger" };
const _sfc_main = {
  data() {
    return {
      CHANGE_STATUS: utils_format.CHANGE_STATUS,
      type: "expense",
      id: null,
      data: {},
      showRejectInput: false,
      rejectReason: "",
      submitting: false,
      showEvidence: utils_features.hasFeature("FEAT_EVIDENCE_CHAIN")
    };
  },
  computed: {
    /** 仅待审批状态可操作 */
    canAudit() {
      const ep = ENDPOINT[this.type];
      return ep && this.data[ep.statusField] === ep.pendingStatus;
    },
    evidenceText() {
      return EVIDENCE[this.data.evidenceLevel] || this.data.evidenceLevel || "";
    },
    evidenceClass() {
      return EVIDENCE_CLS[this.data.evidenceLevel] || "";
    }
  },
  onLoad(options) {
    if (!utils_permission.guardPage("/pages/approval/detail")) return;
    if (!options.type || !ENDPOINT[options.type]) {
      common_vendor.index.showToast({ title: "单据类型错误", icon: "none" });
      return;
    }
    this.type = options.type;
    this.id = options.id;
    this.loadDetail();
  },
  methods: {
    formatAmount: utils_format.formatAmount,
    formatDate: utils_format.formatDate,
    statusText: utils_format.statusText,
    async loadDetail() {
      try {
        const res = await utils_request.get(ENDPOINT[this.type].detail + this.id);
        this.data = res.data || {};
      } catch (e) {
      }
    },
    /** 审批通过 */
    doApprove() {
      if (this.submitting) return;
      common_vendor.index.showModal({
        title: "审批确认",
        content: "确认通过该单据审批？",
        success: async (res) => {
          if (!res.confirm) return;
          this.submitting = true;
          try {
            await this.callAudit(ENDPOINT[this.type].approve);
            common_vendor.index.showToast({ title: "审批通过", icon: "success" });
            setTimeout(() => common_vendor.index.navigateBack(), 800);
          } catch (e) {
          }
          this.submitting = false;
        }
      });
    },
    /** 驳回（报销单原因必填 5-500 字） */
    async doReject() {
      if (this.submitting) return;
      const reason = (this.rejectReason || "").trim();
      if (this.type === "expense" && reason.length < 5) {
        common_vendor.index.showToast({ title: "驳回原因至少 5 个字", icon: "none" });
        return;
      }
      this.submitting = true;
      try {
        const url = ENDPOINT[this.type].reject + "/" + this.id + (reason ? "?reason=" + encodeURIComponent(reason) : "");
        await this.callAudit(url);
        common_vendor.index.showToast({ title: "已驳回", icon: "success" });
        setTimeout(() => common_vendor.index.navigateBack(), 800);
      } catch (e) {
      }
      this.submitting = false;
    },
    /** 按单据类型选择 POST/PUT 调用审批端点 */
    callAudit(url) {
      return ENDPOINT[this.type].method === "POST" ? utils_request.post(url) : utils_request.put(url);
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: $data.type === "expense"
  }, $data.type === "expense" ? common_vendor.e({
    b: common_vendor.t($options.formatAmount($data.data.amount)),
    c: common_vendor.t($data.data.billNo),
    d: $data.data.invoiceDefer === 1
  }, $data.data.invoiceDefer === 1 ? {} : {}, {
    e: $data.showEvidence && $data.data.evidenceLevel
  }, $data.showEvidence && $data.data.evidenceLevel ? {
    f: common_vendor.t($options.evidenceText),
    g: common_vendor.n($options.evidenceClass)
  } : {}, {
    h: $data.data.isOverBudget === 1
  }, $data.data.isOverBudget === 1 ? {} : {}, {
    i: common_vendor.t($data.data.applicantName),
    j: common_vendor.t($data.data.deptName || "-"),
    k: common_vendor.t($data.data.purposeTagName || "-"),
    l: common_vendor.t($data.data.projectName || "公司级（未关联项目）"),
    m: common_vendor.t($data.data.calcBudgetSubjectName || "-"),
    n: common_vendor.t($options.formatDate($data.data.expenseDate)),
    o: common_vendor.t($data.data.expensePeriod || "-"),
    p: $data.data.currencyCode && $data.data.currencyCode !== "CNY"
  }, $data.data.currencyCode && $data.data.currencyCode !== "CNY" ? {
    q: common_vendor.t($data.data.originalAmount),
    r: common_vendor.t($data.data.currencyCode),
    s: common_vendor.t($data.data.exchangeRate)
  } : {}, {
    t: $data.data.creditLevel
  }, $data.data.creditLevel ? {
    v: common_vendor.t($data.data.creditLevel)
  } : {}, {
    w: $data.data.remark
  }, $data.data.remark ? {
    x: common_vendor.t($data.data.remark)
  } : {}) : $data.type === "payment" ? common_vendor.e({
    z: common_vendor.t($options.formatAmount($data.data.amount)),
    A: common_vendor.t($data.data.paymentNo),
    B: common_vendor.t($data.data.projectName || "-"),
    C: common_vendor.t($data.data.payee || "-"),
    D: common_vendor.t($data.data.payer || "-"),
    E: common_vendor.t($data.data.payType || "-"),
    F: common_vendor.t($options.formatDate($data.data.paymentDate)),
    G: common_vendor.t($data.data.invoiceStatus === "HAS" ? "已有发票" : $data.data.invoiceStatus === "PENDING" ? "收集中" : "未到票"),
    H: $data.data.description
  }, $data.data.description ? {
    I: common_vendor.t($data.data.description)
  } : {}) : $data.type === "change" ? common_vendor.e({
    K: common_vendor.t(Number($data.data.contractDelta || 0) >= 0 ? "+" : ""),
    L: common_vendor.t($options.formatAmount($data.data.contractDelta)),
    M: common_vendor.n(Number($data.data.contractDelta || 0) < 0 ? "neg" : ""),
    N: common_vendor.t($data.data.projectName || "项目#" + $data.data.projectId),
    O: common_vendor.t($data.data.changeType || "合同变更"),
    P: common_vendor.t($options.formatAmount($data.data.oldContractAmount)),
    Q: common_vendor.t($options.formatAmount($data.data.newContractAmount)),
    R: common_vendor.t($options.statusText($data.CHANGE_STATUS, $data.data.changeStatus)),
    S: $data.data.changeReason
  }, $data.data.changeReason ? {
    T: common_vendor.t($data.data.changeReason)
  } : {}) : common_vendor.e({
    U: common_vendor.t($options.formatAmount($data.data.currentContractAmount)),
    V: common_vendor.t($data.data.projectName),
    W: common_vendor.t($data.data.clientName || "-"),
    X: common_vendor.t($data.data.projectManagerName || "-"),
    Y: common_vendor.t($options.formatDate($data.data.startDate)),
    Z: common_vendor.t($options.formatDate($data.data.endDate)),
    aa: common_vendor.t($options.formatAmount($data.data.totalBudget)),
    ab: $data.data.remark
  }, $data.data.remark ? {
    ac: common_vendor.t($data.data.remark)
  } : {}), {
    y: $data.type === "payment",
    J: $data.type === "change",
    ad: $options.canAudit
  }, $options.canAudit ? common_vendor.e({
    ae: $data.showRejectInput
  }, $data.showRejectInput ? {
    af: $data.type === "expense" ? "请输入驳回原因（5-500字，必填）" : "请输入驳回原因（选填）",
    ag: $data.rejectReason,
    ah: common_vendor.o(($event) => $data.rejectReason = $event.detail.value)
  } : {}, {
    ai: $data.showRejectInput
  }, $data.showRejectInput ? {
    aj: common_vendor.o(($event) => $data.showRejectInput = false),
    ak: common_vendor.o((...args) => $options.doReject && $options.doReject(...args))
  } : {
    al: common_vendor.o(($event) => $data.showRejectInput = true),
    am: common_vendor.o((...args) => $options.doApprove && $options.doApprove(...args))
  }) : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-3649eef1"]]);
wx.createPage(MiniProgramPage);
