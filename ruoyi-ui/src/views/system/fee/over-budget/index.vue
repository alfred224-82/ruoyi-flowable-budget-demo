<template>
  <div class="app-container">
    <el-table :data="list" v-loading="loading" border>
      <el-table-column label="审批单ID" prop="id" width="80" />
      <el-table-column label="报销单号" prop="billNo" width="150" />
      <el-table-column label="预算科目" prop="budgetSubject" />
      <el-table-column label="原预算" prop="originalBudget" width="100" />
      <el-table-column label="已使用" prop="usedAmount" width="100" />
      <el-table-column label="申请金额" prop="requestAmount" width="100" />
      <el-table-column label="缺口金额" prop="shortageAmount" width="100" />
      <el-table-column label="状态" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 'PENDING' ? 'warning' : scope.row.status === 'APPROVED' ? 'success' : 'danger'">
            {{ scope.row.status }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template slot-scope="scope">
          <el-button size="mini" type="text" @click="$router.push('/system/fee/over-budget/detail/' + scope.row.id)">详情</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { pendingOverBudgetList } from "@/api/system/fee/overBudget";
export default {
  name: "OverBudgetIndex",
  data() {
    return { list: [], total: 0, loading: false, queryParams: { pageNum: 1, pageSize: 10 } }
  },
  created() { this.getList() },
  methods: {
    async getList() {
      this.loading = true;
      try {
        const res = await pendingOverBudgetList(this.queryParams);
        this.list = res.rows || [];
        this.total = res.total || 0;
      } finally { this.loading = false }
    }
  }
}
</script>
