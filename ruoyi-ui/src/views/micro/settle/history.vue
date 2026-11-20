<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="结算员工" prop="staffUserId">
        <el-select v-model="queryParams.staffUserId" placeholder="请选择员工" clearable filterable style="width:200px;">
          <el-option v-for="s in staffOptions" :key="s.userId" :label="staffLabel(s)" :value="s.userId" />
        </el-select>
      </el-form-item>
      <el-form-item label="发放状态" prop="payStatus">
        <el-select v-model="queryParams.payStatus" placeholder="请选择发放状态" clearable style="width:160px;">
          <el-option label="待发放" value="PENDING" />
          <el-option label="已线下发放" value="PAID_OFFLINE" />
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

    <el-table v-loading="loading" :data="historyList">
      <el-table-column label="结算单号" align="center" prop="settleNo" width="170" show-overflow-tooltip />
      <el-table-column label="员工" align="center" prop="staffName" width="100" />
      <el-table-column label="结算周期" align="center" width="200">
        <template slot-scope="scope">{{ scope.row.periodStart }} ~ {{ scope.row.periodEnd }}</template>
      </el-table-column>
      <el-table-column label="完成单量" align="center" prop="totalOrders" width="90" />
      <el-table-column label="应发金额" align="center" prop="totalAmount" width="110">
        <template slot-scope="scope"><span style="color:#f56c6c;font-weight:600;">￥{{ scope.row.totalAmount }}</span></template>
      </el-table-column>
      <el-table-column label="发放状态" align="center" prop="payStatus" width="110">
        <template slot-scope="scope">
          <el-tag :type="scope.row.payStatus === 'PAID_OFFLINE' ? 'success' : 'warning'" size="small">{{ scope.row.payStatus === 'PAID_OFFLINE' ? '已线下发放' : '待发放' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标记人" align="center" prop="markedBy" width="90">
        <template slot-scope="scope"><span>{{ scope.row.markedBy || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="标记时间" align="center" prop="markedTime" width="150">
        <template slot-scope="scope"><span>{{ scope.row.markedTime ? parseTime(scope.row.markedTime) : '—' }}</span></template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="90" fixed="right">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-view" @click="handleDetail(scope.row)" v-hasPermi="['micro:settle:detail']">明细</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 结算明细 -->
    <el-dialog title="结算明细" :visible.sync="open" width="900px" append-to-body>
      <el-descriptions :column="1" border size="medium">
        <el-descriptions-item label="结算单号">{{ detail.settleNo }}</el-descriptions-item>
        <el-descriptions-item label="结算员工">{{ detail.staffName }}</el-descriptions-item>
        <el-descriptions-item label="结算周期">{{ detail.periodStart }} ~ {{ detail.periodEnd }}</el-descriptions-item>
        <el-descriptions-item label="完成单量">{{ detail.totalOrders }} 单</el-descriptions-item>
        <el-descriptions-item label="应发金额"><span style="color:#f56c6c;font-weight:600;">￥{{ detail.totalAmount }}</span></el-descriptions-item>
        <el-descriptions-item label="发放状态">{{ detail.payStatus === 'PAID_OFFLINE' ? '已线下发放' : '待发放' }}</el-descriptions-item>
        <el-descriptions-item label="标记人">{{ detail.markedBy || '—' }}</el-descriptions-item>
        <el-descriptions-item label="标记时间">{{ detail.markedTime ? parseTime(detail.markedTime) : '—' }}</el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '—' }}</el-descriptions-item>
      </el-descriptions>

      <el-divider content-position="left">关联订单明细</el-divider>
      <el-table v-loading="orderLoading" :data="orderList" border size="small" max-height="360">
        <el-table-column label="订单编号" align="center" prop="orderNo" width="160" show-overflow-tooltip />
        <el-table-column label="顾客" align="center" width="120">
          <template slot-scope="scope">
            <span>{{ scope.row.customerName || '游客' }}</span>
            <span v-if="scope.row.customerPhone" style="color:#909399;font-size:12px;">（{{ scope.row.customerPhone }}）</span>
          </template>
        </el-table-column>
        <el-table-column label="服务活动" align="center" prop="serviceName" min-width="120" show-overflow-tooltip />
        <el-table-column label="时长" align="center" width="80">
          <template slot-scope="scope">{{ scope.row.hours ? scope.row.hours + '小时' : '—' }}</template>
        </el-table-column>
        <el-table-column :label="staffRoleName + '费用'" align="center" width="100">
          <template slot-scope="scope">
            <span v-if="scope.row.staffFee != null" style="color:#f56c6c;">￥{{ scope.row.staffFee }}</span>
            <span v-else style="color:#909399;">—</span>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" align="center" width="140">
          <template slot-scope="scope">{{ scope.row.startTime ? parseTime(scope.row.startTime) : '—' }}</template>
        </el-table-column>
        <el-table-column label="结束时间" align="center" width="140">
          <template slot-scope="scope">{{ scope.row.endTime ? parseTime(scope.row.endTime) : '—' }}</template>
        </el-table-column>
      </el-table>

      <div slot="footer" class="dialog-footer">
        <el-button @click="open = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { historySettle, getSettle, listSettleOrders } from '@/api/micro/settle'
import { listStaff } from '@/api/micro/userRole'
import { getRoleDisplayName } from '@/utils/roleDisplay'

export default {
  name: 'MicroSettleHistory',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      historyList: [],
      staffOptions: [],
      open: false,
      detail: {},
      // 员工角色显示名：优先租户角色映射（如助教/导游），无映射回退通用名「员工」
      staffRoleName: '员工',
      orderLoading: false,
      orderList: [],
      queryParams: { pageNum: 1, pageSize: 10, staffUserId: null, payStatus: null }
    }
  },
  created() {
    this.getList()
    this.getStaffOptions()
    getRoleDisplayName('salesman', '员工').then(n => { this.staffRoleName = n })
  },
  methods: {
    getList() {
      this.loading = true
      historySettle(this.queryParams).then(response => {
        this.historyList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    getStaffOptions() {
      listStaff({ roleCode: 'salesman', status: '0', pageNum: 1, pageSize: 200 }).then(response => {
        this.staffOptions = response.rows || []
      })
    },
    staffLabel(s) {
      const name = s.nickName || s.userName || '员工'
      return s.phonenumber ? name + '（' + s.phonenumber + '）' : name
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleDetail(row) {
      getSettle(row.id).then(response => {
        this.detail = response.data || {}
        this.open = true
        // 加载关联订单明细
        this.orderLoading = true
        this.orderList = []
        listSettleOrders(row.id).then(res => {
          this.orderList = res.data || []
          this.orderLoading = false
        }).catch(() => { this.orderLoading = false })
      })
    }
  }
}
</script>
