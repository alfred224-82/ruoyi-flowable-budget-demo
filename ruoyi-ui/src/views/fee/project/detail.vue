<template>
  <div class="app-container">
    <el-card shadow="never" v-loading="loading">
      <div slot="header">
        <span>项目详情：{{ project.projectName }}</span>
        <el-button style="float: right;" size="mini" @click="back">返回</el-button>
      </div>
      <el-descriptions :column="3" border size="small">
        <el-descriptions-item label="客户">{{ project.clientName }}</el-descriptions-item>
        <el-descriptions-item label="项目经理">{{ project.projectManagerName }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ statusText(project.status) }}</el-descriptions-item>
        <el-descriptions-item label="初始合同额">{{ project.contractAmount }}</el-descriptions-item>
        <el-descriptions-item label="当前合同额">{{ project.currentContractAmount }}</el-descriptions-item>
        <el-descriptions-item label="总预算">{{ project.totalBudget }}</el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ parseTime(project.startDate, '{y}-{m}-{d}') }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ parseTime(project.endDate, '{y}-{m}-{d}') }}</el-descriptions-item>
        <el-descriptions-item label="变更次数">{{ project.changeCount }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 利润拆解（纸面/实际分离） -->
    <el-card shadow="never" style="margin-top: 10px;">
      <div slot="header"><span>利润拆解（纸面利润 = 合同额 - 收入 - 支出；实际利润 = 已核验收入 - 支出 - 折旧分摊 - 分成 - 坏账计提）</span></div>
      <el-row :gutter="12">
        <el-col :span="6"><el-statistic title="纸面利润" :value="profit.paperProfit || 0" /></el-col>
        <el-col :span="6"><el-statistic title="实际利润" :value="profit.actualProfit || 0" /></el-col>
        <el-col :span="6"><el-statistic title="利润率%" :value="profit.profitRate || 0" /></el-col>
        <el-col :span="6"><el-statistic title="总支出" :value="profit.totalExpense || 0" /></el-col>
      </el-row>
      <el-descriptions :column="4" border size="small" style="margin-top: 10px;">
        <el-descriptions-item label="已核验收入">{{ profit.verifiedIncome }}</el-descriptions-item>
        <el-descriptions-item label="待核验收入">{{ profit.pendingIncome }}</el-descriptions-item>
        <el-descriptions-item label="折旧分摊">{{ profit.depreciationShare }}</el-descriptions-item>
        <el-descriptions-item label="项目分成">{{ profit.sharingAmount }}</el-descriptions-item>
        <el-descriptions-item label="坏账计提">{{ profit.badDebtProvision }}</el-descriptions-item>
        <el-descriptions-item label="当前合同额">{{ profit.currentContractAmount }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 项目科目（按大类预算） -->
    <el-card shadow="never" style="margin-top: 10px;">
      <div slot="header"><span>项目预算科目（按大类）</span></div>
      <el-table :data="subjects" border size="small">
        <el-table-column label="费用大类" align="center" prop="categoryName" />
        <el-table-column label="预算科目" align="center" prop="subjectName" />
        <el-table-column label="预算金额" align="right" prop="budgetAmount" />
        <el-table-column label="已使用" align="right" prop="usedAmount" />
        <el-table-column label="状态" align="center" width="90">
          <template slot-scope="scope">
            <el-tag :type="scope.row.isActive === 1 ? 'success' : 'info'" size="small">
              {{ scope.row.isActive === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { getProject, getProjectProfit, listSubjects } from '@/api/system/fee/fundProject'

export default {
  name: 'FundProjectDetail',
  data() {
    return {
      loading: true,
      projectId: null,
      project: {},
      profit: {},
      subjects: []
    }
  },
  created() {
    this.projectId = this.$route.query.id
    this.loadData()
  },
  methods: {
    loadData() {
      this.loading = true
      Promise.all([
        getProject(this.projectId),
        getProjectProfit(this.projectId),
        listSubjects(this.projectId)
      ]).then(([p, pr, s]) => {
        this.project = p.data || {}
        this.profit = pr.data || {}
        this.subjects = s.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    statusText(status) {
      const map = { DRAFT: '草稿', PENDING_APPROVAL: '待审批', ACTIVE: '进行中', COMPLETED: '已完成', CLOSED: '已关闭', LOST: '烂账' }
      return map[status] || status
    },
    back() {
      this.$router.push('/fee/fee/project')
    }
  }
}
</script>
