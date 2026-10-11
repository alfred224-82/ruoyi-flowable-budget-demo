<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="企业名称" prop="companyName">
        <el-input
          v-model="queryParams.companyName"
          placeholder="请输入企业名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="租户编码" prop="tenantCode">
        <el-input
          v-model="queryParams.tenantCode"
          placeholder="请输入租户编码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="租户状态" clearable>
          <el-option label="待启用" value="PENDING" />
          <el-option label="正常" value="NORMAL" />
          <el-option label="已停用" value="DISABLED" />
          <el-option label="已到期" value="EXPIRED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['system:tenant:add']"
        >新增</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 数据表格 -->
    <el-table v-loading="loading" :data="tenantList">
      <el-table-column label="租户ID" align="center" prop="id" width="80" />
      <el-table-column label="租户编码" align="center" prop="tenantCode" width="120" />
      <el-table-column label="企业名称" align="center" prop="companyName" min-width="160" show-overflow-tooltip />
      <el-table-column label="联系人" align="center" prop="contactName" width="100" />
      <el-table-column label="联系电话" align="center" prop="contactPhone" width="120" />
      <el-table-column label="产品线" align="center" prop="tenantType" width="90">
        <template slot-scope="scope">
          <el-tag :type="scope.row.tenantType === 'micro' ? 'success' : 'info'" size="small">
            {{ tenantTypeLabel(scope.row.tenantType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="版本档位" align="center" prop="versionType" width="160" show-overflow-tooltip>
        <template slot-scope="scope">
          <span>{{ versionTypeLabel(scope.row.versionType) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="授权类型" align="center" prop="licenseType" width="90">
        <template slot-scope="scope">
          <el-tag :type="scope.row.licenseType === 'FORMAL' ? 'success' : 'warning'" size="small">
            {{ scope.row.licenseType === 'FORMAL' ? '正式' : '试用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="最大用户" align="center" prop="maxUsers" width="90" />
      <el-table-column label="到期时间" align="center" prop="expireDate" width="110">
        <template slot-scope="scope">
          <span>{{ scope.row.expireDate ? parseTime(scope.row.expireDate, '{y}-{m}-{d}') : '永久' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template slot-scope="scope">
          <el-tag :type="statusTagType(scope.row.status)" size="small">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="280">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleDetail(scope.row)"
            v-hasPermi="['system:tenant:query']"
          >详情</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleEdit(scope.row)"
            v-hasPermi="['system:tenant:edit']"
          >编辑</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-s-operation"
            @click="handleAssignVersion(scope.row)"
            v-hasPermi="['system:tenant:edit']"
          >分配版本</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-s-data"
            @click="handleUsage(scope.row)"
            v-hasPermi="['system:tenant:query']"
          >用量</el-button>
          <el-button
            v-if="scope.row.status === 'NORMAL'"
            size="mini"
            type="text"
            icon="el-icon-turn-off"
            @click="handleStatusChange(scope.row, 'DISABLED')"
            v-hasPermi="['system:tenant:edit']"
          >停用</el-button>
          <el-button
            v-if="scope.row.status !== 'NORMAL'"
            size="mini"
            type="text"
            icon="el-icon-open"
            @click="handleStatusChange(scope.row, 'NORMAL')"
            v-hasPermi="['system:tenant:edit']"
          >启用</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 新增/编辑对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="600px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="租户编码" prop="tenantCode" v-if="form.id != null">
              <el-input v-model="form.tenantCode" disabled />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="企业名称" prop="companyName">
              <el-input v-model="form.companyName" placeholder="请输入企业名称" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="联系人" prop="contactName">
              <el-input v-model="form.contactName" placeholder="请输入联系人" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input v-model="form.contactPhone" placeholder="请输入联系电话" />
            </el-form-item>
          </el-col>
        </el-row>
        <!-- ★T15.29 新建租户级联①：选产品线(tenant_type) → 级联版本档位(version_type) → 级联行业模板 -->
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="产品线" prop="tenantType">
              <el-radio-group v-model="form.tenantType" :disabled="form.id != null" @change="handleTenantTypeChange">
                <el-radio label="micro">小微版</el-radio>
                <el-radio label="enterprise">企业版</el-radio>
              </el-radio-group>
              <div v-if="form.id != null" class="form-tip">产品线为身份字段，编辑不可改（后端 updateTenant 不写 tenant_type）。</div>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="版本档位" prop="versionType">
              <el-select
                v-model="form.versionType"
                placeholder="请选择版本档位"
                :disabled="form.id != null"
                style="width: 100%"
                @change="handleVersionTypeChange"
              >
                <el-option v-for="item in formVersionOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
              <div v-if="form.id != null" class="form-tip">档位升降级请点列表「分配版本」，走零迁移接口 assignVersionType。</div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item :label="form.tenantType === 'micro' ? '行业模板' : '行业'" prop="industry">
              <el-select
                v-model="form.industry"
                :placeholder="form.tenantType === 'micro' ? '请选择行业模板' : '请选择行业'"
                style="width: 100%"
              >
                <el-option v-for="item in formIndustryOptions" :key="item.value" :label="item.label" :value="item.value" />
              </el-select>
              <div v-if="form.tenantType === 'micro'" class="form-tip">
                取自 industry_template 启用项（当前 {{ formIndustryOptions.length }} 个）；企业版无行业模板维度。
              </div>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="授权类型" prop="licenseType">
              <el-select v-model="form.licenseType" placeholder="请选择授权类型" style="width: 100%">
                <el-option label="试用" value="TRIAL" />
                <el-option label="正式" value="FORMAL" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="最大用户数" prop="maxUsers">
              <el-input-number v-model="form.maxUsers" :min="1" :max="99999" controls-position="right" style="width: 100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="到期时间" prop="expireDate">
              <el-date-picker
                v-model="form.expireDate"
                type="date"
                value-format="yyyy-MM-dd"
                placeholder="留空表示永久"
                style="width: 100%"
              />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="请输入备注" />
        </el-form-item>
        <!-- 新增租户时显示管理员账号设置 -->
        <el-divider v-if="form.id == null" content-position="left">管理员账号</el-divider>
        <el-row v-if="form.id == null" :gutter="20">
          <el-col :span="12">
            <el-form-item label="管理员账号" prop="username">
              <el-input v-model="form.username" placeholder="请输入管理员登录账号" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="初始密码" prop="password">
              <el-input v-model="form.password" type="password" placeholder="留空则默认 123456" show-password />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 分配版本对话框 -->
    <el-dialog title="分配版本" :visible.sync="versionDialogVisible" width="450px" append-to-body>
      <el-descriptions :column="1" border size="small" style="margin-bottom: 15px">
        <el-descriptions-item label="企业名称">{{ currentTenant.companyName }}</el-descriptions-item>
        <el-descriptions-item label="当前版本">
          <el-tag :type="editionTagType(currentTenant.edition)" size="small" effect="dark">
            {{ editionLabel(currentTenant.edition) }}
          </el-tag>
        </el-descriptions-item>
      </el-descriptions>
      <el-form label-width="80px">
        <el-form-item label="产品线">
          <el-tag :type="currentTenant.tenantType === 'micro' ? 'success' : 'info'" size="small">
            {{ tenantTypeLabel(currentTenant.tenantType) }}
          </el-tag>
        </el-form-item>
        <!-- ★T15.26 零迁移档位升降级：仅 UPDATE version_type + 刷新缓存，角色 code / 用户 / 业务数据零迁移 -->
        <!-- edition 由后端从 version_type 自动推导同步，前端无需选择 -->
        <el-form-item label="版本档位">
          <el-radio-group v-model="targetVersionType">
            <el-radio v-for="item in dialogVersionOptions" :key="item.value" :label="item.value">{{ item.label }}</el-radio>
          </el-radio-group>
          <div class="form-tip">
            当前档位：{{ currentTenant.versionType || '—' }}。候选已按本租户产品线过滤，后端拒绝跨线升降级；
            小微版 lite→pro 升级 = 换 role_template 配方（菜单扩围）。
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="versionLoading" @click="doAssignVersion">确 定</el-button>
        <el-button @click="versionDialogVisible = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 租户详情对话框 -->
    <el-dialog title="租户详情" :visible.sync="detailDialogVisible" width="700px" append-to-body>
      <div v-loading="detailLoading">
        <!-- 基本信息 -->
        <el-descriptions title="基本信息" :column="2" border size="small" style="margin-bottom: 20px">
          <el-descriptions-item label="企业名称" :span="2">{{ detailData.companyName }}</el-descriptions-item>
          <el-descriptions-item label="租户编码">{{ detailData.tenantCode }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ detailData.contactName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detailData.contactPhone }}</el-descriptions-item>
          <el-descriptions-item label="版本">
            <el-tag :type="editionTagType(detailData.edition)" size="small" effect="dark">
              {{ editionLabel(detailData.edition) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="产品线">
            <el-tag :type="detailData.tenantType === 'micro' ? 'success' : 'info'" size="small">
              {{ tenantTypeLabel(detailData.tenantType) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="版本档位">{{ versionTypeLabel(detailData.versionType) }}</el-descriptions-item>
          <el-descriptions-item label="授权类型">{{ detailData.licenseType === 'FORMAL' ? '正式' : '试用' }}</el-descriptions-item>
          <el-descriptions-item label="最大用户数">{{ detailData.maxUsers }}</el-descriptions-item>
          <el-descriptions-item label="到期时间">{{ detailData.expireDate ? parseTime(detailData.expireDate, '{y}-{m}-{d}') : '永久' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusTagType(detailData.status)" size="small">{{ statusLabel(detailData.status) }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 用量统计 -->
        <el-descriptions title="用量统计" :column="3" border size="small" style="margin-bottom: 20px">
          <el-descriptions-item label="用户数">{{ usageData.userCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="部门数">{{ usageData.deptCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="角色数">{{ usageData.roleCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="报销单数">{{ usageData.expenseBillCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="预算编制数">{{ usageData.budgetCount || 0 }}</el-descriptions-item>
          <el-descriptions-item label="版本变更次数">{{ usageData.versionChangeCount || 0 }}</el-descriptions-item>
        </el-descriptions>

        <!-- 版本变更日志 -->
        <div style="margin-bottom: 10px; font-weight: bold; font-size: 14px;">版本变更日志</div>
        <el-table :data="versionLogs" size="mini" border style="margin-bottom: 20px">
          <el-table-column label="变更类型" align="center" prop="changeType" width="100">
            <template slot-scope="scope">
              <el-tag :type="changeTypeTagType(scope.row.changeType)" size="mini">{{ changeTypeLabel(scope.row.changeType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="原版本" align="center" prop="fromEdition" width="100">
            <template slot-scope="scope">{{ editionLabel(scope.row.fromEdition) }}</template>
          </el-table-column>
          <el-table-column label="目标版本" align="center" prop="toEdition" width="100">
            <template slot-scope="scope">{{ editionLabel(scope.row.toEdition) }}</template>
          </el-table-column>
          <el-table-column label="操作人" align="center" prop="operator" width="100" />
          <el-table-column label="备注" align="center" prop="remark" show-overflow-tooltip />
          <el-table-column label="操作时间" align="center" prop="createTime" width="160">
            <template slot-scope="scope">{{ parseTime(scope.row.createTime) }}</template>
          </el-table-column>
        </el-table>
      </div>
      <div slot="footer">
        <el-button @click="detailDialogVisible = false">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- 用量统计对话框 -->
    <el-dialog title="用量统计" :visible.sync="usageDialogVisible" width="500px" append-to-body>
      <el-descriptions :column="2" border v-loading="usageLoading">
        <el-descriptions-item label="企业名称" :span="2">{{ currentTenant.companyName }}</el-descriptions-item>
        <el-descriptions-item label="用户数">{{ usageData.userCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="最大用户数">{{ currentTenant.maxUsers }}</el-descriptions-item>
        <el-descriptions-item label="部门数">{{ usageData.deptCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="角色数">{{ usageData.roleCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="报销单数">{{ usageData.expenseBillCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="预算编制数">{{ usageData.budgetCount || 0 }}</el-descriptions-item>
      </el-descriptions>
      <div slot="footer">
        <el-button @click="usageDialogVisible = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listTenant, getTenant, addTenant, updateTenant,
  changeTenantStatus, assignTenantVersionType, getTenantUsage, listVersionLog
} from '@/api/system/tenant'
// ★T15.29 新建租户级联数据源（perms 复用 system:tenant:list）
import { listVersionOptions, listIndustryOptions } from '@/api/system/edition'

export default {
  name: 'Tenant',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      tenantList: [],
      title: '',
      open: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        companyName: undefined,
        tenantCode: undefined,
        status: undefined
      },
      form: {},
      rules: {
        companyName: [{ required: true, message: '企业名称不能为空', trigger: 'blur' }],
        maxUsers: [{ required: true, message: '最大用户数不能为空', trigger: 'blur' }],
        username: [{ required: true, message: '管理员账号不能为空', trigger: 'blur' }]
      },
      industryOptions: [
        { label: '制造业', value: 'MANUFACTURING' },
        { label: '建筑工程', value: 'CONSTRUCTION' },
        { label: '商贸流通', value: 'TRADE' },
        { label: '现代服务', value: 'SERVICE' },
        { label: '医疗健康', value: 'MEDICAL' },
        { label: '教育培训', value: 'EDUCATION' },
        { label: '信息技术', value: 'IT' },
        { label: '金融保险', value: 'FINANCE' },
        { label: '农林牧渔', value: 'AGRICULTURE' },
        { label: '其他', value: 'OTHER' }
      ],
      // ★T15.29 新建租户级联：产品线 → 版本档位 → 行业模板
      versionOptionsMap: { micro: [], enterprise: [] },
      microIndustryOptions: [],
      // 分配版本（含 T15.26 档位零迁移升降级）
      versionDialogVisible: false,
      versionLoading: false,
      currentTenant: {},
      targetVersionType: '',
      dialogVersionOptions: [],
      // 用量统计
      usageDialogVisible: false,
      usageLoading: false,
      usageData: {},
      // 租户详情
      detailDialogVisible: false,
      detailLoading: false,
      detailData: {},
      versionLogs: []
    }
  },
  computed: {
    /** 表单当前产品线的可选档位（micro→micro_lite/micro_pro；enterprise→basic/advanced/pro） */
    formVersionOptions() {
      return this.versionOptionsMap[this.form.tenantType] || []
    },
    /** 级联②：小微版行业下拉取自 industry_template；企业版沿用内置行业枚举（无行业模板维度） */
    formIndustryOptions() {
      if (this.form.tenantType !== 'micro') {
        return this.industryOptions
      }
      return this.microIndustryOptions
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询租户列表 */
    getList() {
      this.loading = true
      listTenant(this.queryParams).then(response => {
        this.tenantList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    // 表单重置
    reset() {
      this.form = {
        id: undefined,
        tenantCode: undefined,
        companyName: undefined,
        contactName: undefined,
        contactPhone: undefined,
        industry: undefined,
        // ★T15.29 产品线默认 enterprise，档位与后端 deriveVersionType 缺省保持一致
        tenantType: 'enterprise',
        versionType: 'basic',
        licenseType: 'TRIAL',
        expireDate: undefined,
        maxUsers: 50,
        remark: undefined,
        username: undefined,
        password: undefined
      }
      this.resetForm('form')
    },
    /** 搜索按钮 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮 */
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    /** 新增按钮 */
    handleAdd() {
      this.reset()
      // ★T15.29 预载默认产品线的档位候选
      this.loadVersionOptions(this.form.tenantType)
      this.open = true
      this.title = '新增租户'
    },
    /** 编辑按钮 */
    handleEdit(row) {
      this.reset()
      getTenant(row.id).then(response => {
        this.form = response.data
        // 存量租户可能未回填 tenant_type（M15_10 已批量回填，此处兜底避免下拉空白）
        if (!this.form.tenantType) {
          this.form.tenantType = 'enterprise'
        }
        this.loadVersionOptions(this.form.tenantType)
        if (this.form.tenantType === 'micro') {
          this.loadMicroIndustryOptions()
        }
        this.open = true
        this.title = '修改租户'
      })
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) return
        if (this.form.id != null) {
          updateTenant(this.form).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.open = false
            this.getList()
          })
        } else {
          addTenant(this.form).then(() => {
            this.$modal.msgSuccess('新增成功')
            this.open = false
            this.getList()
          })
        }
      })
    },
    /** 取消按钮 */
    cancel() {
      this.open = false
      this.reset()
    },
    /** 启用/停用 */
    handleStatusChange(row, status) {
      const action = status === 'NORMAL' ? '启用' : '停用'
      this.$modal.confirm('确认' + action + '租户【' + row.companyName + '】？').then(() => {
        return changeTenantStatus(row.id, status)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess(action + '成功')
      }).catch(() => {})
    },
    /** 分配版本按钮 */
    handleAssignVersion(row) {
      this.currentTenant = row
      this.targetVersionType = row.versionType || ''
      this.dialogVersionOptions = []
      this.versionDialogVisible = true
      // ★档位候选按本租户产品线加载：后端 assignVersionType 会拒绝跨线档位
      const tenantType = row.tenantType || 'enterprise'
      listVersionOptions(tenantType).then(response => {
        this.dialogVersionOptions = response.data || []
        if (!this.targetVersionType && this.dialogVersionOptions.length) {
          this.targetVersionType = this.dialogVersionOptions[0].value
        }
      }).catch(() => { this.dialogVersionOptions = [] })
    },
    /** 执行分配版本（version_type 为主入口，edition 由后端自动同步） */
    doAssignVersion() {
      const versionChanged = !!this.targetVersionType && this.targetVersionType !== this.currentTenant.versionType
      if (!versionChanged) {
        this.$modal.msgWarning('档位未变更')
        return
      }
      this.versionLoading = true
      assignTenantVersionType(this.currentTenant.id, this.targetVersionType).then(() => {
        this.$modal.msgSuccess('版本分配成功')
        this.versionDialogVisible = false
        this.getList()
      }).catch(() => {}).finally(() => {
        this.versionLoading = false
      })
    },
    // ---- ★T15.29 新建租户级联 ----
    /** 级联①：切换产品线 → 重载档位候选与行业候选，并清空已选值 */
    handleTenantTypeChange(tenantType) {
      this.form.versionType = undefined
      this.form.industry = undefined
      this.loadVersionOptions(tenantType)
      if (tenantType === 'micro') {
        this.loadMicroIndustryOptions()
      }
    },
    loadVersionOptions(tenantType) {
      if (!tenantType || (this.versionOptionsMap[tenantType] || []).length) {
        return
      }
      listVersionOptions(tenantType).then(response => {
        this.$set(this.versionOptionsMap, tenantType, response.data || [])
      }).catch(() => { this.$set(this.versionOptionsMap, tenantType, []) })
    },
    /** 级联②：小微版行业模板（企业版无此维度，直接返回空列表） */
    loadMicroIndustryOptions() {
      if (this.microIndustryOptions.length) {
        return
      }
      listIndustryOptions('micro').then(response => {
        this.microIndustryOptions = response.data || []
      }).catch(() => { this.microIndustryOptions = [] })
    },
    /** 级联②：选定档位（无角色模板预览，角色由租户管理员自行定义） */
    handleVersionTypeChange(versionType) {
      // edition 由后端从 version_type 自动推导，前端不再手动同步
    },
    /** 详情按钮 */
    handleDetail(row) {
      this.detailData = row
      this.usageData = {}
      this.versionLogs = []
      this.detailDialogVisible = true
      this.detailLoading = true
      // 并行加载用量和版本日志
      getTenantUsage(row.id).then(res => {
        this.usageData = res.data || {}
      })
      listVersionLog(row.id).then(res => {
        this.versionLogs = res.data || []
      }).finally(() => {
        this.detailLoading = false
      })
    },
    /** 用量统计按钮 */
    handleUsage(row) {
      this.currentTenant = row
      this.usageDialogVisible = true
      this.usageLoading = true
      getTenantUsage(row.id).then(response => {
        this.usageData = response.data || {}
      }).finally(() => {
        this.usageLoading = false
      })
    },
    // ---- 辅助方法 ----
    editionLabel(edition) {
      const map = { BASIC: '基础版', ADVANCED: '高级版', PROFESSIONAL: '专业版', ENTERPRISE: '企业版' }
      return map[edition] || edition || '-'
    },
    editionTagType(edition) {
      const map = { BASIC: 'info', ADVANCED: 'warning', PROFESSIONAL: 'danger', ENTERPRISE: 'danger' }
      return map[edition] || 'info'
    },
    statusLabel(status) {
      const map = { PENDING: '待启用', NORMAL: '正常', DISABLED: '已停用', EXPIRED: '已到期' }
      return map[status] || status || '-'
    },
    statusTagType(status) {
      const map = { PENDING: 'warning', NORMAL: 'success', DISABLED: 'info', EXPIRED: 'danger' }
      return map[status] || 'info'
    },
    changeTypeLabel(type) {
      const map = { INIT: '初始化', UPGRADE: '升级', DOWNGRADE: '降级' }
      return map[type] || type || '-'
    },
    changeTypeTagType(type) {
      const map = { INIT: 'success', UPGRADE: 'warning', DOWNGRADE: 'danger' }
      return map[type] || 'info'
    },
    tenantTypeLabel(tenantType) {
      const map = { micro: '小微版', enterprise: '企业版' }
      return map[tenantType] || '企业版'
    },
    versionTypeLabel(versionType) {
      const map = {
        micro_lite: '小微版·推广档(免费)',
        micro_pro: '小微版·经营档(收费)',
        basic: '企业版·基础版',
        advanced: '企业版·高级版',
        pro: '企业版·专业版'
      }
      return map[versionType] || versionType || '-'
    }
  }
}
</script>

<style scoped>
.form-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}
</style>
