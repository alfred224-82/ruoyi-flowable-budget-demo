<template>
  <div class="app-container">
    <el-alert
      title="版本档位管理（T15.29）：五档能力总览 + 新建租户级联数据源"
      type="info"
      description="version_type ∈ {micro_lite, micro_pro, basic, advanced, pro}。小微版两档共用同一套标准角色集与菜单表，lite→pro 升级 = 换角色模板配方（菜单扩围），角色 code / 用户 / 业务数据零迁移。"
      show-icon
      :closable="false"
      class="mb8"
    />
    <el-alert
      title="档位配方（菜单范围 / 区块天花板）的写操作请前往「角色模板管理」"
      type="warning"
      description="后端 /system/edition 仅提供只读聚合接口（features / version-options / industry-options / default-role-preview），未提供 list/add/edit/bindMenu 端点；本页为只读档位对照台，配方维护统一走 role_template（perms system:roleTemplate:*）。"
      show-icon
      :closable="false"
      class="mb8"
    />

    <el-row :gutter="16">
      <!-- 左：档位总览 -->
      <el-col :span="15">
        <el-card shadow="never">
          <div slot="header">
            <span style="font-weight:600;">版本档位</span>
            <el-radio-group v-model="tenantType" size="mini" style="float:right;" @change="loadVersionOptions">
              <el-radio-button label="micro">小微版</el-radio-button>
              <el-radio-button label="enterprise">企业版</el-radio-button>
            </el-radio-group>
          </div>
          <div v-loading="versionLoading">
            <el-empty v-if="versionOptions.length === 0" description="该产品线暂无可选档位" />
            <el-card
              v-for="opt in versionOptions"
              :key="opt.value"
              shadow="hover"
              class="edition-card"
              :body-style="{ padding: '14px' }"
            >
              <div class="edition-head">
                <span class="edition-label">{{ opt.label }}</span>
                <el-tag size="mini" type="info">{{ opt.value }}</el-tag>
              </div>
              <el-table :data="previewMap[opt.value] || []" size="mini" :show-header="true" max-height="180">
                <el-table-column label="标准角色" prop="roleCode" align="left" min-width="110" />
                <el-table-column label="菜单数" prop="menuCount" align="center" width="80" />
                <el-table-column label="区块数" prop="blockCount" align="center" width="80" />
                <el-table-column label="操作" align="center" width="90">
                  <template slot-scope="scope">
                    <el-button type="text" size="mini" @click="openRecipe(opt.value, scope.row.roleCode)">配方</el-button>
                  </template>
                </el-table-column>
              </el-table>
              <div class="edition-foot">
                <span style="color:#909399;font-size:12px;">
                  {{ (previewMap[opt.value] || []).length === 0 ? '尚未配置角色模板配方' : '共 ' + (previewMap[opt.value] || []).length + ' 个角色配方' }}
                </span>
                <el-button type="text" size="mini" icon="el-icon-s-operation" @click="openTemplates(opt.value)">维护角色模板</el-button>
              </div>
            </el-card>
          </div>
        </el-card>
      </el-col>

      <!-- 右：当前身份三维能力 + 行业模板 -->
      <el-col :span="9">
        <el-card shadow="never" class="mb16">
          <div slot="header"><span style="font-weight:600;">当前登录身份 · 三维能力</span></div>
          <div v-loading="featureLoading">
            <el-descriptions :column="1" border size="small">
              <el-descriptions-item label="版本编码">{{ features.edition || '—' }}</el-descriptions-item>
              <el-descriptions-item label="业态包">{{ features.bizCode || '—' }}</el-descriptions-item>
              <el-descriptions-item label="访问通道">{{ features.accessChannel || '—' }}</el-descriptions-item>
              <el-descriptions-item label="有效功能数">{{ featureList.length }}</el-descriptions-item>
              <el-descriptions-item label="文案覆盖键数">{{ copyKeys.length }}</el-descriptions-item>
            </el-descriptions>
            <div class="tag-box">
              <el-tag v-for="f in featureList" :key="f" size="mini" class="feature-tag">{{ f }}</el-tag>
              <span v-if="!featureList.length" style="color:#909399;font-size:12px;">无下发功能标识（超管走全量专业版豁免）</span>
            </div>
            <div style="color:#909399;font-size:12px;margin-top:8px;line-height:1.6;">
              features = feature_flag(edition) ∩ biz_bundle(biz_code) ∩ License ∪/− sys_tenant.features；前端据此隐藏未解锁入口，后端 @RequireFeature 为最终防线。
            </div>
          </div>
        </el-card>

        <el-card shadow="never">
          <div slot="header"><span style="font-weight:600;">小微版行业模板（级联第三级）</span></div>
          <div v-loading="industryLoading">
            <el-empty v-if="industryOptions.length === 0" description="无启用行业模板，请先到「行业模板管理」维护" :image-size="60" />
            <div v-for="item in industryOptions" :key="item.value" class="industry-item">
              <span class="industry-name">{{ item.label }}</span>
              <span class="industry-code">{{ item.value }}</span>
              <el-tag v-if="item.icon" size="mini" type="info">{{ item.icon }}</el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 角色配方详情 -->
    <el-dialog :title="'角色配方 - ' + recipeTitle" :visible.sync="recipeOpen" width="720px" append-to-body>
      <el-descriptions :column="1" border size="small" class="mb8">
        <el-descriptions-item label="版本档位">{{ recipe.versionType }}</el-descriptions-item>
        <el-descriptions-item label="标准角色">{{ recipe.roleCode }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="recipe.status === '0' ? 'success' : 'info'" size="mini">{{ recipe.status === '0' ? '启用' : '停用' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注">{{ recipe.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="120px" size="small">
        <el-form-item :label="'菜单范围（' + menuKeys.length + '）'">
          <div class="tag-box">
            <el-tag v-for="k in menuKeys" :key="k" size="mini" class="feature-tag">{{ k }}</el-tag>
            <span v-if="!menuKeys.length" style="color:#909399;font-size:12px;">未配置</span>
          </div>
        </el-form-item>
        <el-form-item :label="'区块天花板（' + blockKeys.length + '）'">
          <div class="tag-box">
            <el-tag v-for="k in blockKeys" :key="k" size="mini" type="success" class="feature-tag">{{ k }}</el-tag>
            <span v-if="!blockKeys.length" style="color:#909399;font-size:12px;">未配置（表示不设天花板限制）</span>
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="gotoRoleTemplate">前往角色模板管理维护</el-button>
        <el-button @click="recipeOpen = false">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- 该档位全部角色模板 -->
    <el-dialog :title="'角色模板配方 - ' + templateVersion" :visible.sync="templatesOpen" width="860px" append-to-body>
      <el-table v-loading="templatesLoading" :data="templateList" max-height="420">
        <el-table-column label="标准角色" prop="roleCode" align="left" min-width="120" />
        <el-table-column label="菜单数" align="center" width="90">
          <template slot-scope="scope"><span>{{ parseKeys(scope.row.menuKeys).length }}</span></template>
        </el-table-column>
        <el-table-column label="区块数" align="center" width="90">
          <template slot-scope="scope"><span>{{ parseKeys(scope.row.blockKeys).length }}</span></template>
        </el-table-column>
        <el-table-column label="状态" align="center" width="90">
          <template slot-scope="scope">
            <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="mini">{{ scope.row.status === '0' ? '启用' : '停用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" prop="remark" align="left" min-width="160" show-overflow-tooltip />
        <el-table-column label="操作" align="center" width="90">
          <template slot-scope="scope">
            <el-button type="text" size="mini" @click="openRecipe(templateVersion, scope.row.roleCode)">查看配方</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="gotoRoleTemplate">前往角色模板管理维护</el-button>
        <el-button @click="templatesOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getEditionFeatures, listVersionOptions, listIndustryOptions, getDefaultRolePreview } from '@/api/system/edition'
import { getRecipe, listTemplateByVersion } from '@/api/micro/roleTemplate'

export default {
  name: 'SystemEdition',
  data() {
    return {
      tenantType: 'micro',
      versionLoading: false,
      versionOptions: [],
      // versionType → default-role-preview 结果
      previewMap: {},
      featureLoading: false,
      features: {},
      industryLoading: false,
      industryOptions: [],
      // 角色配方详情
      recipeOpen: false,
      recipeTitle: '',
      recipe: {},
      menuKeys: [],
      blockKeys: [],
      // 档位角色模板清单
      templatesOpen: false,
      templatesLoading: false,
      templateVersion: '',
      templateList: []
    }
  },
  computed: {
    featureList() {
      const f = this.features.features
      return Array.isArray(f) ? f : []
    },
    copyKeys() {
      const c = this.features.copy
      return c && typeof c === 'object' ? Object.keys(c) : []
    }
  },
  created() {
    this.loadVersionOptions()
    this.loadFeatures()
    this.loadIndustryOptions()
  },
  methods: {
    loadVersionOptions() {
      this.versionLoading = true
      this.previewMap = {}
      listVersionOptions(this.tenantType).then(response => {
        this.versionOptions = response.data || []
        this.versionLoading = false
        this.versionOptions.forEach(opt => this.loadPreview(opt.value))
      }).catch(() => { this.versionLoading = false })
    },
    loadPreview(versionType) {
      getDefaultRolePreview(versionType).then(response => {
        this.$set(this.previewMap, versionType, response.data || [])
      }).catch(() => { this.$set(this.previewMap, versionType, []) })
    },
    loadFeatures() {
      this.featureLoading = true
      getEditionFeatures().then(response => {
        this.features = response.data || {}
        this.featureLoading = false
      }).catch(() => { this.featureLoading = false })
    },
    loadIndustryOptions() {
      this.industryLoading = true
      listIndustryOptions('micro').then(response => {
        this.industryOptions = response.data || []
        this.industryLoading = false
      }).catch(() => { this.industryLoading = false })
    },
    /** menu_keys / block_keys 为 JSON 数组字符串，解析失败按空数组处理 */
    parseKeys(json) {
      if (!json) return []
      try {
        const arr = JSON.parse(json)
        return Array.isArray(arr) ? arr : []
      } catch (err) {
        return []
      }
    },
    openRecipe(versionType, roleCode) {
      this.recipeTitle = versionType + ' / ' + roleCode
      this.recipe = { versionType: versionType, roleCode: roleCode, status: '0', remark: null, menuKeys: null, blockKeys: null }
      this.menuKeys = []
      this.blockKeys = []
      this.recipeOpen = true
      getRecipe(versionType, roleCode).then(response => {
        const data = response.data
        if (data) {
          this.recipe = data
          this.menuKeys = this.parseKeys(data.menuKeys)
          this.blockKeys = this.parseKeys(data.blockKeys)
        }
      }).catch(() => {})
    },
    openTemplates(versionType) {
      this.templateVersion = versionType
      this.templatesOpen = true
      this.templatesLoading = true
      listTemplateByVersion(versionType).then(response => {
        this.templateList = response.data || []
        this.templatesLoading = false
      }).catch(() => { this.templatesLoading = false })
    },
    gotoRoleTemplate() {
      this.recipeOpen = false
      this.templatesOpen = false
      // ★角色模板管理菜单（menu_id=7005）挂资金平台管理菜单(1300)下，路由 path = /fundPlatform/roleTemplate
      this.$router.push({ path: '/fundPlatform/roleTemplate', query: { versionType: this.recipe.versionType || this.templateVersion }})
    }
  }
}
</script>

<style scoped>
.mb16 {
  margin-bottom: 16px;
}
.edition-card {
  margin-bottom: 12px;
}
.edition-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.edition-label {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}
.edition-foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}
.tag-box {
  margin-top: 8px;
  line-height: 2;
}
.feature-tag {
  margin: 0 6px 6px 0;
}
.industry-item {
  display: flex;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px dashed #ebeef5;
}
.industry-name {
  font-size: 13px;
  color: #303133;
  margin-right: 8px;
}
.industry-code {
  font-size: 12px;
  color: #909399;
  margin-right: 8px;
  font-family: Consolas, Monaco, monospace;
}
</style>
