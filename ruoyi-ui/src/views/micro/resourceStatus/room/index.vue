<template>
  <div class="app-container">
    <el-alert title="房间状态管理（T21.6，★FEAT_SELF_BOOKING 功能开关）" type="info"
      description="展示房间当前资源状态：空闲🟢 / 使用🔴 / 预约中🟡。可手动设置状态，仅「空闲」的房间可被顾客预约。"
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

    <!-- 房间状态表格 -->
    <el-table v-loading="loading" :data="roomList" border stripe>
      <el-table-column label="房间名称" align="center" prop="name" min-width="120" />
      <el-table-column label="小时单价" align="center" width="100">
        <template slot-scope="scope">
          <span v-if="scope.row.price != null">¥{{ scope.row.price }}/时</span>
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
      <el-table-column label="上架状态" align="center" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="mini">
            {{ scope.row.status === '0' ? '上架' : '下架' }}
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

    <el-empty v-if="!loading && roomList.length === 0" description="暂无房间数据" />

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
import { listRoomStatus, setResourceStatus, listResourceStatusLogs } from '@/api/micro/resourceStatus'
import { getRoleDisplayName } from '@/utils/roleDisplay'

export default {
  name: 'ResourceStatusRoom',
  data() {
    return {
      loading: false,
      roomList: [],
      // 员工角色显示名：优先租户角色映射（如助教/导游），无映射回退通用名「员工」
      staffRoleName: '员工',
      statusOptions: [
        { value: 'idle', label: '空闲' },
        { value: 'in_use', label: '使用' }
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
      listRoomStatus().then(res => {
        this.roomList = res.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    statusLabel(status) {
      const map = { idle: '空闲', working: '工作', resting: '休息', reserved: '预约中', in_use: '使用' }
      return map[status] || status || '空闲'
    },
    statusTagType(status) {
      const map = { idle: 'success', working: 'danger', resting: 'info', reserved: 'warning', in_use: 'danger' }
      return map[status] || 'info'
    },
    handleSetStatus(row, targetStatus) {
      const label = this.statusLabel(targetStatus)
      this.$modal.confirm('确认将房间「' + (row.name || '') + '」的状态设置为「' + label + '」吗？仅空闲可被预约。').then(() => {
        return setResourceStatus('room', row.id, targetStatus)
      }).then(() => {
        this.$modal.msgSuccess('设置成功')
        this.loadData()
      }).catch(() => {})
    },
    showLogDialog() {
      this.logDialog.visible = true
      listResourceStatusLogs({ resourceType: 'room' }).then(res => {
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
