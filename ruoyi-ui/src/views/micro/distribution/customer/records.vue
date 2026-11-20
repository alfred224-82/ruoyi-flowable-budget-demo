<template>
  <div class="app-container">
    <el-alert title="顾客奖励记录" type="info" description="渠道=顾客：拉新优惠券/分享有礼/裂变奖励逐笔明细，含券面额/有效期/核销状态。" show-icon :closable="false" class="mb8" />

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="80px">
      <el-form-item label="推广人" prop="promoterUserId">
        <el-input v-model="queryParams.promoterUserId" placeholder="用户ID" clearable />
      </el-form-item>
      <el-form-item label="发放状态" prop="issueStatus">
        <el-select v-model="queryParams.issueStatus" placeholder="全部" clearable>
          <el-option label="待发放" value="PENDING" />
          <el-option label="已发放" value="ISSUED" />
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
          <el-tag type="success" size="mini">{{ scope.row.rewardType === 'coupon' ? '优惠券' : '现金' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="优惠券名称" align="center" prop="couponName" min-width="120" show-overflow-tooltip />
      <el-table-column label="触发规则" align="center" prop="triggerRule" width="120" />
      <el-table-column label="发放状态" align="center" prop="issueStatus" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.issueStatus === 'ISSUED' ? 'success' : 'warning'" size="mini">
            {{ scope.row.issueStatus === 'ISSUED' ? '已发放' : '待发放' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { listRewardRecords } from '@/api/micro/distribution'

export default {
  name: 'CustomerRecords',
  data() {
    return {
      loading: true,
      total: 0,
      recordList: [],
      queryParams: { pageNum: 1, pageSize: 10, rewardChannel: 'customer', promoterUserId: null, issueStatus: null }
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
    resetQuery() { this.resetForm('queryForm'); this.handleQuery() }
  }
}
</script>
