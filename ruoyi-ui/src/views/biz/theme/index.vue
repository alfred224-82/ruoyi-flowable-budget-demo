<template>
  <div class="app-container">
    <el-alert title="主题装修三层融合：底层 Design Token（四套预设色板）+ 中层租户/活动素材（Logo 透明底 PNG 自动加衬底 80×80；海报 750×400 / 750×1000，≤200KB，上传后过内容安全审核）+ 上层内容占位。活动级优先，缺失回落租户级默认。" type="info" :closable="false" class="mb8" />

    <!-- 四套主题预设 -->
    <el-row :gutter="16" v-loading="loading">
      <el-col :span="6" v-for="p in presets" :key="p.presetKey" style="margin-bottom: 16px;">
        <el-card shadow="hover" :class="['preset-card', { active: form.presetKey === p.presetKey }]">
          <div slot="header" class="preset-header">
            <span class="preset-name">{{ p.name }}</span>
            <el-tag v-if="p.contrastRatio" size="mini" type="success" effect="plain">对比度 {{ p.contrastRatio }}</el-tag>
          </div>
          <div class="preset-body">
            <el-image v-if="p.previewUrl" :src="p.previewUrl" :preview-src-list="[p.previewUrl]" fit="cover" class="preset-preview" />
            <div v-else class="preset-preview placeholder">无预览图</div>
            <div class="swatches">
              <span v-for="(t, i) in colorTokens(p.tokens)" :key="i" class="swatch" :style="{ background: t.value }" :title="t.key + ': ' + t.value"></span>
            </div>
            <div class="preset-key">{{ p.presetKey }}</div>
          </div>
          <div class="preset-footer">
            <el-button size="mini" :type="form.presetKey === p.presetKey ? 'primary' : 'default'" @click="selectPreset(p)" v-hasPermi="['biz:theme:edit']">选用</el-button>
            <el-button size="mini" type="text" @click="viewTokens(p)">查看 Token</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 装修保存 -->
    <el-card shadow="never" class="mt16">
      <div slot="header"><b>素材装修</b>（当前预设：<span class="hl">{{ form.presetKey || '未选择' }}</span>）</div>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px" style="max-width: 720px;">
        <el-form-item label="应用范围" prop="activityId">
          <el-select v-model="form.activityId" placeholder="留空=租户级默认主题" filterable clearable style="width: 100%;">
            <el-option v-for="p in containers" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
          <div class="tip">选择活动则为该活动单独装修（活动级优先）；留空则作为全店默认。</div>
        </el-form-item>
        <el-form-item label="主题预设" prop="presetKey">
          <el-select v-model="form.presetKey" placeholder="请选择主题预设" style="width: 100%;">
            <el-option v-for="p in presets" :key="p.presetKey" :label="p.name + '（' + p.presetKey + '）'" :value="p.presetKey" />
          </el-select>
        </el-form-item>
        <el-form-item label="Logo（透明底PNG）" prop="logoUrl">
          <image-upload v-model="form.logoUrl" :limit="1" />
          <div class="tip">必须透明底 PNG，系统自动加衬底（圆形/圆角+毛玻璃）并输出 80×80px 成品。</div>
        </el-form-item>
        <el-form-item label="海报原图" prop="posterUrl">
          <image-upload v-model="form.posterUrl" :limit="1" />
          <div class="tip">输出 750×400（列表卡）与 750×1000（详情），尺寸容差 ±5%，文件 ≤200KB，上传后调微信 imgSecCheck 审核。</div>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="el-icon-check" @click="handleDecorate" v-hasPermi="['biz:theme:edit']">保存装修</el-button>
          <el-button icon="el-icon-view" @click="handlePreview">三层融合预览</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Token 明细 -->
    <el-dialog :title="'Design Token · ' + currentPresetName" :visible.sync="tokensOpen" width="520px" append-to-body>
      <el-table :data="tokenRows" size="small" max-height="420">
        <el-table-column label="色板" width="70" align="center">
          <template slot-scope="scope">
            <span v-if="isColor(scope.row.value)" class="swatch lg" :style="{ background: scope.row.value }"></span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="变量" prop="key" min-width="180" show-overflow-tooltip />
        <el-table-column label="值" prop="value" min-width="140" show-overflow-tooltip />
      </el-table>
    </el-dialog>

    <!-- 三层融合预览 -->
    <el-dialog title="三层融合预览" :visible.sync="previewOpen" width="640px" append-to-body>
      <div v-if="preview">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="生效预设">{{ preview.presetKey || '-' }}</el-descriptions-item>
          <el-descriptions-item label="素材来源层级">
            <el-tag :type="levelTag(preview.themeLevel)" size="small">{{ levelText(preview.themeLevel) }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <div class="preview-block">
          <div class="preview-label">底层 · Design Token</div>
          <div class="swatches">
            <span v-for="(v, k) in preview.tokens" :key="k" class="swatch lg" :style="{ background: isColor(v) ? v : '#eee' }" :title="k + ': ' + v"></span>
          </div>
        </div>
        <div class="preview-block" v-if="preview.theme">
          <div class="preview-label">中层 · 素材成品</div>
          <el-row :gutter="12">
            <el-col :span="8">
              <div class="mat-title">Logo 成品（80×80）</div>
              <el-image v-if="preview.theme.logoRenderedUrl" :src="preview.theme.logoRenderedUrl" :preview-src-list="[preview.theme.logoRenderedUrl]" fit="contain" class="mat-img logo" />
              <div v-else class="mat-img placeholder">未生成</div>
            </el-col>
            <el-col :span="16">
              <div class="mat-title">海报成品（750×400 / 750×1000）</div>
              <el-image v-if="preview.theme.renderedPosterUrl" :src="preview.theme.renderedPosterUrl" :preview-src-list="[preview.theme.renderedPosterUrl]" fit="cover" class="mat-img poster" />
              <div v-else class="mat-img placeholder">未生成</div>
            </el-col>
          </el-row>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listPresets, getTokens, previewTheme, decorate } from '@/api/biz/theme'
import { listProject } from '@/api/biz/project'

export default {
  name: 'BizTheme',
  data() {
    return {
      loading: true,
      presets: [],
      containers: [],
      form: { activityId: null, presetKey: null, logoUrl: null, posterUrl: null },
      rules: {
        presetKey: [{ required: true, message: '请选择主题预设', trigger: 'change' }]
      },
      tokensOpen: false,
      tokenRows: [],
      currentPresetName: '',
      previewOpen: false,
      preview: null
    }
  },
  created() {
    this.loadPresets()
    listProject({ pageNum: 1, pageSize: 200, type: 'activity' }).then(res => { this.containers = res.rows || [] })
  },
  methods: {
    loadPresets() {
      this.loading = true
      listPresets().then(res => {
        this.presets = res.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    parseTokens(str) {
      if (!str) { return {} }
      try {
        const o = JSON.parse(str)
        return (o && typeof o === 'object') ? o : {}
      } catch (e) {
        return {}
      }
    },
    isColor(v) {
      return typeof v === 'string' && /^#|^rgb|^hsl/i.test(v.trim())
    },
    colorTokens(str) {
      const o = this.parseTokens(str)
      return Object.keys(o).filter(k => this.isColor(o[k])).slice(0, 10).map(k => ({ key: k, value: o[k] }))
    },
    selectPreset(p) {
      this.form.presetKey = p.presetKey
      this.$modal.msgSuccess('已选用主题：' + p.name)
    },
    viewTokens(p) {
      this.currentPresetName = p.name
      getTokens(p.presetKey).then(res => {
        const map = res.data || {}
        this.tokenRows = Object.keys(map).map(k => ({ key: k, value: map[k] }))
        this.tokensOpen = true
      })
    },
    handlePreview() {
      previewTheme(this.form.activityId).then(res => {
        this.preview = res.data
        this.previewOpen = true
      })
    },
    handleDecorate() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          decorate(this.form).then(() => {
            this.$modal.msgSuccess('装修已保存，衍生图管线已触发')
            this.handlePreview()
          })
        }
      })
    },
    levelText(level) {
      const map = { activity: '活动级', tenant: '租户级默认', none: '未装修' }
      return map[level] || level || '-'
    },
    levelTag(level) {
      const map = { activity: 'success', tenant: 'primary', none: 'info' }
      return map[level] || 'info'
    }
  }
}
</script>

<style scoped>
.mt16 { margin-top: 16px; }
.hl { color: #409eff; font-weight: 600; }
.tip { color: #909399; font-size: 12px; line-height: 1.6; margin-top: 4px; }
.preset-card { transition: all .2s; }
.preset-card.active { border-color: #409eff; box-shadow: 0 0 0 2px rgba(64, 158, 255, .2); }
.preset-header { display: flex; align-items: center; justify-content: space-between; }
.preset-name { font-weight: 600; }
.preset-body { text-align: center; }
.preset-preview { width: 100%; height: 110px; border-radius: 6px; background: #f5f7fa; }
.preset-preview.placeholder { display: flex; align-items: center; justify-content: center; color: #c0c4cc; font-size: 12px; }
.preset-key { color: #909399; font-size: 12px; margin-top: 6px; }
.preset-footer { margin-top: 10px; display: flex; align-items: center; justify-content: space-between; }
.swatches { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; justify-content: center; }
.swatch { width: 20px; height: 20px; border-radius: 4px; border: 1px solid rgba(0, 0, 0, .1); display: inline-block; }
.swatch.lg { width: 26px; height: 26px; }
.preview-block { margin-top: 16px; }
.preview-label { font-weight: 600; margin-bottom: 8px; color: #303133; }
.mat-title { font-size: 12px; color: #909399; margin-bottom: 6px; }
.mat-img { width: 100%; border-radius: 6px; background: #f5f7fa; }
.mat-img.logo { height: 80px; width: 80px; }
.mat-img.poster { height: 140px; }
.mat-img.placeholder { display: flex; align-items: center; justify-content: center; color: #c0c4cc; font-size: 12px; height: 80px; }
</style>
