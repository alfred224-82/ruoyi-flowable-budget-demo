<template>
  <view class="page">
    <!-- ═══ 已选服务 ═══ -->
    <view class="service-card">
      <view class="service-header">
        <text class="service-header-title">已选服务</text>
      </view>
      <view class="service-body">
        <view class="service-info">
          <text class="service-name">{{ serviceName || '未选择' }}</text>
          <text class="service-price">{{ unitPrice }}元/小时</text>
        </view>
        <text class="service-edit" @click="goBack">修改</text>
      </view>
    </view>

    <!-- ═══ 选择日期 ═══ -->
    <view class="card">
      <text class="card-title">选择日期</text>
      <scroll-view scroll-x class="date-scroll" :show-scrollbar="false">
        <view class="date-list">
          <view v-for="(d, i) in dateList" :key="i"
            :class="['date-item', selectedDateIndex === i ? 'active' : '']"
            @click="selectDate(i)">
            <text class="date-mmdd">{{ d.mmdd }}</text>
            <text class="date-label" :class="{ active: selectedDateIndex === i }">{{ d.label }}</text>
          </view>
        </view>
      </scroll-view>
    </view>

    <!-- ═══ 选择时间段 ══ -->
    <view class="card">
      <text class="card-title">选择时间段</text>
      <text v-if="selectedDateIndex === 0" class="card-subtitle">今天最早可选 {{ minStartHour }}:00（需晚于当前时间1小时）</text>
      <view class="slot-grid">
        <view v-for="slot in timeSlots" :key="slot"
          :class="['slot-btn', selectedSlots.includes(slot) ? 'active' : '', isSlotDisabled(slot) ? 'disabled' : '']"
          @click="toggleSlot(slot)">
          {{ slot }}
        </view>
      </view>
    </view>

    <!-- ══ 选择时长 ═══ -->
    <view class="card">
      <view class="counter-row">
        <text class="counter-label">选择时长</text>
        <text class="counter-hint">每小时 {{ unitPrice }}元</text>
        <view class="counter-controls">
          <view class="counter-btn minus" :class="{ disabled: duration <= 1 }" @click="changeDuration(-1)">
            <text class="counter-btn-text">−</text>
          </view>
          <text class="counter-value">{{ duration }} 小时</text>
          <view class="counter-btn plus" @click="changeDuration(1)">
            <text class="counter-btn-text">+</text>
          </view>
        </view>
      </view>
    </view>

    <!-- ═══ 选择人数 ═══ -->
    <view class="card">
      <view class="counter-row">
        <text class="counter-label">选择人数</text>
        <text class="counter-hint">建议房间人数 {{ capacityRange }}</text>
        <view class="counter-controls">
          <view class="counter-btn minus" :class="{ disabled: peopleCount <= 1 }" @click="changePeople(-1)">
            <text class="counter-btn-text">−</text>
          </view>
          <text class="counter-value">{{ peopleCount }} 人</text>
          <view class="counter-btn plus" @click="changePeople(1)">
            <text class="counter-btn-text">+</text>
          </view>
        </view>
      </view>
    </view>

    <!-- ═══ 费用明细 ═══ -->
    <view class="card cost-card">
      <text class="card-title">费用明细</text>
      <view v-if="bookingType === 'project'" class="cost-row">
        <text class="cost-label">基础费用</text>
        <text class="cost-value">{{ unitPrice }}元 × {{ duration }}小时</text>
      </view>
      <view class="cost-row">
        <text class="cost-label">房间费用</text>
        <text class="cost-value">{{ roomCost }}元</text>
      </view>
      <view class="cost-row">
        <text class="cost-label">{{ staffRoleName }}费用</text>
        <text class="cost-value">{{ staffCost }}元</text>
      </view>
      <view class="cost-divider"></view>
      <view class="cost-total">
        <text class="cost-total-label">合计：</text>
        <text class="cost-total-value">{{ totalPrice }}元</text>
      </view>
    </view>

    <!-- ★ 底部操作栏 -->
    <view class="bottom-bar">
      <text class="bar-cancel" @click="goBack">可取消预约</text>
      <view class="bar-confirm" @click="onConfirm">立即确认</view>
    </view>
  </view>
</template>

<script>
import cfg from '@/config/index.js'
import request from '@/utils/request'
import { getRoleDisplayName } from '@/utils/roleDisplay'

export default {
  data() {
    return {
      // 从 selfBooking 传入的服务信息
      serviceName: '',
      serviceId: null,
      unitPrice: 0,
      bookingType: 'project',   // project / staff / room
      staffIds: [],
      roomIds: [],
      staffHourlyRate: 0,
      roomHourlyRate: 0,
      capacityMin: 2,
      capacityMax: 6,

      // 日期
      dateList: [],
      selectedDateIndex: 1,     // 默认明天

      // 时间段（整点，每个 = 1小时）
      timeSlots: ['10:00', '11:00', '12:00', '13:00', '14:00', '15:00', '16:00', '17:00', '18:00', '19:00', '20:00'],
      selectedSlots: [],

      // 时长 & 人数
      duration: 2,
      peopleCount: 4,
      // ★行业角色显示名（salesman 映射：助教/导游/司机，未配置回退员工）
      staffRoleName: '员工'
    }
  },
  computed: {
    capacityRange() {
      return this.capacityMin + '-' + this.capacityMax + '人'
    },
    // ★今天最早可选开始时间（小时）= 当前小时 + 1（如当前17点→最早18点）
    minStartHour() {
      return new Date().getHours() + 1
    },
    baseCost() {
      // 房间/助教类型的主资源单价已计入房间/助教费用，避免重复计算
      return this.bookingType === 'project' ? this.unitPrice * this.duration : 0
    },
    roomCost() {
      return this.roomHourlyRate * this.duration
    },
    staffCost() {
      return this.staffHourlyRate * this.duration
    },
    totalPrice() {
      return this.baseCost + this.roomCost + this.staffCost
    }
  },
  onLoad(options) {
    getRoleDisplayName('salesman', '员工').then(n => { this.staffRoleName = n })
    // 接收 selfBooking 传来的参数
    this.serviceName = decodeURIComponent(options.serviceName || '')
    // ★雪花ID（助教 user_id 等）超过 JS 安全整数精度，Number() 会丢精度导致后端收到不存在的ID（明细回退"已删除"/0元），全程保持字符串
    this.serviceId = options.serviceId || null
    this.unitPrice = Number(options.unitPrice || 0)
    this.bookingType = options.bookingType || 'project'
    this.staffIds = options.staffIds ? options.staffIds.split(',').filter(Boolean) : []
    this.roomIds = options.roomIds ? options.roomIds.split(',').filter(Boolean) : []
    this.staffHourlyRate = Number(options.staffHourlyRate || 0)
    this.roomHourlyRate = Number(options.roomHourlyRate || 0)
    this.capacityMin = Number(options.capacityMin || 2)
    this.capacityMax = Number(options.capacityMax || 6)

    this.buildDateList()
    // 默认选中 2 个时段（若默认日期为今天，需避开已过期时段）
    this.selectedSlots = ['13:00', '14:00']
    this.sanitizeSlots()
  },
  methods: {
    buildDateList() {
      const days = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
      const now = new Date()
      this.dateList = []
      for (let i = 0; i < 5; i++) {
        const d = new Date(now.getTime() + i * 86400000)
        const mm = String(d.getMonth() + 1).padStart(2, '0')
        const dd = String(d.getDate()).padStart(2, '0')
        let label = days[d.getDay()]
        if (i === 0) label = '今天'
        else if (i === 1) label = '明天'
        this.dateList.push({ mmdd: mm + '/' + dd, label })
      }
    },
    selectDate(index) {
      this.selectedDateIndex = index
      // 切换到今天时，清除已过期的选中时段
      this.sanitizeSlots()
    },
    // ★判断时段是否已过（仅当天限制）：开始时间必须晚于当前时间1小时
    isSlotDisabled(slot) {
      if (this.selectedDateIndex !== 0) return false
      const hour = parseInt(slot.split(':')[0], 10)
      return hour < this.minStartHour
    },
    // ★清除已过期选中时段，并在全部过期时自动选择最早可用时段
    sanitizeSlots() {
      if (this.selectedDateIndex !== 0) return
      const valid = this.selectedSlots.filter(s => !this.isSlotDisabled(s))
      if (valid.length > 0) {
        this.selectedSlots = valid
      } else {
        const firstAvailable = this.timeSlots.find(s => !this.isSlotDisabled(s))
        this.selectedSlots = firstAvailable ? [firstAvailable] : []
      }
      this.duration = Math.max(1, this.selectedSlots.length)
    },
    toggleSlot(slot) {
      if (this.isSlotDisabled(slot)) {
        uni.showToast({ title: '该时段已过，请选择更晚时间', icon: 'none' })
        return
      }
      const i = this.selectedSlots.indexOf(slot)
      if (i >= 0) {
        this.selectedSlots.splice(i, 1)
      } else {
        this.selectedSlots.push(slot)
        this.selectedSlots.sort()
      }
      // 同步时长
      this.duration = Math.max(1, this.selectedSlots.length)
    },
    changeDuration(delta) {
      const next = this.duration + delta
      if (next < 1 || next > this.timeSlots.length) return
      this.duration = next
      // 同步时段：保持第一个选中的时段，扩展/缩减数量
      // 没有选中时段时，从第一个可用时段开始（今天需避开已过期时段）
      if (this.selectedSlots.length === 0) {
        const startIdx = this.timeSlots.findIndex(s => !this.isSlotDisabled(s))
        if (startIdx < 0) return
        this.selectedSlots = this.timeSlots.slice(startIdx, startIdx + next)
      } else {
        const first = this.selectedSlots[0]
        const firstIdx = this.timeSlots.indexOf(first)
        this.selectedSlots = this.timeSlots.slice(firstIdx, firstIdx + next)
      }
    },
    changePeople(delta) {
      const next = this.peopleCount + delta
      if (next < 1) return
      this.peopleCount = next
    },
    goBack() {
      uni.navigateBack()
    },
    async onConfirm() {
      if (!this.serviceId) {
        uni.showToast({ title: '请选择服务', icon: 'none' })
        return
      }
      if (this.selectedSlots.length === 0) {
        uni.showToast({ title: '请选择时间段', icon: 'none' })
        return
      }
      // ★今天预约：开始时段必须晚于当前时间1小时
      if (this.selectedSlots.some(s => this.isSlotDisabled(s))) {
        uni.showToast({ title: '所选时段已过，请重新选择', icon: 'none' })
        return
      }
      const d = this.dateList[this.selectedDateIndex]
      const year = new Date().getFullYear()
      const bookingDate = year + '-' + d.mmdd.replace('/', '-')
      // ★时段结束时间 = 最后一个选中时段的下一整点（如选 18:00,19:00 → 18:00-20:00）
      const lastHour = parseInt(this.selectedSlots[this.selectedSlots.length - 1].split(':')[0], 10) + 1
      const timeSlot = this.selectedSlots[0] + '-' + String(lastHour).padStart(2, '0') + ':00'

      // 构建提交数据（主资源与关联资源已由选择页解析传入）
      const data = {
        bookingType: this.bookingType,
        mainResourceId: this.serviceId,
        optionalProjectIds: [],
        optionalStaffIds: this.staffIds,
        optionalRoomIds: this.roomIds,
        bookingDate: bookingDate,
        bookingTimeSlot: timeSlot,
        quantity: this.duration,
        peopleCount: this.peopleCount
      }

      try {
        await request({ url: cfg.api.microSelfBookingSubmit, method: 'POST', data })
        uni.showToast({ title: '预约成功', icon: 'success' })
        setTimeout(() => {
          uni.navigateBack({ delta: 2 })
        }, 1500)
      } catch (e) {
        // 后端已统一提示
      }
    }
  }
}
</script>

<style scoped>
.page {
  padding-bottom: 140rpx;
  background: #f0f2f5;
  min-height: 100vh;
}

/* ═══ 已选服务卡片 ═══ */
.service-card {
  margin: 20rpx 24rpx 0;
  border-radius: 16rpx;
  overflow: hidden;
  background: #fff;
}
.service-header {
  background: linear-gradient(135deg, #2b7de9 0%, #4a9af5 100%);
  padding: 24rpx 28rpx;
}
.service-header-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #fff;
}
.service-body {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 28rpx;
}
.service-info {
  display: flex;
  align-items: baseline;
  flex: 1;
}
.service-name {
  font-size: 32rpx;
  font-weight: bold;
  color: #1a1a1a;
  margin-right: 16rpx;
}
.service-price {
  font-size: 28rpx;
  color: #606266;
}
.service-edit {
  font-size: 28rpx;
  color: #2b7de9;
  flex-shrink: 0;
}

/* ══ 通用卡片 ═══ */
.card {
  margin: 20rpx 24rpx 0;
  background: #fff;
  border-radius: 16rpx;
  padding: 28rpx;
}
.card-title {
  font-size: 32rpx;
  font-weight: bold;
  color: #1a1a1a;
  display: block;
  margin-bottom: 24rpx;
}
.card-subtitle {
  font-size: 24rpx;
  color: #e6a23c;
  display: block;
  margin: -12rpx 0 20rpx;
}

/* ─ 日期滚动 ── */
.date-scroll {
  white-space: nowrap;
}
.date-list {
  display: flex;
  gap: 20rpx;
}
.date-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 120rpx;
  padding: 16rpx 0;
  border-bottom: 4rpx solid transparent;
}
.date-item.active {
  border-bottom-color: #2b7de9;
}
.date-mmdd {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a1a;
}
.date-item.active .date-mmdd {
  color: #2b7de9;
}
.date-label {
  font-size: 24rpx;
  color: #909399;
  margin-top: 8rpx;
}
.date-label.active {
  color: #2b7de9;
}

/* ── 时段网格 ── */
.slot-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;
}
.slot-btn {
  width: calc((100% - 64rpx) / 5);
  text-align: center;
  padding: 18rpx 0;
  border: 2rpx solid #dcdfe6;
  border-radius: 12rpx;
  font-size: 28rpx;
  color: #606266;
  box-sizing: border-box;
}
.slot-btn.active {
  border-color: #2b7de9;
  color: #2b7de9;
  background: #f0f6ff;
  font-weight: 600;
}
.slot-btn.disabled {
  color: #c0c4cc;
  background: #f5f5f5;
  border-color: #ebeef5;
  text-decoration: line-through;
}
.slot-btn:active {
  transform: scale(0.95);
}

/* ── 计数器行 ── */
.counter-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12rpx;
}
.counter-label {
  font-size: 32rpx;
  font-weight: bold;
  color: #1a1a1a;
  margin-right: 8rpx;
}
.counter-hint {
  font-size: 26rpx;
  color: #909399;
  flex: 1;
}
.counter-controls {
  display: flex;
  align-items: center;
  gap: 16rpx;
}
.counter-btn {
  width: 56rpx;
  height: 56rpx;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  border: 2rpx solid #dcdfe6;
  background: #fff;
}
.counter-btn.plus {
  background: #2b7de9;
  border-color: #2b7de9;
}
.counter-btn.plus .counter-btn-text {
  color: #fff;
  font-size: 32rpx;
  font-weight: bold;
}
.counter-btn.minus .counter-btn-text {
  color: #606266;
  font-size: 32rpx;
  font-weight: bold;
}
.counter-btn.disabled {
  opacity: 0.4;
}
.counter-btn:active:not(.disabled) {
  transform: scale(0.9);
}
.counter-value {
  font-size: 32rpx;
  font-weight: 600;
  color: #1a1a1a;
  min-width: 120rpx;
  text-align: center;
}

/* ═══ 费用明细 ═══ */
.cost-card .cost-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}
.cost-card .cost-row:last-of-type {
  border-bottom: none;
}
.cost-label {
  font-size: 28rpx;
  color: #303133;
}
.cost-value {
  font-size: 28rpx;
  color: #1a1a1a;
}
.cost-divider {
  height: 1rpx;
  background: #f0f0f0;
  margin: 8rpx 0;
}
.cost-total {
  display: flex;
  justify-content: flex-end;
  align-items: baseline;
  padding-top: 16rpx;
}
.cost-total-label {
  font-size: 30rpx;
  color: #303133;
}
.cost-total-value {
  font-size: 40rpx;
  font-weight: bold;
  color: #2b7de9;
}

/* ★ 底部操作栏 */
.bottom-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20rpx 30rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  box-shadow: 0 -2rpx 12rpx rgba(0, 0, 0, 0.06);
  z-index: 10;
}
.bar-cancel {
  font-size: 28rpx;
  color: #909399;
}
.bar-cancel:active {
  color: #606266;
}
.bar-confirm {
  background: #2b7de9;
  color: #fff;
  font-size: 32rpx;
  font-weight: 600;
  padding: 22rpx 60rpx;
  border-radius: 44rpx;
}
.bar-confirm:active {
  opacity: 0.85;
}
</style>
