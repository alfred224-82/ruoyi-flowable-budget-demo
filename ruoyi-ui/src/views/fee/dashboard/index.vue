<template>
  <div class="app-container">
    <!-- 总览 -->
    <el-row :gutter="12">
      <el-col style="width: 14.28%;">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="合同总额" :value="dash.totalContractAmount || 0" />
        </el-card>
      </el-col>
      <el-col style="width: 14.28%;">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="已核验收入" :value="dash.totalVerifiedIncome || 0" />
        </el-card>
      </el-col>
      <el-col style="width: 14.28%;">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="待核验收入" :value="dash.totalPendingIncome || 0" />
        </el-card>
      </el-col>
      <el-col style="width: 14.28%;">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="总支出" :value="dash.totalExpense || 0" />
        </el-card>
      </el-col>
      <el-col style="width: 14.28%;">
        <el-card shadow="hover" class="stat-card profit-card">
          <el-statistic title="实际利润" :value="dash.totalActualProfit || 0" />
        </el-card>
      </el-col>
      <el-col style="width: 14.28%;">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="进行中项目" :value="dash.activeProjectCount || 0" />
        </el-card>
      </el-col>
      <el-col style="width: 14.28%;">
        <el-card shadow="hover" class="stat-card">
          <el-statistic title="待审批事项" :value="dash.pendingApprovalCount || 0" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="12" style="margin-top: 12px;">
      <!-- 大类支出卡片 -->
      <el-col :span="16">
        <el-card shadow="never">
          <div slot="header"><span>按会计大类支出（点击下钻）</span></div>
          <el-empty v-if="!(dash.categoryExpenses && dash.categoryExpenses.length)" description="暂无支出数据" :image-size="60" />
          <el-row v-else :gutter="10">
            <el-col v-for="c in dash.categoryExpenses" :key="c.categoryName" :span="8" style="margin-bottom: 10px;">
              <el-card shadow="hover" class="category-card" @click.native="drill(2, c.categoryName)">
                <div class="category-name">{{ c.categoryName }}</div>
                <div class="category-amount">{{ c.amount }}</div>
                <div class="category-count">{{ c.billCount }} 单</div>
              </el-card>
            </el-col>
          </el-row>
        </el-card>
      </el-col>

      <!-- 固定成本 + 未归类池 -->
      <el-col :span="8">
        <el-card shadow="never">
          <div slot="header"><span>固定成本（{{ dash.accrualMonth || '本月' }}计提）</span></div>
          <div class="fixed-row"><span>薪资社保计提</span><b>{{ dash.monthlySalaryAccrual || 0 }}</b></div>
          <div class="fixed-row"><span>资产折旧计提</span><b>{{ dash.monthlyDepreciation || 0 }}</b></div>
          <div class="fixed-row"><span>贷款利息计提</span><b>{{ dash.monthlyLoanInterest || 0 }}</b></div>
        </el-card>
        <el-card shadow="never" style="margin-top: 12px;">
          <div slot="header"><span>未归类池（未关联项目报销）</span></div>
          <div class="fixed-row"><span>单据数</span><b>{{ dash.unclassifiedBillCount || 0 }}</b></div>
          <div class="fixed-row"><span>金额</span><b>{{ dash.unclassifiedBillAmount || 0 }}</b></div>
          <el-button size="mini" type="text" icon="el-icon-search" @click="drill(1)">下钻查看</el-button>
        </el-card>
      </el-col>
    </el-row>

    <!-- 风险预警区 -->
    <el-card shadow="never" style="margin-top: 12px;">
      <div slot="header"><span>风险预警区</span></div>
      <el-row :gutter="12">
        <el-col :span="12">
          <div class="risk-title">烂账风险</div>
          <el-table :data="dash.badDebtWarnings || []" size="mini" max-height="220">
            <el-table-column label="项目" prop="projectName" min-width="120" show-overflow-tooltip />
            <el-table-column label="建议" prop="proposal" width="90">
              <template slot-scope="scope">{{ proposalText(scope.row.proposal) }}</template>
            </el-table-column>
            <el-table-column label="计提" align="right" prop="provisionAmount" width="100" />
            <el-table-column label="状态" prop="status" width="80">
              <template slot-scope="scope">{{ scope.row.status === 'OPEN' ? '待处理' : '跟进中' }}</template>
            </el-table-column>
          </el-table>
        </el-col>
        <el-col :span="12">
          <div class="risk-title">合同变更待收款确认（含逾期）</div>
          <el-table :data="dash.changeConfirmWarnings || []" size="mini" max-height="220">
            <el-table-column label="项目" prop="projectName" min-width="120" show-overflow-tooltip />
            <el-table-column label="合同额变更量" align="right" prop="contractDelta" width="110" />
            <el-table-column label="状态" prop="changeStatus" width="90">
              <template slot-scope="scope">
                <el-tag :type="scope.row.changeStatus === 'OVERDUE' ? 'danger' : 'warning'" size="mini">{{ scope.row.changeStatus === 'OVERDUE' ? '逾期' : '待确认' }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-col>
      </el-row>
    </el-card>

    <!-- 团队人力分布（专业版 FEAT_MEMBER_LOAD） -->
    <el-card v-if="canMemberLoad" shadow="never" style="margin-top: 12px;">
      <div slot="header"><span>团队人力分布（跨项目投入比例，超限仅提示）</span></div>
      <el-table :data="memberLoads" size="mini" max-height="300">
        <el-table-column label="成员" prop="userName" width="120" />
        <el-table-column label="参与项目数" prop="projectCount" width="100" align="center" />
        <el-table-column label="投入比例合计" width="150" align="center">
          <template slot-scope="scope">
            <span :style="{ color: scope.row.overLoad ? '#F56C6C' : '#606266', fontWeight: scope.row.overLoad ? 'bold' : 'normal' }">{{ scope.row.totalRatio }}%</span>
          </template>
        </el-table-column>
        <el-table-column label="负载状态" width="110" align="center">
          <template slot-scope="scope">
            <el-tag v-if="scope.row.overLoad" type="danger" size="mini">超限</el-tag>
            <el-tag v-else type="success" size="mini">正常</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="参与项目" min-width="260">
          <template slot-scope="scope">
            <el-tag v-for="p in scope.row.projects" :key="p.projectId" size="mini" style="margin-right: 4px; margin-bottom: 2px;">{{ p.projectName }}（{{ p.investRatio }}%）</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 项目卡片 -->
    <el-card shadow="never" style="margin-top: 12px;">
      <div slot="header"><span>项目卡片（按实际利润升序，点击下钻）</span></div>
      <el-table :data="projectCards" size="small">
        <el-table-column label="项目" prop="projectName" min-width="160" show-overflow-tooltip />
        <el-table-column label="当前合同额" align="right" prop="currentContractAmount" width="120" />
        <el-table-column label="已核验收入" align="right" prop="verifiedIncome" width="120" />
        <el-table-column label="总支出" align="right" prop="totalExpense" width="120" />
        <el-table-column label="实际利润" align="right" width="120">
          <template slot-scope="scope">
            <span :style="{ color: actualProfit(scope.row) < 0 ? '#F56C6C' : '#67C23A' }">{{ actualProfit(scope.row) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="预算使用" align="right" width="140">
          <template slot-scope="scope">
            {{ scope.row.usedBudget || 0 }} / {{ scope.row.totalBudget || 0 }}
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="90">
          <template slot-scope="scope">
            <el-button size="mini" type="text" icon="el-icon-search" @click="drill(3, null, scope.row.id)">下钻</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 下钻对话框 -->
    <el-dialog :title="drillTitle" :visible.sync="drillOpen" width="760px" append-to-body>
      <el-table :data="drillRows" size="small" border>
        <el-table-column v-for="col in drillColumns" :key="col" :label="col" :prop="col" min-width="120" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { getDashboard, listProjectCards, drillDown } from '@/api/system/fee/fundDashboard'
import { dashboardMemberLoad } from '@/api/system/fee/fundPro'
import { loadFeatures, featureVisible } from '@/utils/feature'

export default {
  name: 'FundDashboard',
  data() {
    return {
      dash: {},
      projectCards: [],
      canMemberLoad: false,
      memberLoads: [],
      drillOpen: false,
      drillTitle: '',
      drillRows: [],
      drillColumns: []
    }
  },
  created() {
    this.loadDashboard()
    // 专业版开关：团队人力分布卡片按 License 下发
    loadFeatures().then(() => {
      this.canMemberLoad = featureVisible('FEAT_MEMBER_LOAD')
      if (this.canMemberLoad) {
        dashboardMemberLoad().then(res => { this.memberLoads = res.data || [] }).catch(() => {})
      }
    })
  },
  methods: {
    loadDashboard() {
      getDashboard().then(res => { this.dash = res.data || {} })
      listProjectCards().then(res => {
        const list = res.data || []
        list.sort((a, b) => this.actualProfit(a) - this.actualProfit(b))
        this.projectCards = list
      })
    },
    actualProfit(row) {
      return (row.verifiedIncome || 0) - (row.totalExpense || 0)
    },
    proposalText(proposal) {
      const map = { BAD_DEBT: '计提坏账', FOLLOW_UP: '继续跟进', LEGAL: '法律途径' }
      return map[proposal] || proposal
    },
    drill(level, category, projectId) {
      const titles = { 1: '公司级下钻：大类汇总', 2: '大类下钻：项目汇总', 3: '项目下钻：支出明细', 4: '明细下钻' }
      this.drillTitle = titles[level] || '下钻'
      drillDown(level, category, projectId).then(res => {
        const rows = res.data || []
        this.drillRows = rows
        this.drillColumns = rows.length ? Object.keys(rows[0]) : []
        this.drillOpen = true
      })
    }
  }
}
</script>

<style scoped>
.stat-card {
  text-align: center;
}
.profit-card ::v-deep .el-statistic__number {
  color: #67c23a;
  font-weight: bold;
}
.category-card {
  cursor: pointer;
  text-align: center;
}
.category-name {
  font-size: 13px;
  color: #909399;
}
.category-amount {
  font-size: 18px;
  font-weight: bold;
  margin: 6px 0;
}
.category-count {
  font-size: 12px;
  color: #c0c4cc;
}
.fixed-row {
  display: flex;
  justify-content: space-between;
  padding: 6px 0;
  font-size: 13px;
}
.risk-title {
  font-weight: bold;
  font-size: 13px;
  margin-bottom: 8px;
}
</style>
