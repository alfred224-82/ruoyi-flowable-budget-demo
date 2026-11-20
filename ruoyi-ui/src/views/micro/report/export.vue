<template>
  <div class="app-container">
    <el-card shadow="never">
      <div slot="header"><span>收支明细导出</span></div>
      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="mb12"
        title="导出当前租户的 cash_flow 记账流水（收入/支出），生成 Excel 文件。可按流水类型、收支分类、来源类型过滤；导出范围为符合过滤条件的全部日期数据。"
      />
      <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="90px">
        <el-form-item label="流水类型" prop="flowType">
          <el-select v-model="queryParams.flowType" placeholder="全部" clearable style="width: 160px;">
            <el-option label="收入" value="income" />
            <el-option label="支出" value="expense" />
          </el-select>
        </el-form-item>
        <el-form-item label="收支分类" prop="category">
          <el-input v-model="queryParams.category" placeholder="请输入分类" clearable style="width: 160px;" />
        </el-form-item>
        <el-form-item label="来源类型" prop="relatedType">
          <el-select v-model="queryParams.relatedType" placeholder="全部" clearable style="width: 160px;">
            <el-option label="预约订单" value="booking" />
            <el-option label="人员结算" value="settlement" />
            <el-option label="手动补录" value="manual" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="warning" plain icon="el-icon-download" @click="handleExport" v-hasPermi="['micro:report:export']">导出 Excel</el-button>
          <el-button icon="el-icon-refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
export default {
  name: 'MicroReportExport',
  data() {
    return {
      queryParams: { flowType: null, category: null, relatedType: null }
    }
  },
  methods: {
    resetQuery() {
      this.queryParams = { flowType: null, category: null, relatedType: null }
    },
    handleExport() {
      const params = {}
      if (this.queryParams.flowType) {
        params.flowType = this.queryParams.flowType
      }
      if (this.queryParams.category) {
        params.category = this.queryParams.category
      }
      if (this.queryParams.relatedType) {
        params.relatedType = this.queryParams.relatedType
      }
      this.download('micro/report/export', params, `收支明细_${new Date().getTime()}.xlsx`)
    }
  }
}
</script>

<style scoped>
.mb12 { margin-bottom: 12px; }
</style>
