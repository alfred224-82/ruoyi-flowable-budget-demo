"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_auth = require("../../../utils/auth.js");
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const utils_format = require("../../../utils/format.js");
const utils_themeMixin = require("../../../utils/themeMixin.js");
const MicroTabbar = () => "../../../components/micro-tabbar/micro-tabbar.js";
const _sfc_main = {
  components: { MicroTabbar },
  mixins: [utils_themeMixin.themeMixin],
  data() {
    return {
      loaded: false,
      list: [],
      activeTab: "ALL",
      tabs: [
        { key: "ALL", label: "全部" },
        { key: "ONGOING", label: "进行中" },
        { key: "ENDED", label: "已结束" }
      ]
    };
  },
  computed: {
    filteredList() {
      if (this.activeTab === "ALL") return this.list;
      return this.list.filter((item) => {
        if (this.activeTab === "ONGOING") {
          if (item.sourceType === "BOOKING") {
            return item.status !== "COMPLETED" && item.status !== "CANCELLED";
          }
          if (item.status && item.status !== "ENDED") {
            if (item.endTime) {
              return Date.now() <= new Date(item.endTime).getTime();
            }
            return true;
          }
          return false;
        }
        if (this.activeTab === "ENDED") {
          if (item.sourceType === "BOOKING") {
            return item.status === "COMPLETED";
          }
          if (item.status === "ENDED") return true;
          if (item.endTime) {
            return Date.now() > new Date(item.endTime).getTime();
          }
          return false;
        }
        return true;
      });
    }
  },
  onShow() {
    if (!utils_auth.getToken()) {
      common_vendor.index.reLaunch({ url: "/pages/login/index" });
      return;
    }
    this.loadList();
  },
  methods: {
    formatAmount: utils_format.formatAmount,
    switchTab(key) {
      this.activeTab = key;
    },
    /** 构建标题：活动标题 + 主持人信息 */
    buildTitle(item) {
      if (item.sourceType === "ACTIVITY_SIGNUP" && item.hostNames) {
        return `${item.title || "-"}，由${item.hostNames}主持`;
      }
      return item.title || item.serviceName || "-";
    },
    /** 状态标签文案 */
    statusLabel(item) {
      if (item.sourceType === "BOOKING") {
        const map2 = { PENDING: "待确认", CONFIRMED: "已确认", IN_PROGRESS: "进行中", COMPLETED: "已完成", CANCELLED: "已取消", PENDING_SERVICE: "待服务" };
        return map2[item.status] || item.status || "-";
      }
      const map = { DRAFT: "草稿", PUBLISHED: "进行中", ENDED: "已结束" };
      if (item.status === "PUBLISHED" && item.endTime && Date.now() > new Date(item.endTime).getTime()) {
        return "已结束";
      }
      return map[item.status] || item.status || "-";
    },
    /** 状态标签样式类 */
    statusTagClass(item) {
      const label = this.statusLabel(item);
      if (label === "进行中" || label === "已确认" || label === "待确认" || label === "待服务") return "tag-ongoing";
      if (label === "已结束" || label === "已完成") return "tag-ended";
      if (label === "已取消" || label === "草稿") return "tag-draft";
      return "tag-ongoing";
    },
    async loadList() {
      this.loaded = false;
      try {
        const res = await utils_request.get(config_index.config.api.microCustomerMyActivities);
        this.list = (res && res.data || []).map((x) => {
          if (x.sourceType === "BOOKING") {
            x.orderStatus = x.status;
            x.cost = null;
          }
          return x;
        });
      } catch (e) {
        this.list = [];
      } finally {
        this.loaded = true;
      }
    },
    loadMore() {
    },
    goDetail(item) {
      if (item.sourceType === "BOOKING") {
        common_vendor.index.navigateTo({ url: "/pages/micro/customerBook/detail?id=" + item.bizId });
      } else {
        common_vendor.index.navigateTo({ url: "/pages/micro/activity/detail?id=" + JSON.stringify(item) });
      }
    },
    cancelBook(id) {
      common_vendor.index.showModal({
        title: "取消预约",
        content: "确认取消该预约？",
        success: async (r) => {
          if (r.confirm) {
            try {
              await utils_request.put(config_index.config.api.microCustomerCancelBook + "/" + id);
              common_vendor.index.showToast({ title: "已取消", icon: "success" });
              this.loadList();
            } catch (e) {
            }
          }
        }
      });
    }
  }
};
if (!Array) {
  const _easycom_micro_tabbar2 = common_vendor.resolveComponent("micro-tabbar");
  _easycom_micro_tabbar2();
}
const _easycom_micro_tabbar = () => "../../../components/micro-tabbar/micro-tabbar.js";
if (!Math) {
  _easycom_micro_tabbar();
}
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.f($data.tabs, (tab, k0, i0) => {
      return common_vendor.e({
        a: common_vendor.t(tab.label),
        b: $data.activeTab === tab.key
      }, $data.activeTab === tab.key ? {} : {}, {
        c: tab.key,
        d: $data.activeTab === tab.key ? 1 : "",
        e: common_vendor.o(($event) => $options.switchTab(tab.key), tab.key)
      });
    }),
    b: !$data.loaded
  }, !$data.loaded ? {} : !$options.filteredList.length ? {} : {
    d: common_vendor.f($options.filteredList, (item, k0, i0) => {
      return common_vendor.e({
        a: common_vendor.t($options.buildTitle(item)),
        b: item.sourceType === "ACTIVITY_SIGNUP"
      }, item.sourceType === "ACTIVITY_SIGNUP" ? {} : {}, {
        c: item.isLocked
      }, item.isLocked ? {} : {}, {
        d: common_vendor.t($options.statusLabel(item)),
        e: common_vendor.n($options.statusTagClass(item)),
        f: common_vendor.t(item.startTime || "-"),
        g: item.sourceType === "ACTIVITY_SIGNUP" && item.perPersonPrice != null
      }, item.sourceType === "ACTIVITY_SIGNUP" && item.perPersonPrice != null ? {
        h: common_vendor.t(item.perPersonPrice)
      } : {}, {
        i: item.sourceType === "ACTIVITY_SIGNUP" && item.participantCount
      }, item.sourceType === "ACTIVITY_SIGNUP" && item.participantCount ? {
        j: common_vendor.t(item.participantCount)
      } : {}, {
        k: item.sourceType === "BOOKING" && item.amount
      }, item.sourceType === "BOOKING" && item.amount ? {
        l: common_vendor.t($options.formatAmount(item.amount))
      } : {}, {
        m: item.totalSeats != null && item.totalSeats > 0
      }, item.totalSeats != null && item.totalSeats > 0 ? common_vendor.e({
        n: common_vendor.t(item.currentSeats || 0),
        o: common_vendor.t(item.totalSeats),
        p: item.currentSeats != null
      }, item.currentSeats != null ? {
        q: common_vendor.t(item.totalSeats - item.currentSeats)
      } : {}) : {}, {
        r: item.sourceType === "BOOKING" && item.status === "PENDING_SERVICE"
      }, item.sourceType === "BOOKING" && item.status === "PENDING_SERVICE" ? {
        s: common_vendor.o(($event) => $options.cancelBook(item.bizId), item.sourceType + "-" + item.bizId)
      } : {}, {
        t: item.sourceType + "-" + item.bizId,
        v: common_vendor.o(($event) => $options.goDetail(item), item.sourceType + "-" + item.bizId)
      });
    })
  }, {
    c: !$options.filteredList.length,
    e: common_vendor.o((...args) => $options.loadMore && $options.loadMore(...args)),
    f: common_vendor.p({
      current: "/pages/micro/customerBook/list"
    }),
    g: common_vendor.n(_ctx.themeClass),
    h: common_vendor.s(_ctx.pageStyle)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-7e18bce6"]]);
wx.createPage(MiniProgramPage);
