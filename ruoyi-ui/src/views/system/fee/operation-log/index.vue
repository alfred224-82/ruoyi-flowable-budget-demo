<template>
  <div class="app-container">
    <el-card>
      <div slot="header"><span>费用操作日志</span></div>

      <!-- 查询条件 -->
      <el-form :model="queryParams" :inline="true" label-width="80px" style="margin-bottom: 15px;">
        <el-form-item label="单据编号">
          <el-input v-model="queryParams.billNo" placeholder="请输入单据编号" clearable @keyup.enter.native="handleQuery" />
        </el-form-item>
        <el-form-item label="操作类型">
          <el-select v-model="queryParams.logType" placeholder="全部" clearable>
            <el-option v-for="item in logTypeOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="业务模块">
          <el-select v-model="queryParams.bizModule" placeholder="全部" clearable>
            <el-option label="报销单" value="EXPENSE_BILL" />
            <el-option label="超预算审批" value="OVER_BUDGET" />
            <el-option label="预留预算" value="RESERVED_BUDGET" />
            <el-option label="预算调整" value="BUDGET_ADJUSTMENT" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-date-picker v-model="dateRange" type="daterange" range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期"
            value-format="yyyy-MM-dd" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
          <el-button icon="el-icon-refresh" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 日志列表 -->
      <el-table :data="list" v-loading="loading" border>
        <el-table-column label="ID" prop="id" width="60" />
        <el-table-column label="单据编号" prop="billNo" width="160" />
        <el-table-column label="业务模块" prop="bizModule" width="120">
          <template slot-scope="scope">
            {{ bizModuleMap[scope.row.bizModule] || scope.row.bizModule }}
          </template>
        </el-table-column>
        <el-table-column label="操作类型" prop="logType" width="140">
          <template slot-scope="scope">
            <el-tag size="small" :type="logTypeTagType(scope.row.logType)">
              {{ logTypeLabel(scope.row.logType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作内容" prop="content" show-overflow-tooltip />
        <el-table-column label="操作人" prop="operatorName" width="100" />
        <el-table-column label="操作时间" prop="createTime" width="160" />
      </el-table>

      <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
    </el-card>
  </div>
</template>

<script>
import { listOperationLog } from "@/api/system/fee/operationLog";

export default {
  name: "FeeOperationLog",
  data() {
    return {
      list: [],
      total: 0,
      loading: false,
      dateRange: [],
      queryParams: {
        pageNum: 1,
        pageSize: 20,
        billNo: '',
        logType: '',
        bizModule: ''
      },
      logTypeOptions: [
        { value: 'SUBMIT', label: '提交' },
        { value: 'APPROVE', label: '审批通过' },
        { value: 'REJECT', label: '驳回' },
        { value: 'ADJUST', label: '调剂' },
        { value: 'BA_APPLY', label: '调整申请' },
        { value: 'BA_ALLOCATE', label: '分摊' },
        { value: 'BA_ESCALATE', label: '转审' }
      ],
      bizModuleMap: {
        'EXPENSE_BILL': '报销单',
        'OVER_BUDGET': '超预算审批',
        'RESERVED_BUDGET': '预留预算',
        'BUDGET_ADJUSTMENT': '预算调整'
      }
    }
  },
  created() { this.getList() },
  methods: {
    async getList() {
      this.loading = true;
      try {
        const params = { ...this.queryParams };
        if (this.dateRange && this.dateRange.length === 2) {
          params.beginTime = this.dateRange[0];
          params.endTime = this.dateRange[1];
        }
        const res = await listOperationLog(params);
        this.list = res.rows || [];
        this.total = res.total || 0;
      } finally { this.loading = false }
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    handleReset() {
      this.queryParams = { pageNum: 1, pageSize: 20, billNo: '', logType: '', bizModule: '' };
      this.dateRange = [];
      this.getList();
    },
    logTypeTagType(logType) {
      const map = {
        'SUBMIT': 'primary', 'APPROVE': 'success', 'REJECT': 'danger',
        'ADJUST': 'warning', 'BA_APPLY': 'info', 'BA_ALLOCATE': 'success', 'BA_ESCALATE': 'warning'
      };
      return map[logType] || 'info';
    },
    logTypeLabel(logType) {
      const opt = this.logTypeOptions.find(o => o.value === logType);
      return opt ? opt.label : logType;
    }
  }
}
</script>
