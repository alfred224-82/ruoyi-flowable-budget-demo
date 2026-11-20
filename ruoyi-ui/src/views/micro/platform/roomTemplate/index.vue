<template>
  <div class="app-container">
    <el-alert
      title="平台默认房间模板（T26.6）"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom:12px;">
      <template slot="title">
        平台 Admin 按行业配置默认房间模板（模板名称 + 默认背景图，建议使用简洁线条示意图 SVG/PNG）。Boss 新建房间时按 industry_code 匹配，可一键应用模板快速起步。数据固定 tenant_id=1 + is_industry_default=1，跨租户共享。
      </template>
    </el-alert>

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="行业" prop="industryCode">
        <el-select v-model="queryParams.industryCode" placeholder="全部行业" clearable style="width:200px;">
          <el-option
            v-for="item in industryOptions"
            :key="item.industryCode"
            :label="item.industryName"
            :value="item.industryCode" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['system:platformRoomTemplate:add']">新增模板</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="templateList">
      <el-table-column label="模板名称" align="left" prop="name" min-width="180" show-overflow-tooltip />
      <el-table-column label="所属行业" align="center" width="140">
        <template slot-scope="scope">
          <el-tag size="small">{{ scope.row.industryName || scope.row.industryCode }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="背景图预览" align="center" width="160">
        <template slot-scope="scope">
          <el-image
            v-if="scope.row.bgImageUrl"
            :src="scope.row.bgImageUrl"
            :preview-src-list="[scope.row.bgImageUrl]"
            style="width:120px;height:70px;"
            fit="cover" />
          <span v-else style="color:#909399;">—</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="170" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="180">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['system:platformRoomTemplate:edit']">编辑</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['system:platformRoomTemplate:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑平台默认模板 -->
    <el-dialog :title="title" :visible.sync="open" width="620px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="form.name" placeholder="如：标准包厢 8 人位 / 旅游大巴 45 座" maxlength="100" show-word-limit />
        </el-form-item>
        <el-form-item label="所属行业" prop="industryCode">
          <el-select v-model="form.industryCode" placeholder="请选择行业" style="width:100%;">
            <el-option
              v-for="item in industryOptions"
              :key="item.industryCode"
              :label="item.industryName"
              :value="item.industryCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="背景图" prop="bgImageUrl">
          <el-upload
            class="bg-uploader"
            :action="uploadUrl"
            :headers="uploadHeaders"
            :show-file-list="false"
            :before-upload="beforeBgUpload"
            :on-success="handleBgUploadSuccess"
            :on-error="handleBgUploadError"
            accept="image/png,image/svg+xml">
            <img v-if="form.bgImageUrl" :src="form.bgImageUrl" class="bg-preview" />
            <i v-else class="el-icon-plus bg-uploader-icon"></i>
          </el-upload>
          <div class="upload-hint">
            ★默认背景图必须为透明底 SVG / PNG（简洁线条示意图，非真实照片），≤2MB。前端会按租户主题主色自动铺底联动（M27 T27.2 / T27.3）。
          </div>
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
import {
  listPlatformRoomTemplate, getPlatformRoomTemplate,
  addPlatformRoomTemplate, updatePlatformRoomTemplate, delPlatformRoomTemplate
} from '@/api/micro/platformRoomTemplate'
import { listAllIndustry } from '@/api/micro/industry'
import { getToken } from '@/utils/auth'

export default {
  name: 'MicroPlatformRoomTemplate',
  data() {
    return {
      loading: true,
      showSearch: true,
      templateList: [],
      industryOptions: [],
      open: false,
      title: '',
      queryParams: { industryCode: null },
      form: {},
      rules: {
        name: [
          { required: true, message: '模板名称不能为空', trigger: 'blur' },
          { max: 100, message: '模板名称不能超过 100 字符', trigger: 'blur' }
        ],
        industryCode: [{ required: true, message: '请选择所属行业', trigger: 'change' }]
      },
      // OSS 上传配置（M27 T27.2：默认图专用端点，仅放行透明底 SVG/PNG）
      uploadUrl: process.env.VUE_APP_BASE_API + '/system/platformRoomTemplate/uploadImage',
      uploadHeaders: { Authorization: 'Bearer ' + getToken() }
    }
  },
  created() {
    this.getList()
    this.getIndustryOptions()
  },
  methods: {
    getList() {
      this.loading = true
      listPlatformRoomTemplate(this.queryParams).then(response => {
        this.templateList = response.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    getIndustryOptions() {
      listAllIndustry().then(response => {
        this.industryOptions = response.data || []
      })
    },
    handleQuery() {
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    reset() {
      this.form = { id: null, name: null, industryCode: null, bgImageUrl: null }
      this.resetForm('form')
    },
    cancel() {
      this.open = false
      this.reset()
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增平台默认房间模板'
    },
    handleUpdate(row) {
      this.reset()
      getPlatformRoomTemplate(row.id).then(response => {
        this.form = response.data
        this.open = true
        this.title = '编辑平台默认房间模板'
      })
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) return
        const api = this.form.id != null ? updatePlatformRoomTemplate : addPlatformRoomTemplate
        api(this.form).then(() => {
          this.$modal.msgSuccess(this.form.id != null ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
        }).catch(err => {
          this.$modal.msgError(err.message || '操作失败')
        })
      })
    },
    handleDelete(row) {
      this.$modal.confirm(`确认删除模板「${row.name}」？删除后已应用该模板的房间布局不受影响（脱钩原则）。`).then(() => {
        return delPlatformRoomTemplate(row.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.getList()
      }).catch(() => {})
    },
    // ========== 背景图上传 ==========
    beforeBgUpload(file) {
      // M27 T27.2：默认图仅放行透明底 SVG / PNG（后端同步内容嗅探校验）
      const isTransparent = ['image/png', 'image/svg+xml'].includes(file.type)
        || /\.(svg|png)$/i.test(file.name || '')
      const isLt2M = file.size / 1024 / 1024 < 2
      if (!isTransparent) { this.$modal.msgError('默认背景图仅支持透明底 SVG / PNG（简洁线条示意图）'); return false }
      if (!isLt2M) { this.$modal.msgError('图片大小不能超过 2MB'); return false }
      return true
    },
    handleBgUploadSuccess(res) {
      if (res.code === 200 && res.data && res.data.url) {
        this.form.bgImageUrl = res.data.url
        this.$modal.msgSuccess('上传成功')
      } else {
        this.$modal.msgError(res.msg || '上传失败')
      }
    },
    handleBgUploadError(err) {
      this.$modal.msgError('上传失败：' + (err.message || err))
    }
  }
}
</script>

<style lang="scss" scoped>
.bg-uploader {
  ::v-deep .el-upload {
    border: 1px dashed #d9d9d9;
    border-radius: 6px;
    cursor: pointer;
    position: relative;
    overflow: hidden;
    &:hover { border-color: #409eff; }
  }
  .bg-uploader-icon {
    font-size: 28px;
    color: #8c939d;
    width: 240px;
    height: 140px;
    line-height: 140px;
    text-align: center;
  }
  .bg-preview {
    width: 240px;
    height: 140px;
    display: block;
    object-fit: cover;
  }
}
.upload-hint {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
</style>
