<template>
  <div class="app-container">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>盈利分析表</span>
      </div>

      <!-- 筛选条件 -->
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="100px">
        <el-form-item label="组织机构" prop="orgId">
          <el-tree-select
            v-model="queryParams.orgId"
            :data="deptTree"
            :props="{ label: 'label', value: 'id', children: 'children' }"
            placeholder="请选择组织机构（不选则查全部）"
            clearable
            check-strictly
            style="width: 250px"
          />
        </el-form-item>
        <el-form-item label="年度" prop="year">
          <el-date-picker
            v-model="queryParams.year"
            type="year"
            placeholder="选择年度"
            value-format="yyyy"
            style="width: 120px"
          />
        </el-form-item>
        <el-form-item label="月份" prop="month">
          <el-select v-model="queryParams.month" placeholder="全年汇总" clearable style="width: 120px">
            <el-option
              v-for="m in 12"
              :key="m"
              :label="m + '月'"
              :value="m"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">查询</el-button>
          <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 视图切换 -->
      <el-row class="mb8">
        <el-radio-group v-model="viewType" size="small" @change="onViewChange">
          <el-radio-button label="detail">明细视图</el-radio-button>
          <el-radio-button label="summary">汇总视图（按会计6大科目）</el-radio-button>
        </el-radio-group>
      </el-row>

      <!-- 明细视图表格 -->
      <el-table
        v-if="viewType === 'detail'"
        v-loading="loading"
        :data="detailData"
        border
        show-summary
        :summary-method="getDetailSummary"
      >
        <el-table-column label="责任中心" align="center" prop="costCenterName" width="150" />
        <el-table-column label="预算科目" align="center" prop="budgetSubjectName" />
        <el-table-column label="会计大类" align="center" prop="accountingClass" width="120">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.accountingClass === 'EXPENSE'" type="warning">费用类</el-tag>
            <el-tag v-else-if="scope.row.accountingClass === 'COST'" type="success">成本类</el-tag>
            <el-tag v-else-if="scope.row.accountingClass === 'ASSET'" type="primary">资产类</el-tag>
            <el-tag v-else>{{ scope.row.accountingClass }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预算金额" align="right" prop="budgetAmount">
          <template slot-scope="scope">
            ¥{{ Number(scope.row.budgetAmount || 0).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="实际金额" align="right" prop="actualAmount">
          <template slot-scope="scope">
            ¥{{ Number(scope.row.actualAmount || 0).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="执行差异" align="right" prop="varianceAmount">
          <template slot-scope="scope">
            <span :style="{ color: getVarianceColor(scope.row.varianceAmount) }">
              ¥{{ Number(scope.row.varianceAmount || 0).toFixed(2) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="差异率" align="right" width="120">
          <template slot-scope="scope">
            <span :style="{ color: getVarianceColor(scope.row.varianceAmount) }">
              {{ getVarianceRate(scope.row) }}
            </span>
          </template>
        </el-table-column>
      </el-table>

      <!-- 汇总视图表格 -->
      <el-table
        v-if="viewType === 'summary'"
        v-loading="loading"
        :data="summaryData"
        border
        show-summary
        :summary-method="getSummarySummary"
      >
        <el-table-column label="会计大类" align="center" prop="accountingClass">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.accountingClass === 'ASSET'" type="primary">资产类</el-tag>
            <el-tag v-else-if="scope.row.accountingClass === 'LIABILITY'" type="danger">负债类</el-tag>
            <el-tag v-else-if="scope.row.accountingClass === 'EQUITY'" type="success">所有者权益类</el-tag>
            <el-tag v-else-if="scope.row.accountingClass === 'INCOME'" type="warning">收入类</el-tag>
            <el-tag v-else-if="scope.row.accountingClass === 'COST'" type="info">成本类</el-tag>
            <el-tag v-else-if="scope.row.accountingClass === 'EXPENSE'" type="warning">费用类</el-tag>
            <el-tag v-else>{{ scope.row.accountingClass }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预算金额" align="right" prop="budgetAmount">
          <template slot-scope="scope">
            ¥{{ Number(scope.row.budgetAmount || 0).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="实际金额" align="right" prop="actualAmount">
          <template slot-scope="scope">
            ¥{{ Number(scope.row.actualAmount || 0).toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column label="执行差异" align="right" prop="varianceAmount">
          <template slot-scope="scope">
            <span :style="{ color: getVarianceColor(scope.row.varianceAmount) }">
              ¥{{ Number(scope.row.varianceAmount || 0).toFixed(2) }}
            </span>
          </template>
        </el-table-column>
        <el-table-column label="差异率" align="right" width="120">
          <template slot-scope="scope">
            <span :style="{ color: getVarianceColor(scope.row.varianceAmount) }">
              {{ getVarianceRate(scope.row) }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { profitAnalysisDetail, profitAnalysisSummary } from "@/api/system/fee/report";
import { treeselect } from "@/api/system/dept";

export default {
  name: "ProfitAnalysis",
  data() {
    return {
      loading: false,
      viewType: 'detail',
      deptTree: [],
      detailData: [],
      summaryData: [],
      queryParams: {
        orgId: undefined,
        year: new Date().getFullYear().toString(),
        month: undefined
      }
    };
  },
  created() {
    this.loadDeptTree();
    this.handleQuery();
  },
  methods: {
    loadDeptTree() {
      treeselect().then(response => {
        this.deptTree = response.data || [];
      });
    },
    handleQuery() {
      if (!this.queryParams.year) {
        this.$message.warning('请选择年度');
        return;
      }
      this.loadData();
    },
    resetQuery() {
      this.queryParams = {
        orgId: undefined,
        year: new Date().getFullYear().toString(),
        month: undefined
      };
      this.handleQuery();
    },
    onViewChange() {
      this.loadData();
    },
    loadData() {
      this.loading = true;
      const params = {
        orgId: this.queryParams.orgId,
        year: parseInt(this.queryParams.year),
        month: this.queryParams.month ? parseInt(this.queryParams.month) : undefined
      };

      if (this.viewType === 'detail') {
        profitAnalysisDetail(params).then(response => {
          this.detailData = response.data || [];
          this.loading = false;
        }).catch(() => {
          this.loading = false;
        });
      } else {
        profitAnalysisSummary(params).then(response => {
          this.summaryData = response.data || [];
          this.loading = false;
        }).catch(() => {
          this.loading = false;
        });
      }
    },
    getVarianceColor(varianceAmount) {
      const amount = Number(varianceAmount || 0);
      if (amount > 0) return '#67c23a'; // 绿色，预算有结余
      if (amount < 0) return '#f56c6c'; // 红色，超预算
      return '#909399'; // 灰色，持平
    },
    getVarianceRate(row) {
      const budget = Number(row.budgetAmount || 0);
      const variance = Number(row.varianceAmount || 0);
      if (budget === 0) return '-';
      const rate = ((variance / budget) * 100).toFixed(2);
      return rate + '%';
    },
    getDetailSummary({ columns, data }) {
      const sums = [];
      columns.forEach((column, index) => {
        if (index === 0) {
          sums[index] = '合计';
          return;
        }
        if (['budgetAmount', 'actualAmount', 'varianceAmount'].includes(column.property)) {
          const values = data.map(item => Number(item[column.property] || 0));
          const total = values.reduce((prev, curr) => prev + curr, 0);
          sums[index] = '¥' + total.toFixed(2);
        } else {
          sums[index] = '';
        }
      });
      return sums;
    },
    getSummarySummary({ columns, data }) {
      return this.getDetailSummary({ columns, data });
    }
  }
};
</script>

<style scoped>
.box-card {
  margin: 20px;
}
.mb8 {
  margin-bottom: 8px;
}
</style>
