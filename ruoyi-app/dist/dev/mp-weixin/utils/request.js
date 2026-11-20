"use strict";
const common_vendor = require("../common/vendor.js");
const config_index = require("../config/index.js");
const utils_auth = require("./auth.js");
var _documentCurrentScript = typeof document !== "undefined" ? document.currentScript : null;
const __vite_import_meta_env__ = {};
const isH5 = typeof window !== "undefined" && typeof navigator !== "undefined";
const isDev = typeof { url: typeof document === "undefined" ? require("url").pathToFileURL(__filename).href : _documentCurrentScript && _documentCurrentScript.tagName.toUpperCase() === "SCRIPT" && _documentCurrentScript.src || new URL("utils/request.js", document.baseURI).href } !== "undefined" && !!__vite_import_meta_env__ && true;
const diagContext = () => ({
  page: isH5 ? window.location.href : "mp",
  origin: isH5 ? window.location.origin : "mp",
  sw: isH5 && navigator.serviceWorker ? !!navigator.serviceWorker.controller : false
});
const request = (options) => {
  return new Promise((resolve, reject) => {
    const token = utils_auth.getToken();
    let fullUrl = config_index.config.baseUrl + options.url;
    if (isH5 && isDev && (options.method || "GET").toUpperCase() === "GET") {
      fullUrl += (fullUrl.includes("?") ? "&" : "?") + "_cb=" + Date.now();
    }
    const ctx = diagContext();
    console.log("[request] →", options.method || "GET", fullUrl, "| page=", ctx.page, "| swControlled=", ctx.sw);
    common_vendor.index.request({
      url: fullUrl,
      method: options.method || "GET",
      data: options.data || {},
      timeout: config_index.config.timeout,
      header: {
        "Content-Type": "application/json;charset=utf-8",
        ...token ? { Authorization: "Bearer " + token } : {},
        ...options.header || {}
      },
      success: (res) => {
        const body = res.data;
        const header = res.header || {};
        const ctype = header["Content-Type"] || header["content-type"] || "";
        console.log("[request] ←", fullUrl, "| statusCode=", res.statusCode, "| content-type=", ctype, "| typeof body=", typeof body);
        if (typeof body === "string") {
          console.error(
            "[request] ★接口返回网页而非数据",
            fullUrl,
            "| statusCode=",
            res.statusCode,
            "| content-type=",
            ctype,
            "| page=",
            ctx.page,
            "| swControlled=",
            ctx.sw,
            "| headers=",
            JSON.stringify(header),
            "| bodyHead=",
            body.slice(0, 300)
          );
          common_vendor.index.showModal({
            title: "接口诊断",
            content: "页面源：" + ctx.origin + "\n请求地址：" + fullUrl + "\n状态码：" + res.statusCode + "\nSW拦截：" + (ctx.sw ? "是" : "否") + "\n请将此截图反馈开发",
            showCancel: false
          });
          return reject(new Error("接口返回非 JSON 响应（statusCode=" + res.statusCode + "）"));
        }
        const data = body || {};
        if (res.statusCode === 401 || data.code === 401) {
          utils_auth.clearAuth();
          return reject(new Error("登录已过期，请重新登录"));
        }
        if (data.code === void 0) {
          console.error("[request] ★响应缺少 code 字段", fullUrl, "| statusCode=", res.statusCode, "| body=", JSON.stringify(data).slice(0, 300), "| page=", ctx.page);
          common_vendor.index.showToast({ title: "接口异常（状态码 " + res.statusCode + "），请稍后重试", icon: "none" });
          return reject(new Error("接口响应缺少 code 字段（statusCode=" + res.statusCode + "）"));
        }
        if (data.code !== 200) {
          common_vendor.index.showToast({ title: data.msg || "请求失败", icon: "none" });
          return reject(new Error(data.msg || "请求失败"));
        }
        resolve(data);
      },
      fail: (err) => {
        console.error("[request] ★网络失败", fullUrl, "| err=", err && err.errMsg, "| page=", ctx.page, "| swControlled=", ctx.sw);
        common_vendor.index.showToast({ title: "网络异常，请稍后重试", icon: "none" });
        reject(err);
      }
    });
  });
};
const get = (url, data) => request({ url, method: "GET", data });
const post = (url, data) => request({ url, method: "POST", data });
const put = (url, data) => request({ url, method: "PUT", data });
const del = (url, data) => request({ url, method: "DELETE", data });
const upload = (url, filePath, formData) => {
  return new Promise((resolve, reject) => {
    const token = utils_auth.getToken();
    common_vendor.index.uploadFile({
      url: config_index.config.baseUrl + url,
      filePath,
      name: "file",
      formData: {},
      timeout: config_index.config.timeout,
      header: token ? { Authorization: "Bearer " + token } : {},
      success: (res) => {
        let body = {};
        try {
          body = JSON.parse(res.data);
        } catch (e) {
          common_vendor.index.showToast({ title: "上传响应解析失败", icon: "none" });
          return reject(new Error("上传响应解析失败"));
        }
        if (res.statusCode === 401 || body.code === 401) {
          utils_auth.clearAuth();
          return reject(new Error("登录已过期，请重新登录"));
        }
        if (body.code !== void 0 && body.code !== 200) {
          common_vendor.index.showToast({ title: body.msg || "上传失败", icon: "none" });
          return reject(new Error(body.msg || "上传失败"));
        }
        resolve(body);
      },
      fail: (err) => {
        common_vendor.index.showToast({ title: "网络异常，请稍后重试", icon: "none" });
        reject(err);
      }
    });
  });
};
exports.del = del;
exports.get = get;
exports.post = post;
exports.put = put;
exports.request = request;
exports.upload = upload;
