"use strict";
const common_vendor = require("../common/vendor.js");
const TOKEN_KEY = "Authorization";
const ROLE_KEY = "app_roles";
const USER_KEY = "app_user";
const ORG_KEY = "app_org";
const VERSION_TYPE_KEY = "app_version_type";
function getToken() {
  return common_vendor.index.getStorageSync(TOKEN_KEY);
}
function setToken(token) {
  common_vendor.index.setStorageSync(TOKEN_KEY, token);
}
function removeToken() {
  common_vendor.index.removeStorageSync(TOKEN_KEY);
}
function setRoles(roles) {
  common_vendor.index.setStorageSync(ROLE_KEY, roles || []);
}
function getRoles() {
  return common_vendor.index.getStorageSync(ROLE_KEY) || [];
}
function setUser(user) {
  common_vendor.index.setStorageSync(USER_KEY, user || {});
}
function getUser() {
  return common_vendor.index.getStorageSync(USER_KEY) || {};
}
function setOrg(org) {
  common_vendor.index.setStorageSync(ORG_KEY, org || null);
}
function getOrg() {
  return common_vendor.index.getStorageSync(ORG_KEY) || null;
}
function clearAuth() {
  clearAuthTo("/pages/login/index");
}
let authNavigating = false;
function clearAuthTo(url) {
  common_vendor.index.removeStorageSync(TOKEN_KEY);
  common_vendor.index.removeStorageSync(ROLE_KEY);
  common_vendor.index.removeStorageSync(USER_KEY);
  common_vendor.index.removeStorageSync(ORG_KEY);
  common_vendor.index.removeStorageSync(VERSION_TYPE_KEY);
  const target = url || "/pages/login/index";
  const pages = getCurrentPages() || [];
  const current = pages.length ? "/" + (pages[pages.length - 1].route || "") : "";
  if (current === target) return;
  if (authNavigating) return;
  authNavigating = true;
  common_vendor.index.reLaunch({ url: target, complete: () => {
    authNavigating = false;
  } });
}
function setVersionType(versionType) {
  common_vendor.index.setStorageSync(VERSION_TYPE_KEY, versionType || "");
}
function getVersionType() {
  return common_vendor.index.getStorageSync(VERSION_TYPE_KEY) || "";
}
exports.clearAuth = clearAuth;
exports.clearAuthTo = clearAuthTo;
exports.getOrg = getOrg;
exports.getRoles = getRoles;
exports.getToken = getToken;
exports.getUser = getUser;
exports.getVersionType = getVersionType;
exports.removeToken = removeToken;
exports.setOrg = setOrg;
exports.setRoles = setRoles;
exports.setToken = setToken;
exports.setUser = setUser;
exports.setVersionType = setVersionType;
