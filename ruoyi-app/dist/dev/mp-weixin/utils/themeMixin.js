"use strict";
const utils_theme = require("./theme.js");
const themeMixin = {
  data() {
    return {
      themeKey: utils_theme.resolveThemeKey(null),
      themePinned: null
    };
  },
  computed: {
    /** 页面根节点注入的完整主题 CSS 变量集 */
    pageStyle() {
      return utils_theme.themeCssVars(this.themeKey);
    },
    /** 主题类名（供全局 theme.css 中 .theme-xxx 差异化钩子使用） */
    themeClass() {
      return "theme-" + this.themeKey;
    }
  },
  onLoad(options) {
    if (options && options.theme) {
      this.themePinned = utils_theme.resolveThemeKey(options.theme);
      this.themeKey = this.themePinned;
    }
  },
  onShow() {
    if (this.themeAutoLoad === false || this.themePinned) return;
    const cached = utils_theme.getThemeSync();
    if (cached && cached.key) this.themeKey = cached.key;
    utils_theme.loadTenantTheme().then((t) => {
      if (t && t.key && !this.themePinned) this.themeKey = t.key;
    }).catch(() => {
    });
  }
};
exports.themeMixin = themeMixin;
