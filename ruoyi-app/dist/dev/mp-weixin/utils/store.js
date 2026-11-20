"use strict";
const common_vendor = require("../common/vendor.js");
const STORE_KEY = "app_selected_store";
function setSelectedStore(store) {
  if (!store) {
    common_vendor.index.removeStorageSync(STORE_KEY);
    return;
  }
  common_vendor.index.setStorageSync(STORE_KEY, {
    tenantId: store.tenantId,
    tenantCode: store.tenantCode,
    companyName: store.companyName,
    city: store.city,
    address: store.address,
    logoUrl: store.logoUrl,
    bizCode: store.bizCode,
    accessChannel: store.accessChannel
  });
}
function getSelectedStore() {
  return common_vendor.index.getStorageSync(STORE_KEY) || null;
}
function clearSelectedStore() {
  common_vendor.index.removeStorageSync(STORE_KEY);
}
const TENANT_NO_KEY = "tenantNo";
const TENANT_STORE_CANDIDATES_KEY = "tenant_store_candidates";
function setTenantNo(tenantNo) {
  if (!tenantNo) {
    common_vendor.index.removeStorageSync(TENANT_NO_KEY);
    return;
  }
  common_vendor.index.setStorageSync(TENANT_NO_KEY, tenantNo);
}
function getTenantNo() {
  return common_vendor.index.getStorageSync(TENANT_NO_KEY) || "";
}
function clearTenantNo() {
  common_vendor.index.removeStorageSync(TENANT_NO_KEY);
}
function setTenantStoreCandidates(list) {
  common_vendor.index.setStorageSync(TENANT_STORE_CANDIDATES_KEY, list || []);
}
function getTenantStoreCandidates() {
  return common_vendor.index.getStorageSync(TENANT_STORE_CANDIDATES_KEY) || [];
}
function clearTenantStoreCandidates() {
  common_vendor.index.removeStorageSync(TENANT_STORE_CANDIDATES_KEY);
}
exports.clearSelectedStore = clearSelectedStore;
exports.clearTenantNo = clearTenantNo;
exports.clearTenantStoreCandidates = clearTenantStoreCandidates;
exports.getSelectedStore = getSelectedStore;
exports.getTenantNo = getTenantNo;
exports.getTenantStoreCandidates = getTenantStoreCandidates;
exports.setSelectedStore = setSelectedStore;
exports.setTenantNo = setTenantNo;
exports.setTenantStoreCandidates = setTenantStoreCandidates;
