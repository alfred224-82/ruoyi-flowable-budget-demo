"use strict";
const common_vendor = require("../../../common/vendor.js");
const config_index = require("../../../config/index.js");
const utils_format = require("../../../utils/format.js");
const _sfc_main = {
  data() {
    return {
      checkInEnabled: null,
      activityList: [],
      activityNames: [],
      activityIndex: 0,
      photoUrl: "",
      locationName: "",
      locationLat: null,
      locationLng: null,
      submitting: false,
      myRecords: [],
      showSuccess: false,
      lastResult: {}
    };
  },
  computed: {
    canSubmit() {
      return this.activityList.length > 0 && this.photoUrl && this.locationName;
    }
  },
  onShow() {
    this.checkEnabled();
    this.loadMyRecords();
  },
  methods: {
    checkEnabled() {
      common_vendor.index.request({
        url: getApp().globalData.baseUrl + config_index.config.api.microCheckInEnabled,
        header: { "Authorization": "Bearer " + common_vendor.index.getStorageSync("token") },
        success: (res) => {
          if (res.data && res.data.code === 200) {
            this.checkInEnabled = res.data.data;
            if (this.checkInEnabled) {
              this.loadActivities();
            }
          }
        },
        fail: () => {
          this.checkInEnabled = false;
        }
      });
    },
    loadActivities() {
      const store = common_vendor.index.getStorageSync("microActivities");
      if (store) {
        try {
          const list = JSON.parse(store);
          this.activityList = list.filter((a) => a.status !== "stopped");
          this.activityNames = this.activityList.map((a) => a.title || "活动#" + a.id);
        } catch (e) {
          this.activityList = [];
          this.activityNames = [];
        }
      }
    },
    onActivityChange(e) {
      this.activityIndex = parseInt(e.detail.value);
    },
    choosePhoto() {
      common_vendor.index.chooseImage({
        count: 1,
        sourceType: ["camera"],
        success: (res) => {
          const tempPath = res.tempFilePaths[0];
          common_vendor.index.uploadFile({
            url: getApp().globalData.baseUrl + "/resource/upload",
            filePath: tempPath,
            name: "file",
            header: { "Authorization": "Bearer " + common_vendor.index.getStorageSync("token") },
            success: (uploadRes) => {
              const data = JSON.parse(uploadRes.data);
              if (data.code === 200) {
                this.photoUrl = data.data.url;
              } else {
                common_vendor.index.showToast({ title: "照片上传失败", icon: "none" });
              }
            },
            fail: () => {
              common_vendor.index.showToast({ title: "照片上传失败", icon: "none" });
            }
          });
        }
      });
    },
    getLocation() {
      common_vendor.index.getLocation({
        type: "gcj02",
        success: (res) => {
          this.locationLat = res.latitude;
          this.locationLng = res.longitude;
          this.locationName = `纬度${res.latitude.toFixed(6)}, 经度${res.longitude.toFixed(6)}`;
        },
        fail: () => {
          common_vendor.index.showToast({ title: "获取位置失败，请授权位置权限", icon: "none" });
        }
      });
    },
    handleSubmit() {
      if (!this.canSubmit) return;
      this.submitting = true;
      const activity = this.activityList[this.activityIndex];
      const now = /* @__PURE__ */ new Date();
      const watermarkText = utils_format.formatDateTime(now) + " | " + this.locationName;
      common_vendor.index.request({
        url: getApp().globalData.baseUrl + config_index.config.api.microCheckIn,
        method: "POST",
        header: {
          "Authorization": "Bearer " + common_vendor.index.getStorageSync("token"),
          "Content-Type": "application/json"
        },
        data: {
          activityId: activity.id,
          photoUrl: this.photoUrl,
          locationName: this.locationName,
          locationLat: this.locationLat,
          locationLng: this.locationLng,
          watermarkText
        },
        success: (res) => {
          if (res.data && res.data.code === 200) {
            this.lastResult = {
              checkInTime: utils_format.formatDateTime(now),
              locationName: this.locationName,
              isLate: 0
            };
            this.showSuccess = true;
            this.photoUrl = "";
            this.locationName = "";
            this.loadMyRecords();
          } else {
            common_vendor.index.showToast({ title: res.data.msg || "打卡失败", icon: "none" });
          }
        },
        fail: () => {
          common_vendor.index.showToast({ title: "网络错误", icon: "none" });
        },
        complete: () => {
          this.submitting = false;
        }
      });
    },
    loadMyRecords() {
      common_vendor.index.request({
        url: getApp().globalData.baseUrl + config_index.config.api.microCheckInMy,
        header: { "Authorization": "Bearer " + common_vendor.index.getStorageSync("token") },
        success: (res) => {
          if (res.data && res.data.code === 200) {
            this.myRecords = (res.data.data || []).slice(0, 10);
          }
        }
      });
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: $data.checkInEnabled !== null
  }, $data.checkInEnabled !== null ? common_vendor.e({
    b: !$data.checkInEnabled
  }, !$data.checkInEnabled ? {} : {}) : {}, {
    c: $data.checkInEnabled
  }, $data.checkInEnabled ? common_vendor.e({
    d: $data.activityList.length > 0
  }, $data.activityList.length > 0 ? {
    e: common_vendor.t($data.activityList.length > 0 ? $data.activityNames[$data.activityIndex] : "暂无活动"),
    f: $data.activityNames,
    g: common_vendor.o((...args) => $options.onActivityChange && $options.onActivityChange(...args))
  } : {}) : {}, {
    h: $data.checkInEnabled && $data.activityList.length > 0
  }, $data.checkInEnabled && $data.activityList.length > 0 ? common_vendor.e({
    i: $data.photoUrl
  }, $data.photoUrl ? {
    j: $data.photoUrl,
    k: common_vendor.o((...args) => $options.choosePhoto && $options.choosePhoto(...args))
  } : {
    l: common_vendor.o((...args) => $options.choosePhoto && $options.choosePhoto(...args))
  }) : {}, {
    m: $data.checkInEnabled && $data.activityList.length > 0
  }, $data.checkInEnabled && $data.activityList.length > 0 ? common_vendor.e({
    n: $data.locationName
  }, $data.locationName ? {
    o: common_vendor.t($data.locationName)
  } : {
    p: common_vendor.o((...args) => $options.getLocation && $options.getLocation(...args))
  }) : {}, {
    q: $data.checkInEnabled && $data.activityList.length > 0
  }, $data.checkInEnabled && $data.activityList.length > 0 ? {
    r: common_vendor.t($data.submitting ? "提交中..." : "打卡"),
    s: !$options.canSubmit,
    t: $data.submitting,
    v: common_vendor.o((...args) => $options.handleSubmit && $options.handleSubmit(...args))
  } : {}, {
    w: $data.checkInEnabled
  }, $data.checkInEnabled ? common_vendor.e({
    x: $data.myRecords.length > 0
  }, $data.myRecords.length > 0 ? {
    y: common_vendor.f($data.myRecords, (record, idx, i0) => {
      return common_vendor.e({
        a: common_vendor.t(record.checkInTime),
        b: common_vendor.t(record.isLate === 1 ? "迟到" : "准时"),
        c: record.isLate === 1 ? 1 : "",
        d: record.source === "SUPPLEMENT"
      }, record.source === "SUPPLEMENT" ? {} : {}, {
        e: common_vendor.t(record.locationName || "未知位置"),
        f: idx
      });
    })
  } : {}) : {}, {
    z: $data.showSuccess
  }, $data.showSuccess ? {
    A: common_vendor.t($data.lastResult.checkInTime),
    B: common_vendor.t($data.lastResult.locationName),
    C: common_vendor.t($data.lastResult.isLate ? "迟到" : "准时到岗"),
    D: $data.lastResult.isLate ? 1 : "",
    E: common_vendor.o(($event) => $data.showSuccess = false),
    F: common_vendor.o(() => {
    }),
    G: common_vendor.o(($event) => $data.showSuccess = false)
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-8967f9b1"]]);
wx.createPage(MiniProgramPage);
