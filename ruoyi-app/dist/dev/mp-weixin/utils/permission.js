"use strict";
const common_vendor = require("../common/vendor.js");
const utils_auth = require("./auth.js");
const utils_roles = require("./roles.js");
const PAGE_ROLES = {
  "/pages/dashboard/category": [utils_roles.ROLES.BOSS],
  "/pages/project/list": [utils_roles.ROLES.BOSS, utils_roles.ROLES.MANAGER],
  "/pages/project/detail": [utils_roles.ROLES.BOSS, utils_roles.ROLES.MANAGER],
  "/pages/project/income": [utils_roles.ROLES.MANAGER],
  "/pages/project/incomeCreate": [utils_roles.ROLES.MANAGER],
  "/pages/approval/list": [utils_roles.ROLES.BOSS],
  "/pages/approval/detail": [utils_roles.ROLES.BOSS],
  // 报销人（含项目经理兼任，权限文档 6.6 多角色并集）
  "/pages/expense/create": [utils_roles.ROLES.EMPLOYEE, utils_roles.ROLES.MANAGER],
  "/pages/expense/mine": [utils_roles.ROLES.EMPLOYEE, utils_roles.ROLES.MANAGER],
  "/pages/expense/supplement": [utils_roles.ROLES.EMPLOYEE, utils_roles.ROLES.MANAGER]
};
function canAccess(path) {
  const allow = PAGE_ROLES[path];
  if (!allow) return true;
  const roles = utils_auth.getRoles();
  return allow.some((r) => roles.includes(r));
}
function guardPage(pagePath) {
  if (canAccess(pagePath)) {
    return true;
  }
  common_vendor.index.showToast({ title: "无权限访问该页面", icon: "none" });
  setTimeout(() => {
    const pages = getCurrentPages();
    if (pages.length > 1) {
      common_vendor.index.navigateBack();
    } else {
      common_vendor.index.switchTab({ url: "/pages/tab-home/index" });
    }
  }, 800);
  return false;
}
exports.guardPage = guardPage;
