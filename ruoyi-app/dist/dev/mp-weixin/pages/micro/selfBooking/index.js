"use strict";
const common_vendor = require("../../../common/vendor.js");
const config_index = require("../../../config/index.js");
const utils_request = require("../../../utils/request.js");
const utils_format = require("../../../utils/format.js");
const utils_roleDisplay = require("../../../utils/roleDisplay.js");
const utils_bookingPolicy = require("../../../utils/bookingPolicy.js");
const _sfc_main = {
  data() {
    const now = /* @__PURE__ */ new Date();
    const fmt = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
    return {
      today: fmt(now),
      staffList: [],
      staffLoaded: false,
      roomList: [],
      roomsLoaded: false,
      // 房间单选 + 助教多选（组合预约：1 房间 + N 助教）
      selectedRoomId: null,
      selectedStaffIds: [],
      // ★行业角色显示名（salesman 映射：助教/导游/司机，未配置回退员工）
      staffRoleName: "员工",
      // ★REQ-01 三类资源控制策略（getBookingResourcePolicy 下发；默认全开放/非必填）
      policy: {
        customerSelfBooking: true,
        projectBookingEnabled: 1,
        projectBookingRequired: 0,
        roomBookingEnabled: 1,
        roomBookingRequired: 0,
        staffBookingEnabled: 1,
        staffBookingRequired: 0,
        industryCode: null
      }
    };
  },
  computed: {
    hasSelection() {
      return !!this.selectedRoomId || this.selectedStaffIds.length > 0;
    },
    selectedRoom() {
      return this.roomList.find((r) => r.id === this.selectedRoomId) || null;
    },
    // REQ-01：场地/员工是否对客户开放（enabled=0 隐藏入口）
    showRoom() {
      return this.policy.roomBookingEnabled !== 0;
    },
    showStaff() {
      return this.policy.staffBookingEnabled !== 0;
    },
    // REQ-01：场地/员工是否必填（required=1 提交前强制校验）
    roomRequired() {
      return this.policy.roomBookingRequired === 1;
    },
    staffRequired() {
      return this.policy.staffBookingRequired === 1;
    }
  },
  onLoad() {
    utils_roleDisplay.getRoleDisplayName("salesman", "员工").then((n) => {
      this.staffRoleName = n;
    });
    this.guardAndLoad(true);
  },
  onShow() {
    if (this.roomsLoaded || this.staffLoaded) this.guardAndLoad(false);
  },
  methods: {
    toHttps: utils_format.toHttps,
    avatarSize: utils_format.avatarSize,
    /** ★预约策略闸门：租户未开启顾客自助预约时提示并返回；否则按 policy 按需加载已开放资源 */
    guardAndLoad(first) {
      utils_bookingPolicy.getBookingResourcePolicy().then((p) => {
        this.policy = p;
        if (!p.customerSelfBooking) {
          common_vendor.index.showToast({ title: "本店未开启自助预约", icon: "none" });
          setTimeout(() => common_vendor.index.navigateBack(), 800);
          return;
        }
        if (this.showRoom && (first || this.roomsLoaded)) this.loadRooms();
        if (this.showStaff && (first || this.staffLoaded)) this.loadStaff();
      });
    },
    toggleRoom(id) {
      this.selectedRoomId = this.selectedRoomId === id ? null : id;
    },
    toggleStaff(id) {
      const i = this.selectedStaffIds.indexOf(id);
      if (i >= 0) this.selectedStaffIds.splice(i, 1);
      else this.selectedStaffIds.push(id);
    },
    onLinkRooms() {
      common_vendor.index.showToast({ title: `共${this.roomList.length}间空闲房间可预约`, icon: "none" });
    },
    onLinkStaff() {
      common_vendor.index.showToast({ title: `共${this.staffList.length}位${this.staffRoleName}可预约`, icon: "none" });
    },
    onConfirm() {
      if (this.roomRequired && !this.selectedRoomId) {
        common_vendor.index.showToast({ title: "请选择房间", icon: "none" });
        return;
      }
      if (this.staffRequired && !this.selectedStaffIds.length) {
        common_vendor.index.showToast({ title: `请选择${this.staffRoleName}`, icon: "none" });
        return;
      }
      if (!this.hasSelection) {
        common_vendor.index.showToast({ title: `请先选择房间或${this.staffRoleName}`, icon: "none" });
        return;
      }
      const staffs = this.staffList.filter((s) => this.selectedStaffIds.includes(s.id));
      const rateOf = (s) => Number(s.hourlyRate || 0);
      const staffRateSum = staffs.reduce((sum, s) => sum + rateOf(s), 0);
      let params;
      if (this.selectedRoom) {
        const room = this.selectedRoom;
        params = {
          bookingType: "room",
          serviceId: room.id,
          serviceName: encodeURIComponent(room.name),
          unitPrice: room.price || 0,
          staffIds: staffs.map((s) => s.id).join(","),
          roomIds: "",
          staffHourlyRate: staffRateSum,
          roomHourlyRate: room.price || 0,
          capacityMin: room.capacity ? Math.max(2, Math.floor(room.capacity * 0.5)) : 2,
          capacityMax: room.capacity || 6
        };
      } else {
        const main = staffs[0];
        params = {
          bookingType: "staff",
          serviceId: main.id,
          serviceName: encodeURIComponent(main.name),
          unitPrice: rateOf(main),
          staffIds: staffs.slice(1).map((s) => s.id).join(","),
          roomIds: "",
          staffHourlyRate: staffRateSum,
          roomHourlyRate: 0,
          capacityMin: 2,
          capacityMax: 6
        };
      }
      const query = Object.entries(params).map(([k, v]) => k + "=" + v).join("&");
      common_vendor.index.navigateTo({ url: "/pages/micro/selfBooking/confirm?" + query });
    },
    loadRooms() {
      this.roomsLoaded = false;
      utils_request.request({
        url: config_index.config.api.microSelfBookingRooms,
        method: "GET",
        data: { bookingDate: this.today }
      }).then((res) => {
        this.roomList = res.data || [];
        if (this.selectedRoomId && !this.roomList.some((r) => r.id === this.selectedRoomId)) {
          this.selectedRoomId = null;
        }
      }).finally(() => {
        this.roomsLoaded = true;
      });
    },
    loadStaff() {
      this.staffLoaded = false;
      utils_request.request({
        url: config_index.config.api.microSelfBookingStaff,
        method: "GET",
        data: { bookingDate: this.today }
      }).then((res) => {
        this.staffList = res.data || [];
        this.selectedStaffIds = this.selectedStaffIds.filter((id) => this.staffList.some((s) => s.id === id));
      }).finally(() => {
        this.staffLoaded = true;
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.t($data.staffRoleName),
    b: common_vendor.t($data.staffRoleName),
    c: common_vendor.t($data.staffRoleName),
    d: $options.showRoom
  }, $options.showRoom ? common_vendor.e({
    e: $options.roomRequired
  }, $options.roomRequired ? {} : {}, {
    f: common_vendor.o((...args) => $options.onLinkRooms && $options.onLinkRooms(...args)),
    g: !$data.roomsLoaded
  }, !$data.roomsLoaded ? {} : !$data.roomList.length ? {} : {
    i: common_vendor.f($data.roomList, (room, k0, i0) => {
      return common_vendor.e({
        a: room.coverUrl
      }, room.coverUrl ? {
        b: $options.toHttps(room.coverUrl)
      } : {}, {
        c: $data.selectedRoomId === room.id
      }, $data.selectedRoomId === room.id ? {} : {}, {
        d: common_vendor.t(room.name),
        e: common_vendor.t(room.capacity ? room.capacity + "人" : room.description || ""),
        f: $data.selectedRoomId === room.id
      }, $data.selectedRoomId === room.id ? {} : {}, {
        g: room.id,
        h: common_vendor.n($data.selectedRoomId === room.id ? "selected" : ""),
        i: common_vendor.o(($event) => $options.toggleRoom(room.id), room.id)
      });
    })
  }, {
    h: !$data.roomList.length
  }) : {}, {
    j: $options.showStaff
  }, $options.showStaff ? common_vendor.e({
    k: common_vendor.t($data.staffRoleName),
    l: $options.staffRequired
  }, $options.staffRequired ? {} : {}, {
    m: common_vendor.o((...args) => $options.onLinkStaff && $options.onLinkStaff(...args)),
    n: !$data.staffLoaded
  }, !$data.staffLoaded ? {} : !$data.staffList.length ? {
    p: common_vendor.t($data.staffRoleName)
  } : {
    q: common_vendor.f($data.staffList, (staff, k0, i0) => {
      return common_vendor.e({
        a: staff.avatar
      }, staff.avatar ? {
        b: $options.avatarSize(staff.avatar, "sm"),
        c: common_vendor.o(($event) => staff.avatar = "", staff.id)
      } : {
        d: common_vendor.t((staff.name || "?")[0])
      }, {
        e: common_vendor.t(staff.name),
        f: common_vendor.t($data.selectedStaffIds.includes(staff.id) ? "已选" : "选择"),
        g: common_vendor.n($data.selectedStaffIds.includes(staff.id) ? "staff-btn-on" : ""),
        h: staff.id,
        i: common_vendor.n($data.selectedStaffIds.includes(staff.id) ? "selected" : ""),
        j: common_vendor.o(($event) => $options.toggleStaff(staff.id), staff.id)
      });
    }),
    r: common_vendor.t($data.staffRoleName)
  }, {
    o: !$data.staffList.length
  }) : {}, {
    s: !$options.hasSelection
  }, !$options.hasSelection ? {} : common_vendor.e({
    t: $options.selectedRoom
  }, $options.selectedRoom ? {
    v: common_vendor.t($options.selectedRoom.name)
  } : {}, {
    w: $data.selectedStaffIds.length
  }, $data.selectedStaffIds.length ? {} : {}, {
    x: $data.selectedStaffIds.length
  }, $data.selectedStaffIds.length ? {
    y: common_vendor.t($data.selectedStaffIds.length)
  } : {}, {
    z: $data.selectedStaffIds.length
  }, $data.selectedStaffIds.length ? {
    A: common_vendor.t($data.staffRoleName)
  } : {}), {
    B: !$options.hasSelection ? 1 : "",
    C: common_vendor.o((...args) => $options.onConfirm && $options.onConfirm(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-7a545587"]]);
wx.createPage(MiniProgramPage);
