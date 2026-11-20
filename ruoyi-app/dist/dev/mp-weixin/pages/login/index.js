"use strict";
const common_vendor = require("../../common/vendor.js");
const config_index = require("../../config/index.js");
const utils_request = require("../../utils/request.js");
const utils_auth = require("../../utils/auth.js");
const utils_roles = require("../../utils/roles.js");
const utils_features = require("../../utils/features.js");
const utils_store = require("../../utils/store.js");
const utils_theme = require("../../utils/theme.js");
const utils_themeIcons = require("../../utils/themeIcons.js");
const _sfc_main = {
  data() {
    return {
      loading: false,
      store: null,
      // M27 T27.5：当前主题 key（?theme= 显式预览优先，否则租户主题）
      themeKey: utils_theme.resolveThemeKey(null),
      pinnedTheme: null,
      form: { username: "", password: "", code: "", uuid: "" },
      // ★H5推广码归因：顾客扫码进入登录页时携带 ref_t(租户ID) + ref_u(推广人用户ID)
      refTenantId: null,
      refUserId: null
    };
  },
  computed: {
    /** M27 T27.5：页面根节点注入主题 CSS 变量（4 风格变体共用） */
    pageStyle() {
      return utils_theme.themeCssVars(this.themeKey);
    }
  },
  /** ★H5推广码：从 URL 参数提取 ref_t / ref_u（顾客手机浏览器扫码进入）；★M27 T27.5：?theme= 预览 4 风格变体 */
  onLoad(options) {
    if (options) {
      if (options.ref_t) {
        this.refTenantId = options.ref_t;
        try {
          common_vendor.index.setStorageSync("promo_ref_t", options.ref_t);
        } catch (e) {
        }
      }
      if (options.ref_u) {
        this.refUserId = options.ref_u;
        try {
          common_vendor.index.setStorageSync("promo_ref_u", options.ref_u);
        } catch (e) {
        }
      }
      if (options.theme) {
        this.pinnedTheme = utils_theme.resolveThemeKey(options.theme);
        this.themeKey = this.pinnedTheme;
      }
    }
  },
  onShow() {
    this.store = utils_store.getSelectedStore();
    console.log("[login] onShow - store:", this.store ? this.store.companyName : "null", "| themeKey:", this.themeKey, "| pinnedTheme:", this.pinnedTheme);
    if (!this.store) {
      this.goStorePick();
      return;
    }
    if (!this.pinnedTheme) {
      const cachedTheme = utils_theme.getThemeSync();
      console.log("[login] onShow - getThemeSync() 缓存主题:", JSON.stringify(cachedTheme));
      this.themeKey = (cachedTheme || {}).key || this.themeKey;
      console.log("[login] onShow - 缓存赋值后 themeKey:", this.themeKey);
      if (this.store && this.store.tenantId && !utils_auth.getToken()) {
        console.log("[login] onShow - 按 tenantId 加载主题:", this.store.tenantId);
        utils_theme.loadThemeByTenantId(this.store.tenantId).then((t) => {
          console.log("[login] onShow - loadThemeByTenantId() 返回:", JSON.stringify(t));
          if (t && !this.pinnedTheme) {
            this.themeKey = t.key;
            console.log("[login] onShow - tenantId 赋值后 themeKey:", this.themeKey);
          }
        }).catch((e) => {
          console.error("[login] onShow - loadThemeByTenantId() 异常:", e);
        });
      } else {
        utils_theme.loadTenantTheme().then((t) => {
          console.log("[login] onShow - loadTenantTheme() 返回:", JSON.stringify(t));
          if (t && !this.pinnedTheme) {
            this.themeKey = t.key;
            console.log("[login] onShow - API 赋值后 themeKey:", this.themeKey);
          }
        }).catch((e) => {
          console.error("[login] onShow - loadTenantTheme() 异常:", e);
        });
      }
    } else {
      console.log("[login] onShow - pinnedTheme 存在，跳过主题加载，pinnedTheme:", this.pinnedTheme);
    }
    utils_roles.applyNavBarTitle();
  },
  methods: {
    iconUrl: utils_themeIcons.iconUrl,
    /**
     * ★门店点选回退（M29 文档流程：租户号输入 → 门店点选 → 凭证登录）：
     *   有门店候选 → 门店点选页；无候选 → 租户号输入页。
     *   绝不使用旧版 /pages/store/select（旧引导流程，与文档不一致）。
     */
    goStorePick() {
      if (utils_store.getTenantStoreCandidates().length) {
        common_vendor.index.reLaunch({ url: "/pages/micro/tenant/select?mode=store" });
      } else {
        common_vendor.index.reLaunch({ url: "/pages/micro/tenant/input" });
      }
    },
    /** 切换门店：清除已选门店与候选，避免 input.vue routeByLoginState 检测到旧门店弹回登录页 */
    switchStore() {
      utils_store.clearSelectedStore();
      utils_store.clearTenantStoreCandidates();
      this.store = null;
      common_vendor.index.reLaunch({ url: "/pages/micro/tenant/input" });
    },
    async handleLogin() {
      if (!this.form.username || !this.form.password) {
        common_vendor.index.showToast({ title: "请输入账号和密码", icon: "none" });
        return;
      }
      this.loading = true;
      try {
        const selTenant = utils_roles.getSelectedTenant();
        const loginTenantId = selTenant ? selTenant.tenantId : this.store ? this.store.tenantId : null;
        const loginBody = { ...this.form, tenantId: loginTenantId };
        const loginRes = await utils_request.post(config_index.config.api.login, loginBody);
        const token = loginRes.data && loginRes.data.token || loginRes.data;
        await this.afterLogin(token);
      } catch (e) {
      } finally {
        this.loading = false;
      }
    },
    /** 微信一键登录（T13.1 code2session）：wx.login 拿 code + 选定门店 tenantId → /xcxLogin。
     *  需后端配置 wx.miniapp.appid/secret，且该微信已绑定门店账号（首次请先用账号密码登录回填 userId） */
    async handleWxLogin() {
      if (!this.store) {
        this.goStorePick();
        return;
      }
      this.loading = true;
      try {
        const code = await this.wxLoginCode();
        if (!code) {
          common_vendor.index.showToast({ title: "微信登录失败，请重试", icon: "none" });
          return;
        }
        const res = await utils_request.post(config_index.config.api.xcxLogin, { xcxCode: code, tenantId: this.store.tenantId });
        const token = res.data && res.data.token || res.data;
        await this.afterLogin(token);
      } catch (e) {
      } finally {
        this.loading = false;
      }
    },
    /** 封装 uni.login 获取微信临时登录凭证 code */
    wxLoginCode() {
      return new Promise((resolve) => {
        common_vendor.index.login({
          provider: "weixin",
          success: (r) => resolve(r.code),
          fail: () => resolve(null)
        });
      });
    },
    /**
     * T15.8 手机号一键登录（小程序专属）：
     * 用户点击 → wx.getPhoneNumber 授权弹窗 → 后端 /micro/login 解密 phoneCode 直取手机号。
     * 多租户时返回 needSelectTenant + tenants 候选，前端弹「我的店铺」选择页。
     */
    async handlePhoneLogin(e) {
      const phoneCode = e.detail && e.detail.code || "";
      if (!phoneCode) {
        common_vendor.index.showToast({ title: "手机号授权失败，请重试", icon: "none" });
        return;
      }
      this.loading = true;
      try {
        const selTenant = utils_roles.getSelectedTenant();
        const loginTenantId = selTenant ? selTenant.tenantId : this.store ? this.store.tenantId : null;
        const loginRes = await utils_request.post(config_index.config.api.microLogin, {
          phoneCode,
          clientType: "mini",
          tenantId: loginTenantId
        });
        const data = loginRes.data || loginRes || {};
        if (data.needSelectTenant && data.tenants && data.tenants.length > 0) {
          const { setTenantList } = await "../../utils/roles.js";
          setTenantList(data.tenants);
          common_vendor.index.reLaunch({ url: "/pages/micro/tenant/select" });
          return;
        }
        if (!data.token) {
          common_vendor.index.showToast({ title: "登录失败，请重试", icon: "none" });
          return;
        }
        const loginRole = data.roleCode || "";
        if (loginRole) {
          utils_auth.setRoles([loginRole]);
        }
        if (data.versionType) {
          utils_auth.setVersionType(data.versionType);
        }
        await this.afterLogin(data.token, loginRole);
      } catch (e2) {
      } finally {
        this.loading = false;
      }
    },
    /** 登录成功后置流程（账号密码 / 微信一键登录 / 手机号一键登录共用）：用户信息→能力开关→多组织→角色分流 */
    async afterLogin(token, loginRole) {
      utils_auth.setToken(token);
      await this.doPromoAttribution();
      let roles = [];
      if (loginRole) {
        roles = [loginRole];
      } else {
        try {
          const infoRes = await utils_request.get(config_index.config.api.userInfo);
          const data = infoRes && infoRes.data ? infoRes.data : infoRes || {};
          utils_auth.setUser(data.user || {});
          if (data.roleCode) {
            roles = [data.roleCode];
          } else {
            roles = data.roles || [];
          }
          if (data.versionType) {
            utils_auth.setVersionType(data.versionType);
          }
        } catch (e) {
        }
      }
      utils_auth.setRoles(roles);
      if (roles.includes("platform_admin")) {
        common_vendor.index.showModal({ title: "提示", content: "平台管理员请使用 PC 端租户管理", showCancel: false });
        return;
      }
      if (loginRole) {
        await utils_features.loadEditionFeatures();
      } else {
        try {
          const infoRes = await utils_request.get(config_index.config.api.userInfo);
          const infoData = infoRes && infoRes.data ? infoRes.data : infoRes || {};
          if (Array.isArray(infoData.features) && infoData.edition) {
            utils_features.setEditionFeatures(infoData.features, infoData.edition);
          } else {
            await utils_features.loadEditionFeatures();
          }
        } catch (e) {
          await utils_features.loadEditionFeatures();
        }
      }
      if (utils_features.hasFeature("FEAT_MULTI_ORG")) {
        await this.chooseOrg();
      }
      if (utils_roles.isMicroVersion()) {
        const conf = utils_roles.getMicroTabConfig();
        const firstPage = conf.tabs.find((t) => t.page) || {};
        common_vendor.index.reLaunch({ url: firstPage.page || "/pages/tab-home/index" });
      } else {
        common_vendor.index.switchTab({ url: "/pages/tab-home/index" });
      }
    },
    /** ★H5推广码归因：登录成功后调用，记录顾客是扫描了谁的二维码登录的（用于后续奖励发放） */
    async doPromoAttribution() {
      let refT = this.refTenantId;
      let refU = this.refUserId;
      if (!refT) {
        try {
          refT = common_vendor.index.getStorageSync("promo_ref_t");
        } catch (e) {
        }
      }
      if (!refU) {
        try {
          refU = common_vendor.index.getStorageSync("promo_ref_u");
        } catch (e) {
        }
      }
      if (!refT || !refU) return;
      try {
        const profile = await utils_request.get(config_index.config.api.microProfile);
        const customerUserId = profile && profile.data && profile.data.userId || null;
        if (!customerUserId) return;
        await utils_request.post(config_index.config.api.microPromoScan, {
          sceneParam: "t=" + refT + "&u=" + refU,
          customerUserId,
          customerPhone: profile && profile.data && profile.data.phonenumber || ""
        });
        try {
          common_vendor.index.removeStorageSync("promo_ref_t");
          common_vendor.index.removeStorageSync("promo_ref_u");
        } catch (e) {
        }
      } catch (e) {
      }
    },
    /** 多组织选择：拉取本人可选组织，多个时弹窗选择（后端接口未就绪时优雅降级） */
    async chooseOrg() {
      try {
        const res = await utils_request.get(config_index.config.api.orgMyList);
        const orgs = res && res.data || [];
        if (orgs.length > 1) {
          const names = orgs.map((o) => o.orgName || o.name);
          common_vendor.index.showActionSheet({
            itemList: names,
            success: (r) => utils_auth.setOrg(orgs[r.tapIndex])
          });
        } else if (orgs.length === 1) {
          utils_auth.setOrg(orgs[0]);
        }
      } catch (e) {
      }
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: $data.themeKey === "theme_handdrawn"
  }, $data.themeKey === "theme_handdrawn" ? {
    b: $options.iconUrl("hand-cloud")
  } : {}, {
    c: $data.themeKey === "theme_handdrawn"
  }, $data.themeKey === "theme_handdrawn" ? {
    d: $options.iconUrl("hand-star")
  } : {}, {
    e: $data.themeKey === "theme_handdrawn"
  }, $data.themeKey === "theme_handdrawn" ? {
    f: $options.iconUrl("hand-sparkle")
  } : {}, {
    g: $data.themeKey === "theme_handdrawn"
  }, $data.themeKey === "theme_handdrawn" ? {
    h: $options.iconUrl("hand-heart")
  } : {}, {
    i: $data.themeKey === "theme_handdrawn"
  }, $data.themeKey === "theme_handdrawn" ? {
    j: $options.iconUrl("hand-star")
  } : {}, {
    k: $data.themeKey === "theme_handdrawn"
  }, $data.themeKey === "theme_handdrawn" ? {
    l: $options.iconUrl("hand-cloud")
  } : {}, {
    m: $data.themeKey === "theme_handdrawn"
  }, $data.themeKey === "theme_handdrawn" ? {
    n: $options.iconUrl("hand-sparkle")
  } : {}, {
    o: $data.themeKey === "theme_handdrawn"
  }, $data.themeKey === "theme_handdrawn" ? {
    p: $options.iconUrl("hand-bubble")
  } : {}, {
    q: $data.themeKey === "theme_handdrawn"
  }, $data.themeKey === "theme_handdrawn" ? {
    r: $options.iconUrl("hand-bubble")
  } : {}, {
    s: common_vendor.t($data.store ? $data.store.companyName : "欢迎登录"),
    t: $data.store
  }, $data.store ? common_vendor.e({
    v: common_vendor.t($data.store.companyName),
    w: $data.store.city
  }, $data.store.city ? {
    x: common_vendor.t($data.store.city)
  } : {}, {
    y: common_vendor.o((...args) => $options.switchStore && $options.switchStore(...args))
  }) : {}, {
    z: $data.form.username,
    A: common_vendor.o(($event) => $data.form.username = $event.detail.value),
    B: $data.form.password,
    C: common_vendor.o(($event) => $data.form.password = $event.detail.value),
    D: $data.loading,
    E: common_vendor.o((...args) => $options.handleLogin && $options.handleLogin(...args)),
    F: $data.loading,
    G: common_vendor.o((...args) => $options.handleWxLogin && $options.handleWxLogin(...args)),
    H: $data.loading,
    I: common_vendor.o((...args) => $options.handlePhoneLogin && $options.handlePhoneLogin(...args)),
    J: common_vendor.n("theme-" + $data.themeKey),
    K: common_vendor.s($options.pageStyle)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-45258083"]]);
wx.createPage(MiniProgramPage);
