<template>
  <div class="app-container">
    <el-card shadow="never">
      <div slot="header"><span>立项（老板建 / 财务代建 / 项目经理申请，提交后待老板审批）</span></div>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px" style="max-width: 860px;">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="项目名称" prop="projectName">
              <el-input v-model="form.projectName" placeholder="请输入项目名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户名称" prop="clientName">
              <el-input v-model="form.clientName" placeholder="请输入客户名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="初始合同额" prop="contractAmount">
              <el-input-number v-model="form.contractAmount" :min="0" :precision="2" style="width: 100%;" placeholder="元" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="项目经理" prop="projectManagerId">
              <el-select v-model="form.projectManagerId" placeholder="请选择项目经理" filterable clearable style="width: 100%;" @change="onManagerChange">
                <el-option v-for="u in managerOptions" :key="u.userId" :label="u.nickName" :value="u.userId" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="开始日期" prop="startDate">
              <el-date-picker v-model="form.startDate" type="date" value-format="yyyy-MM-dd" placeholder="选择开始日期" style="width: 100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="结束日期" prop="endDate">
              <el-date-picker v-model="form.endDate" type="date" value-format="yyyy-MM-dd" placeholder="选择结束日期" style="width: 100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
        </el-form-item>

        <!-- 项目预算（按大类分配） -->
        <el-divider content-position="left">项目预算（按大类分配）</el-divider>
        <el-row :gutter="10" class="mb8">
          <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="addSubject">追加大类</el-button>
        </el-row>
        <el-table :data="form.subjects" border size="small">
          <el-table-column label="费用大类" min-width="160">
            <template slot-scope="scope">
              <el-select v-model="scope.row.categoryId" placeholder="请选择费用大类" size="small" filterable clearable style="width: 100%;" @change="val => onCategoryChange(scope.row, val)">
                <el-option v-for="c in categoryOptions" :key="c.id" :label="c.categoryName" :value="c.id" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="预算科目" min-width="160">
            <template slot-scope="scope">
              <el-select v-model="scope.row.budgetSubjectId" placeholder="请选择预算科目" size="small" filterable clearable :disabled="!scope.row.categoryId" style="width: 100%;" @change="val => onSubjectChange(scope.row, val)">
                <el-option v-for="s in subjectOptionsOf(scope.row)" :key="s.id" :label="s.subjectName" :value="s.id" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="预算金额" width="160">
            <template slot-scope="scope">
              <el-input-number v-model="scope.row.budgetAmount" :min="0" :precision="2" size="small" style="width: 100%;" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template slot-scope="scope">
              <el-button size="mini" type="text" icon="el-icon-delete" @click="form.subjects.splice(scope.$index, 1)" />
            </template>
          </el-table-column>
        </el-table>

        <!-- 项目成员 -->
        <el-divider content-position="left">项目成员</el-divider>
        <el-row :gutter="10" class="mb8">
          <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="addMember">追加成员</el-button>
        </el-row>
        <el-table :data="form.members" border size="small">
          <el-table-column label="用户ID" width="140" align="center">
            <template slot-scope="scope">
              <span>{{ scope.row.userId || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="姓名" min-width="140">
            <template slot-scope="scope">
              <el-select v-model="scope.row.userId" placeholder="请选择报销人" size="small" filterable clearable style="width: 100%;" @change="val => onMemberChange(scope.row, val)">
                <el-option v-for="u in reimburserOptions" :key="u.userId" :label="u.nickName" :value="u.userId" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="成员角色" width="150">
            <template slot-scope="scope">
              <el-select v-model="scope.row.role" size="small">
                <el-option label="项目经理" value="MANAGER" />
                <el-option label="成员" value="MEMBER" />
                <el-option label="只读" value="VIEWER" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="分成比例%" width="130">
            <template slot-scope="scope">
              <el-input-number v-model="scope.row.investRatio" :min="0" :max="100" size="small" style="width: 100%;" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="80" align="center">
            <template slot-scope="scope">
              <el-button size="mini" type="text" icon="el-icon-delete" @click="form.members.splice(scope.$index, 1)" />
            </template>
          </el-table-column>
        </el-table>

        <el-form-item style="margin-top: 20px;">
          <el-button type="primary" @click="submitForm">提 交</el-button>
          <el-button @click="back">返 回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { addProject, listReimbursers, listManagers } from '@/api/system/fee/fundProject'
import { listActiveCategory, listAllSubject } from '@/api/system/fee/fundCategory'

export default {
  name: 'FundProjectCreate',
  data() {
    return {
      form: {
        projectName: null,
        clientName: null,
        contractAmount: null,
        projectManagerId: null,
        projectManagerName: null,
        startDate: null,
        endDate: null,
        remark: null,
        subjects: [],
        members: []
      },
      // 费用大类下拉（admin 定义 ∩ platform_admin 勾选启用的租户大类）
      categoryOptions: [],
      // 全部启用预算科目（映射B：科目→大类 由 admin 内置固定）
      subjectOptions: [],
      // 报销人(employee)角色用户下拉
      reimburserOptions: [],
      // 项目经理(manager)角色用户下拉
      managerOptions: [],
      rules: {
        projectName: [{ required: true, message: '项目名称不能为空', trigger: 'blur' }],
        contractAmount: [{ required: true, message: '合同额不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.loadOptions()
  },
  methods: {
    loadOptions() {
      listActiveCategory().then(res => { this.categoryOptions = res.data || [] })
      listAllSubject().then(res => { this.subjectOptions = res.data || [] })
      listReimbursers().then(res => { this.reimburserOptions = res.data || [] })
      listManagers().then(res => { this.managerOptions = res.data || [] })
    },
    addSubject() {
      this.form.subjects.push({ categoryId: null, categoryCode: null, categoryName: null, budgetSubjectId: null, subjectName: null, budgetAmount: null, isActive: 1 })
    },
    // 选中大类后回填编码/名称，并重置科目（科目按映射B联动过滤）
    onCategoryChange(row, categoryId) {
      const cat = this.categoryOptions.find(c => c.id === categoryId)
      row.categoryCode = cat ? cat.categoryCode : null
      row.categoryName = cat ? cat.categoryName : null
      row.budgetSubjectId = null
      row.subjectName = null
    },
    // 大类下可用预算科目（映射B：subject.categoryCode = category.categoryCode）
    subjectOptionsOf(row) {
      return this.subjectOptions.filter(s => s.categoryCode === row.categoryCode)
    },
    onSubjectChange(row, subjectId) {
      const subject = this.subjectOptions.find(s => s.id === subjectId)
      row.subjectName = subject ? subject.subjectName : null
    },
    addMember() {
      this.form.members.push({ userId: null, userName: null, role: 'MEMBER', investRatio: 100 })
    },
    // 成员仅限报销人角色人员，选中后回填姓名
    onMemberChange(row, userId) {
      const user = this.reimburserOptions.find(u => u.userId === userId)
      row.userName = user ? user.nickName : null
    },
    // 项目经理仅限 manager 角色人员，选中后回填姓名
    onManagerChange(userId) {
      const user = this.managerOptions.find(u => u.userId === userId)
      this.form.projectManagerName = user ? user.nickName : null
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          addProject(this.form).then(() => {
            this.$modal.msgSuccess('立项成功，等待老板审批')
            this.back()
          })
        }
      })
    },
    back() {
      this.$router.push('/fee/fee/project')
    }
  }
}
</script>
