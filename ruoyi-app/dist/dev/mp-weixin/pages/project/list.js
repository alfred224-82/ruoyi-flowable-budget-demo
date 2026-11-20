"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_permission = require("../../utils/permission.js");
const utils_request = require("../../utils/request.js");
const config_index = require("../../config/index.js");
const utils_format = require("../../utils/format.js");
const _sfc_main = {
  data() {
    return {
      PROJECT_STATUS: utils_format.PROJECT_STATUS,
      filters: [
        { label: "全部", value: "" },
        { label: "进行中", value: "ACTIVE" },
        { label: "待审批", value: "PENDING_APPROVAL" },
        { label: "已完成", value: "COMPLETED" },
        { label: "烂账", value: "LOST" }
      ],
      query: { status: "", pageNum: 1, pageSize: 10 },
      list: [],
      total: 0,
      loading: false,
      finished: false
    };
  },
  onLoad() {
    if (!utils_permission.guardPage("/pages/project/list")) return;
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
      this.query.pageNum += 1;
      this.loadList();
    }
  },
  methods: {
    formatWan: utils_format.formatWan,
    statusText: utils_format.statusText,
    statusColor: utils_format.statusColor,
    /** 项目列表（后端按角色控制数据范围：项目经理仅本人参与项目，fee_project_member） */
    async loadList(reset = false) {
      if (reset) {
        this.query.pageNum = 1;
        this.list = [];
        this.finished = false;
      }
      this.loading = true;
      try {
        const params = { pageNum: this.query.pageNum, pageSize: this.query.pageSize };
        if (this.query.status) params.status = this.query.status;
        const res = await utils_request.get(config_index.config.api.projectList, params);
        const rows = res.rows || [];
        this.list = reset ? rows : this.list.concat(rows);
        this.total = res.total || 0;
        this.finished = this.list.length >= this.total;
      } catch (e) {
      }
      this.loading = false;
    },
    changeStatus(status) {
      if (this.query.status === status) return;
      this.query.status = status;
      this.loadList(true);
    },
    /** 预算使用率 */
    usedPercent(p) {
      const total = Number(p.totalBudget || 0);
      if (!total) return 0;
      return Math.min(100, Math.round(Number(p.usedBudget || 0) / total * 100));
    },
    /** 使用率色：<70% 绿，70%~90% 黄，>90% 红 */
    usedColor(p) {
      const percent = this.usedPercent(p);
      if (percent > 90) return "#f56c6c";
      if (percent >= 70) return "#e6a23c";
      return "#67c23a";
    },
    goDetail(p) {
      common_vendor.index.navigateTo({ url: "/pages/project/detail?id=" + p.id });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.f($data.filters, (f, k0, i0) => {
      return {
        a: common_vendor.t(f.label),
        b: f.value,
        c: $data.query.status === f.value ? 1 : "",
        d: common_vendor.o(($event) => $options.changeStatus(f.value), f.value)
      };
    }),
    b: !$data.list.length && !$data.loading
  }, !$data.list.length && !$data.loading ? {} : {}, {
    c: common_vendor.f($data.list, (p, k0, i0) => {
      return {
        a: common_vendor.t(p.projectName),
        b: common_vendor.t($options.statusText($data.PROJECT_STATUS, p.status)),
        c: $options.statusColor($data.PROJECT_STATUS, p.status),
        d: $options.statusColor($data.PROJECT_STATUS, p.status),
        e: common_vendor.t(p.clientName || "-"),
        f: common_vendor.t(p.projectManagerName || "-"),
        g: $options.usedPercent(p) + "%",
        h: $options.usedColor(p),
        i: common_vendor.t($options.usedPercent(p)),
        j: common_vendor.t($options.formatWan(p.usedBudget)),
        k: common_vendor.t($options.formatWan(p.totalBudget)),
        l: common_vendor.t($options.formatWan(p.currentContractAmount)),
        m: common_vendor.t($options.formatWan(p.verifiedIncome)),
        n: common_vendor.t($options.formatWan(p.totalExpense)),
        o: p.id,
        p: common_vendor.o(($event) => $options.goDetail(p), p.id)
      };
    }),
    d: $data.list.length
  }, $data.list.length ? {
    e: common_vendor.t($data.finished ? "— 已加载全部 —" : $data.loading ? "加载中…" : "上拉加载更多")
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-00a44e1d"]]);
wx.createPage(MiniProgramPage);
