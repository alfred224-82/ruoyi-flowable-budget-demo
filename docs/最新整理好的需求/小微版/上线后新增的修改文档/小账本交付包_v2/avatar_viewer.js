/* ============================================================
   头像点击放大查看器
   用法：给任意 <img class="avatar"> 加 data-zoom 即可
        <img class="avatar" src="avatars_out/a1_female_sea_md.webp"
             data-zoom data-name="张伟">

   只有两档：
     缩略档 _md.webp  120px  2.6–4.1 KB   列表/顶栏/卡片，首屏加载
     放大档 _xl.webp  400px  15.9–21.4 KB 点击后才加载，不进首屏

   设计要点：
   1. 事件委托 —— 动态插入的头像（列表刷新）无需重新绑定
   2. 渐进加载 —— 先用已缓存的缩略图模糊放大占位，xl 到位后替换
   3. 按需加载 —— 不点就不下载大图，不占首屏流量
   4. 自动命名 —— 没写 data-xl 时，按 _md/_sm → _xl 规则推导
   ============================================================ */

(function () {
  // ---------- 创建唯一的遮罩层 ----------
  const mask = document.createElement('div');
  mask.className = 'av-mask';
  mask.innerHTML = `
    <span class="av-tip">点击任意处关闭</span>
    <img alt="">
    <div class="av-cap"></div>
    <div class="av-close">×</div>
  `;
  document.body.appendChild(mask);

  const imgEl = mask.querySelector('img');
  const capEl = mask.querySelector('.av-cap');

  // ---------- 推导 xl 大图路径（只有缩略档 md 与放大档 xl 两档）----------
  // a1_female_sea_md.webp / .jpg  →  a1_female_sea_xl.webp
  function xlOf(src) {
    const m = src.match(/^(.*)_md\.(webp|jpg|jpeg|png)$/i);
    return m ? `${m[1]}_xl.webp` : src;   // 匹配不到就用原图，不报错
  }

  // ---------- 打开 ----------
  function open(el) {
    const thumb = el.currentSrc || el.src;
    const xl = el.dataset.xl || xlOf(thumb);
    const name = el.dataset.name || el.alt || '';

    capEl.textContent = name;
    // 第一步：用缩略图模糊放大立即显示（已在缓存里，0 延迟）
    imgEl.src = thumb;
    imgEl.className = 'loading';
    mask.classList.add('on');

    // 第二步：后台加载 xl，到位后平滑替换
    const full = new Image();
    full.onload = () => {
      // 防止快速连点导致错图
      if (!mask.classList.contains('on')) return;
      imgEl.src = xl;
      imgEl.className = 'ready';
    };
    full.onerror = () => { imgEl.className = 'ready'; };  // 失败就保持缩略图
    full.src = xl;
  }

  // ---------- 关闭 ----------
  function close() {
    mask.classList.remove('on');
    setTimeout(() => { imgEl.removeAttribute('src'); }, 240);
  }

  // ---------- 事件委托：只需绑定一次 ----------
  document.addEventListener('click', (e) => {
    const av = e.target.closest('[data-zoom]');
    if (av) { open(av); return; }
    if (e.target.closest('.av-mask')) close();
  });

  // ---------- 键盘与手势 ----------
  document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape') close();
  });
  // iOS 下滑关闭
  let y0 = null;
  mask.addEventListener('touchstart', (e) => { y0 = e.touches[0].clientY; }, { passive: true });
  mask.addEventListener('touchmove', (e) => {
    if (y0 === null) return;
    if (e.touches[0].clientY - y0 > 80) { close(); y0 = null; }
  }, { passive: true });

  // ---------- 对外暴露（可选手动调用） ----------
  window.AvatarViewer = { open, close };
})();
