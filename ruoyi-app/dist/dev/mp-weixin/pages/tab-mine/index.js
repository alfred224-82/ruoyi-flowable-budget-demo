"use strict";
const common_vendor = require("../../common/vendor.js");
const utils_auth = require("../../utils/auth.js");
const utils_roles = require("../../utils/roles.js");
const utils_features = require("../../utils/features.js");
const config_index = require("../../config/index.js");
const utils_request = require("../../utils/request.js");
const EDITION_TEXT = { BASIC: "基础版", ADVANCED: "高级版", PROFESSIONAL: "专业版" };
const CREDIT_LEVEL = { EXCELLENT: "优秀（免老板审批/后补14天）", STANDARD: "标准", RESTRICTED: "受限（禁用无发票报销）" };
const _sfc_main = {
  data() {
    return { user: {}, roleName: "", edition: "BASIC", org: null, credit: null, creditTip: "加载中…" };
  },
  computed: {
    editionText() {
      return EDITION_TEXT[this.edition] || this.edition;
    },
    creditLevelText() {
      return CREDIT_LEVEL[this.credit.creditLevel] || this.credit.creditLevel || "-";
    }
  },
  onShow() {
    this.user = utils_auth.getUser();
    const role = utils_roles.getPrimaryRole();
    this.roleName = (utils_roles.TAB_CONFIG[role] || {}).name || role;
    this.edition = utils_features.getEdition();
    this.org = utils_auth.getOrg();
    if (utils_features.hasFeature("FEAT_CREDIT_SCORE")) {
      this.loadCredit();
    }
  },
  methods: {
    hasFeature: utils_features.hasFeature,
    /** 本人信用分（后端接口就绪前优雅降级，不影响基础功能） */
    async loadCredit() {
      this.creditTip = "加载中…";
      try {
        const res = await utils_request.get(config_index.config.api.creditMine);
        this.credit = res.data || null;
        if (!this.credit) {
          this.creditTip = "暂无信用分数据";
        }
      } catch (e) {
        this.credit = null;
        this.creditTip = "信用分服务即将开通";
      }
    },
    /** 专业版组织切换 */
    async switchOrg() {
      try {
        const res = await utils_request.get(config_index.config.api.orgMyList);
        const orgs = res && res.data || [];
        if (!orgs.length) {
          common_vendor.index.showToast({ title: "暂无可切换的组织", icon: "none" });
          return;
        }
        common_vendor.index.showActionSheet({
          itemList: orgs.map((o) => o.orgName || o.name),
          success: (r) => {
            utils_auth.setOrg(orgs[r.tapIndex]);
            this.org = orgs[r.tapIndex];
          }
        });
      } catch (e) {
      }
    },
    async handleLogout() {
      try {
        await utils_request.post(config_index.config.api.logout);
      } catch (e) {
      }
      utils_auth.clearAuth();
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: common_vendor.t($data.user.nickName || "未登录"),
    b: common_vendor.t($data.roleName),
    c: common_vendor.t($options.editionText),
    d: $data.org
  }, $data.org ? {
    e: common_vendor.t($data.org.orgName || $data.org.name)
  } : {}, {
    f: $options.hasFeature("FEAT_CREDIT_SCORE")
  }, $options.hasFeature("FEAT_CREDIT_SCORE") ? common_vendor.e({
    g: $data.credit
  }, $data.credit ? {
    h: common_vendor.t($data.credit.creditScore),
    i: common_vendor.t($options.creditLevelText)
  } : {
    j: common_vendor.t($data.creditTip)
  }) : {}, {
    k: $options.hasFeature("FEAT_MULTI_ORG")
  }, $options.hasFeature("FEAT_MULTI_ORG") ? {
    l: common_vendor.t($data.org ? $data.org.orgName || $data.org.name : "点击选择组织"),
    m: common_vendor.o((...args) => $options.switchOrg && $options.switchOrg(...args))
  } : {}, {
    n: common_vendor.o((...args) => $options.handleLogout && $options.handleLogout(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-4077dd9b"]]);
wx.createPage(MiniProgramPage);
