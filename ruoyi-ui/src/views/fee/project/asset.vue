<template>
  <div class="app-container">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="资产台账" name="asset">
        <el-row :gutter="10" class="mb8">
          <el-col :span="1.5">
            <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['fee:asset:edit']">登记资产</el-button>
          </el-col>
          <el-col :span="1.5">
            <el-button type="warning" plain icon="el-icon-time" size="mini" @click="handleAccrue" v-hasPermi="['fee:asset:edit']">手动计提折旧</el-button>
          </el-col>
        </el-row>

        <el-table v-loading="loading" :data="assetList">
          <el-table-column label="资产名称" align="left" prop="assetName" min-width="140" show-overflow-tooltip />
          <el-table-column label="分类" align="center" prop="category" width="100">
            <template slot-scope="scope">{{ categoryText(scope.row.category) }}</template>
          </el-table-column>
          <el-table-column label="原值" align="right" prop="originalValue" width="100" />
          <el-table-column label="残值" align="right" prop="residualValue" width="90" />
          <el-table-column label="使用月数" align="center" prop="lifeMonths" width="80" />
          <el-table-column label="月折旧额" align="right" prop="monthlyDepreciation" width="100" />
          <el-table-column label="已折月数" align="center" prop="depreciatedMonths" width="80" />
          <el-table-column label="净值" align="right" prop="netValue" width="100" />
          <el-table-column label="归属" align="center" prop="attributionType" width="120">
            <template slot-scope="scope">
              {{ scope.row.attributionType === 'PROJECT' ? '项目：' + (scope.row.projectName || scope.row.projectId) : '公司级' }}
            </template>
          </el-table-column>
          <el-table-column label="状态" align="center" prop="status" width="80">
            <template slot-scope="scope">
              <el-tag :type="scope.row.status === 'ACTIVE' ? 'success' : 'info'" size="small">{{ scope.row.status === 'ACTIVE' ? '在用' : '已报废' }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
            <template slot-scope="scope">
              <el-button v-if="scope.row.status === 'ACTIVE'" size="mini" type="text" icon="el-icon-edit" @click="handleEdit(scope.row)" v-hasPermi="['fee:asset:edit']">修改</el-button>
              <el-button v-if="scope.row.status === 'ACTIVE'" size="mini" type="text" icon="el-icon-delete" @click="handleScrap(scope.row)" v-hasPermi="['fee:asset:edit']">报废</el-button>
            </template>
          </el-table-column>
        </el-table>
        <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
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

    <!-- 登记/修改资产 -->
    <el-dialog :title="form.id ? '修改资产' : '登记资产（月折旧额自动计算）'" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="资产名称" prop="assetName">
          <el-input v-model="form.assetName" placeholder="请输入资产名称" />
        </el-form-item>
        <el-form-item label="资产分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%;">
            <el-option label="电子设备" value="ELECTRONIC" />
            <el-option label="家具" value="FURNITURE" />
            <el-option label="车辆" value="VEHICLE" />
            <el-option label="办公设备" value="OFFICE" />
          </el-select>
        </el-form-item>
        <el-form-item label="原值" prop="originalValue">
          <el-input-number v-model="form.originalValue" :min="0" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="残值" prop="residualValue">
          <el-input-number v-model="form.residualValue" :min="0" :precision="2" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="使用年限(月)" prop="lifeMonths">
          <el-input-number v-model="form.lifeMonths" :min="1" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="归属" prop="attributionType">
          <el-radio-group v-model="form.attributionType">
            <el-radio label="COMPANY">公司级</el-radio>
            <el-radio label="PROJECT">项目分摊</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="form.attributionType === 'PROJECT'" label="分摊项目" prop="projectId">
          <el-select v-model="form.projectId" placeholder="请选择项目" filterable style="width: 100%;">
            <el-option v-for="p in activeProjects" :key="p.id" :label="p.projectName" :value="p.id" />
          </el-select>
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
import { listAsset, saveAsset, scrapAsset, listDepreciation, accrueDepreciation } from '@/api/system/fee/fundAssetDepr'
import { listActiveProject } from '@/api/system/fee/fundProject'

export default {
  name: 'FundAsset',
  data() {
    return {
      loading: true,
      recordLoading: false,
      activeTab: 'asset',
      total: 0,
      recordTotal: 0,
      assetList: [],
      recordList: [],
      activeProjects: [],
      open: false,
      queryParams: { pageNum: 1, pageSize: 10 },
      recordQuery: { pageNum: 1, pageSize: 10, accrualMonth: null },
      form: {},
      rules: {
        assetName: [{ required: true, message: '资产名称不能为空', trigger: 'blur' }],
        category: [{ required: true, message: '请选择分类', trigger: 'change' }],
        originalValue: [{ required: true, message: '原值不能为空', trigger: 'blur' }],
        lifeMonths: [{ required: true, message: '使用年限不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
    this.getRecordList()
    listActiveProject().then(res => { this.activeProjects = res.data || [] })
  },
  methods: {
    getList() {
      this.loading = true
      listAsset(this.queryParams).then(response => {
        this.assetList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
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
    handleAdd() {
      this.form = { assetName: null, category: 'ELECTRONIC', originalValue: null, residualValue: 0, lifeMonths: 36, attributionType: 'COMPANY', projectId: null }
      this.open = true
    },
    handleEdit(row) {
      this.form = { ...row }
      this.open = true
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          saveAsset(this.form).then(() => {
            this.$modal.msgSuccess('保存成功')
            this.open = false
            this.getList()
          })
        }
      })
    },
    handleScrap(row) {
      this.$modal.confirm('报废后该资产停止折旧，确认报废【' + row.assetName + '】？').then(() => {
        return scrapAsset(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('已报废')
      }).catch(() => {})
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
        this.getList()
        this.$modal.msgSuccess('计提完成')
      }).catch(() => {})
    }
  }
}
</script>
