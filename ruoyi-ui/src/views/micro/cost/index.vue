<template>
  <div class="app-container">
    <!-- 本月总支出概览 -->
    <el-row :gutter="20" class="mb16">
      <el-col :span="8">
        <el-card shadow="hover">
          <div style="text-align:center;">
            <div style="color:#909399;font-size:14px;">本月固定支出合计（月分摊）</div>
            <div style="font-size:28px;font-weight:700;color:#f56c6c;margin-top:8px;">¥ {{ monthSum }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div style="text-align:center;">
            <div style="color:#909399;font-size:14px;">启用项数</div>
            <div style="font-size:28px;font-weight:700;color:#409eff;margin-top:8px;">{{ enabledCount }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card shadow="hover">
          <div style="text-align:center;">
            <div style="color:#909399;font-size:14px;">总项数</div>
            <div style="font-size:28px;font-weight:700;color:#67c23a;margin-top:8px;">{{ total }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 搜索 + 操作栏 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="80px">
      <el-form-item label="支出项" prop="name">
        <el-input v-model="queryParams.name" placeholder="名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="分类" prop="category">
        <el-select v-model="queryParams.category" placeholder="全部分类" clearable>
          <el-option v-for="tag in tagList" :key="tag.id" :label="tag.tagName" :value="tag.tagCode || String(tag.id)" />
        </el-select>
      </el-form-item>
      <el-form-item label="月份" prop="effectiveMonth">
        <el-date-picker v-model="queryParams.effectiveMonth" type="month" placeholder="选择月份" value-format="yyyy-MM" clearable />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['micro:pro:cost:add']">新增</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 列表 -->
    <el-table v-loading="loading" :data="costList">
      <el-table-column label="支出项" align="left" prop="name" min-width="120" show-overflow-tooltip />
      <el-table-column label="分类" align="center" prop="categoryName" width="100" />
      <el-table-column label="总额" align="right" prop="amount" width="120">
        <template slot-scope="scope">¥ {{ scope.row.amount }}</template>
      </el-table-column>
      <el-table-column label="每月分摊" align="right" width="130">
        <template slot-scope="scope">
          <span>¥ {{ scope.row.monthlyAmount != null ? scope.row.monthlyAmount : scope.row.amount }}</span>
          <el-tooltip v-if="scope.row.amortizeMonths > 0" placement="top"
            :content="'总额 ¥' + scope.row.amount + ' 按周期分摊 ' + scope.row.amortizeMonths + ' 个月'">
            <i class="el-icon-info" style="color:#909399;margin-left:2px;" />
          </el-tooltip>
        </template>
      </el-table-column>
      <el-table-column label="周期规则" align="center" prop="periodRuleName" width="120" show-overflow-tooltip>
        <template slot-scope="scope">
          <span v-if="scope.row.periodRuleName">{{ scope.row.periodRuleName }}</span>
          <span v-else style="color:#c0c4cc;">-</span>
        </template>
      </el-table-column>
      <el-table-column label="生效月份" align="center" prop="effectiveMonth" width="110" />
      <el-table-column label="每月重复" align="center" width="90">
        <template slot-scope="scope">
          <el-tag :type="scope.row.isRecurring === 1 ? 'success' : 'info'" size="mini">{{ scope.row.isRecurring === 1 ? '是' : '否' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="启用" align="center" width="80">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.enabled" :active-value="1" :inactive-value="0" @change="handleToggle(scope.row)" />
        </template>
      </el-table-column>
      <el-table-column label="备注" align="left" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" align="center" width="140">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(scope.row)" v-hasPermi="['micro:pro:cost:edit']">编辑</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['micro:pro:cost:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="dialogTitle" :visible.sync="dialogOpen" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="支出项名称" prop="name">
          <el-input v-model="form.name" placeholder="如：房租、水电费" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="分类（标签）" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类标签" style="width:100%;" @change="handleCategoryChange">
            <el-option v-for="tag in tagList" :key="tag.id" :label="tag.tagName" :value="tag.tagCode || String(tag.id)" />
          </el-select>
        </el-form-item>
        <el-form-item label="周期规则" prop="periodRuleId">
          <el-select v-model="form.periodRuleId" placeholder="选择周期规则（可选）" style="width:100%;" clearable @change="handlePeriodRuleChange">
            <el-option v-for="rule in periodRuleList" :key="rule.id"
              :label="rule.ruleName + '（' + rule.periodValue + rule.periodUnitLabel + '）'"
              :value="rule.id" />
          </el-select>
          <div v-if="amortizationInfo" style="margin-top:6px;font-size:12px;color:#909399;">
            {{ amortizationInfo }}
          </div>
        </el-form-item>
        <el-form-item label="总金额（元）" prop="amount">
          <el-input-number v-model="form.amount" :min="0" :precision="2" :step="100" controls-position="right" style="width:100%;" />
        </el-form-item>
        <el-form-item label="生效月份" prop="effectiveMonth">
          <el-date-picker v-model="form.effectiveMonth" type="month" placeholder="选择月份" value-format="yyyy-MM" style="width:100%;" />
        </el-form-item>
        <el-form-item label="每月重复">
          <el-radio-group v-model="form.isRecurring">
            <el-radio :label="1">是（每月自动计入）</el-radio>
            <el-radio :label="0">否（仅当月）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="选填" maxlength="255" show-word-limit />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="dialogOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listCost, getMonthSum, addCost, updateCost, delCost, listAllTags, listRulesByTag } from '@/api/micro/monthlyCost'

// 时间单位中文映射
const UNIT_LABEL_MAP = {
  DAY: '天',
  WEEK: '周',
  MONTH: '个月',
  QUARTER: '个季度',
  YEAR: '年'
}

// 单位→天数换算
const UNIT_DAYS_MAP = {
  DAY: 1,
  WEEK: 7,
  MONTH: 30,
  QUARTER: 90,
  YEAR: 365
}

// ★T30.6 单位→月数换算（与后端分摊口径一致：YEAR×12 / QUARTER×3 / MONTH×1，WEEK/DAY 按 30 天/月近似）
const UNIT_MONTHS_MAP = {
  YEAR: 12,
  QUARTER: 3,
  MONTH: 1
}

export default {
  name: 'MonthlyCost',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      costList: [],
      monthSum: '0.00',
      enabledCount: 0,
      dialogOpen: false,
      dialogTitle: '',
      form: {},
      tagList: [],
      periodRuleList: [],
      amortizationInfo: '',
      queryParams: { pageNum: 1, pageSize: 10, name: null, category: null, effectiveMonth: null },
      rules: {
        name: [{ required: true, message: '支出项名称不能为空', trigger: 'blur' }],
        category: [{ required: true, message: '请选择分类标签', trigger: 'change' }],
        amount: [{ required: true, message: '金额不能为空', trigger: 'blur' }],
        effectiveMonth: [{ required: true, message: '请选择生效月份', trigger: 'change' }]
      }
    }
  },
  created() {
    this.getList()
    this.loadMonthSum()
    this.loadTags()
  },
  methods: {
    getList() {
      this.loading = true
      listCost(this.queryParams).then(res => {
        this.costList = res.rows
        this.total = res.total
        this.enabledCount = res.rows.filter(r => r.enabled === 1).length
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    loadMonthSum() {
      getMonthSum().then(res => { this.monthSum = (res.data || 0).toFixed(2) })
    },
    loadTags() {
      listAllTags().then(res => {
        this.tagList = res.data || []
      }).catch(() => { this.tagList = [] })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm('queryForm'); this.handleQuery() },
    reset() {
      this.form = { id: null, name: '', category: '', periodRuleId: null, amount: 0, isRecurring: 1, effectiveMonth: '', enabled: 1, remark: '' }
      this.periodRuleList = []
      this.amortizationInfo = ''
    },
    handleAdd() { this.reset(); this.dialogTitle = '新增固定支出'; this.dialogOpen = true },
    handleEdit(row) {
      this.form = { ...row }
      this.dialogTitle = '编辑固定支出'
      this.dialogOpen = true
      // 编辑时加载该分类对应的周期规则
      if (this.form.category) {
        this.loadPeriodRulesByCategory(this.form.category)
      }
    },
    /** 分类变更 → 加载对应周期规则 */
    handleCategoryChange(val) {
      this.form.periodRuleId = null
      this.amortizationInfo = ''
      this.loadPeriodRulesByCategory(val)
    },
    loadPeriodRulesByCategory(category) {
      // 通过 tagCode 或 tagId 查找对应的标签
      const tag = this.tagList.find(t => (t.tagCode || String(t.id)) === category)
      if (!tag || !tag.id) {
        this.periodRuleList = []
        return
      }
      listRulesByTag(tag.id).then(res => {
        this.periodRuleList = (res.data || []).map(r => ({
          ...r,
          periodUnitLabel: UNIT_LABEL_MAP[r.periodUnit] || r.periodUnit
        }))
      }).catch(() => { this.periodRuleList = [] })
    },
    /** 周期规则变更 → 计算分摊信息（★T30.6：每月分摊 = 总额 ÷ 周期总月数，如 5 年 50 万 → 500000÷60） */
    handlePeriodRuleChange(ruleId) {
      this.amortizationInfo = ''
      if (!ruleId || !this.form.amount || this.form.amount <= 0) return
      const rule = this.periodRuleList.find(r => r.id === ruleId)
      if (!rule) return
      let totalMonths = UNIT_MONTHS_MAP[rule.periodUnit] ? rule.periodValue * UNIT_MONTHS_MAP[rule.periodUnit] : 0
      if (!totalMonths) {
        const totalDays = rule.totalDays || (rule.periodValue * (UNIT_DAYS_MAP[rule.periodUnit] || 1))
        totalMonths = totalDays / 30
      }
      if (totalMonths > 0) {
        const months = Math.max(1, Math.ceil(totalMonths))
        const monthlyCost = (this.form.amount / totalMonths).toFixed(2)
        this.amortizationInfo = `周期 ${rule.periodValue}${rule.periodUnitLabel}（共 ${months} 个月），每月分摊 ¥${monthlyCost}`
      }
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) return
        const action = this.form.id ? updateCost(this.form) : addCost(this.form)
        action.then(() => {
          this.$modal.msgSuccess(this.form.id ? '修改成功' : '新增成功')
          this.dialogOpen = false
          this.getList()
          this.loadMonthSum()
        })
      })
    },
    handleToggle(row) {
      updateCost({ id: row.id, enabled: row.enabled }).then(() => {
        this.$modal.msgSuccess(row.enabled === 1 ? '已启用' : '已停用')
        this.loadMonthSum()
      }).catch(() => { row.enabled = row.enabled === 1 ? 0 : 1 })
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除支出项「' + row.name + '」吗？')
        .then(() => delCost(row.id))
        .then(() => { this.$modal.msgSuccess('删除成功'); this.getList(); this.loadMonthSum() })
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
</style>
