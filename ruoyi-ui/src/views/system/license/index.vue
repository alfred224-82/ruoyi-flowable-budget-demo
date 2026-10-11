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
          type="warning"
          size="mini"
          :disabled="!selectedTenantId"
          @click="handleExportLicense"
        >导出 License</el-button>
      </div>
      <el-descriptions :column="3" border v-loading="loading" v-if="selectedTenantId">
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
      <el-empty v-else description="请先选择租户，再查看授权信息" :image-size="80" />
    </el-card>

    <!-- 功能开关列表 -->
    <el-card class="box-card" shadow="hover" style="margin-top: 15px">
      <div slot="header" class="clearfix">
        <span><i class="el-icon-s-operation" /> 功能开关清单</span>
        <el-tag size="mini" type="info" style="float: right">已启用 {{ enabledCount }} / {{ allFeatures.length }}</el-tag>
        <el-radio-group v-model="editionFilter" size="mini" style="float: right; margin-right: 12px">
          <el-radio-button label="ALL">全部({{ allFeatures.length }})</el-radio-button>
          <el-radio-button label="MICRO">小微版({{ microCount }})</el-radio-button>
          <el-radio-button label="BASIC">基础版({{ basicCount }})</el-radio-button>
          <el-radio-button label="ADVANCED">高级版({{ advancedCount }})</el-radio-button>
          <el-radio-button label="PROFESSIONAL">专业版独有({{ professionalOnlyCount }})</el-radio-button>
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
            <el-tag size="mini" :type="editionTagOfRow(scope.row.edition)" effect="plain">
              {{ editionTextOfRow(scope.row.edition) }}
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
        <span class="legend-item"><el-tag size="mini" type="success" effect="dark">小微版</el-tag> 含 {{ microCount }} 项专属功能：推广发布/店铺管理/预约订单/防爽约/推广分销/通用评价（micro_lite 免费档 / micro_pro 收费档）</span>
        <span class="legend-item"><el-tag size="mini" type="info" effect="dark">基础版</el-tag> 核心编审闭环（{{ basicCount }} 项）：单级审批、固定5条风控规则、结果通知、薪资/资产/利息计提</span>
        <span class="legend-item"><el-tag size="mini" type="warning" effect="dark">高级版</el-tag> 含 {{ advancedCount }} 项增值功能：可配置审批流、风控自定义、AI辅助、资金池（部门级）</span>
        <span class="legend-item"><el-tag size="mini" type="danger" effect="dark">专业版</el-tag> 全部 {{ allFeatures.length }} 项功能开放：三级审批、三级资金池、计提引擎、AI私有化部署</span>
      </div>
    </el-card>

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

    <!-- 导出 License 对话框（T15.33+ 改造：原「机器指纹」改为「签发 License JSON」） -->
    <el-dialog title="导出 License" :visible.sync="exportLicenseDialogVisible" width="720px" append-to-body>
      <el-alert
        title="配置授权参数后点击「生成 License」，复制生成的 JSON 交给租户管理员，在其「版本升级」页面粘贴导入即可激活对应版本。"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 15px"
      />
      <!-- 配置表单 -->
      <el-form ref="exportForm" :model="exportForm" :rules="exportRules" label-width="100px" v-if="!exportLicenseJson">
        <el-form-item label="目标租户">
          <el-tag v-if="selectedTenantId" type="primary">{{ selectedTenantName }}</el-tag>
          <el-tag v-else type="info">未选择（将生成系统级 License）</el-tag>
        </el-form-item>
        <el-form-item label="目标产品线" prop="tenantType">
          <el-radio-group v-model="exportForm.tenantType" @change="handleExportTenantTypeChange">
            <el-radio label="micro">小微版</el-radio>
            <el-radio label="enterprise">企业版</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="版本档位" prop="versionType">
          <el-radio-group v-model="exportForm.versionType" @change="handleExportVersionTypeChange">
            <template v-if="exportForm.tenantType === 'micro'">
              <el-radio label="micro_lite">小微版·推广档(免费)</el-radio>
              <el-radio label="micro_pro">小微版·经营档(收费)</el-radio>
            </template>
            <template v-else>
              <el-radio label="basic">基础版</el-radio>
              <el-radio label="advanced">高级版</el-radio>
              <el-radio label="pro">专业版</el-radio>
            </template>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="授权类型" prop="licenseType">
          <el-radio-group v-model="exportForm.licenseType" @change="handleExportLicenseTypeChange">
            <el-radio label="TRIAL">试用授权</el-radio>
            <el-radio label="FORMAL">正式授权</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="授权期限" prop="duration">
          <el-select v-model="exportForm.duration" placeholder="请选择授权期限" style="width: 100%">
            <el-option v-if="exportForm.licenseType === 'TRIAL'" label="7天（试用）" value="7" />
            <template v-if="exportForm.licenseType === 'FORMAL'">
              <el-option label="3个月" value="90" />
              <el-option label="1年" value="365" />
              <el-option label="永久" value="permanent" />
            </template>
          </el-select>
          <div class="duration-hint" v-if="exportForm.duration && exportForm.duration !== 'permanent'">
            预计到期时间：{{ exportExpectedExpireDate }}
          </div>
          <div class="duration-hint" v-if="exportForm.duration === 'permanent'">
            永久授权，无到期时间
          </div>
        </el-form-item>
        <el-form-item label="客户名称" prop="customer">
          <el-input v-model="exportForm.customer" placeholder="请输入客户真实名称" maxlength="100" show-word-limit />
        </el-form-item>
      </el-form>
      <!-- 生成结果展示 -->
      <div v-else>
        <el-form label-width="100px">
          <el-form-item label="目标租户">
            <el-tag v-if="selectedTenantId" type="primary">{{ selectedTenantName }}</el-tag>
            <el-tag v-else type="info">系统级 License</el-tag>
          </el-form-item>
          <el-form-item label="License JSON">
            <el-input
              type="textarea"
              :rows="14"
              :value="exportLicenseJson"
              readonly
              ref="exportLicenseInput"
            />
          </el-form-item>
        </el-form>
      </div>
      <div slot="footer" class="dialog-footer">
        <template v-if="!exportLicenseJson">
          <el-button @click="exportLicenseDialogVisible = false">取 消</el-button>
          <el-button type="primary" :loading="exportLicenseLoading" @click="handleGenerateLicense">导 出</el-button>
        </template>
        <template v-else>
          <el-button @click="resetExportForm">重新配置</el-button>
          <el-button type="primary" icon="el-icon-document-copy" @click="copyLicenseJson">再次复制</el-button>
        </template>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getLicenseInfo, replaceLicense, generateLicense } from '@/api/system/license'
import { getTenantLicense, getTenantSelector } from '@/api/system/tenant'

// 全量功能清单（与后端 EditionFeatureConfig 保持同步）
// edition: 该功能最低要求的版本等级（MICRO=小微版专属, ADVANCED=高级版起含, PROFESSIONAL=仅专业版）
const ALL_FEATURES = [
  // === 基础版 · 核心编审闭环 ===
  { code: 'BASIC_SINGLE_APPROVAL', name: '单级审批', module: '费用模块', edition: 'BASIC' },
  { code: 'BASIC_FIXED_RISK_RULES', name: '固定5条风控规则', module: '费用模块', edition: 'BASIC' },
  { code: 'BASIC_RESULT_NOTIFY', name: '结果通知', module: '费用模块', edition: 'BASIC' },
  { code: 'BASIC_SALARY_ACCRUAL', name: '薪资计提（每月1号）', module: '费用模块', edition: 'BASIC' },
  { code: 'BASIC_ASSET_DEPRECIATION', name: '资产折旧（每月1号）', module: '费用模块', edition: 'BASIC' },
  { code: 'BASIC_LOAN_INTEREST', name: '利息计提（每月1号）', module: '费用模块', edition: 'BASIC' },
  // === 小微版专属（v1.29 T3.18 新增，micro_lite/micro_pro 两档均可用） ===
  { code: 'MICRO_PROMOTION_PUBLISH', name: '推广发布(活动+海报)', module: '小微模块', edition: 'MICRO' },
  { code: 'MICRO_SHOP_MANAGE', name: '店铺管理(营业时间/收款方式)', module: '小微模块', edition: 'MICRO' },
  { code: 'MICRO_ACTIVITY_BROWSE', name: '活动浏览页(lite 档唯一菜单)', module: '小微模块', edition: 'MICRO' },
  { code: 'MICRO_BOOKING_ORDER', name: '预约订单(顾客扫码预约·仅 micro_pro)', module: '小微模块', edition: 'MICRO' },
  { code: 'MICRO_STAFF_SETTLE', name: '人员结算(只记账不打款·仅 micro_pro)', module: '小微模块', edition: 'MICRO' },
  { code: 'MICRO_DAILY_REVENUE', name: '收支记账+每日营收快照(仅 micro_pro)', module: '小微模块', edition: 'MICRO' },
  { code: 'MICRO_REPORT_CENTER', name: '报表中心(周报/月报/利润·仅 micro_pro)', module: '小微模块', edition: 'MICRO' },
  { code: 'MICRO_SERVICE_PRICING', name: '服务定价(五种计价+改价留痕·仅 micro_pro)', module: '小微模块', edition: 'MICRO' },
  { code: 'MICRO_NO_SHOW_BLACKLIST', name: '防爽约/黑名单(仅 micro_pro)', module: '小微模块', edition: 'MICRO' },
  { code: 'MICRO_CUSTOMER_CREDIT', name: '顾客管理(信誉/爽约/黑名单·仅 micro_pro)', module: '小微模块', edition: 'MICRO' },
  { code: 'FEAT_PROMOTION_DISTRIBUTION', name: '推广分销与激励(推广码/归因/拉新奖励/首单)', module: '小微模块', edition: 'MICRO' },
  { code: 'FEAT_RATING', name: '通用评价与点赞(服务人员评价·仅 micro_pro)', module: '小微模块', edition: 'MICRO' },
  // === 费用模块 · 高级版 ===
  { code: 'FEE_MULTI_CURRENCY', name: '多币种报销', module: '费用模块', edition: 'ADVANCED' },
  { code: 'FEE_COMPLIANCE_CUSTOM', name: '合规级别自定义配置', module: '费用模块', edition: 'ADVANCED' },
  { code: 'FEE_INDUSTRY_SINGLE', name: '单行业配置', module: '费用模块', edition: 'ADVANCED' },
  { code: 'FEE_TRACK_CONTROL', name: '三轨预算管控', module: '费用模块', edition: 'ADVANCED' },
  { code: 'FEE_PROFIT_CUSTOM_TIP', name: '盈利提示自定义', module: '费用模块', edition: 'ADVANCED' },
  { code: 'FEE_SUBSIDY_CONFIG', name: '补助标准配置', module: '费用模块', edition: 'ADVANCED' },
  { code: 'FEE_FUND_POOL', name: '资金池（高级版部门级/专业版三级）', module: '费用模块', edition: 'ADVANCED' },
  { code: 'FEE_AI_CUSTOM_PROMPT', name: 'AI话术自定义', module: '费用模块', edition: 'ADVANCED' },
  { code: 'FEE_AI_CONFIDENCE_REVIEW', name: 'AI置信度转人工', module: '费用模块', edition: 'ADVANCED' },
  // === 费用模块 · 专业版独有 ===
  { code: 'FEE_INDUSTRY_PACK_IO', name: '行业配置包导入导出', module: '费用模块', edition: 'PROFESSIONAL' },
  { code: 'FEE_ACCRUAL_ENGINE', name: '周期计提引擎（资产折旧+合同摊销）', module: '费用模块', edition: 'PROFESSIONAL' },
  { code: 'FEE_LOAN_LEDGER', name: '借款台账+利息计提', module: '费用模块', edition: 'PROFESSIONAL' },
  { code: 'FEE_AI_PRIVATE_DEPLOY', name: 'AI私有化部署', module: '费用模块', edition: 'PROFESSIONAL' },
  { code: 'FEE_DESENSITIZE_CUSTOM', name: '数据脱敏自定义', module: '费用模块', edition: 'PROFESSIONAL' },
  { code: 'FEE_CROSS_DEPT_ALLOCATE', name: '跨部门分摊', module: '费用模块', edition: 'PROFESSIONAL' },
  { code: 'FEE_AUDIT_FULL_EXPORT', name: '全节点审计导出', module: '费用模块', edition: 'PROFESSIONAL' },
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
  // === 预算模块 · 专业版独有 ===
  { code: 'BUDGET_THREE_LEVEL_APPROVAL', name: '固定三级审批（部门→分公司→总公司）', module: '预算模块', edition: 'PROFESSIONAL' },
  { code: 'BUDGET_SEND_BACK', name: '打回至部门', module: '预算模块', edition: 'PROFESSIONAL' },
  { code: 'BUDGET_TIMEOUT_ALERT', name: '超时告警', module: '预算模块', edition: 'PROFESSIONAL' },
  { code: 'BUDGET_RISK_FORMULA', name: '公式/建议性规则', module: '预算模块', edition: 'PROFESSIONAL' },
  { code: 'BUDGET_RISK_AI', name: 'AI辅助风控', module: '预算模块', edition: 'PROFESSIONAL' }
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
      // 客户名称对话框
      customerDialogVisible: false,
      customerName: '',
      customerLoading: false,
      // 导出 License 对话框（T15.33+ 改造：原「机器指纹」改为「签发 License JSON」）
      exportLicenseDialogVisible: false,
      exportLicenseJson: '',
      exportLicenseLoading: false,
      exportForm: {
        tenantType: 'enterprise',
        versionType: 'basic',
        edition: 'BASIC',
        licenseType: 'FORMAL',
        duration: '365',
        customer: ''
      },
      exportRules: {
        tenantType: [{ required: true, message: '请选择目标产品线', trigger: 'change' }],
        versionType: [{ required: true, message: '请选择版本档位', trigger: 'change' }],
        licenseType: [{ required: true, message: '请选择授权类型', trigger: 'change' }],
        duration: [{ required: true, message: '请选择授权期限', trigger: 'change' }],
        customer: [{ required: true, message: '请输入客户名称', trigger: 'blur' }]
      },
      // 租户选择器（T3.17）
      selectedTenantId: null,
      selectedTenantName: '',
      tenantGroups: {},
      tenantLoading: false,
      tenantLicenseInfo: {}
    }
  },
  computed: {
    editionLabel() {
      const map = { 'MICRO': '小微版', 'BASIC': '基础版', 'ADVANCED': '高级版', 'PROFESSIONAL': '专业版', 'ENTERPRISE': '专业版' }
      return map[this.licenseInfo.edition] || '未知'
    },
    editionTagType() {
      const map = { 'MICRO': 'success', 'BASIC': 'info', 'ADVANCED': 'warning', 'PROFESSIONAL': 'danger', 'ENTERPRISE': 'danger' }
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
    /** 高级版功能数（含专业版也有的） */
    advancedCount() {
      return this.allFeatures.filter(f => f.edition === 'ADVANCED').length
    },
    /** 专业版独有功能数 */
    professionalOnlyCount() {
      return this.allFeatures.filter(f => f.edition === 'PROFESSIONAL' || f.edition === 'ENTERPRISE').length
    },
    /** 小微版专属功能数 */
    microCount() {
      return this.allFeatures.filter(f => f.edition === 'MICRO').length
    },
    /** 基础版功能数（核心编审闭环 + 定时计提任务） */
    basicCount() {
      return this.allFeatures.filter(f => f.edition === 'BASIC').length
    },
    /** 按版本筛选后的功能列表 */
    filteredFeatures() {
      if (this.editionFilter === 'ALL') return this.allFeatures
      if (this.editionFilter === 'PROFESSIONAL') {
        return this.allFeatures.filter(f => f.edition === 'PROFESSIONAL' || f.edition === 'ENTERPRISE')
      }
      return this.allFeatures.filter(f => f.edition === this.editionFilter)
    },
    /** 当前版本功能摘要 */
    editionFeatureSummary() {
      const edition = this.licenseInfo.edition
      if (edition === 'PROFESSIONAL' || edition === 'ENTERPRISE') return '已开放全部 ' + this.allFeatures.length + ' 项功能'
      if (edition === 'ADVANCED') return '已开放 ' + this.advancedCount + ' 项高级功能，升级专业版可再解锁 ' + this.professionalOnlyCount + ' 项'
      if (edition === 'MICRO') return '小微版专属功能集（' + this.microCount + ' 项），升级基础版可解锁 ' + this.advancedCount + ' 项高级功能'
      if (edition === 'BASIC') return '基础版核心功能集（' + this.basicCount + ' 项），升级高级版可解锁 ' + this.advancedCount + ' 项高级功能'
      return '仅含核心编审闭环，升级高级版可解锁 ' + this.advancedCount + ' 项功能'
    },
    /** 导出 License 对话框的预计到期时间 */
    exportExpectedExpireDate() {
      const days = parseInt(this.exportForm.duration)
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
    // 不自动加载系统级 License，需先选择租户
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
    /** 表格行版本文案（兼容 ENTERPRISE 遗留值） */
    editionTextOfRow(edition) {
      const map = { 'MICRO': '小微版', 'ADVANCED': '高级版', 'PROFESSIONAL': '专业版', 'ENTERPRISE': '专业版', 'BASIC': '基础版' }
      return map[edition] || edition
    },
    /** 表格行版本标签颜色 */
    editionTagOfRow(edition) {
      const map = { 'MICRO': 'success', 'ADVANCED': 'warning', 'PROFESSIONAL': 'danger', 'ENTERPRISE': 'danger', 'BASIC': 'info' }
      return map[edition] || 'info'
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
    /** 打开「导出 License」对话框（T15.33+ 改造：原「导出机器指纹」） */
    handleExportLicense() {
      // 根据当前选中租户的 License 信息预填表单
      const curEdition = this.licenseInfo.edition || 'BASIC'
      const isMicro = curEdition === 'MICRO'
      const defaultVersionType = isMicro ? 'micro_lite'
        : (curEdition === 'ADVANCED' ? 'advanced'
        : (curEdition === 'PROFESSIONAL' || curEdition === 'ENTERPRISE' ? 'pro' : 'basic'))
      this.exportForm = {
        tenantType: isMicro ? 'micro' : 'enterprise',
        versionType: defaultVersionType,
        edition: curEdition,
        licenseType: this.licenseInfo.licenseType || 'FORMAL',
        duration: this.licenseInfo.licenseType === 'TRIAL'
          ? '7'
          : (this.licenseInfo.expiresAt === '永久' ? 'permanent' : '365'),
        customer: this.licenseInfo.customer || this.selectedTenantName || ''
      }
      this.exportLicenseJson = ''
      this.exportLicenseDialogVisible = true
    },
    /** 导出表单：产品线切换时重置版本档位 */
    handleExportTenantTypeChange(val) {
      this.exportForm.versionType = val === 'micro' ? 'micro_lite' : 'basic'
      this.syncExportEditionFromVersionType()
    },
    /** 导出表单：版本档位切换时同步 edition */
    handleExportVersionTypeChange() {
      this.syncExportEditionFromVersionType()
    },
    /** 导出表单：version_type → edition 推导（与后端 deriveEditionFromVersionType 一致） */
    syncExportEditionFromVersionType() {
      const vt = this.exportForm.versionType
      const map = {
        'micro_lite': 'MICRO',
        'micro_pro': 'MICRO',
        'basic': 'BASIC',
        'advanced': 'ADVANCED',
        'pro': 'PROFESSIONAL'
      }
      this.exportForm.edition = map[vt] || 'BASIC'
    },
    /** 导出表单：授权类型切换时重置期限 */
    handleExportLicenseTypeChange(val) {
      this.exportForm.duration = val === 'TRIAL' ? '7' : '365'
    },
    /** 调用后端签发 License JSON，成功后自动复制到剪贴板 */
    handleGenerateLicense() {
      this.$refs.exportForm.validate(valid => {
        if (!valid) return
        this.exportLicenseLoading = true
        const payload = {
          tenantId: this.selectedTenantId || null,
          edition: this.exportForm.edition,
          tenantType: this.exportForm.tenantType,
          versionType: this.exportForm.versionType,
          licenseType: this.exportForm.licenseType,
          customer: this.exportForm.customer,
          durationDays: this.exportForm.duration === 'permanent' ? null : parseInt(this.exportForm.duration)
        }
        generateLicense(payload).then(response => {
          this.exportLicenseJson = response.data || ''
          if (!this.exportLicenseJson) {
            this.$modal.msgError('签发失败：后端返回空内容')
            return
          }
          // 自动复制到剪贴板
          this.copyLicenseJson()
        }).finally(() => {
          this.exportLicenseLoading = false
        })
      })
    },
    /** 重新配置：清空已生成的 JSON，回到表单 */
    resetExportForm() {
      this.exportLicenseJson = ''
    },
    /** 复制 License JSON 到剪贴板 */
    copyLicenseJson() {
      const text = this.exportLicenseJson
      if (!text) {
        this.$modal.msgWarning('License 内容为空')
        return
      }
      // 优先使用 Clipboard API
      if (navigator.clipboard && navigator.clipboard.writeText) {
        navigator.clipboard.writeText(text).then(() => {
          this.$modal.msgSuccess('复制成功，请交给租户管理员在「版本升级」页面粘贴导入')
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
      const map = { 'MICRO': '小微版', 'BASIC': '基础版', 'ADVANCED': '高级版', 'PROFESSIONAL': '专业版', 'ENTERPRISE': '专业版' }
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
