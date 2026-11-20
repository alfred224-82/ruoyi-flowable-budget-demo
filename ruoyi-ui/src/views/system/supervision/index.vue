<template>
  <div class="app-container">
    <el-alert
      title="平台监管台（T25.4，platform_admin 跨租户查看全部投诉 + 停止活动/租户）"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom:12px;"
    />

    <!-- 搜索栏 -->
    <el-form :inline="true" size="small" class="mb16">
      <el-form-item label="渠道">
        <el-select v-model="queryParams.channel" placeholder="全部渠道" clearable style="width:140px;" @change="handleQuery">
          <el-option label="Boss 门店" value="BOSS" />
          <el-option label="平台 Admin" value="PLATFORM" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态">
        <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width:140px;" @change="handleQuery">
          <el-option label="待处理" value="PENDING" />
          <el-option label="处理中" value="PROCESSING" />
          <el-option label="已解决" value="RESOLVED" />
          <el-option label="已关闭" value="CLOSED" />
        </el-select>
      </el-form-item>
      <el-form-item label="租户ID">
        <el-input v-model="queryParams.tenantId" placeholder="租户ID" clearable style="width:140px;" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 投诉列表 -->
    <el-table v-loading="loading" :data="tableData" border stripe style="width:100%;">
      <el-table-column label="ID" prop="id" width="60" align="center" />
      <el-table-column label="渠道" prop="complaintChannel" width="100" align="center">
        <template slot-scope="scope">
          <el-tag :type="scope.row.complaintChannel === 'PLATFORM' ? 'danger' : ''" size="mini">
            {{ scope.row.complaintChannel === 'BOSS' ? 'Boss门店' : '平台Admin' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="类别" prop="category" width="100" align="center">
        <template slot-scope="scope">{{ scope.row.category || '—' }}</template>
      </el-table-column>
      <el-table-column label="投诉内容" prop="contentText" min-width="200" show-overflow-tooltip />
      <el-table-column label="投诉人" width="100" align="center">
        <template slot-scope="scope">
          <span>{{ scope.row.complainantName || '匿名' }}</span>
          <div v-if="scope.row.complainantPhone" style="font-size:11px;color:#999;">{{ scope.row.complainantPhone }}</div>
        </template>
      </el-table-column>
      <el-table-column label="被投诉员工" prop="staffName" width="100" align="center">
        <template slot-scope="scope">{{ scope.row.staffName || '—' }}</template>
      </el-table-column>
      <el-table-column label="关联活动" prop="activityTitle" width="130" show-overflow-tooltip>
        <template slot-scope="scope">{{ scope.row.activityTitle || '—' }}</template>
      </el-table-column>
      <el-table-column label="租户ID" prop="tenantId" width="80" align="center" />
      <el-table-column label="状态" prop="status" width="90" align="center">
        <template slot-scope="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="mini">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="投诉时间" prop="submitTime" width="155" align="center" />
      <el-table-column label="操作" width="200" align="center" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" size="mini" icon="el-icon-view" @click="handleDetail(scope.row)">详情</el-button>
          <el-button
            v-if="scope.row.status === 'PENDING' || scope.row.status === 'PROCESSING'"
            type="text" size="mini" icon="el-icon-chat-dot-square"
            v-hasPermi="['system:supervision:view']"
            @click="handleProcess(scope.row)">处理</el-button>
          <el-button
            v-if="scope.row.activityId"
            type="text" size="mini" style="color:#F56C6C;"
            v-hasPermi="['system:supervision:stopActivity']"
            @click="handleStopActivity(scope.row)">停止活动</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 处理弹窗 -->
    <el-dialog title="投诉详情与处理" :visible.sync="processOpen" width="600px" append-to-body>
      <el-descriptions :column="2" border size="small" style="margin-bottom:16px;" v-if="currentRow.id">
        <el-descriptions-item label="渠道">
          <el-tag :type="currentRow.complaintChannel === 'PLATFORM' ? 'danger' : ''" size="mini">
            {{ currentRow.complaintChannel === 'BOSS' ? 'Boss门店' : '平台Admin' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(currentRow.status)" size="mini">{{ statusLabel(currentRow.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="投诉人" :span="2">
          {{ currentRow.complainantName || '匿名' }}
          <span v-if="currentRow.complainantPhone" style="color:#999;margin-left:8px;">{{ currentRow.complainantPhone }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="投诉内容" :span="2">{{ currentRow.contentText }}</el-descriptions-item>
        <el-descriptions-item label="关联活动" v-if="currentRow.activityTitle">{{ currentRow.activityTitle }}</el-descriptions-item>
        <el-descriptions-item label="被投诉员工" v-if="currentRow.staffName">{{ currentRow.staffName }}</el-descriptions-item>
        <!-- ★附加图片（BOSS 渠道） -->
        <el-descriptions-item label="附加图片" :span="2" v-if="photoList.length">
          <div class="photo-preview">
            <el-image v-for="(url, idx) in photoList" :key="idx" :src="url"
              :preview-src-list="photoList" fit="cover"
              style="width:80px;height:80px;margin-right:8px;border-radius:4px;cursor:pointer;" />
          </div>
        </el-descriptions-item>
        <!-- ★取证照片（PLATFORM 渠道） -->
        <el-descriptions-item label="取证照片" :span="2" v-if="evidenceList.length">
          <div class="photo-preview">
            <el-image v-for="(url, idx) in evidenceList" :key="idx" :src="url"
              :preview-src-list="evidenceList" fit="cover"
              style="width:80px;height:80px;margin-right:8px;border-radius:4px;cursor:pointer;" />
          </div>
        </el-descriptions-item>
        <el-descriptions-item label="租户ID">{{ currentRow.tenantId }}</el-descriptions-item>
        <el-descriptions-item label="投诉时间">{{ currentRow.submitTime }}</el-descriptions-item>
      </el-descriptions>

      <el-divider v-if="currentRow.platformReplyContent">平台回复</el-divider>
      <div v-if="currentRow.platformReplyContent" style="background:#f5f7fa;padding:10px;border-radius:4px;margin-bottom:15px;">
        {{ currentRow.platformReplyContent }}
      </div>

      <el-divider>处理操作</el-divider>
      <el-form ref="processForm" :model="processForm" :rules="processRules" label-width="80px">
        <el-form-item label="回复内容" prop="replyContent">
          <el-input v-model="processForm.replyContent" type="textarea" :rows="4" placeholder="请输入平台处理回复..." maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item label="处理结果" prop="handleStatus">
          <el-radio-group v-model="processForm.handleStatus">
            <el-radio label="RESOLVED">已解决</el-radio>
            <el-radio label="CLOSED">已关闭</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <div slot="footer">
        <el-button type="primary" @click="submitProcess" :loading="submitLoading">提 交</el-button>
        <el-button @click="processOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import request from '@/utils/request'

export default {
  name: 'Supervision',
  data() {
    return {
      loading: false,
      tableData: [],
      queryParams: { channel: null, status: null, tenantId: null },
      // 处理弹窗
      processOpen: false,
      currentRow: {},
      processForm: { replyContent: '', handleStatus: 'RESOLVED' },
      processRules: {
        replyContent: [{ required: true, message: '请输入回复内容', trigger: 'blur' }]
      },
      submitLoading: false,
      photoList: [],
      evidenceList: []
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询投诉列表 */
    getList() {
      this.loading = true
      const params = { ...this.queryParams }
      if (params.tenantId) params.tenantId = Number(params.tenantId)
      request({ url: '/system/supervision/complaints', method: 'get', params }).then(res => {
        this.tableData = res.data || []
      }).finally(() => {
        this.loading = false
      })
    },
    /** 搜索 */
    handleQuery() {
      this.getList()
    },
    /** 重置 */
    resetQuery() {
      this.queryParams = { channel: null, status: null, tenantId: null }
      this.getList()
    },
    /** 解析 JSON 图片 URL 列表 */
    parsePhotoUrls(jsonStr) {
      if (!jsonStr) return []
      try {
        const arr = JSON.parse(jsonStr)
        return Array.isArray(arr) ? arr : []
      } catch (e) { return [] }
    },
    /** 查看详情 */
    handleDetail(row) {
      this.currentRow = row
      this.photoList = this.parsePhotoUrls(row.photoUrls)
      this.evidenceList = this.parsePhotoUrls(row.evidencePhotos)
      this.processForm = { replyContent: '', handleStatus: 'RESOLVED' }
      this.processOpen = true
    },
    /** 处理投诉 */
    handleProcess(row) {
      this.currentRow = row
      this.photoList = this.parsePhotoUrls(row.photoUrls)
      this.evidenceList = this.parsePhotoUrls(row.evidencePhotos)
      this.processForm = { replyContent: '', handleStatus: 'RESOLVED' }
      this.processOpen = true
    },
    /** 提交处理 */
    submitProcess() {
      this.$refs.processForm.validate(valid => {
        if (!valid) return
        this.$modal.confirm('确认提交处理结果？').then(() => {
          this.submitLoading = true
          request({
            url: '/micro/complaint/' + this.currentRow.id + '/reply',
            method: 'put',
            data: { replyContent: this.processForm.replyContent, handleStatus: this.processForm.handleStatus }
          }).then(() => {
            this.$modal.msgSuccess('处理成功')
            this.processOpen = false
            this.getList()
          }).finally(() => {
            this.submitLoading = false
          })
        }).catch(() => {})
      })
    },
    /** 停止活动 */
    handleStopActivity(row) {
      this.$modal.confirm('确认停止该活动？停止后顾客端将不再展示。').then(() => {
        request({ url: '/system/supervision/stopActivity/' + row.activityId, method: 'put' }).then(() => {
          this.$modal.msgSuccess('活动已停止')
          this.getList()
        })
      }).catch(() => {})
    },
    /** 状态标签文本 */
    statusLabel(status) {
      const map = { PENDING: '待处理', PROCESSING: '处理中', RESOLVED: '已解决', CLOSED: '已关闭' }
      return map[status] || status
    },
    /** 状态标签颜色 */
    statusTagType(status) {
      const map = { PENDING: 'info', PROCESSING: 'warning', RESOLVED: 'success', CLOSED: '' }
      return map[status] || 'info'
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
</style>
