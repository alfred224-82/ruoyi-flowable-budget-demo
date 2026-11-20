<template>
    <!-- ★M34 T34.6 顾客点头像弹名片（设计方案 §八之二）
         用法：页面底部放 <staff-card-popup ref="card" />，
               员工头像处 <image @click="$refs.card.open(host)" />
         host = { staffId, name, avatar, role, intro }
         名片联系方式矩阵（show_phone × show_wechat）：
           开开 = 打电话(#18B478) + 加微信(#07C160) 两图标
           开关 = 只有打电话
           关开 = 只有加微信（二维码长按识别）
           关关 = 只有头像和名称 + 提示「请通过门店联系」 -->
    <view v-if="visible" class="scp-mask" @click="close" @touchstart="onTouchStart" @touchmove="onTouchMove">
        <view class="scp-sheet" @click.stop>
            <!-- 顶部拖拽条 -->
            <view class="scp-handle"></view>

            <!-- 名片头：头像 + 姓名 + 角色 + 简介 -->
            <view class="scp-head">
                <image v-if="card.avatar" class="scp-avatar" :src="toHttps(card.avatar)" mode="aspectFill" />
                <view v-else class="scp-avatar scp-avatar-none">
                    <text class="scp-avatar-char">{{ (card.name || '?').charAt(0) }}</text>
                </view>
                <view class="scp-head-info">
                    <view class="scp-name-line">
                        <text class="scp-name">{{ card.name || '-' }}</text>
                        <text v-if="roleLabel" class="scp-role">{{ roleLabel }}</text>
                    </view>
                    <text v-if="card.intro" class="scp-intro">{{ card.intro }}</text>
                </view>
                <view class="scp-close" @click.stop="close"><text class="scp-close-x">×</text></view>
            </view>

            <!-- 加载中 -->
            <view v-if="loading" class="scp-loading"><text class="scp-loading-text">加载联系方式…</text></view>

            <!-- 联系方式区 -->
            <view v-else class="scp-contacts">
                <!-- 至少一个开关打开：显示对应入口 -->
                <view v-if="showPhone || showWechat" class="scp-actions">
                    <!-- 打电话：绿色 #18B478 手机图标 -->
                    <view v-if="showPhone" class="scp-action" @click="dial">
                        <view class="scp-icon-box phone">
                            <text class="scp-icon-glyph">📱</text>
                        </view>
                        <text class="scp-action-label">打电话</text>
                    </view>
                    <!-- 加微信：微信绿 #07C160 气泡图标 -->
                    <view v-if="showWechat" class="scp-action" @click="toggleWechat">
                        <view class="scp-icon-box wechat">
                            <text class="scp-icon-glyph">💬</text>
                        </view>
                        <text class="scp-action-label">加微信</text>
                    </view>
                </view>

                <!-- 微信二维码（点击「加微信」后展开，长按识别添加） -->
                <view v-if="showWechat && wechatOpen" class="scp-wechat-panel">
                    <image v-if="wechatQrcodeUrl" class="scp-qrcode" :src="toHttps(wechatQrcodeUrl)" mode="aspectFit" show-menu-by-longpress />
                    <view v-else class="scp-qrcode scp-qrcode-none">
                        <text class="scp-qrcode-tip">暂未上传微信二维码</text>
                    </view>
                    <text class="scp-wechat-hint">长按识别二维码添加微信</text>
                </view>

                <!-- 两开关全关：只显示头像姓名 + 门店转达提示 -->
                <view v-if="!showPhone && !showWechat" class="scp-private">
                    <text class="scp-private-text">该员工未公开联系方式，请通过门店联系</text>
                    <text class="scp-private-link" @click="contactStore">联系门店 ›</text>
                </view>
            </view>
        </view>
    </view>
</template>

<script>
/**
 * 顾客点头像弹名片组件（M34 T34.6，设计方案 §八之二）
 *
 * 设计要点：
 * 1. 顾客在有员工头像处点击 → 底部弹出名片（头像/姓名/角色/简介）
 * 2. 联系方式按 staff_contact_public.show_phone / show_wechat 开关显示：
 *    - 打电话：绿色 #18B478 手机图标 → tel: 拨号
 *    - 加微信：微信绿 #07C160 气泡图标 → 展示二维码长按识别
 * 3. 两开关全关 → 只显示头像姓名 + 提示「请通过门店联系」
 * 4. 开关状态实时从 BE GET /micro/staff/contactPublic/{staffId} 拉取（默认全关）
 * 5. 关闭方式：点击遮罩 / Esc（H5）/ iOS 下滑 / × 按钮
 */
import { get } from '../../utils/request'
import config from '../../config'
import { toHttps } from '../../utils/format'
import { loadRoleDisplayNames } from '../../utils/roleDisplay'

export default {
    name: 'StaffCardPopup',
    data() {
        return {
            visible: false,
            loading: false,
            wechatOpen: false,
            card: { staffId: '', name: '', avatar: '', role: '', intro: '', phone: '' },
            showPhone: false,
            showWechat: false,
            wechatQrcodeUrl: '',
            touchStartY: 0,
            // ★行业角色显示名映射（salesman→导游/助教，boss→旅行社社长等）
            roleDisplayNames: {}
        }
    },
    computed: {
        roleLabel() {
            // 优先用父组件透传的角色显示名，其次用租户角色映射，最后回退硬编码
            if (this.card.roleLabel) return this.card.roleLabel
            const dynamic = this.roleDisplayNames[this.card.role]
            if (dynamic) return dynamic
            const map = { salesman: '员工', boss: '老板', host: '主持人', customer: '顾客' }
            return map[this.card.role] || ''
        }
    },
    methods: {
        toHttps,
        /**
         * 打开名片（供父组件 ref 调用）
         * @param {Object} staff { staffId, name, avatar, role, intro }
         */
        open(staff) {
            if (!staff) return
            this.card = {
                staffId: staff.staffId || staff.userId || '',
                name: staff.name || staff.nickName || '',
                avatar: staff.avatar || '',
                role: staff.role || 'salesman',
                intro: staff.intro || staff.remark || '',
                phone: staff.phone || staff.fullPhone || staff.phoneNumber || ''
            }
            // 默认全关，拉取后再决定显示（fail-safe：拉取失败保持全关）
            this.showPhone = false
            this.showWechat = false
            this.wechatQrcodeUrl = ''
            this.wechatOpen = false
            this.visible = true
            // ★加载租户角色显示名映射（行业术语动态化：salesman→导游/助教）
            loadRoleDisplayNames().then(map => { this.roleDisplayNames = map || {} })
            this.loadContact()
        },
        close() {
            this.visible = false
            this.wechatOpen = false
        },
        /** 拉取员工联系方式公开开关（默认全关，BE 无记录时返回 showPhone=0/showWechat=0） */
        async loadContact() {
            if (!this.card.staffId) return
            this.loading = true
            try {
                const res = await get(config.api.microStaffContactPublic + this.card.staffId)
                const d = (res && res.data) || {}
                this.showPhone = d.showPhone === 1 || d.showPhone === true
                this.showWechat = d.showWechat === 1 || d.showWechat === true
                this.wechatQrcodeUrl = d.wechatQrcodeUrl || ''
            } catch (e) {
                // 拉取失败保持全关（隐私优先，不默认公开）
                this.showPhone = false
                this.showWechat = false
            } finally {
                this.loading = false
            }
        },
        /** 打电话：tel: 拨号（H5 用 location.href，小程序用 makePhoneCall） */
        dial() {
            const phone = this.card.phone || this.card.fullPhone || ''
            if (!phone) {
                uni.showToast({ title: '暂无可拨号码', icon: 'none' })
                return
            }
            // #ifdef H5
            window.location.href = 'tel:' + phone
            // #endif
            // #ifndef H5
            uni.makePhoneCall({ phoneNumber: String(phone) })
            // #endif
        },
        /** 加微信：展开/收起二维码面板 */
        toggleWechat() {
            this.wechatOpen = !this.wechatOpen
        },
        /** 联系门店：跳转顾客端「联系我们」页 */
        contactStore() {
            this.close()
            uni.navigateTo({ url: '/pages/micro/customer/contactUs' })
        },
        /** iOS 下滑关闭 */
        onTouchStart(e) {
            this.touchStartY = e.touches ? e.touches[0].clientY : 0
        },
        onTouchMove(e) {
            if (!this.touchStartY) return
            const dy = (e.touches ? e.touches[0].clientY : 0) - this.touchStartY
            if (dy > 80) {
                this.close()
                this.touchStartY = 0
            }
        }
    },
    // #ifdef H5
    mounted() {
        this._escHandler = (e) => {
            if (e.key === 'Escape' && this.visible) this.close()
        }
        document.addEventListener('keydown', this._escHandler)
    },
    beforeDestroy() {
        if (this._escHandler) document.removeEventListener('keydown', this._escHandler)
    }
    // #endif
}
</script>

<style scoped>
/* ---------- 遮罩层 ---------- */
.scp-mask {
    position: fixed; top: 0; left: 0; right: 0; bottom: 0;
    z-index: 9998;
    background: rgba(0, 0, 0, .5);
    display: flex; align-items: flex-end; justify-content: center;
    animation: scp-fade-in 200ms ease both;
}
@keyframes scp-fade-in { from { opacity: 0; } to { opacity: 1; } }

/* ---------- 底部弹出名片 ---------- */
.scp-sheet {
    width: 100%;
    max-width: 750rpx;
    background: var(--card-bg, #fff);
    border-radius: 32rpx 32rpx 0 0;
    padding: 16rpx 40rpx calc(env(safe-area-inset-bottom) + 40rpx);
    box-sizing: border-box;
    animation: scp-slide-up 260ms cubic-bezier(.34, 1.2, .64, 1) both;
}
@keyframes scp-slide-up { from { transform: translateY(100%); } to { transform: translateY(0); } }

.scp-handle {
    width: 72rpx; height: 8rpx; border-radius: 4rpx;
    background: var(--border, #e4e7ed);
    margin: 8rpx auto 24rpx;
}

/* ---------- 名片头 ---------- */
.scp-head { display: flex; align-items: center; gap: 24rpx; }
.scp-avatar {
    width: 120rpx; height: 120rpx; border-radius: 50%;
    background: var(--brand-weak, #f0f2f5); flex-shrink: 0;
}
.scp-avatar-none { display: flex; align-items: center; justify-content: center; }
.scp-avatar-char { font-size: 48rpx; color: var(--text-sub, #909399); font-weight: bold; }
.scp-head-info { flex: 1; overflow: hidden; }
.scp-name-line { display: flex; align-items: center; gap: 12rpx; }
.scp-name { font-size: 34rpx; font-weight: bold; color: var(--text, #1a1a1a); }
.scp-role {
    font-size: 22rpx; color: #fff; background: var(--brand, #FFB347);
    padding: 2rpx 14rpx; border-radius: 8rpx;
}
.scp-intro {
    display: block; font-size: 26rpx; color: var(--text-sub, #909399);
    margin-top: 10rpx; line-height: 1.5;
}
.scp-close {
    width: 56rpx; height: 56rpx; border-radius: 50%;
    background: var(--brand-weak, #f0f2f5);
    display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.scp-close-x { font-size: 36rpx; color: var(--text-sub, #909399); line-height: 1; }

/* ---------- 加载 ---------- */
.scp-loading { text-align: center; padding: 48rpx 0; }
.scp-loading-text { font-size: 26rpx; color: var(--text-sub, #909399); }

/* ---------- 联系方式 ---------- */
.scp-contacts { padding-top: 32rpx; }
.scp-actions { display: flex; gap: 48rpx; justify-content: center; }
.scp-action { display: flex; flex-direction: column; align-items: center; gap: 12rpx; }
.scp-icon-box {
    width: 108rpx; height: 108rpx; border-radius: 28rpx;
    display: flex; align-items: center; justify-content: center;
    transition: transform 120ms ease;
}
.scp-icon-box:active { transform: scale(.94); }
/* 打电话：浅色底 + 绿色 #18B478 图标 */
.scp-icon-box.phone { background: rgba(24, 180, 120, .12); }
/* 加微信：浅色底 + 微信绿 #07C160 图标 */
.scp-icon-box.wechat { background: rgba(7, 193, 96, .12); }
.scp-icon-glyph { font-size: 52rpx; line-height: 1; }
.scp-action-label { font-size: 26rpx; color: var(--text, #3a3a3a); }

/* ---------- 微信二维码面板 ---------- */
.scp-wechat-panel {
    margin-top: 32rpx; padding: 32rpx;
    background: var(--brand-weak, #f7f8fa); border-radius: 20rpx;
    display: flex; flex-direction: column; align-items: center;
}
.scp-qrcode { width: 320rpx; height: 320rpx; border-radius: 12rpx; background: #fff; }
.scp-qrcode-none { display: flex; align-items: center; justify-content: center; }
.scp-qrcode-tip { font-size: 24rpx; color: var(--text-sub, #c0c4cc); }
.scp-wechat-hint { font-size: 24rpx; color: var(--text-sub, #909399); margin-top: 16rpx; }

/* ---------- 全关提示 ---------- */
.scp-private { text-align: center; padding: 16rpx 0 8rpx; }
.scp-private-text { display: block; font-size: 26rpx; color: var(--text-sub, #909399); }
.scp-private-link {
    display: inline-block; margin-top: 16rpx; font-size: 26rpx;
    color: var(--brand, #FFB347); font-weight: 600;
}

/* ---------- 尊重「减弱动效」 ---------- */
@media (prefers-reduced-motion: reduce) {
    .scp-mask, .scp-sheet { transition-duration: .01ms !important; animation-duration: .01ms !important; }
}
</style>
