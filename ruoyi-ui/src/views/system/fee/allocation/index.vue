<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="分摊期间" prop="period">
        <el-date-picker
          v-model="queryParams.period"
          type="month"
          value-format="yyyy-MM"
          placeholder="请选择分摊期间"
          clearable
        />
      </el-form-item>
      <el-form-item label="来源部门" prop="sourceDeptId">
        <el-input v-model="queryParams.sourceDeptId" placeholder="请输入来源部门ID" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-refresh" size="mini" @click="handleExecute" v-hasPermi="['fee:allocation:config']">执行分摊</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="allocationList">
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="分摊期间" align="center" prop="period" width="120" />
      <el-table-column label="来源部门ID" align="center" prop="sourceDeptId" width="110" />
      <el-table-column label="来源部门" align="center" prop="sourceDeptName" min-width="150" />
      <el-table-column label="目标部门ID" align="center" prop="targetDeptId" width="110" />
      <el-table-column label="目标部门" align="center" prop="targetDeptName" min-width="150" />
      <el-table-column label="分摊基准" align="center" prop="basis" width="120">
        <template slot-scope="scope">
          <el-tag size="small">{{ getBasisLabel(scope.row.basis) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="分摊金额" align="center" prop="allocAmount" width="120">
        <template slot-scope="scope">
          <span>{{ scope.row.allocAmount ? '¥' + Number(scope.row.allocAmount).toLocaleString() : '-' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 执行分摊对话框 -->
    <el-dialog title="执行跨部门分摊" :visible.sync="executeOpen" width="450px" append-to-body>
      <el-form ref="executeForm" :model="executeForm" :rules="executeRules" label-width="100px">
        <el-form-item label="分摊期间" prop="period">
          <el-date-picker
            v-model="executeForm.period"
            type="month"
            value-format="yyyy-MM"
            placeholder="请选择分摊期间"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="来源部门ID" prop="sourceDeptId">
          <el-input-number v-model="executeForm.sourceDeptId" :min="1" placeholder="请输入来源部门ID" style="width: 100%" />
        </el-form-item>
        <el-form-item label="分摊基准" prop="basis">
          <el-select v-model="executeForm.basis" placeholder="请选择分摊基准" style="width: 100%">
            <el-option label="按人数" value="HEADCOUNT" />
            <el-option label="按面积" value="AREA" />
            <el-option label="按收入" value="REVENUE" />
          </el-select>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitExecute">确 定</el-button>
        <el-button @click="executeOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listAllocLogs, executeAllocation } from '@/api/system/fee/crossAlloc'

export default {
  name: 'Allocation',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      allocationList: [],
      executeOpen: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        period: null,
        sourceDeptId: null
      },
      executeForm: {
        period: null,
        sourceDeptId: null,
        basis: 'HEADCOUNT'
      },
      executeRules: {
        period: [{ required: true, message: '分摊期间不能为空', trigger: 'change' }],
        sourceDeptId: [{ required: true, message: '来源部门ID不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询分摊记录列表 */
    getList() {
      this.loading = true
      listAllocLogs(this.queryParams).then(response => {
        this.allocationList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    /** 打开执行分摊对话框 */
    handleExecute() {
      this.executeForm = { period: null, sourceDeptId: null, basis: 'HEADCOUNT' }
      this.executeOpen = true
    },
    /** 提交执行分摊 */
    submitExecute() {
      this.$refs['executeForm'].validate(valid => {
        if (valid) {
          this.$modal.confirm('确认执行跨部门分摊吗？').then(() => {
            return executeAllocation(this.executeForm)
          }).then(() => {
            this.$modal.msgSuccess('分摊执行成功')
            this.executeOpen = false
            this.getList()
          }).catch(() => {})
        }
      })
    },
    /** 获取分摊基准标签 */
    getBasisLabel(basis) {
      const map = { HEADCOUNT: '按人数', AREA: '按面积', REVENUE: '按收入' }
      return map[basis] || basis || '-'
    }
  }
}
</script>
