"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_auth = require("../../../utils/auth.js");
const utils_request = require("../../../utils/request.js");
const config_index = require("../../../config/index.js");
const utils_roles = require("../../../utils/roles.js");
const utils_theme = require("../../../utils/theme.js");
const utils_format = require("../../../utils/format.js");
const MicroTabbar = () => "../../../components/micro-tabbar/micro-tabbar.js";
const ROLE_MAP = {
  boss: "老板",
  salesman: "业务员",
  staff: "员工",
  customer: "顾客"
};
const _sfc_main = {
  components: { MicroTabbar },
  data() {
    return {
      theme: utils_theme.getThemeSync(),
      loaded: false,
      qrcodeUrl: "",
      h5Url: "",
      scene: "",
      userName: "",
      roleKey: "",
      tenantName: "",
      // 推广数据（复用 microPromoMy 聚合）
      broughtCount: 0,
      consumedCount: 0,
      newCustomerCount: 0
    };
  },
  computed: {
    /** ★20261016 四主题重设计：根节点注入完整主题令牌（沿 DOM 继承到全部子元素/子组件） */
    rootStyle() {
      return utils_theme.themeCssVars(this.theme.key);
    },
    roleText() {
      return ROLE_MAP[this.roleKey] || this.roleKey || "-";
    }
  },
  onShow() {
    if (!utils_auth.getToken()) {
      common_vendor.index.reLaunch({ url: "/pages/login/index" });
      return;
    }
    utils_roles.applyNavBarTitle();
    this.theme = utils_theme.getThemeSync();
    utils_theme.loadTenantTheme().then((t) => {
      this.theme = t;
    });
    this.loadQrcode();
    this.loadPromotion();
  },
  methods: {
    toHttps: utils_format.toHttps,
    /** ★H5推广码：后端生成 H5 URL 的二维码（base64 图片），顾客扫码直接进入登录页 */
    async loadQrcode() {
      this.loaded = false;
      try {
        const res = await utils_request.get(config_index.config.api.microPromoMyQrcode);
        const data = res && res.data || {};
        this.qrcodeUrl = data.qrcodeUrl || "";
        this.h5Url = data.h5Url || "";
        this.scene = data.scene || "";
        this.userName = data.userName || "";
        this.roleKey = data.roleKey || "";
        this.tenantName = data.tenantName || utils_roles.getTenantName() || "";
      } catch (e) {
        this.qrcodeUrl = "";
      } finally {
        this.loaded = true;
      }
    },
    /** 推广数据汇总（带来人数 / 已消费 / 拉新成功） */
    async loadPromotion() {
      try {
        const res = await utils_request.get(config_index.config.api.microPromoMy);
        const data = res && res.data || {};
        this.broughtCount = data.broughtCount || 0;
        this.consumedCount = data.consumedCount || 0;
        this.newCustomerCount = data.newCustomerCount || 0;
      } catch (e) {
      }
    },
    /** 退出登录：清除 token 后返回登录页 */
    handleLogout() {
      common_vendor.index.showModal({
        title: "提示",
        content: "确认退出登录？",
        success: (r) => {
          if (r.confirm) {
            utils_auth.removeToken();
            common_vendor.index.reLaunch({ url: "/pages/login/index" });
          }
        }
      });
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
    a: !$data.loaded
  }, !$data.loaded ? {} : common_vendor.e({
    b: $data.qrcodeUrl
  }, $data.qrcodeUrl ? {
    c: $options.toHttps($data.qrcodeUrl)
  } : {
    d: common_vendor.t($data.h5Url || "暂无推广码"),
    e: common_vendor.o((...args) => $options.loadQrcode && $options.loadQrcode(...args))
  }, {
    f: common_vendor.t($data.tenantName || "-"),
    g: common_vendor.t($data.userName || "-"),
    h: common_vendor.t($options.roleText)
  }), {
    i: common_vendor.t($data.broughtCount),
    j: common_vendor.t($data.consumedCount),
    k: common_vendor.t($data.newCustomerCount),
    l: common_vendor.p({
      current: "/pages/micro/bossPromo/index"
    }),
    m: common_vendor.o((...args) => $options.handleLogout && $options.handleLogout(...args)),
    n: common_vendor.n("theme-" + $data.theme.key),
    o: common_vendor.s($options.rootStyle)
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-23060328"]]);
wx.createPage(MiniProgramPage);
