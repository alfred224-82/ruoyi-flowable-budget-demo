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
            <el-form-item label="项目经理" prop="projectManagerName">
              <el-input v-model="form.projectManagerName" placeholder="项目经理姓名" />
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
              <el-input v-model="scope.row.categoryName" placeholder="如：差旅招待" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="预算科目" min-width="160">
            <template slot-scope="scope">
              <el-input v-model="scope.row.subjectName" placeholder="科目名称" size="small" />
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
          <el-table-column label="用户ID" width="140">
            <template slot-scope="scope">
              <el-input-number v-model="scope.row.userId" :min="1" :precision="0" size="small" style="width: 100%;" />
            </template>
          </el-table-column>
          <el-table-column label="姓名" min-width="140">
            <template slot-scope="scope">
              <el-input v-model="scope.row.userName" placeholder="姓名" size="small" />
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
          <el-table-column label="投入比例%" width="130">
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
import { addProject } from '@/api/system/fee/fundProject'

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
      rules: {
        projectName: [{ required: true, message: '项目名称不能为空', trigger: 'blur' }],
        contractAmount: [{ required: true, message: '合同额不能为空', trigger: 'blur' }]
      }
    }
  },
  methods: {
    addSubject() {
      this.form.subjects.push({ categoryName: null, subjectName: null, budgetAmount: null, isActive: 1 })
    },
    addMember() {
      this.form.members.push({ userId: null, userName: null, role: 'MEMBER', investRatio: 100 })
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
