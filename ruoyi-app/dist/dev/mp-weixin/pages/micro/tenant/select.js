"use strict";
const common_vendor = require("../../../common/vendor.js");
const config_index = require("../../../config/index.js");
const utils_request = require("../../../utils/request.js");
const utils_auth = require("../../../utils/auth.js");
const utils_roles = require("../../../utils/roles.js");
const utils_store = require("../../../utils/store.js");
const utils_format = require("../../../utils/format.js");
const utils_theme = require("../../../utils/theme.js");
const _sfc_main = {
  data() {
    return { loaded: false, loading: false, list: [], selectedId: null, remember: true, mode: "tenant", themeKey: utils_theme.resolveThemeKey(null), pinnedTheme: null };
  },
  computed: {
    /** M27 T27.5：页面根节点注入主题 CSS 变量（4 风格变体共用） */
    pageStyle() {
      return utils_theme.themeCssVars(this.themeKey);
    }
  },
  onLoad(options) {
    this.mode = options && options.mode === "store" ? "store" : "tenant";
    if (options && options.theme) {
      this.pinnedTheme = utils_theme.resolveThemeKey(options.theme);
      this.themeKey = this.pinnedTheme;
    } else {
      this.themeKey = (utils_theme.getThemeSync() || {}).key || this.themeKey;
    }
    if (this.mode === "store") {
      this.loadStoreCandidates();
    } else {
      this.loadTenants();
    }
  },
  methods: {
    /**
     * ★T29.5 主题刷新：按「已选中 > 列表首项」的 tenantId 走匿名端点 currentByTenant
     * 拉取最新主题（loadTenantTheme 无 token 时回退旧本地缓存，PC 端新选主题不生效）；
     * 点选门店/店铺时实时预览对应租户主题。
     */
    refreshTheme() {
      if (this.pinnedTheme) return;
      const target = this.list.find((t) => t.tenantId === this.selectedId) || (this.mode === "store" ? this.list[0] : null);
      const p = target && target.tenantId ? utils_theme.loadThemeByTenantId(target.tenantId) : utils_theme.loadTenantTheme();
      p.then((t) => {
        if (t && !this.pinnedTheme) this.themeKey = t.key;
      });
    },
    /** T29.2 登录前门店点选：读取 input.vue 缓存的门店候选（PortalStoreVo），归一为卡片展示字段 */
    loadStoreCandidates() {
      this.loaded = false;
      const raw = utils_store.getTenantStoreCandidates() || [];
      this.list = raw.map((s) => ({ ...s, tenantName: s.companyName || s.tenantName }));
      if (this.list.length === 1) {
        this.selectedId = this.list[0].tenantId;
      }
      this.loaded = true;
      this.refreshTheme();
    },
    /** T29.2 门店点选确认：锁定选定门店（公开信息）后进入登录页（租户以登录后 JWT 为准） */
    confirmStore() {
      const target = this.list.find((t) => t.tenantId === this.selectedId);
      if (!target) {
        common_vendor.index.showToast({ title: "请选择门店", icon: "none" });
        return;
      }
      utils_store.setSelectedStore(target);
      utils_store.clearTenantStoreCandidates();
      console.log("[select] confirmStore - 加载主题 tenantId:", target.tenantId);
      utils_theme.loadThemeByTenantId(target.tenantId).then((theme) => {
        console.log("[select] confirmStore - 主题已缓存:", theme.key);
      });
      common_vendor.index.reLaunch({ url: "/pages/login/index" });
    },
    async loadTenants() {
      this.loaded = false;
      let list = utils_roles.getTenantList();
      if ((!list || !list.length) && utils_auth.getToken()) {
        try {
          const res = await utils_request.get(config_index.config.api.microMyTenants);
          list = res && res.data || [];
          utils_roles.setTenantList(list);
        } catch (e) {
          list = [];
        }
      }
      this.list = list || [];
      const sel = utils_roles.getSelectedTenant();
      if (sel && this.list.some((t) => t.tenantId === sel.tenantId)) {
        this.selectedId = sel.tenantId;
      } else if (this.list.length === 1) {
        this.selectedId = this.list[0].tenantId;
      }
      this.loaded = true;
      this.refreshTheme();
    },
    choose(item) {
      this.selectedId = item.tenantId;
      this.refreshTheme();
    },
    async confirm() {
      if (this.mode === "store") {
        this.confirmStore();
        return;
      }
      const target = this.list.find((t) => t.tenantId === this.selectedId);
      if (!target) {
        common_vendor.index.showToast({ title: "请选择店铺", icon: "none" });
        return;
      }
      utils_roles.selectTenant(this.remember ? target : null);
      const ctx = common_vendor.index.getStorageSync("micro_login_context");
      if (ctx && ctx.phonenumber) {
        this.loading = true;
        try {
          const res = await utils_request.post(config_index.config.api.microLogin, { ...ctx, tenantId: target.tenantId });
          const data = res && res.data || {};
          if (data.needSelectTenant) {
            utils_roles.setTenantList(data.tenants || []);
            common_vendor.index.showToast({ title: "请重新选择店铺", icon: "none" });
            return;
          }
          if (data.token) {
            utils_auth.setToken(data.token);
            utils_auth.setRoles(data.roles && data.roles.length ? data.roles : data.roleCode ? [data.roleCode] : []);
          }
          if (data.versionType) {
            utils_auth.setVersionType(data.versionType);
          }
          common_vendor.index.removeStorageSync("micro_login_context");
          await this.enterHome();
        } catch (e) {
        } finally {
          this.loading = false;
        }
      } else if (utils_auth.getToken()) {
        common_vendor.index.showModal({
          title: "切换店铺",
          content: "切换到「" + (target.tenantName || "该店铺") + "」需重新登录验证身份，是否继续？",
          success: (r) => {
            if (r.confirm) {
              utils_auth.clearAuth();
            }
          }
        });
      } else {
        await this.enterHome();
      }
    },
    /** 刷新用户信息 + 应用角色 Tab 分流（MICRO_TAB_CONFIG 依赖 role + version_type），进入对应角色首页 */
    async enterHome() {
      try {
        const infoRes = await utils_request.get(config_index.config.api.userInfo);
        const data = infoRes && infoRes.data ? infoRes.data : infoRes || {};
        if (data.user) utils_auth.setUser(data.user);
        if (data.roles) utils_auth.setRoles(data.roles);
      } catch (e) {
      }
      if (utils_roles.isMicroVersion()) {
        const conf = utils_roles.getMicroTabConfig();
        const firstPage = conf.tabs.find((t) => t.page) || {};
        common_vendor.index.reLaunch({ url: firstPage.page || "/pages/tab-home/index" });
      } else {
        common_vendor.index.reLaunch({ url: "/pages/tab-home/index" });
      }
    },
    roleText(code) {
      return { boss: "老板", salesman: "业务员", customer: "顾客" }[code] || "成员";
    },
    editionText(vt) {
      return vt === "micro_pro" ? "专业版" : vt === "micro_lite" ? "免费版" : "标准";
    },
    industryIcon(code) {
      return { boardgame: "🎲", fresh_food: "🥬", fund_control: "💰" }[code] || "🏪";
    },
    industryText(code) {
      return { boardgame: "桌游", fresh_food: "生鲜", fund_control: "资金" }[code] || code;
    },
    toHttps: utils_format.toHttps
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.t($data.mode === "store" ? "选择门店" : "我的店铺"),
    b: common_vendor.t($data.mode === "store" ? "请选择要进入的门店，随后使用该门店账号登录" : "同一手机号可归属多个店铺，请选择本次要进入的店铺"),
    c: !$data.loaded
  }, !$data.loaded ? {} : !$data.list.length ? {
    e: common_vendor.t($data.mode === "store" ? "该租户号下暂无门店" : "暂无可切换的店铺")
  } : {
    f: common_vendor.f($data.list, (item, k0, i0) => {
      return common_vendor.e({
        a: item.logoUrl
      }, item.logoUrl ? {
        b: $options.toHttps(item.logoUrl)
      } : {
        c: common_vendor.t((item.tenantName || "店").charAt(0))
      }, {
        d: common_vendor.t(item.tenantName || "未命名店铺"),
        e: common_vendor.t(item.roleName || $options.roleText(item.roleCode)),
        f: common_vendor.t($options.editionText(item.versionType)),
        g: common_vendor.n(item.versionType),
        h: item.industryCode
      }, item.industryCode ? {
        i: common_vendor.t($options.industryIcon(item.industryCode)),
        j: common_vendor.t($options.industryText(item.industryCode))
      } : {}, {
        k: item.city
      }, item.city ? {
        l: common_vendor.t(item.city)
      } : {}, {
        m: $data.selectedId === item.tenantId
      }, $data.selectedId === item.tenantId ? {} : {}, {
        n: item.tenantId,
        o: $data.selectedId === item.tenantId ? 1 : "",
        p: common_vendor.o(($event) => $options.choose(item), item.tenantId)
      });
    })
  }, {
    d: !$data.list.length,
    g: $data.list.length && $data.mode !== "store"
  }, $data.list.length && $data.mode !== "store" ? {
    h: common_vendor.t($data.remember ? "✓" : ""),
    i: $data.remember ? 1 : "",
    j: common_vendor.o(($event) => $data.remember = !$data.remember)
  } : {}, {
    k: common_vendor.t($data.mode === "store" ? "进入门店" : "进入店铺"),
    l: !$data.selectedId,
    m: $data.loading,
    n: common_vendor.o((...args) => $options.confirm && $options.confirm(...args)),
    o: common_vendor.n("theme-" + $data.themeKey),
    p: common_vendor.s($options.pageStyle)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-f75cdadd"]]);
wx.createPage(MiniProgramPage);
