"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const utils_theme = require("../../../utils/theme.js");
const utils_roles = require("../../../utils/roles.js");
const utils_format = require("../../../utils/format.js");
const MicroTabbar = () => "../../../components/micro-tabbar/micro-tabbar.js";
const _sfc_main = {
  components: { MicroTabbar },
  data() {
    return {
      tenantId: null,
      loaded: false,
      list: []
    };
  },
  computed: {
    showTabbar() {
      const tabs = utils_roles.getMicroTabConfig().tabs || [];
      return tabs.some((t) => t.page === "/pages/micro/activity/browse");
    }
  },
  onLoad(options) {
    if (options.tenantId) {
      this.tenantId = options.tenantId;
    } else {
      const tid = utils_roles.getTenantId();
      if (tid) {
        this.tenantId = String(tid);
      }
    }
    if (this.tenantId) {
      this.loadActivities();
    }
    this.checkFirstVisit();
  },
  methods: {
    toHttps: utils_format.toHttps,
    firstImage: utils_format.firstImage,
    themeBg(themeKey) {
      return utils_theme.themeGradient(themeKey);
    },
    /** 活动状态文案 */
    activityStatusText(item) {
      if (item.status === "ENDED") return "已结束";
      if (item.endTime) {
        const end = new Date(item.endTime).getTime();
        if (Date.now() > end) return "已结束";
      }
      return "报名进行中";
    },
    async loadActivities() {
      this.loaded = false;
      try {
        const params = { tenantId: this.tenantId };
        const res = await utils_request.get(config_index.config.api.microActivityBrowse, params);
        this.list = res && res.data || [];
      } catch (e) {
        this.list = [];
      } finally {
        this.loaded = true;
      }
    },
    /** ★活动详情单页化：所有角色/入口统一跳 pages/micro/activity/detail，页面按 id 自取全量数据 */
    viewDetail(item) {
      common_vendor.index.navigateTo({ url: "/pages/micro/activity/detail?id=" + encodeURIComponent(JSON.stringify(item)) });
    },
    /** T18.17 首次进入活动页弹窗确认录音知情 */
    checkFirstVisit() {
      const agreed = common_vendor.index.getStorageSync("voice_consent_agreed");
      const visited = common_vendor.index.getStorageSync("voice_browse_visited");
      if (!visited && !agreed) {
        common_vendor.index.setStorageSync("voice_browse_visited", true);
        common_vendor.index.showModal({
          title: "录音知情告知",
          content: "本店服务过程可能进行录音/录像，参与活动即视为知悉并同意。点击「确认」查看详情。",
          confirmText: "确认",
          cancelText: "稍后再看",
          success: (res) => {
            if (res.confirm) {
              common_vendor.index.navigateTo({ url: "/pages/micro/customer/consent" });
            }
          }
        });
      }
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
    a: !$data.tenantId
  }, !$data.tenantId ? {} : common_vendor.e({
    b: !$data.loaded
  }, !$data.loaded ? {} : !$data.list.length ? {} : {
    d: common_vendor.f($data.list, (item, k0, i0) => {
      return common_vendor.e({
        a: item.bgImageUrl || item.coverUrl
      }, item.bgImageUrl || item.coverUrl ? {
        b: $options.toHttps(item.bgImageUrl || $options.firstImage(item.coverUrl))
      } : {
        c: $options.themeBg(item.themeKey)
      }, {
        d: common_vendor.t(item.title || "-"),
        e: common_vendor.t($options.activityStatusText(item)),
        f: item.id,
        g: common_vendor.o(($event) => $options.viewDetail(item), item.id)
      });
    })
  }, {
    c: !$data.list.length
  }), {
    e: $options.showTabbar
  }, $options.showTabbar ? {
    f: common_vendor.p({
      current: "/pages/micro/activity/browse"
    })
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-7ff58014"]]);
wx.createPage(MiniProgramPage);
