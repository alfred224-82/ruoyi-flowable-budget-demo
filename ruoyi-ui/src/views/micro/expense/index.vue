<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="收支分类" prop="category">
        <el-input v-model="queryParams.category" placeholder="请输入收支分类" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="来源类型" prop="relatedType">
        <el-select v-model="queryParams.relatedType" placeholder="请选择来源" clearable>
          <el-option label="人员结算" value="settlement" />
          <el-option label="手动补录" value="manual" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['micro:expense:add']">手动补录支出</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="expenseList">
      <el-table-column label="发生日期" align="center" prop="occurDate" width="120">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.occurDate, '{y}-{m}-{d}') }}</span></template>
      </el-table-column>
      <el-table-column label="收支分类" align="center" prop="category" min-width="120" show-overflow-tooltip />
      <el-table-column label="金额" align="center" prop="amount" width="120">
        <template slot-scope="scope"><span style="color:#f56c6c;font-weight:600;">-￥{{ scope.row.amount }}</span></template>
      </el-table-column>
      <el-table-column label="来源" align="center" prop="relatedType" width="100">
        <template slot-scope="scope"><span>{{ relatedTypeText(scope.row.relatedType) }}</span></template>
      </el-table-column>
      <el-table-column label="补录人" align="center" prop="createBy" width="100">
        <template slot-scope="scope"><span>{{ scope.row.createBy || '—' }}</span></template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" min-width="140" show-overflow-tooltip />
      <el-table-column label="创建时间" align="center" prop="createTime" width="150">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 手动补录支出 -->
    <el-dialog title="手动补录支出" :visible.sync="open" width="520px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="收支分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择或输入分类" filterable allow-create default-first-option style="width:100%;">
            <el-option v-for="c in categoryOptions" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="金额" prop="amount">
          <el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="1" controls-position="right" style="width:100%;" />
        </el-form-item>
        <el-form-item label="发生日期" prop="occurDate">
          <el-date-picker v-model="form.occurDate" type="date" value-format="yyyy-MM-dd" placeholder="默认当天" style="width:100%;" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
        </el-form-item>
        <el-alert type="warning" :closable="false" show-icon title="★纯记账口径：补录支出仅登记资金流水，不对接任何支付/打款接口。人员结算发放建议由结算列表「标记已线下发放」自动生成。" />
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listExpense, addExpense } from '@/api/micro/cashflow'

export default {
  name: 'MicroExpense',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      expenseList: [],
      categoryOptions: ['工资结算', '房租', '水电费', '采购成本', '营销费用', '其他支出'],
      open: false,
      queryParams: { pageNum: 1, pageSize: 10, category: null, relatedType: null },
      form: {},
      rules: {
        amount: [{ required: true, message: '金额不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listExpense(this.queryParams).then(response => {
        this.expenseList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    relatedTypeText(v) {
      return { booking: '预约订单', settlement: '人员结算', manual: '手动补录' }[v] || v || '—'
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    reset() {
      this.form = { category: null, amount: null, occurDate: this.parseTime(new Date(), '{y}-{m}-{d}'), remark: null }
      this.resetForm('form')
    },
    cancel() {
      this.open = false
      this.reset()
    },
    handleAdd() {
      this.reset()
      this.open = true
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          addExpense(this.form).then(() => {
            this.$modal.msgSuccess('补录成功')
            this.open = false
            this.getList()
          })
        }
      })
    }
  }
}
</script>
