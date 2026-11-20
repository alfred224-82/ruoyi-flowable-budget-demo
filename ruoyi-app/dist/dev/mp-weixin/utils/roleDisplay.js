"use strict";
const utils_request = require("./request.js");
const config_index = require("../config/index.js");
let cachePromise = null;
function loadRoleDisplayNames() {
  if (!cachePromise) {
    cachePromise = utils_request.get(config_index.config.api.microRoleDisplayNames).then((res) => res && res.data || {}).catch(() => {
      cachePromise = null;
      return {};
    });
  }
  return cachePromise;
}
function getRoleDisplayName(roleCode, defaultName) {
  return loadRoleDisplayNames().then((map) => map[roleCode] || defaultName);
}
function getSpaceDisplayName(defaultName) {
  return loadRoleDisplayNames().then((map) => map._space_ || defaultName || "房间");
}
exports.getRoleDisplayName = getRoleDisplayName;
exports.getSpaceDisplayName = getSpaceDisplayName;
