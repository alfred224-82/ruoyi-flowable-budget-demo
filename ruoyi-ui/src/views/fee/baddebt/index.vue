<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable>
          <el-option label="待处理" value="OPEN" />
          <el-option label="跟进中" value="FOLLOWING" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-warning" size="mini" @click="handleMark" v-hasPermi="['fee:baddebt:mark']">标记烂账风险</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="badDebtList">
      <el-table-column label="项目" align="left" prop="projectName" min-width="150" show-overflow-tooltip />
      <el-table-column label="烂账类型" align="center" prop="type" width="110">
        <template slot-scope="scope">{{ typeText(scope.row.type) }}</template>
      </el-table-column>
      <el-table-column label="处置建议" align="center" prop="proposal" width="100">
        <template slot-scope="scope">{{ proposalText(scope.row.proposal) }}</template>
      </el-table-column>
      <el-table-column label="原因说明" align="left" prop="reason" min-width="160" show-overflow-tooltip />
      <el-table-column label="坏账计提" align="right" prop="provisionAmount" width="110" />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template slot-scope="scope">
          <el-tag :type="statusTag(scope.row.status)" size="small">{{ statusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="跟进记录" align="left" prop="followUpNotes" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="220">
        <template slot-scope="scope">
          <template v-if="scope.row.status !== 'CLOSED'">
            <el-button size="mini" type="text" icon="el-icon-money" @click="handleProvision(scope.row)" v-hasPermi="['fee:baddebt:mark']">追加计提</el-button>
            <el-button size="mini" type="text" icon="el-icon-edit" @click="handleFollow(scope.row)" v-hasPermi="['fee:baddebt:follow']">跟进</el-button>
            <el-button size="mini" type="text" icon="el-icon-circle-close" @click="handleClose(scope.row)" v-hasPermi="['fee:baddebt:follow']">关闭</el-button>
          </template>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 标记烂账风险 -->
    <el-dialog title="标记烂账风险（带计提金额时同步计提坏账）" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="所属项目" prop="projectId">
          <el-select v-model="form.projectId" placeholder="请选择项目" filterable style="width: 100%;">
            <el-option v-for="p in activeProjects" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="烂账类型" prop="type">
          <el-select v-model="form.type" placeholder="请选择烂账类型" style="width: 100%;">
            <el-option label="失联/跑路" value="LOST_CONTACT" />
            <el-option label="质量纠纷" value="QUALITY_DISPUTE" />
            <el-option label="价格争议" value="PRICE_ISSUE" />
            <el-option label="资料不全" value="INCOMPLETE" />
            <el-option label="其他" value="OTHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="处置建议" prop="proposal">
          <el-radio-group v-model="form.proposal">
            <el-radio label="BAD_DEBT">计提坏账</el-radio>
            <el-radio label="FOLLOW_UP">继续跟进</el-radio>
            <el-radio label="LEGAL">法律途径</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="计提坏账金额" prop="provisionAmount">
          <el-input-number v-model="form.provisionAmount" :min="0" :precision="2" style="width: 100%;" placeholder="0=暂不计提" />
        </el-form-item>
        <el-form-item label="原因说明" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="2" placeholder="请说明烂账原因" />
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
import { listBaddebt, markBaddebt, provisionBaddebt, followBaddebt, closeBaddebt } from '@/api/system/fee/fundBaddebt'
import { listActiveProject } from '@/api/system/fee/fundProject'

export default {
  name: 'FundBaddebt',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      badDebtList: [],
      activeProjects: [],
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        status: null
      },
      form: {},
      rules: {
        projectId: [{ required: true, message: '请选择项目', trigger: 'change' }],
        type: [{ required: true, message: '请选择烂账类型', trigger: 'change' }],
        reason: [{ required: true, message: '原因说明不能为空', trigger: 'blur' }]
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
      listBaddebt(this.queryParams).then(response => {
        this.badDebtList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    typeText(type) {
      const map = { LOST_CONTACT: '失联/跑路', QUALITY_DISPUTE: '质量纠纷', PRICE_ISSUE: '价格争议', INCOMPLETE: '资料不全', OTHER: '其他' }
      return map[type] || type
    },
    proposalText(proposal) {
      const map = { BAD_DEBT: '计提坏账', FOLLOW_UP: '继续跟进', LEGAL: '法律途径' }
      return map[proposal] || proposal
    },
    statusText(status) {
      const map = { OPEN: '待处理', FOLLOWING: '跟进中', CLOSED: '已关闭' }
      return map[status] || status
    },
    statusTag(status) {
      const map = { OPEN: 'danger', FOLLOWING: 'warning', CLOSED: 'info' }
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
    handleMark() {
      this.form = { projectId: null, type: 'OTHER', proposal: 'FOLLOW_UP', provisionAmount: 0, reason: null }
      this.open = true
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          markBaddebt(this.form).then(() => {
            this.$modal.msgSuccess('风险已标记')
            this.open = false
            this.getList()
          })
        }
      })
    },
    handleProvision(row) {
      this.$prompt('请输入追加计提金额', '追加坏账计提', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /^\d+(\.\d{1,2})?$/,
        inputErrorMessage: '请输入有效金额'
      }).then(({ value }) => {
        return provisionBaddebt(row.id, value)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('计提成功')
      }).catch(() => {})
    },
    handleFollow(row) {
      this.$prompt('请输入跟进记录', '烂账跟进', {
        confirmButtonText: '确定',
        cancelButtonText: '取消'
      }).then(({ value }) => {
        return followBaddebt(row.id, value)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('跟进记录已追加')
      }).catch(() => {})
    },
    handleClose(row) {
      this.$modal.confirm('关闭后项目状态将置为烂账（LOST），确认关闭？').then(() => {
        return closeBaddebt(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('已关闭')
      }).catch(() => {})
    }
  }
}
</script>
