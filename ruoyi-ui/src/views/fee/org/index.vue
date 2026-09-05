<template>
  <div class="app-container">
    <el-row :gutter="20">
      <!-- 左侧：组织树 -->
      <el-col :span="8" :xs="24">
        <el-card shadow="never" v-loading="treeLoading">
          <div slot="header">
            <span>组织树</span>
            <el-button style="float: right;" type="primary" size="mini" icon="el-icon-plus" @click="handleAddOrg(null)" v-hasPermi="['fee:org:edit']">新增顶级</el-button>
          </div>
          <el-input v-model="treeFilter" placeholder="输入组织名称过滤" clearable size="small" style="margin-bottom: 10px;" />
          <el-tree
            ref="orgTree"
            :data="orgTree"
            :props="{ label: 'orgName', children: 'children' }"
            :filter-node-method="filterNode"
            node-key="id"
            highlight-current
            default-expand-all
            @node-click="handleNodeClick">
            <span class="custom-tree-node" slot-scope="{ node, data }">
              <span>{{ data.orgName }} <el-tag size="mini" type="info">{{ data.userCount || 0 }}人</el-tag></span>
              <span>
                <el-button type="text" size="mini" icon="el-icon-plus" @click.stop="handleAddOrg(data)" v-hasPermi="['fee:org:edit']" />
                <el-button type="text" size="mini" icon="el-icon-edit" @click.stop="handleEditOrg(data)" v-hasPermi="['fee:org:edit']" />
                <el-button type="text" size="mini" icon="el-icon-delete" style="color:#F56C6C;" @click.stop="handleDeleteOrg(data)" v-hasPermi="['fee:org:edit']" />
              </span>
            </span>
          </el-tree>
        </el-card>
      </el-col>

      <!-- 右侧：组织成员 -->
      <el-col :span="16" :xs="24">
        <el-card shadow="never" v-loading="memberLoading">
          <div slot="header">
            <span>组织成员：{{ currentOrg ? currentOrg.orgName : '请选择左侧组织' }}</span>
            <el-button v-if="currentOrg" style="float: right; margin-left: 10px;" type="primary" size="mini" @click="handleSaveUsers" v-hasPermi="['fee:org:edit']">保存成员</el-button>
            <el-button v-if="currentOrg" style="float: right;" type="primary" plain size="mini" icon="el-icon-plus" @click="addUserRow" v-hasPermi="['fee:org:edit']">追加成员</el-button>
          </div>
          <el-table v-if="currentOrg" :data="orgUsers" border size="small">
            <el-table-column label="用户ID" width="160">
              <template slot-scope="scope">
                <el-input-number v-model="scope.row.userId" :min="1" :precision="0" size="small" style="width: 100%;" />
              </template>
            </el-table-column>
            <el-table-column label="姓名" min-width="160">
              <template slot-scope="scope">
                <el-input v-model="scope.row.userName" placeholder="姓名" size="small" />
              </template>
            </el-table-column>
            <el-table-column label="是否主组织" width="140" align="center">
              <template slot-scope="scope">
                <el-switch v-model="scope.row.isPrimary" :active-value="1" :inactive-value="0" />
              </template>
            </el-table-column>
            <el-table-column label="操作" width="80" align="center">
              <template slot-scope="scope">
                <el-button size="mini" type="text" icon="el-icon-delete" @click="orgUsers.splice(scope.$index, 1)" v-hasPermi="['fee:org:edit']" />
              </template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="点击左侧组织节点查看/维护成员" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 新增/修改组织 -->
    <el-dialog :title="orgDialogTitle" :visible.sync="orgDialogVisible" width="500px" append-to-body>
      <el-form ref="orgForm" :model="orgForm" :rules="orgRules" label-width="100px">
        <el-form-item label="上级组织">
          <el-cascader
            v-model="orgForm.parentId"
            :options="parentOptions"
            :props="{ checkStrictly: true, value: 'id', label: 'orgName', children: 'children', emitPath: false }"
            clearable
            placeholder="不选则为顶级组织"
            style="width: 100%;" />
        </el-form-item>
        <el-form-item label="组织编码" prop="orgCode">
          <el-input v-model="orgForm.orgCode" placeholder="如 ORG001" />
        </el-form-item>
        <el-form-item label="组织名称" prop="orgName">
          <el-input v-model="orgForm.orgName" placeholder="请输入组织名称" />
        </el-form-item>
        <el-form-item label="负责人用户ID">
          <el-input-number v-model="orgForm.leaderUserId" :min="1" :precision="0" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="orgForm.sortOrder" :min="0" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="orgForm.status">
            <el-radio label="0">正常</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="orgDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitOrgForm">确 定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getOrgTree, addOrg, updateOrg, delOrg, listOrgUsers, saveOrgUsers } from '@/api/system/fee/fundPro'

export default {
  name: 'FeeOrg',
  data() {
    return {
      treeLoading: true,
      memberLoading: false,
      orgTree: [],
      treeFilter: '',
      currentOrg: null,
      orgUsers: [],
      orgDialogVisible: false,
      orgForm: {},
      orgRules: {
        orgCode: [{ required: true, message: '组织编码不能为空', trigger: 'blur' }],
        orgName: [{ required: true, message: '组织名称不能为空', trigger: 'blur' }]
      }
    }
  },
  computed: {
    orgDialogTitle() {
      return this.orgForm.id ? '修改组织' : '新增组织'
    },
    parentOptions() {
      // 编辑时排除自身及其下级，防止形成环
      if (!this.orgForm.id) return this.orgTree
      const excludeId = this.orgForm.id
      const cut = (nodes) => (nodes || [])
        .filter(n => n.id !== excludeId)
        .map(n => ({ ...n, children: cut(n.children) }))
      return cut(this.orgTree)
    }
  },
  watch: {
    treeFilter(val) {
      this.$refs.orgTree && this.$refs.orgTree.filter(val)
    }
  },
  created() {
    this.loadTree()
  },
  methods: {
    loadTree(selectId) {
      this.treeLoading = true
      getOrgTree().then(res => {
        this.orgTree = res.data || []
        this.treeLoading = false
        if (selectId) {
          this.$nextTick(() => {
            this.$refs.orgTree && this.$refs.orgTree.setCurrentKey(selectId)
            const node = this.findOrg(selectId, this.orgTree)
            if (node) this.selectOrg(node)
          })
        }
      }).catch(() => { this.treeLoading = false })
    },
    findOrg(id, nodes) {
      for (const n of (nodes || [])) {
        if (n.id === id) return n
        const hit = this.findOrg(id, n.children)
        if (hit) return hit
      }
      return null
    },
    filterNode(value, data) {
      if (!value) return true
      return data.orgName.indexOf(value) !== -1
    },
    handleNodeClick(data) {
      this.selectOrg(data)
    },
    selectOrg(data) {
      this.currentOrg = data
      this.memberLoading = true
      listOrgUsers(data.id).then(res => {
        this.orgUsers = (res.data || []).map(u => ({ ...u }))
        this.memberLoading = false
      }).catch(() => { this.memberLoading = false })
    },
    handleAddOrg(parent) {
      this.orgForm = { parentId: parent ? parent.id : 0, orgCode: '', orgName: '', leaderUserId: null, sortOrder: 0, status: '0' }
      this.orgDialogVisible = true
      this.$nextTick(() => this.$refs.orgForm && this.$refs.orgForm.clearValidate())
    },
    handleEditOrg(data) {
      this.orgForm = { ...data, children: undefined }
      this.orgDialogVisible = true
      this.$nextTick(() => this.$refs.orgForm && this.$refs.orgForm.clearValidate())
    },
    submitOrgForm() {
      this.$refs.orgForm.validate(valid => {
        if (!valid) return
        const action = this.orgForm.id ? updateOrg : addOrg
        action(this.orgForm).then(() => {
          this.$modal.msgSuccess(this.orgForm.id ? '修改成功' : '新增成功')
          this.orgDialogVisible = false
          this.loadTree(this.orgForm.id)
        })
      })
    },
    handleDeleteOrg(data) {
      this.$modal.confirm('确认删除组织「' + data.orgName + '」？').then(() => {
        return delOrg(data.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        if (this.currentOrg && this.currentOrg.id === data.id) {
          this.currentOrg = null
          this.orgUsers = []
        }
        this.loadTree()
      }).catch(() => {})
    },
    addUserRow() {
      this.orgUsers.push({ userId: null, userName: null, isPrimary: 0 })
    },
    handleSaveUsers() {
      saveOrgUsers({ orgId: this.currentOrg.id, users: this.orgUsers }).then(() => {
        this.$modal.msgSuccess('成员保存成功')
        this.selectOrg(this.currentOrg)
        this.loadTree(this.currentOrg.id)
      })
    }
  }
}
</script>

<style scoped>
.custom-tree-node {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 14px;
  padding-right: 8px;
}
</style>
