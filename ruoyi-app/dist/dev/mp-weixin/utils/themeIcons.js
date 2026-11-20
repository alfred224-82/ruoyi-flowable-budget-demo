"use strict";
const ICON_BASE = "/static/theme/stickers";
function iconUrl(name) {
  return `${ICON_BASE}/${name}.svg`;
}
exports.iconUrl = iconUrl;
