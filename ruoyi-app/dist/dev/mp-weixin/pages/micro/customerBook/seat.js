"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const utils_format = require("../../../utils/format.js");
const utils_theme = require("../../../utils/theme.js");
const utils_themeIcons = require("../../../utils/themeIcons.js");
const SEAT_MARKER_GAP_FACTOR = 0.85;
const SEAT_MARKER_FALLBACK_RATIO = 0.04;
const SEAT_MARKER_MAX_RATIO = 0.06;
const SEAT_MARKER_MIN_PX = 12;
const _sfc_main = {
  data() {
    return {
      loaded: false,
      activityId: null,
      // M27 T27.3：租户主题（首屏用缓存同步渲染，onShow 拉取最新）
      theme: utils_theme.getThemeSync(),
      seatMap: {
        activityId: null,
        activityTitle: "",
        roomId: null,
        roomName: "",
        layoutId: null,
        bgImageUrl: null,
        bgImageSource: null,
        layoutWidth: 1920,
        layoutHeight: 1080,
        activityLocked: false,
        totalSeats: 0,
        occupiedSeats: 0,
        freeSeats: 0,
        presetSeats: 0,
        mySeatId: null,
        seats: []
      },
      infoVisible: false,
      infoSeat: {},
      // ★座位圆点直径（px）：按画布实际宽度等比换算，与背景图座位同大小
      seatSize: 0
    };
  },
  computed: {
    /** 当前登录用户的座位（用于底部操作栏展示） */
    mySeat() {
      if (!this.seatMap.mySeatId) return null;
      return (this.seatMap.seats || []).find((s) => s.seatId === this.seatMap.mySeatId) || null;
    },
    /** M27 T27.3：页面根节点注入主题 CSS 变量（座位边框/按钮/链接跟随主题主色） */
    pageStyle() {
      return utils_theme.themeCssVars(this.theme && this.theme.key);
    },
    /** M27 T27.3：背景图改为 <image widthFix> 流内渲染（与 PC 画布/编辑器几何一致），容器仅保留 PRESET 主题铺底色 */
    canvasBgStyle() {
      const style = { "--ss": (this.seatSize || 40) + "px" };
      if (this.seatMap.bgImageSource === "PRESET") {
        style.backgroundColor = utils_theme.themeSoftBg(this.theme && this.theme.key, 0.2);
      }
      return style;
    }
  },
  onLoad(options) {
    this.activityId = options.activityId || options.id || null;
    if (!this.activityId) {
      common_vendor.index.showToast({ title: "缺少活动ID", icon: "none" });
      return;
    }
    this.initSeatSize();
    this._resizeHandler = () => this.refineSeatSize();
    common_vendor.index.onWindowResize(this._resizeHandler);
    this.loadSeatMap();
  },
  onUnload() {
    if (this._resizeHandler) {
      common_vendor.index.offWindowResize(this._resizeHandler);
      this._resizeHandler = null;
    }
  },
  onShow() {
    this.theme = utils_theme.getThemeSync();
    utils_theme.loadTenantTheme().then((t) => {
      this.theme = t;
    });
  },
  methods: {
    toHttps: utils_format.toHttps,
    iconUrl: utils_themeIcons.iconUrl,
    /** 加载活动座位图 */
    async loadSeatMap() {
      try {
        const res = await utils_request.get(config_index.config.api.microSeatMap + this.activityId);
        this.seatMap = res && res.data || this.seatMap;
      } catch (e) {
      } finally {
        this.loaded = true;
        this.$nextTick(() => this.refineSeatSize());
      }
    },
    /** 按屏宽估算圆点直径（首屏用；画布宽 = 屏宽 - 页面/卡片内边距共 88rpx） */
    initSeatSize() {
      const windowWidth = (common_vendor.index.getSystemInfoSync() || {}).windowWidth || 375;
      const canvasWidth = windowWidth - 88 * (windowWidth / 750);
      this.seatSize = this.calcSeatSize(canvasWidth);
    },
    /** 按画布实际宽度校准圆点直径，保证圆点与背景图座位正好同大小 */
    refineSeatSize() {
      common_vendor.index.createSelectorQuery().in(this).select(".canvas-with-bg").boundingClientRect((rect) => {
        if (rect && rect.width) {
          this.seatSize = this.calcSeatSize(rect.width);
        }
      }).exec();
    },
    /** 圆点直径 = 座位点最小邻距（百分比坐标换算为画布宽比例）× 0.85，与背景图座位方块同大小 */
    calcSeatSize(canvasWidth) {
      const seats = this.seatMap.seats || [];
      const aspect = (this.seatMap.layoutHeight || 1080) / (this.seatMap.layoutWidth || 1920);
      let minDist = 0;
      for (let i = 0; i < seats.length; i++) {
        for (let j = i + 1; j < seats.length; j++) {
          const dx = parseFloat(seats[i].posXPercent || 0) - parseFloat(seats[j].posXPercent || 0);
          const dy = (parseFloat(seats[i].posYPercent || 0) - parseFloat(seats[j].posYPercent || 0)) * aspect;
          const dist = Math.sqrt(dx * dx + dy * dy);
          if (dist > 0 && (minDist === 0 || dist < minDist)) minDist = dist;
        }
      }
      const ratio = minDist > 0 ? minDist * SEAT_MARKER_GAP_FACTOR : SEAT_MARKER_FALLBACK_RATIO;
      return Math.max(SEAT_MARKER_MIN_PX, Math.round(canvasWidth * Math.min(ratio, SEAT_MARKER_MAX_RATIO)));
    },
    /** 座位样式类（空闲/已占/预置/我的） */
    seatClass(seat) {
      const cls = [];
      if (seat.status === "FREE") cls.push("seat-free");
      else if (seat.status === "OCCUPIED") cls.push("seat-occupied");
      else if (seat.status === "PRESET") cls.push("seat-preset");
      if (this.seatMap.mySeatId && seat.seatId === this.seatMap.mySeatId) cls.push("seat-mine");
      if (this.seatMap.activityLocked) cls.push("seat-readonly");
      return cls.join(" ");
    },
    /** 座位绝对定位样式（百分比坐标 → 像素） */
    seatStyle(seat) {
      const x = parseFloat(seat.posXPercent || 0) * 100;
      const y = parseFloat(seat.posYPercent || 0) * 100;
      return {
        left: x + "%",
        top: y + "%"
      };
    },
    /** 点击座位 */
    onSeatTap(seat) {
      if (this.seatMap.activityLocked) {
        common_vendor.index.showToast({ title: "活动已锁定，座位只读", icon: "none" });
        return;
      }
      if (seat.status === "FREE") {
        this.confirmPick(seat);
        return;
      }
      if (seat.status === "OCCUPIED" || seat.status === "PRESET") {
        this.infoSeat = seat;
        this.infoVisible = true;
        return;
      }
    },
    /** 确认选座弹窗 */
    confirmPick(seat) {
      common_vendor.index.showModal({
        title: "确认选座",
        content: "您选择座位「" + (seat.seatNo || "-") + "」吗？",
        success: async (r) => {
          if (r.confirm) {
            await this.doPick(seat);
          }
        }
      });
    },
    /** 执行选座 */
    async doPick(seat) {
      try {
        await utils_request.post(config_index.config.api.microSeatPick, {
          activityId: this.activityId,
          seatId: seat.seatId,
          seatNo: seat.seatNo
        });
        common_vendor.index.showToast({ title: "选座成功", icon: "success" });
        this.loadSeatMap();
        common_vendor.index.$emit("seatAssignmentChanged");
      } catch (e) {
      }
    },
    /** 取消自己的座位 */
    releaseMine() {
      if (!this.mySeat || !this.mySeat.assignmentId) {
        common_vendor.index.showToast({ title: "未找到占座记录", icon: "none" });
        return;
      }
      common_vendor.index.showModal({
        title: "取消选座",
        content: "确认取消座位「" + this.mySeat.seatNo + "」吗？",
        success: async (r) => {
          if (r.confirm) {
            try {
              await utils_request.del(config_index.config.api.microSeatRelease + this.mySeat.assignmentId);
              common_vendor.index.showToast({ title: "已取消", icon: "success" });
              this.loadSeatMap();
              common_vendor.index.$emit("seatAssignmentChanged");
            } catch (e) {
            }
          }
        }
      });
    },
    /** 预置角色中文 */
    presetRoleText(role) {
      const map = { guide: "导游", driver: "司机", assistant: "助教" };
      return map[role] || role || "预置";
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: $data.theme && $data.theme.key === "theme_handdrawn"
  }, $data.theme && $data.theme.key === "theme_handdrawn" ? {
    b: $options.iconUrl("hand-star")
  } : {}, {
    c: $data.theme && $data.theme.key === "theme_handdrawn"
  }, $data.theme && $data.theme.key === "theme_handdrawn" ? {
    d: $options.iconUrl("hand-cloud")
  } : {}, {
    e: $data.theme && $data.theme.key === "theme_handdrawn"
  }, $data.theme && $data.theme.key === "theme_handdrawn" ? {
    f: $options.iconUrl("hand-star")
  } : {}, {
    g: $data.theme && $data.theme.key === "theme_handdrawn"
  }, $data.theme && $data.theme.key === "theme_handdrawn" ? {
    h: $options.iconUrl("hand-bubble")
  } : {}, {
    i: $data.theme && $data.theme.key === "theme_handdrawn"
  }, $data.theme && $data.theme.key === "theme_handdrawn" ? {
    j: $options.iconUrl("hand-sparkle")
  } : {}, {
    k: $data.theme && $data.theme.key === "theme_handdrawn"
  }, $data.theme && $data.theme.key === "theme_handdrawn" ? {
    l: $options.iconUrl("hand-sparkle")
  } : {}, {
    m: $data.theme && $data.theme.key === "theme_handdrawn"
  }, $data.theme && $data.theme.key === "theme_handdrawn" ? {
    n: $options.iconUrl("hand-cloud")
  } : {}, {
    o: !$data.loaded
  }, !$data.loaded ? {} : common_vendor.e({
    p: common_vendor.t($data.seatMap.activityTitle || "活动选座"),
    q: common_vendor.t($data.seatMap.totalSeats || 0),
    r: common_vendor.t($data.seatMap.freeSeats || 0),
    s: common_vendor.t($data.seatMap.occupiedSeats || 0),
    t: $data.seatMap.presetSeats > 0
  }, $data.seatMap.presetSeats > 0 ? {
    v: common_vendor.t($data.seatMap.presetSeats)
  } : {}, {
    w: $data.seatMap.activityLocked
  }, $data.seatMap.activityLocked ? {} : {}, {
    x: $data.seatMap.bgImageUrl
  }, $data.seatMap.bgImageUrl ? {
    y: $options.toHttps($data.seatMap.bgImageUrl),
    z: common_vendor.f($data.seatMap.seats, (seat, k0, i0) => {
      return common_vendor.e({
        a: seat.status === "FREE"
      }, seat.status === "FREE" ? {} : seat.userAvatar ? {
        c: $options.toHttps(seat.userAvatar)
      } : {}, {
        b: seat.userAvatar,
        d: seat.status === "PRESET"
      }, seat.status === "PRESET" ? {} : {}, $data.seatSize >= 32 ? {
        e: common_vendor.t(seat.seatNo || "-")
      } : {}, {
        f: seat.seatId || seat.seatNo,
        g: common_vendor.n($options.seatClass(seat)),
        h: common_vendor.s($options.seatStyle(seat)),
        i: common_vendor.o(($event) => $options.onSeatTap(seat), seat.seatId || seat.seatNo)
      });
    }),
    A: $data.seatSize >= 32,
    B: $data.seatMap.bgImageSource === "PRESET" ? 1 : "",
    C: common_vendor.s($options.canvasBgStyle)
  } : common_vendor.e({
    D: !$data.seatMap.seats || $data.seatMap.seats.length === 0
  }, !$data.seatMap.seats || $data.seatMap.seats.length === 0 ? {} : {}, {
    E: common_vendor.f($data.seatMap.seats, (seat, k0, i0) => {
      return common_vendor.e({
        a: seat.status === "FREE"
      }, seat.status === "FREE" ? {} : seat.userAvatar ? {
        c: $options.toHttps(seat.userAvatar)
      } : {}, {
        b: seat.userAvatar,
        d: seat.status === "PRESET"
      }, seat.status === "PRESET" ? {} : {}, {
        e: common_vendor.t(seat.seatNo || "-"),
        f: seat.seatId || seat.seatNo,
        g: common_vendor.n($options.seatClass(seat)),
        h: common_vendor.o(($event) => $options.onSeatTap(seat), seat.seatId || seat.seatNo)
      });
    })
  }), {
    F: $options.mySeat
  }, $options.mySeat ? {
    G: common_vendor.t($options.mySeat.seatNo),
    H: $data.seatMap.activityLocked,
    I: common_vendor.o((...args) => $options.releaseMine && $options.releaseMine(...args))
  } : {
    J: common_vendor.t($data.seatMap.activityLocked ? "活动已锁定，无法选座" : "点击空闲座位（＋）即可选座")
  }), {
    K: $data.infoVisible
  }, $data.infoVisible ? common_vendor.e({
    L: $data.infoSeat.userAvatar
  }, $data.infoSeat.userAvatar ? {
    M: $options.toHttps($data.infoSeat.userAvatar)
  } : {}, {
    N: common_vendor.t($data.infoSeat.userNickName || "已占座"),
    O: common_vendor.t($data.infoSeat.seatNo || "-"),
    P: $data.infoSeat.status === "PRESET"
  }, $data.infoSeat.status === "PRESET" ? {
    Q: common_vendor.t($data.infoSeat.roleDisplayName || $options.presetRoleText($data.infoSeat.presetRole))
  } : {}, {
    R: common_vendor.o(($event) => $data.infoVisible = false),
    S: common_vendor.o(() => {
    }),
    T: common_vendor.o(($event) => $data.infoVisible = false)
  }) : {}, {
    U: common_vendor.n("theme-" + ($data.theme && $data.theme.key)),
    V: common_vendor.s($options.pageStyle)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-33c32ee1"]]);
wx.createPage(MiniProgramPage);
