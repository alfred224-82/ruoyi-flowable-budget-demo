"use strict";
const utils_request = require("./request.js");
const config_index = require("../config/index.js");
function getBookingResourcePolicy() {
  return utils_request.get(config_index.config.api.microBookingPolicyCustomerView).then((res) => {
    const d = res && res.data || {};
    return {
      customerSelfBooking: d.customerSelfBooking === 1,
      projectBookingEnabled: d.projectBookingEnabled === void 0 ? 1 : d.projectBookingEnabled,
      projectBookingRequired: d.projectBookingRequired === void 0 ? 0 : d.projectBookingRequired,
      roomBookingEnabled: d.roomBookingEnabled === void 0 ? 1 : d.roomBookingEnabled,
      roomBookingRequired: d.roomBookingRequired === void 0 ? 0 : d.roomBookingRequired,
      staffBookingEnabled: d.staffBookingEnabled === void 0 ? 1 : d.staffBookingEnabled,
      staffBookingRequired: d.staffBookingRequired === void 0 ? 0 : d.staffBookingRequired,
      industryCode: d.industryCode || null
    };
  }).catch(() => ({
    customerSelfBooking: false,
    projectBookingEnabled: 1,
    projectBookingRequired: 0,
    roomBookingEnabled: 1,
    roomBookingRequired: 0,
    staffBookingEnabled: 1,
    staffBookingRequired: 0,
    industryCode: null
  }));
}
exports.getBookingResourcePolicy = getBookingResourcePolicy;
