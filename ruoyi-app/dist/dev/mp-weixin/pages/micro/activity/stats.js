"use strict";
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const utils_theme = require("../../../utils/theme.js");
const utils_format = require("../../../utils/format.js");
const common_vendor = require("../../../common/vendor.js");
const STATUS_LABEL = { PUBLISHED: "进行中", ENDED: "已结束", DRAFT: "草稿" };
const STATUS_CLASS = { PUBLISHED: "st-on", ENDED: "st-off", DRAFT: "st-draft" };
const _sfc_main = {
  data() {
    return {
      loaded: false,
      activities: [],
      // ★v1.43 租户主题色板（缓存先渲染，接口回包后刷新）
      theme: utils_theme.getThemeSync()
    };
  },
  onShow() {
    this.theme = utils_theme.getThemeSync();
    utils_theme.loadTenantTheme().then((t) => {
      this.theme = t;
    });
    this.loadStats();
  },
  methods: {
    toHttps: utils_format.toHttps,
    firstImage: utils_format.firstImage,
    statusLabel(a) {
      return STATUS_LABEL[a.status] || "已结束";
    },
    statusClass(a) {
      return STATUS_CLASS[a.status] || "st-off";
    },
    /** 进行中徽标按主题主色淡底渲染（8 位 hex 透明度） */
    statusStyle(a) {
      if (a.status !== "PUBLISHED") return {};
      return { background: this.theme.primaryColor + "1a", color: this.theme.primaryColor };
    },
    /** 无封面图时按活动自身 theme_key 渲染渐变占位 */
    activityGradient(themeKey) {
      return utils_theme.themeGradient(themeKey);
    },
    /** 渐变占位封面标题色：浅色渐变用深色字（写死白色在浅色底不可读） */
    gradText(themeKey) {
      return utils_theme.themeGradientText(themeKey).text;
    },
    async loadStats() {
      this.loaded = false;
      try {
        const res = await utils_request.get(config_index.config.api.microActivityStats, { pageNum: 1, pageSize: 20 });
        const data = res && res.data || {};
        this.activities = data.rows || data.list || (Array.isArray(data) ? data : []);
      } catch (e) {
        this.activities = [];
      }
      this.loaded = true;
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: $data.loaded && $data.activities.length
  }, $data.loaded && $data.activities.length ? {
    b: common_vendor.t($data.activities.length)
  } : {}, {
    c: !$data.loaded
  }, !$data.loaded ? {} : !$data.activities.length ? {} : {
    e: common_vendor.f($data.activities, (a, k0, i0) => {
      return common_vendor.e({
        a: a.bgImageUrl || a.posterUrl || a.coverUrl
      }, a.bgImageUrl || a.posterUrl || a.coverUrl ? {
        b: $options.toHttps(a.bgImageUrl || a.posterUrl || $options.firstImage(a.coverUrl))
      } : {
        c: common_vendor.t(a.title || "活动"),
        d: $options.gradText(a.themeKey),
        e: $options.activityGradient(a.themeKey)
      }, {
        f: a.logoUrl
      }, a.logoUrl ? {
        g: $options.toHttps(a.logoUrl)
      } : {}, {
        h: common_vendor.t(a.title || a.name || "活动"),
        i: common_vendor.t($options.statusLabel(a)),
        j: common_vendor.n($options.statusClass(a)),
        k: common_vendor.s($options.statusStyle(a)),
        l: common_vendor.t(a.viewCount || 0),
        m: common_vendor.t(a.participantCount || 0),
        n: common_vendor.t(a.shareCount || 0),
        o: a.startTime || a.endTime
      }, a.startTime || a.endTime ? {
        p: common_vendor.t(a.startTime || ""),
        q: common_vendor.t(a.endTime || "")
      } : {}, {
        r: a.id
      });
    }),
    f: $data.theme.primaryColor,
    g: $data.theme.primaryColor,
    h: $data.theme.primaryColor
  }, {
    d: !$data.activities.length,
    i: $data.theme.bgColor
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-c0899cf5"]]);
wx.createPage(MiniProgramPage);
