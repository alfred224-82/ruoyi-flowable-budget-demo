"use strict";
const common_vendor = require("../../../common/vendor.js");
const config_index = require("../../../config/index.js");
const _sfc_main = {
  data() {
    return {
      channel: "BOSS",
      category: "",
      categories: ["黄赌毒", "违法宣传", "违规活动", "其他"],
      activityList: [],
      activityNames: [],
      activityIndex: 0,
      selectedActivityId: null,
      staffList: [],
      staffNames: [],
      staffIndex: 0,
      selectedStaffId: null,
      selectedStaffName: "",
      complainantName: "",
      complainantPhone: "",
      contentText: "",
      photoList: [],
      evidenceList: [],
      submitting: false,
      myComplaints: []
    };
  },
  computed: {
    canSubmit() {
      if (!this.contentText.trim()) return false;
      if (this.channel === "PLATFORM") {
        if (this.contentText.trim().length < 20) return false;
        if (!this.category) return false;
        if (this.evidenceList.length === 0) return false;
      }
      return true;
    },
    contentPlaceholder() {
      return this.channel === "PLATFORM" ? "请详细描述投诉内容（不少于20字）..." : "请描述您的问题或反馈...";
    }
  },
  onShow() {
    this.loadMyComplaints();
    this.loadActivities();
    this.loadStaff();
  },
  methods: {
    loadActivities() {
      common_vendor.index.request({
        url: getApp().globalData.baseUrl + config_index.config.api.microCustomerMyActivities,
        header: { "Authorization": "Bearer " + common_vendor.index.getStorageSync("token") },
        success: (res) => {
          if (res.data && res.data.code === 200) {
            const list = res.data.data || [];
            this.activityList = list.filter((a) => a.sourceType === "ACTIVITY_SIGNUP");
            this.activityNames = this.activityList.map((a) => a.title || "活动#" + a.bizId);
          }
        },
        fail: () => {
          this.activityList = [];
        }
      });
    },
    loadStaff() {
      common_vendor.index.request({
        url: getApp().globalData.baseUrl + config_index.config.api.microComplaintStaffOptions,
        header: { "Authorization": "Bearer " + common_vendor.index.getStorageSync("token") },
        success: (res) => {
          if (res.data && res.data.code === 200) {
            this.staffList = res.data.data || [];
            this.staffNames = this.staffList.map((s) => s.name || "员工#" + s.id);
          }
        },
        fail: () => {
          this.staffList = [];
        }
      });
    },
    onCategoryChange(e) {
      this.category = this.categories[parseInt(e.detail.value)];
    },
    onActivityChange(e) {
      this.activityIndex = parseInt(e.detail.value);
      this.selectedActivityId = this.activityList[this.activityIndex].bizId;
    },
    onStaffChange(e) {
      this.staffIndex = parseInt(e.detail.value);
      this.selectedStaffId = this.staffList[this.staffIndex].id;
      this.selectedStaffName = this.staffList[this.staffIndex].name;
    },
    addPhoto() {
      common_vendor.index.chooseImage({
        count: 3 - this.photoList.length,
        sourceType: ["album", "camera"],
        success: (res) => {
          this.photoList = this.photoList.concat(res.tempFilePaths);
        }
      });
    },
    removePhoto(idx) {
      this.photoList.splice(idx, 1);
    },
    addEvidence() {
      common_vendor.index.chooseImage({
        count: 1,
        sourceType: ["camera"],
        success: (res) => {
          this.evidenceList = this.evidenceList.concat(res.tempFilePaths);
        }
      });
    },
    removeEvidence(idx) {
      this.evidenceList.splice(idx, 1);
    },
    handleSubmit() {
      if (!this.canSubmit) return;
      this.submitting = true;
      const data = {
        complaintChannel: this.channel,
        contentText: this.contentText,
        complainantName: this.complainantName,
        complainantPhone: this.complainantPhone,
        activityId: this.selectedActivityId,
        staffId: this.selectedStaffId,
        staffName: this.selectedStaffName
      };
      if (this.channel === "PLATFORM") {
        data.category = this.category;
        data.evidencePhotos = JSON.stringify(this.evidenceList);
      } else {
        if (this.photoList.length > 0) {
          data.photoUrls = JSON.stringify(this.photoList);
        }
      }
      common_vendor.index.request({
        url: getApp().globalData.baseUrl + config_index.config.api.microComplaintSubmit,
        method: "POST",
        header: {
          "Authorization": "Bearer " + common_vendor.index.getStorageSync("token"),
          "Content-Type": "application/json"
        },
        data,
        success: (res) => {
          if (res.data && res.data.code === 200) {
            common_vendor.index.showToast({ title: "投诉提交成功", icon: "success" });
            this.resetForm();
            this.loadMyComplaints();
          } else {
            common_vendor.index.showToast({ title: res.data.msg || "提交失败", icon: "none" });
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
    resetForm() {
      this.contentText = "";
      this.category = "";
      this.photoList = [];
      this.evidenceList = [];
      this.complainantName = "";
      this.complainantPhone = "";
      this.selectedActivityId = null;
      this.selectedStaffId = null;
      this.selectedStaffName = "";
    },
    loadMyComplaints() {
      common_vendor.index.request({
        url: getApp().globalData.baseUrl + config_index.config.api.microComplaintMy,
        header: { "Authorization": "Bearer " + common_vendor.index.getStorageSync("token") },
        success: (res) => {
          if (res.data && res.data.code === 200) {
            this.myComplaints = res.data.data || [];
          }
        }
      });
    },
    statusLabel(status) {
      const map = { PENDING: "待处理", PROCESSING: "处理中", RESOLVED: "已解决", CLOSED: "已关闭" };
      return map[status] || status;
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: $data.channel === "BOSS" ? 1 : "",
    b: common_vendor.o(($event) => $data.channel = "BOSS"),
    c: $data.channel === "PLATFORM" ? 1 : "",
    d: common_vendor.o(($event) => $data.channel = "PLATFORM"),
    e: $data.channel === "PLATFORM"
  }, $data.channel === "PLATFORM" ? {
    f: common_vendor.t($data.category || "请选择类别"),
    g: $data.categories,
    h: common_vendor.o((...args) => $options.onCategoryChange && $options.onCategoryChange(...args))
  } : {}, {
    i: $data.activityList.length > 0
  }, $data.activityList.length > 0 ? {
    j: common_vendor.t($data.selectedActivityId ? $data.activityNames[$data.activityIndex] : "不关联活动"),
    k: $data.activityNames,
    l: common_vendor.o((...args) => $options.onActivityChange && $options.onActivityChange(...args))
  } : {}, {
    m: $data.staffList.length > 0
  }, $data.staffList.length > 0 ? {
    n: common_vendor.t($data.selectedStaffId ? $data.staffNames[$data.staffIndex] : "不指定员工"),
    o: $data.staffNames,
    p: common_vendor.o((...args) => $options.onStaffChange && $options.onStaffChange(...args))
  } : {}, {
    q: $data.complainantName,
    r: common_vendor.o(($event) => $data.complainantName = $event.detail.value),
    s: $data.complainantPhone,
    t: common_vendor.o(($event) => $data.complainantPhone = $event.detail.value),
    v: $options.contentPlaceholder,
    w: $data.contentText,
    x: common_vendor.o(($event) => $data.contentText = $event.detail.value),
    y: $data.channel === "PLATFORM"
  }, $data.channel === "PLATFORM" ? {
    z: common_vendor.t($data.contentText.length)
  } : {}, {
    A: $data.channel === "BOSS"
  }, $data.channel === "BOSS" ? common_vendor.e({
    B: common_vendor.f($data.photoList, (photo, idx, i0) => {
      return {
        a: photo,
        b: common_vendor.o(($event) => $options.removePhoto(idx), idx),
        c: idx
      };
    }),
    C: $data.photoList.length < 3
  }, $data.photoList.length < 3 ? {
    D: common_vendor.o((...args) => $options.addPhoto && $options.addPhoto(...args))
  } : {}) : {}, {
    E: $data.channel === "PLATFORM"
  }, $data.channel === "PLATFORM" ? {
    F: common_vendor.f($data.evidenceList, (photo, idx, i0) => {
      return {
        a: photo,
        b: common_vendor.o(($event) => $options.removeEvidence(idx), idx),
        c: idx
      };
    }),
    G: common_vendor.o((...args) => $options.addEvidence && $options.addEvidence(...args))
  } : {}, {
    H: common_vendor.t($data.submitting ? "提交中..." : "提交投诉"),
    I: !$options.canSubmit,
    J: $data.submitting,
    K: common_vendor.o((...args) => $options.handleSubmit && $options.handleSubmit(...args)),
    L: $data.myComplaints.length > 0
  }, $data.myComplaints.length > 0 ? {
    M: common_vendor.f($data.myComplaints, (item, idx, i0) => {
      return common_vendor.e({
        a: common_vendor.t(item.complaintChannel === "BOSS" ? "Boss门店" : "平台Admin"),
        b: common_vendor.t($options.statusLabel(item.status)),
        c: common_vendor.n("status-" + item.status),
        d: common_vendor.t(item.contentText),
        e: common_vendor.t(item.submitTime),
        f: item.handlerReply
      }, item.handlerReply ? {
        g: common_vendor.t(item.handlerReply)
      } : {}, {
        h: idx
      });
    })
  } : {});
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-954f58c1"]]);
wx.createPage(MiniProgramPage);
