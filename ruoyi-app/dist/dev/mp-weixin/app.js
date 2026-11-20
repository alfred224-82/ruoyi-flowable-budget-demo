"use strict";
Object.defineProperty(exports, Symbol.toStringTag, { value: "Module" });
const common_vendor = require("./common/vendor.js");
const utils_roles = require("./utils/roles.js");
if (!Math) {
  "./pages/micro/tenant/input.js";
  "./pages/store/select.js";
  "./pages/login/index.js";
  "./pages/tab-home/index.js";
  "./pages/tab-work/index.js";
  "./pages/tab-approve/index.js";
  "./pages/tab-mine/index.js";
  "./pages/dashboard/category.js";
  "./pages/project/list.js";
  "./pages/project/detail.js";
  "./pages/project/income.js";
  "./pages/project/incomeCreate.js";
  "./pages/approval/list.js";
  "./pages/approval/detail.js";
  "./pages/expense/create.js";
  "./pages/expense/mine.js";
  "./pages/expense/supplement.js";
  "./pages/micro/bossHome/index.js";
  "./pages/micro/bossTodo/index.js";
  "./pages/micro/bossPromo/index.js";
  "./pages/micro/bossBook/list.js";
  "./pages/micro/booking/list.js";
  "./pages/micro/booking/detail.js";
  "./pages/micro/booking/add.js";
  "./pages/micro/report/index.js";
  "./pages/micro/report/trend.js";
  "./pages/micro/shop/config.js";
  "./pages/micro/mine/index.js";
  "./pages/micro/staffHome/index.js";
  "./pages/micro/staffIncome/index.js";
  "./pages/micro/staffIncome/detail.js";
  "./pages/micro/staffMine/index.js";
  "./pages/micro/staffBook/list.js";
  "./pages/micro/customerHome/index.js";
  "./pages/micro/customerBook/submit.js";
  "./pages/micro/customerBook/list.js";
  "./pages/micro/customerBook/detail.js";
  "./pages/micro/customerBook/seat.js";
  "./pages/micro/customerMine/index.js";
  "./pages/micro/activity/browse.js";
  "./pages/micro/activity/detail.js";
  "./pages/micro/activity/history.js";
  "./pages/micro/cost/list.js";
  "./pages/micro/cost/edit.js";
  "./pages/micro/staff/promo.js";
  "./pages/micro/customer/promo.js";
  "./pages/micro/customer/coupon.js";
  "./pages/micro/customer/records.js";
  "./pages/micro/customer/consent.js";
  "./pages/micro/activity/stats.js";
  "./pages/micro/staff/customers.js";
  "./pages/micro/staff/mine.js";
  "./pages/micro/rating/submit.js";
  "./pages/micro/tenant/select.js";
  "./pages/micro/account/settings.js";
  "./pages/micro/selfBooking/index.js";
  "./pages/micro/selfBooking/confirm.js";
  "./pages/micro/checkIn/index.js";
  "./pages/micro/complaint/index.js";
  "./pages/micro/agreement/sign.js";
}
const _sfc_main = {
  onLaunch() {
    ["navigateTo", "redirectTo", "reLaunch", "switchTab"].forEach((api) => {
      common_vendor.index.addInterceptor(api, { complete: () => utils_roles.applyNavBarTitle() });
    });
  },
  onShow() {
    utils_roles.applyNavBarTitle();
  }
};
function createApp() {
  const app = common_vendor.createSSRApp(_sfc_main);
  return { app };
}
createApp().app.mount("#app");
exports.createApp = createApp;
