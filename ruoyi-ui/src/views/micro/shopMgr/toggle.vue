<template>
  <div class="app-container">
    <el-alert
      title="服务项目上下架（T15.24，perms micro:service:toggle）"
      type="info"
      description="下架的项目不再出现在顾客端「浏览店铺」与预约选择中，但历史订单与结算数据不受影响。上架即时生效，无需重新发布小程序。"
      show-icon
      :closable="false"
      class="mb8"
    />

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="项目名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入服务项目名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="所属分类" prop="categoryId">
        <el-select v-model="queryParams.categoryId" placeholder="请选择分类" clearable style="width:160px;">
          <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width:120px;">
          <el-option label="上架" value="0" />
          <el-option label="下架" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-top" size="mini" :disabled="multiple" @click="handleBatch('0')" v-hasPermi="['micro:service:toggle']">批量上架</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" plain icon="el-icon-bottom" size="mini" :disabled="multiple" @click="handleBatch('1')" v-hasPermi="['micro:service:toggle']">批量下架</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="serviceList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="服务项目" align="left" prop="name" min-width="160" show-overflow-tooltip />
      <el-table-column label="所属分类" align="center" prop="categoryName" width="120">
        <template slot-scope="scope"><span>{{ scope.row.categoryName || '未分类' }}</span></template>
      </el-table-column>
      <el-table-column label="单价" align="center" prop="price" width="110">
        <template slot-scope="scope"><span style="color:#f56c6c;font-weight:600;">￥{{ scope.row.price }}</span></template>
      </el-table-column>
      <el-table-column label="单位" align="center" prop="unit" width="80" />
      <el-table-column label="顾客端可见" align="center" width="130">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.status"
            active-value="0"
            inactive-value="1"
            active-text="上架"
            inactive-text="下架"
            @change="handleToggle(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sortOrder" width="80" />
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />
  </div>
</template>

<script>
import { listService, toggleService } from '@/api/micro/service'
import { enabledCategory } from '@/api/micro/category'

export default {
  name: 'MicroShopToggle',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      serviceList: [],
      categoryOptions: [],
      selectedRows: [],
      multiple: true,
      queryParams: { pageNum: 1, pageSize: 10, name: null, categoryId: null, status: null }
    }
  },
  created() {
    this.getList()
    this.getCategoryOptions()
  },
  methods: {
    getList() {
      this.loading = true
      listService(this.queryParams).then(response => {
        this.serviceList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    getCategoryOptions() {
      enabledCategory().then(response => {
        this.categoryOptions = response.data || []
      }).catch(() => {})
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.selectedRows = selection
      this.multiple = !selection.length
    },
    handleToggle(row) {
      const text = row.status === '0' ? '上架' : '下架'
      this.$modal.confirm('确认' + text + '服务项目「' + row.name + '」吗？')
        .then(() => toggleService(row.id, row.status))
        .then(() => { this.$modal.msgSuccess(text + '成功') })
        .catch(() => { row.status = row.status === '0' ? '1' : '0' })
    },
    /** 批量上下架：串行调用单条 toggle 接口（后端未提供批量端点），逐条容错不中断 */
    handleBatch(status) {
      const text = status === '0' ? '上架' : '下架'
      const rows = this.selectedRows.filter(row => row.status !== status)
      if (!rows.length) {
        this.$modal.msgWarning('选中项目均已' + text)
        return
      }
      this.$modal.confirm('确认批量' + text + ' ' + rows.length + ' 个服务项目吗？').then(() => {
        const tasks = rows.map(row => toggleService(row.id, status).then(() => true).catch(() => false))
        return Promise.all(tasks).then(results => {
          const ok = results.filter(item => item).length
          if (ok === results.length) {
            this.$modal.msgSuccess('批量' + text + '成功，共 ' + ok + ' 项')
          } else {
            this.$modal.msgWarning('批量' + text + '完成：成功 ' + ok + ' 项，失败 ' + (results.length - ok) + ' 项')
          }
          this.getList()
        })
      }).catch(() => {})
    }
  }
}
</script>
