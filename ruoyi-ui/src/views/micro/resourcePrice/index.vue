<template>
  <div class="app-container">
    <el-alert title="资源价格设置（T21.7，★FEAT_SELF_BOOKING 功能开关）" type="info"
      :description="'设置' + staffRoleName + '的计价方式与价格，顾客在自助预约选择时可以看到。支持计时/套餐/计量/一口价 4 种模式，与房间定价体系统一。'"
      show-icon :closable="false" class="mb16" />

    <el-table v-loading="loading" :data="staffList" border stripe>
      <el-table-column :label="staffRoleName" align="center" min-width="140">
        <template slot-scope="scope">
          <div style="display:flex;align-items:center;justify-content:center;">
            <el-avatar v-if="scope.row.avatar" :size="32" :src="scope.row.avatar" style="margin-right:8px;" />
            <el-avatar v-else :size="32" icon="el-icon-user-solid" style="margin-right:8px;" />
            <span>{{ scope.row.nickName || '未设置昵称' }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="计价模式" align="center" width="120">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.pricingType" :type="pricingTypeTag(scope.row.pricingType)" size="mini">
            {{ pricingTypeLabel(scope.row.pricingType) }}
          </el-tag>
          <span v-else class="text-muted">未设置</span>
        </template>
      </el-table-column>
      <el-table-column label="价格" align="center" width="180">
        <template slot-scope="scope">
          <span v-if="scope.row.hourlyRate != null">
            <span style="color:#f56c6c;font-weight:600;">¥{{ scope.row.hourlyRate }}</span>
            <span class="text-muted" style="margin-left:4px;">/{{ priceUnit(scope.row.pricingType) }}</span>
          </span>
          <span v-else class="text-muted">未设置</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.enabled === 1 ? 'success' : 'info'" size="mini">
            {{ scope.row.enabled === 1 ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="120">
        <template slot-scope="scope">
          <el-button size="mini" type="primary" icon="el-icon-edit"
            v-hasPermi="['micro:resourcePrice:staff:set']"
            @click="handleEdit(scope.row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="!loading && staffList.length === 0" :description="'暂无' + staffRoleName + '数据'" />

    <!-- 编辑定价对话框 -->
    <el-dialog :title="'设置' + staffRoleName + '定价'" :visible.sync="dialogVisible" width="620px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-form-item :label="staffRoleName">
          <span>{{ editingStaff.nickName || '未设置昵称' }}</span>
        </el-form-item>

        <!-- ★ 计价模式（4种） -->
        <el-form-item label="计价模式" prop="pricingType">
          <el-select v-model="form.pricingType" placeholder="请选择计价模式" style="width:100%;" @change="onPricingTypeChange">
            <el-option label="计时 — 单价 × 时长" value="duration" />
            <el-option label="套餐 — 基础价 + 超时费" value="package" />
            <el-option label="计量 — 单价 × 数量" value="times" />
            <el-option label="一口价 — 固定值" value="fixed" />
          </el-select>
        </el-form-item>

        <!-- ① 计时 / 计量：价格 + 单位 -->
        <el-row v-if="form.pricingType === 'duration' || form.pricingType === 'times'">
          <el-col :span="12">
            <el-form-item :label="form.pricingType === 'duration' ? '时薪(元)' : '单价(元)'" prop="hourlyRate">
              <el-input-number v-model="form.hourlyRate" :min="0" :precision="2" :step="10" controls-position="right" style="width:100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="单位" prop="unit">
              <el-select v-model="form.unit" placeholder="请选择" style="width:100%;">
                <el-option v-for="u in unitOptions" :key="u" :label="u" :value="u" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- ② 套餐：基础价 + 含时长 + 超时费 -->
        <template v-if="form.pricingType === 'package'">
          <el-row>
            <el-col :span="12">
              <el-form-item label="基础价(元)" prop="hourlyRate">
                <el-input-number v-model="form.hourlyRate" :min="0" :precision="2" :step="10" controls-position="right" style="width:100%;" />
                <div class="field-hint">套餐固定基础价</div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="含时长(小时)" prop="extraConfig.includedDuration">
                <el-input-number v-model="extraConfig.includedDuration" :min="0" :precision="1" :step="0.5" controls-position="right" style="width:100%;" />
                <div class="field-hint">基础价包含的使用时长</div>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="12">
              <el-form-item label="超时费(元/时)" prop="extraConfig.overtimePrice">
                <el-input-number v-model="extraConfig.overtimePrice" :min="0" :precision="2" :step="5" controls-position="right" style="width:100%;" />
                <div class="field-hint">超出含时长后每小时加收</div>
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <!-- ③ 一口价：固定价 + 含时长 + 超时费 -->
        <template v-if="form.pricingType === 'fixed'">
          <el-row>
            <el-col :span="12">
              <el-form-item label="固定价格(元)" prop="hourlyRate">
                <el-input-number v-model="form.hourlyRate" :min="0" :precision="2" :step="10" controls-position="right" style="width:100%;" />
                <div class="field-hint">一口价固定金额</div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="含时长(小时)" prop="extraConfig.includedDuration">
                <el-input-number v-model="extraConfig.includedDuration" :min="0" :precision="1" :step="0.5" controls-position="right" style="width:100%;" />
                <div class="field-hint">一口价包含的使用时长上限</div>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="12">
              <el-form-item label="超时费(元/时)" prop="extraConfig.overtimePrice">
                <el-input-number v-model="extraConfig.overtimePrice" :min="0" :precision="2" :step="5" controls-position="right" style="width:100%;" />
                <div class="field-hint">超出含时长后每小时加收（留空=不限时）</div>
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <el-form-item label="状态">
          <el-radio-group v-model="form.enabled">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" placeholder="如：资深/特价等" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSave">保 存</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listStaffPricing, saveStaffPrice } from '@/api/micro/resourcePrice'
import { getRoleDisplayName } from '@/utils/roleDisplay'

export default {
  name: 'ResourcePrice',
  data() {
    return {
      loading: false,
      staffList: [],
      staffRoleName: '员工',
      dialogVisible: false,
      saving: false,
      editingStaff: {},
      form: {
        userId: null,
        hourlyRate: 0,
        pricingType: 'duration',
        enabled: 1,
        remark: '',
        unit: '小时'
      },
      extraConfig: {
        includedDuration: null,
        overtimePrice: null
      },
      rules: {
        pricingType: [{ required: true, message: '请选择计价模式', trigger: 'change' }],
        hourlyRate: [{ required: true, message: '价格不能为空', trigger: 'blur' }]
      }
    }
  },
  computed: {
    unitOptions() {
      if (this.form.pricingType === 'duration') return ['小时', '天']
      if (this.form.pricingType === 'times') return ['次', '公里', '吨', '份']
      return ['小时']
    }
  },
  created() {
    this.loadData()
    getRoleDisplayName('salesman', '员工').then(n => { this.staffRoleName = n })
  },
  methods: {
    loadData() {
      this.loading = true
      listStaffPricing().then(res => {
        this.staffList = res.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleEdit(row) {
      this.editingStaff = row
      this.form = {
        userId: row.userId,
        hourlyRate: row.hourlyRate || 0,
        pricingType: row.pricingType || 'duration',
        enabled: row.enabled != null ? row.enabled : 1,
        remark: row.remark || '',
        unit: '小时'
      }
      // 反序列化 extraConfig
      this.extraConfig = { includedDuration: null, overtimePrice: null }
      if (row.extraConfig) {
        try {
          const cfg = typeof row.extraConfig === 'string' ? JSON.parse(row.extraConfig) : row.extraConfig
          Object.assign(this.extraConfig, cfg)
        } catch (e) {
          console.warn('[ResourcePrice] extraConfig 解析失败:', e)
        }
      }
      this.dialogVisible = true
    },
    onPricingTypeChange() {
      this.form.unit = this.form.pricingType === 'times' ? '次' : '小时'
      this.extraConfig = { includedDuration: null, overtimePrice: null }
    },
    handleSave() {
      this.$refs.form.validate(valid => {
        if (!valid) return
        this.saving = true
        const data = {
          userId: this.form.userId,
          hourlyRate: this.form.hourlyRate,
          pricingType: this.form.pricingType,
          enabled: this.form.enabled,
          remark: this.form.remark
        }
        // 套餐/一口价：序列化 extraConfig
        if (this.form.pricingType === 'package' || this.form.pricingType === 'fixed') {
          const cfg = {}
          if (this.extraConfig.includedDuration != null) cfg.includedDuration = this.extraConfig.includedDuration
          if (this.extraConfig.overtimePrice != null) cfg.overtimePrice = this.extraConfig.overtimePrice
          data.extraConfig = Object.keys(cfg).length > 0 ? JSON.stringify(cfg) : null
        }
        saveStaffPrice(data).then(() => {
          this.$modal.msgSuccess('保存成功')
          this.dialogVisible = false
          this.loadData()
        }).finally(() => { this.saving = false })
      })
    },
    /** 计价模式标签颜色 */
    pricingTypeTag(type) {
      const map = { duration: '', package: 'success', times: 'warning', fixed: 'danger' }
      return map[type] || ''
    },
    /** 计价模式显示名 */
    pricingTypeLabel(type) {
      const map = { duration: '计时', package: '套餐', times: '计量', fixed: '一口价' }
      return map[type] || type
    },
    /** 价格单位文本 */
    priceUnit(type) {
      const map = { duration: '小时', package: '套餐', times: '次', fixed: '口' }
      return map[type] || ''
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.text-muted { color: #c0c4cc; font-size: 12px; }
.field-hint { font-size: 12px; color: #909399; margin-top: 4px; }
</style>
