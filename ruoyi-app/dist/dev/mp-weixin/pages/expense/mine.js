"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_permission = require("../../utils/permission.js");
const utils_request = require("../../utils/request.js");
const config_index = require("../../config/index.js");
const utils_format = require("../../utils/format.js");
const _sfc_main = {
  data() {
    return {
      EXPENSE_STATUS: utils_format.EXPENSE_STATUS,
      filters: [
        { label: "全部", value: "" },
        { label: "待审批", value: "SUBMITTED" },
        { label: "已通过", value: "APPROVED" },
        { label: "已驳回", value: "REJECTED" }
      ],
      query: { status: "", pageNum: 1, pageSize: 10 },
      list: [],
      loading: false,
      finished: false,
      pendingSupplementCount: 0
    };
  },
  onLoad() {
    if (!utils_permission.guardPage("/pages/expense/mine")) return;
    this.loadList(true);
  },
  onShow() {
    if (this.list.length) this.loadList(true);
    this.loadSupplementCount();
  },
  onPullDownRefresh() {
    Promise.all([this.loadList(true), this.loadSupplementCount()]).finally(() => common_vendor.index.stopPullDownRefresh());
  },
  onReachBottom() {
    if (!this.finished && !this.loading) {
      this.query.pageNum += 1;
      this.loadList();
    }
  },
  methods: {
    formatAmount: utils_format.formatAmount,
    formatDate: utils_format.formatDate,
    statusText: utils_format.statusText,
    statusColor: utils_format.statusColor,
    /** 我的报销列表（后端已按当前申请人过滤） */
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
        const res = await utils_request.get(config_index.config.api.expenseList, params);
        const rows = res.rows || [];
        this.list = reset ? rows : this.list.concat(rows);
        this.finished = this.list.length >= (res.total || 0);
      } catch (e) {
      }
      this.loading = false;
    },
    /** 待补票数量（提醒条） */
    async loadSupplementCount() {
      try {
        const res = await utils_request.get(config_index.config.api.supplementList, { status: "PENDING", pageNum: 1, pageSize: 50 });
        this.pendingSupplementCount = res.total || 0;
      } catch (e) {
      }
    },
    changeStatus(status) {
      if (this.query.status === status) return;
      this.query.status = status;
      this.loadList(true);
    },
    goSupplement() {
      common_vendor.index.navigateTo({ url: "/pages/expense/supplement" });
    },
    goReapply() {
      common_vendor.index.switchTab({ url: "/pages/tab-work/index" });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: $data.pendingSupplementCount > 0
  }, $data.pendingSupplementCount > 0 ? {
    b: common_vendor.t($data.pendingSupplementCount),
    c: common_vendor.o((...args) => $options.goSupplement && $options.goSupplement(...args))
  } : {}, {
    d: common_vendor.f($data.filters, (f, k0, i0) => {
      return {
        a: common_vendor.t(f.label),
        b: f.value,
        c: $data.query.status === f.value ? 1 : "",
        d: common_vendor.o(($event) => $options.changeStatus(f.value), f.value)
      };
    }),
    e: !$data.list.length && !$data.loading
  }, !$data.list.length && !$data.loading ? {} : {}, {
    f: common_vendor.f($data.list, (item, k0, i0) => {
      return common_vendor.e({
        a: common_vendor.t(item.billNo),
        b: common_vendor.t($options.formatAmount(item.amount)),
        c: common_vendor.t($options.statusText($data.EXPENSE_STATUS, item.status)),
        d: $options.statusColor($data.EXPENSE_STATUS, item.status),
        e: $options.statusColor($data.EXPENSE_STATUS, item.status),
        f: item.invoiceDefer === 1
      }, item.invoiceDefer === 1 ? {} : {}, {
        g: common_vendor.t(item.purposeTagName || "未分类"),
        h: common_vendor.t($options.formatDate(item.expenseDate)),
        i: common_vendor.t(item.projectName || "公司级"),
        j: common_vendor.t(item.expensePeriod),
        k: item.status === "REJECTED" && item.rejectReason
      }, item.status === "REJECTED" && item.rejectReason ? {
        l: common_vendor.t(item.rejectReason),
        m: common_vendor.o((...args) => $options.goReapply && $options.goReapply(...args), item.id)
      } : {}, {
        n: item.id
      });
    }),
    g: $data.list.length
  }, $data.list.length ? {
    h: common_vendor.t($data.finished ? "— 已加载全部 —" : $data.loading ? "加载中…" : "上拉加载更多")
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-03489997"]]);
wx.createPage(MiniProgramPage);
