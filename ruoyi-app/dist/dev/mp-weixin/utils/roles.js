"use strict";
Object.defineProperty(exports, Symbol.toStringTag, { value: "Module" });
const common_vendor = require("../common/vendor.js");
const utils_auth = require("./auth.js");
const utils_store = require("./store.js");
const ROLES = {
  BOSS: "boss",
  FINANCE: "finance",
  MANAGER: "manager",
  EMPLOYEE: "employee",
  PLATFORM_ADMIN: "platform_admin",
  // 小微版角色（T15.15 单包多角色）
  SALESMAN: "salesman",
  CUSTOMER: "customer"
};
const MICRO_ROLES = [ROLES.BOSS, ROLES.SALESMAN, ROLES.CUSTOMER];
const TAB_CONFIG = {
  [ROLES.BOSS]: {
    name: "老板",
    tabs: [
      { index: 0, text: "看板", view: "dashboard", page: "/pages/dashboard/category" },
      { index: 1, text: "项目", view: "projects", page: "/pages/project/list" },
      { index: 2, text: "审批", view: "approval", page: "/pages/approval/list" },
      { index: 3, text: "我的", view: "mine", page: "" }
    ]
  },
  [ROLES.FINANCE]: {
    name: "财务",
    tabs: [
      { index: 0, text: "通知", view: "notice", page: "" },
      { index: 1, text: "", view: "hidden", page: "" },
      { index: 2, text: "", view: "hidden", page: "" },
      { index: 3, text: "我的", view: "mine", page: "" }
    ]
  },
  [ROLES.MANAGER]: {
    name: "项目经理",
    tabs: [
      { index: 0, text: "我的项目", view: "projects", page: "/pages/project/list" },
      { index: 1, text: "收入确认", view: "income", page: "/pages/project/income" },
      { index: 2, text: "", view: "hidden", page: "" },
      { index: 3, text: "我的", view: "mine", page: "" }
    ]
  },
  [ROLES.EMPLOYEE]: {
    name: "报销人",
    tabs: [
      { index: 0, text: "首页", view: "todo", page: "" },
      { index: 1, text: "发起报销", view: "expense", page: "/pages/expense/create" },
      { index: 2, text: "我的报销", view: "myExpense", page: "/pages/expense/mine" },
      { index: 3, text: "我的", view: "mine", page: "" }
    ]
  }
};
function isMicroVersion() {
  const roles = utils_auth.getRoles();
  return MICRO_ROLES.some((r) => roles.includes(r));
}
function getPrimaryRole() {
  const roles = utils_auth.getRoles();
  if (isMicroVersion()) {
    const microPriority = [ROLES.BOSS, ROLES.SALESMAN, ROLES.CUSTOMER];
    for (const r of microPriority) {
      if (roles.includes(r)) return r;
    }
  }
  const priority = [ROLES.BOSS, ROLES.MANAGER, ROLES.EMPLOYEE, ROLES.FINANCE];
  for (const r of priority) {
    if (roles.includes(r)) return r;
  }
  return "";
}
function isPlatformAdmin() {
  return utils_auth.getRoles().includes(ROLES.PLATFORM_ADMIN);
}
function getTabConfig() {
  return TAB_CONFIG[getPrimaryRole()] || TAB_CONFIG[ROLES.EMPLOYEE];
}
function isMicroLite() {
  const vt = utils_auth.getVersionType() || getSelectedTenant() && getSelectedTenant().versionType || "";
  return isMicroVersion() && vt === "micro_lite";
}
const MICRO_TAB_CONFIG = {
  [ROLES.BOSS]: {
    name: "老板",
    tabs: [
      { index: 0, text: "首页", icon: "🏠", view: "bossHome", page: "/pages/micro/bossHome/index" },
      { index: 1, text: "我的活动", icon: "🎉", view: "myActivity", page: "/pages/micro/bossBook/list" },
      { index: 2, text: "我的待处理", icon: "📋", view: "todo", page: "/pages/micro/bossTodo/index" },
      { index: 3, text: "我的推广码", icon: "📢", view: "promo", page: "/pages/micro/bossPromo/index" }
    ]
  },
  [ROLES.SALESMAN]: {
    name: "业务员",
    tabs: [
      { index: 0, text: "工作台", icon: "🏠", view: "schedule", page: "/pages/micro/staffHome/index" },
      { index: 1, text: "我的推广码", icon: "📢", view: "promo", page: "/pages/micro/staff/promo" },
      { index: 2, text: "我的", icon: "👤", view: "mine", page: "/pages/micro/staffMine/index" }
    ]
  },
  [ROLES.CUSTOMER]: {
    name: "顾客",
    tabs: [
      { index: 0, text: "发现", icon: "🏠", view: "shop", page: "/pages/micro/customerHome/index" },
      { index: 1, text: "我的推广码", icon: "📢", view: "promo", page: "/pages/micro/customer/promo" },
      { index: 2, text: "我的活动", icon: "🎉", view: "myBook", page: "/pages/micro/customerBook/list" },
      { index: 3, text: "我的", icon: "👤", view: "mine", page: "/pages/micro/customerMine/index" }
    ]
  }
};
function getMicroTabConfig() {
  const role = getPrimaryRole();
  const conf = MICRO_TAB_CONFIG[role] || MICRO_TAB_CONFIG[ROLES.CUSTOMER];
  return { name: conf.name, tabs: conf.tabs };
}
function applyRoleTabs() {
  if (isMicroVersion()) {
    const conf2 = getMicroTabConfig();
    conf2.tabs.forEach((tab) => {
      if (tab.view === "hidden") {
        try {
          common_vendor.index.hideTabBarRedDot && common_vendor.index.hideTabBarRedDot({ index: tab.index });
        } catch (e) {
        }
      } else if (tab.text) {
        try {
          common_vendor.index.setTabBarItem({ index: tab.index, text: tab.text });
        } catch (e) {
        }
      }
    });
    return;
  }
  const conf = getTabConfig();
  conf.tabs.forEach((tab) => {
    if (tab.view === "hidden") {
      try {
        common_vendor.index.hideTabBarRedDot && common_vendor.index.hideTabBarRedDot({ index: tab.index });
      } catch (e) {
      }
    } else if (tab.text) {
      common_vendor.index.setTabBarItem({ index: tab.index, text: tab.text });
    }
  });
  if (getPrimaryRole() === ROLES.FINANCE) {
    common_vendor.index.setTabBarItem({ index: 1, text: "预警" });
    common_vendor.index.setTabBarItem({ index: 2, text: "通知" });
  }
}
const TENANT_LIST_KEY = "app_tenant_list";
const SELECTED_TENANT_KEY = "app_selected_tenant";
function setTenantList(list) {
  common_vendor.index.setStorageSync(TENANT_LIST_KEY, list || []);
}
function getTenantList() {
  return common_vendor.index.getStorageSync(TENANT_LIST_KEY) || [];
}
function selectTenant(tenant) {
  if (!tenant) {
    common_vendor.index.removeStorageSync(SELECTED_TENANT_KEY);
    return;
  }
  common_vendor.index.setStorageSync(SELECTED_TENANT_KEY, {
    tenantId: tenant.tenantId,
    tenantName: tenant.tenantName,
    roleCode: tenant.roleCode,
    roleName: tenant.roleName,
    versionType: tenant.versionType,
    industryCode: tenant.industryCode,
    logoUrl: tenant.logoUrl,
    city: tenant.city
  });
}
function getSelectedTenant() {
  return common_vendor.index.getStorageSync(SELECTED_TENANT_KEY) || null;
}
function clearTenantContext() {
  common_vendor.index.removeStorageSync(TENANT_LIST_KEY);
  common_vendor.index.removeStorageSync(SELECTED_TENANT_KEY);
}
function getTenantName() {
  const tenant = getSelectedTenant();
  if (tenant && tenant.tenantName) return tenant.tenantName;
  const store = utils_store.getSelectedStore();
  if (store && store.companyName) return store.companyName;
  return "";
}
function getTenantId() {
  const tenant = getSelectedTenant();
  if (tenant && tenant.tenantId) return tenant.tenantId;
  const store = utils_store.getSelectedStore();
  if (store && store.tenantId) return store.tenantId;
  return null;
}
const SYSTEM_NAME = "小账本";
function applyNavBarTitle(retried) {
  if (getCurrentPages().length === 0) {
    if (!retried) setTimeout(() => applyNavBarTitle(true), 50);
    return;
  }
  common_vendor.index.setNavigationBarTitle({
    title: SYSTEM_NAME,
    fail: () => {
      if (!retried) setTimeout(() => applyNavBarTitle(true), 50);
    }
  });
}
exports.MICRO_TAB_CONFIG = MICRO_TAB_CONFIG;
exports.ROLES = ROLES;
exports.SYSTEM_NAME = SYSTEM_NAME;
exports.TAB_CONFIG = TAB_CONFIG;
exports.applyNavBarTitle = applyNavBarTitle;
exports.applyRoleTabs = applyRoleTabs;
exports.clearTenantContext = clearTenantContext;
exports.getMicroTabConfig = getMicroTabConfig;
exports.getPrimaryRole = getPrimaryRole;
exports.getSelectedTenant = getSelectedTenant;
exports.getTabConfig = getTabConfig;
exports.getTenantId = getTenantId;
exports.getTenantList = getTenantList;
exports.getTenantName = getTenantName;
exports.isMicroLite = isMicroLite;
exports.isMicroVersion = isMicroVersion;
exports.isPlatformAdmin = isPlatformAdmin;
exports.selectTenant = selectTenant;
exports.setTenantList = setTenantList;
