<template>
  <div class="app-container">
    <el-alert title="项目关联规则（三层）：角色级硬规则 > 大类级软提示 > 财务兜底。未选项目的报销单默认归入公司级（未归类池）。" type="info" :closable="false" style="margin-bottom: 10px;" />

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['fee:linkRule:edit']">新增规则</el-button>
      </el-col>
      <right-toolbar :showSearch="false" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="ruleList">
      <el-table-column label="作用域" align="center" prop="scope" width="120">
        <template slot-scope="scope">
          <el-tag :type="scopeTag(scope.row.scope)" size="small">{{ scopeText(scope.row.scope) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="目标" align="center" prop="target" min-width="160" show-overflow-tooltip>
        <template slot-scope="scope">{{ targetText(scope.row) }}</template>
      </el-table-column>
      <el-table-column label="规则" align="center" prop="rule" width="160">
        <template slot-scope="scope">{{ ruleText(scope.row.rule) }}</template>
      </el-table-column>
      <el-table-column label="是否启用" align="center" prop="isEnabled" width="120">
        <template slot-scope="scope">
          <el-switch :value="scope.row.isEnabled === 1" @change="toggleRule(scope.row)" v-hasPermi="['fee:linkRule:edit']" />
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleEdit(scope.row)" v-hasPermi="['fee:linkRule:edit']">修改</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['fee:linkRule:edit']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/修改规则 -->
    <el-dialog :title="form.id ? '修改关联规则' : '新增关联规则'" :visible.sync="open" width="520px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="作用域" prop="scope">
          <el-select v-model="form.scope" placeholder="请选择作用域" style="width: 100%;">
            <el-option label="角色级（硬规则）" value="ROLE" />
            <el-option label="大类级（软提示）" value="CATEGORY" />
            <el-option label="项目级" value="PROJECT" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标" prop="target">
          <template v-if="form.scope === 'ROLE'">
            <el-select v-model="form.target" placeholder="请选择角色" filterable style="width: 100%;">
              <el-option v-for="r in roles" :key="r.roleKey" :label="r.roleName" :value="r.roleKey" />
            </el-select>
          </template>
          <template v-else-if="form.scope === 'PROJECT'">
            <el-select v-model="form.target" placeholder="请选择项目" filterable style="width: 100%;">
              <el-option v-for="p in activeProjects" :key="p.id" :label="p.projectName" :value="String(p.id)" />
            </el-select>
          </template>
          <template v-else>
            <el-input v-model="form.target" placeholder="费用大类ID" />
          </template>
        </el-form-item>
        <el-form-item label="规则" prop="rule">
          <el-radio-group v-model="form.rule">
            <el-radio label="REQUIRED">必须关联项目</el-radio>
            <el-radio label="OPTIONAL">软提示可跳过</el-radio>
            <el-radio label="NO_PROMPT">不提示</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="是否启用" prop="isEnabled">
          <el-switch :value="form.isEnabled === 1" @change="v => (form.isEnabled = v ? 1 : 0)" />
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
import { listLinkRule, saveLinkRule, delLinkRule } from '@/api/system/fee/fundLinkRule'
import { listActiveProject } from '@/api/system/fee/fundProject'
import { allRoleExcludeAdmin as listRoleOption } from '@/api/system/role'

export default {
  name: 'FundLinkRule',
  data() {
    return {
      loading: true,
      ruleList: [],
      roles: [],
      activeProjects: [],
      open: false,
      form: {},
      rules: {
        scope: [{ required: true, message: '请选择作用域', trigger: 'change' }],
        target: [{ required: true, message: '目标不能为空', trigger: 'change' }],
        rule: [{ required: true, message: '请选择规则', trigger: 'change' }]
      }
    }
  },
  created() {
    this.getList()
    listActiveProject().then(res => { this.activeProjects = res.data || [] })
    listRoleOption().then(res => { this.roles = res.data || [] })
  },
  methods: {
    getList() {
      this.loading = true
      listLinkRule().then(res => {
        this.ruleList = res.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    scopeText(scope) {
      const map = { ROLE: '角色级', CATEGORY: '大类级', PROJECT: '项目级' }
      return map[scope] || scope
    },
    scopeTag(scope) {
      const map = { ROLE: 'danger', CATEGORY: 'warning', PROJECT: 'success' }
      return map[scope] || 'info'
    },
    ruleText(rule) {
      const map = { REQUIRED: '必须关联项目', OPTIONAL: '软提示可跳过', NO_PROMPT: '不提示' }
      return map[rule] || rule
    },
    targetText(row) {
      if (row.scope === 'ROLE') {
        const role = this.roles.find(r => r.roleKey === row.target)
        return role ? role.roleName : row.target
      }
      if (row.scope === 'PROJECT') {
        const project = this.activeProjects.find(p => String(p.id) === String(row.target))
        return project ? project.projectName : row.target
      }
      return '大类ID：' + row.target
    },
    handleAdd() {
      this.form = { scope: 'ROLE', target: null, rule: 'REQUIRED', isEnabled: 1 }
      this.open = true
    },
    handleEdit(row) {
      this.form = { ...row }
      this.open = true
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          saveLinkRule(this.form).then(() => {
            this.$modal.msgSuccess('保存成功')
            this.open = false
            this.getList()
          })
        }
      })
    },
    toggleRule(row) {
      saveLinkRule({ ...row, isEnabled: row.isEnabled === 1 ? 0 : 1 }).then(() => {
        this.$modal.msgSuccess('已更新')
        this.getList()
      })
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除该关联规则？').then(() => {
        return delLinkRule(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>
