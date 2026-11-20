"use strict";
const common_vendor = require("../../../common/vendor.js");
const config_index = require("../../../config/index.js");
const utils_request = require("../../../utils/request.js");
const utils_roleDisplay = require("../../../utils/roleDisplay.js");
const _sfc_main = {
  data() {
    return {
      // 从 selfBooking 传入的服务信息
      serviceName: "",
      serviceId: null,
      unitPrice: 0,
      bookingType: "project",
      // project / staff / room
      staffIds: [],
      roomIds: [],
      staffHourlyRate: 0,
      roomHourlyRate: 0,
      capacityMin: 2,
      capacityMax: 6,
      // 日期
      dateList: [],
      selectedDateIndex: 1,
      // 默认明天
      // 时间段（整点，每个 = 1小时）
      timeSlots: ["10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00", "18:00", "19:00", "20:00"],
      selectedSlots: [],
      // 时长 & 人数
      duration: 2,
      peopleCount: 4,
      // ★行业角色显示名（salesman 映射：助教/导游/司机，未配置回退员工）
      staffRoleName: "员工"
    };
  },
  computed: {
    capacityRange() {
      return this.capacityMin + "-" + this.capacityMax + "人";
    },
    // ★今天最早可选开始时间（小时）= 当前小时 + 1（如当前17点→最早18点）
    minStartHour() {
      return (/* @__PURE__ */ new Date()).getHours() + 1;
    },
    baseCost() {
      return this.bookingType === "project" ? this.unitPrice * this.duration : 0;
    },
    roomCost() {
      return this.roomHourlyRate * this.duration;
    },
    staffCost() {
      return this.staffHourlyRate * this.duration;
    },
    totalPrice() {
      return this.baseCost + this.roomCost + this.staffCost;
    }
  },
  onLoad(options) {
    utils_roleDisplay.getRoleDisplayName("salesman", "员工").then((n) => {
      this.staffRoleName = n;
    });
    this.serviceName = decodeURIComponent(options.serviceName || "");
    this.serviceId = options.serviceId || null;
    this.unitPrice = Number(options.unitPrice || 0);
    this.bookingType = options.bookingType || "project";
    this.staffIds = options.staffIds ? options.staffIds.split(",").filter(Boolean) : [];
    this.roomIds = options.roomIds ? options.roomIds.split(",").filter(Boolean) : [];
    this.staffHourlyRate = Number(options.staffHourlyRate || 0);
    this.roomHourlyRate = Number(options.roomHourlyRate || 0);
    this.capacityMin = Number(options.capacityMin || 2);
    this.capacityMax = Number(options.capacityMax || 6);
    this.buildDateList();
    this.selectedSlots = ["13:00", "14:00"];
    this.sanitizeSlots();
  },
  methods: {
    buildDateList() {
      const days = ["周日", "周一", "周二", "周三", "周四", "周五", "周六"];
      const now = /* @__PURE__ */ new Date();
      this.dateList = [];
      for (let i = 0; i < 5; i++) {
        const d = new Date(now.getTime() + i * 864e5);
        const mm = String(d.getMonth() + 1).padStart(2, "0");
        const dd = String(d.getDate()).padStart(2, "0");
        let label = days[d.getDay()];
        if (i === 0) label = "今天";
        else if (i === 1) label = "明天";
        this.dateList.push({ mmdd: mm + "/" + dd, label });
      }
    },
    selectDate(index) {
      this.selectedDateIndex = index;
      this.sanitizeSlots();
    },
    // ★判断时段是否已过（仅当天限制）：开始时间必须晚于当前时间1小时
    isSlotDisabled(slot) {
      if (this.selectedDateIndex !== 0) return false;
      const hour = parseInt(slot.split(":")[0], 10);
      return hour < this.minStartHour;
    },
    // ★清除已过期选中时段，并在全部过期时自动选择最早可用时段
    sanitizeSlots() {
      if (this.selectedDateIndex !== 0) return;
      const valid = this.selectedSlots.filter((s) => !this.isSlotDisabled(s));
      if (valid.length > 0) {
        this.selectedSlots = valid;
      } else {
        const firstAvailable = this.timeSlots.find((s) => !this.isSlotDisabled(s));
        this.selectedSlots = firstAvailable ? [firstAvailable] : [];
      }
      this.duration = Math.max(1, this.selectedSlots.length);
    },
    toggleSlot(slot) {
      if (this.isSlotDisabled(slot)) {
        common_vendor.index.showToast({ title: "该时段已过，请选择更晚时间", icon: "none" });
        return;
      }
      const i = this.selectedSlots.indexOf(slot);
      if (i >= 0) {
        this.selectedSlots.splice(i, 1);
      } else {
        this.selectedSlots.push(slot);
        this.selectedSlots.sort();
      }
      this.duration = Math.max(1, this.selectedSlots.length);
    },
    changeDuration(delta) {
      const next = this.duration + delta;
      if (next < 1 || next > this.timeSlots.length) return;
      this.duration = next;
      if (this.selectedSlots.length === 0) {
        const startIdx = this.timeSlots.findIndex((s) => !this.isSlotDisabled(s));
        if (startIdx < 0) return;
        this.selectedSlots = this.timeSlots.slice(startIdx, startIdx + next);
      } else {
        const first = this.selectedSlots[0];
        const firstIdx = this.timeSlots.indexOf(first);
        this.selectedSlots = this.timeSlots.slice(firstIdx, firstIdx + next);
      }
    },
    changePeople(delta) {
      const next = this.peopleCount + delta;
      if (next < 1) return;
      this.peopleCount = next;
    },
    goBack() {
      common_vendor.index.navigateBack();
    },
    async onConfirm() {
      if (!this.serviceId) {
        common_vendor.index.showToast({ title: "请选择服务", icon: "none" });
        return;
      }
      if (this.selectedSlots.length === 0) {
        common_vendor.index.showToast({ title: "请选择时间段", icon: "none" });
        return;
      }
      if (this.selectedSlots.some((s) => this.isSlotDisabled(s))) {
        common_vendor.index.showToast({ title: "所选时段已过，请重新选择", icon: "none" });
        return;
      }
      const d = this.dateList[this.selectedDateIndex];
      const year = (/* @__PURE__ */ new Date()).getFullYear();
      const bookingDate = year + "-" + d.mmdd.replace("/", "-");
      const lastHour = parseInt(this.selectedSlots[this.selectedSlots.length - 1].split(":")[0], 10) + 1;
      const timeSlot = this.selectedSlots[0] + "-" + String(lastHour).padStart(2, "0") + ":00";
      const data = {
        bookingType: this.bookingType,
        mainResourceId: this.serviceId,
        optionalProjectIds: [],
        optionalStaffIds: this.staffIds,
        optionalRoomIds: this.roomIds,
        bookingDate,
        bookingTimeSlot: timeSlot,
        quantity: this.duration,
        peopleCount: this.peopleCount
      };
      try {
        await utils_request.request({ url: config_index.config.api.microSelfBookingSubmit, method: "POST", data });
        common_vendor.index.showToast({ title: "预约成功", icon: "success" });
        setTimeout(() => {
          common_vendor.index.navigateBack({ delta: 2 });
        }, 1500);
      } catch (e) {
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.t($data.serviceName || "未选择"),
    b: common_vendor.t($data.unitPrice),
    c: common_vendor.o((...args) => $options.goBack && $options.goBack(...args)),
    d: common_vendor.f($data.dateList, (d, i, i0) => {
      return {
        a: common_vendor.t(d.mmdd),
        b: common_vendor.t(d.label),
        c: $data.selectedDateIndex === i ? 1 : "",
        d: i,
        e: common_vendor.n($data.selectedDateIndex === i ? "active" : ""),
        f: common_vendor.o(($event) => $options.selectDate(i), i)
      };
    }),
    e: $data.selectedDateIndex === 0
  }, $data.selectedDateIndex === 0 ? {
    f: common_vendor.t($options.minStartHour)
  } : {}, {
    g: common_vendor.f($data.timeSlots, (slot, k0, i0) => {
      return {
        a: common_vendor.t(slot),
        b: slot,
        c: common_vendor.n($data.selectedSlots.includes(slot) ? "active" : ""),
        d: common_vendor.n($options.isSlotDisabled(slot) ? "disabled" : ""),
        e: common_vendor.o(($event) => $options.toggleSlot(slot), slot)
      };
    }),
    h: common_vendor.t($data.unitPrice),
    i: $data.duration <= 1 ? 1 : "",
    j: common_vendor.o(($event) => $options.changeDuration(-1)),
    k: common_vendor.t($data.duration),
    l: common_vendor.o(($event) => $options.changeDuration(1)),
    m: common_vendor.t($options.capacityRange),
    n: $data.peopleCount <= 1 ? 1 : "",
    o: common_vendor.o(($event) => $options.changePeople(-1)),
    p: common_vendor.t($data.peopleCount),
    q: common_vendor.o(($event) => $options.changePeople(1)),
    r: $data.bookingType === "project"
  }, $data.bookingType === "project" ? {
    s: common_vendor.t($data.unitPrice),
    t: common_vendor.t($data.duration)
  } : {}, {
    v: common_vendor.t($options.roomCost),
    w: common_vendor.t($data.staffRoleName),
    x: common_vendor.t($options.staffCost),
    y: common_vendor.t($options.totalPrice),
    z: common_vendor.o((...args) => $options.goBack && $options.goBack(...args)),
    A: common_vendor.o((...args) => $options.onConfirm && $options.onConfirm(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-3554e5f0"]]);
wx.createPage(MiniProgramPage);
