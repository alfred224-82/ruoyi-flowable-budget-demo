<template>
  <div class="app-container">
    <el-page-header @back="goBack" content="预约订单详情" style="margin-bottom:16px;" />

    <el-card v-loading="loading" shadow="never">
      <div slot="header" class="clearfix">
        <span style="font-weight:600;">订单 {{ detail.orderNo }}</span>
        <el-tag :type="payStatusTag(detail.payStatus)" size="small" style="margin-left:10px;">{{ payStatusText(detail.payStatus) }}</el-tag>
        <el-tag :type="orderStatusTag(detail.orderStatus)" size="small" style="margin-left:6px;">{{ orderStatusText(detail.orderStatus) }}</el-tag>
        <div style="float:right;">
          <el-button type="success" size="mini" icon="el-icon-money" v-if="detail.payStatus === 'PENDING_PAY' && detail.sourceType !== 'ACTIVITY'" @click="handleReceive" v-hasPermi="['micro:booking:edit']">标记已收款</el-button>
          <el-button type="primary" size="mini" icon="el-icon-finished" v-if="detail.orderStatus === 'PENDING_SERVICE' && detail.sourceType !== 'ACTIVITY'" @click="handleComplete" v-hasPermi="['micro:booking:complete']">完成服务</el-button>
          <el-button type="danger" size="mini" icon="el-icon-close" v-if="detail.orderStatus === 'PENDING_SERVICE' && detail.sourceType !== 'ACTIVITY'" @click="handleCancel" v-hasPermi="['micro:booking:cancel']">取消预约</el-button>
        </div>
      </div>

      <el-descriptions :column="2" border size="medium">
        <el-descriptions-item label="订单编号">{{ detail.orderNo }}</el-descriptions-item>
        <el-descriptions-item label="订单金额">
          <span v-if="detail.amount != null" style="color:#f56c6c;font-weight:600;">￥{{ detail.amount }}</span>
          <span v-else>—</span>
        </el-descriptions-item>
        <el-descriptions-item label="顾客昵称">{{ detail.customerName || '游客' }}</el-descriptions-item>
        <el-descriptions-item label="顾客手机">{{ detail.customerPhone }}</el-descriptions-item>
        <el-descriptions-item label="服务项目">{{ detail.serviceName }}</el-descriptions-item>
        <el-descriptions-item label="服务员工">{{ detail.staffName || '未分配' }}</el-descriptions-item>
        <el-descriptions-item label="预约到店时间">{{ parseTime(detail.bookingTime) }}</el-descriptions-item>
        <el-descriptions-item label="数量/时长">{{ detail.quantity != null ? detail.quantity : '—' }}</el-descriptions-item>
        <el-descriptions-item label="收款时间">{{ detail.payTime ? parseTime(detail.payTime) : '—' }}</el-descriptions-item>
        <el-descriptions-item label="完成时间">{{ detail.completeTime ? parseTime(detail.completeTime) : '—' }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ parseTime(detail.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '—' }}</el-descriptions-item>
      </el-descriptions>

      <el-alert style="margin-top:16px;" type="info" :closable="false" show-icon title="★V1 零支付合规：标记「已收款」仅记录线下到账并自动生成收入流水，不对接任何在线支付/转账接口。" />
    </el-card>
  </div>
</template>

<script>
import { getBooking, cancelBooking, completeBooking, receiveBooking } from '@/api/micro/booking'

export default {
  name: 'MicroOrderDetail',
  data() {
    return {
      loading: false,
      id: null,
      detail: {}
    }
  },
  created() {
    this.id = this.$route.query.id
    if (this.id) {
      this.getDetail()
    }
  },
  methods: {
    getDetail() {
      this.loading = true
      getBooking(this.id).then(response => {
        this.detail = response.data || {}
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    payStatusText(v) {
      return { PENDING_PAY: '未收款', RECEIVED: '已收款' }[v] || v || '未收款'
    },
    payStatusTag(v) {
      return { PENDING_PAY: 'warning', RECEIVED: 'success' }[v] || 'warning'
    },
    orderStatusText(v) {
      return { PENDING_SERVICE: '待服务', COMPLETED: '已完成', CANCELLED: '已取消' }[v] || v || '—'
    },
    orderStatusTag(v) {
      return { PENDING_SERVICE: 'primary', COMPLETED: 'success', CANCELLED: 'info' }[v] || 'info'
    },
    handleReceive() {
      this.$modal.confirm('确认已收到订单「' + this.detail.orderNo + '」的到店付款 ￥' + this.detail.amount + '？将自动记一笔收入流水。').then(() => {
        return receiveBooking(this.id)
      }).then(() => {
        this.$modal.msgSuccess('已标记收款并记入收入流水')
        this.getDetail()
      }).catch(() => {})
    },
    handleComplete() {
      this.$modal.confirm('确认订单「' + this.detail.orderNo + '」服务已完成？').then(() => {
        return completeBooking(this.id)
      }).then(() => {
        this.$modal.msgSuccess('服务已完成')
        this.getDetail()
      }).catch(() => {})
    },
    handleCancel() {
      this.$modal.confirm('确认取消订单「' + this.detail.orderNo + '」？').then(() => {
        return cancelBooking(this.id)
      }).then(() => {
        this.$modal.msgSuccess('已取消')
        this.getDetail()
      }).catch(() => {})
    },
    goBack() {
      this.$router.push({ path: '/microOps/bookingList' })
    }
  }
}
</script>
