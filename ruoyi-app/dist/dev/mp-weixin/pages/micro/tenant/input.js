"use strict";
const common_vendor = require("../../../common/vendor.js");
const config_index = require("../../../config/index.js");
const utils_request = require("../../../utils/request.js");
const utils_store = require("../../../utils/store.js");
const utils_auth = require("../../../utils/auth.js");
const utils_roles = require("../../../utils/roles.js");
const utils_theme = require("../../../utils/theme.js");
const _sfc_main = {
  data() {
    return { tenantNo: "", loading: false, scenePending: false, themeKey: utils_theme.resolveThemeKey(null), pinnedTheme: null };
  },
  computed: {
    /** M27 T27.5：页面根节点注入主题 CSS 变量 */
    pageStyle() {
      return utils_theme.themeCssVars(this.themeKey);
    }
  },
  onShow() {
    utils_roles.applyNavBarTitle();
    if (this.scenePending) return;
    this.routeByLoginState();
  },
  onLoad(options) {
    console.log("[tenant-input] onLoad page=", typeof window !== "undefined" ? window.location.href : "mp", "| baseUrl=", config_index.config.baseUrl, "| api=", config_index.config.api.microTenantStores);
    const scene = this.extractScene(options);
    if (scene) {
      this.scenePending = true;
      this.resolveSceneLogin(scene);
      return;
    }
    if (options && options.theme) {
      this.pinnedTheme = utils_theme.resolveThemeKey(options.theme);
      this.themeKey = this.pinnedTheme;
    } else {
      this.themeKey = (utils_theme.getThemeSync() || {}).key || this.themeKey;
      this.refreshTheme();
    }
    if (!this.tenantNo) this.tenantNo = utils_store.getTenantNo() || "";
  },
  methods: {
    /**
     * ★T29.5 主题刷新：登录前 tenantId 已知时（门店候选/门店列表响应）走匿名端点
     * currentByTenant 拉取最新主题（loadTenantTheme 无 token 时回退旧本地缓存，
     * 会导致 PC 端新选主题不生效）；拉到后同步写缓存并刷新本页 themeKey。
     */
    refreshTheme(tenantId) {
      if (this.pinnedTheme) return;
      const tid = tenantId || (utils_store.getTenantStoreCandidates()[0] || {}).tenantId;
      const p = tid ? utils_theme.loadThemeByTenantId(tid) : utils_theme.loadTenantTheme();
      p.then((t) => {
        if (t && !this.pinnedTheme) this.themeKey = t.key;
      });
    },
    /**
     * ★入口分流（替代原 App.vue onLaunch 跳转，在路由就绪后单点执行，避免导航竞争）
     * 文档 M29 流程：输入租户号（本页）→ 门店点选 → 凭证登录。
     * ★不按「门店候选」自动跳转：本页 → navigateTo 门店点选页后，用户返回本页属正常操作，
     *   若此处再弹回门店页会形成回退死循环；改为回填租户号（T29.4 免输）让用户自行点「下一步」。
     */
    routeByLoginState() {
      if (utils_auth.getToken()) {
        if (utils_roles.isMicroVersion()) {
          const conf = utils_roles.getMicroTabConfig();
          const firstPage = conf.tabs.find((t) => t.page) || {};
          common_vendor.index.reLaunch({ url: firstPage.page || "/pages/tab-home/index" });
        } else if (!utils_roles.getSelectedTenant() && utils_roles.getTenantList().length > 1) {
          common_vendor.index.reLaunch({ url: "/pages/micro/tenant/select" });
        } else {
          common_vendor.index.switchTab({ url: "/pages/tab-home/index" });
        }
        return;
      }
      if (utils_store.getSelectedStore()) {
        common_vendor.index.reLaunch({ url: "/pages/login/index" });
        return;
      }
    },
    /**
     * T29.3 提取扫码 scene：小程序端启动参数在 options.query.scene，H5/首页 onLoad 直接在 options.scene（需 decodeURIComponent）；
     * 微信启动场景值 scene 为纯数字，非本业务的租户 scene，需排除。
     */
    extractScene(options) {
      const query = options && (options.query || options) || {};
      let raw = query.scene || "";
      if (!raw) return "";
      if (/^\d+$/.test(String(raw))) return "";
      try {
        raw = decodeURIComponent(raw);
      } catch (e) {
      }
      return /(^|&)t=/.test(raw) ? raw : "";
    },
    /**
     * T29.3 扫码登录：调 /micro/login/resolveScene 校验 scene 合法性（租户存在/未停用/未过期、门店归属），
     * 通过则锁定门店上下文 + 记住租户号 → 直接进登录页；非法则提示「二维码无效」并停留本页。
     */
    async resolveSceneLogin(scene) {
      try {
        const res = await utils_request.post(config_index.config.api.microLoginResolveScene, { scene });
        const data = res && res.data || {};
        if (!data.tenantId) {
          throw new Error("invalid scene");
        }
        utils_store.setSelectedStore({
          tenantId: data.tenantId,
          tenantCode: data.tenantCode,
          companyName: data.tenantName,
          city: data.city,
          logoUrl: data.logoUrl
        });
        if (data.tenantCode) utils_store.setTenantNo(data.tenantCode);
        common_vendor.index.reLaunch({ url: "/pages/login/index" });
      } catch (e) {
        common_vendor.index.showToast({ title: "二维码无效", icon: "none" });
        this.scenePending = false;
      }
    },
    async confirm() {
      const no = (this.tenantNo || "").trim();
      if (!no) {
        common_vendor.index.showToast({ title: "请输入租户号", icon: "none" });
        return;
      }
      this.loading = true;
      try {
        const res = await utils_request.get(config_index.config.api.microTenantStores, { tenantNo: no });
        const list = res && res.data || [];
        console.log("[tenant-input] stores 响应 code=", res && res.code, "| listLen=", list.length, "| raw=", JSON.stringify(res).slice(0, 300));
        if (!list.length) {
          common_vendor.index.showToast({ title: "该租户号下暂无门店", icon: "none" });
          return;
        }
        utils_store.setTenantNo(list[0].tenantCode || no);
        utils_store.setTenantStoreCandidates(list);
        this.refreshTheme(list[0].tenantId);
        common_vendor.index.navigateTo({ url: "/pages/micro/tenant/select?mode=store" });
      } catch (e) {
      } finally {
        this.loading = false;
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return {
    a: common_vendor.o((...args) => $options.confirm && $options.confirm(...args)),
    b: $data.tenantNo,
    c: common_vendor.o(($event) => $data.tenantNo = $event.detail.value),
    d: $data.loading,
    e: common_vendor.o((...args) => $options.confirm && $options.confirm(...args)),
    f: common_vendor.n("theme-" + $data.themeKey),
    g: common_vendor.s($options.pageStyle)
  };
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-0c09f162"]]);
wx.createPage(MiniProgramPage);
