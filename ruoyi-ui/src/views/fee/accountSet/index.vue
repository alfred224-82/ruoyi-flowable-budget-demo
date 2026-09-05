<template>
  <div class="app-container">
    <el-alert
      title="多账套为专业版能力（FEAT_MULTI_ACCOUNT_SET）：每个账套拥有独立科目表与预算版本，单据按账套归属隔离核算；默认账套用于承接未显式指定账套的单据。有单据归属或为默认账套时不可删除。"
      type="info" :closable="false" style="margin-bottom: 10px;" />

    <el-form :inline="true" size="small" v-show="showSearch">
      <el-form-item label="账套名称">
        <el-input v-model="queryParams.name" placeholder="请输入账套名称" clearable style="width: 180px;" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="账套编码">
        <el-input v-model="queryParams.code" placeholder="请输入账套编码" clearable style="width: 180px;" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="全部" clearable style="width: 140px;">
          <el-option v-for="d in statusOptions" :key="d.value" :label="d.label" :value="d.value" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['fee:accountSet:add']">新增账套</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-s-data" size="mini" @click="handleReport" v-hasPermi="['fee:accountSet:mergeReport']">合并报表</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="accountSetList" border size="small">
      <el-table-column label="账套编码" align="center" prop="code" width="130" />
      <el-table-column label="账套名称" align="center" prop="name" min-width="140" show-overflow-tooltip />
      <el-table-column label="所属组织" align="center" prop="orgName" min-width="120">
        <template slot-scope="scope">{{ scope.row.orgName || '租户级' }}</template>
      </el-table-column>
      <el-table-column label="财年起始" align="center" prop="fiscalYearStart" width="90" />
      <el-table-column label="默认账套" align="center" width="90">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.isDefault === 1" type="warning" size="small">默认</el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="90">
        <template slot-scope="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="small">{{ statusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" width="150">
        <template slot-scope="scope">{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="270">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(scope.row)" v-hasPermi="['fee:accountSet:edit']">修改</el-button>
          <el-button v-if="scope.row.isDefault !== 1" size="mini" type="text" icon="el-icon-star-off" @click="handleSetDefault(scope.row)" v-hasPermi="['fee:accountSet:edit']">设默认</el-button>
          <el-button size="mini" type="text" icon="el-icon-sort" @click="handleSwitch(scope.row)" v-hasPermi="['fee:accountSet:switch']">切换</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['fee:accountSet:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/修改账套 -->
    <el-dialog :title="form.id ? '修改账套' : '新增账套'" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="账套编码" prop="code">
          <el-input v-model="form.code" placeholder="如 AS001（租户内唯一）" :disabled="!!form.id" />
        </el-form-item>
        <el-form-item label="账套名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入账套名称" />
        </el-form-item>
        <el-form-item label="所属组织ID" prop="orgId">
          <el-input-number v-model="form.orgId" :min="1" :precision="0" controls-position="right" placeholder="留空为租户级账套" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="财年起始" prop="fiscalYearStart">
          <el-input v-model="form.fiscalYearStart" placeholder="MM-DD，如 01-01" style="width: 200px;" />
        </el-form-item>
        <el-form-item label="默认账套" prop="isDefault">
          <el-switch :value="form.isDefault === 1" @change="v => (form.isDefault = v ? 1 : 0)" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 200px;">
            <el-option v-for="d in statusOptions" :key="d.value" :label="d.label" :value="d.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="open = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 合并报表（集团视角：各账套已审批单据汇总，老板只读） -->
    <el-dialog title="合并报表（集团视角）" :visible.sync="reportOpen" width="760px" append-to-body>
      <el-form :inline="true" size="small">
        <el-form-item label="会计期间">
          <el-input v-model="reportPeriod" placeholder="如 2026-09，留空为全部" clearable style="width: 220px;" @keyup.enter.native="loadReport" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-search" @click="loadReport">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table v-loading="reportLoading" :data="reportList" border size="small" show-summary :summary-method="reportSummary">
        <el-table-column label="账套编码" align="center" prop="accountSetCode" width="150">
          <template slot-scope="scope">{{ scope.row.accountSetCode || '默认账套' }}</template>
        </el-table-column>
        <el-table-column label="账套名称" align="center" prop="accountSetName" min-width="160">
          <template slot-scope="scope">{{ scope.row.accountSetName || '默认账套' }}</template>
        </el-table-column>
        <el-table-column label="已审批单据数" align="center" prop="billCount" width="130" />
        <el-table-column label="已审批金额合计" align="center" prop="totalAmount" width="160">
          <template slot-scope="scope">{{ formatAmount(scope.row.totalAmount) }}</template>
        </el-table-column>
      </el-table>
      <div slot="footer" class="dialog-footer">
        <el-button @click="reportOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listAccountSet, getAccountSet, addAccountSet, updateAccountSet, delAccountSet, setDefaultAccountSet, switchAccountSet, consolidatedReport } from '@/api/system/fee/accountSet'

export default {
  name: 'FeeAccountSet',
  data() {
    return {
      loading: false,
      showSearch: true,
      accountSetList: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10, name: null, code: null, status: null },
      statusOptions: [
        { value: 'ACTIVE', label: '启用' },
        { value: 'FROZEN', label: '冻结' },
        { value: 'CLOSED', label: '已关闭' }
      ],
      open: false,
      form: {},
      rules: {
        code: [{ required: true, message: '账套编码不能为空', trigger: 'blur' }],
        name: [{ required: true, message: '账套名称不能为空', trigger: 'blur' }]
      },
      reportOpen: false,
      reportLoading: false,
      reportPeriod: null,
      reportList: []
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listAccountSet(this.queryParams).then(res => {
        this.accountSetList = res.rows || []
        this.total = res.total || 0
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    statusText(status) {
      const d = this.statusOptions.find(o => o.value === status)
      return d ? d.label : (status || '-')
    },
    statusTagType(status) {
      const map = { ACTIVE: 'success', FROZEN: 'warning', CLOSED: 'info' }
      return map[status] || 'info'
    },
    formatAmount(v) {
      if (v === null || v === undefined || v === '') return '0.00'
      return Number(v).toFixed(2)
    },
    reportSummary({ columns, data }) {
      const sums = []
      columns.forEach((col, idx) => {
        if (idx === 0) { sums[idx] = '合计'; return }
        if (col.property === 'billCount') {
          sums[idx] = data.reduce((s, r) => s + Number(r.billCount || 0), 0)
        } else if (col.property === 'totalAmount') {
          sums[idx] = data.reduce((s, r) => s + Number(r.totalAmount || 0), 0).toFixed(2)
        } else {
          sums[idx] = ''
        }
      })
      return sums
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.queryParams = { pageNum: 1, pageSize: 10, name: null, code: null, status: null }
      this.getList()
    },
    handleAdd() {
      this.form = { id: null, orgId: null, name: null, code: null, fiscalYearStart: '01-01', isDefault: 0, status: 'ACTIVE', remark: null }
      this.open = true
      this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
    },
    handleEdit(row) {
      getAccountSet(row.id).then(res => {
        this.form = { ...(res.data || {}) }
        this.open = true
        this.$nextTick(() => this.$refs.form && this.$refs.form.clearValidate())
      })
    },
    submitForm() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        const action = this.form.id ? updateAccountSet : addAccountSet
        action(this.form).then(() => {
          this.$modal.msgSuccess(this.form.id ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleSetDefault(row) {
      this.$modal.confirm('确认将「' + row.name + '」设为默认账套？').then(() => {
        return setDefaultAccountSet(row.id)
      }).then(() => {
        this.$modal.msgSuccess('已设为默认账套')
        this.getList()
      }).catch(() => {})
    },
    handleSwitch(row) {
      this.$modal.confirm('确认切换当前操作账套为「' + row.name + '」？后续新建单据将归属该账套。').then(() => {
        return switchAccountSet(row.id)
      }).then(() => {
        this.$modal.msgSuccess('已切换账套上下文')
      }).catch(() => {})
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除账套「' + row.name + '」？').then(() => {
        return delAccountSet(row.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.getList()
      }).catch(() => {})
    },
    handleReport() {
      this.reportPeriod = null
      this.reportList = []
      this.reportOpen = true
      this.loadReport()
    },
    loadReport() {
      this.reportLoading = true
      consolidatedReport(this.reportPeriod).then(res => {
        this.reportList = res.data || []
        this.reportLoading = false
      }).catch(() => { this.reportLoading = false })
    }
  }
}
</script>
