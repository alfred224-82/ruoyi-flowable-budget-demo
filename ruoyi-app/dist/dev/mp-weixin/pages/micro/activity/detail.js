"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const utils_theme = require("../../../utils/theme.js");
const utils_format = require("../../../utils/format.js");
const utils_auth = require("../../../utils/auth.js");
const utils_roles = require("../../../utils/roles.js");
const utils_themeIcons = require("../../../utils/themeIcons.js");
const utils_themeMixin = require("../../../utils/themeMixin.js");
const _sfc_main = {
  mixins: [utils_themeMixin.themeMixin],
  data() {
    return {
      theme: utils_theme.getThemeSync(),
      item: {},
      sd: {},
      role: "",
      logged: false,
      submitting: false,
      voiceEnabled: true,
      innerAudioContext: null,
      isPlaying: false,
      voiceDuration: 0,
      voiceProgress: 0,
      posterIndex: 0,
      statusBarHeight: 0
    };
  },
  computed: {
    cover() {
      return [this.item.bgImageUrl, this.item.posterUrl, utils_format.firstImage(this.item.coverUrl), this.sd.roomBgImageUrl].find(utils_format.isImageUrl) || "";
    },
    posters() {
      const multi = utils_format.splitImages(this.item.coverUrl);
      return multi.length > 1 ? multi : this.cover ? [this.cover] : [];
    },
    heroGradient() {
      return utils_theme.themeGradient(this.item.themeKey || this.theme.key);
    },
    heroDefaultText() {
      return utils_theme.themeGradientText(this.item.themeKey || this.theme.key).text;
    },
    ended() {
      if (this.item.status === "ENDED") return true;
      if (this.item.endTime) {
        return Date.now() > new Date(this.item.endTime).getTime();
      }
      return false;
    },
    statusText() {
      return this.ended ? "已结束" : "报名进行中";
    },
    timeText() {
      if (!this.item.startTime && !this.item.endTime) return "-";
      const s = this.item.startTime ? this.item.startTime.replace("T", " ").substring(0, 16) : "";
      const e = this.item.endTime ? this.item.endTime.replace("T", " ").substring(0, 16) : "";
      return s && e ? `${s} ~ ${e}` : s || e || "-";
    },
    roomText() {
      return (this.sd.roomNames || []).join("、");
    },
    countText() {
      const count = this.sd.signedCount || 0;
      const wait = this.sd.waitlistCount || 0;
      let text = `${count}人已报名`;
      if (wait > 0) text += `，${wait}人候补`;
      return text;
    },
    minMaxText() {
      const quota = this.sd.quota || 0;
      return quota ? `名额${quota}人` : "";
    },
    gapText() {
      const remaining = this.sd.remaining || 0;
      return remaining > 0 ? `还差${remaining}人成团` : "";
    },
    /** ★boss 视角空位占位数（与原报名详情弹窗口径一致：剩余名额最多展示 10 个） */
    emptySeatCount() {
      if (this.role !== "boss") return 0;
      return Math.min(this.sd.remaining || 0, 10);
    },
    submitState() {
      if (!this.logged) return { disabled: true, text: "请先登录" };
      if (this.ended) return { disabled: true, text: "活动已结束" };
      if (this.sd.isLocked) return { disabled: true, text: "活动已锁定" };
      if (this.submitting) return { disabled: true, text: "报名中..." };
      const isMember = (this.sd.members || []).some((m) => m.userId === this.currentUserId);
      if (isMember) return { disabled: true, text: "已报名" };
      if (this.sd.hasSeatLayout) {
        return this.sd.mySeatNo ? { disabled: false, text: "已选座 " + this.sd.mySeatNo + " · 查看座位", toSeat: true } : { disabled: false, text: "选座报名", toSeat: true };
      }
      if (!this.sd.projectId) return { disabled: true, text: "未开放线上报名" };
      const max = this.sd.maxParticipants || 0;
      const count = (this.sd.members || []).filter((m) => !m.isWaitlist).length;
      if (max && count >= max) return { disabled: false, text: "加入候补" };
      return { disabled: false, text: "确认报名" };
    },
    currentUserId() {
      var _a, _b, _c;
      return ((_c = (_b = (_a = this.$store) == null ? void 0 : _a.state) == null ? void 0 : _b.user) == null ? void 0 : _c.id) || "";
    }
  },
  onLoad(options) {
    try {
      this.statusBarHeight = common_vendor.index.getSystemInfoSync().statusBarHeight || 0;
    } catch (e) {
    }
    this.logged = !!utils_auth.getToken();
    this.role = utils_roles.getPrimaryRole() || "";
    this.item = this.parseItemParam(options && options.id);
    this.loadSignupDetail();
  },
  onShow() {
    this.onSeatChanged = () => {
      this.loadSignupDetail();
    };
    common_vendor.index.$on("seatAssignmentChanged", this.onSeatChanged);
  },
  beforeUnmount() {
    if (this.innerAudioContext) {
      this.innerAudioContext.destroy();
      this.innerAudioContext = null;
    }
    if (this.onSeatChanged) {
      common_vendor.index.$off("seatAssignmentChanged", this.onSeatChanged);
      this.onSeatChanged = null;
    }
  },
  methods: {
    toHttps: utils_format.toHttps,
    avatarSize: utils_format.avatarSize,
    iconUrl: utils_themeIcons.iconUrl,
    /** 格式化语音时长 */
    formatDuration(seconds) {
      if (!seconds) return "0:00";
      const m = Math.floor(seconds / 60);
      const s = Math.floor(seconds % 60);
      return m + ":" + String(s).padStart(2, "0");
    },
    /** ★统一详情入口参数解析：兼容完整活动 JSON（各列表页）、仅 bizId 的薄数据 JSON（myActivities）、纯 id 三种传参 */
    parseItemParam(raw) {
      if (!raw) return {};
      const candidates = [raw];
      try {
        candidates.push(decodeURIComponent(raw));
      } catch (e) {
      }
      for (const cand of candidates) {
        try {
          const parsed = JSON.parse(cand);
          if (parsed && typeof parsed === "object") {
            if (!parsed.id && parsed.bizId) parsed.id = parsed.bizId;
            return parsed;
          }
        } catch (e) {
        }
      }
      return /^\d+$/.test(raw) ? { id: raw } : {};
    },
    async loadSignupDetail() {
      if (!this.item.id) return;
      try {
        const res = await utils_request.get(config_index.config.api.microActivitySignupDetail, { activityId: this.item.id });
        this.sd = res && res.data || {};
        if (this.sd.activity && typeof this.sd.activity === "object") {
          this.item = Object.assign({}, this.item, this.sd.activity);
        }
      } catch (e) {
        this.sd = {};
      }
    },
    onSwiperChange(e) {
      this.posterIndex = e.detail.current;
    },
    previewImage(index) {
      if (!this.posters.length) return;
      common_vendor.index.previewImage({
        urls: this.posters.map((u) => utils_format.toHttps(u)),
        current: index
      });
    },
    playVoice() {
      if (!this.item.voiceUrl) return;
      if (!this.innerAudioContext) {
        this.innerAudioContext = common_vendor.index.createInnerAudioContext();
        this.innerAudioContext.src = utils_format.toHttps(this.item.voiceUrl);
        this.innerAudioContext.onTimeUpdate(() => {
          const dur = this.innerAudioContext.duration || 0;
          const cur = this.innerAudioContext.currentTime || 0;
          this.voiceDuration = dur;
          this.voiceProgress = dur > 0 ? cur / dur * 100 : 0;
        });
        this.innerAudioContext.onEnded(() => {
          this.isPlaying = false;
          this.voiceProgress = 0;
        });
      }
      if (this.isPlaying) {
        this.innerAudioContext.pause();
        this.isPlaying = false;
      } else {
        this.innerAudioContext.play();
        this.isPlaying = true;
      }
    },
    goConsent() {
      common_vendor.index.navigateTo({ url: "/pages/micro/customer/consent" });
    },
    async confirmSignup() {
      const st = this.submitState;
      if (st.disabled) return;
      if (!this.logged) {
        common_vendor.index.navigateTo({ url: "/pages/login/index" });
        return;
      }
      if (st.toSeat) {
        common_vendor.index.navigateTo({ url: "/pages/micro/customerBook/seat?activityId=" + this.item.id });
        return;
      }
      this.submitting = true;
      try {
        await utils_request.post(config_index.config.api.microActivitySignup + this.item.id, {});
        common_vendor.index.showToast({ title: "报名成功", icon: "success" });
        this.loadSignupDetail();
      } catch (e) {
        common_vendor.index.showToast({ title: e.message || "报名失败", icon: "none" });
      } finally {
        this.submitting = false;
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    b: $options.iconUrl("hand-sparkle")
  } : {}, {
    c: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    d: $options.iconUrl("hand-star")
  } : {}, {
    e: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    f: $options.iconUrl("hand-heart")
  } : {}, {
    g: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    h: $options.iconUrl("hand-sparkle")
  } : {}, {
    i: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    j: $options.iconUrl("hand-bubble")
  } : {}, {
    k: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    l: $options.iconUrl("hand-cloud")
  } : {}, {
    m: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    n: $options.iconUrl("hand-star")
  } : {}, {
    o: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    p: $options.iconUrl("hand-heart")
  } : {}, {
    q: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    r: $options.iconUrl("hand-bubble")
  } : {}, {
    s: _ctx.themeKey === "theme_handdrawn"
  }, _ctx.themeKey === "theme_handdrawn" ? {
    t: $options.iconUrl("hand-cloud")
  } : {}, {
    v: $options.posters.length > 1
  }, $options.posters.length > 1 ? {
    w: common_vendor.f($options.posters, (url, i, i0) => {
      return {
        a: $options.toHttps(url),
        b: common_vendor.o(($event) => $options.previewImage(i), i),
        c: i
      };
    }),
    x: common_vendor.o((...args) => $options.onSwiperChange && $options.onSwiperChange(...args))
  } : $options.cover ? {
    z: $options.toHttps($options.cover),
    A: common_vendor.o(($event) => $options.previewImage(0))
  } : {
    B: common_vendor.t($data.item.title || "活动"),
    C: $options.heroDefaultText,
    D: $options.heroGradient
  }, {
    y: $options.cover,
    E: $options.posters.length > 1
  }, $options.posters.length > 1 ? {
    F: common_vendor.t($data.posterIndex + 1),
    G: common_vendor.t($options.posters.length)
  } : {}, {
    H: common_vendor.t($data.item.title || "-"),
    I: common_vendor.t($options.statusText),
    J: common_vendor.t($options.timeText),
    K: $options.roomText
  }, $options.roomText ? {
    L: common_vendor.t($options.roomText)
  } : {}, {
    M: ($data.sd.hosts || []).length || $options.roomText
  }, ($data.sd.hosts || []).length || $options.roomText ? {
    N: $data.theme.primaryColor
  } : {}, {
    O: $data.sd.hasSeatLayout
  }, $data.sd.hasSeatLayout ? {
    P: $data.theme.primaryColor
  } : {}, {
    Q: ($data.sd.hosts || []).length
  }, ($data.sd.hosts || []).length ? {
    R: common_vendor.f($data.sd.hosts, (h, i, i0) => {
      return common_vendor.e({
        a: h.avatar
      }, h.avatar ? {
        b: $options.avatarSize(h.avatar, "sm"),
        c: common_vendor.o(($event) => h.avatar = "", i)
      } : {
        d: common_vendor.t((h.name || "?").charAt(0))
      }, {
        e: common_vendor.t(h.name || "-"),
        f: i
      });
    })
  } : {}, {
    S: $data.sd.projectId
  }, $data.sd.projectId ? common_vendor.e({
    T: common_vendor.t($options.countText),
    U: common_vendor.t($options.minMaxText),
    V: ($data.sd.members || []).length || $options.emptySeatCount
  }, ($data.sd.members || []).length || $options.emptySeatCount ? {
    W: common_vendor.f($data.sd.members, (m, i, i0) => {
      return common_vendor.e({
        a: m.avatar
      }, m.avatar ? {
        b: m.isWaitlist ? 1 : "",
        c: $options.avatarSize(m.avatar, "sm"),
        d: common_vendor.o(($event) => m.avatar = "", i)
      } : {
        e: common_vendor.t((m.name || "?").charAt(0)),
        f: m.isWaitlist ? 1 : ""
      }, {
        g: common_vendor.t(m.isWaitlist ? m.name + "·候补" : m.name),
        h: m.isWaitlist ? 1 : "",
        i
      });
    }),
    X: common_vendor.f($options.emptySeatCount, (n, k0, i0) => {
      return {
        a: "empty-" + n
      };
    })
  } : {}, {
    Y: $options.gapText
  }, $options.gapText ? {
    Z: common_vendor.t($options.gapText),
    aa: $data.theme.primaryColor
  } : {}) : {}, {
    ab: $data.item.voiceUrl
  }, $data.item.voiceUrl ? {
    ac: common_vendor.t($data.isPlaying ? "⏸" : "▶️"),
    ad: common_vendor.o((...args) => $options.playVoice && $options.playVoice(...args)),
    ae: $data.voiceProgress + "%",
    af: common_vendor.t($options.formatDuration($data.voiceDuration))
  } : {}, {
    ag: common_vendor.t($data.item.content || "暂无活动说明"),
    ah: $data.item.remark
  }, $data.item.remark ? {
    ai: common_vendor.t($data.item.remark)
  } : {}, {
    aj: $data.voiceEnabled
  }, $data.voiceEnabled ? {
    ak: common_vendor.o((...args) => $options.goConsent && $options.goConsent(...args))
  } : {}, {
    al: $data.sd.perPersonPrice != null && $data.role !== "salesman"
  }, $data.sd.perPersonPrice != null && $data.role !== "salesman" ? common_vendor.e({
    am: common_vendor.t($data.sd.perPersonPrice),
    an: common_vendor.t($data.sd.participantCount || "-"),
    ao: $data.sd.isLocked
  }, $data.sd.isLocked ? {} : {}) : {}, {
    ap: $data.role !== "salesman" && $data.role !== "boss"
  }, $data.role !== "salesman" && $data.role !== "boss" ? {
    aq: common_vendor.t($options.submitState.text),
    ar: $options.submitState.disabled ? 1 : "",
    as: $options.submitState.disabled ? "#c0c4cc" : $data.theme.primaryColor,
    at: common_vendor.o((...args) => $options.confirmSignup && $options.confirmSignup(...args))
  } : {}, {
    av: common_vendor.n("theme-" + _ctx.themeKey),
    aw: common_vendor.s(_ctx.pageStyle)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-6b7683d9"]]);
wx.createPage(MiniProgramPage);
