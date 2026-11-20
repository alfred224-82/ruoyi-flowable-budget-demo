<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="订单编号" prop="orderNo">
        <el-input v-model="queryParams.orderNo" placeholder="请输入订单编号" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="顾客手机" prop="customerPhone">
        <el-input v-model="queryParams.customerPhone" placeholder="请输入顾客手机号" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="收款状态" prop="payStatus">
        <el-select v-model="queryParams.payStatus" placeholder="请选择收款状态" clearable>
          <el-option label="未收款" value="PENDING_PAY" />
          <el-option label="已收款" value="RECEIVED" />
        </el-select>
      </el-form-item>
      <el-form-item label="订单状态" prop="orderStatus">
        <el-select v-model="queryParams.orderStatus" placeholder="请选择订单状态" clearable>
          <el-option label="待服务" value="PENDING_SERVICE" />
          <el-option label="已完成" value="COMPLETED" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="bookingList">
      <el-table-column label="订单编号" align="center" prop="orderNo" width="170" show-overflow-tooltip />
      <el-table-column label="顾客" align="center" min-width="120">
        <template slot-scope="scope">
          <div>{{ scope.row.customerName || '游客' }}</div>
          <div style="color:#909399;font-size:12px;">{{ scope.row.customerPhone }}</div>
        </template>
      </el-table-column>
      <el-table-column label="服务项目" align="center" prop="serviceName" min-width="110" show-overflow-tooltip />
      <el-table-column label="服务员工" align="center" prop="staffName" width="90">
        <template slot-scope="scope"><span>{{ scope.row.staffName || '未分配' }}</span></template>
      </el-table-column>
      <el-table-column label="预约时间" align="center" prop="bookingTime" width="150">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.bookingTime) }}</span></template>
      </el-table-column>
      <el-table-column label="数量" align="center" prop="quantity" width="70" />
      <el-table-column label="金额" align="center" prop="amount" width="90">
        <template slot-scope="scope">
          <span v-if="scope.row.amount != null" style="color:#f56c6c;font-weight:600;">￥{{ scope.row.amount }}</span>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="收款状态" align="center" prop="payStatus" width="90">
        <template slot-scope="scope">
          <el-tag :type="payStatusTag(scope.row.payStatus)" size="small">{{ payStatusText(scope.row.payStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="订单状态" align="center" prop="orderStatus" width="90">
        <template slot-scope="scope">
          <el-tag :type="orderStatusTag(scope.row.orderStatus)" size="small">{{ orderStatusText(scope.row.orderStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="220" fixed="right">
        <!-- ★活动合并行（sourceType=ACTIVITY）为 promotion_activity 视图，订单级操作仅对顾客预约单生效 -->
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="goDetail(scope.row)" v-hasPermi="['micro:order:detail']">详情</el-button>
          <el-button size="mini" type="text" icon="el-icon-money" @click="handleReceive(scope.row)" v-if="scope.row.payStatus === 'PENDING_PAY' && scope.row.sourceType !== 'ACTIVITY'" v-hasPermi="['micro:booking:edit']">标记已收款</el-button>
          <el-button size="mini" type="text" icon="el-icon-finished" @click="handleComplete(scope.row)" v-if="scope.row.orderStatus === 'PENDING_SERVICE' && scope.row.sourceType !== 'ACTIVITY'" v-hasPermi="['micro:booking:complete']">完成服务</el-button>
          <el-button size="mini" type="text" icon="el-icon-close" @click="handleCancel(scope.row)" v-if="scope.row.orderStatus === 'PENDING_SERVICE' && scope.row.sourceType !== 'ACTIVITY'" v-hasPermi="['micro:booking:cancel']">取消</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />


  </div>
</template>

<script>
import { listBooking, cancelBooking, completeBooking, receiveBooking } from '@/api/micro/booking'

export default {
  name: 'MicroBooking',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      bookingList: [],
      queryParams: { pageNum: 1, pageSize: 10, orderNo: null, customerPhone: null, payStatus: null, orderStatus: null }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listBooking(this.queryParams).then(response => {
        this.bookingList = response.rows
        this.total = response.total
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
      return { PENDING_SERVICE: '待服务', COMPLETED: '已完成', CANCELLED: '已取消' }[v] || v
    },
    orderStatusTag(v) {
      return { PENDING_SERVICE: 'primary', COMPLETED: 'success', CANCELLED: 'info' }[v] || 'info'
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    // ★标记已收款：线下收款后点此，后端自动写 cash_flow 收入流水（不调支付接口）
    handleReceive(row) {
      this.$modal.confirm('确认已收到订单「' + row.orderNo + '」的到店付款 ￥' + row.amount + '？将自动记一笔收入流水。').then(() => {
        return receiveBooking(row.id)
      }).then(() => {
        this.$modal.msgSuccess('已标记收款并记入收入流水')
        this.getList()
      }).catch(() => {})
    },
    handleComplete(row) {
      this.$modal.confirm('确认订单「' + row.orderNo + '」服务已完成？').then(() => {
        return completeBooking(row.id)
      }).then(() => {
        this.$modal.msgSuccess('服务已完成')
        this.getList()
      }).catch(() => {})
    },
    handleCancel(row) {
      this.$modal.confirm('确认取消订单「' + row.orderNo + '」？').then(() => {
        return cancelBooking(row.id)
      }).then(() => {
        this.$modal.msgSuccess('已取消')
        this.getList()
      }).catch(() => {})
    },
    goDetail(row) {
      this.$router.push({ path: '/micro/booking/detail', query: { id: row.id }})
    }
  }
}
</script>
