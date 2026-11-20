<template>
  <view class="page">
    <!-- ★ 顶部说明 -->
    <view class="intro">
      <text class="intro-title">选择房间、{{ staffRoleName }}或组合预约</text>
      <view class="intro-tip">可只预约房间，只预约{{ staffRoleName }}，也可选择房间后添加多位{{ staffRoleName }}</view>
    </view>

    <!-- ★ 空闲房间（REQ-01：场地未开放时整块隐藏） -->
    <view class="section" v-if="showRoom">
      <view class="section-header">
        <view class="section-title-wrap">
          <view class="door-icon"></view>
          <text class="section-title">空闲房间</text>
          <text v-if="roomRequired" class="req-star">*</text>
        </view>
        <view class="section-link" @click="onLinkRooms">
          可预约<text class="link-arrow">›</text>
        </view>
      </view>
      <view v-if="!roomsLoaded" class="loading">加载中…</view>
      <view v-else-if="!roomList.length" class="empty">暂无空闲房间</view>
      <view v-else class="room-grid">
        <view v-for="room in roomList" :key="room.id"
          :class="['room-card', selectedRoomId === room.id ? 'selected' : '']"
          @click="toggleRoom(room.id)">
          <view class="room-img-wrap">
            <image v-if="room.coverUrl" :src="toHttps(room.coverUrl)" class="room-img" mode="aspectFill" />
            <view v-else class="room-img-placeholder"></view>
            <view v-if="selectedRoomId === room.id" class="check-badge">
              <text class="check-badge-icon">✓</text>
            </view>
          </view>
          <text class="room-name">{{ room.name }}</text>
          <text class="room-cap">{{ room.capacity ? room.capacity + '人' : (room.description || '') }}</text>
          <view class="room-foot">
            <view v-if="selectedRoomId === room.id" class="tag-idle">当前空闲</view>
            <view v-else class="btn-select">选择</view>
          </view>
        </view>
      </view>
    </view>

    <!-- ★ 空闲{{ staffRoleName }}（REQ-01：员工未开放时整块隐藏） -->
    <view class="section" v-if="showStaff">
      <view class="section-header">
        <view class="section-title-wrap">
          <text class="section-title">空闲{{ staffRoleName }}</text>
          <text v-if="staffRequired" class="req-star">*</text>
        </view>
        <view class="section-link" @click="onLinkStaff">
          可预约<text class="link-arrow">›</text>
        </view>
      </view>
      <view v-if="!staffLoaded" class="loading">加载中…</view>
      <view v-else-if="!staffList.length" class="empty">暂无空闲{{ staffRoleName }}</view>
      <view v-else class="staff-list">
        <view v-for="staff in staffList" :key="staff.id"
          :class="['staff-row', selectedStaffIds.includes(staff.id) ? 'selected' : '']"
          @click="toggleStaff(staff.id)">
          <image v-if="staff.avatar" :src="avatarSize(staff.avatar, 'sm')" class="staff-avatar" mode="aspectFill" @error="staff.avatar = ''" />
          <view v-else class="staff-avatar-placeholder">
            <text class="staff-avatar-text">{{ (staff.name || '?')[0] }}</text>
          </view>
          <view class="staff-info">
            <text class="staff-name">{{ staff.name }}</text>
            <view class="staff-tag">{{ staffRoleName }}</view>
            <view class="staff-status">
              <text class="staff-status-icon">◷</text>
              <text class="staff-status-text">当前空闲</text>
            </view>
          </view>
          <view :class="['staff-btn', selectedStaffIds.includes(staff.id) ? 'staff-btn-on' : '']">
            {{ selectedStaffIds.includes(staff.id) ? '已选' : '选择' }}
          </view>
        </view>
      </view>
    </view>

    <!-- ★ 底部操作栏 -->
    <view class="bottom-bar">
      <view class="summary">
        <text class="summary-label">已选：</text>
        <text v-if="!hasSelection" class="summary-empty">暂未选择</text>
        <view v-else class="summary-content">
          <text v-if="selectedRoom" class="summary-room">{{ selectedRoom.name }}</text>
          <text v-if="selectedStaffIds.length" class="summary-plus">+</text>
          <text v-if="selectedStaffIds.length" class="summary-staff-count">{{ selectedStaffIds.length }}</text>
          <text v-if="selectedStaffIds.length" class="summary-staff">位{{ staffRoleName }}</text>
        </view>
      </view>
      <view class="btn-confirm" :class="{ disabled: !hasSelection }" @click="onConfirm">确认预约</view>
    </view>
  </view>
</template>

<script>
import cfg from '@/config/index.js'
import request from '@/utils/request'
import { toHttps, avatarSize } from '@/utils/format'
import { getRoleDisplayName } from '@/utils/roleDisplay'
import { getBookingResourcePolicy } from '@/utils/bookingPolicy'

export default {
  data() {
    const now = new Date()
    const fmt = d => `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')}`
    return {
      today: fmt(now),
      staffList: [],
      staffLoaded: false,
      roomList: [],
      roomsLoaded: false,
      // 房间单选 + 助教多选（组合预约：1 房间 + N 助教）
      selectedRoomId: null,
      selectedStaffIds: [],
      // ★行业角色显示名（salesman 映射：助教/导游/司机，未配置回退员工）
      staffRoleName: '员工',
      // ★REQ-01 三类资源控制策略（getBookingResourcePolicy 下发；默认全开放/非必填）
      policy: {
        customerSelfBooking: true,
        projectBookingEnabled: 1, projectBookingRequired: 0,
        roomBookingEnabled: 1, roomBookingRequired: 0,
        staffBookingEnabled: 1, staffBookingRequired: 0,
        industryCode: null
      }
    }
  },
  computed: {
    hasSelection() {
      return !!this.selectedRoomId || this.selectedStaffIds.length > 0
    },
    selectedRoom() {
      return this.roomList.find(r => r.id === this.selectedRoomId) || null
    },
    // REQ-01：场地/员工是否对客户开放（enabled=0 隐藏入口）
    showRoom() { return this.policy.roomBookingEnabled !== 0 },
    showStaff() { return this.policy.staffBookingEnabled !== 0 },
    // REQ-01：场地/员工是否必填（required=1 提交前强制校验）
    roomRequired() { return this.policy.roomBookingRequired === 1 },
    staffRequired() { return this.policy.staffBookingRequired === 1 }
  },
  onLoad() {
    getRoleDisplayName('salesman', '员工').then(n => { this.staffRoleName = n })
    this.guardAndLoad(true)
  },
  onShow() {
    // 返回本页时刷新资源状态，并清理已不可约的选中项
    if (this.roomsLoaded || this.staffLoaded) this.guardAndLoad(false)
  },
  methods: {
    toHttps,
    avatarSize,
    /** ★预约策略闸门：租户未开启顾客自助预约时提示并返回；否则按 policy 按需加载已开放资源 */
    guardAndLoad(first) {
      getBookingResourcePolicy().then(p => {
        this.policy = p
        if (!p.customerSelfBooking) {
          uni.showToast({ title: '本店未开启自助预约', icon: 'none' })
          setTimeout(() => uni.navigateBack(), 800)
          return
        }
        // REQ-01：仅加载对客户开放的资源；未开放的区块整块隐藏
        if (this.showRoom && (first || this.roomsLoaded)) this.loadRooms()
        if (this.showStaff && (first || this.staffLoaded)) this.loadStaff()
      })
    },
    toggleRoom(id) {
      this.selectedRoomId = this.selectedRoomId === id ? null : id
    },
    toggleStaff(id) {
      const i = this.selectedStaffIds.indexOf(id)
      if (i >= 0) this.selectedStaffIds.splice(i, 1)
      else this.selectedStaffIds.push(id)
    },
    onLinkRooms() {
      uni.showToast({ title: `共${this.roomList.length}间空闲房间可预约`, icon: 'none' })
    },
    onLinkStaff() {
      uni.showToast({ title: `共${this.staffList.length}位${this.staffRoleName}可预约`, icon: 'none' })
    },
    onConfirm() {
      // ★REQ-01 必填资源提交前校验（后端 submitSelfBooking 为最终防线）
      if (this.roomRequired && !this.selectedRoomId) {
        uni.showToast({ title: '请选择房间', icon: 'none' })
        return
      }
      if (this.staffRequired && !this.selectedStaffIds.length) {
        uni.showToast({ title: `请选择${this.staffRoleName}`, icon: 'none' })
        return
      }
      if (!this.hasSelection) {
        uni.showToast({ title: `请先选择房间或${this.staffRoleName}`, icon: 'none' })
        return
      }
      const staffs = this.staffList.filter(s => this.selectedStaffIds.includes(s.id))
      const rateOf = s => Number(s.hourlyRate || 0)
      const staffRateSum = staffs.reduce((sum, s) => sum + rateOf(s), 0)
      let params
      if (this.selectedRoom) {
        // 房间为主资源，所选助教全部作为关联助教
        const room = this.selectedRoom
        params = {
          bookingType: 'room',
          serviceId: room.id,
          serviceName: encodeURIComponent(room.name),
          unitPrice: room.price || 0,
          staffIds: staffs.map(s => s.id).join(','),
          roomIds: '',
          staffHourlyRate: staffRateSum,
          roomHourlyRate: room.price || 0,
          capacityMin: room.capacity ? Math.max(2, Math.floor(room.capacity * 0.5)) : 2,
          capacityMax: room.capacity || 6
        }
      } else {
        // 仅助教：首位为主资源，其余为关联助教
        const main = staffs[0]
        params = {
          bookingType: 'staff',
          serviceId: main.id,
          serviceName: encodeURIComponent(main.name),
          unitPrice: rateOf(main),
          staffIds: staffs.slice(1).map(s => s.id).join(','),
          roomIds: '',
          staffHourlyRate: staffRateSum,
          roomHourlyRate: 0,
          capacityMin: 2,
          capacityMax: 6
        }
      }
      const query = Object.entries(params).map(([k, v]) => k + '=' + v).join('&')
      uni.navigateTo({ url: '/pages/micro/selfBooking/confirm?' + query })
    },
    loadRooms() {
      this.roomsLoaded = false
      request({ url: cfg.api.microSelfBookingRooms, method: 'GET',
        data: { bookingDate: this.today }
      }).then(res => {
        this.roomList = res.data || []
        // 清理已不在空闲列表中的选中房间
        if (this.selectedRoomId && !this.roomList.some(r => r.id === this.selectedRoomId)) {
          this.selectedRoomId = null
        }
      }).finally(() => { this.roomsLoaded = true })
    },
    loadStaff() {
      this.staffLoaded = false
      request({ url: cfg.api.microSelfBookingStaff, method: 'GET',
        data: { bookingDate: this.today }
      }).then(res => {
        this.staffList = res.data || []
        // 清理已不在空闲列表中的选中助教
        this.selectedStaffIds = this.selectedStaffIds.filter(id => this.staffList.some(s => s.id === id))
      }).finally(() => { this.staffLoaded = true })
    }
  }
}
</script>

<style scoped>
.page { padding: 0 24rpx 160rpx; background: #f5f6f8; min-height: 100vh; box-sizing: border-box; }

/* ★ 顶部说明 */
.intro { padding: 28rpx 0 8rpx; }
.intro-title { display: block; text-align: center; font-size: 30rpx; color: #606266; }
.intro-tip {
  margin-top: 24rpx; background: #fff; border-radius: 12rpx;
  padding: 24rpx 20rpx; text-align: center;
  font-size: 28rpx; color: #303133;
}

/* ★ 分区 */
.section { margin-top: 40rpx; }
.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 24rpx; }
.section-title-wrap { display: flex; align-items: center; }
.section-title { font-size: 36rpx; font-weight: bold; color: #1a1a1a; }
.req-star { font-size: 32rpx; color: #f56c6c; margin-left: 6rpx; font-weight: bold; }
.section-link { font-size: 28rpx; color: #2b7de9; font-weight: 600; }
.link-arrow { font-size: 28rpx; color: #2b7de9; margin-left: 4rpx; }

/* 门形小图标 */
.door-icon { width: 26rpx; height: 34rpx; background: #7ba4f0; border-radius: 6rpx; margin-right: 12rpx; position: relative; }
.door-icon::after {
  content: ''; position: absolute; right: 5rpx; top: 14rpx;
  width: 4rpx; height: 8rpx; background: #fff; border-radius: 2rpx;
}

/* ── 房间网格 ── */
.room-grid { display: flex; flex-wrap: wrap; gap: 16rpx; }
.room-card {
  width: calc((100% - 32rpx) / 3); flex-shrink: 0;
  background: #fff; border-radius: 16rpx; padding: 16rpx;
  box-sizing: border-box; border: 3rpx solid transparent;
}
.room-card.selected { border-color: #2b7de9; }
.room-card:active { transform: scale(0.97); }
.room-img-wrap { position: relative; }
.room-img { width: 100%; height: 160rpx; border-radius: 10rpx; display: block; }
.room-img-placeholder { width: 100%; height: 160rpx; border-radius: 10rpx; background: linear-gradient(135deg, #e8f0fe, #f0f6ff); }
.check-badge {
  position: absolute; right: -8rpx; bottom: -8rpx;
  width: 40rpx; height: 40rpx; border-radius: 50%;
  background: #2b7de9; border: 3rpx solid #fff;
  display: flex; align-items: center; justify-content: center;
}
.check-badge-icon { color: #fff; font-size: 24rpx; font-weight: bold; }
.room-name { display: block; font-size: 32rpx; font-weight: bold; color: #1a1a1a; margin-top: 20rpx;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.room-cap { display: block; font-size: 28rpx; color: #909399; margin-top: 8rpx;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.room-foot { display: flex; justify-content: center; margin-top: 20rpx; }
.tag-idle {
  padding: 12rpx 32rpx; border-radius: 32rpx;
  background: #e5f6ef; color: #3eb589; font-size: 26rpx; font-weight: 600;
}
.btn-select {
  padding: 12rpx 40rpx; border-radius: 32rpx;
  border: 2rpx solid #2b7de9; color: #2b7de9; font-size: 26rpx; font-weight: 600;
}

/* ── 助教列表 ── */
.staff-list { display: flex; flex-direction: column; gap: 20rpx; }
.staff-row {
  display: flex; align-items: center; background: #fff; border-radius: 16rpx;
  padding: 28rpx 24rpx; border: 3rpx solid transparent;
}
.staff-row.selected { border-color: #2b7de9; background: #f6faff; }
.staff-row:active { transform: scale(0.98); }
.staff-avatar { width: 120rpx; height: 120rpx; border-radius: 50%; flex-shrink: 0; }
.staff-avatar-placeholder {
  width: 120rpx; height: 120rpx; border-radius: 50%; background: #2b7de9;
  display: flex; align-items: center; justify-content: center; flex-shrink: 0;
}
.staff-avatar-text { color: #fff; font-size: 44rpx; }
.staff-info { flex: 1; margin-left: 24rpx; min-width: 0; }
.staff-name { display: block; font-size: 34rpx; font-weight: bold; color: #1a1a1a;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.staff-tag {
  display: inline-block; margin-top: 10rpx; padding: 4rpx 18rpx;
  background: #a5a1e0; border-radius: 8rpx; color: #fff; font-size: 24rpx;
}
.staff-status { display: flex; align-items: center; margin-top: 12rpx; }
.staff-status-icon { font-size: 26rpx; color: #3eb589; margin-right: 6rpx; }
.staff-status-text { font-size: 26rpx; color: #3eb589; }
.staff-btn {
  flex-shrink: 0; padding: 14rpx 44rpx; border-radius: 36rpx;
  border: 2rpx solid #2b7de9; color: #2b7de9; font-size: 28rpx; font-weight: 600;
}
.staff-btn-on { background: #2b7de9; color: #fff; }

/* ★ 底部操作栏 */
.bottom-bar {
  position: fixed; bottom: 0; left: 0; right: 0;
  display: flex; align-items: center; justify-content: space-between;
  padding: 20rpx 30rpx; padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff; box-shadow: 0 -2rpx 12rpx rgba(0,0,0,0.06); z-index: 10;
}
.summary { flex: 1; display: flex; align-items: center; min-width: 0; margin-right: 20rpx; }
.summary-label { font-size: 28rpx; color: #606266; flex-shrink: 0; }
.summary-empty { font-size: 28rpx; color: #c0c4cc; }
.summary-content { display: flex; align-items: center; min-width: 0; }
.summary-room { font-size: 30rpx; font-weight: bold; color: #1a1a1a;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.summary-plus { font-size: 30rpx; color: #2b7de9; font-weight: bold; margin: 0 8rpx; }
.summary-staff-count { font-size: 30rpx; color: #2b7de9; font-weight: bold; }
.summary-staff { font-size: 30rpx; font-weight: bold; color: #1a1a1a; }
.btn-confirm {
  flex-shrink: 0; background: #2b7de9; color: #fff;
  font-size: 32rpx; font-weight: 600; padding: 24rpx 64rpx; border-radius: 44rpx;
}
.btn-confirm.disabled { background: #c0c4cc; }
.btn-confirm:active:not(.disabled) { opacity: 0.85; }

/* ★ 通用 */
.loading { text-align: center; color: #909399; padding: 60rpx 0; font-size: 28rpx; }
.empty { text-align: center; color: #c0c4cc; padding: 60rpx 0; font-size: 28rpx; }
</style>
