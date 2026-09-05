<template>
  <div class="app-container">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- Tab1: 工作流配置 -->
      <el-tab-pane label="工作流配置" name="workflow">
        <div v-loading="workflowLoading">
          <el-card v-for="item in workflowList" :key="item.id" class="workflow-card" shadow="hover">
            <div class="workflow-header">
              <div class="workflow-info">
                <span class="workflow-name">{{ item.workflowName }}</span>
                <el-tag :type="statusTagType(item.status)" size="small" style="margin-left: 10px">
                  {{ statusText(item.status) }}
                </el-tag>
              </div>
              <div class="workflow-actions">
                <el-button size="mini" type="primary" icon="el-icon-setting" @click="openConfigDialog(item)">配置</el-button>
                <el-button size="mini" type="success" icon="el-icon-upload2" :disabled="item.status === 'DEPLOYED'" @click="handleDeploy(item)">部署</el-button>
                <el-button v-if="item.status === 'DEPLOYED'" size="mini" type="warning" icon="el-icon-video-pause" @click="handleDisable(item)">停用</el-button>
                <el-button v-if="item.status === 'DISABLED'" size="mini" type="info" icon="el-icon-video-play" @click="handleEnable(item)">启用</el-button>
              </div>
            </div>
            <div class="workflow-desc">{{ item.description }}</div>
            <div class="workflow-nodes">
              <span class="node-label">审批节点：</span>
              <el-tag v-for="node in item.nodes" :key="node.id" size="small" type="info" style="margin-right: 5px">
                {{ node.nodeSeq }}.{{ node.nodeName }}({{ node.roleName }})
              </el-tag>
              <span v-if="!item.nodes || item.nodes.length === 0" class="no-node">未配置节点</span>
            </div>
          </el-card>
          <el-empty v-if="workflowList.length === 0" description="暂无工作流配置" />
        </div>
      </el-tab-pane>

      <!-- Tab2: 资金池配置 -->
      <el-tab-pane label="资金池配置" name="fundPool">
        <div v-loading="fundPoolLoading">
          <el-table :data="fundPoolList" border stripe style="width: 100%">
            <el-table-column prop="levelName" label="层级" width="120" align="center" />
            <el-table-column label="关联角色" width="180" align="center">
              <template slot-scope="scope">
                <el-select v-model="scope.row.roleId" placeholder="选择角色" size="small" filterable>
                  <el-option v-for="role in roleOptions" :key="role.roleId" :label="role.roleName" :value="role.roleId" />
                </el-select>
              </template>
            </el-table-column>
            <el-table-column label="初始额度(元)" width="200" align="center">
              <template slot-scope="scope">
                <el-input-number v-model="scope.row.initialAmount" :min="0" :precision="2" :step="10000" size="small" controls-position="right" style="width: 160px" />
              </template>
            </el-table-column>
            <el-table-column label="预警阈值(%)" width="160" align="center">
              <template slot-scope="scope">
                <el-input-number v-model="scope.row.warningThreshold" :min="0" :max="100" :precision="2" size="small" controls-position="right" style="width: 120px" />
              </template>
            </el-table-column>
            <el-table-column prop="status" label="状态" width="100" align="center">
              <template slot-scope="scope">
                <el-tag :type="scope.row.status === 'ENABLED' ? 'success' : 'danger'" size="small">
                  {{ scope.row.status === 'ENABLED' ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
          <div style="margin-top: 20px; text-align: center">
            <el-button type="primary" icon="el-icon-check" :loading="saving" @click="handleSaveFundPool">保存配置</el-button>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>

    <!-- 工作流配置弹窗 -->
    <el-dialog :title="'配置审批节点 — ' + (currentWorkflow ? currentWorkflow.workflowName : '')" :visible.sync="workflowDialogVisible" width="650px" append-to-body>
      <div v-for="(node, index) in nodeList" :key="index" class="node-item">
        <el-row :gutter="10" type="flex" align="middle">
          <el-col :span="2"><span class="node-seq">{{ index + 1 }}</span></el-col>
          <el-col :span="7">
            <el-input v-model="node.nodeName" placeholder="节点名称" size="small" />
          </el-col>
          <el-col :span="8">
            <el-select v-model="node.roleId" placeholder="审批角色" size="small" filterable style="width: 100%">
              <el-option v-for="role in roleOptions" :key="role.roleId" :label="role.roleName" :value="role.roleId" />
            </el-select>
          </el-col>
          <el-col :span="5">
            <el-select v-model="node.approvalType" size="small" style="width: 100%">
              <el-option label="单人审批" value="ANY" />
              <el-option label="会签" value="ALL" />
            </el-select>
          </el-col>
          <el-col :span="2">
            <el-button type="danger" icon="el-icon-delete" size="mini" circle @click="removeNode(index)" />
          </el-col>
        </el-row>
      </div>
      <el-button type="text" icon="el-icon-plus" @click="addNode">添加审批节点</el-button>
      <div slot="footer">
        <el-button @click="workflowDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveWorkflow">保 存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listWorkflowConfig, saveWorkflowConfig, deployWorkflow, disableWorkflow, enableWorkflow, listFundPoolConfig, saveFundPoolConfig } from '@/api/system/fee/init'
import { allRoleExcludeAdmin } from '@/api/system/role'

export default {
  name: 'FeeInit',
  data() {
    return {
      activeTab: 'workflow',
      // 工作流配置
      workflowList: [],
      workflowLoading: false,
      workflowDialogVisible: false,
      currentWorkflow: null,
      nodeList: [],
      roleOptions: [],
      // 资金池配置
      fundPoolList: [],
      fundPoolLoading: false,
      saving: false
    }
  },
  created() {
    this.loadWorkflowList()
    this.loadFundPoolList()
    this.loadRoleOptions()
  },
  methods: {
    /** 加载工作流列表 */
    loadWorkflowList() {
      this.workflowLoading = true
      listWorkflowConfig().then(res => {
        this.workflowList = res.data || []
      }).finally(() => {
        this.workflowLoading = false
      })
    },
    /** 加载资金池配置 */
    loadFundPoolList() {
      this.fundPoolLoading = true
      listFundPoolConfig().then(res => {
        this.fundPoolList = res.data || []
      }).finally(() => {
        this.fundPoolLoading = false
      })
    },
    /** 加载角色选项（排除admin，无需权限） */
    loadRoleOptions() {
      allRoleExcludeAdmin().then(res => {
        this.roleOptions = res.data || []
      })
    },
    /** 状态标签类型 */
    statusTagType(status) {
      const map = { DEPLOYED: 'success', DRAFT: 'info', DISABLED: 'danger' }
      return map[status] || 'info'
    },
    /** 状态文本 */
    statusText(status) {
      const map = { DEPLOYED: '已部署', DRAFT: '未部署', DISABLED: '已停用' }
      return map[status] || status
    },
    /** 打开配置弹窗 */
    openConfigDialog(item) {
      this.currentWorkflow = item
      this.nodeList = (item.nodes || []).map(n => ({
        nodeSeq: n.nodeSeq,
        nodeName: n.nodeName,
        roleId: n.roleId,
        approvalType: n.approvalType
      }))
      if (this.nodeList.length === 0) {
        this.addNode()
      }
      this.workflowDialogVisible = true
    },
    /** 添加节点 */
    addNode() {
      this.nodeList.push({
        nodeSeq: this.nodeList.length + 1,
        nodeName: '',
        roleId: null,
        approvalType: 'ANY'
      })
    },
    /** 删除节点 */
    removeNode(index) {
      this.nodeList.splice(index, 1)
      this.nodeList.forEach((n, i) => { n.nodeSeq = i + 1 })
    },
    /** 保存工作流配置 */
    handleSaveWorkflow() {
      if (this.nodeList.length === 0) {
        this.$modal.msgError('至少配置一个审批节点')
        return
      }
      for (const node of this.nodeList) {
        if (!node.nodeName || !node.roleId) {
          this.$modal.msgError('请完善所有节点信息')
          return
        }
      }
      this.saving = true
      const data = {
        id: this.currentWorkflow.id,
        workflowKey: this.currentWorkflow.workflowKey,
        workflowName: this.currentWorkflow.workflowName,
        description: this.currentWorkflow.description,
        nodes: this.nodeList
      }
      saveWorkflowConfig(data).then(() => {
        this.$modal.msgSuccess('保存成功，需重新部署后生效')
        this.workflowDialogVisible = false
        this.loadWorkflowList()
      }).finally(() => {
        this.saving = false
      })
    },
    /** 部署工作流 */
    handleDeploy(item) {
      this.$modal.confirm('确认部署「' + item.workflowName + '」？部署后将替换当前流程定义。').then(() => {
        return deployWorkflow(item.id)
      }).then(() => {
        this.$modal.msgSuccess('部署成功')
        this.loadWorkflowList()
      }).catch(() => {})
    },
    /** 停用工作流 */
    handleDisable(item) {
      this.$modal.confirm('确认停用「' + item.workflowName + '」？停用后新流程将无法启动。').then(() => {
        return disableWorkflow(item.id)
      }).then(() => {
        this.$modal.msgSuccess('已停用')
        this.loadWorkflowList()
      }).catch(() => {})
    },
    /** 启用工作流 */
    handleEnable(item) {
      this.$modal.confirm('确认启用「' + item.workflowName + '」？').then(() => {
        return enableWorkflow(item.id)
      }).then(() => {
        this.$modal.msgSuccess('已启用')
        this.loadWorkflowList()
      }).catch(() => {})
    },
    /** 保存资金池配置 */
    handleSaveFundPool() {
      this.saving = true
      saveFundPoolConfig(this.fundPoolList).then(() => {
        this.$modal.msgSuccess('保存成功')
        this.loadFundPoolList()
      }).finally(() => {
        this.saving = false
      })
    }
  }
}
</script>

<style scoped>
.workflow-card {
  margin-bottom: 15px;
}
.workflow-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.workflow-name {
  font-size: 16px;
  font-weight: bold;
}
.workflow-desc {
  color: #909399;
  font-size: 13px;
  margin: 8px 0;
}
.workflow-nodes {
  margin-top: 8px;
}
.node-label {
  font-size: 13px;
  color: #606266;
}
.no-node {
  color: #c0c4cc;
  font-size: 13px;
}
.node-item {
  margin-bottom: 12px;
  padding: 8px;
  background: #f5f7fa;
  border-radius: 4px;
}
.node-seq {
  display: inline-block;
  width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  background: #409eff;
  color: #fff;
  border-radius: 50%;
  font-size: 12px;
}
</style>
