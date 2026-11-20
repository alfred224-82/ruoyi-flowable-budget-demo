<template>
  <div class="app-container">
    <el-alert
      title="活动留痕查询（T25.6，platform_admin 查看租户操作记录，租户不可删除）"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom:12px;"
    />

    <!-- 搜索栏 -->
    <el-form :inline="true" size="small" class="mb16">
      <el-form-item label="租户ID">
        <el-input v-model="queryParams.tenantId" placeholder="留空查全部" clearable style="width:180px;" />
      </el-form-item>
      <el-form-item label="记录数">
        <el-input-number v-model="queryParams.limit" :min="10" :max="500" :step="10" style="width:140px;" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">查询</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 留痕列表 -->
    <el-table v-loading="loading" :data="tableData" border stripe style="width:100%;">
      <el-table-column label="租户ID" prop="tenant_id" width="100" align="center" />
      <el-table-column label="租户名称" prop="tenant_name" width="180" />
      <el-table-column label="操作类型" prop="action_type" width="140" align="center">
        <template slot-scope="scope">
          <el-tag :type="actionTagType(scope.row.action_type)" size="mini">{{ scope.row.action_type }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作详情" prop="action_detail" min-width="250" show-overflow-tooltip />
      <el-table-column label="操作人" prop="operator_name" width="120" align="center" />
      <el-table-column label="IP地址" prop="operator_ip" width="140" align="center" />
      <el-table-column label="操作时间" prop="created_at" width="170" align="center" />
    </el-table>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'ActivityTrace',
  data() {
    return {
      loading: false,
      tableData: [],
      queryParams: { tenantId: null, limit: 100 }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询留痕 */
    getList() {
      this.loading = true
      const tid = this.queryParams.tenantId ? Number(this.queryParams.tenantId) : 0
      request({
        url: '/system/supervision/activityTrace/' + tid,
        method: 'get',
        params: { limit: this.queryParams.limit }
      }).then(res => {
        this.tableData = res.data || []
      }).finally(() => {
        this.loading = false
      })
    },
    /** 搜索 */
    handleQuery() {
      this.getList()
    },
    /** 重置 */
    resetQuery() {
      this.queryParams = { tenantId: null, limit: 100 }
      this.getList()
    },
    /** 操作类型标签颜色 */
    actionTagType(type) {
      if (!type) return ''
      const t = String(type).toUpperCase()
      if (t.indexOf('STOP') >= 0 || t.indexOf('DELETE') >= 0) return 'danger'
      if (t.indexOf('CREATE') >= 0 || t.indexOf('ISSUE') >= 0) return 'success'
      if (t.indexOf('UPDATE') >= 0 || t.indexOf('RESUME') >= 0) return 'warning'
      return ''
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
</style>
