"use strict";
const common_vendor = require("../common/vendor.js");
const utils_request = require("./request.js");
const config_index = require("../config/index.js");
const FEATURES_KEY = "app_edition_features";
const EDITION_KEY = "app_edition";
function setEditionFeatures(features, edition) {
  common_vendor.index.setStorageSync(FEATURES_KEY, features || []);
  common_vendor.index.setStorageSync(EDITION_KEY, edition || "BASIC");
}
function loadEditionFeatures() {
  return utils_request.get(config_index.config.api.editionFeatures).then((res) => {
    const data = res.data || {};
    const features = data.features || [];
    common_vendor.index.setStorageSync(FEATURES_KEY, features);
    common_vendor.index.setStorageSync(EDITION_KEY, data.edition || "BASIC");
    return features;
  }).catch(() => {
    common_vendor.index.setStorageSync(FEATURES_KEY, []);
    return [];
  });
}
function hasFeature(code) {
  const features = common_vendor.index.getStorageSync(FEATURES_KEY) || [];
  return features.includes(code);
}
function getEdition() {
  return common_vendor.index.getStorageSync(EDITION_KEY) || "BASIC";
}
exports.getEdition = getEdition;
exports.hasFeature = hasFeature;
exports.loadEditionFeatures = loadEditionFeatures;
exports.setEditionFeatures = setEditionFeatures;
