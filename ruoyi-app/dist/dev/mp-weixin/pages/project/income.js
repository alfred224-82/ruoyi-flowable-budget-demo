"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_permission = require("../../utils/permission.js");
const utils_request = require("../../utils/request.js");
const config_index = require("../../config/index.js");
const utils_format = require("../../utils/format.js");
const BASIS = { CONTRACT: "合同", INVOICE: "发票", RECEIPT: "回款单", OTHER: "其他" };
const _sfc_main = {
  data() {
    return {
      INCOME_STATUS: utils_format.INCOME_STATUS,
      filters: [
        { label: "全部", value: "" },
        { label: "待核验", value: "PENDING" },
        { label: "已核验", value: "VERIFIED" },
        { label: "已驳回", value: "REJECTED" }
      ],
      query: { verifyStatus: "", pageNum: 1, pageSize: 10 },
      list: [],
      loading: false,
      finished: false
    };
  },
  onLoad() {
    if (!utils_permission.guardPage("/pages/project/income")) return;
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
    formatAmount: utils_format.formatAmount,
    formatDate: utils_format.formatDate,
    statusText: utils_format.statusText,
    statusColor: utils_format.statusColor,
    /** 收入确认列表（后端数据范围：项目经理仅本人参与项目） */
    async loadList(reset = false) {
      if (reset) {
        this.query.pageNum = 1;
        this.list = [];
        this.finished = false;
      }
      this.loading = true;
      try {
        const params = { pageNum: this.query.pageNum, pageSize: this.query.pageSize };
        if (this.query.verifyStatus) params.verifyStatus = this.query.verifyStatus;
        const res = await utils_request.get(config_index.config.api.incomeList, params);
        const rows = res.rows || [];
        this.list = reset ? rows : this.list.concat(rows);
        this.finished = this.list.length >= (res.total || 0);
      } catch (e) {
      }
      this.loading = false;
    },
    changeStatus(status) {
      if (this.query.verifyStatus === status) return;
      this.query.verifyStatus = status;
      this.loadList(true);
    },
    basisText(type) {
      return BASIS[type] || type || "未填依据";
    },
    goCreate() {
      common_vendor.index.navigateTo({ url: "/pages/project/incomeCreate" });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.f($data.filters, (f, k0, i0) => {
      return {
        a: common_vendor.t(f.label),
        b: f.value,
        c: $data.query.verifyStatus === f.value ? 1 : "",
        d: common_vendor.o(($event) => $options.changeStatus(f.value), f.value)
      };
    }),
    b: !$data.list.length && !$data.loading
  }, !$data.list.length && !$data.loading ? {} : {}, {
    c: common_vendor.f($data.list, (item, k0, i0) => {
      return common_vendor.e({
        a: common_vendor.t(item.projectName || "项目#" + item.projectId),
        b: common_vendor.t($options.formatAmount(item.confirmedAmount)),
        c: common_vendor.t($options.statusText($data.INCOME_STATUS, item.verifyStatus)),
        d: $options.statusColor($data.INCOME_STATUS, item.verifyStatus),
        e: $options.statusColor($data.INCOME_STATUS, item.verifyStatus),
        f: common_vendor.t($options.basisText(item.basisType)),
        g: common_vendor.t(item.basisNo ? " · " + item.basisNo : ""),
        h: common_vendor.t($options.formatDate(item.confirmDate)),
        i: common_vendor.t(item.confirmedByName || "项目经理"),
        j: item.verifyStatus === "REJECTED" && item.rejectReason
      }, item.verifyStatus === "REJECTED" && item.rejectReason ? {
        k: common_vendor.t(item.rejectReason)
      } : {}, {
        l: item.id
      });
    }),
    d: $data.list.length
  }, $data.list.length ? {
    e: common_vendor.t($data.finished ? "— 已加载全部 —" : $data.loading ? "加载中…" : "上拉加载更多")
  } : {}, {
    f: common_vendor.o((...args) => $options.goCreate && $options.goCreate(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-1fc0e25b"]]);
wx.createPage(MiniProgramPage);
