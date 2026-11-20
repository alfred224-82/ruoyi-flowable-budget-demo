"use strict";
function formatAmount(value) {
  const num = Number(value);
  if (isNaN(num)) return "0.00";
  return num.toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}
function formatWan(value) {
  const num = Number(value);
  if (isNaN(num)) return "0";
  if (Math.abs(num) >= 1e4) {
    return (num / 1e4).toFixed(2) + "万";
  }
  return formatAmount(num);
}
function formatDate(value) {
  if (!value) return "-";
  const d = new Date(value);
  if (isNaN(d.getTime())) return String(value).substring(0, 10);
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}
function formatDateTime(value) {
  if (!value) return "-";
  const d = new Date(value);
  if (isNaN(d.getTime())) return String(value);
  return formatDate(d) + " " + String(d.getHours()).padStart(2, "0") + ":" + String(d.getMinutes()).padStart(2, "0") + ":" + String(d.getSeconds()).padStart(2, "0");
}
const PROJECT_STATUS = {
  DRAFT: { label: "草稿", color: "#909399" },
  PENDING_APPROVAL: { label: "待审批", color: "#e6a23c" },
  ACTIVE: { label: "进行中", color: "#67c23a" },
  COMPLETED: { label: "已完成", color: "#409eff" },
  CLOSED: { label: "已关闭", color: "#909399" },
  LOST: { label: "烂账", color: "#f56c6c" }
};
const EXPENSE_STATUS = {
  DRAFT: { label: "草稿", color: "#909399" },
  SUBMITTED: { label: "待审批", color: "#e6a23c" },
  APPROVED: { label: "已通过", color: "#67c23a" },
  REJECTED: { label: "已驳回", color: "#f56c6c" },
  OVER_BUDGET_PENDING: { label: "超预算审批中", color: "#e6a23c" }
};
const INCOME_STATUS = {
  PENDING: { label: "待核验", color: "#e6a23c" },
  VERIFIED: { label: "已核验", color: "#67c23a" },
  REJECTED: { label: "已驳回", color: "#f56c6c" }
};
const CHANGE_STATUS = {
  PENDING: { label: "待审批", color: "#e6a23c" },
  REJECTED: { label: "已驳回", color: "#f56c6c" },
  CONFIRMED: { label: "已确认", color: "#67c23a" },
  OVERDUE: { label: "逾期未确认", color: "#f56c6c" }
};
const SUPPLEMENT_STATUS = {
  PENDING: { label: "待补票", color: "#e6a23c" },
  COMPLETED: { label: "已补票", color: "#67c23a" },
  OVERDUE: { label: "已超期", color: "#f56c6c" }
};
function statusText(map, status) {
  return (map[status] || {}).label || status || "-";
}
function statusColor(map, status) {
  return (map[status] || {}).color || "#909399";
}
function today() {
  return formatDate(/* @__PURE__ */ new Date());
}
function currentPeriod() {
  return today().substring(0, 7);
}
function addDays(dateStr, days) {
  const d = dateStr ? new Date(dateStr) : /* @__PURE__ */ new Date();
  d.setDate(d.getDate() + days);
  return formatDate(d);
}
const BOOKING_STATUS = {
  PENDING_SERVICE: { label: "待服务", color: "#e6a23c" },
  PENDING_PAY: { label: "未收款", color: "#409eff" },
  COMPLETED: { label: "已完成", color: "#67c23a" },
  CANCELLED: { label: "已取消", color: "#909399" },
  NO_SHOW: { label: "已爽约", color: "#f56c6c" }
};
const PAY_STATUS = {
  PENDING_PAY: { label: "未收款", color: "#e6a23c" },
  RECEIVED: { label: "已收款", color: "#67c23a" }
};
function parseBookingCost(remark) {
  const empty = { details: [], total: 0, hours: 0, peopleCount: 0, remarkText: "" };
  if (!remark || typeof remark !== "string") return empty;
  let obj = null;
  try {
    obj = JSON.parse(remark);
  } catch (e) {
    return empty;
  }
  if (!obj || typeof obj !== "object") return empty;
  const details = Array.isArray(obj.costDetails) ? obj.costDetails : [];
  let total = Number(obj.totalAmount);
  if (isNaN(total)) total = details.reduce((s, d) => s + Number(d.subtotal || 0), 0);
  return {
    details,
    total,
    hours: Number(obj.hours || 0),
    peopleCount: Number(obj.peopleCount || 0),
    remarkText: obj.remark || ""
  };
}
function isLocalUrl(url) {
  return /^https?:\/\/(localhost|127\.0\.0\.1|0\.0\.0\.0|192\.168\.\d+\.\d+|10\.\d+\.\d+\.\d+|172\.(1[6-9]|2\d|3[01])\.\d+\.\d+)/i.test(url);
}
function toHttps(url) {
  if (!url) return url;
  if (typeof url !== "string") return url;
  if (isLocalUrl(url)) return url;
  if (typeof window === "undefined") return url.replace(/^http:\/\//i, "https://");
  if (window.location.protocol === "https:") return url.replace(/^http:\/\//i, "https://");
  return url;
}
function avatarSize(url, _size) {
  return toHttps(url);
}
function isImageUrl(url) {
  return !!url && typeof url === "string" && !/^\d+$/.test(url.trim());
}
function splitImages(value) {
  if (!value || typeof value !== "string") return [];
  return value.split(",").map((s) => s.trim()).filter((s) => isImageUrl(s));
}
function firstImage(value) {
  const list = splitImages(value);
  return list.length ? list[0] : "";
}
exports.BOOKING_STATUS = BOOKING_STATUS;
exports.CHANGE_STATUS = CHANGE_STATUS;
exports.EXPENSE_STATUS = EXPENSE_STATUS;
exports.INCOME_STATUS = INCOME_STATUS;
exports.PAY_STATUS = PAY_STATUS;
exports.PROJECT_STATUS = PROJECT_STATUS;
exports.SUPPLEMENT_STATUS = SUPPLEMENT_STATUS;
exports.addDays = addDays;
exports.avatarSize = avatarSize;
exports.currentPeriod = currentPeriod;
exports.firstImage = firstImage;
exports.formatAmount = formatAmount;
exports.formatDate = formatDate;
exports.formatDateTime = formatDateTime;
exports.formatWan = formatWan;
exports.isImageUrl = isImageUrl;
exports.parseBookingCost = parseBookingCost;
exports.splitImages = splitImages;
exports.statusColor = statusColor;
exports.statusText = statusText;
exports.toHttps = toHttps;
exports.today = today;
