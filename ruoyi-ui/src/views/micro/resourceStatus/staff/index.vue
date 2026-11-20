<template>
  <div class="app-container">
    <el-alert title="员工状态管理（T21.6，★FEAT_SELF_BOOKING 功能开关）" type="info"
      :description="'展示' + staffRoleName + '当前资源状态：空闲🟢 / 工作🔴 / 休息⚪ / 预约中🟡 / 使用中🔴。状态从 resource_status_log 最新记录推导。可手动设置状态，仅「空闲」的' + staffRoleName + '可被顾客预约。'"
      show-icon :closable="false" class="mb16" />

    <!-- 操作栏 -->
    <el-row :gutter="10" class="mb16">
      <el-col :span="1.5">
        <el-button type="primary" icon="el-icon-refresh" size="mini" @click="loadData">刷新</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" icon="el-icon-document" size="mini" @click="showLogDialog">状态日志</el-button>
      </el-col>
    </el-row>

    <!-- 员工状态表格（列名按角色映射动态显示） -->
    <el-table v-loading="loading" :data="staffList" border stripe>
      <el-table-column :label="staffRoleName" align="center" min-width="140">
        <template slot-scope="scope">
          <div style="display:flex;align-items:center;justify-content:center;">
            <el-avatar v-if="scope.row.avatar" :size="32" :src="scope.row.avatar" style="margin-right:8px;" />
            <el-avatar v-else :size="32" icon="el-icon-user-solid" style="margin-right:8px;" />
            <span>{{ scope.row.nickName || '未设置昵称' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="小时单价" align="center" width="100">
        <template slot-scope="scope">
          <span v-if="scope.row.hourlyRate != null">¥{{ scope.row.hourlyRate }}/时</span>
          <span v-else class="text-muted">未设置</span>
        </template>
      </el-table-column>
      <el-table-column label="资源状态" align="center" width="120">
        <template slot-scope="scope">
          <el-tag :type="statusTagType(scope.row.resourceStatus)" size="small" effect="dark">
            {{ statusLabel(scope.row.resourceStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="账号状态" align="center" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="mini">
            {{ scope.row.status === '0' ? '正常' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="160" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-dropdown v-hasPermi="['micro:resourceStatus:release']" trigger="click"
            @command="(cmd) => handleSetStatus(scope.row, cmd)">
            <el-button size="mini" type="primary" icon="el-icon-setting">
              设置状态<i class="el-icon-arrow-down el-icon--right"></i>
            </el-button>
            <el-dropdown-menu slot="dropdown">
              <el-dropdown-item v-for="opt in statusOptions" :key="opt.value" :command="opt.value"
                :disabled="opt.value === scope.row.resourceStatus">{{ opt.label }}</el-dropdown-item>
            </el-dropdown-menu>
          </el-dropdown>
        </template>
      </el-table-column>
    </el-table>

    <el-empty v-if="!loading && staffList.length === 0" :description="'暂无' + staffRoleName + '数据'" />

    <!-- 状态日志对话框 -->
    <el-dialog title="资源状态变更日志（最近50条）" :visible.sync="logDialog.visible" width="700px" append-to-body>
      <el-table :data="logDialog.list" border stripe size="mini" max-height="400">
        <el-table-column label="时间" prop="operateTime" width="150" />
        <el-table-column label="类型" prop="resourceType" width="60" align="center">
          <template slot-scope="scope">{{ scope.row.resourceType === 'room' ? '房间' : staffRoleName }}</template>
        </el-table-column>
        <el-table-column label="资源" prop="resourceName" width="100" />
        <el-table-column label="变更" width="140" align="center">
          <template slot-scope="scope">
            <el-tag :type="statusTagType(scope.row.fromStatus)" size="mini">{{ statusLabel(scope.row.fromStatus) }}</el-tag>
            → <el-tag :type="statusTagType(scope.row.toStatus)" size="mini">{{ statusLabel(scope.row.toStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="原因" prop="reason" min-width="120" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { listStaffStatus, setResourceStatus, listResourceStatusLogs } from '@/api/micro/resourceStatus'
import { getRoleDisplayName } from '@/utils/roleDisplay'

export default {
  name: 'ResourceStatusStaff',
  data() {
    return {
      loading: false,
      staffList: [],
      // 员工角色显示名：优先租户角色映射（如助教/导游），无映射回退通用名「员工」
      staffRoleName: '员工',
      statusOptions: [
        { value: 'idle', label: '空闲' },
        { value: 'working', label: '工作' },
        { value: 'resting', label: '休息' }
      ],
      logDialog: { visible: false, list: [] }
    }
  },
  created() {
    this.loadData()
    getRoleDisplayName('salesman', '员工').then(n => { this.staffRoleName = n })
  },
  methods: {
    loadData() {
      this.loading = true
      listStaffStatus().then(res => {
        this.staffList = res.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    statusLabel(status) {
      const map = { idle: '空闲', working: '工作', resting: '休息', reserved: '预约中', in_use: '使用中' }
      return map[status] || status || '空闲'
    },
    statusTagType(status) {
      const map = { idle: 'success', working: 'danger', resting: 'info', reserved: 'warning', in_use: 'danger' }
      return map[status] || 'info'
    },
    handleSetStatus(row, targetStatus) {
      const label = this.statusLabel(targetStatus)
      this.$modal.confirm('确认将' + this.staffRoleName + '「' + (row.nickName || '') + '」的状态设置为「' + label + '」吗？仅空闲可被预约。').then(() => {
        return setResourceStatus('staff', row.userId, targetStatus)
      }).then(() => {
        this.$modal.msgSuccess('设置成功')
        this.loadData()
      }).catch(() => {})
    },
    showLogDialog() {
      this.logDialog.visible = true
      listResourceStatusLogs({ resourceType: 'staff' }).then(res => {
        this.logDialog.list = res.data || []
      })
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.text-muted { color: #c0c4cc; font-size: 12px; }
</style>
