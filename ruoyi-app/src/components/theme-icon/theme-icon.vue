<template>
    <!-- 主题图标（20261016 四主题重设计）：CSS mask 方案，颜色随主题主色自动变化。
         easycom 自动注册，页面直接 <theme-icon name="ico-home" size="lg" /> 即可。 -->
    <text class="th-ico" :class="[size, { muted: muted }]" :style="icoStyle"></text>
</template>

<script>
import { iconStyle } from '../../utils/themeIcons'

export default {
    name: 'ThemeIcon',
    props: {
        // 图标名（见 utils/themeIcons.js THEME_ICONS，如 ico-home / ico-qrcode）
        name: { type: String, required: true },
        // 尺寸变体：''(默认44rpx) / 'sm'(32rpx) / 'lg'(64rpx)
        size: { type: String, default: '' },
        // 可选覆盖色（不传则跟随主题 --icon-color / --brand）
        color: { type: String, default: '' },
        // 次要图标（用辅助文字色）
        muted: { type: Boolean, default: false }
    },
    computed: {
        icoStyle() {
            return iconStyle(this.name, this.color || undefined)
        }
    }
}
</script>
