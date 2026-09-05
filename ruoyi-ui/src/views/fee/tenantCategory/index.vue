<template>
  <div class="app-container">
    <el-alert
      title="从管理员定义的费用大类中勾选本租户需要启用的大类；未勾选的大类在预算编制和报销管控中将不可见。"
      type="info" :closable="false" style="margin-bottom: 10px;" />

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-check" size="mini" :loading="saving" @click="handleSave" v-hasPermi="['fee:tenantCategory:edit']">保存配置</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button plain icon="el-icon-refresh" size="mini" @click="getList">刷新</el-button>
      </el-col>
      <right-toolbar :showSearch="false" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="categoryList" border>
      <el-table-column label="勾选" align="center" width="80">
        <template slot-scope="scope">
          <el-checkbox :value="scope.row.isEnabled === 1" @change="val => toggleCategory(scope.row, val)" />
        </template>
      </el-table-column>
      <el-table-column label="大类编码" align="center" prop="categoryCode" width="160" />
      <el-table-column label="大类名称" align="center" prop="categoryName" min-width="160" />
      <el-table-column label="状态" align="center" width="120">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.isEnabled === 1" type="success" size="small">已启用</el-tag>
          <el-tag v-else type="info" size="small">未启用</el-tag>
        </template>
      </el-table-column>
    </el-table>

    <div style="margin-top: 15px; color: #909399; font-size: 13px;">
      已启用 <b style="color: #409EFF;">{{ enabledCount }}</b> / {{ categoryList.length }} 个大类
    </div>
  </div>
</template>

<script>
import { listTenantCategory, saveTenantCategories } from '@/api/system/fee/fundTenantCategory'

export default {
  name: 'FeeTenantCategory',
  data() {
    return {
      loading: true,
      saving: false,
      categoryList: []
    }
  },
  computed: {
    enabledCount() {
      return this.categoryList.filter(c => c.isEnabled === 1).length
    },
    enabledCodes() {
      return this.categoryList.filter(c => c.isEnabled === 1).map(c => c.categoryCode)
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listTenantCategory().then(res => {
        this.categoryList = res.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    toggleCategory(row, checked) {
      row.isEnabled = checked ? 1 : 0
    },
    handleSave() {
      if (this.enabledCodes.length === 0) {
        this.$modal.msgWarning('至少需要启用一个大类')
        return
      }
      this.saving = true
      saveTenantCategories(this.enabledCodes).then(() => {
        this.$modal.msgSuccess('保存成功')
        this.saving = false
      }).catch(() => { this.saving = false })
    }
  }
}
</script>
