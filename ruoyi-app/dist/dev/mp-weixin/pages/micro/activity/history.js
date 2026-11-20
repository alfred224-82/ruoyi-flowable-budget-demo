"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const utils_theme = require("../../../utils/theme.js");
const utils_format = require("../../../utils/format.js");
const utils_themeMixin = require("../../../utils/themeMixin.js");
const utils_themeIcons = require("../../../utils/themeIcons.js");
const _sfc_main = {
  mixins: [utils_themeMixin.themeMixin],
  data() {
    return {
      loaded: false,
      list: [],
      // ★搜索条件：名称 + 时间范围
      searchTitle: "",
      searchBegin: "",
      searchEnd: ""
    };
  },
  onLoad() {
    this.loadHistory();
  },
  methods: {
    toHttps: utils_format.toHttps,
    firstImage: utils_format.firstImage,
    iconUrl: utils_themeIcons.iconUrl,
    themeBg(themeKey) {
      return utils_theme.themeGradient(themeKey);
    },
    /** 渐变占位封面标题色：浅色渐变用深色字（写死白色在浅色底不可读） */
    gradText(themeKey) {
      return utils_theme.themeGradientText(themeKey).text;
    },
    /** DRAFT=已取消（boss 下架）；ENDED / 过期 PUBLISHED=已完成 */
    statusLabel(item) {
      return item.status === "DRAFT" ? "已取消" : "已完成";
    },
    async loadHistory() {
      this.loaded = false;
      try {
        const params = { pageNum: 1, pageSize: 50 };
        if (this.searchTitle) {
          params.title = this.searchTitle;
        }
        if (this.searchBegin) {
          params.beginTime = this.searchBegin;
        }
        if (this.searchEnd) {
          params.endTime = this.searchEnd;
        }
        const res = await utils_request.get(config_index.config.api.microActivityHistory, params);
        this.list = res && res.rows || [];
      } catch (e) {
        this.list = [];
      } finally {
        this.loaded = true;
      }
    },
    /** ★搜索（名称 + 时间范围） */
    doSearch() {
      this.loadHistory();
    },
    /** ★重置搜索条件 */
    resetSearch() {
      this.searchTitle = "";
      this.searchBegin = "";
      this.searchEnd = "";
      this.loadHistory();
    },
    onBeginChange(e) {
      this.searchBegin = e.detail.value;
    },
    onEndChange(e) {
      this.searchEnd = e.detail.value;
    },
    /** ★活动详情单页化：所有角色/入口统一跳 pages/micro/activity/detail */
    viewDetail(item) {
      common_vendor.index.navigateTo({ url: "/pages/micro/activity/detail?id=" + encodeURIComponent(JSON.stringify(item)) });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    b: $options.iconUrl("hand-bubble")
  } : {}, {
    c: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    d: $options.iconUrl("hand-sparkle")
  } : {}, {
    e: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    f: $options.iconUrl("hand-bubble")
  } : {}, {
    g: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    h: $options.iconUrl("hand-star")
  } : {}, {
    i: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    j: $options.iconUrl("hand-bubble")
  } : {}, {
    k: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    l: $options.iconUrl("hand-star")
  } : {}, {
    m: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    n: $options.iconUrl("hand-cloud")
  } : {}, {
    o: common_vendor.o((...args) => $options.doSearch && $options.doSearch(...args)),
    p: $data.searchTitle,
    q: common_vendor.o(($event) => $data.searchTitle = $event.detail.value),
    r: common_vendor.t($data.searchBegin || "开始日期"),
    s: $data.searchBegin,
    t: common_vendor.o((...args) => $options.onBeginChange && $options.onBeginChange(...args)),
    v: common_vendor.t($data.searchEnd || "结束日期"),
    w: $data.searchEnd,
    x: common_vendor.o((...args) => $options.onEndChange && $options.onEndChange(...args)),
    y: common_vendor.o((...args) => $options.doSearch && $options.doSearch(...args)),
    z: common_vendor.o((...args) => $options.resetSearch && $options.resetSearch(...args)),
    A: !$data.loaded
  }, !$data.loaded ? {} : !$data.list.length ? {} : {
    C: common_vendor.f($data.list, (item, k0, i0) => {
      return common_vendor.e({
        a: item.bgImageUrl || item.posterUrl || item.coverUrl
      }, item.bgImageUrl || item.posterUrl || item.coverUrl ? {
        b: $options.toHttps(item.bgImageUrl || item.posterUrl || $options.firstImage(item.coverUrl))
      } : {
        c: common_vendor.t(item.title || "活动"),
        d: $options.gradText(item.themeKey),
        e: $options.themeBg(item.themeKey)
      }, {
        f: item.logoUrl
      }, item.logoUrl ? {
        g: $options.toHttps(item.logoUrl)
      } : {}, {
        h: common_vendor.t(item.title || "-"),
        i: common_vendor.t($options.statusLabel(item)),
        j: common_vendor.n(item.status === "DRAFT" ? "st-cancel" : "st-done"),
        k: item.startTime
      }, item.startTime ? {
        l: common_vendor.t(item.startTime),
        m: common_vendor.t(item.endTime || "")
      } : item.publishTime ? {
        o: common_vendor.t(item.publishTime)
      } : {}, {
        n: item.publishTime,
        p: common_vendor.t(item.content ? item.content.length > 60 ? item.content.substring(0, 60) + "..." : item.content : ""),
        q: item.id,
        r: common_vendor.o(($event) => $options.viewDetail(item), item.id)
      });
    })
  }, {
    B: !$data.list.length,
    D: common_vendor.n(_ctx.themeClass),
    E: common_vendor.s(_ctx.pageStyle)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-091d2cb5"]]);
wx.createPage(MiniProgramPage);
