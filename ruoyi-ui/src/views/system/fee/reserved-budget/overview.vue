<template>
  <div class="app-container">
    <el-card v-if="overview">
      <div slot="header"><span>预留预算总览</span></div>
      <el-descriptions :column="3" border>
        <el-descriptions-item label="预算期间">{{ overview.budgetPeriod }}</el-descriptions-item>
        <el-descriptions-item label="层级">{{ overview.levelType }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ overview.deptName || '总公司' }}</el-descriptions-item>
        <el-descriptions-item label="总额度">¥{{ overview.totalAmount }}</el-descriptions-item>
        <el-descriptions-item label="已分摊">¥{{ overview.allocatedAmount }}</el-descriptions-item>
        <el-descriptions-item label="剩余额度">¥{{ (overview.totalAmount - overview.allocatedAmount).toFixed(2) }}</el-descriptions-item>
      </el-descriptions>
      <div style="margin-top: 20px;">
        <span>使用比例：</span>
        <el-progress :percentage="usagePercent" :color="usagePercent > 80 ? '#f56c6c' : '#67c23a'" :stroke-width="20" />
      </div>
    </el-card>
    <el-empty v-else description="当前期间无预留预算数据" />
  </div>
</template>

<script>
import { getReservedBudgetOverview } from "@/api/system/fee/reservedBudget";
export default {
  name: "ReservedBudgetOverview",
  data() { return { overview: null } },
  computed: {
    usagePercent() {
      if (!this.overview || !this.overview.totalAmount) return 0;
      return Math.round((this.overview.allocatedAmount / this.overview.totalAmount) * 100);
    }
  },
  created() {
    const now = new Date();
    const period = now.getFullYear() + '-' + String(now.getMonth() + 1).padStart(2, '0');
    this.loadOverview(period);
  },
  methods: {
    async loadOverview(period) {
      try {
        const res = await getReservedBudgetOverview(period);
        this.overview = res.data;
      } catch (e) { this.overview = null }
    }
  }
}
</script>
