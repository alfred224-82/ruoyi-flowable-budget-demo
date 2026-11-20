"use strict";
const common_vendor = require("../../common/vendor.js");
const config_index = require("../../config/index.js");
const utils_request = require("../../utils/request.js");
const utils_store = require("../../utils/store.js");
const utils_format = require("../../utils/format.js");
const _sfc_main = {
  data() {
    return {
      keyword: "",
      city: "",
      stores: [],
      loading: false,
      locating: false,
      located: false,
      locatedCity: "",
      userLng: null,
      userLat: null,
      boundTenants: [],
      pinnedStore: null
    };
  },
  onLoad(options) {
    const opt = options || {};
    if (opt.mpAppId) {
      this.resolveByMpAppId(opt.mpAppId);
    }
    if (opt.openid) {
      this.loadBoundTenants(opt.openid);
    }
    this.loadStores();
  },
  methods: {
    /** 尝试获取定位（gcj02），成功则按距离就近排序，失败降级 */
    tryLocation() {
      this.locating = true;
      common_vendor.index.getLocation({
        type: "gcj02",
        success: (res) => {
          this.located = true;
          this.userLng = res.longitude;
          this.userLat = res.latitude;
          this.locating = false;
          this.loadStores();
        },
        fail: () => {
          this.located = false;
          this.userLng = null;
          this.userLat = null;
          this.locating = false;
          this.loadStores();
        }
      });
    },
    reLocate() {
      this.tryLocation();
    },
    /** 加载门店列表（/portal/stores，仅返回公开展示字段） */
    loadStores() {
      this.loading = true;
      const params = { limit: 50 };
      if (this.keyword && this.keyword.trim()) params.keyword = this.keyword.trim();
      if (this.city && this.city.trim()) params.city = this.city.trim();
      if (this.located && this.userLng != null && this.userLat != null) {
        params.longitude = this.userLng;
        params.latitude = this.userLat;
      }
      utils_request.get(config_index.config.api.portalStores, params).then((res) => {
        this.stores = res && res.data || [];
        this.loading = false;
      }).catch(() => {
        this.stores = [];
        this.loading = false;
      });
    },
    /** 按 openid 反查已绑定门店（引导态只读，不写会话） */
    loadBoundTenants(openid) {
      utils_request.get(config_index.config.api.portalTenants, { openid }).then((res) => {
        this.boundTenants = res && res.data || [];
      }).catch(() => {
        this.boundTenants = [];
      });
    },
    /** 公众号 AppID 解析归属主体 */
    resolveByMpAppId(mpAppId) {
      utils_request.get(config_index.config.api.portalResolve, { mpAppId }).then((res) => {
        this.pinnedStore = res && res.data || null;
      }).catch(() => {
        this.pinnedStore = null;
      });
    },
    onSearch() {
      this.loadStores();
    },
    /** 用户显式选定门店：缓存公开信息后进入登录页（鉴权态由登录签发 JWT 决定租户） */
    selectStore(store) {
      if (!store || store.tenantId == null) {
        common_vendor.index.showToast({ title: "门店信息不完整", icon: "none" });
        return;
      }
      utils_store.setSelectedStore(store);
      common_vendor.index.reLaunch({ url: "/pages/login/index" });
    },
    channelText(channel) {
      const map = { wechat: "微信", wecom: "企业微信" };
      return map[channel] || "";
    },
    toHttps: utils_format.toHttps,
    initial(name) {
      return name ? String(name).charAt(0) : "店";
    },
    formatDistance(km) {
      const d = Number(km);
      if (isNaN(d)) return "";
      if (d < 1) return Math.round(d * 1e3) + " m";
      return d.toFixed(1) + " km";
    }
  }
};
function _sfc_render(_ctx, _cache, $props, $setup, $data, $options) {
  return common_vendor.e({
    a: $data.locating
  }, $data.locating ? {} : $data.located ? {
    c: common_vendor.t($data.locatedCity ? "：" + $data.locatedCity : "")
  } : {}, {
    b: $data.located,
    d: common_vendor.t($data.located ? "重新定位" : "开启定位"),
    e: common_vendor.o((...args) => $options.reLocate && $options.reLocate(...args)),
    f: common_vendor.o((...args) => $options.onSearch && $options.onSearch(...args)),
    g: $data.keyword,
    h: common_vendor.o(($event) => $data.keyword = $event.detail.value),
    i: common_vendor.o((...args) => $options.onSearch && $options.onSearch(...args)),
    j: $data.city,
    k: common_vendor.o(($event) => $data.city = $event.detail.value),
    l: common_vendor.o((...args) => $options.onSearch && $options.onSearch(...args)),
    m: $data.pinnedStore
  }, $data.pinnedStore ? common_vendor.e({
    n: common_vendor.t($data.pinnedStore.companyName),
    o: $data.pinnedStore.tenantCode
  }, $data.pinnedStore.tenantCode ? {
    p: common_vendor.t($data.pinnedStore.tenantCode)
  } : {}, {
    q: $options.channelText($data.pinnedStore.accessChannel)
  }, $options.channelText($data.pinnedStore.accessChannel) ? {
    r: common_vendor.t($options.channelText($data.pinnedStore.accessChannel))
  } : {}, {
    s: common_vendor.o(($event) => $options.selectStore($data.pinnedStore))
  }) : {}, {
    t: $data.boundTenants.length
  }, $data.boundTenants.length ? {
    v: common_vendor.f($data.boundTenants, (item, k0, i0) => {
      return common_vendor.e({
        a: common_vendor.t($options.initial(item.companyName)),
        b: common_vendor.t(item.companyName),
        c: item.isDefault === 1
      }, item.isDefault === 1 ? {} : {}, {
        d: item.tenantCode
      }, item.tenantCode ? {
        e: common_vendor.t(item.tenantCode)
      } : {}, {
        f: $options.channelText(item.accessChannel)
      }, $options.channelText(item.accessChannel) ? {
        g: common_vendor.t($options.channelText(item.accessChannel))
      } : {}, {
        h: "b" + item.tenantId,
        i: common_vendor.o(($event) => $options.selectStore(item), "b" + item.tenantId)
      });
    })
  } : {}, {
    w: common_vendor.t($data.located ? "附近门店" : "全部门店"),
    x: $data.loading
  }, $data.loading ? {} : common_vendor.e({
    y: common_vendor.f($data.stores, (item, k0, i0) => {
      return common_vendor.e({
        a: item.logoUrl
      }, item.logoUrl ? {
        b: $options.toHttps(item.logoUrl)
      } : {
        c: common_vendor.t($options.initial(item.companyName))
      }, {
        d: common_vendor.t(item.companyName),
        e: item.city
      }, item.city ? {
        f: common_vendor.t(item.city)
      } : {}, {
        g: item.address
      }, item.address ? {
        h: common_vendor.t(item.address)
      } : {}, {
        i: item.storeIntro
      }, item.storeIntro ? {
        j: common_vendor.t(item.storeIntro)
      } : {}, {
        k: item.bizCode
      }, item.bizCode ? {
        l: common_vendor.t(item.bizCode)
      } : {}, {
        m: $options.channelText(item.accessChannel)
      }, $options.channelText(item.accessChannel) ? {
        n: common_vendor.t($options.channelText(item.accessChannel))
      } : {}, {
        o: item.distanceKm != null
      }, item.distanceKm != null ? {
        p: common_vendor.t($options.formatDistance(item.distanceKm))
      } : {}, {
        q: "s" + item.tenantId,
        r: common_vendor.o(($event) => $options.selectStore(item), "s" + item.tenantId)
      });
    }),
    z: !$data.stores.length
  }, !$data.stores.length ? {} : {}));
}
const MiniProgramPage = /* @__PURE__ */ common_vendor._export_sfc(_sfc_main, [["render", _sfc_render], ["__scopeId", "data-v-e965526f"]]);
wx.createPage(MiniProgramPage);
