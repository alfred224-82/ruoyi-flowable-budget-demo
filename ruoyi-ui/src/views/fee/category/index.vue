<template>
  <div class="app-container">
    <el-alert
      title="费用大类为预算编制与报销管控的上层限额；预算科目→大类映射B为系统内置固定（见预算科目管理）；默认拆分比例用于编制时无历史实绩的大类额度拆分（合计须=100%）。"
      type="info" :closable="false" style="margin-bottom: 10px;" />

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['fee:category:add']">新增大类</el-button>
      </el-col>
      <right-toolbar :showSearch="false" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="categoryList">
      <el-table-column label="大类编码" align="center" prop="categoryCode" width="140" />
      <el-table-column label="大类名称" align="center" prop="categoryName" min-width="140" />
      <el-table-column label="内置" align="center" prop="isBuiltin" width="80">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.isBuiltin === 1" type="warning" size="small">内置</el-tag>
          <el-tag v-else size="small">自建</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sortOrder" width="80" />
      <el-table-column label="是否启用" align="center" prop="isActive" width="110">
        <template slot-scope="scope">
          <el-switch :value="scope.row.isActive === 1" @change="toggleActive(scope.row)" v-hasPermi="['fee:category:edit']" />
        </template>
      </el-table-column>
      <el-table-column label="备注" align="center" prop="remark" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="220">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-s-operation" @click="handleRatio(scope.row)" v-hasPermi="['fee:category:list']">拆分比例</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(scope.row)" v-hasPermi="['fee:category:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['fee:category:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/修改大类 -->
    <el-dialog :title="form.id ? '修改费用大类' : '新增费用大类'" :visible.sync="open" width="520px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="大类编码" prop="categoryCode">
          <el-input v-model="form.categoryCode" placeholder="如 TRAVEL（建议大写英文）" :disabled="form.isBuiltin === 1" />
          <div v-if="form.isBuiltin === 1" class="el-form-item__tip">内置大类编码固定不可修改（映射B依赖）</div>
        </el-form-item>
        <el-form-item label="大类名称" prop="categoryName">
          <el-input v-model="form.categoryName" placeholder="如 差旅招待" />
        </el-form-item>
        <el-form-item label="排序号" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="是否启用" prop="isActive">
          <el-switch :value="form.isActive === 1" @change="v => (form.isActive = v ? 1 : 0)" />
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="open = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 默认拆分比例设置（财务维护，支撑 T5.14 编制拆分） -->
    <el-dialog :title="'默认拆分比例 - ' + ratioCategory.categoryName" :visible.sync="ratioOpen" width="720px" append-to-body>
      <el-alert title="无历史实绩时编制预算按此比例将大类额度拆分到科目；比例合计必须等于 100%。" type="warning" :closable="false" style="margin-bottom: 10px;" />
      <el-table :data="ratioList" border size="small">
        <el-table-column label="预算科目" align="center" min-width="280">
          <template slot-scope="scope">
            <el-select v-model="scope.row.subjectCode" placeholder="请选择叶子科目" filterable style="width: 100%;" @change="val => onSubjectChange(scope.row, val)">
              <el-option v-for="s in leafSubjects" :key="s.subjectCode" :label="s.subjectCode + ' ' + s.subjectName" :value="s.subjectCode" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="拆分比例(%)" align="center" width="160">
          <template slot-scope="scope">
            <el-input-number v-model="scope.row.ratioPercent" :min="0.01" :max="100" :precision="2" :controls="false" style="width: 120px;" />
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="80">
          <template slot-scope="scope">
            <el-button size="mini" type="text" icon="el-icon-delete" @click="ratioList.splice(scope.$index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div style="margin-top: 10px; display: flex; justify-content: space-between; align-items: center;">
        <el-button type="primary" plain size="mini" icon="el-icon-plus" @click="addRatioRow">添加科目</el-button>
        <span>合计：<b :style="{ color: ratioTotalOk ? '#67C23A' : '#F56C6C' }">{{ ratioTotal }}%</b>（须等于 100%）</span>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitRatio" v-hasPermi="['fee:categoryRatio:edit']">保 存</el-button>
        <el-button @click="ratioOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listCategory, addCategory, updateCategory, delCategory, listCategoryRatio, saveCategoryRatio, listAllSubject } from '@/api/system/fee/fundCategory'

export default {
  name: 'FeeCategory',
  data() {
    return {
      loading: true,
      categoryList: [],
      open: false,
      form: {},
      rules: {
        categoryCode: [{ required: true, message: '大类编码不能为空', trigger: 'blur' }],
        categoryName: [{ required: true, message: '大类名称不能为空', trigger: 'blur' }]
      },
      ratioOpen: false,
      ratioCategory: {},
      ratioList: [],
      subjects: []
    }
  },
  computed: {
    leafSubjects() {
      return (this.subjects || []).filter(s => s.isLeaf === 1)
    },
    ratioTotal() {
      return this.ratioList.reduce((sum, r) => sum + Number(r.ratioPercent || 0), 0).toFixed(2)
    },
    ratioTotalOk() {
      return Number(this.ratioTotal) === 100
    }
  },
  created() {
    this.getList()
    listAllSubject().then(res => { this.subjects = res.data || [] })
  },
  methods: {
    getList() {
      this.loading = true
      listCategory().then(res => {
        this.categoryList = res.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleAdd() {
      this.form = { categoryCode: null, categoryName: null, sortOrder: 0, isActive: 1, remark: null }
      this.open = true
    },
    handleEdit(row) {
      this.form = { ...row }
      this.open = true
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) return
        const action = this.form.id ? updateCategory : addCategory
        action(this.form).then(() => {
          this.$modal.msgSuccess('保存成功')
          this.open = false
          this.getList()
        })
      })
    },
    toggleActive(row) {
      updateCategory({ ...row, isActive: row.isActive === 1 ? 0 : 1 }).then(() => {
        this.$modal.msgSuccess('已更新')
        this.getList()
      })
    },
    handleDelete(row) {
      if (row.isBuiltin === 1) {
        this.$modal.msgWarning('内置大类不可删除，可停用代替')
        return
      }
      this.$modal.confirm('确认删除费用大类「' + row.categoryName + '」？其默认拆分比例将一并删除。').then(() => {
        return delCategory(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleRatio(row) {
      this.ratioCategory = row
      this.ratioList = []
      this.ratioOpen = true
      listCategoryRatio(row.id).then(res => {
        this.ratioList = (res.data || []).map(r => ({
          subjectCode: r.subjectCode,
          subjectName: r.subjectName,
          ratioPercent: Number(r.ratioPercent)
        }))
      })
    },
    onSubjectChange(row, subjectCode) {
      const subject = this.subjects.find(s => s.subjectCode === subjectCode)
      row.subjectName = subject ? subject.subjectName : null
    },
    addRatioRow() {
      this.ratioList.push({ subjectCode: null, subjectName: null, ratioPercent: null })
    },
    submitRatio() {
      if (this.ratioList.length > 0 && !this.ratioTotalOk) {
        this.$modal.msgError('拆分比例合计必须等于100%')
        return
      }
      saveCategoryRatio(this.ratioCategory.id, this.ratioList).then(() => {
        this.$modal.msgSuccess('保存成功')
        this.ratioOpen = false
      })
    }
  }
}
</script>
