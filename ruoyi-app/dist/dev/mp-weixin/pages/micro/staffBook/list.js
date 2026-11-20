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
    /** 状态标签文案 */
    statusLabel(item) {
      if (item.sourceType === "BOOKING") {
        const map2 = { PENDING: "待确认", CONFIRMED: "已确认", IN_PROGRESS: "进行中", COMPLETED: "已完成", CANCELLED: "已取消" };
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
      if (label === "进行中" || label === "已确认" || label === "待确认") return "tag-ongoing";
      if (label === "已结束" || label === "已完成") return "tag-ended";
      if (label === "已取消" || label === "草稿") return "tag-draft";
      return "tag-ongoing";
    },
    async loadList() {
      this.loaded = false;
      try {
        const res = await utils_request.get(config_index.config.api.microStaffMyActivities);
        this.list = res && res.data || [];
      } catch (e) {
        this.list = [];
      } finally {
        this.loaded = true;
      }
    },
    loadMore() {
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
        a: item.sourceType === "ACTIVITY_SIGNUP" && item.coverUrl
      }, item.sourceType === "ACTIVITY_SIGNUP" && item.coverUrl ? {
        b: item.coverUrl,
        c: common_vendor.t(item.title || "-")
      } : common_vendor.e({
        d: common_vendor.t(item.title || item.serviceName || "-"),
        e: item.sourceType === "BOOKING"
      }, item.sourceType === "BOOKING" ? {
        f: common_vendor.t($options.statusLabel(item)),
        g: common_vendor.n($options.statusTagClass(item))
      } : {}), {
        h: item.sourceType === "ACTIVITY_SIGNUP"
      }, item.sourceType === "ACTIVITY_SIGNUP" ? {
        i: common_vendor.t(item.participantCount || 0)
      } : {}, {
        j: item.hostNames
      }, item.hostNames ? {
        k: common_vendor.t(item.hostNames.charAt(0)),
        l: common_vendor.t(item.roleLabel || "主持"),
        m: common_vendor.t(item.hostNames)
      } : {}, {
        n: item.sourceType === "BOOKING"
      }, item.sourceType === "BOOKING" ? common_vendor.e({
        o: common_vendor.t(item.startTime || "-"),
        p: item.amount
      }, item.amount ? {
        q: common_vendor.t($options.formatAmount(item.amount))
      } : {}) : {}, {
        r: item.totalSeats != null && item.totalSeats > 0
      }, item.totalSeats != null && item.totalSeats > 0 ? {
        s: common_vendor.t(item.totalSeats),
        t: common_vendor.t(item.totalSeats - (item.currentSeats || 0))
      } : {}, {
        v: common_vendor.t(item.startTime || ""),
        w: common_vendor.t(item.endTime || ""),
        x: item.sourceType + "-" + item.bizId
      });
    })
  }, {
    c: !$options.filteredList.length,
    e: common_vendor.o((...args) => $options.loadMore && $options.loadMore(...args)),
    f: common_vendor.p({
      current: "/pages/micro/staffMine/index"
    }),
    g: common_vendor.n(_ctx.themeClass),
    h: common_vendor.s(_ctx.pageStyle)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-318491e4"]]);
wx.createPage(MiniProgramPage);
