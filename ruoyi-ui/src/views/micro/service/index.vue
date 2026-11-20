<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="项目名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入服务项目名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="所属分类" prop="categoryId">
        <el-select v-model="queryParams.categoryId" placeholder="请选择分类" clearable>
          <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['micro:service:add']">新增项目</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="serviceList">
      <el-table-column label="服务项目" align="left" prop="name" min-width="140" show-overflow-tooltip />
      <el-table-column label="所属分类" align="center" prop="categoryName" width="110">
        <template slot-scope="scope"><span>{{ scope.row.categoryName || '未分类' }}</span></template>
      </el-table-column>
      <el-table-column label="单价" align="center" prop="price" width="100">
        <template slot-scope="scope"><span style="color:#f56c6c;font-weight:600;">￥{{ scope.row.price }}</span></template>
      </el-table-column>
      <el-table-column label="单位" align="center" prop="unit" width="70" />
      <el-table-column label="计价方式" align="center" prop="pricingType" width="100">
        <template slot-scope="scope"><span>{{ pricingTypeText(scope.row.pricingType) }}</span></template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sortOrder" width="70" />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="small">{{ scope.row.status === '0' ? '上架' : '下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="220">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['micro:service:edit']">编辑</el-button>
          <el-button size="mini" type="text" :icon="scope.row.status === '0' ? 'el-icon-bottom' : 'el-icon-top'" @click="handleToggle(scope.row)" v-hasPermi="['micro:service:toggle']">{{ scope.row.status === '0' ? '下架' : '上架' }}</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['micro:service:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑服务项目 -->
    <el-dialog :title="title" :visible.sync="open" width="620px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="项目名称" prop="name">
              <el-input v-model="form.name" placeholder="如：标准间 / 初级助教 / 白菜" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="请选择分类" clearable style="width:100%;">
                <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="单价" prop="price">
              <el-input-number v-model="form.price" :min="0" :precision="2" :step="1" controls-position="right" style="width:100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="计价方式" prop="pricingType">
              <el-select v-model="form.pricingType" placeholder="请选择计价方式" style="width:100%;">
                <el-option v-for="(label, val) in pricingTypeMap" :key="val" :label="label" :value="val" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-select v-model="form.unit" placeholder="请选择单位" filterable allow-create default-first-option style="width:100%;">
                <el-option v-for="u in unitOptions" :key="u" :label="u" :value="u" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序" prop="sortOrder">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width:100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">上架</el-radio>
            <el-radio label="1">下架</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="项目描述" prop="description">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="项目描述" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
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
import { listService, getService, addService, updateService, toggleService, delService } from '@/api/micro/service'
import { enabledCategory } from '@/api/micro/category'

export default {
  name: 'MicroService',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      serviceList: [],
      categoryOptions: [],
      unitOptions: ['小时', '斤', '节', '人', '次', '份', '间', '个', '桌'],
      pricingTypeMap: { duration: '计时', package: '套餐', times: '计量', fixed: '一口价', weight: '按重量', person: '按人', portion: '按份' },
      open: false,
      title: '',
      queryParams: { pageNum: 1, pageSize: 10, name: null, categoryId: null, status: null },
      form: {},
      rules: {
        name: [{ required: true, message: '服务项目名称不能为空', trigger: 'blur' }],
        price: [{ required: true, message: '单价不能为空', trigger: 'blur' }]
      }
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
      })
    },
    pricingTypeText(val) {
      return this.pricingTypeMap[val] || val || '按次'
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
      this.form = { id: null, categoryId: null, name: null, price: null, unit: null, pricingType: 'times', description: null, sortOrder: 0, status: '0', remark: null }
      this.resetForm('form')
    },
    cancel() {
      this.open = false
      this.reset()
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增服务项目'
    },
    handleUpdate(row) {
      this.reset()
      getService(row.id).then(response => {
        this.form = response.data
        this.open = true
        this.title = '编辑服务项目'
      })
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateService(this.form).then(() => {
              this.$modal.msgSuccess('修改成功')
              this.open = false
              this.getList()
            })
          } else {
            addService(this.form).then(() => {
              this.$modal.msgSuccess('新增成功')
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleToggle(row) {
      const target = row.status === '0' ? '1' : '0'
      const text = row.status === '0' ? '下架' : '上架'
      this.$modal.confirm('确认' + text + '服务项目「' + row.name + '」？').then(() => {
        return toggleService(row.id, target)
      }).then(() => {
        this.$modal.msgSuccess(text + '成功')
        this.getList()
      }).catch(() => {})
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除服务项目「' + row.name + '」？').then(() => {
        return delService(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>
