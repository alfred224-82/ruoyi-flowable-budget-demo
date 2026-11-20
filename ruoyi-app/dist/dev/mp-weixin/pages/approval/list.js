"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_permission = require("../../utils/permission.js");
const utils_request = require("../../utils/request.js");
const config_index = require("../../config/index.js");
const utils_features = require("../../utils/features.js");
const utils_format = require("../../utils/format.js");
const SOURCE = {
  expense: { url: config_index.config.api.expensePending, params: {} },
  payment: { url: config_index.config.api.paymentList, params: { status: "PENDING" } },
  change: { url: config_index.config.api.changeList, params: { changeStatus: "PENDING" } },
  project: { url: config_index.config.api.projectList, params: { status: "PENDING_APPROVAL" } }
};
const EVIDENCE = {
  strong: { label: "证据充分", cls: "ok" },
  medium: { label: "证据一般", cls: "warn" },
  weak: { label: "证据薄弱", cls: "danger" }
};
const _sfc_main = {
  data() {
    return {
      CHANGE_STATUS: utils_format.CHANGE_STATUS,
      types: [
        { label: "报销", value: "expense" },
        { label: "付款", value: "payment" },
        { label: "变更", value: "change" },
        { label: "立项", value: "project" }
      ],
      type: "expense",
      list: [],
      pageNum: 1,
      pageSize: 10,
      finished: false,
      loading: false,
      showEvidence: utils_features.hasFeature("FEAT_EVIDENCE_CHAIN")
    };
  },
  onLoad(options) {
    if (!utils_permission.guardPage("/pages/approval/list")) return;
    if (options.type && SOURCE[options.type]) this.type = options.type;
    this.loadList(true);
  },
  onShow() {
    if (this.list.length) this.loadList(true);
  },
  onPullDownRefresh() {
    this.loadList(true).finally(() => common_vendor.index.stopPullDownRefresh());
  },
  onReachBottom() {
    if (!this.finished && !this.loading) {
      this.pageNum += 1;
      this.loadList();
    }
  },
  methods: {
    formatAmount: utils_format.formatAmount,
    statusText: utils_format.statusText,
    changeType(type) {
      if (this.type === type) return;
      this.type = type;
      this.loadList(true);
    },
    /** 按当前类型加载待审批列表 */
    async loadList(reset = false) {
      if (reset) {
        this.pageNum = 1;
        this.list = [];
        this.finished = false;
      }
      this.loading = true;
      try {
        const src = SOURCE[this.type];
        const res = await utils_request.get(src.url, { ...src.params, pageNum: this.pageNum, pageSize: this.pageSize });
        const rows = res.rows || [];
        this.list = reset ? rows : this.list.concat(rows);
        this.finished = this.list.length >= (res.total || 0);
      } catch (e) {
      }
      this.loading = false;
    },
    evidenceText(level) {
      return (EVIDENCE[level] || {}).label || level;
    },
    evidenceClass(level) {
      return (EVIDENCE[level] || {}).cls || "";
    },
    goDetail(item) {
      common_vendor.index.navigateTo({ url: "/pages/approval/detail?type=" + this.type + "&id=" + item.id });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.f($data.types, (t, k0, i0) => {
      return {
        a: common_vendor.t(t.label),
        b: t.value,
        c: $data.type === t.value ? 1 : "",
        d: common_vendor.o(($event) => $options.changeType(t.value), t.value)
      };
    }),
    b: !$data.list.length && !$data.loading
  }, !$data.list.length && !$data.loading ? {} : {}, {
    c: common_vendor.f($data.list, (item, k0, i0) => {
      return common_vendor.e($data.type === "expense" ? common_vendor.e({
        a: common_vendor.t(item.billNo),
        b: common_vendor.t($options.formatAmount(item.amount)),
        c: common_vendor.t(item.applicantName),
        d: common_vendor.t(item.deptName || "-"),
        e: common_vendor.t(item.purposeTagName || "未分类"),
        f: item.invoiceDefer === 1
      }, item.invoiceDefer === 1 ? {} : {}, {
        g: $data.showEvidence && item.evidenceLevel
      }, $data.showEvidence && item.evidenceLevel ? {
        h: common_vendor.t($options.evidenceText(item.evidenceLevel)),
        i: common_vendor.n($options.evidenceClass(item.evidenceLevel))
      } : {}, {
        j: item.projectName
      }, item.projectName ? {
        k: common_vendor.t(item.projectName)
      } : {}) : $data.type === "payment" ? {
        l: common_vendor.t(item.paymentNo),
        m: common_vendor.t($options.formatAmount(item.amount)),
        n: common_vendor.t(item.projectName || "-"),
        o: common_vendor.t(item.payee || "-"),
        p: common_vendor.t(item.invoiceStatus === "HAS" ? "已有发票" : "发票未到"),
        q: common_vendor.n(item.invoiceStatus === "HAS" ? "ok" : "warn")
      } : $data.type === "change" ? {
        r: common_vendor.t(item.projectName || "项目#" + item.projectId),
        s: common_vendor.t(Number(item.contractDelta || 0) >= 0 ? "+" : ""),
        t: common_vendor.t($options.formatAmount(item.contractDelta)),
        v: common_vendor.n(Number(item.contractDelta || 0) < 0 ? "neg" : ""),
        w: common_vendor.t(item.changeType || "合同变更"),
        x: common_vendor.t($options.statusText($data.CHANGE_STATUS, item.changeStatus))
      } : {
        y: common_vendor.t(item.projectName),
        z: common_vendor.t($options.formatAmount(item.currentContractAmount)),
        A: common_vendor.t(item.clientName || "-"),
        B: common_vendor.t(item.projectManagerName || "-")
      }, {
        C: $data.type + "-" + item.id,
        D: common_vendor.o(($event) => $options.goDetail(item), $data.type + "-" + item.id)
      });
    }),
    d: $data.type === "expense",
    e: $data.type === "payment",
    f: $data.type === "change",
    g: $data.list.length
  }, $data.list.length ? {
    h: common_vendor.t($data.finished ? "— 已加载全部 —" : $data.loading ? "加载中…" : "上拉加载更多")
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-bb510287"]]);
wx.createPage(MiniProgramPage);
