"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_request = require("../../utils/request.js");
const config_index = require("../../config/index.js");
const utils_format = require("../../utils/format.js");
const utils_theme = require("../../utils/theme.js");
const utils_themeIcons = require("../../utils/themeIcons.js");
const _sfc_main = {
  name: "BossActivityDetail",
  props: {
    /** 是否显示 */
    visible: { type: Boolean, default: false },
    /** 活动对象（browse 列表项，图片 URL 已翻译） */
    item: { type: Object, default: () => ({}) }
  },
  data() {
    return {
      theme: utils_theme.getThemeSync(),
      // 状态栏高度（px，悬浮导航避让刘海屏）
      statusBarHeight: 0,
      // 报名详情聚合（/micro/activity/signupDetail）
      sd: {},
      // ★V3.9 §一.3 多图横滑当前下标（页码徽章展示）
      posterIndex: 0
    };
  },
  created() {
    try {
      this.statusBarHeight = common_vendor.index.getSystemInfoSync().statusBarHeight || 0;
    } catch (e) {
    }
  },
  computed: {
    /** ★20261016 四主题重设计：根节点注入完整主题令牌（沿 DOM 继承到全部子元素） */
    rootStyle() {
      return utils_theme.themeCssVars(this.theme.key);
    },
    /** 悬浮导航 top（状态栏 + 8px 间距） */
    navTop() {
      return this.statusBarHeight + 8 + "px";
    },
    cover() {
      return [this.item.bgImageUrl, this.item.posterUrl, utils_format.firstImage(this.item.coverUrl)].find(utils_format.isImageUrl) || "";
    },
    /** ★V3.9 §一.3 活动海报多图列表（1~6 张横滑）：coverUrl 逗号拆分；无多图回落单封面 */
    posters() {
      const multi = utils_format.splitImages(this.item.coverUrl);
      return multi.length > 1 ? multi : this.cover ? [this.cover] : [];
    },
    heroGradient() {
      return utils_theme.themeGradient(this.item.themeKey || this.theme.key);
    },
    /** 渐变占位头图标题色：浅色渐变用深色字（写死白色在浅色底不可读） */
    heroDefaultText() {
      return utils_theme.themeGradientText(this.item.themeKey || this.theme.key).text;
    },
    /** 名单单元：主持（实心标签）→ 报名成员（候补/角色标签）→ 空位（虚线占位） */
    cells() {
      const list = [];
      (this.sd.hosts || []).forEach((h) => list.push({ ...h, tag: "主持", tagType: "host" }));
      (this.sd.members || []).forEach((m) => list.push({
        ...m,
        tag: m.isWaitlist ? "候补" : m.roleLabel || "",
        tagType: m.isWaitlist ? "wait" : "member"
      }));
      const empty = Math.min(this.sd.remaining || 0, 10);
      for (let i = 0; i < empty; i++) {
        list.push({ isEmpty: true, name: "空位" });
      }
      return list;
    }
  },
  watch: {
    visible(val) {
      if (val) {
        this.theme = utils_theme.getThemeSync();
        utils_theme.loadTenantTheme().then((t) => {
          this.theme = t;
        });
        this.loadSignupDetail();
        this.posterIndex = 0;
      } else {
        this.sd = {};
      }
    }
  },
  methods: {
    toHttps: utils_format.toHttps,
    avatarSize: utils_format.avatarSize,
    iconUrl: utils_themeIcons.iconUrl,
    close() {
      this.$emit("close");
    },
    /** 头像加载失败时清除 URL，回退显示首字 */
    onAvatarError(cell) {
      cell.avatar = "";
    },
    /** ★多图横滑切换：同步页码徽章 */
    onSwiperChange(e) {
      this.posterIndex = e && e.detail && typeof e.detail.current === "number" ? e.detail.current : 0;
    },
    /** ★点击头图放大预览：传入当前下标，多图则从当前张开始 */
    previewImage(index) {
      const urls = (this.posters.length ? this.posters : this.cover ? [this.cover] : []).map((u) => utils_format.toHttps(u));
      if (!urls.length) return;
      common_vendor.index.previewImage({ current: index, urls });
    },
    async loadSignupDetail() {
      if (!this.item || !this.item.id) return;
      try {
        const res = await utils_request.get(config_index.config.api.microActivitySignupDetail, { activityId: this.item.id });
        this.sd = res && res.data || {};
      } catch (e) {
        this.sd = {};
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: $props.visible
  }, $props.visible ? common_vendor.e({
    b: $options.posters.length > 1
  }, $options.posters.length > 1 ? {
    c: common_vendor.f($options.posters, (url, i, i0) => {
      return {
        a: $options.toHttps(url),
        b: common_vendor.o(($event) => $options.previewImage(i), i),
        c: i
      };
    }),
    d: common_vendor.o((...args) => $options.onSwiperChange && $options.onSwiperChange(...args))
  } : $options.cover ? {
    f: $options.toHttps($options.cover),
    g: common_vendor.o(($event) => $options.previewImage(0))
  } : {
    h: common_vendor.t($props.item.title || "活动"),
    i: $options.heroDefaultText,
    j: $options.heroGradient
  }, {
    e: $options.cover,
    k: $options.posters.length > 1
  }, $options.posters.length > 1 ? {
    l: common_vendor.t($data.posterIndex + 1),
    m: common_vendor.t($options.posters.length)
  } : {}, {
    n: common_vendor.t($data.sd.signedCount || 0),
    o: $data.theme.primaryColor,
    p: $data.sd.quota
  }, $data.sd.quota ? {
    q: common_vendor.t($data.sd.quota)
  } : {}, {
    r: $data.sd.quota
  }, $data.sd.quota ? {
    s: common_vendor.t($data.sd.remaining || 0),
    t: $data.theme.primaryColor
  } : {}, {
    v: $data.sd.waitlistCount
  }, $data.sd.waitlistCount ? {
    w: common_vendor.t($data.sd.waitlistCount)
  } : {}, {
    x: $data.theme.key === "theme_handdrawn"
  }, $data.theme.key === "theme_handdrawn" ? {
    y: $options.iconUrl("hand-bubble")
  } : {}, {
    z: $data.theme.key === "theme_handdrawn"
  }, $data.theme.key === "theme_handdrawn" ? {
    A: $options.iconUrl("hand-star")
  } : {}, {
    B: !$options.cells.length
  }, !$options.cells.length ? {} : {
    C: common_vendor.f($options.cells, (cell, idx, i0) => {
      return common_vendor.e({
        a: cell.avatar
      }, cell.avatar ? {
        b: $options.avatarSize(cell.avatar, "sm"),
        c: common_vendor.o(($event) => $options.onAvatarError(cell), idx)
      } : common_vendor.e({
        d: !cell.isEmpty
      }, !cell.isEmpty ? {
        e: common_vendor.t((cell.name || "?").charAt(0))
      } : {}, {
        f: cell.isEmpty ? 1 : ""
      }), {
        g: cell.tag
      }, cell.tag ? {
        h: common_vendor.t(cell.tag),
        i: common_vendor.n("tag-" + cell.tagType),
        j: common_vendor.s(cell.tagType === "host" ? {
          background: $data.theme.primaryColor,
          borderColor: $data.theme.primaryColor
        } : {})
      } : {}, {
        k: !cell.isEmpty
      }, !cell.isEmpty ? {
        l: common_vendor.t(cell.name)
      } : {}, {
        m: idx
      });
    })
  }, {
    D: $props.item.startTime
  }, $props.item.startTime ? {
    E: common_vendor.t($props.item.startTime),
    F: common_vendor.t($props.item.endTime)
  } : {}, {
    G: common_vendor.t($props.item.content || "暂无活动说明"),
    H: $props.item.remark
  }, $props.item.remark ? {
    I: common_vendor.t($props.item.remark)
  } : {}, {
    J: $options.navTop,
    K: common_vendor.o((...args) => $options.close && $options.close(...args)),
    L: $options.navTop,
    M: $data.theme.primaryColor,
    N: common_vendor.o((...args) => $options.close && $options.close(...args)),
    O: common_vendor.n("theme-" + $data.theme.key),
    P: common_vendor.s($options.rootStyle)
  }) : {});
}
const Component = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-a42ac4ce"]]);
wx.createComponent(Component);
