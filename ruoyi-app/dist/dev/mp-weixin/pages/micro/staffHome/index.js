"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_auth = require("../../../utils/auth.js");
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const utils_format = require("../../../utils/format.js");
const utils_features = require("../../../utils/features.js");
const utils_themeMixin = require("../../../utils/themeMixin.js");
const MicroTabbar = () => "../../../components/micro-tabbar/micro-tabbar.js";
const _sfc_main = {
  components: { MicroTabbar },
  mixins: [utils_themeMixin.themeMixin],
  data() {
    return {
      loaded: false,
      schedule: [],
      todayOrderCount: 0,
      todayRevenue: "0.00",
      avgRating: "-",
      pendingCount: 0,
      completedCount: 0,
      weekOrderCount: 0,
      // ★T22.4 我的排班
      featStaffSchedule: false,
      scheduleLoaded: false,
      mySchedules: []
    };
  },
  computed: {
    /** ★V3.7 我参与的活动（未结束的）：过滤掉已完成/已取消/已爽约等终态 */
    ongoingList() {
      const ended = ["COMPLETED", "CANCELLED", "NO_SHOW"];
      return this.schedule.filter((i) => ended.indexOf(i.orderStatus) === -1);
    },
    /** ★T22.4 按日期分组排班 */
    groupedSchedules() {
      const groups = {};
      this.mySchedules.forEach((sch) => {
        const date = sch.scheduleDate;
        if (!groups[date]) groups[date] = [];
        groups[date].push(sch);
      });
      const sorted = {};
      Object.keys(groups).sort().forEach((k) => {
        sorted[k] = groups[k];
      });
      return sorted;
    }
  },
  onShow() {
    if (!utils_auth.getToken()) {
      common_vendor.index.reLaunch({ url: "/pages/login/index" });
      return;
    }
    this.loadSchedule();
    this.loadTodayStats();
    this.loadWeekStats();
    this.featStaffSchedule = utils_features.hasFeature("FEAT_STAFF_SCHEDULE");
    if (this.featStaffSchedule) {
      this.loadMySchedules();
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
      if (!t) return "-";
      const d = new Date(t);
      if (isNaN(d.getTime())) return String(t).substring(11, 16) || "-";
      return String(d.getHours()).padStart(2, "0") + ":" + String(d.getMinutes()).padStart(2, "0");
    },
    async loadSchedule() {
      this.loaded = false;
      try {
        const res = await utils_request.get(config_index.config.api.microStaffSchedule);
        this.schedule = res && res.data || [];
        this.pendingCount = this.schedule.filter((i) => i.orderStatus === "PENDING_SERVICE").length;
        this.completedCount = this.schedule.filter((i) => i.orderStatus === "COMPLETED").length;
        this.todayOrderCount = this.completedCount;
      } catch (e) {
        this.schedule = [];
      } finally {
        this.loaded = true;
      }
    },
    async loadTodayStats() {
      try {
        const list = this.schedule;
        let revenue = 0;
        list.forEach((i) => {
          if (i.orderStatus === "COMPLETED" && i.amount) revenue += parseFloat(i.amount);
        });
        this.todayRevenue = revenue.toFixed(2);
      } catch (e) {
      }
    },
    async loadWeekStats() {
      try {
        const res = await utils_request.get(config_index.config.api.microStaffIncomeWeek);
        const data = res && res.data || {};
        this.weekOrderCount = data.totalOrders || 0;
      } catch (e) {
      }
    },
    goCheckIn() {
      common_vendor.index.navigateTo({ url: "/pages/micro/checkIn/index" });
    },
    checkinSubmit(item) {
      common_vendor.index.showModal({
        title: "打卡确认",
        content: "确认已完成该顾客的服务？",
        success: async (r) => {
          if (r.confirm) {
            try {
              await utils_request.post(config_index.config.api.microStaffCheckinSubmit + "/" + item.id);
              common_vendor.index.showToast({ title: "打卡成功", icon: "success" });
              this.loadSchedule();
            } catch (e) {
            }
          }
        }
      });
    },
    // ===== ★T22.4 我的排班 =====
    async loadMySchedules() {
      this.scheduleLoaded = false;
      try {
        const now = /* @__PURE__ */ new Date();
        const iso = this.getIsoWeek(now);
        const periodKey = iso.year + "-W" + String(iso.week).padStart(2, "0");
        const res = await utils_request.get(config_index.config.api.microStaffScheduleMy, {
          periodType: "week",
          periodKey
        });
        this.mySchedules = res && res.data || [];
      } catch (e) {
        this.mySchedules = [];
      } finally {
        this.scheduleLoaded = true;
      }
    },
    getIsoWeek(date) {
      const d = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
      const dayNum = d.getUTCDay() || 7;
      d.setUTCDate(d.getUTCDate() + 4 - dayNum);
      const yearStart = new Date(Date.UTC(d.getUTCFullYear(), 0, 1));
      const weekNo = Math.ceil(((d - yearStart) / 864e5 + 1) / 7);
      return { year: d.getUTCFullYear(), week: weekNo };
    },
    formatDateLabel(dateStr) {
      if (!dateStr) return "";
      const d = new Date(dateStr);
      const weekNames = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"];
      const today = /* @__PURE__ */ new Date();
      today.setHours(0, 0, 0, 0);
      d.setHours(0, 0, 0, 0);
      const diff = Math.round((d - today) / 864e5);
      let prefix = "";
      if (diff === 0) prefix = "今天 ";
      else if (diff === 1) prefix = "明天 ";
      else if (diff === -1) prefix = "昨天 ";
      return prefix + (d.getMonth() + 1) + "/" + d.getDate() + " " + weekNames[d.getDay()];
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
    a: common_vendor.t($data.todayOrderCount),
    b: common_vendor.t($data.todayRevenue),
    c: common_vendor.t($data.avgRating),
    d: common_vendor.t($data.pendingCount),
    e: common_vendor.t($data.completedCount),
    f: common_vendor.t($data.weekOrderCount),
    g: !$data.loaded
  }, !$data.loaded ? {} : !$options.ongoingList.length ? {} : {
    i: common_vendor.f($options.ongoingList, (item, k0, i0) => {
      return common_vendor.e({
        a: common_vendor.t(item.customerName || "顾客"),
        b: common_vendor.t(item.serviceName || item.serviceItemName || "-"),
        c: common_vendor.t($options.formatTime(item.bookingTime)),
        d: common_vendor.t($options.statusText(item.orderStatus)),
        e: $options.statusColor(item.orderStatus),
        f: common_vendor.o((...args) => $options.goCheckIn && $options.goCheckIn(...args), item.id),
        g: item.orderStatus === "PENDING_SERVICE"
      }, item.orderStatus === "PENDING_SERVICE" ? {
        h: common_vendor.o(($event) => $options.checkinSubmit(item), item.id)
      } : {}, {
        i: item.id
      });
    })
  }, {
    h: !$options.ongoingList.length,
    j: $data.featStaffSchedule
  }, $data.featStaffSchedule ? common_vendor.e({
    k: common_vendor.o((...args) => $options.loadMySchedules && $options.loadMySchedules(...args)),
    l: !$data.scheduleLoaded
  }, !$data.scheduleLoaded ? {} : !$data.mySchedules.length ? {} : {
    n: common_vendor.f($options.groupedSchedules, (group, date, i0) => {
      return {
        a: common_vendor.t($options.formatDateLabel(date)),
        b: common_vendor.f(group, (sch, k1, i1) => {
          return common_vendor.e({
            a: common_vendor.t(sch.shiftType === "morning" ? "早班" : "晚班"),
            b: common_vendor.n("shift-" + sch.shiftType),
            c: common_vendor.t($options.formatHour(sch.startTime)),
            d: common_vendor.t($options.formatHour(sch.endTime)),
            e: sch.remark
          }, sch.remark ? {
            f: common_vendor.t(sch.remark)
          } : {}, {
            g: sch.id
          });
        }),
        c: date
      };
    })
  }, {
    m: !$data.mySchedules.length
  }) : {}, {
    o: common_vendor.p({
      current: "/pages/micro/staffHome/index"
    }),
    p: common_vendor.n(_ctx.themeClass),
    q: common_vendor.s(_ctx.pageStyle)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-6b889fb9"]]);
wx.createPage(MiniProgramPage);
