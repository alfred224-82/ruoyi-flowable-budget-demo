<template>
  <div class="app-container">
    <el-alert title="分成规则配置：项目经理在分成管理发起申请，老板在此审批定比例与基数，审批通过即生效计入项目分成。" type="info" :closable="false" style="margin-bottom: 10px;" />

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="项目" prop="projectId">
        <el-select v-model="queryParams.projectId" placeholder="全部项目" clearable filterable>
          <el-option v-for="p in activeProjects" :key="p.id" :label="p.projectName" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable>
          <el-option label="待审批" value="PENDING" />
          <el-option label="已生效" value="ACTIVE" />
          <el-option label="已支付" value="PAID" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="sharingList">
      <el-table-column label="项目" align="left" prop="projectName" min-width="140" show-overflow-tooltip />
      <el-table-column label="分成对象" align="center" prop="shareToUserName" width="110" />
      <el-table-column label="角色" align="center" prop="role" width="90">
        <template slot-scope="scope">{{ roleText(scope.row.role) }}</template>
      </el-table-column>
      <el-table-column label="基数类型" align="center" prop="baseType" width="100">
        <template slot-scope="scope">{{ baseTypeText(scope.row.baseType) }}</template>
      </el-table-column>
      <el-table-column label="分成基数" align="right" prop="baseAmount" width="110" />
      <el-table-column label="比例(%)" align="right" prop="ratio" width="90" />
      <el-table-column label="分成金额" align="right" prop="shareAmount" width="110" />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template slot-scope="scope">
          <el-tag :type="statusTag(scope.row.status)" size="small">{{ statusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180">
        <template slot-scope="scope">
          <template v-if="scope.row.status === 'PENDING'">
            <el-button size="mini" type="text" icon="el-icon-check" @click="handleApprove(scope.row)" v-hasPermi="['fee:sharingRule:edit']">定比例审批</el-button>
            <el-button size="mini" type="text" icon="el-icon-close" @click="handleReject(scope.row)" v-hasPermi="['fee:sharingRule:edit']">驳回</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 老板审批（定比例与基数） -->
    <el-dialog title="分成审批（老板定比例与基数）" :visible.sync="approveOpen" width="480px" append-to-body>
      <el-form ref="approveForm" :model="approveForm" :rules="approveRules" label-width="100px">
        <el-form-item label="分成比例(%)" prop="ratio">
          <el-input-number v-model="approveForm.ratio" :min="0" :max="100" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="分成基数" prop="baseAmount">
          <el-input-number v-model="approveForm.baseAmount" :min="0" :precision="2" style="width: 100%;" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitApprove">确 定</el-button>
        <el-button @click="approveOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listSharing, approveSharing, rejectSharing } from '@/api/system/fee/fundSharing'
import { listActiveProject } from '@/api/system/fee/fundProject'

export default {
  name: 'FundSharingRule',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      sharingList: [],
      activeProjects: [],
      approveOpen: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        projectId: null,
        status: 'PENDING'
      },
      approveForm: {},
      approveRules: {
        ratio: [{ required: true, message: '请输入分成比例', trigger: 'blur' }],
        baseAmount: [{ required: true, message: '请输入分成基数', trigger: 'blur' }]
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
      listSharing(this.queryParams).then(response => {
        this.sharingList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    roleText(role) {
      const map = { INTRODUCER: '介绍人', PARTNER: '合伙人', CHANNEL: '渠道方' }
      return map[role] || role
    },
    baseTypeText(type) {
      const map = { CONTRACT: '合同额', INCOME: '已核验收入', PROFIT: '项目利润', FIXED: '固定金额' }
      return map[type] || type
    },
    statusText(status) {
      const map = { PENDING: '待审批', ACTIVE: '已生效', PAID: '已支付' }
      return map[status] || status
    },
    statusTag(status) {
      const map = { PENDING: 'warning', ACTIVE: 'success', PAID: 'info' }
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
    handleApprove(row) {
      this.approveForm = { id: row.id, ratio: row.ratio || 0, baseAmount: row.baseAmount || 0 }
      this.approveOpen = true
    },
    submitApprove() {
      this.$refs['approveForm'].validate(valid => {
        if (valid) {
          approveSharing(this.approveForm.id, this.approveForm.ratio, this.approveForm.baseAmount).then(() => {
            this.$modal.msgSuccess('审批通过，分成已生效')
            this.approveOpen = false
            this.getList()
          })
        }
      })
    },
    handleReject(row) {
      this.$modal.confirm('驳回将删除该待审批分成记录，确认？').then(() => {
        return rejectSharing(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('已驳回')
      }).catch(() => {})
    }
  }
}
</script>
