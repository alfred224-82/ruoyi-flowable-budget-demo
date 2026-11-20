<template>
  <div class="app-container">
    <el-card class="box-card" shadow="hover">
      <div slot="header" class="clearfix">
        <span style="font-size:16px;font-weight:600;">当前版本信息</span>
      </div>
      <el-descriptions :column="2" border v-loading="loading">
        <el-descriptions-item label="租户名称">{{ info.companyName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="当前版本">
          <el-tag :type="info.versionType === 'micro_pro' ? 'success' : 'info'" size="small">
            {{ info.versionLabel || '未分配' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="到期日期">{{ info.expireDate || '永久' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <dict-tag :options="dict.type.sys_tenant_status" :value="info.status" />
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 升级/续费/更新入口：始终显示，保证租户管理员任何状态下都能导入 License -->
    <el-card class="box-card mt16" shadow="hover">
      <div slot="header" class="clearfix">
        <span style="font-size:16px;font-weight:600;">{{ uploadCardTitle }}</span>
      </div>
      <el-alert
        v-if="!isPro"
        title="升级至小微版·专业版（micro_pro）"
        type="warning"
        description="升级后可使用全部经营管理功能：预约管理、人员结算、推广分销、固定支出、报表导出等。请将平台管理员签发的 License 密文（LIC1- 开头单行串）粘贴到下方文本框完成升级；若 License 档位与当前档位相同（micro_lite），导入按续费处理，仅更新授权到期日。"
        show-icon
        :closable="false"
        class="mb16"
      />
      <el-alert
        v-else-if="isExpiredPro"
        title="当前授权已到期，请导入新 License 续费"
        type="warning"
        description="您的小微版·专业版授权已到期，系统功能受限。请将平台管理员签发的新 License 密文粘贴到下方文本框完成续费，恢复全部功能。"
        show-icon
        :closable="false"
        class="mb16"
      />
      <el-alert
        v-else
        title="已是专业版"
        type="success"
        description="您的租户已升级至小微版·专业版，享有全部经营管理功能。如需续费或更新授权，请将平台管理员签发的新 License 密文粘贴到下方文本框导入，同档导入仅更新授权到期日。"
        show-icon
        :closable="false"
        class="mb16"
      />
      <el-form ref="licenseForm" :model="licenseForm" :rules="licenseRules" label-width="120px">
        <el-form-item label="License 密文" prop="licenseKey">
          <el-input
            type="textarea"
            :rows="10"
            v-model="licenseForm.licenseKey"
            placeholder='粘贴平台管理员签发的 License 密文（LIC1- 开头单行串，兼容存量明文 JSON）'
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="upgrading" @click="handleUpgrade">{{ uploadButtonText }}</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { getUpgradeInfo, importLicense } from '@/api/micro/platformUpgrade'
import { listMicroPackage } from '@/api/micro/package'

export default {
  name: 'PlatformUpgrade',
  dicts: ['sys_tenant_status'],
  data() {
    return {
      loading: true,
      upgrading: false,
      info: {},
      licenseForm: { licenseKey: '' },
      licenseRules: {
        licenseKey: [{ required: true, message: 'License 密文不能为空', trigger: 'blur' }]
      },
      renewPlans: []
    }
  },
  created() {
    this.getInfo()
    this.loadRenewPlans()
  },
  computed: {
    /** 当前为小微版·专业版（含正常与过期状态） */
    isPro() {
      return this.info.versionType === 'micro_pro'
    },
    /** 当前为 micro_pro 但已过期/宽限期，显示续费上传卡片 */
    isExpiredPro() {
      return this.isPro && (this.info.status === 'EXPIRED' || this.info.status === 'GRACE')
    },
    /** 上传卡片标题：按当前档位与状态区分升级/续费/更新 */
    uploadCardTitle() {
      if (this.isExpiredPro) return 'License 续费'
      if (this.isPro) return 'License 续费/更新'
      return '版本升级'
    },
    /** 提交按钮文案：与卡片标题语义保持一致 */
    uploadButtonText() {
      if (this.isExpiredPro) return '立即续费'
      if (this.isPro) return '导入更新'
      return '立即升级'
    }
  },
  methods: {
    getInfo() {
      this.loading = true
      getUpgradeInfo().then(response => {
        this.info = response.data || {}
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleUpgrade() {
      this.$refs['licenseForm'].validate(valid => {
        if (!valid) return
        // 预解析 License：明文 JSON 的格式与产品线问题前端直接拦截，不发起无效请求；
        // 密文（非 { 开头，含复制丢 LIC1- 前缀的情况）跳过预解析（前端无密钥），由后端解密后强制 RSA 验签兼底
        const inputKey = (this.licenseForm.licenseKey || '').trim()
        const isCipherToken = !inputKey.startsWith('{')
        let targetVersion = ''
        let expiresAt = '永久'
        if (!isCipherToken) {
          let license = null
          try {
            license = JSON.parse(inputKey)
          } catch (e) {
            this.$modal.msgError('License 格式错误，请粘贴 Admin 侧导出的完整密文（LIC1- 开头）或 JSON 内容')
            return
          }
          if (!license || typeof license !== 'object' || Array.isArray(license)) {
            this.$modal.msgError('License 格式错误，请粘贴 Admin 侧导出的完整 JSON 对象')
            return
          }
          targetVersion = license.versionType || ''
          expiresAt = license.expiresAt || '永久'
          if (targetVersion && targetVersion !== 'micro_lite' && targetVersion !== 'micro_pro') {
            this.$modal.msgError('该 License 档位[' + targetVersion + ']属于企业版产品线，小微版租户无法导入，请联系平台管理员')
            return
          }
        }
        // 确认文案按 License 实际目标档位 + 当前状态生成：micro_pro=升级/续费；micro_lite=同档续费或降档；密文/未声明=后端解密后判定
        let confirmMsg
        if (isCipherToken) {
          confirmMsg = this.isPro
            ? '确认导入此 License 密文？后端将自动解密并验签，更新授权档位与到期日'
            : '确认导入此 License 密文？后端将自动解密并验签，完成版本升级/续费'
        } else if (targetVersion === 'micro_pro') {
          if (this.isPro) {
            confirmMsg = '确认使用此 License 续费小微版·专业版（micro_pro）吗？有效期至 ' + expiresAt
          } else {
            confirmMsg = '确认使用此 License 升级至小微版·专业版（micro_pro）吗？有效期至 ' + expiresAt
          }
        } else if (targetVersion === 'micro_lite') {
          if (this.isPro) {
            confirmMsg = '该 License 档位为小微版·免费版（低于当前专业版），导入后将降级并更新授权到期日（至 ' + expiresAt + '），确认导入吗？'
          } else {
            confirmMsg = '该 License 档位为小微版·免费版（与当前档位相同），导入仅更新授权到期日（至 ' + expiresAt + '），确认导入吗？'
          }
        } else {
          if (this.isPro) {
            confirmMsg = '该 License 未声明版本档位(versionType)，导入将更新授权版本与到期日（至 ' + expiresAt + '），确认导入吗？'
          } else {
            confirmMsg = '该 License 未声明版本档位(versionType)，导入仅更新授权版本与到期日（至 ' + expiresAt + '），确认导入吗？'
          }
        }
        this.$modal.confirm(confirmMsg)
          .then(() => {
            this.upgrading = true
            importLicense(this.licenseForm.licenseKey).then(response => {
              // 成功提示按后端返回的变更结论生成，避免文案与实际结果不符
              const data = response.data || {}
              if (data.changeType === 'UPGRADE') {
                this.$modal.msgSuccess('升级成功！当前档位：' + (data.versionLabel || '小微版·专业版'))
              } else if (data.changeType === 'DOWNGRADE') {
                this.$modal.msgSuccess('降档成功！当前档位：' + (data.versionLabel || this.info.versionLabel || '-') + '，有效期至 ' + (data.expiresAt || '永久'))
              } else if (data.changeType === 'RENEW') {
                this.$modal.msgSuccess('续费成功！有效期至 ' + (data.expiresAt || '永久'))
              } else {
                this.$modal.msgSuccess('License 导入成功，当前档位：' + (data.versionLabel || this.info.versionLabel || '-') + '，有效期至 ' + (data.expiresAt || '永久'))
              }
              this.licenseForm.licenseKey = ''
              this.getInfo()
              this.upgrading = false
            }).catch(() => { this.upgrading = false })
          })
          .catch(() => {})
      })
    },
    loadRenewPlans() {
      listMicroPackage('micro_pro').then(res => {
        const grouped = res.data || {}
        const proPlans = grouped.micro_pro || []
        this.renewPlans = proPlans.map(p => ({
          planCode: p.planCode,
          planName: p.planName,
          price: p.price != null ? p.price.toFixed(2) : '0.00',
          durationLabel: p.durationDays >= 365 ? '包年' : p.durationDays >= 30 ? '月付' : '试用'
        }))
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.mt16 { margin-top: 16px; }
.mb16 { margin-bottom: 16px; }
.plan-card { text-align: center; cursor: pointer; transition: all 0.3s; }
.plan-card:hover { transform: translateY(-4px); border-color: #409eff; }
</style>
