"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_auth = require("../../../utils/auth.js");
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
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
          if (item.status && item.status !== "ENDED") {
            if (item.endTime) {
              return Date.now() <= new Date(item.endTime).getTime();
            }
            return true;
          }
          return false;
        }
        if (this.activeTab === "ENDED") {
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
    switchTab(key) {
      this.activeTab = key;
    },
    async loadList() {
      this.loaded = false;
      try {
        const res = await utils_request.get(config_index.config.api.microBossMyActivities);
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
        a: item.coverUrl
      }, item.coverUrl ? {
        b: item.coverUrl,
        c: common_vendor.t(item.title || "-")
      } : {
        d: common_vendor.t(item.title || "-")
      }, {
        e: common_vendor.t(item.participantCount || 0),
        f: item.hostNames
      }, item.hostNames ? {
        g: common_vendor.t(item.hostNames.charAt(0)),
        h: common_vendor.t(item.hostNames)
      } : {}, {
        i: item.totalSeats != null && item.totalSeats > 0
      }, item.totalSeats != null && item.totalSeats > 0 ? {
        j: common_vendor.t(item.totalSeats),
        k: common_vendor.t(item.totalSeats - (item.currentSeats || 0))
      } : {}, {
        l: common_vendor.t(item.startTime || ""),
        m: common_vendor.t(item.endTime || ""),
        n: "act-" + item.bizId
      });
    })
  }, {
    c: !$options.filteredList.length,
    e: common_vendor.o((...args) => $options.loadMore && $options.loadMore(...args)),
    f: common_vendor.p({
      current: "/pages/micro/bossBook/list"
    }),
    g: common_vendor.n(_ctx.themeClass),
    h: common_vendor.s(_ctx.pageStyle)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-64100fb6"]]);
wx.createPage(MiniProgramPage);
