"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_permission = require("../../utils/permission.js");
const utils_request = require("../../utils/request.js");
const config_index = require("../../config/index.js");
const utils_auth = require("../../utils/auth.js");
const utils_roles = require("../../utils/roles.js");
const utils_format = require("../../utils/format.js");
const MEMBER_ROLE = { MANAGER: "项目经理", MEMBER: "成员", VIEWER: "观察者" };
const _sfc_main = {
  data() {
    return {
      PROJECT_STATUS: utils_format.PROJECT_STATUS,
      projectId: null,
      project: {},
      profit: {},
      subjects: [],
      members: [],
      canApprove: false
    };
  },
  computed: {
    profitRateText() {
      const rate = Number(this.profit.profitRate || 0);
      return (rate > 1 ? rate : rate * 100).toFixed(2) + "%";
    }
  },
  onLoad(options) {
    if (!utils_permission.guardPage("/pages/project/detail")) return;
    this.projectId = options.id;
    this.loadAll();
  },
  methods: {
    formatAmount: utils_format.formatAmount,
    formatDate: utils_format.formatDate,
    statusText: utils_format.statusText,
    statusColor: utils_format.statusColor,
    async loadAll() {
      try {
        const [detail, profit, subjects, members] = await Promise.all([
          utils_request.get("/fee/project/" + this.projectId),
          utils_request.get(config_index.config.api.projectProfit + "/" + this.projectId).catch(() => ({ data: {} })),
          utils_request.get(config_index.config.api.projectSubjects + "/" + this.projectId).catch(() => ({ data: [] })),
          utils_request.get(config_index.config.api.projectMembers + "/" + this.projectId).catch(() => ({ data: [] }))
        ]);
        this.project = detail.data || {};
        this.profit = profit.data || {};
        this.subjects = subjects.data || [];
        this.members = members.data || [];
        this.canApprove = this.project.status === "PENDING_APPROVAL" && utils_auth.getRoles().includes(utils_roles.ROLES.BOSS);
      } catch (e) {
      }
    },
    subjectPercent(s) {
      const budget = Number(s.budgetAmount || 0);
      if (!budget) return 0;
      return Math.min(100, Math.round(Number(s.usedAmount || 0) / budget * 100));
    },
    subjectColor(s) {
      const percent = this.subjectPercent(s);
      if (percent > 90) return "#f56c6c";
      if (percent >= 70) return "#e6a23c";
      return "#67c23a";
    },
    memberRoleText(role) {
      return MEMBER_ROLE[role] || role || "-";
    },
    /** 老板审批通过立项 */
    onApprove() {
      common_vendor.index.showModal({
        title: "审批确认",
        content: "确认通过该项目的立项审批？",
        success: async (res) => {
          if (!res.confirm) return;
          try {
            await utils_request.put(config_index.config.api.projectApprove + "/" + this.projectId);
            common_vendor.index.showToast({ title: "审批通过", icon: "success" });
            this.loadAll();
          } catch (e) {
          }
        }
      });
    },
    /** 老板驳回立项（原因可选填） */
    onReject() {
      common_vendor.index.showModal({
        title: "驳回立项",
        content: "确认驳回该项目的立项申请？",
        editable: true,
        placeholderText: "驳回原因（选填）",
        success: async (res) => {
          if (!res.confirm) return;
          try {
            const reason = encodeURIComponent(res.content || "");
            await utils_request.put(config_index.config.api.projectReject + "/" + this.projectId + (reason ? "?reason=" + reason : ""));
            common_vendor.index.showToast({ title: "已驳回", icon: "success" });
            this.loadAll();
          } catch (e) {
          }
        }
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.t($data.project.projectName),
    b: common_vendor.t($options.statusText($data.PROJECT_STATUS, $data.project.status)),
    c: $options.statusColor($data.PROJECT_STATUS, $data.project.status),
    d: $options.statusColor($data.PROJECT_STATUS, $data.project.status),
    e: common_vendor.t($data.project.clientName || "暂无客户信息"),
    f: common_vendor.t($data.project.projectManagerName || "-"),
    g: common_vendor.t($options.formatDate($data.project.startDate)),
    h: common_vendor.t($options.formatDate($data.project.endDate)),
    i: common_vendor.t($options.formatAmount($data.project.currentContractAmount)),
    j: $data.project.changeCount
  }, $data.project.changeCount ? {
    k: common_vendor.t($data.project.changeCount)
  } : {}, {
    l: $data.canApprove
  }, $data.canApprove ? {
    m: common_vendor.o((...args) => $options.onReject && $options.onReject(...args)),
    n: common_vendor.o((...args) => $options.onApprove && $options.onApprove(...args))
  } : {}, {
    o: common_vendor.t($options.formatAmount($data.profit.actualProfit)),
    p: common_vendor.n(Number($data.profit.actualProfit || 0) < 0 ? "neg" : ""),
    q: common_vendor.t($options.formatAmount($data.profit.paperProfit)),
    r: common_vendor.t($options.profitRateText),
    s: common_vendor.t($options.formatAmount($data.profit.verifiedIncome)),
    t: common_vendor.t($options.formatAmount($data.profit.pendingIncome)),
    v: common_vendor.t($options.formatAmount($data.profit.totalExpense)),
    w: common_vendor.t($options.formatAmount($data.profit.depreciationShare)),
    x: common_vendor.t($options.formatAmount($data.profit.sharingAmount)),
    y: Number($data.profit.badDebtProvision || 0) > 0
  }, Number($data.profit.badDebtProvision || 0) > 0 ? {
    z: common_vendor.t($options.formatAmount($data.profit.badDebtProvision))
  } : {}, {
    A: !$data.subjects.length
  }, !$data.subjects.length ? {} : {}, {
    B: common_vendor.f($data.subjects, (s, k0, i0) => {
      return {
        a: common_vendor.t(s.subjectName),
        b: common_vendor.t(s.categoryName),
        c: $options.subjectPercent(s) + "%",
        d: $options.subjectColor(s),
        e: common_vendor.t($options.formatAmount(s.usedAmount)),
        f: common_vendor.t($options.formatAmount(s.remainAmount)),
        g: common_vendor.t($options.formatAmount(s.budgetAmount)),
        h: s.id
      };
    }),
    C: !$data.members.length
  }, !$data.members.length ? {} : {}, {
    D: common_vendor.f($data.members, (m, k0, i0) => {
      return common_vendor.e({
        a: common_vendor.t(m.userName),
        b: common_vendor.t($options.memberRoleText(m.role)),
        c: m.investRatio
      }, m.investRatio ? {
        d: common_vendor.t(m.investRatio)
      } : {}, {
        e: m.id
      });
    }),
    E: $data.project.status === "LOST"
  }, $data.project.status === "LOST" ? {
    F: common_vendor.t($data.project.lossType || "-"),
    G: common_vendor.t($data.project.lossReason || "-"),
    H: common_vendor.t($options.formatAmount($data.project.badDebtProvision))
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-0e6189de"]]);
wx.createPage(MiniProgramPage);
