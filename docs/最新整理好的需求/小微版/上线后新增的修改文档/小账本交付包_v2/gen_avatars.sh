#!/usr/bin/env bash
# ============================================================
# 头像批量处理脚本
# 用法：./gen_avatars.sh 输入目录 输出目录
# 依赖：ImageMagick (magick) + libwebp (cwebp)
#   Ubuntu/Debian: sudo apt install imagemagick webp
#   macOS:         brew install imagemagick webp
# ============================================================
set -e
IN=${1:-./avatars_orig}
OUT=${2:-./avatars_out}
mkdir -p "$OUT"

# 三档尺寸：sm 顶栏小图 / md 列表常用 / lg 个人中心大图
SM=80
MD=120
LG=200

for f in "$IN"/*.{jpg,jpeg,png,JPG,JPEG,PNG}; do
  [ -e "$f" ] || continue
  name=$(basename "${f%.*}")

  # -resize 用 ^ 填充 + -gravity center -extent 居中裁正方形
  # （头像多为圆形，非正方形原图直接缩放会被拉变形）

  # WebP 三档
  magick "$f" -resize "${SM}x${SM}^" -gravity center -extent "${SM}x${SM}" \
         -strip -quality 80 "$OUT/${name}_sm.webp"
  magick "$f" -resize "${MD}x${MD}^" -gravity center -extent "${MD}x${MD}" \
         -strip -quality 80 "$OUT/${name}_md.webp"
  magick "$f" -resize "${LG}x${LG}^" -gravity center -extent "${LG}x${LG}" \
         -strip -quality 80 "$OUT/${name}_lg.webp"

  # JPEG 兜底（不支持 WebP 的老浏览器）
  magick "$f" -resize "${MD}x${MD}^" -gravity center -extent "${MD}x${MD}" \
         -strip -interlace Plane -quality 82 "$OUT/${name}_md.jpg"

  echo "✓ $name  $(du -h "$f" | cut -f1) → sm/md/lg 已生成"
done

echo ""
echo "产物清单："
du -sh "$OUT"
ls -1 "$OUT" | wc -l | xargs echo "文件数："
