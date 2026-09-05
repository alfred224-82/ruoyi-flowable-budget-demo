<template>
  <div class="app-container">
    <!-- 租户选择器（T3.17） -->
    <el-card class="box-card" shadow="hover" style="margin-bottom: 15px">
      <div slot="header" class="clearfix">
        <span><i class="el-icon-office-building" /> 租户选择</span>
      </div>
      <el-row :gutter="12">
        <el-col :span="8">
          <el-select
            v-model="selectedTenantId"
            filterable
            remote
            reserve-keyword
            clearable
            placeholder="请选择租户（支持名称搜索）"
            :remote-method="searchTenants"
            :loading="tenantLoading"
            style="width: 100%"
            @change="handleTenantChange"
            @clear="handleTenantClear"
          >
            <el-option-group
              v-for="(tenants, edition) in tenantGroups"
              :key="edition"
              :label="editionGroupLabel(edition)"
            >
              <el-option
                v-for="t in tenants"
                :key="t.id"
                :label="t.companyName"
                :value="t.id"
              >
                <span>{{ t.companyName }}</span>
                <span style="float: right; color: #8492a6; font-size: 12px">
                  {{ t.hasLicense ? t.licenseId : '未授权' }}
                </span>
              </el-option>
            </el-option-group>
          </el-select>
        </el-col>
        <el-col :span="4">
          <el-tag v-if="selectedTenantId" :type="tenantLicenseInfo.status === 'ACTIVE' ? 'success' : 'info'" size="large">
            {{ tenantLicenseInfo.licenseId === '未授权' ? '未导入 License' : '已导入 License' }}
          </el-tag>
          <el-tag v-else type="info">请选择租户</el-tag>
        </el-col>
      </el-row>
    </el-card>

    <!-- 授权信息卡片 -->
    <el-card class="box-card" shadow="hover">
      <div slot="header" class="clearfix">
        <span><i class="el-icon-key" /> 授权信息</span>
        <el-button
          v-hasPermi="['system:license:manage']"
          style="float: right; padding: 3px 10px"
          type="primary"
          size="mini"
          @click="openEditionDialog"
        >切换版本 / 更新授权</el-button>
        <el-button
          v-hasPermi="['system:tenant:edit']"
          v-if="selectedTenantId"
          style="float: right; padding: 3px 10px; margin-right: 10px"
          type="success"
          size="mini"
          @click="openImportDialog"
        >导入租户 License</el-button>
        <el-button
          v-hasPermi="['system:license:manage']"
          style="float: right; padding: 3px 10px; margin-right: 10px"
          type="warning"
          size="mini"
          @click="handleFingerprint"
        >导出机器指纹</el-button>
      </div>
      <el-descriptions :column="3" border v-loading="loading">
        <el-descriptions-item label="授权编号">
          <span>{{ licenseInfo.licenseId || '未授权' }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="授权类型">
          <el-tag :type="licenseInfo.licenseType === 'FORMAL' ? 'success' : 'warning'" size="small">
            {{ licenseTypeLabel }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="客户名称">
          <span>{{ licenseInfo.customer || '-' }}</span>
          <el-button
            v-hasPermi="['system:license:manage']"
            type="text"
            size="mini"
            icon="el-icon-edit"
            style="margin-left: 5px"
            @click="openCustomerDialog"
          >修改</el-button>
        </el-descriptions-item>
        <el-descriptions-item label="版本等级">
          <el-tag :type="editionTagType" effect="dark" size="medium">{{ editionLabel }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="授权状态">
          <el-tag :type="statusTagType" size="small">{{ statusLabel }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="到期时间">
          {{ licenseInfo.expiresAt || '永久授权' }}
        </el-descriptions-item>
        <el-descriptions-item label="试用剩余" v-if="licenseInfo.licenseType === 'TRIAL'">
          <span :class="{ 'trial-warning': trialRemainingDays <= 3 }">
            {{ trialRemainingText }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="授权行业" :span="2">
          <el-tag
            v-for="item in licenseInfo.industries"
            :key="item"
            size="mini"
            style="margin-right: 5px; margin-bottom: 3px"
          >{{ industryLabel(item) }}</el-tag>
          <span v-if="!licenseInfo.industries || licenseInfo.industries.length === 0">全部行业</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 功能开关列表 -->
    <el-card class="box-card" shadow="hover" style="margin-top: 15px">
      <div slot="header" class="clearfix">
        <span><i class="el-icon-s-operation" /> 功能开关清单</span>
        <el-tag size="mini" type="info" style="float: right">已启用 {{ enabledCount }} / {{ allFeatures.length }}</el-tag>
        <el-radio-group v-model="editionFilter" size="mini" style="float: right; margin-right: 12px">
          <el-radio-button label="ALL">全部({{ allFeatures.length }})</el-radio-button>
          <el-radio-button label="ADVANCED">高级版({{ advancedCount }})</el-radio-button>
          <el-radio-button label="ENTERPRISE">企业版独有({{ enterpriseOnlyCount }})</el-radio-button>
        </el-radio-group>
      </div>
      <el-alert
        :title="'当前版本【' + editionLabel + '】' + editionFeatureSummary"
        type="info"
        :closable="false"
        style="margin-bottom: 12px"
      />
      <el-table :data="filteredFeatures" border stripe size="small" v-loading="loading">
        <el-table-column label="功能编码" prop="code" width="240" />
        <el-table-column label="功能名称" prop="name" min-width="180" />
        <el-table-column label="所属模块" prop="module" width="100" align="center">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.module === '费用模块' ? '' : 'success'">{{ scope.row.module }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="所属版本" width="120" align="center">
          <template slot-scope="scope">
            <el-tag size="mini" :type="scope.row.edition === 'ADVANCED' ? 'warning' : 'danger'" effect="plain">
              {{ scope.row.edition === 'ADVANCED' ? '高级版' : '企业版' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template slot-scope="scope">
            <el-tag size="small" :type="isFeatureEnabled(scope.row.code) ? 'success' : 'info'">
              {{ isFeatureEnabled(scope.row.code) ? '已启用' : '未启用' }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div class="edition-legend">
        <span class="legend-item"><el-tag size="mini" type="info" effect="dark">基础版</el-tag> 核心编审闭环：单级审批、固定5条风控规则、结果通知（无额外功能开关）</span>
        <span class="legend-item"><el-tag size="mini" type="warning" effect="dark">高级版</el-tag> 含 {{ advancedCount }} 项增值功能：可配置审批流、风控自定义、AI辅助、资金池（部门级）</span>
        <span class="legend-item"><el-tag size="mini" type="danger" effect="dark">企业版</el-tag> 全部 {{ allFeatures.length }} 项功能开放：三级审批、三级资金池、计提引擎、AI私有化部署</span>
      </div>
    </el-card>

    <!-- 版本切换/更新授权对话框 -->
    <el-dialog title="切换版本 / 更新授权" :visible.sync="editionDialogVisible" width="520px" append-to-body>
      <el-alert
        title="切换后功能开关立即生效。试用授权到期后系统自动降级为基础版。"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 15px"
      />
      <el-form ref="editionForm" :model="editionForm" :rules="editionRules" label-width="100px">
        <el-form-item label="目标版本" prop="edition">
          <el-radio-group v-model="editionForm.edition">
            <el-radio label="BASIC">基础版</el-radio>
            <el-radio label="ADVANCED">高级版</el-radio>
            <el-radio label="ENTERPRISE">企业版</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="授权类型" prop="licenseType">
          <el-radio-group v-model="editionForm.licenseType" @change="handleLicenseTypeChange">
            <el-radio label="TRIAL">试用授权</el-radio>
            <el-radio label="FORMAL">正式授权</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="授权期限" prop="duration">
          <el-select v-model="editionForm.duration" placeholder="请选择授权期限" style="width: 100%">
            <el-option v-if="editionForm.licenseType === 'TRIAL'" label="7天（试用）" value="7" />
            <template v-if="editionForm.licenseType === 'FORMAL'">
              <el-option label="3个月" value="90" />
              <el-option label="1年" value="365" />
              <el-option label="永久" value="permanent" />
            </template>
          </el-select>
          <div class="duration-hint" v-if="editionForm.duration && editionForm.duration !== 'permanent'">
            预计到期时间：{{ expectedExpireDate }}
          </div>
          <div class="duration-hint" v-if="editionForm.duration === 'permanent'">
            永久授权，无到期时间
          </div>
        </el-form-item>
        <el-form-item label="客户名称" prop="customer">
          <el-input v-model="editionForm.customer" placeholder="请输入客户真实名称" maxlength="100" show-word-limit />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="editionDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="switchLoading" @click="handleSwitchEdition">确 定</el-button>
      </div>
    </el-dialog>

    <!-- 修改客户名称对话框 -->
    <el-dialog title="修改客户名称" :visible.sync="customerDialogVisible" width="420px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="客户名称">
          <el-input v-model="customerName" placeholder="请输入客户真实名称" maxlength="100" show-word-limit />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="customerDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="customerLoading" @click="handleSaveCustomer">保 存</el-button>
      </div>
    </el-dialog>

    <!-- 机器指纹对话框 -->
    <el-dialog title="机器指纹" :visible.sync="fingerprintDialogVisible" width="560px" append-to-body>
      <el-alert
        title="将以下指纹信息提供给授权服务商，用于申请或续期License。"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 15px"
      />
      <el-input
        type="textarea"
        :rows="4"
        :value="fingerprint"
        readonly
        ref="fingerprintInput"
      />
      <div slot="footer" class="dialog-footer">
        <el-button @click="fingerprintDialogVisible = false">关 闭</el-button>
        <el-button type="primary" icon="el-icon-document-copy" @click="copyFingerprint">复 制</el-button>
      </div>
    </el-dialog>

    <!-- 导入租户 License 对话框（T3.14） -->
    <el-dialog title="导入租户 License" :visible.sync="importDialogVisible" width="620px" append-to-body>
      <el-alert
        title="粘贴平台签发的 License JSON 文件内容，导入后将激活租户对应版本功能。同步更新租户到期日和版本等级。"
        type="warning"
        :closable="false"
        show-icon
        style="margin-bottom: 15px"
      />
      <el-form label-width="100px">
        <el-form-item label="目标租户">
          <el-tag type="primary">{{ selectedTenantName }}</el-tag>
        </el-form-item>
        <el-form-item label="License JSON">
          <el-input
            type="textarea"
            :rows="10"
            v-model="importLicenseJson"
            placeholder='粘贴完整的 License JSON 内容，如：&#10;{&#10;  "licenseId": "LIC-2026-000001",&#10;  "edition": "ADVANCED",&#10;  "licenseType": "FORMAL",&#10;  ...&#10;}'
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button @click="importDialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="importLoading" @click="handleImportLicense">导 入</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getLicenseInfo, getFingerprint, replaceLicense } from '@/api/system/license'
import { importTenantLicense, getTenantLicense, getTenantSelector } from '@/api/system/tenant'

// 全量功能清单（与后端 EditionFeatureConfig 保持同步）
// edition: 该功能最低要求的版本等级（ADVANCED=高级版起含, ENTERPRISE=仅企业版）
const ALL_FEATURES = [
  // === 费用模块 · 高级版 ===
  { code: 'FEE_MULTI_CURRENCY', name: '多币种报销', module: '费用模块', edition: 'ADVANCED' },
  { code: 'FEE_COMPLIANCE_CUSTOM', name: '合规级别自定义配置', module: '费用模块', edition: 'ADVANCED' },
  { code: 'FEE_INDUSTRY_SINGLE', name: '单行业配置', module: '费用模块', edition: 'ADVANCED' },
  { code: 'FEE_TRACK_CONTROL', name: '三轨预算管控', module: '费用模块', edition: 'ADVANCED' },
  { code: 'FEE_PROFIT_CUSTOM_TIP', name: '盈利提示自定义', module: '费用模块', edition: 'ADVANCED' },
  { code: 'FEE_SUBSIDY_CONFIG', name: '补助标准配置', module: '费用模块', edition: 'ADVANCED' },
  { code: 'FEE_FUND_POOL', name: '资金池（高级版部门级/企业版三级）', module: '费用模块', edition: 'ADVANCED' },
  { code: 'FEE_AI_CUSTOM_PROMPT', name: 'AI话术自定义', module: '费用模块', edition: 'ADVANCED' },
  { code: 'FEE_AI_CONFIDENCE_REVIEW', name: 'AI置信度转人工', module: '费用模块', edition: 'ADVANCED' },
  // === 费用模块 · 企业版独有 ===
  { code: 'FEE_INDUSTRY_PACK_IO', name: '行业配置包导入导出', module: '费用模块', edition: 'ENTERPRISE' },
  { code: 'FEE_ACCRUAL_ENGINE', name: '周期计提引擎（资产折旧+合同摊销）', module: '费用模块', edition: 'ENTERPRISE' },
  { code: 'FEE_LOAN_LEDGER', name: '借款台账+利息计提', module: '费用模块', edition: 'ENTERPRISE' },
  { code: 'FEE_AI_PRIVATE_DEPLOY', name: 'AI私有化部署', module: '费用模块', edition: 'ENTERPRISE' },
  { code: 'FEE_DESENSITIZE_CUSTOM', name: '数据脱敏自定义', module: '费用模块', edition: 'ENTERPRISE' },
  { code: 'FEE_CROSS_DEPT_ALLOCATE', name: '跨部门分摊', module: '费用模块', edition: 'ENTERPRISE' },
  { code: 'FEE_AUDIT_FULL_EXPORT', name: '全节点审计导出', module: '费用模块', edition: 'ENTERPRISE' },
  // === 预算模块 · 高级版 ===
  { code: 'BUDGET_BATCH_SUBMIT', name: '批量提交审核', module: '预算模块', edition: 'ADVANCED' },
  { code: 'BUDGET_BATCH_APPROVE', name: '批量审批', module: '预算模块', edition: 'ADVANCED' },
  { code: 'BUDGET_PERIOD_CONTROL', name: '编制期管控+提醒', module: '预算模块', edition: 'ADVANCED' },
  { code: 'BUDGET_MULTI_LEVEL_APPROVAL', name: '可配置多级审批', module: '预算模块', edition: 'ADVANCED' },
  { code: 'BUDGET_DEADLINE_REMIND', name: '截止前提醒', module: '预算模块', edition: 'ADVANCED' },
  { code: 'BUDGET_RISK_WARNING', name: 'WARNING级风控', module: '预算模块', edition: 'ADVANCED' },
  { code: 'BUDGET_RISK_CUSTOM', name: '风控规则自定义', module: '预算模块', edition: 'ADVANCED' },
  { code: 'BUDGET_AI_CHAT', name: 'AI对话', module: '预算模块', edition: 'ADVANCED' },
  { code: 'BUDGET_AI_REPORT', name: 'AI报表生成', module: '预算模块', edition: 'ADVANCED' },
  // === 预算模块 · 企业版独有 ===
  { code: 'BUDGET_THREE_LEVEL_APPROVAL', name: '固定三级审批（部门→分公司→总公司）', module: '预算模块', edition: 'ENTERPRISE' },
  { code: 'BUDGET_SEND_BACK', name: '打回至部门', module: '预算模块', edition: 'ENTERPRISE' },
  { code: 'BUDGET_TIMEOUT_ALERT', name: '超时告警', module: '预算模块', edition: 'ENTERPRISE' },
  { code: 'BUDGET_RISK_FORMULA', name: '公式/建议性规则', module: '预算模块', edition: 'ENTERPRISE' },
  { code: 'BUDGET_RISK_AI', name: 'AI辅助风控', module: '预算模块', edition: 'ENTERPRISE' }
]

const INDUSTRY_MAP = {
  'MANUFACTURING': '制造业',
  'CONSTRUCTION': '建筑工程',
  'TRADE': '商贸流通',
  'SERVICE': '现代服务',
  'MEDICAL': '医疗健康',
  'EDUCATION': '教育培训',
  'IT': '信息技术',
  'FINANCE': '金融保险',
  'AGRICULTURE': '农林牧渔',
  'OTHER': '其他'
}

export default {
  name: 'License',
  data() {
    return {
      loading: false,
      licenseInfo: {},
      allFeatures: ALL_FEATURES,
      editionFilter: 'ALL',
      // 版本切换对话框
      editionDialogVisible: false,
      editionForm: {
        edition: 'BASIC',
        licenseType: 'TRIAL',
        duration: '7',
        customer: ''
      },
      editionRules: {
        edition: [{ required: true, message: '请选择目标版本', trigger: 'change' }],
        licenseType: [{ required: true, message: '请选择授权类型', trigger: 'change' }],
        duration: [{ required: true, message: '请选择授权期限', trigger: 'change' }],
        customer: [{ required: true, message: '请输入客户名称', trigger: 'blur' }]
      },
      switchLoading: false,
      // 客户名称对话框
      customerDialogVisible: false,
      customerName: '',
      customerLoading: false,
      // 指纹
      fingerprintDialogVisible: false,
      fingerprint: '',
      // 租户选择器（T3.17）
      selectedTenantId: null,
      selectedTenantName: '',
      tenantGroups: {},
      tenantLoading: false,
      tenantLicenseInfo: {},
      // 导入 License 对话框（T3.14）
      importDialogVisible: false,
      importLicenseJson: '',
      importLoading: false
    }
  },
  computed: {
    editionLabel() {
      const map = { 'BASIC': '基础版', 'ADVANCED': '高级版', 'ENTERPRISE': '企业版' }
      return map[this.licenseInfo.edition] || '未知'
    },
    editionTagType() {
      const map = { 'BASIC': 'info', 'ADVANCED': 'warning', 'ENTERPRISE': 'danger' }
      return map[this.licenseInfo.edition] || 'info'
    },
    licenseTypeLabel() {
      if (this.licenseInfo.licenseType === 'FORMAL') return '正式授权'
      if (this.licenseInfo.licenseType === 'TRIAL') {
        const days = this.licenseInfo.trialDays || 7
        return `试用授权（${days}天）`
      }
      return '未授权'
    },
    statusLabel() {
      const map = { 'ACTIVE': '正常', 'EXPIRED': '已过期', 'REVOKED': '已吊销', 'TAMPERED': '已篡改' }
      return map[this.licenseInfo.status] || (this.licenseInfo.status || '未授权')
    },
    statusTagType() {
      const map = { 'ACTIVE': 'success', 'EXPIRED': 'warning', 'REVOKED': 'danger', 'TAMPERED': 'danger' }
      return map[this.licenseInfo.status] || 'info'
    },
    trialRemainingDays() {
      return Math.floor((this.licenseInfo.trialRemainingHours || 0) / 24)
    },
    trialRemainingText() {
      const hours = this.licenseInfo.trialRemainingHours || 0
      if (hours <= 0) return '已过期'
      const days = Math.floor(hours / 24)
      const remainHours = hours % 24
      return days > 0 ? `${days}天${remainHours}小时` : `${remainHours}小时`
    },
    enabledCount() {
      const features = this.licenseInfo.features || []
      return this.allFeatures.filter(f => features.includes(f.code)).length
    },
    /** 高级版功能数（含企业版也有的） */
    advancedCount() {
      return this.allFeatures.filter(f => f.edition === 'ADVANCED').length
    },
    /** 企业版独有功能数 */
    enterpriseOnlyCount() {
      return this.allFeatures.filter(f => f.edition === 'ENTERPRISE').length
    },
    /** 按版本筛选后的功能列表 */
    filteredFeatures() {
      if (this.editionFilter === 'ALL') return this.allFeatures
      return this.allFeatures.filter(f => f.edition === this.editionFilter)
    },
    /** 当前版本功能摘要 */
    editionFeatureSummary() {
      const edition = this.licenseInfo.edition
      if (edition === 'ENTERPRISE') return '已开放全部 ' + this.allFeatures.length + ' 项功能'
      if (edition === 'ADVANCED') return '已开放 ' + this.advancedCount + ' 项高级功能，升级企业版可再解锁 ' + this.enterpriseOnlyCount + ' 项'
      return '仅含核心编审闭环，升级高级版可解锁 ' + this.advancedCount + ' 项功能'
    },
    /** 预计到期时间 */
    expectedExpireDate() {
      const days = parseInt(this.editionForm.duration)
      if (!days) return ''
      const d = new Date()
      d.setDate(d.getDate() + days)
      const y = d.getFullYear()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${y}-${m}-${day}`
    }
  },
  created() {
    this.getInfo()
    this.loadTenantSelector()
  },
  methods: {
    getInfo() {
      this.loading = true
      // 如果选中了租户，加载租户级 License 信息；否则加载系统级
      if (this.selectedTenantId) {
        getTenantLicense(this.selectedTenantId).then(response => {
          this.licenseInfo = response.data || {}
          this.loading = false
        }).catch(() => {
          this.loading = false
        })
      } else {
        getLicenseInfo().then(response => {
          this.licenseInfo = response.data || {}
          this.loading = false
        }).catch(() => {
          this.loading = false
        })
      }
    },
    isFeatureEnabled(code) {
      return (this.licenseInfo.features || []).includes(code)
    },
    industryLabel(code) {
      return INDUSTRY_MAP[code] || code
    },
    /** 打开版本切换对话框 */
    openEditionDialog() {
      this.editionForm = {
        edition: this.licenseInfo.edition || 'BASIC',
        licenseType: this.licenseInfo.licenseType || 'TRIAL',
        duration: this.licenseInfo.licenseType === 'FORMAL'
          ? (this.licenseInfo.expiresAt === '永久' ? 'permanent' : '365')
          : '7',
        customer: this.licenseInfo.customer || ''
      }
      this.editionDialogVisible = true
    },
    /** 授权类型切换时重置期限 */
    handleLicenseTypeChange(val) {
      this.editionForm.duration = val === 'TRIAL' ? '7' : '365'
    },
    /** 执行版本切换 */
    handleSwitchEdition() {
      this.$refs.editionForm.validate(valid => {
        if (!valid) return
        this.switchLoading = true
        const payload = {
          edition: this.editionForm.edition,
          licenseType: this.editionForm.licenseType,
          customer: this.editionForm.customer,
          durationDays: this.editionForm.duration === 'permanent' ? null : parseInt(this.editionForm.duration)
        }
        replaceLicense(JSON.stringify(payload)).then(() => {
          this.$modal.msgSuccess('授权更新成功')
          this.editionDialogVisible = false
          this.getInfo()
        }).finally(() => {
          this.switchLoading = false
        })
      })
    },
    /** 打开修改客户名称对话框 */
    openCustomerDialog() {
      this.customerName = this.licenseInfo.customer || ''
      this.customerDialogVisible = true
    },
    /** 保存客户名称 */
    handleSaveCustomer() {
      if (!this.customerName || !this.customerName.trim()) {
        this.$modal.msgWarning('请输入客户名称')
        return
      }
      this.customerLoading = true
      replaceLicense(JSON.stringify({ customer: this.customerName.trim() })).then(() => {
        this.$modal.msgSuccess('客户名称修改成功')
        this.customerDialogVisible = false
        this.getInfo()
      }).finally(() => {
        this.customerLoading = false
      })
    },
    /** 导出机器指纹 */
    handleFingerprint() {
      getFingerprint().then(response => {
        this.fingerprint = response.data || ''
        this.fingerprintDialogVisible = true
      })
    },
    /** 复制指纹到剪贴板 */
    copyFingerprint() {
      const text = this.fingerprint
      if (!text) {
        this.$modal.msgWarning('指纹内容为空')
        return
      }
      // 优先使用 Clipboard API
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(() => {
          this.$modal.msgSuccess('复制成功')
        }).catch(() => {
          this.fallbackCopy(text)
        })
      } else {
        this.fallbackCopy(text)
      }
    },
    /** 降级复制方案：操作真实textarea元素 */
    fallbackCopy(text) {
      const textarea = document.createElement('textarea')
      textarea.value = text
      textarea.style.position = 'fixed'
      textarea.style.opacity = '0'
      document.body.appendChild(textarea)
      textarea.select()
      try {
        document.execCommand('copy')
        this.$modal.msgSuccess('复制成功')
      } catch (e) {
        this.$modal.msgError('复制失败，请手动选择复制')
      }
      document.body.removeChild(textarea)
    },
    // ==================== 租户选择器（T3.17） ====================
    /** 加载租户选择器数据 */
    loadTenantSelector(name) {
      this.tenantLoading = true
      getTenantSelector(name).then(response => {
        this.tenantGroups = response.data || {}
        this.tenantLoading = false
      }).catch(() => {
        this.tenantLoading = false
      })
    },
    /** 远程搜索租户 */
    searchTenants(query) {
      this.loadTenantSelector(query)
    },
    /** 版本编码转中文（分组标签用） */
    editionGroupLabel(edition) {
      const map = { 'BASIC': '基础版', 'ADVANCED': '高级版', 'PROFESSIONAL': '专业版', 'ENTERPRISE': '企业版' }
      return map[edition] || edition
    },
    /** 选中租户后加载 License 信息 */
    handleTenantChange(tenantId) {
      if (!tenantId) {
        this.tenantLicenseInfo = {}
        this.selectedTenantName = ''
        return
      }
      // 找到租户名称
      for (const tenants of Object.values(this.tenantGroups)) {
        const found = tenants.find(t => t.id === tenantId)
        if (found) {
          this.selectedTenantName = found.companyName
          break
        }
      }
      // 加载租户 License 信息
      getTenantLicense(tenantId).then(response => {
        this.tenantLicenseInfo = response.data || {}
        // 同步刷新主信息卡片为租户 License
        this.getInfo()
      })
    },
    /** 清除租户选择 */
    handleTenantClear() {
      this.selectedTenantId = null
      this.selectedTenantName = ''
      this.tenantLicenseInfo = {}
      // 恢复为系统级 License 信息
      this.getInfo()
    },
    /** 打开导入 License 对话框 */
    openImportDialog() {
      this.importLicenseJson = ''
      this.importDialogVisible = true
    },
    /** 执行导入 License */
    handleImportLicense() {
      if (!this.importLicenseJson || !this.importLicenseJson.trim()) {
        this.$modal.msgWarning('请粘贴 License JSON 内容')
        return
      }
      this.importLoading = true
      importTenantLicense(this.selectedTenantId, this.importLicenseJson.trim()).then(() => {
        this.$modal.msgSuccess('License 导入成功，租户功能已激活')
        this.importDialogVisible = false
        // 刷新租户 License 信息
        this.handleTenantChange(this.selectedTenantId)
        // 刷新选择器数据
        this.loadTenantSelector()
      }).finally(() => {
        this.importLoading = false
      })
    }
  }
}
</script>

<style scoped>
.trial-warning {
  color: #e6a23c;
  font-weight: bold;
}
.duration-hint {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
  line-height: 1.4;
}
.edition-legend {
  margin-top: 12px;
  padding: 10px 12px;
  background: #f8f9fa;
  border-radius: 4px;
}
.edition-legend .legend-item {
  display: block;
  font-size: 12px;
  color: #606266;
  line-height: 2.2;
}
</style>
