"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_auth = require("../../../utils/auth.js");
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const utils_format = require("../../../utils/format.js");
const utils_roles = require("../../../utils/roles.js");
const utils_theme = require("../../../utils/theme.js");
const utils_themeIcons = require("../../../utils/themeIcons.js");
const MicroTabbar = () => "../../../components/micro-tabbar/micro-tabbar.js";
const TYPE_MAP = {
  activity: "活动",
  notice: "预告",
  poster: "海报"
};
const _sfc_main = {
  components: { MicroTabbar },
  data() {
    return {
      // ★v1.42 租户名称/用户昵称（导航栏标题同源）
      tenantName: "",
      userName: "",
      // ★v1.43 租户主题色板（/micro/platform/theme/current 下发，首屏先用缓存同步渲染）
      theme: utils_theme.getThemeSync(),
      // 版本档位（登录响应 versionType：micro_pro/micro_lite）
      isPro: false,
      // ★V3.7 经营驾驶舱聚合数据（/micro/bossDashboard）
      dash: {},
      costComposition: []
    };
  },
  computed: {
    /** ★20261016 四主题重设计：根节点注入完整主题令牌（沿 DOM 继承到全部子元素/子组件） */
    rootStyle() {
      return utils_theme.themeCssVars(this.theme.key);
    },
    /** ★v1.43 欢迎区渐变：按租户主题渲染 */
    welcomeGradient() {
      return utils_theme.themeGradient(this.theme.key);
    },
    /** ★欢迎区前景文字色：浅色渐变用同系深色字、深色渐变用白字（禁止写死白色，浅色底不可读） */
    welcomeText() {
      return utils_theme.themeGradientText(this.theme.key);
    },
    /** ★v1.43 时段问候语 */
    greeting() {
      const h = (/* @__PURE__ */ new Date()).getHours();
      if (h < 6) return "夜深了";
      if (h < 9) return "早上好";
      if (h < 12) return "上午好";
      if (h < 14) return "中午好";
      if (h < 18) return "下午好";
      return "晚上好";
    }
  },
  onShow() {
    if (!utils_auth.getToken()) {
      common_vendor.index.reLaunch({ url: "/pages/login/index" });
      return;
    }
    utils_roles.applyNavBarTitle();
    this.loadData();
  },
  methods: {
    formatAmount: utils_format.formatAmount,
    toHttps: utils_format.toHttps,
    iconUrl: utils_themeIcons.iconUrl,
    typeLabel(type) {
      return TYPE_MAP[type] || "活动";
    },
    /** ★v1.43 活动封面占位渐变：按活动自身 theme_key */
    activityGradient(themeKey) {
      return utils_theme.themeGradient(themeKey);
    },
    go(url) {
      common_vendor.index.navigateTo({ url });
    },
    async loadData() {
      this.tenantName = utils_roles.getTenantName();
      let vt = utils_auth.getVersionType();
      if (!vt) {
        const sel = utils_roles.getSelectedTenant();
        vt = sel && sel.versionType || "";
      }
      this.isPro = vt === "micro_pro";
      this.theme = utils_theme.getThemeSync();
      utils_theme.loadTenantTheme().then((t) => {
        this.theme = t;
      });
      this.loadProfile();
      if (this.isPro) {
        this.loadDashboard();
      }
    },
    /** ★V3.7 经营驾驶舱聚合数据（核心经营数据 + 成本构成 + 业务待办计数） */
    async loadDashboard() {
      try {
        const res = await utils_request.get(config_index.config.api.microBossDashboard);
        this.dash = res && res.data || {};
        this.costComposition = this.dash.costComposition || [];
      } catch (e) {
        this.dash = {};
        this.costComposition = [];
      }
    },
    async loadProfile() {
      try {
        const res = await utils_request.get(config_index.config.api.microProfile);
        const data = res && res.data || {};
        this.userName = data.nickName || "";
      } catch (e) {
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
    a: $data.theme.key === "theme_handdrawn"
  }, $data.theme.key === "theme_handdrawn" ? {
    b: $options.iconUrl("hand-star")
  } : {}, {
    c: $data.theme.key === "theme_handdrawn"
  }, $data.theme.key === "theme_handdrawn" ? {
    d: $options.iconUrl("hand-sparkle")
  } : {}, {
    e: $data.theme.key === "theme_handdrawn"
  }, $data.theme.key === "theme_handdrawn" ? {
    f: $options.iconUrl("hand-bubble")
  } : {}, {
    g: $data.theme.key === "theme_handdrawn"
  }, $data.theme.key === "theme_handdrawn" ? {
    h: $options.iconUrl("hand-cloud")
  } : {}, {
    i: $data.theme.key === "theme_handdrawn"
  }, $data.theme.key === "theme_handdrawn" ? {
    j: $options.iconUrl("hand-cloud")
  } : {}, {
    k: $options.welcomeText.text,
    l: common_vendor.t($options.greeting),
    m: common_vendor.t($data.tenantName),
    n: common_vendor.t($data.userName),
    o: $options.welcomeText.sub,
    p: $options.welcomeGradient,
    q: $data.isPro
  }, $data.isPro ? common_vendor.e({
    r: common_vendor.t($options.formatAmount($data.dash.todayReceivable || 0)),
    s: common_vendor.t($options.formatAmount($data.dash.todayReceived || 0)),
    t: common_vendor.t($options.formatAmount($data.dash.todayExpense || 0)),
    v: common_vendor.t($options.formatAmount($data.dash.todayNetProfit || 0)),
    w: ($data.dash.todayNetProfit || 0) >= 0 ? $data.theme.primaryColor : "#f56c6c",
    x: common_vendor.t($options.formatAmount($data.dash.monthReceivable || 0)),
    y: common_vendor.t($options.formatAmount($data.dash.monthReceived || 0)),
    z: common_vendor.t($options.formatAmount($data.dash.monthExpense || 0)),
    A: common_vendor.t($options.formatAmount($data.dash.monthNetProfit || 0)),
    B: ($data.dash.monthNetProfit || 0) >= 0 ? $data.theme.primaryColor : "#f56c6c",
    C: common_vendor.t($data.dash.monthOrderCount || 0),
    D: $data.costComposition.length
  }, $data.costComposition.length ? {
    E: common_vendor.f($data.costComposition, (item, k0, i0) => {
      return {
        a: common_vendor.t(item.name || item.category),
        b: (item.percent || 0) + "%",
        c: common_vendor.t($options.formatAmount(item.amount || 0)),
        d: common_vendor.t(item.percent || 0),
        e: item.category
      };
    }),
    F: $data.theme.primaryColor || "#409eff"
  } : {}) : {}, {
    G: !$data.isPro
  }, !$data.isPro ? {
    H: $data.theme.primaryColor
  } : {}, {
    I: common_vendor.p({
      current: "/pages/micro/bossHome/index"
    }),
    J: common_vendor.n("theme-" + $data.theme.key),
    K: common_vendor.s($options.rootStyle)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-d00c6a62"]]);
wx.createPage(MiniProgramPage);
