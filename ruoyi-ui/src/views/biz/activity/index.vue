<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="活动名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入活动名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="草稿" value="DRAFT" />
          <el-option label="已发布" value="PUBLISHED" />
          <el-option label="进行中" value="ONGOING" />
          <el-option label="已结束" value="FINISHED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['biz:activity:add']">新增活动</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="activityList">
      <el-table-column label="活动名称" align="left" prop="name" min-width="160" show-overflow-tooltip />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template slot-scope="scope">
          <el-tag :type="statusTag(scope.row.status)" size="small">{{ statusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="开始时间" align="center" prop="startTime" width="150">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.startTime) }}</span></template>
      </el-table-column>
      <el-table-column label="结束时间" align="center" prop="endTime" width="150">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.endTime) }}</span></template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="150">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.createTime) }}</span></template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="260">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['biz:activity:edit']">编辑</el-button>
          <el-button size="mini" type="text" icon="el-icon-upload2" @click="handlePublish(scope.row)" v-hasPermi="['biz:activity:edit']" v-if="scope.row.status === 'DRAFT'">发布</el-button>
          <el-button size="mini" type="text" icon="el-icon-finished" @click="handleFinish(scope.row)" v-hasPermi="['biz:activity:edit']" v-if="scope.row.status !== 'FINISHED'">结束</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['biz:activity:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑活动 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="活动名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入活动名称" />
        </el-form-item>
        <el-form-item label="起止时间">
          <el-date-picker v-model="dateRange" type="datetimerange" value-format="yyyy-MM-dd HH:mm:ss" start-placeholder="开始时间" end-placeholder="结束时间" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="业态差异配置">
          <el-select v-model="extraMode" placeholder="请选择配置类型" style="width: 100%;" @change="handleExtraModeChange">
            <el-option label="无配置" value="none" />
            <el-option label="拼桌成团（桌数 × 每桌人数）" value="fullhouse" />
            <el-option label="团购成团（成团阈值）" value="groupbuy" />
            <el-option label="自定义JSON（高级）" value="custom" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="extraMode === 'fullhouse'" label="拼桌配置">
          <span class="extra-label">桌数</span>
          <el-input-number v-model="extraForm.tableCount" :min="1" :max="9999" style="width: 110px;" />
          <span class="extra-label">每桌人数</span>
          <el-input-number v-model="extraForm.perTable" :min="1" :max="9999" style="width: 110px;" />
          <div class="extra-tip">目标人数 = 桌数 × 每桌人数，用于拼桌满员成团判定</div>
        </el-form-item>
        <el-form-item v-if="extraMode === 'groupbuy'" label="成团阈值">
          <el-input-number v-model="extraForm.targetCount" :min="1" :max="999999" style="width: 140px;" />
          <span class="extra-label">份</span>
          <div class="extra-tip">累计下单达到阈值即团购成团，可享阶梯优惠价</div>
        </el-form-item>
        <el-form-item v-if="extraMode === 'custom'" label="自定义JSON">
          <el-input v-model="form.extraConfig" type="textarea" :rows="3" placeholder='JSON，如 {"tableCount":4,"perTable":6}' />
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
  </div>
</template>

<script>
import { listActivity, addActivity, updateActivity, publishActivity, finishActivity, delActivity } from '@/api/biz/activity'

export default {
  name: 'BizActivity',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      activityList: [],
      open: false,
      title: '',
      dateRange: [],
      extraMode: 'none',
      extraForm: { tableCount: null, perTable: null, targetCount: null },
      queryParams: { pageNum: 1, pageSize: 10, name: null, status: null },
      form: {},
      rules: {
        name: [{ required: true, message: '活动名称不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listActivity(this.queryParams).then(response => {
        this.activityList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    statusText(status) {
      const map = { DRAFT: '草稿', PUBLISHED: '已发布', ONGOING: '进行中', FINISHED: '已结束', CLOSED: '已关闭', CANCELLED: '已取消' }
      return map[status] || status
    },
    statusTag(status) {
      const map = { DRAFT: 'info', PUBLISHED: 'primary', ONGOING: 'success', FINISHED: 'warning', CLOSED: 'info', CANCELLED: 'danger' }
      return map[status] || 'info'
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
      this.form = { id: null, name: null, startTime: null, endTime: null, extraConfig: null, remark: null }
      this.dateRange = []
      this.extraMode = 'none'
      this.extraForm = { tableCount: null, perTable: null, targetCount: null }
      this.resetForm('form')
    },
    cancel() {
      this.open = false
      this.reset()
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增活动'
    },
    handleUpdate(row) {
      this.reset()
      this.form = { id: row.id, name: row.name, extraConfig: row.extraConfig, remark: row.remark }
      this.dateRange = row.startTime && row.endTime ? [row.startTime, row.endTime] : []
      this.parseExtraConfig(row.extraConfig)
      this.open = true
      this.title = '编辑活动'
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          this.form.startTime = this.dateRange && this.dateRange.length === 2 ? this.dateRange[0] : null
          this.form.endTime = this.dateRange && this.dateRange.length === 2 ? this.dateRange[1] : null
          if (!this.applyExtraConfig()) return
          if (this.form.id != null) {
            updateActivity(this.form).then(() => {
              this.$modal.msgSuccess('修改成功')
              this.open = false
              this.getList()
            })
          } else {
            addActivity(this.form).then(() => {
              this.$modal.msgSuccess('新增成功')
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleExtraModeChange(val) {
      // 切到自定义模式时，把已选的结构化配置回填为 JSON 草稿，便于继续编辑
      if (val === 'custom' && !this.form.extraConfig) {
        const cfg = {}
        if (this.extraForm.tableCount != null) cfg.tableCount = this.extraForm.tableCount
        if (this.extraForm.perTable != null) cfg.perTable = this.extraForm.perTable
        if (this.extraForm.targetCount != null) cfg.targetCount = this.extraForm.targetCount
        this.form.extraConfig = Object.keys(cfg).length ? JSON.stringify(cfg) : ''
      }
    },
    /** 解析存量 extraConfig JSON：自动识别配置类型并回显到下拉与结构化表单 */
    parseExtraConfig(raw) {
      this.extraMode = 'none'
      this.extraForm = { tableCount: null, perTable: null, targetCount: null }
      if (!raw) return
      let obj = null
      try { obj = JSON.parse(raw) } catch (e) { obj = null }
      if (obj && typeof obj === 'object' && !Array.isArray(obj)) {
        if (obj.tableCount != null || obj.perTable != null) {
          this.extraMode = 'fullhouse'
          this.extraForm.tableCount = obj.tableCount != null ? Number(obj.tableCount) : null
          this.extraForm.perTable = obj.perTable != null ? Number(obj.perTable) : null
          return
        }
        if (obj.targetCount != null) {
          this.extraMode = 'groupbuy'
          this.extraForm.targetCount = Number(obj.targetCount)
          return
        }
      }
      this.extraMode = 'custom'
    },
    /** 下拉+结构化表单序列化为 extraConfig JSON 串（键名与后端 FullHouseStrategy/GroupBuyStrategy 对齐） */
    applyExtraConfig() {
      if (this.extraMode === 'fullhouse') {
        const cfg = {}
        if (this.extraForm.tableCount != null) cfg.tableCount = this.extraForm.tableCount
        if (this.extraForm.perTable != null) cfg.perTable = this.extraForm.perTable
        this.form.extraConfig = Object.keys(cfg).length ? JSON.stringify(cfg) : null
      } else if (this.extraMode === 'groupbuy') {
        this.form.extraConfig = this.extraForm.targetCount != null ? JSON.stringify({ targetCount: this.extraForm.targetCount }) : null
      } else if (this.extraMode === 'custom') {
        const raw = (this.form.extraConfig || '').trim()
        if (!raw) {
          this.form.extraConfig = null
        } else {
          try {
            JSON.parse(raw)
            this.form.extraConfig = raw
          } catch (e) {
            this.$modal.msgError('自定义JSON格式不正确，请检查后提交')
            return false
          }
        }
      } else {
        this.form.extraConfig = null
      }
      return true
    },
    handlePublish(row) {
      this.$modal.confirm('确认发布活动「' + row.name + '」？发布后报名可用').then(() => {
        return publishActivity(row.id)
      }).then(() => {
        this.$modal.msgSuccess('发布成功')
        this.getList()
      }).catch(() => {})
    },
    handleFinish(row) {
      this.$modal.confirm('确认结束活动「' + row.name + '」？结束后不可报名').then(() => {
        return finishActivity(row.id)
      }).then(() => {
        this.$modal.msgSuccess('活动已结束')
        this.getList()
      }).catch(() => {})
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除活动「' + row.name + '」？').then(() => {
        return delActivity(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.extra-label { color: #606266; font-size: 13px; margin: 0 6px; }
.extra-tip { color: #909399; font-size: 12px; line-height: 1.6; }
</style>
