/* ============================================================
   贴纸 & 图标 · 接入脚本
   依赖：svg_assets.css、stickers/*.svg
   作用：1) 声明资源清单  2) 生成 DOM  3) 主题切换时自动换色
   ============================================================ */

/* ---------- 资源清单 ---------- */
const STICKERS = [
  { f: 'hand-star',    n: '星星' },
  { f: 'hand-bubble',  n: '气泡' },
  { f: 'hand-cloud',   n: '云朵' },
  { f: 'hand-heart',   n: '爱心' },
  { f: 'hand-sparkle', n: '闪光' },
  { f: 'hand-sun',     n: '太阳' },
  { f: 'hand-flower',  n: '花朵' },
  { f: 'hand-leaf',    n: '叶子' },
];

const ICONS = [
  { f: 'ico-home',      n: '发现'   },
  { f: 'ico-activity',  n: '活动'   },
  { f: 'ico-todo',      n: '待处理' },
  { f: 'ico-qrcode',    n: '推广码' },
  { f: 'ico-mine',      n: '我的'   },
  { f: 'ico-workbench', n: '工作台' },
  { f: 'ico-booking',   n: '预约'   },
  { f: 'ico-setting',   n: '设置'   },
  { f: 'ico-feedback',  n: '反馈'   },
  { f: 'ico-logout',    n: '退出'   },
  { f: 'ico-refresh',   n: '刷新'   },
  { f: 'ico-location',  n: '定位'   },
  { f: 'ico-search',    n: '搜索'   },
  { f: 'ico-arrow',     n: '箭头'   },
  { f: 'ico-check',     n: '对勾'   },
];

/* 图标在页面中的复用：把某个图标挂到已有元素上，
   只需给元素加 data-ico="ico-home" 即可，无需手写 <svg> */
const ICO_MAP = Object.fromEntries(ICONS.map(i => [i.f, i.f]));

/* 资源解析优先顺序：
   1) ICON_URI / STICKER_URI（data URI，本地 file:// 与线上都能用）
   2) stickers/ 目录下的独立 .svg 文件（部署到 http(s) 服务器时可切回）
   改 USE_FILE 为 true 即切换为文件引用模式。 */
const USE_FILE = false;
const PATH = 'stickers/';

function uri(map, name) {
  if (!USE_FILE && typeof map !== 'undefined' && map[name]) return `url('${map[name]}')`;
  return `url('${PATH}${name}.svg')`;
}

/* ---------- 渲染：贴纸（<img> 直引，自带颜色） ---------- */
function renderStickers(el) {
  el.innerHTML = STICKERS.map(s => `
    <img class="sticker" src="${USE_FILE ? PATH + s.f + '.svg' : STICKER_URI[s.f]}"
         alt="${s.n}" title="${s.n} · ${s.f}.svg">
  `).join('');
}

/* ---------- 渲染：图标（CSS mask，可变色） ---------- */
function renderIcons(el, size = '') {
  el.innerHTML = ICONS.map(i => `
    <div>
      <span class="ico-svg ${size}" style="--ico:${uri(ICON_URI, i.f)}"></span>
      <span class="nm">${i.n}</span>
    </div>
  `).join('');
}

/* ---------- 渲染：四主题取色演示 ---------- */
function renderTones(el) {
  const themes = [
    { t: 't1', n: '主题1', c: '#4F8FF7' },
    { t: 't2', n: '主题2', c: '#F5455C' },
    { t: 't3', n: '主题3', c: '#00E5FF' },
    { t: 't4', n: '主题4', c: '#F08A24' },
  ];
  el.innerHTML = themes.map(x => `
    <div>
      <span class="ico-svg lg" style="--ico:${uri(ICON_URI,'ico-home')}; background:${x.c}"></span>
      <div class="lb">${x.n}</div>
    </div>
  `).join('');
}

/* ---------- 给已有 DOM 批量挂图标 ----------
   HTML 里写 <i data-ico="ico-search" class="ico-svg"></i>
   本函数自动补 --ico 变量，省得逐个写 style
------------------------------------------------ */
function bindIcons(root = document) {
  root.querySelectorAll('[data-ico]').forEach(el => {
    const name = el.getAttribute('data-ico');
    el.style.setProperty('--ico', uri(ICON_URI, name));
    el.classList.add('ico-svg');
  });
}

/* ---------- 主题切换 ----------
   切主题只改 <html data-theme>，颜色由 CSS 变量自动生效，
   JS 不需要逐个改图标颜色 —— 这是 CSS 变量方案最大的好处。
------------------------------------------------ */
function setTheme(t, btn) {
  document.documentElement.setAttribute('data-theme', t);
  document.querySelectorAll('.switcher button')
          .forEach(b => b.classList.toggle('on', b === btn));
  try { localStorage.setItem('xzb-theme', t); } catch (e) {}
}

/* ---------- 初始化：记住上次选择的主题 ---------- */
(function init() {
  let saved = 't1';
  try { saved = localStorage.getItem('xzb-theme') || 't1'; } catch (e) {}
  document.documentElement.setAttribute('data-theme', saved);
  document.querySelectorAll('.switcher button').forEach(b => {
    b.classList.toggle('on', b.dataset.theme === saved);
  });
})();
