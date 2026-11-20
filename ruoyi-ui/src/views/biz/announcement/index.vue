<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="标题" prop="title">
        <el-input v-model="queryParams.title" placeholder="请输入标题" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="平台" prop="platform">
        <el-select v-model="queryParams.platform" placeholder="请选择平台" clearable>
          <el-option label="视频号" value="video_account" />
          <el-option label="抖音" value="douyin" />
        </el-select>
      </el-form-item>
      <el-form-item label="业务状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="待开播" value="PENDING" />
          <el-option label="直播中" value="LIVING" />
          <el-option label="已下播" value="OFFLINE" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['biz:announcement:add']">新增公告</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="info" plain icon="el-icon-monitor" size="mini" @click="handleBoard">看板预览</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-alert title="展示态由服务端按状态机派生：living 排第一 / pending≤30min 临近提醒 / pending>30min 隐藏 / offline≤10min 可回放 / offline>10min 隐藏；视频号可跳转，抖音仅引流。" type="info" :closable="false" class="mb8" />

    <el-table v-loading="loading" :data="announcementList">
      <el-table-column label="标题" align="left" prop="title" min-width="160" show-overflow-tooltip />
      <el-table-column label="平台" align="center" prop="platform" width="90">
        <template slot-scope="scope">{{ platformText(scope.row.platform) }}</template>
      </el-table-column>
      <el-table-column label="关联活动" align="center" min-width="130" show-overflow-tooltip>
        <template slot-scope="scope">{{ containerName(scope.row.projectId) }}</template>
      </el-table-column>
      <el-table-column label="业务状态" align="center" prop="status" width="90">
        <template slot-scope="scope">
          <el-tag :type="statusTag(scope.row.status)" size="small">{{ statusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="展示态" align="center" prop="displayState" width="120">
        <template slot-scope="scope">
          <el-tag :type="displayTag(scope.row.displayState)" size="small" effect="plain">{{ displayText(scope.row.displayState) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="跳转" align="center" width="80">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.canNavigate" type="success" size="small">可跳转</el-tag>
          <span v-else>仅引流</span>
        </template>
      </el-table-column>
      <el-table-column label="主播" align="center" prop="anchor" width="100" show-overflow-tooltip />
      <el-table-column label="开播时间" align="center" prop="startTime" width="150">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.startTime) }}</span></template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sort" width="70" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="240">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-video-play" @click="handleStartLiving(scope.row)" v-hasPermi="['biz:announcement:edit']" v-if="scope.row.status === 'PENDING'">开播</el-button>
          <el-button size="mini" type="text" icon="el-icon-video-pause" @click="handleOffline(scope.row)" v-hasPermi="['biz:announcement:edit']" v-if="scope.row.status === 'LIVING'">下播</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['biz:announcement:edit']">编辑</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['biz:announcement:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑公告 -->
    <el-dialog :title="title" :visible.sync="open" width="640px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="请输入公告标题" />
        </el-form-item>
        <el-form-item label="关联活动" prop="projectId">
          <el-select v-model="form.projectId" placeholder="请选择活动容器（可空）" filterable clearable style="width: 100%;">
            <el-option v-for="p in containers" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="平台" prop="platform">
          <el-radio-group v-model="form.platform">
            <el-radio label="video_account">视频号（可跳转）</el-radio>
            <el-radio label="douyin">抖音（仅引流）</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="封面图" prop="coverUrl">
          <image-upload v-model="form.coverUrl" :limit="1" />
        </el-form-item>
        <el-form-item label="直播间外链" prop="liveUrl">
          <el-input v-model="form.liveUrl" placeholder="抖音引流链接 / 视频号兜底外链" />
        </el-form-item>
        <el-form-item label="视频号房间ID" prop="roomId" v-if="form.platform === 'video_account'">
          <el-input v-model="form.roomId" placeholder="navigateToChannelsLive 所需 roomId" />
        </el-form-item>
        <el-form-item label="主播/发起人" prop="anchor">
          <el-input v-model="form.anchor" placeholder="请输入主播或发起人" />
        </el-form-item>
        <el-form-item label="开播时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="选择开播时间" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="业务状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%;">
            <el-option label="待开播" value="PENDING" />
            <el-option label="直播中" value="LIVING" />
            <el-option label="已下播" value="OFFLINE" />
          </el-select>
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" style="width: 100%;" />
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

    <!-- 看板预览 -->
    <el-dialog title="首页直播看板（消费者可见项）" :visible.sync="boardOpen" width="560px" append-to-body>
      <el-alert v-if="board && !board.hasAnnouncement" title="当前无可展示公告：has_announcement=false（首页不展示直播位）" type="warning" :closable="false" />
      <el-table v-else :data="board.items" size="small">
        <el-table-column label="标题" prop="title" min-width="140" show-overflow-tooltip />
        <el-table-column label="平台" width="80">
          <template slot-scope="scope">{{ platformText(scope.row.platform) }}</template>
        </el-table-column>
        <el-table-column label="展示态" width="120">
          <template slot-scope="scope">
            <el-tag :type="displayTag(scope.row.displayState)" size="small" effect="plain">{{ displayText(scope.row.displayState) }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>
  </div>
</template>

<script>
import { listAnnouncement, getAnnouncement, addAnnouncement, updateAnnouncement, startLiving, offlineAnnouncement, delAnnouncement, getAnnouncementBoard } from '@/api/biz/announcement'
import { listProject } from '@/api/biz/project'

export default {
  name: 'BizAnnouncement',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      announcementList: [],
      containers: [],
      open: false,
      boardOpen: false,
      board: null,
      title: '',
      queryParams: { pageNum: 1, pageSize: 10, title: null, platform: null, status: null },
      form: {},
      rules: {
        title: [{ required: true, message: '标题不能为空', trigger: 'blur' }],
        platform: [{ required: true, message: '请选择平台', trigger: 'change' }]
      }
    }
  },
  created() {
    this.getList()
    listProject({ pageNum: 1, pageSize: 200, type: 'activity' }).then(res => { this.containers = res.rows || [] })
  },
  methods: {
    getList() {
      this.loading = true
      listAnnouncement(this.queryParams).then(response => {
        this.announcementList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    containerName(id) {
      if (id == null) { return '-' }
      const p = this.containers.find(c => c.id === id)
      return p ? p.name : ('#' + id)
    },
    platformText(platform) {
      const map = { video_account: '视频号', douyin: '抖音' }
      return map[platform] || platform
    },
    statusText(status) {
      const map = { PENDING: '待开播', LIVING: '直播中', OFFLINE: '已下播' }
      return map[status] || status
    },
    statusTag(status) {
      const map = { PENDING: 'info', LIVING: 'danger', OFFLINE: 'warning' }
      return map[status] || 'info'
    },
    displayText(state) {
      const map = { LIVING: '直播中', PENDING_SOON: '临近开播', PENDING_HIDE: '待开播·隐藏', OFFLINE_REPLAY: '回放中', OFFLINE_HIDE: '已结束·隐藏' }
      return map[state] || state || '-'
    },
    displayTag(state) {
      const map = { LIVING: 'danger', PENDING_SOON: 'warning', PENDING_HIDE: 'info', OFFLINE_REPLAY: 'primary', OFFLINE_HIDE: 'info' }
      return map[state] || 'info'
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
      this.form = { id: null, projectId: null, platform: 'video_account', title: null, coverUrl: null, liveUrl: null, roomId: null, anchor: null, startTime: null, status: 'PENDING', sort: 0, remark: null }
      this.resetForm('form')
    },
    cancel() {
      this.open = false
      this.reset()
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增公告'
    },
    handleUpdate(row) {
      this.reset()
      getAnnouncement(row.id).then(res => {
        this.form = res.data
        this.open = true
        this.title = '编辑公告'
      })
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateAnnouncement(this.form).then(() => {
              this.$modal.msgSuccess('修改成功')
              this.open = false
              this.getList()
            })
          } else {
            addAnnouncement(this.form).then(() => {
              this.$modal.msgSuccess('新增成功')
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleStartLiving(row) {
      this.$modal.confirm('确认开播「' + row.title + '」？状态将置为直播中并排到看板首位').then(() => {
        return startLiving(row.id)
      }).then(() => {
        this.$modal.msgSuccess('已开播')
        this.getList()
      }).catch(() => {})
    },
    handleOffline(row) {
      this.$modal.confirm('确认下播「' + row.title + '」？下播后 10 分钟内可进回放').then(() => {
        return offlineAnnouncement(row.id)
      }).then(() => {
        this.$modal.msgSuccess('已下播')
        this.getList()
      }).catch(() => {})
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除公告「' + row.title + '」？').then(() => {
        return delAnnouncement(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    },
    handleBoard() {
      getAnnouncementBoard().then(res => {
        this.board = res.data
        this.boardOpen = true
      })
    }
  }
}
</script>
