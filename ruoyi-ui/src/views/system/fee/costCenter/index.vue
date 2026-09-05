<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="中心编码" prop="centerCode">
        <el-input v-model="queryParams.centerCode" placeholder="请输入成本中心编码" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="中心名称" prop="centerName">
        <el-input v-model="queryParams.centerName" placeholder="请输入成本中心名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="启用" value="ENABLED" />
          <el-option label="停用" value="DISABLED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['fee:costCenter:add']">新增</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="costCenterList">
      <el-table-column label="ID" align="center" prop="id" width="80" />
      <el-table-column label="中心编码" align="center" prop="centerCode" width="150" />
      <el-table-column label="中心名称" align="center" prop="centerName" min-width="200" />
      <el-table-column label="默认会计大类" align="center" prop="defaultAccountingClass" width="130">
        <template slot-scope="scope">
          <el-tag size="small">{{ scope.row.defaultAccountingClass || 'EXPENSE' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="待分摊中心" align="center" prop="isPending" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.isPending === 1 ? 'warning' : 'info'" size="small">
            {{ scope.row.isPending === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 'ENABLED' ? 'success' : 'danger'" size="small">
            {{ scope.row.status === 'ENABLED' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['fee:costCenter:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['fee:costCenter:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 添加或修改成本中心对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="500px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="中心编码" prop="centerCode">
          <el-input v-model="form.centerCode" placeholder="请输入成本中心编码" :disabled="form.id != null" />
        </el-form-item>
        <el-form-item label="中心名称" prop="centerName">
          <el-input v-model="form.centerName" placeholder="请输入成本中心名称" />
        </el-form-item>
        <el-form-item label="默认会计大类" prop="defaultAccountingClass">
          <el-select v-model="form.defaultAccountingClass" placeholder="请选择默认会计大类">
            <el-option label="费用" value="EXPENSE" />
            <el-option label="资产" value="ASSET" />
            <el-option label="成本" value="COST" />
          </el-select>
        </el-form-item>
        <el-form-item label="待分摊中心" prop="isPending">
          <el-radio-group v-model="form.isPending">
            <el-radio :label="0">否</el-radio>
            <el-radio :label="1">是</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="ENABLED">启用</el-radio>
            <el-radio label="DISABLED">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listCostCenter, getCostCenter, addCostCenter, updateCostCenter, delCostCenter } from '@/api/system/fee/costCenter'

export default {
  name: 'CostCenter',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      costCenterList: [],
      title: '',
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        centerCode: null,
        centerName: null,
        status: null
      },
      form: {},
      rules: {
        centerCode: [{ required: true, message: '中心编码不能为空', trigger: 'blur' }],
        centerName: [{ required: true, message: '中心名称不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询成本中心列表 */
    getList() {
      this.loading = true
      listCostCenter(this.queryParams).then(response => {
        this.costCenterList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    cancel() {
      this.open = false
      this.reset()
    },
    reset() {
      this.form = {
        id: null,
        centerCode: null,
        centerName: null,
        defaultAccountingClass: 'EXPENSE',
        isPending: 0,
        status: 'ENABLED'
      }
      this.resetForm('form')
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '添加成本中心'
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.reset()
      const id = row.id
      getCostCenter(id).then(response => {
        this.form = response.data
        this.open = true
        this.title = '修改成本中心'
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateCostCenter(this.form).then(() => {
              this.$modal.msgSuccess('修改成功')
              this.open = false
              this.getList()
            })
          } else {
            addCostCenter(this.form).then(() => {
              this.$modal.msgSuccess('新增成功')
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      this.$modal.confirm('是否确认删除成本中心"' + row.centerName + '"？').then(() => {
        return delCostCenter(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>
