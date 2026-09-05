<template>
  <div class="app-container">
    <el-alert title="组织预算编制：按组织分别编制与锁定快照，集团合并视角汇总各组织数据，锁定后快照不可变（版本号+1不覆盖）。" type="info" :closable="false" style="margin-bottom: 15px;" />

    <el-tabs v-model="activeTab">
      <!-- 集团合并视角 -->
      <el-tab-pane label="集团合并视角" name="consolidated">
        <el-form :inline="true" size="small">
          <el-form-item label="预算期间">
            <el-date-picker v-model="period" type="month" value-format="yyyy-MM" placeholder="全部期间" clearable style="width: 160px;" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="loadConsolidated">查询</el-button>
          </el-form-item>
        </el-form>
        <el-table :data="consolidatedRows" border size="small" v-loading="consolidatedLoading" :row-class-name="rowClassName">
          <el-table-column label="组织" min-width="180" prop="orgName">
            <template slot-scope="scope">
              <span :style="scope.row.totalRow ? 'font-weight:bold;' : ''">{{ scope.row.orgName || '（未挂组织）' }}{{ scope.row.totalRow ? ' 【集团合计】' : '' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="预算单数" width="100" align="center" prop="sheetCount" />
          <el-table-column label="已批准" width="90" align="center" prop="approvedCount" />
          <el-table-column label="预算总额" width="150" align="right">
            <template slot-scope="scope">{{ formatAmount(scope.row.totalBudget) }}</template>
          </el-table-column>
          <el-table-column label="实际总额" width="150" align="right">
            <template slot-scope="scope">{{ formatAmount(scope.row.totalActual) }}</template>
          </el-table-column>
          <el-table-column label="最新快照版本" width="120" align="center">
            <template slot-scope="scope">
              <el-tag v-if="scope.row.latestSnapshotVersion" size="small">V{{ scope.row.latestSnapshotVersion }}</el-tag>
              <span v-else>-</span>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 组织维度编制列表 -->
      <el-tab-pane label="组织编制列表" name="orgList">
        <el-form :inline="true" size="small">
          <el-form-item label="组织">
            <el-cascader
              v-model="queryOrgId"
              :options="orgTree"
              :props="{ checkStrictly: true, value: 'id', label: 'orgName', children: 'children', emitPath: false }"
              clearable
              placeholder="请选择组织"
              style="width: 240px;" />
          </el-form-item>
          <el-form-item label="预算期间">
            <el-date-picker v-model="period" type="month" value-format="yyyy-MM" placeholder="全部期间" clearable style="width: 160px;" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="loadOrgPreparations">查询</el-button>
          </el-form-item>
        </el-form>
        <el-table :data="preparations" border size="small" v-loading="prepLoading">
          <el-table-column label="预算单号" min-width="160" prop="sheetNo" />
          <el-table-column label="期间" width="100" align="center" prop="budgetPeriod" />
          <el-table-column label="状态" width="110" align="center">
            <template slot-scope="scope">
              <el-tag :type="statusTagType(scope.row.status)" size="small">{{ statusText(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="审批阶段" width="100" align="center" prop="approvalStage" />
          <el-table-column label="预算总额" width="150" align="right">
            <template slot-scope="scope">{{ formatAmount(scope.row.totalBudget) }}</template>
          </el-table-column>
          <el-table-column label="实际总额" width="150" align="right">
            <template slot-scope="scope">{{ formatAmount(scope.row.totalActual) }}</template>
          </el-table-column>
        </el-table>
      </el-tab-pane>

      <!-- 快照管理 -->
      <el-tab-pane label="快照管理" name="snapshot">
        <el-form :inline="true" size="small">
          <el-form-item label="组织">
            <el-cascader
              v-model="snapshotOrgId"
              :options="orgTree"
              :props="{ checkStrictly: true, value: 'id', label: 'orgName', children: 'children', emitPath: false }"
              clearable
              placeholder="请选择组织"
              style="width: 240px;" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" icon="el-icon-search" @click="loadVersions" :disabled="!snapshotOrgId">查询版本</el-button>
            <el-button type="warning" icon="el-icon-lock" @click="handleLock" :disabled="!snapshotOrgId" v-hasPermi="['fee:orgBudget:snapshot']">锁定快照</el-button>
          </el-form-item>
        </el-form>
        <el-table :data="versions" border size="small" v-loading="versionLoading">
          <el-table-column label="版本号" width="90" align="center">
            <template slot-scope="scope">V{{ scope.row.versionNo }}</template>
          </el-table-column>
          <el-table-column label="期间" width="100" align="center" prop="period" />
          <el-table-column label="状态" width="100" align="center">
            <template slot-scope="scope">
              <el-tag :type="scope.row.status === 'LOCKED' ? 'success' : 'info'" size="small">{{ scope.row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="锁定时间" width="170" align="center" prop="lockedAt" />
          <el-table-column label="操作" width="100" align="center">
            <template slot-scope="scope">
              <el-button size="mini" type="text" icon="el-icon-view" @click="handleViewSnapshot(scope.row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <!-- 快照详情 -->
    <el-dialog :title="snapshotTitle" :visible.sync="snapshotDialogVisible" width="700px" append-to-body>
      <el-table v-if="snapshotSheets.length" :data="snapshotSheets" border size="small">
        <el-table-column label="表单号" min-width="140" prop="sheetNo" />
        <el-table-column label="状态" width="100" align="center" prop="status" />
        <el-table-column label="预算总额" width="150" align="right">
          <template slot-scope="scope">{{ formatAmount(scope.row.totalBudget) }}</template>
        </el-table-column>
        <el-table-column label="实际总额" width="150" align="right">
          <template slot-scope="scope">{{ formatAmount(scope.row.totalActual) }}</template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="快照无明细" />
    </el-dialog>
  </div>
</template>

<script>
import { getOrgTree, listOrgPreparations, listConsolidated, lockOrgSnapshot, listOrgSnapshotVersions } from '@/api/system/fee/fundPro'

export default {
  name: 'FeeOrgBudget',
  data() {
    return {
      activeTab: 'consolidated',
      orgTree: [],
      period: null,
      // 集团合并
      consolidatedRows: [],
      consolidatedLoading: false,
      // 组织编制列表
      queryOrgId: null,
      preparations: [],
      prepLoading: false,
      // 快照
      snapshotOrgId: null,
      versions: [],
      versionLoading: false,
      snapshotDialogVisible: false,
      snapshotTitle: '',
      snapshotSheets: []
    }
  },
  created() {
    getOrgTree().then(res => { this.orgTree = res.data || [] })
    this.loadConsolidated()
  },
  methods: {
    formatAmount(val) {
      if (val === null || val === undefined || val === '') return '-'
      return Number(val).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
    },
    rowClassName({ row }) {
      return row.totalRow ? 'warning-row' : ''
    },
    statusText(status) {
      const map = { Draft: '草稿', Pending: '审批中', Approved: '已批准', Rejected: '已驳回' }
      return map[status] || status
    },
    statusTagType(status) {
      const map = { Draft: 'info', Pending: 'warning', Approved: 'success', Rejected: 'danger' }
      return map[status] || 'info'
    },
    loadConsolidated() {
      this.consolidatedLoading = true
      listConsolidated(this.period).then(res => {
        this.consolidatedRows = res.data || []
        this.consolidatedLoading = false
      }).catch(() => { this.consolidatedLoading = false })
    },
    loadOrgPreparations() {
      if (!this.queryOrgId) {
        this.$modal.msgWarning('请先选择组织')
        return
      }
      this.prepLoading = true
      listOrgPreparations(this.queryOrgId, this.period).then(res => {
        this.preparations = res.data || []
        this.prepLoading = false
      }).catch(() => { this.prepLoading = false })
    },
    loadVersions() {
      this.versionLoading = true
      listOrgSnapshotVersions(this.snapshotOrgId).then(res => {
        this.versions = res.data || []
        this.versionLoading = false
      }).catch(() => { this.versionLoading = false })
    },
    handleLock() {
      this.$prompt('请输入要锁定的预算期间（格式 yyyy-MM）', '锁定组织预算快照', {
        inputPattern: /^\d{4}-\d{2}$/,
        inputErrorMessage: '期间格式应为 yyyy-MM'
      }).then(({ value }) => {
        return lockOrgSnapshot(this.snapshotOrgId, value).then(res => {
          this.$modal.msgSuccess('快照锁定成功，版本号 V' + (res.data ? res.data.versionNo : ''))
          this.loadVersions()
        })
      }).catch(() => {})
    },
    handleViewSnapshot(row) {
      this.snapshotTitle = '快照详情 V' + row.versionNo + '（' + row.period + '）'
      try {
        const data = JSON.parse(row.snapshotData || '{}')
        this.snapshotSheets = data.sheets || []
      } catch (e) {
        this.snapshotSheets = []
      }
      this.snapshotDialogVisible = true
    }
  }
}
</script>

<style scoped>
::v-deep .warning-row {
  background-color: #fdf6ec;
  font-weight: bold;
}
</style>
