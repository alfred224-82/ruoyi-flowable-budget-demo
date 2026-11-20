"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_permission = require("../../utils/permission.js");
const utils_request = require("../../utils/request.js");
const config_index = require("../../config/index.js");
const utils_format = require("../../utils/format.js");
const _sfc_main = {
  data() {
    return {
      SUPPLEMENT_STATUS: utils_format.SUPPLEMENT_STATUS,
      filters: [
        { label: "待补票", value: "PENDING" },
        { label: "已完成", value: "COMPLETED" },
        { label: "已超期", value: "OVERDUE" },
        { label: "全部", value: "" }
      ],
      query: { status: "PENDING", pageNum: 1, pageSize: 10 },
      list: [],
      loading: false,
      finished: false,
      // 本地已上传成功的发票地址（按补票记录 id 分组）
      uploadMap: {}
    };
  },
  onLoad() {
    if (!utils_permission.guardPage("/pages/expense/supplement")) return;
    this.loadList(true);
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
    toHttps: utils_format.toHttps,
    /** 补票记录列表 */
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
        const res = await utils_request.get(config_index.config.api.supplementList, params);
        const rows = res.rows || [];
        this.list = reset ? rows : this.list.concat(rows);
        this.finished = this.list.length >= (res.total || 0);
      } catch (e) {
      }
      this.loading = false;
    },
    changeStatus(status) {
      if (this.query.status === status) return;
      this.query.status = status;
      this.loadList(true);
    },
    /** 选择并上传发票图片（/system/oss/upload 需 system:oss:upload 权限，403 时提示联系财务） */
    chooseImage(item) {
      common_vendor.index.chooseImage({
        count: 3,
        sizeType: ["compressed"],
        success: async (res) => {
          for (const path of res.tempFilePaths) {
            try {
              const r = await utils_request.upload(config_index.config.api.upload, path);
              const url = (r.data || {}).url;
              if (url) {
                const arr = this.uploadMap[item.id] || [];
                this.$set(this.uploadMap, item.id, arr.concat(url));
              }
            } catch (e) {
            }
          }
        }
      });
    },
    previewImage(id, idx) {
      common_vendor.index.previewImage({ urls: this.uploadMap[id] || [], current: idx });
    },
    removeImage(id, idx) {
      const arr = (this.uploadMap[id] || []).slice();
      arr.splice(idx, 1);
      this.$set(this.uploadMap, id, arr);
    },
    /** 提交补票（预占转实扣，后端必填 invoiceUrls 校验） */
    submitInvoice(item) {
      const urls = this.uploadMap[item.id] || [];
      if (!urls.length) {
        common_vendor.index.showToast({ title: "请先上传发票图片", icon: "none" });
        return;
      }
      common_vendor.index.showModal({
        title: "提交补票",
        content: "确认提交 " + urls.length + " 张发票？提交后预占额度转为实扣。",
        success: async (res) => {
          if (!res.confirm) return;
          this.$set(item, "_uploading", true);
          try {
            await utils_request.put(config_index.config.api.supplementSubmit + "/" + item.id + "?invoiceUrls=" + encodeURIComponent(urls.join(",")));
            common_vendor.index.showToast({ title: "补票成功", icon: "success" });
            this.loadList(true);
          } catch (e) {
          }
          this.$set(item, "_uploading", false);
        }
      });
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
    c: common_vendor.f($data.list, (item, k0, i0) => {
      return common_vendor.e({
        a: common_vendor.t(item.billNo || "报销单#" + item.billId),
        b: common_vendor.t($options.formatAmount(item.billAmount)),
        c: common_vendor.t($options.statusText($data.SUPPLEMENT_STATUS, item.status)),
        d: $options.statusColor($data.SUPPLEMENT_STATUS, item.status),
        e: $options.statusColor($data.SUPPLEMENT_STATUS, item.status),
        f: common_vendor.t($options.formatDate(item.estimatedDate)),
        g: common_vendor.t($options.formatAmount(item.reservedAmount)),
        h: item.status === "COMPLETED"
      }, item.status === "COMPLETED" ? {
        i: common_vendor.t($options.formatDate(item.supplementDate))
      } : {}, {
        j: item.status === "PENDING"
      }, item.status === "PENDING" ? {
        k: common_vendor.f($data.uploadMap[item.id] || [], (img, idx, i1) => {
          return {
            a: $options.toHttps(img),
            b: common_vendor.o(($event) => $options.previewImage(item.id, idx), idx),
            c: common_vendor.o(($event) => $options.removeImage(item.id, idx), idx),
            d: idx
          };
        }),
        l: common_vendor.o(($event) => $options.chooseImage(item), item.id),
        m: item._uploading,
        n: !($data.uploadMap[item.id] || []).length,
        o: common_vendor.o(($event) => $options.submitInvoice(item), item.id)
      } : {}, {
        p: item.id,
        q: item.status === "OVERDUE" ? 1 : ""
      });
    }),
    d: $data.list.length
  }, $data.list.length ? {
    e: common_vendor.t($data.finished ? "— 已加载全部 —" : $data.loading ? "加载中…" : "上拉加载更多")
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-76ea8c5d"]]);
wx.createPage(MiniProgramPage);
