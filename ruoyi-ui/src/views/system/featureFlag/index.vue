<template>
  <div class="app-container">
    <el-alert
      title="功能开关管理（T10.3 表驱动开关）"
      type="info"
      :description="alertDesc"
      show-icon
      :closable="false"
      class="mb8"
    />

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-select v-model="filterGroup" size="small" placeholder="按分组筛选" clearable @change="loadList" style="width:160px;">
          <el-option v-for="g in groups" :key="g" :label="g" :value="g" />
        </el-select>
      </el-col>
      <el-col :span="1.5">
        <el-select v-model="filterEdition" size="small" placeholder="按版本筛选" clearable style="width:160px;">
          <el-option label="小微版" value="MICRO" />
          <el-option label="基础版" value="BASIC" />
          <el-option label="高级版" value="ADVANCED" />
          <el-option label="专业版" value="PROFESSIONAL" />
        </el-select>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" icon="el-icon-delete" size="mini" @click="handleEvictCache">清空缓存</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button icon="el-icon-refresh" size="mini" @click="loadList">刷新</el-button>
      </el-col>
      <el-col :span="6" style="text-align:right;">
        <span class="stat-text">
          共 {{ filteredList.length }} 项 | 启用 {{ enabledCount }} | 停用 {{ disabledCount }}
        </span>
      </el-col>
    </el-row>

    <!-- 开关列表 -->
    <el-table v-loading="loading" :data="filteredList" border :row-class-name="rowClassName">
      <el-table-column label="分组" prop="flagGroup" align="center" width="80" />
      <el-table-column label="开关编码" prop="code" align="left" min-width="220">
        <template slot-scope="scope">
          <span class="code-text">{{ scope.row.code }}</span>
          <span v-if="scope.row.name" class="name-text">（{{ scope.row.name }}）</span>
        </template>
      </el-table-column>
      <el-table-column label="业态包" prop="bizCode" align="center" width="110" />
      <el-table-column label="基础版" align="center" width="80">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.basicEnabled === 1" type="success" size="mini">ON</el-tag>
          <el-tag v-else type="info" size="mini">OFF</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="高级版" align="center" width="80">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.advancedEnabled === 1" type="success" size="mini">ON</el-tag>
          <el-tag v-else type="info" size="mini">OFF</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="专业版" align="center" width="80">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.proEnabled === 1" type="success" size="mini">ON</el-tag>
          <el-tag v-else type="info" size="mini">OFF</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="小微版" align="center" width="80">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.microEnabled === 1" type="success" size="mini">ON</el-tag>
          <el-tag v-else type="info" size="mini">OFF</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="90">
        <template slot-scope="scope">
          <el-switch
            v-model="scope.row.status"
            active-value="0"
            inactive-value="1"
            @change="handleToggle(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column label="备注" prop="remark" align="left" min-width="140" show-overflow-tooltip />
    </el-table>

    <!-- 版本功能概览 -->
    <el-card shadow="never" class="edition-card">
      <div slot="header">
        <span style="font-weight:600;">版本功能概览（表驱动 FEAT_* ∪ 常量兜底 legacy）</span>
        <el-button type="text" size="mini" icon="el-icon-refresh" style="float:right;" @click="loadEditionSummary">刷新</el-button>
      </div>
      <el-row :gutter="16">
        <el-col :span="6" v-for="ed in editions" :key="ed.key">
          <div class="edition-col">
            <div class="edition-label">{{ ed.label }}（{{ ed.key }}）</div>
            <div class="edition-count">{{ editionMap[ed.key] ? editionMap[ed.key].length : 0 }} 项功能</div>
            <div class="tag-box">
              <el-tag
                v-for="f in (editionMap[ed.key] || []).slice(0, 20)"
                :key="f"
                size="mini"
                class="feature-tag"
                :type="f.startsWith('FEAT_') ? '' : 'info'"
              >{{ f }}</el-tag>
              <span v-if="(editionMap[ed.key] || []).length > 20" style="color:#909399;font-size:12px;">
                ...等 {{ editionMap[ed.key].length }} 项
              </span>
              <span v-if="!editionMap[ed.key] || editionMap[ed.key].length === 0" style="color:#909399;font-size:12px;">
                无功能
              </span>
            </div>
          </div>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<script>
import { listFeatureFlag, getEditionFeatures, toggleFeatureFlagStatus, evictFeatureFlagCache } from '@/api/system/featureFlag'

export default {
  name: 'SystemFeatureFlag',
  data() {
    return {
      alertDesc: 'feature_flag 表驱动开关；EditionFeatureConfig 常量为兜底。可启停开关（status），不可改版本归属（basic/advanced/pro_enabled）以免串版本。启停即时生效（缓存刷新）。',
      loading: false,
      list: [],
      filterGroup: '',
      filterEdition: '',
      // 版本功能概览
      editions: [
        { key: 'MICRO', label: '小微版' },
        { key: 'BASIC', label: '基础版' },
        { key: 'ADVANCED', label: '高级版' },
        { key: 'PROFESSIONAL', label: '专业版' }
      ],
      editionMap: {}
    }
  },
  computed: {
    groups() {
      const set = new Set()
      this.list.forEach(item => {
        if (item.flagGroup) set.add(item.flagGroup)
      })
      return Array.from(set).sort()
    },
    filteredList() {
      let result = this.list
      if (this.filterGroup) {
        result = result.filter(item => item.flagGroup === this.filterGroup)
      }
      if (this.filterEdition) {
        const col = this.filterEdition === 'PROFESSIONAL' ? 'proEnabled'
          : this.filterEdition === 'ADVANCED' ? 'advancedEnabled'
          : this.filterEdition === 'MICRO' ? 'microEnabled' : 'basicEnabled'
        result = result.filter(item => item[col] === 1)
      }
      return result
    },
    enabledCount() {
      return this.list.filter(item => item.status === '0').length
    },
    disabledCount() {
      return this.list.filter(item => item.status === '1').length
    }
  },
  created() {
    this.loadList()
    this.loadEditionSummary()
  },
  methods: {
    loadList() {
      this.loading = true
      listFeatureFlag().then(response => {
        this.list = response.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    loadEditionSummary() {
      this.editions.forEach(ed => {
        getEditionFeatures(ed.key).then(response => {
          const features = response.data || []
          this.$set(this.editionMap, ed.key, Array.isArray(features) ? features.sort() : [])
        })
      })
    },
    handleToggle(row) {
      const newStatus = row.status
      const label = newStatus === '0' ? '启用' : '停用'
      this.$confirm('确认' + label + '开关 [' + row.code + ']？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        toggleFeatureFlagStatus(row.id, newStatus).then(() => {
          this.$modal.msgSuccess(label + '成功')
          this.loadEditionSummary()
        }).catch(() => {
          row.status = newStatus === '0' ? '1' : '0'
        })
      }).catch(() => {
        row.status = newStatus === '0' ? '1' : '0'
      })
    },
    handleEvictCache() {
      this.$confirm('确认清空全部功能开关缓存？', '提示', { type: 'warning' }).then(() => {
        evictFeatureFlagCache().then(() => {
          this.$modal.msgSuccess('缓存已清除')
          this.loadEditionSummary()
        })
      }).catch(() => {})
    },
    rowClassName({ row }) {
      return row.status === '1' ? 'disabled-row' : ''
    }
  }
}
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.stat-text { font-size: 13px; color: #606266; line-height: 32px; }
.code-text { font-family: Consolas, Monaco, monospace; font-size: 12px; color: #303133; }
.name-text { font-size: 12px; color: #909399; }
.edition-card { margin-top: 16px; }
.edition-col { text-align: center; }
.edition-label { font-weight: 600; font-size: 14px; color: #303133; margin-bottom: 4px; }
.edition-count { font-size: 12px; color: #909399; margin-bottom: 8px; }
.tag-box { text-align: left; line-height: 2; }
.feature-tag { margin: 0 4px 4px 0; }
::v-deep .disabled-row { opacity: 0.5; }
</style>
