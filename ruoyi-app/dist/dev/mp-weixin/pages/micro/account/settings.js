"use strict";
const common_vendor = require("../../../common/vendor.js");
const utils_auth = require("../../../utils/auth.js");
const utils_request = require("../../../utils/request.js");
const utils_format = require("../../../utils/format.js");
const utils_roles = require("../../../utils/roles.js");
const utils_store = require("../../../utils/store.js");
const config_index = require("../../../config/index.js");
const _sfc_main = {
  data() {
    return {
      profile: {},
      form: { nickName: "", phonenumber: "", email: "" },
      pwdForm: { oldPassword: "", newPassword: "", confirmPassword: "" }
    };
  },
  computed: {
    roleLabel() {
      const roles = utils_auth.getRoles();
      if (!roles || !roles.length) return "用户";
      if (roles.includes("boss")) return "老板";
      if (roles.includes("employee") || roles.includes("salesman")) return "员工";
      if (roles.includes("customer")) return "顾客";
      return "用户";
    }
  },
  onShow() {
    if (!utils_auth.getToken()) {
      common_vendor.index.reLaunch({ url: "/pages/login/index" });
      return;
    }
    this.loadProfile();
  },
  methods: {
    toHttps: utils_format.toHttps,
    avatarSize: utils_format.avatarSize,
    async loadProfile() {
      try {
        const res = await utils_request.get(config_index.config.api.microProfile);
        const data = res && res.data || {};
        this.profile = data;
        this.form.nickName = data.nickName || "";
        this.form.phonenumber = data.phonenumber || "";
        this.form.email = data.email || "";
      } catch (e) {
      }
    },
    async saveProfile() {
      if (!this.form.nickName || !this.form.nickName.trim()) {
        common_vendor.index.showToast({ title: "昵称不能为空", icon: "none" });
        return;
      }
      if (this.form.nickName.length > 30) {
        common_vendor.index.showToast({ title: "昵称不能超过30个字符", icon: "none" });
        return;
      }
      if (this.form.phonenumber && !/^1[3-9]\d{9}$/.test(this.form.phonenumber)) {
        common_vendor.index.showToast({ title: "手机号格式不正确", icon: "none" });
        return;
      }
      if (this.form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(this.form.email)) {
        common_vendor.index.showToast({ title: "邮箱格式不正确", icon: "none" });
        return;
      }
      try {
        await utils_request.put(config_index.config.api.microUpdateProfile, {
          nickName: this.form.nickName.trim(),
          phonenumber: this.form.phonenumber || "",
          email: this.form.email || ""
        });
        common_vendor.index.showToast({ title: "保存成功", icon: "success" });
        const localUser = utils_auth.getUser();
        localUser.nickName = this.form.nickName.trim();
        localUser.phonenumber = this.form.phonenumber || "";
        localUser.email = this.form.email || "";
        common_vendor.index.setStorageSync("app_user", localUser);
      } catch (e) {
        common_vendor.index.showToast({ title: "保存失败", icon: "none" });
      }
    },
    async changePassword() {
      if (!this.pwdForm.oldPassword) {
        common_vendor.index.showToast({ title: "请输入旧密码", icon: "none" });
        return;
      }
      if (!this.pwdForm.newPassword) {
        common_vendor.index.showToast({ title: "请输入新密码", icon: "none" });
        return;
      }
      if (this.pwdForm.newPassword.length < 4 || this.pwdForm.newPassword.length > 20) {
        common_vendor.index.showToast({ title: "新密码长度需4~20位", icon: "none" });
        return;
      }
      if (this.pwdForm.newPassword !== this.pwdForm.confirmPassword) {
        common_vendor.index.showToast({ title: "两次密码输入不一致", icon: "none" });
        return;
      }
      try {
        await utils_request.put(config_index.config.api.microUpdatePwd, {
          oldPassword: this.pwdForm.oldPassword,
          newPassword: this.pwdForm.newPassword
        });
        common_vendor.index.showToast({ title: "密码修改成功", icon: "success" });
        this.pwdForm = { oldPassword: "", newPassword: "", confirmPassword: "" };
      } catch (e) {
        const msg = e && e.message || "修改密码失败";
        common_vendor.index.showToast({ title: msg, icon: "none" });
      }
    },
    logout() {
      common_vendor.index.showModal({
        title: "提示",
        content: "确认退出登录？",
        success: (r) => {
          if (r.confirm) utils_auth.clearAuth();
        }
      });
    },
    /** 切换租户（T29.4）：二次确认 → 清除租户号/门店/门店候选/多租户上下文/登录态 → 回租户号输入页 */
    switchTenantNo() {
      common_vendor.index.showModal({
        title: "切换租户",
        content: "切换租户需清除当前登录并重新输入租户号，是否继续？",
        success: (r) => {
          if (!r.confirm) return;
          utils_store.clearTenantNo();
          utils_store.clearSelectedStore();
          utils_store.clearTenantStoreCandidates();
          utils_roles.clearTenantContext();
          utils_auth.clearAuthTo("/pages/micro/tenant/input");
        }
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: $data.profile.avatar
  }, $data.profile.avatar ? {
    b: $options.avatarSize($data.profile.avatar, "sm"),
    c: common_vendor.o(($event) => $data.profile.avatar = "")
  } : {
    d: common_vendor.t(($data.profile.nickName || "用").charAt(0))
  }, {
    e: common_vendor.t($data.profile.nickName || "-"),
    f: common_vendor.t($options.roleLabel),
    g: $data.form.nickName,
    h: common_vendor.o(($event) => $data.form.nickName = $event.detail.value),
    i: $data.form.phonenumber,
    j: common_vendor.o(($event) => $data.form.phonenumber = $event.detail.value),
    k: $data.form.email,
    l: common_vendor.o(($event) => $data.form.email = $event.detail.value),
    m: common_vendor.o((...args) => $options.saveProfile && $options.saveProfile(...args)),
    n: $data.pwdForm.oldPassword,
    o: common_vendor.o(($event) => $data.pwdForm.oldPassword = $event.detail.value),
    p: $data.pwdForm.newPassword,
    q: common_vendor.o(($event) => $data.pwdForm.newPassword = $event.detail.value),
    r: $data.pwdForm.confirmPassword,
    s: common_vendor.o(($event) => $data.pwdForm.confirmPassword = $event.detail.value),
    t: common_vendor.o((...args) => $options.changePassword && $options.changePassword(...args)),
    v: common_vendor.o((...args) => $options.switchTenantNo && $options.switchTenantNo(...args)),
    w: common_vendor.o((...args) => $options.logout && $options.logout(...args))
  });
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-f3c86043"]]);
wx.createPage(MiniProgramPage);
