<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="项目" prop="projectId">
        <el-select v-model="queryParams.projectId" placeholder="全部项目" clearable filterable>
          <el-option v-for="p in activeProjects" :key="p.id" :label="p.projectName" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="changeStatus">
        <el-select v-model="queryParams.changeStatus" placeholder="全部状态" clearable>
          <el-option label="待审批" value="PENDING" />
          <el-option label="待收款确认" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
          <el-option label="已确认" value="CONFIRMED" />
          <el-option label="逾期" value="OVERDUE" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['fee:change:add']">发起变更</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="changeList">
      <el-table-column label="项目" align="left" prop="projectName" min-width="150" show-overflow-tooltip />
      <el-table-column label="变更类型" align="center" prop="changeType" width="110">
        <template slot-scope="scope">{{ typeText(scope.row.changeType) }}</template>
      </el-table-column>
      <el-table-column label="合同额变更量" align="right" prop="contractDelta" width="120" />
      <el-table-column label="新合同额" align="right" prop="newContractAmount" width="120" />
      <el-table-column label="变更原因" align="left" prop="changeReason" min-width="160" show-overflow-tooltip />
      <el-table-column label="状态" align="center" prop="changeStatus" width="110">
        <template slot-scope="scope">
          <el-tag :type="statusTag(scope.row.changeStatus)" size="small">{{ statusText(scope.row.changeStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180">
        <template slot-scope="scope">
          <template v-if="scope.row.changeStatus === 'PENDING'">
            <el-button size="mini" type="text" icon="el-icon-check" @click="handleApprove(scope.row)" v-hasPermi="['fee:change:approve']">审批</el-button>
            <el-button size="mini" type="text" icon="el-icon-close" @click="handleReject(scope.row)" v-hasPermi="['fee:change:approve']">驳回</el-button>
          </template>
          <template v-if="scope.row.changeStatus === 'APPROVED' || scope.row.changeStatus === 'OVERDUE'">
            <el-button size="mini" type="text" icon="el-icon-money" @click="handleConfirm(scope.row)" v-hasPermi="['fee:change:confirm']">收款确认</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 发起变更 -->
    <el-dialog title="发起合同变更（增量叠加，基线不回溯）" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="所属项目" prop="projectId">
          <el-select v-model="form.projectId" placeholder="请选择项目" filterable style="width: 100%;">
            <el-option v-for="p in activeProjects" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="合同额变更量" prop="contractDelta">
          <el-input-number v-model="form.contractDelta" :precision="2" style="width: 100%;" placeholder="正数=增加，负数=减少，0=不变" />
        </el-form-item>
        <el-form-item label="预算变更">
          <el-row :gutter="10" class="mb8">
            <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="addBudgetDelta">追加大类</el-button>
          </el-row>
          <el-table :data="budgetDeltaRows" border size="small">
            <el-table-column label="费用大类" min-width="160">
              <template slot-scope="scope">
                <el-input v-model="scope.row.category" placeholder="如：差旅招待" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="预算变更量" width="180">
              <template slot-scope="scope">
                <el-input-number v-model="scope.row.delta" :precision="2" size="small" style="width: 100%;" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="70" align="center">
              <template slot-scope="scope">
                <el-button size="mini" type="text" icon="el-icon-delete" @click="budgetDeltaRows.splice(scope.$index, 1)" />
              </template>
            </el-table-column>
          </el-table>
        </el-form-item>
        <el-form-item label="变更原因" prop="changeReason">
          <el-input v-model="form.changeReason" type="textarea" :rows="2" placeholder="请输入变更原因" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="open = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listChange, addChange, approveChange, rejectChange, confirmChange } from '@/api/system/fee/fundChange'
import { listActiveProject } from '@/api/system/fee/fundProject'

export default {
  name: 'FundChange',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      changeList: [],
      activeProjects: [],
      open: false,
      budgetDeltaRows: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        projectId: null,
        changeStatus: null
      },
      form: {},
      rules: {
        projectId: [{ required: true, message: '请选择项目', trigger: 'change' }]
      }
    }
  },
  created() {
    this.getList()
    listActiveProject().then(res => { this.activeProjects = res.data || [] })
  },
  methods: {
    getList() {
      this.loading = true
      listChange(this.queryParams).then(response => {
        this.changeList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    typeText(type) {
      const map = { CONTRACT_INCREASE: '合同额增加', BUDGET_INCREASE: '预算增加', COMPREHENSIVE: '综合变更' }
      return map[type] || type
    },
    statusText(status) {
      const map = { PENDING: '待审批', APPROVED: '待收款确认', REJECTED: '已驳回', CONFIRMED: '已确认', OVERDUE: '逾期' }
      return map[status] || status
    },
    statusTag(status) {
      const map = { PENDING: 'warning', APPROVED: 'primary', REJECTED: 'info', CONFIRMED: 'success', OVERDUE: 'danger' }
      return map[status] || 'info'
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleAdd() {
      this.form = { projectId: null, contractDelta: 0, changeReason: null }
      this.budgetDeltaRows = []
      this.open = true
    },
    addBudgetDelta() {
      this.budgetDeltaRows.push({ category: null, delta: 0 })
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          const budgetDeltas = {}
          this.budgetDeltaRows.forEach(row => {
            if (row.category && row.delta !== 0) {
              budgetDeltas[row.category] = row.delta
            }
          })
          addChange({ ...this.form, budgetDeltas }).then(() => {
            this.$modal.msgSuccess('变更已提交，等待老板审批')
            this.open = false
            this.getList()
          })
        }
      })
    },
    handleApprove(row) {
      this.$modal.confirm('确认审批通过该变更？通过后合同额/预算将增量叠加到项目。').then(() => {
        return approveChange(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('审批通过')
      }).catch(() => {})
    },
    handleReject(row) {
      this.$prompt('请输入驳回原因', '驳回合同变更', {
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }).then(({ value }) => {
        return rejectChange(row.id, value)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('已驳回')
      }).catch(() => {})
    },
    handleConfirm(row) {
      this.$modal.confirm('确认该变更的增量款项已收款？').then(() => {
        return confirmChange(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('收款确认完成')
      }).catch(() => {})
    }
  }
}
</script>
