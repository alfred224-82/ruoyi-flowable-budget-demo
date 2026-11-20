"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_auth = require("../../../utils/auth.js");
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const utils_format = require("../../../utils/format.js");
const utils_roles = require("../../../utils/roles.js");
const utils_theme = require("../../../utils/theme.js");
const utils_features = require("../../../utils/features.js");
const utils_roleDisplay = require("../../../utils/roleDisplay.js");
const MicroTabbar = () => "../../../components/micro-tabbar/micro-tabbar.js";
const PANEL_TITLE_STATIC = {
  activity: "活动状态设置",
  notify: "预约失败通知"
};
const ROOM_STATUS = {
  idle: { label: "空闲", type: "primary-solid" },
  reserved: { label: "预约中", type: "warn-soft" },
  in_use: { label: "使用中", type: "warn-solid" }
};
const STAFF_STATUS = {
  idle: { label: "在岗", type: "primary-soft" },
  working: { label: "忙碌", type: "warn-soft" },
  resting: { label: "休息", type: "gray" },
  reserved: { label: "服务中", type: "warn-solid" }
};
const _sfc_main = {
  components: { MicroTabbar },
  data() {
    return {
      theme: utils_theme.getThemeSync(),
      isPro: false,
      // 自助预约功能开关（FEAT_SELF_BOOKING，资源状态接口功能门槛）
      selfBookingOn: false,
      // ★M28 活动锁定功能开关（FEAT_ACTIVITY_LOCK，锁定/解锁按钮门槛）
      activityLockOn: false,
      // 当前设置面板（room/staff/activity/notify，空=枢纽态）
      panel: "",
      // 枢纽卡数据源
      rooms: [],
      staff: [],
      activities: [],
      failedNotices: [],
      // 业务待办/异常（有数据才展示）
      pendingBookings: [],
      costAnomalyCount: 0,
      // ★BUG-03 行业术语动态化：默认回退名，onShow 后异步替换为租户行业映射名
      staffLabel: "员工",
      spaceLabel: "房间"
    };
  },
  computed: {
    /** ★20261016 四主题重设计：根节点注入完整主题令牌 */
    rootStyle() {
      return utils_theme.themeCssVars(this.theme.key);
    },
    /** 设置态标题（★BUG-03 房间/员工面板标题拼接行业名） */
    panelTitle() {
      if (this.panel === "room") return this.spaceLabel + "状态设置";
      if (this.panel === "staff") return this.staffLabel + "状态设置";
      return PANEL_TITLE_STATIC[this.panel] || "";
    },
    /** 房间枢纽 pill：任一占用/预约中=占用中，否则空闲 */
    roomPill() {
      if (!this.selfBookingOn) return { label: "未开通", type: "gray" };
      if (!this.rooms.length) return { label: "暂无" + this.spaceLabel, type: "gray" };
      const busy = this.rooms.some((r) => r.resourceStatus === "in_use" || r.resourceStatus === "reserved");
      return busy ? { label: "占用中", type: "warn-solid" } : { label: "空闲", type: "primary-solid" };
    },
    /** 员工枢纽 pill：任一在岗/忙碌=在岗，否则休息中 */
    staffPill() {
      if (!this.selfBookingOn) return { label: "未开通", type: "gray" };
      if (!this.staff.length) return { label: "暂无" + this.staffLabel, type: "gray" };
      const onDuty = this.staff.some((u) => u.resourceStatus === "idle" || u.resourceStatus === "working");
      return onDuty ? { label: "在岗", type: "primary-soft" } : { label: "休息中", type: "gray" };
    },
    /** 活动枢纽 pill：取最新一条活动状态 */
    actPill() {
      if (!this.activities.length) return { label: "暂无活动", type: "gray" };
      return this.actStatus(this.activities[0]);
    }
  },
  onShow() {
    if (!utils_auth.getToken()) {
      common_vendor.index.reLaunch({ url: "/pages/login/index" });
      return;
    }
    this.theme = utils_theme.getThemeSync();
    utils_theme.loadTenantTheme().then((t) => {
      this.theme = t;
    });
    let vt = utils_auth.getVersionType();
    if (!vt) {
      const sel = utils_roles.getSelectedTenant();
      vt = sel && sel.versionType || "";
    }
    this.isPro = vt === "micro_pro";
    this.selfBookingOn = utils_features.hasFeature("FEAT_SELF_BOOKING");
    this.activityLockOn = utils_features.hasFeature("FEAT_ACTIVITY_LOCK");
    utils_roleDisplay.getRoleDisplayName("salesman", "员工").then((name) => {
      this.staffLabel = name || "员工";
    });
    utils_roleDisplay.getSpaceDisplayName("房间").then((name) => {
      this.spaceLabel = name || "房间";
    });
    this.loadSummary();
    if (this.selfBookingOn) {
      this.loadFailedNotices();
    }
    this.loadPendingBookings();
    if (this.isPro) {
      this.loadCostAnomaly();
    }
  },
  methods: {
    statusText(s) {
      return (utils_format.BOOKING_STATUS[s] || {}).label || s || "-";
    },
    statusColor(s) {
      return (utils_format.BOOKING_STATUS[s] || {}).color || "#909399";
    },
    formatTime(t) {
      if (!t) return "";
      const d = new Date(typeof t === "string" ? t.replace(/-/g, "/") : t);
      if (isNaN(d.getTime())) return String(t).substring(0, 16);
      const p = (n) => String(n).padStart(2, "0");
      return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`;
    },
    /** 状态映射取值（房间/助教），未知状态兜底灰 pill */
    roomStatus(s) {
      return ROOM_STATUS[s] || { label: s || "空闲", type: "gray" };
    },
    staffStatus(s) {
      return STAFF_STATUS[s] || { label: s || "在岗", type: "gray" };
    },
    /** 活动状态文案：PUBLISHED 未过结束时间=报名中，过时/ENDED=已结束，DRAFT=已下架；★锁定优先展示已锁定 */
    actStatus(a) {
      if (a.status === "DRAFT") return { label: "已下架", type: "gray" };
      if (a.status === "ENDED") return { label: "已结束", type: "gray" };
      if (a.endTime && Date.now() > new Date(a.endTime).getTime()) return { label: "已结束", type: "gray" };
      if (a.isLocked) return { label: "已锁定", type: "warn-solid" };
      return { label: "报名中", type: "success-soft" };
    },
    /** 主题色 pill/按钮内联样式（色板取租户主题下发，不写死） */
    pillStyle(type) {
      const primary = this.theme.primaryColor || "#409eff";
      if (type === "primary-solid") return { background: primary, color: "#fff" };
      if (type === "primary-soft") return { color: primary, background: primary + "1a" };
      return {};
    },
    /** 进入设置态（整屏切换，保持页面一屏不滚动） */
    openPanel(key) {
      this.panel = key;
    },
    closePanel() {
      this.panel = "";
    },
    /** 枢纽卡汇总数据（pill 与设置列表共用） */
    loadSummary() {
      if (this.selfBookingOn) {
        this.loadRooms();
        this.loadStaff();
      }
      this.loadActivities();
    },
    async loadRooms() {
      try {
        const res = await utils_request.get(config_index.config.api.microResourceRoomList);
        this.rooms = res && res.data || [];
      } catch (e) {
        this.rooms = [];
      }
    },
    async loadStaff() {
      try {
        const res = await utils_request.get(config_index.config.api.microResourceStaffList);
        this.staff = res && res.data || [];
      } catch (e) {
        this.staff = [];
      }
    },
    /** 活动列表（/micro/activity/stats，取前 5 条供状态管理） */
    async loadActivities() {
      try {
        const res = await utils_request.get(config_index.config.api.microActivityStats);
        this.activities = (res && res.data || []).slice(0, 5);
      } catch (e) {
        this.activities = [];
      }
    },
    /** 设置房间状态（idle/in_use） */
    async setRoom(r, targetStatus) {
      try {
        await utils_request.put(config_index.config.api.microResourceSet + "?resourceType=room&resourceId=" + r.id + "&targetStatus=" + targetStatus);
        common_vendor.index.showToast({ title: "设置成功", icon: "success" });
        this.loadRooms();
      } catch (e) {
      }
    },
    /** 设置助教状态（idle/working/resting） */
    async setStaff(u, targetStatus) {
      try {
        await utils_request.put(config_index.config.api.microResourceSet + "?resourceType=staff&resourceId=" + u.userId + "&targetStatus=" + targetStatus);
        common_vendor.index.showToast({ title: "设置成功", icon: "success" });
        this.loadStaff();
      } catch (e) {
      }
    },
    /** 活动发布/结束/下架（PUBLISHED/ENDED/DRAFT） */
    async setAct(a, status) {
      try {
        await utils_request.put(config_index.config.api.microPromoStatus + "/" + a.id + "/" + status);
        common_vendor.index.showToast({ title: "操作成功", icon: "success" });
        this.loadActivities();
      } catch (e) {
      }
    },
    /** ★M28 T28.1/T28.5 活动锁定/解锁（解锁需二次确认：解锁后座位可调整） */
    setLock(a, locked) {
      const title = a.title || "";
      if (locked) {
        common_vendor.index.showModal({
          title: "锁定活动",
          content: "锁定后顾客不可报名/选座，座位变只读。确定锁定「" + title + "」吗？",
          success: async (res) => {
            if (!res.confirm) return;
            try {
              await utils_request.put(config_index.config.api.microActivityLockBase + a.id + "/lock");
              common_vendor.index.showToast({ title: "锁定成功", icon: "success" });
              this.loadActivities();
            } catch (e) {
            }
          }
        });
      } else {
        common_vendor.index.showModal({
          title: "解锁活动",
          content: "解锁后座位可调整，确定解锁吗？",
          success: async (res) => {
            if (!res.confirm) return;
            try {
              await utils_request.put(config_index.config.api.microActivityLockBase + a.id + "/unlock");
              common_vendor.index.showToast({ title: "解锁成功", icon: "success" });
              this.loadActivities();
            } catch (e) {
            }
          }
        });
      }
    },
    /** 预约失败通知：按 messageType=RESERVATION_FAILED 过滤 */
    async loadFailedNotices() {
      try {
        const res = await utils_request.get(config_index.config.api.microNotifyList, { messageType: "RESERVATION_FAILED", pageNum: 1, pageSize: 10 });
        this.failedNotices = res && res.rows || [];
      } catch (e) {
        this.failedNotices = [];
      }
    },
    /** 一键重新预约：带原预约 bizId 进入新建预约页回填原参数 */
    rebook(msg) {
      const bizId = msg.bizId || "";
      common_vendor.index.navigateTo({ url: "/pages/micro/booking/add?rebookId=" + bizId });
    },
    /** 预约冲突：仅拉取 CONFLICT 状态订单（正常 PENDING_SERVICE 不应出现在冲突区） */
    async loadPendingBookings() {
      try {
        const res = await utils_request.get(config_index.config.api.microBookingList, { orderStatus: "CONFLICT", pageNum: 1, pageSize: 20 });
        this.pendingBookings = res && res.rows || [];
      } catch (e) {
        this.pendingBookings = [];
      }
    },
    /** 固定支出异常计数：取经营驾驶舱聚合的 costAnomalyCount */
    async loadCostAnomaly() {
      try {
        const res = await utils_request.get(config_index.config.api.microBossDashboard);
        const dash = res && res.data || {};
        this.costAnomalyCount = dash.costAnomalyCount || 0;
      } catch (e) {
        this.costAnomalyCount = 0;
      }
    },
    goBookingAll() {
      common_vendor.index.navigateTo({ url: "/pages/micro/booking/list" });
    },
    goBookingDetail(id) {
      common_vendor.index.navigateTo({ url: "/pages/micro/booking/detail?id=" + id });
    },
    goCost() {
      common_vendor.index.navigateTo({ url: "/pages/micro/cost/list" });
    },
    /** 跳转历史活动全部列表页（支持名称/时间查询） */
    goHistoryAll() {
      common_vendor.index.navigateTo({ url: "/pages/micro/activity/history" });
    },
    /** ★活动详情单页化：统一跳 pages/micro/activity/detail，页面按 id 自取全量数据 */
    viewActivity(item) {
      common_vendor.index.navigateTo({ url: "/pages/micro/activity/detail?id=" + encodeURIComponent(JSON.stringify(item)) });
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
    a: !$data.panel
  }, !$data.panel ? common_vendor.e({
    b: common_vendor.t($data.spaceLabel),
    c: common_vendor.t($data.spaceLabel),
    d: common_vendor.t($options.roomPill.label),
    e: common_vendor.n("t-" + $options.roomPill.type),
    f: common_vendor.s($options.pillStyle($options.roomPill.type)),
    g: $data.theme.primaryColor,
    h: common_vendor.o(($event) => $options.openPanel("room")),
    i: common_vendor.t($data.staffLabel),
    j: common_vendor.t($data.staffLabel),
    k: common_vendor.t($options.staffPill.label),
    l: common_vendor.n("t-" + $options.staffPill.type),
    m: common_vendor.s($options.pillStyle($options.staffPill.type)),
    n: common_vendor.o(($event) => $options.openPanel("staff")),
    o: common_vendor.t($options.actPill.label),
    p: common_vendor.n("t-" + $options.actPill.type),
    q: common_vendor.s($options.pillStyle($options.actPill.type)),
    r: common_vendor.s($options.pillStyle("primary-soft")),
    s: common_vendor.o(($event) => $options.openPanel("activity")),
    t: $data.failedNotices.length
  }, $data.failedNotices.length ? {
    v: common_vendor.t($data.failedNotices.length)
  } : {}, {
    w: common_vendor.o(($event) => $options.openPanel("notify")),
    x: $data.pendingBookings.length
  }, $data.pendingBookings.length ? {
    y: $data.theme.primaryColor,
    z: common_vendor.o((...args) => $options.goBookingAll && $options.goBookingAll(...args)),
    A: common_vendor.f($data.pendingBookings, (item, k0, i0) => {
      return common_vendor.e({
        a: common_vendor.t(item.customerName || "顾客"),
        b: common_vendor.t(item.serviceName || "-"),
        c: common_vendor.t(item.bookingTime || "-"),
        d: common_vendor.t($options.statusText(item.orderStatus)),
        e: $options.statusColor(item.orderStatus),
        f: item.orderStatus === "PENDING_SERVICE"
      }, item.orderStatus === "PENDING_SERVICE" ? {
        g: $data.theme.primaryColor,
        h: common_vendor.o(($event) => $options.goBookingDetail(item.id), item.id)
      } : {}, {
        i: item.id
      });
    })
  } : {}, {
    B: $data.isPro && $data.costAnomalyCount > 0
  }, $data.isPro && $data.costAnomalyCount > 0 ? {
    C: common_vendor.t($data.costAnomalyCount),
    D: common_vendor.o((...args) => $options.goCost && $options.goCost(...args))
  } : {}, {
    E: !$data.isPro
  }, !$data.isPro ? {
    F: $data.theme.primaryColor
  } : {}) : common_vendor.e({
    G: common_vendor.t($options.panelTitle),
    H: $data.theme.primaryColor,
    I: common_vendor.o((...args) => $options.closePanel && $options.closePanel(...args)),
    J: $data.panel === "room"
  }, $data.panel === "room" ? common_vendor.e({
    K: !$data.selfBookingOn
  }, !$data.selfBookingOn ? {
    L: common_vendor.t($data.spaceLabel)
  } : !$data.rooms.length ? {
    N: common_vendor.t($data.spaceLabel)
  } : {
    O: common_vendor.f($data.rooms, (r, k0, i0) => {
      return common_vendor.e({
        a: common_vendor.t(r.name),
        b: common_vendor.t($options.roomStatus(r.resourceStatus).label),
        c: common_vendor.n("t-" + $options.roomStatus(r.resourceStatus).type),
        d: common_vendor.s($options.pillStyle($options.roomStatus(r.resourceStatus).type)),
        e: r.resourceStatus !== "idle"
      }, r.resourceStatus !== "idle" ? {
        f: $data.theme.primaryColor,
        g: common_vendor.o(($event) => $options.setRoom(r, "idle"), r.id)
      } : {}, {
        h: r.resourceStatus !== "in_use"
      }, r.resourceStatus !== "in_use" ? {
        i: common_vendor.o(($event) => $options.setRoom(r, "in_use"), r.id)
      } : {}, {
        j: r.id
      });
    })
  }, {
    M: !$data.rooms.length
  }) : $data.panel === "staff" ? common_vendor.e({
    Q: !$data.selfBookingOn
  }, !$data.selfBookingOn ? {
    R: common_vendor.t($data.staffLabel)
  } : !$data.staff.length ? {
    T: common_vendor.t($data.staffLabel)
  } : {
    U: common_vendor.f($data.staff, (u, k0, i0) => {
      return common_vendor.e({
        a: common_vendor.t(u.nickName),
        b: common_vendor.t($options.staffStatus(u.resourceStatus).label),
        c: common_vendor.n("t-" + $options.staffStatus(u.resourceStatus).type),
        d: common_vendor.s($options.pillStyle($options.staffStatus(u.resourceStatus).type)),
        e: u.resourceStatus !== "idle"
      }, u.resourceStatus !== "idle" ? {
        f: common_vendor.s($options.pillStyle("primary-soft")),
        g: common_vendor.o(($event) => $options.setStaff(u, "idle"), u.userId)
      } : {}, {
        h: u.resourceStatus !== "working"
      }, u.resourceStatus !== "working" ? {
        i: common_vendor.o(($event) => $options.setStaff(u, "working"), u.userId)
      } : {}, {
        j: u.resourceStatus !== "resting"
      }, u.resourceStatus !== "resting" ? {
        k: common_vendor.o(($event) => $options.setStaff(u, "resting"), u.userId)
      } : {}, {
        l: u.userId
      });
    })
  }, {
    S: !$data.staff.length
  }) : $data.panel === "activity" ? common_vendor.e({
    W: !$data.activities.length
  }, !$data.activities.length ? {} : {
    X: common_vendor.f($data.activities, (a, k0, i0) => {
      return common_vendor.e({
        a: common_vendor.t(a.title || "-"),
        b: common_vendor.t($options.actStatus(a).label),
        c: common_vendor.n("t-" + $options.actStatus(a).type),
        d: common_vendor.s($options.pillStyle($options.actStatus(a).type)),
        e: common_vendor.o(($event) => $options.viewActivity(a), a.id),
        f: a.status !== "PUBLISHED"
      }, a.status !== "PUBLISHED" ? {
        g: $data.theme.primaryColor,
        h: common_vendor.o(($event) => $options.setAct(a, "PUBLISHED"), a.id)
      } : {}, {
        i: a.status === "PUBLISHED"
      }, a.status === "PUBLISHED" ? {
        j: common_vendor.o(($event) => $options.setAct(a, "ENDED"), a.id)
      } : {}, {
        k: a.status === "PUBLISHED"
      }, a.status === "PUBLISHED" ? {
        l: common_vendor.o(($event) => $options.setAct(a, "DRAFT"), a.id)
      } : {}, {
        m: $data.activityLockOn && a.status === "PUBLISHED" && !a.isLocked
      }, $data.activityLockOn && a.status === "PUBLISHED" && !a.isLocked ? {
        n: common_vendor.o(($event) => $options.setLock(a, true), a.id)
      } : {}, {
        o: $data.activityLockOn && a.status === "PUBLISHED" && a.isLocked
      }, $data.activityLockOn && a.status === "PUBLISHED" && a.isLocked ? {
        p: common_vendor.o(($event) => $options.setLock(a, false), a.id)
      } : {}, {
        q: a.id
      });
    })
  }, {
    Y: $data.theme.primaryColor,
    Z: common_vendor.o((...args) => $options.goHistoryAll && $options.goHistoryAll(...args))
  }) : common_vendor.e({
    aa: !$data.failedNotices.length
  }, !$data.failedNotices.length ? {} : {
    ab: common_vendor.f($data.failedNotices, (msg, k0, i0) => {
      return {
        a: common_vendor.t(msg.title || "预约失败"),
        b: common_vendor.t(msg.content || ""),
        c: common_vendor.t($options.formatTime(msg.createTime)),
        d: common_vendor.o(($event) => $options.rebook(msg), msg.id),
        e: msg.id
      };
    })
  }), {
    P: $data.panel === "staff",
    V: $data.panel === "activity"
  }), {
    ac: common_vendor.p({
      current: "/pages/micro/bossTodo/index"
    }),
    ad: common_vendor.n("theme-" + $data.theme.key),
    ae: common_vendor.s($options.rootStyle)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-68819407"]]);
wx.createPage(MiniProgramPage);
