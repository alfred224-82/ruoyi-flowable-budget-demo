<template>
  <div class="app-container">
    <el-alert title="业务员奖励记录" type="info" description="渠道=业务员：拉新现金/首单提成/消费返佣逐笔明细，标记已线下发放状态。" show-icon :closable="false" class="mb8" />

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="80px">
      <el-form-item label="推广人" prop="promoterUserId">
        <el-input v-model="queryParams.promoterUserId" placeholder="用户ID" clearable />
      </el-form-item>
      <el-form-item label="发放状态" prop="issueStatus">
        <el-select v-model="queryParams.issueStatus" placeholder="全部" clearable>
          <el-option label="待发放" value="PENDING" />
          <el-option label="已线下发放" value="PAID_OFFLINE" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="recordList">
      <el-table-column label="推广人" align="center" prop="promoterUserName" width="120" />
      <el-table-column label="顾客" align="center" prop="customerName" width="120" />
      <el-table-column label="奖励类型" align="center" prop="rewardType" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.rewardType === 'cash' ? 'warning' : 'success'" size="mini">
            {{ scope.row.rewardType === 'cash' ? '现金' : '优惠券' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="金额" align="right" prop="rewardAmount" width="100">
        <template slot-scope="scope">¥ {{ scope.row.rewardAmount || '-' }}</template>
      </el-table-column>
      <el-table-column label="触发规则" align="center" prop="triggerRule" width="120" />
      <el-table-column label="发放状态" align="center" prop="issueStatus" width="120">
        <template slot-scope="scope">
          <el-tag :type="scope.row.issueStatus === 'PAID_OFFLINE' ? 'success' : 'warning'" size="mini">
            {{ scope.row.issueStatus === 'PAID_OFFLINE' ? '已线下发放' : '待发放' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" width="130">
        <template slot-scope="scope">
          <el-button v-if="scope.row.issueStatus === 'PENDING' && scope.row.rewardType === 'cash'"
            size="mini" type="text" @click="handleMarkPaid(scope.row)">标记已发放</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { listRewardRecords, markPaidOffline } from '@/api/micro/distribution'

export default {
  name: 'SalesmanRecords',
  data() {
    return {
      loading: true,
      total: 0,
      recordList: [],
      queryParams: { pageNum: 1, pageSize: 10, rewardChannel: 'salesman', promoterUserId: null, issueStatus: null }
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      listRewardRecords(this.queryParams).then(res => {
        this.recordList = res.rows
        this.total = res.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm('queryForm'); this.handleQuery() },
    handleMarkPaid(row) {
      this.$modal.confirm('确认标记此笔现金奖励已线下发放吗？')
        .then(() => markPaidOffline(row.id))
        .then(() => { this.$modal.msgSuccess('标记成功'); this.getList() })
    }
  }
}
</script>
