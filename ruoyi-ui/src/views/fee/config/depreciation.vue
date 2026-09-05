<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="折旧规则" name="rule">
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAddRule" v-hasPermi="['fee:depreciationRule:edit']">维护规则</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="warning" plain icon="el-icon-time" size="mini" @click="handleAccrue" v-hasPermi="['fee:depreciationRule:edit']">手动计提折旧</el-button>
          </el-col>
        </el-row>
        <el-table v-loading="ruleLoading" :data="ruleList" border>
          <el-table-column label="资产分类" align="center" prop="category" width="160">
            <template slot-scope="scope">{{ categoryText(scope.row.category) }}</template>
          </el-table-column>
          <el-table-column label="使用年限(月)" align="center" prop="lifeMonths" width="140" />
          <el-table-column label="残值率(%)" align="center" prop="residualRate" width="140" />
          <el-table-column label="是否启用" align="center" prop="isEnabled" width="120">
            <template slot-scope="scope">
              <el-switch :value="scope.row.isEnabled === 1" @change="toggleRule(scope.row)" v-hasPermi="['fee:depreciationRule:edit']" />
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width">
            <template slot-scope="scope">
              <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEditRule(scope.row)" v-hasPermi="['fee:depreciationRule:edit']">修改</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <el-tab-pane label="折旧记录" name="record">
        <el-form :model="recordQuery" ref="recordQueryForm" size="small" :inline="true" label-width="80px">
          <el-form-item label="计提月份" prop="accrualMonth">
            <el-date-picker v-model="recordQuery.accrualMonth" type="month" value-format="yyyy-MM" placeholder="选择月份" clearable />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" size="mini" @click="handleRecordQuery">搜索</el-button>
            <el-button icon="el-icon-refresh" size="mini" @click="resetRecordQuery">重置</el-button>
          </el-form-item>
        </el-form>
        <el-table v-loading="recordLoading" :data="recordList">
          <el-table-column label="计提月份" align="center" prop="accrualMonth" width="110" />
          <el-table-column label="资产名称" align="left" prop="assetName" min-width="150" show-overflow-tooltip />
          <el-table-column label="月折旧额" align="right" prop="amount" width="120" />
          <el-table-column label="项目分摊比例(%)" align="right" prop="shareRatio" width="130" />
          <el-table-column label="计提时间" align="center" prop="createTime" width="160">
            <template slot-scope="scope">
              <span>{{ parseTime(scope.row.createTime) }}</span>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="recordTotal > 0" :total="recordTotal" :page.sync="recordQuery.pageNum" :limit.sync="recordQuery.pageSize" @pagination="getRecordList" />
      </el-tab-pane>
    </el-tabs>

    <!-- 维护折旧规则 -->
    <el-dialog title="维护折旧规则（资产分类唯一）" :visible.sync="ruleOpen" width="480px" append-to-body>
      <el-form ref="ruleForm" :model="ruleForm" :rules="ruleRules" label-width="110px">
        <el-form-item label="资产分类" prop="category">
          <el-select v-model="ruleForm.category" placeholder="请选择分类" :disabled="!!ruleForm.id" style="width: 100%;">
            <el-option label="电子设备" value="ELECTRONIC" />
            <el-option label="家具" value="FURNITURE" />
            <el-option label="车辆" value="VEHICLE" />
            <el-option label="办公设备" value="OFFICE" />
          </el-select>
        </el-form-item>
        <el-form-item label="使用年限(月)" prop="lifeMonths">
          <el-input-number v-model="ruleForm.lifeMonths" :min="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="残值率(%)" prop="residualRate">
          <el-input-number v-model="ruleForm.residualRate" :min="0" :max="100" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="是否启用" prop="isEnabled">
          <el-switch :value="ruleForm.isEnabled === 1" @change="v => (ruleForm.isEnabled = v ? 1 : 0)" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitRule">确 定</el-button>
        <el-button @click="ruleOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listDepreciationRule, saveDepreciationRule, listDepreciation, accrueDepreciation } from '@/api/system/fee/fundAssetDepr'

export default {
  name: 'FundDepreciation',
  data() {
    return {
      activeTab: 'rule',
      ruleLoading: false,
      recordLoading: false,
      ruleList: [],
      recordList: [],
      recordTotal: 0,
      ruleOpen: false,
      recordQuery: { pageNum: 1, pageSize: 10, accrualMonth: null },
      ruleForm: {},
      ruleRules: {
        category: [{ required: true, message: '请选择资产分类', trigger: 'change' }],
        lifeMonths: [{ required: true, message: '使用年限不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getRuleList()
    this.getRecordList()
  },
  methods: {
    getRuleList() {
      this.ruleLoading = true
      listDepreciationRule().then(res => {
        this.ruleList = res.data || []
        this.ruleLoading = false
      }).catch(() => { this.ruleLoading = false })
    },
    getRecordList() {
      this.recordLoading = true
      listDepreciation(this.recordQuery).then(response => {
        this.recordList = response.rows
        this.recordTotal = response.total
        this.recordLoading = false
      }).catch(() => { this.recordLoading = false })
    },
    categoryText(category) {
      const map = { ELECTRONIC: '电子设备', FURNITURE: '家具', VEHICLE: '车辆', OFFICE: '办公设备' }
      return map[category] || category
    },
    handleRecordQuery() {
      this.recordQuery.pageNum = 1
      this.getRecordList()
    },
    resetRecordQuery() {
      this.resetForm('recordQueryForm')
      this.handleRecordQuery()
    },
    handleAddRule() {
      this.ruleForm = { category: 'ELECTRONIC', lifeMonths: 36, residualRate: 5, isEnabled: 1 }
      this.ruleOpen = true
    },
    handleEditRule(row) {
      this.ruleForm = { ...row }
      this.ruleOpen = true
    },
    submitRule() {
      this.$refs['ruleForm'].validate(valid => {
        if (valid) {
          saveDepreciationRule(this.ruleForm).then(() => {
            this.$modal.msgSuccess('保存成功')
            this.ruleOpen = false
            this.getRuleList()
          })
        }
      })
    },
    toggleRule(row) {
      saveDepreciationRule({ ...row, isEnabled: row.isEnabled === 1 ? 0 : 1 }).then(() => {
        this.$modal.msgSuccess('已更新')
        this.getRuleList()
      })
    },
    handleAccrue() {
      this.$prompt('请输入计提月份', '手动计提折旧', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        inputPattern: /^\d{4}-\d{2}$/,
        inputErrorMessage: '格式为 yyyy-MM，如 2026-08'
      }).then(({ value }) => {
        return accrueDepreciation(value)
      }).then(() => {
        this.getRecordList()
        this.$modal.msgSuccess('计提完成')
      }).catch(() => {})
    }
  }
}
</script>
