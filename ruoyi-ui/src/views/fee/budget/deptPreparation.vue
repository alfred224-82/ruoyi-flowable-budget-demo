<template>
  <div class="app-container">
    <el-alert title="部门级预算编制（高级版）：部门粒度编制闭环复用公司级审批流程，报销按提交人部门扣额度；弹性额度内超支预警放行。" type="info" :closable="false" style="margin-bottom: 15px;" />

    <el-tabs v-model="activeTab">
      <!-- 部门编制单列表 -->
      <el-tab-pane label="部门编制列表" name="list">
        <el-form :inline="true" size="small">
          <el-form-item label="部门">
            <treeselect v-model="queryParams.departmentId" :options="deptOptions" :normalizer="normalizer" placeholder="请选择部门" style="width: 220px;" :clearable="true" />
          </el-form-item>
          <el-form-item label="预算年度">
            <el-input-number v-model="queryParams.budgetYear" :min="2000" :max="2100" controls-position="right" placeholder="年度" style="width: 110px;" />
          </el-form-item>
          <el-form-item label="月份">
            <el-select v-model="queryParams.budgetMonth" placeholder="全部" clearable style="width: 90px;">
              <el-option v-for="m in 12" :key="m" :label="m + '月'" :value="m" />
            </el-select>
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 120px;">
              <el-option label="草稿" value="Draft" />
              <el-option label="待审核" value="Pending_Review" />
              <el-option label="待修订" value="Pending_Revision" />
              <el-option label="已批准" value="Approved" />
              <el-option label="已驳回" value="Rejected" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
            <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
            <el-button type="primary" plain icon="el-icon-plus" @click="handleCreate" v-hasPermi="['fee:deptBudget:edit']">创建编制单</el-button>
          </el-form-item>
        </el-form>
        <el-table :data="prepList" border size="small" v-loading="prepLoading">
          <el-table-column label="预算单号" min-width="160" prop="sheetNo" />
          <el-table-column label="部门" min-width="140" prop="departmentName">
            <template slot-scope="scope">{{ scope.row.departmentName || '-' }}</template>
          </el-table-column>
          <el-table-column label="期间" width="100" align="center" prop="budgetPeriod" />
          <el-table-column label="状态" width="100" align="center">
            <template slot-scope="scope">
              <el-tag :type="statusTagType(scope.row.status)" size="small">{{ statusText(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="审批阶段" width="100" align="center" prop="approvalStage" />
          <el-table-column label="预算总额" width="140" align="right">
            <template slot-scope="scope">{{ formatAmount(scope.row.totalBudget) }}</template>
          </el-table-column>
          <el-table-column label="实际总额" width="140" align="right">
            <template slot-scope="scope">{{ formatAmount(scope.row.totalActual) }}</template>
          </el-table-column>
          <el-table-column label="创建时间" width="150" align="center">
            <template slot-scope="scope">{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}</template>
          </el-table-column>
        </el-table>
        <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="loadPreparations" />
      </el-tab-pane>

      <!-- 部门额度汇总 -->
      <el-tab-pane label="部门额度汇总" name="quota">
        <el-form :inline="true" size="small">
          <el-form-item label="部门">
            <treeselect v-model="quotaQuery.departmentId" :options="deptOptions" :normalizer="normalizer" placeholder="请选择部门" style="width: 220px;" :clearable="true" />
          </el-form-item>
          <el-form-item label="预算年度">
            <el-input-number v-model="quotaQuery.budgetYear" :min="2000" :max="2100" controls-position="right" style="width: 110px;" />
          </el-form-item>
          <el-form-item label="月份">
            <el-select v-model="quotaQuery.budgetMonth" placeholder="月份" style="width: 90px;">
              <el-option v-for="m in 12" :key="m" :label="m + '月'" :value="m" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="loadQuota">查询</el-button>
          </el-form-item>
        </el-form>
        <el-table :data="quotaRows" border size="small" v-loading="quotaLoading">
          <el-table-column label="预算科目" min-width="180" prop="budgetSubject" />
          <el-table-column label="编制额度" width="160" align="right">
            <template slot-scope="scope">{{ formatAmount(scope.row.budgetAmount) }}</template>
          </el-table-column>
          <el-table-column label="已用金额" width="160" align="right">
            <template slot-scope="scope">{{ formatAmount(scope.row.usedAmount) }}</template>
          </el-table-column>
          <el-table-column label="剩余额度" width="160" align="right">
            <template slot-scope="scope">
              <span :style="Number(scope.row.remainAmount || 0) < 0 ? 'color:#f56c6c;font-weight:bold;' : ''">{{ formatAmount(scope.row.remainAmount) }}</span>
            </template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!quotaLoading && quotaRows.length === 0" description="请选择部门与期间后查询" />
      </el-tab-pane>

      <!-- 弹性额度配置 -->
      <el-tab-pane label="弹性额度配置" name="elastic">
        <el-form :inline="true" size="small">
          <el-form-item>
            <el-button type="primary" plain icon="el-icon-plus" @click="handleAddElastic" v-hasPermi="['fee:deptBudget:edit']">新增配置</el-button>
          </el-form-item>
        </el-form>
        <el-table :data="elasticList" border size="small" v-loading="elasticLoading">
          <el-table-column label="费用大类ID（空=全局）" width="180" align="center" prop="categoryId">
            <template slot-scope="scope">{{ scope.row.categoryId || '全局' }}</template>
          </el-table-column>
          <el-table-column label="弹性阈值（%）" width="150" align="center" prop="thresholdPct" />
          <el-table-column label="是否启用" width="120" align="center">
            <template slot-scope="scope">
              <el-tag :type="scope.row.isEnabled === 1 ? 'success' : 'info'" size="small">{{ scope.row.isEnabled === 1 ? '启用' : '停用' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="160" align="center">
            <template slot-scope="scope">
              <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEditElastic(scope.row)" v-hasPermi="['fee:deptBudget:edit']">修改</el-button>
              <el-button size="mini" type="text" icon="el-icon-delete" style="color:#f56c6c;" @click="handleDelElastic(scope.row)" v-hasPermi="['fee:deptBudget:edit']">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 创建部门编制单 -->
    <el-dialog title="创建部门预算编制单" :visible.sync="createVisible" width="520px" append-to-body>
      <el-form :model="createForm" :rules="createRules" ref="createForm" label-width="100px">
        <el-form-item label="部门" prop="departmentId">
          <treeselect v-model="createForm.departmentId" :options="deptOptions" :normalizer="normalizer" placeholder="请选择部门" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="预算年度" prop="budgetYear">
          <el-input-number v-model="createForm.budgetYear" :min="2000" :max="2100" controls-position="right" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="预算月份" prop="budgetMonth">
          <el-select v-model="createForm.budgetMonth" placeholder="请选择月份" style="width: 100%;">
            <el-option v-for="m in 12" :key="m" :label="m + '月'" :value="m" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="createForm.remark" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitCreate">确定</el-button>
      </div>
    </el-dialog>

    <!-- 弹性额度配置弹窗 -->
    <el-dialog :title="elasticForm.id ? '修改弹性额度配置' : '新增弹性额度配置'" :visible.sync="elasticVisible" width="520px" append-to-body>
      <el-form :model="elasticForm" :rules="elasticRules" ref="elasticForm" label-width="140px">
        <el-form-item label="费用大类ID">
          <el-input-number v-model="elasticForm.categoryId" :min="1" controls-position="right" placeholder="留空=全局" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="弹性阈值（%）" prop="thresholdPct">
          <el-input-number v-model="elasticForm.thresholdPct" :min="0" :max="100" :precision="2" controls-position="right" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="是否启用">
          <el-switch v-model="elasticForm.isEnabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="elasticVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitElastic">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import Treeselect from '@riophae/vue-treeselect'
import '@riophae/vue-treeselect/dist/vue-treeselect.css'
import { listDept } from '@/api/system/dept'
import { listDeptBudget, getDeptQuota, addDeptBudget, listElasticQuota, addElasticQuota, updateElasticQuota, delElasticQuota } from '@/api/system/fee/fundDeptBudget'

export default {
  name: 'FeeDeptBudget',
  components: { Treeselect },
  data() {
    return {
      activeTab: 'list',
      deptOptions: [],
      // 编制列表
      prepList: [],
      prepLoading: false,
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10, departmentId: null, budgetYear: null, budgetMonth: null, status: null },
      // 额度汇总
      quotaRows: [],
      quotaLoading: false,
      quotaQuery: { departmentId: null, budgetYear: new Date().getFullYear(), budgetMonth: new Date().getMonth() + 1 },
      // 弹性额度
      elasticList: [],
      elasticLoading: false,
      elasticVisible: false,
      elasticForm: { id: null, categoryId: null, thresholdPct: 10, isEnabled: 1 },
      elasticRules: {
        thresholdPct: [{ required: true, message: '弹性阈值不能为空', trigger: 'blur' }]
      },
      // 创建编制单
      createVisible: false,
      submitLoading: false,
      createForm: { departmentId: null, budgetYear: new Date().getFullYear(), budgetMonth: new Date().getMonth() + 1, remark: '' },
      createRules: {
        departmentId: [{ required: true, message: '请选择部门', trigger: 'change' }],
        budgetYear: [{ required: true, message: '请填写预算年度', trigger: 'blur' }],
        budgetMonth: [{ required: true, message: '请选择预算月份', trigger: 'change' }]
      }
    }
  },
  created() {
    this.loadDeptTree()
    this.loadPreparations()
    this.loadElastic()
  },
  methods: {
    loadDeptTree() {
      listDept({ status: '0' }).then(res => {
        this.deptOptions = this.buildDeptTree(res.data || [], 0)
      })
    },
    buildDeptTree(list, parentId) {
      return list.filter(d => d.parentId === parentId).map(d => ({
        id: d.deptId,
        label: d.deptName,
        children: this.buildDeptTree(list, d.deptId)
      }))
    },
    normalizer(node) {
      if (node.children && !node.children.length) delete node.children
      return { id: node.id, label: node.label, children: node.children }
    },
    formatAmount(val) {
      if (val === null || val === undefined || val === '') return '-'
      return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    },
    statusText(status) {
      const map = { Draft: '草稿', Pending_Review: '待审核', Pending_Revision: '待修订', Approved: '已批准', Rejected: '已驳回' }
      return map[status] || status
    },
    statusTagType(status) {
      const map = { Draft: 'info', Pending_Review: 'warning', Pending_Revision: 'warning', Approved: 'success', Rejected: 'danger' }
      return map[status] || 'info'
    },
    loadPreparations() {
      this.prepLoading = true
      listDeptBudget(this.queryParams).then(res => {
        this.prepList = res.rows || []
        this.total = res.total || 0
        this.prepLoading = false
      }).catch(() => { this.prepLoading = false })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.loadPreparations()
    },
    resetQuery() {
      this.queryParams = { pageNum: 1, pageSize: 10, departmentId: null, budgetYear: null, budgetMonth: null, status: null }
      this.loadPreparations()
    },
    handleCreate() {
      this.createForm = { departmentId: null, budgetYear: new Date().getFullYear(), budgetMonth: new Date().getMonth() + 1, remark: '' }
      this.createVisible = true
    },
    submitCreate() {
      this.$refs.createForm.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        addDeptBudget(this.createForm).then(() => {
          this.$modal.msgSuccess('部门预算编制单创建成功')
          this.createVisible = false
          this.loadPreparations()
        }).finally(() => { this.submitLoading = false })
      })
    },
    loadQuota() {
      if (!this.quotaQuery.departmentId) {
        this.$modal.msgWarning('请先选择部门')
        return
      }
      this.quotaLoading = true
      getDeptQuota(this.quotaQuery).then(res => {
        this.quotaRows = res.data || []
        this.quotaLoading = false
      }).catch(() => { this.quotaLoading = false })
    },
    loadElastic() {
      this.elasticLoading = true
      listElasticQuota().then(res => {
        this.elasticList = res.data || []
        this.elasticLoading = false
      }).catch(() => { this.elasticLoading = false })
    },
    handleAddElastic() {
      this.elasticForm = { id: null, categoryId: null, thresholdPct: 10, isEnabled: 1 }
      this.elasticVisible = true
    },
    handleEditElastic(row) {
      this.elasticForm = { ...row }
      this.elasticVisible = true
    },
    submitElastic() {
      this.$refs.elasticForm.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        const action = this.elasticForm.id ? updateElasticQuota : addElasticQuota
        action(this.elasticForm).then(() => {
          this.$modal.msgSuccess('保存成功')
          this.elasticVisible = false
          this.loadElastic()
        }).finally(() => { this.submitLoading = false })
      })
    },
    handleDelElastic(row) {
      this.$modal.confirm('确认删除该弹性额度配置？').then(() => {
        return delElasticQuota(row.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.loadElastic()
      }).catch(() => {})
    }
  }
}
</script>
