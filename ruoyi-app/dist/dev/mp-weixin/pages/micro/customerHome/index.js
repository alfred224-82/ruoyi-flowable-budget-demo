"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_auth = require("../../../utils/auth.js");
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const utils_format = require("../../../utils/format.js");
const utils_roles = require("../../../utils/roles.js");
const utils_theme = require("../../../utils/theme.js");
const utils_themeMixin = require("../../../utils/themeMixin.js");
const utils_themeIcons = require("../../../utils/themeIcons.js");
const utils_features = require("../../../utils/features.js");
const utils_roleDisplay = require("../../../utils/roleDisplay.js");
const utils_bookingPolicy = require("../../../utils/bookingPolicy.js");
const MicroTabbar = () => "../../../components/micro-tabbar/micro-tabbar.js";
const _sfc_main = {
  components: { MicroTabbar },
  mixins: [utils_themeMixin.themeMixin],
  data() {
    return {
      tenantName: "",
      profile: {},
      pendingScene: "",
      actLoaded: false,
      activities: [],
      detailVisible: false,
      detailItem: {},
      // ★自助预约入口开关（FEAT_SELF_BOOKING + 租户策略 customer_self_booking）
      selfBookingOn: false,
      // ★REQ-01 三类资源控制策略（默认全开放，入口文案据此动态展示）
      policy: {
        projectBookingEnabled: 1,
        roomBookingEnabled: 1,
        staffBookingEnabled: 1
      },
      // ★行业角色显示名（salesman 映射：助教/导游/司机，未配置回退员工）
      staffRoleName: "员工",
      // ★T22.5 门店排班
      featStaffSchedule: false,
      storeScheduleLoaded: false,
      storeSchedules: {}
      // { '2026-10-10': [...], '2026-10-11': [...] }
    };
  },
  computed: {
    /** 头部主题渐变背景 */
    headerBg() {
      return utils_theme.themeGradient(this.themeKey);
    },
    /** 头部渐变上的前景文字色（浅色渐变用深色字，深色/高饱和用白字） */
    headerText() {
      return utils_theme.themeGradientText(this.themeKey).text;
    },
    headerSubText() {
      return utils_theme.themeGradientText(this.themeKey).sub;
    },
    /** ★REQ-01 自助预约入口文案：仅列出对客户开放的资源 */
    bookingDesc() {
      const parts = [];
      if (this.policy.projectBookingEnabled !== 0) parts.push("项目");
      if (this.policy.roomBookingEnabled !== 0) parts.push("房间");
      if (this.policy.staffBookingEnabled !== 0) parts.push(this.staffRoleName);
      return (parts.length ? parts.join(" / ") : "服务") + "，按时计费";
    },
    /** ★T22.5 是否有门店排班数据 */
    hasStoreSchedule() {
      return Object.keys(this.storeSchedules).some((k) => this.storeSchedules[k] && this.storeSchedules[k].length > 0);
    },
    /** ★T22.5 按日期分组（仅展示今明两天） */
    storeScheduleGroups() {
      const groups = {};
      const today = /* @__PURE__ */ new Date();
      today.setHours(0, 0, 0, 0);
      const tomorrow = new Date(today.getTime() + 864e5);
      const todayStr = this.toDateStr(today);
      const tomorrowStr = this.toDateStr(tomorrow);
      if (this.storeSchedules[todayStr] && this.storeSchedules[todayStr].length) {
        groups[todayStr] = this.storeSchedules[todayStr];
      }
      if (this.storeSchedules[tomorrowStr] && this.storeSchedules[tomorrowStr].length) {
        groups[tomorrowStr] = this.storeSchedules[tomorrowStr];
      }
      return groups;
    }
  },
  onLoad(options) {
    if (options && options.scene) {
      this.pendingScene = decodeURIComponent(options.scene);
    }
  },
  onShow() {
    if (!utils_auth.getToken()) {
      common_vendor.index.reLaunch({ url: "/pages/login/index" });
      return;
    }
    this.tenantName = utils_roles.getTenantName();
    this.loadActivities();
    this.loadSelfBookingOn();
    utils_roleDisplay.getRoleDisplayName("salesman", "员工").then((n) => {
      this.staffRoleName = n;
    });
    this.featStaffSchedule = utils_features.hasFeature("FEAT_STAFF_SCHEDULE");
    if (this.featStaffSchedule) {
      this.loadStoreSchedules();
    }
    if (this.pendingScene) {
      const scene = this.pendingScene;
      this.pendingScene = "";
      this.handlePromoScene(scene);
    }
  },
  methods: {
    toHttps: utils_format.toHttps,
    firstImage: utils_format.firstImage,
    iconUrl: utils_themeIcons.iconUrl,
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
    // ===== T16.6 扫码归因 =====
    async handlePromoScene(scene) {
      try {
        const params = {};
        scene.split("&").forEach((pair) => {
          const [k, v] = pair.split("=", 2);
          if (k) params[k] = v || "";
        });
        if (!params.u) return;
        const profile = await this.ensureProfile();
        const customerUserId = profile.userId;
        if (!customerUserId) return;
        await utils_request.post(config_index.config.api.microPromoScan, {
          sceneParam: scene,
          customerUserId,
          customerPhone: profile.phonenumber || ""
        });
      } catch (e) {
      }
    },
    async ensureProfile() {
      if (this.profile.userId) return this.profile;
      try {
        const res = await utils_request.get(config_index.config.api.microProfile);
        this.profile = res && res.data || {};
      } catch (e) {
        this.profile = {};
      }
      return this.profile;
    },
    // ===== 最新活动 =====
    async loadActivities() {
      this.actLoaded = false;
      try {
        const tid = utils_roles.getTenantId();
        if (!tid) {
          this.activities = [];
          return;
        }
        const res = await utils_request.get(config_index.config.api.microActivityBrowse, { tenantId: tid });
        const list = res && res.data || [];
        this.activities = list.slice(0, 3);
      } catch (e) {
        this.activities = [];
      } finally {
        this.actLoaded = true;
      }
    },
    goActivity() {
      common_vendor.index.navigateTo({ url: "/pages/micro/activity/browse" });
    },
    goSelfBooking() {
      common_vendor.index.navigateTo({ url: "/pages/micro/selfBooking/index" });
    },
    /** ★自助预约入口开关：版本能力 FEAT_SELF_BOOKING 与租户策略 customer_self_booking 均需开启；
     *   REQ-01：三类资源全部未开放时隐藏入口 */
    async loadSelfBookingOn() {
      if (!utils_features.hasFeature("FEAT_SELF_BOOKING")) {
        this.selfBookingOn = false;
        return;
      }
      const p = await utils_bookingPolicy.getBookingResourcePolicy();
      this.policy = p;
      const anyResource = p.projectBookingEnabled !== 0 || p.roomBookingEnabled !== 0 || p.staffBookingEnabled !== 0;
      this.selfBookingOn = !!p.customerSelfBooking && anyResource;
    },
    viewActivity(item) {
      common_vendor.index.navigateTo({ url: "/pages/micro/activity/detail?id=" + encodeURIComponent(JSON.stringify(item)) });
    },
    // ===== ★T22.5 门店排班 =====
    async loadStoreSchedules() {
      this.storeScheduleLoaded = false;
      try {
        const today = /* @__PURE__ */ new Date();
        today.setHours(0, 0, 0, 0);
        const tomorrow = new Date(today.getTime() + 864e5);
        const res = await utils_request.get(config_index.config.api.microStaffScheduleStore, {
          startDate: this.toDateStr(today),
          endDate: this.toDateStr(tomorrow)
        });
        this.storeSchedules = res && res.data || {};
      } catch (e) {
        this.storeSchedules = {};
      } finally {
        this.storeScheduleLoaded = true;
      }
    },
    toDateStr(d) {
      const y = d.getFullYear();
      const m = String(d.getMonth() + 1).padStart(2, "0");
      const day = String(d.getDate()).padStart(2, "0");
      return y + "-" + m + "-" + day;
    },
    formatDateLabel(dateStr) {
      if (!dateStr) return "";
      const d = new Date(dateStr);
      const today = /* @__PURE__ */ new Date();
      today.setHours(0, 0, 0, 0);
      d.setHours(0, 0, 0, 0);
      const diff = Math.round((d - today) / 864e5);
      if (diff === 0) return "今天";
      if (diff === 1) return "明天";
      return d.getMonth() + 1 + "/" + d.getDate();
    },
    formatHour(t) {
      if (!t) return "";
      return t.substring(0, 5);
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
    a: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    b: $options.iconUrl("hand-star")
  } : {}, {
    c: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    d: $options.iconUrl("hand-sparkle")
  } : {}, {
    e: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    f: $options.iconUrl("hand-sparkle")
  } : {}, {
    g: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    h: $options.iconUrl("hand-bubble")
  } : {}, {
    i: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    j: $options.iconUrl("hand-bubble")
  } : {}, {
    k: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    l: $options.iconUrl("hand-sparkle")
  } : {}, {
    m: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    n: $options.iconUrl("hand-cloud")
  } : {}, {
    o: common_vendor.t($data.tenantName || "欢迎到店"),
    p: $options.headerText,
    q: $options.headerSubText,
    r: $options.headerBg,
    s: common_vendor.o((...args) => $options.goActivity && $options.goActivity(...args)),
    t: !$data.actLoaded
  }, !$data.actLoaded ? {} : !$data.activities.length ? {} : {
    w: common_vendor.f($data.activities, (item, k0, i0) => {
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
        g: common_vendor.o(($event) => $options.viewActivity(item), item.id)
      });
    })
  }, {
    v: !$data.activities.length,
    x: $data.featStaffSchedule
  }, $data.featStaffSchedule ? common_vendor.e({
    y: $data.selfBookingOn
  }, $data.selfBookingOn ? {
    z: common_vendor.t($data.staffRoleName),
    A: common_vendor.o((...args) => $options.goSelfBooking && $options.goSelfBooking(...args))
  } : {}, {
    B: !$data.storeScheduleLoaded
  }, !$data.storeScheduleLoaded ? {} : !$options.hasStoreSchedule ? {} : {
    D: common_vendor.f($options.storeScheduleGroups, (group, date, i0) => {
      return {
        a: common_vendor.t($options.formatDateLabel(date)),
        b: common_vendor.f(group, (sch, k1, i1) => {
          return common_vendor.e({
            a: sch.staffAvatar
          }, sch.staffAvatar ? {
            b: $options.toHttps(sch.staffAvatar)
          } : {}, {
            c: common_vendor.t(sch.staffName || $data.staffRoleName),
            d: common_vendor.t(sch.shiftType === "morning" ? "早班" : "晚班"),
            e: common_vendor.n("ss-shift-" + sch.shiftType),
            f: common_vendor.t($options.formatHour(sch.startTime)),
            g: common_vendor.t($options.formatHour(sch.endTime)),
            h: sch.id
          });
        }),
        c: date
      };
    })
  }, {
    C: !$options.hasStoreSchedule
  }) : {}, {
    E: $data.selfBookingOn
  }, $data.selfBookingOn ? {
    F: common_vendor.t($options.bookingDesc),
    G: common_vendor.o((...args) => $options.goSelfBooking && $options.goSelfBooking(...args))
  } : {}, {
    H: common_vendor.p({
      current: "/pages/micro/customerHome/index"
    }),
    I: common_vendor.n(_ctx.themeClass),
    J: common_vendor.s(_ctx.pageStyle)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-bf901a49"]]);
wx.createPage(MiniProgramPage);
