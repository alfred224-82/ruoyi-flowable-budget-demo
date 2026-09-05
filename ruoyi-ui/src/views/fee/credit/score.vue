<template>
  <div class="app-container">
    <el-alert title="员工信用分（高级版）：优秀≥80免老板审批且补票14天 / 正常50-79标准流程 / 受限<50禁用无发票报销。每周一自动重算。" type="info" :closable="false" style="margin-bottom: 15px;" />

    <el-form :inline="true" size="small">
      <el-form-item label="用户ID">
        <el-input v-model="queryParams.userId" placeholder="请输入用户ID" clearable style="width: 160px;" @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="信用等级">
        <el-select v-model="queryParams.creditLevel" placeholder="全部" clearable style="width: 140px;">
          <el-option label="优秀" value="EXCELLENT" />
          <el-option label="正常" value="NORMAL" />
          <el-option label="受限" value="RESTRICTED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" @click="handleQuery">查询</el-button>
        <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table :data="creditList" border size="small" v-loading="loading">
      <el-table-column label="用户ID" width="110" align="center" prop="userId" />
      <el-table-column label="信用分" width="110" align="center">
        <template slot-scope="scope">
          <span style="font-weight: bold; font-size: 15px;">{{ scope.row.creditScore }}</span>
        </template>
      </el-table-column>
      <el-table-column label="信用等级" width="110" align="center">
        <template slot-scope="scope">
          <el-tag :type="levelTagType(scope.row.creditLevel)" size="small">{{ levelText(scope.row.creditLevel) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="按时补票次数" width="110" align="center" prop="onTimeCount" />
      <el-table-column label="逾期次数" width="100" align="center" prop="overdueCount" />
      <el-table-column label="连续按时" width="100" align="center" prop="consecutiveOnTime" />
      <el-table-column label="最近重算" width="160" align="center">
        <template slot-scope="scope">{{ parseTime(scope.row.lastCalcDate, '{y}-{m}-{d} {h}:{i}') }}</template>
      </el-table-column>
      <el-table-column label="操作" width="120" align="center">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleAdjust(scope.row)" v-hasPermi="['fee:credit:edit']">调整</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="loadList" />

    <!-- 调整信用分 -->
    <el-dialog title="调整信用分" :visible.sync="adjustVisible" width="460px" append-to-body>
      <el-form :model="adjustForm" :rules="adjustRules" ref="adjustForm" label-width="100px">
        <el-form-item label="用户">
          <span style="font-weight: bold;">{{ adjustForm.userId }}</span>
          <el-tag :type="levelTagType(adjustForm.level)" size="small" style="margin-left: 8px;">{{ levelText(adjustForm.level) }}（{{ adjustForm.score }}分）</el-tag>
        </el-form-item>
        <el-form-item label="调整分值" prop="delta">
          <el-input-number v-model="adjustForm.delta" :min="-100" :max="100" controls-position="right" style="width: 100%;" placeholder="正数加分/负数扣分" />
        </el-form-item>
        <el-form-item label="调整原因" prop="reason">
          <el-input v-model="adjustForm.reason" type="textarea" :rows="2" placeholder="请输入调整原因" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="adjustVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="submitAdjust">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listCredit, adjustCredit } from '@/api/system/fee/fundCredit'

export default {
  name: 'FeeCreditScore',
  data() {
    return {
      loading: false,
      creditList: [],
      total: 0,
      queryParams: { pageNum: 1, pageSize: 10, userId: null, creditLevel: null },
      adjustVisible: false,
      submitLoading: false,
      adjustForm: { userId: null, score: 0, level: '', delta: null, reason: '' },
      adjustRules: {
        delta: [{ required: true, message: '请填写调整分值', trigger: 'blur' }],
        reason: [{ required: true, message: '请输入调整原因', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.loadList()
  },
  methods: {
    levelText(level) {
      const map = { EXCELLENT: '优秀', NORMAL: '正常', RESTRICTED: '受限' }
      return map[level] || level || '-'
    },
    levelTagType(level) {
      const map = { EXCELLENT: 'success', NORMAL: 'warning', RESTRICTED: 'danger' }
      return map[level] || 'info'
    },
    loadList() {
      this.loading = true
      listCredit(this.queryParams).then(res => {
        this.creditList = res.rows || []
        this.total = res.total || 0
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.loadList()
    },
    resetQuery() {
      this.queryParams = { pageNum: 1, pageSize: 10, userId: null, creditLevel: null }
      this.loadList()
    },
    handleAdjust(row) {
      this.adjustForm = { userId: row.userId, score: row.creditScore, level: row.creditLevel, delta: null, reason: '' }
      this.adjustVisible = true
    },
    submitAdjust() {
      this.$refs.adjustForm.validate(valid => {
        if (!valid) return
        this.submitLoading = true
        adjustCredit(this.adjustForm.userId, this.adjustForm.delta, this.adjustForm.reason).then(() => {
          this.$modal.msgSuccess('信用分调整成功')
          this.adjustVisible = false
          this.loadList()
        }).finally(() => { this.submitLoading = false })
      })
    }
  }
}
</script>
