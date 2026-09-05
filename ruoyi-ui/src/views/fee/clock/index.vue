<template>
  <div class="app-container">
    <el-form :inline="true" size="small">
      <el-form-item label="报销单ID">
        <el-input v-model="queryParams.billId" placeholder="请输入报销单ID" clearable style="width: 160px;" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="打卡用户ID">
        <el-input v-model="queryParams.userId" placeholder="请输入用户ID" clearable style="width: 160px;" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="recordList" border size="small" v-loading="loading">
      <el-table-column label="报销单ID" width="110" align="center" prop="billId" />
      <el-table-column label="打卡用户" width="100" align="center" prop="userId" />
      <el-table-column label="触发规则" width="180" align="center">
        <template slot-scope="scope">
          <el-tag size="small" v-for="code in String(scope.row.triggerRule || '').split(',')" :key="code" style="margin-right: 4px;">{{ triggerText(code) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="定位地址" min-width="200" prop="address" :show-overflow-tooltip="true" />
      <el-table-column label="经度" width="110" align="center" prop="longitude" />
      <el-table-column label="纬度" width="110" align="center" prop="latitude" />
      <el-table-column label="精度(米)" width="90" align="center" prop="accuracy" />
      <el-table-column label="打卡时间" width="150" align="center">
        <template slot-scope="scope">{{ parseTime(scope.row.clockTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</template>
      </el-table-column>
      <el-table-column label="定位存疑" width="90" align="center">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.isSuspicious === 1" type="danger" size="small">存疑</el-tag>
          <el-tag v-else type="success" size="small">正常</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="证据等级" width="90" align="center">
        <template slot-scope="scope">
          <el-tag :type="evidenceTagType(scope.row.evidenceLevel)" size="small">{{ scope.row.evidenceLevel || '-' }}</el-tag>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="loadList" />
  </div>
</template>

<script>
import { listClockRecord } from '@/api/system/fee/fundClock'

export default {
  name: 'FeeClockRecord',
  data() {
    return {
      loading: false,
      recordList: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10, billId: null, userId: null }
    }
  },
  created() {
    this.loadList()
  },
  methods: {
    triggerText(code) {
      const map = { TRAVEL_CATEGORY: '差旅招待', AMOUNT_THRESHOLD: '金额阈值', HISTORY_ABNORMAL: '历史异常' }
      return map[code] || code
    },
    evidenceTagType(level) {
      const map = { strong: 'success', medium: 'warning', weak: 'danger' }
      return map[level] || 'info'
    },
    loadList() {
      this.loading = true
      listClockRecord(this.queryParams).then(res => {
        this.recordList = res.rows || []
        this.total = res.total || 0
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.loadList()
    },
    resetQuery() {
      this.queryParams = { pageNum: 1, pageSize: 10, billId: null, userId: null }
      this.loadList()
    }
  }
}
</script>
