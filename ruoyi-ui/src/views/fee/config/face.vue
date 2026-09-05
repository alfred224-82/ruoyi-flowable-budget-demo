<template>
  <div class="app-container">
    <el-alert title="人脸核身：大额（默认≥2000）或敏感大类报销触发人脸采集，与入职底照比对；不一致仅标记存疑进入异常池，不拦截流程（strong 级证据）。" type="info" :closable="false" style="margin-bottom: 15px;" />

    <el-row :gutter="20">
      <!-- 配置区 -->
      <el-col :span="10" :xs="24">
        <el-card shadow="never" v-loading="configLoading">
          <div slot="header">
            <span>核身配置</span>
            <el-button style="float: right;" type="primary" size="mini" @click="handleSaveConfig" v-hasPermi="['fee:face:edit']">保存配置</el-button>
          </div>
          <el-form :model="config" label-width="130px" size="small">
            <el-form-item label="启用人脸核身">
              <el-switch v-model="config.enabled" />
            </el-form-item>
            <el-form-item label="大额触发阈值">
              <el-input-number v-model="config.amountThreshold" :min="0" :precision="2" style="width: 220px;" />
              <span style="margin-left: 8px; color: #909399;">金额≥该值触发核身</span>
            </el-form-item>
            <el-form-item label="存疑相似度阈值">
              <el-input-number v-model="config.suspiciousSimilarity" :min="0" :max="1" :step="0.05" :precision="2" style="width: 220px;" />
              <span style="margin-left: 8px; color: #909399;">低于该值判定存疑</span>
            </el-form-item>
            <el-form-item label="敏感大类">
              <el-tag
                v-for="cat in config.sensitiveCategories"
                :key="cat"
                closable
                size="medium"
                style="margin-right: 6px; margin-bottom: 6px;"
                @close="removeCategory(cat)">{{ cat }}</el-tag>
              <el-input
                v-if="categoryInputVisible"
                ref="categoryInput"
                v-model="categoryInputValue"
                size="small"
                style="width: 140px;"
                @keyup.enter.native="confirmCategory"
                @blur="confirmCategory" />
              <el-button v-else size="small" icon="el-icon-plus" @click="showCategoryInput">添加大类</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>

      <!-- 核身记录 / 异常池 -->
      <el-col :span="14" :xs="24">
        <el-card shadow="never" v-loading="recordLoading">
          <div slot="header">
            <span>核身记录</span>
            <el-switch v-model="suspiciousOnly" active-text="仅看异常池（存疑）" size="small" style="float: right; margin-top: 4px;" @change="loadRecords" />
          </div>
          <el-table :data="records" border size="small" max-height="520">
            <el-table-column label="报销单" min-width="120" prop="billNo">
              <template slot-scope="scope">{{ scope.row.billNo || '-' }}</template>
            </el-table-column>
            <el-table-column label="用户" width="110" align="center" prop="userName" />
            <el-table-column label="金额" width="110" align="right">
              <template slot-scope="scope">{{ scope.row.amount == null ? '-' : Number(scope.row.amount).toLocaleString() }}</template>
            </el-table-column>
            <el-table-column label="触发类型" width="120" align="center">
              <template slot-scope="scope">
                <el-tag size="mini" :type="scope.row.triggerType === 'AMOUNT_THRESHOLD' ? 'warning' : 'danger'">
                  {{ scope.row.triggerType === 'AMOUNT_THRESHOLD' ? '大额' : '敏感大类' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="相似度" width="90" align="center">
              <template slot-scope="scope">{{ scope.row.similarity == null ? '-' : scope.row.similarity }}</template>
            </el-table-column>
            <el-table-column label="结果" width="110" align="center">
              <template slot-scope="scope">
                <el-tag size="mini" :type="scope.row.isSuspicious === 1 ? 'danger' : 'success'">
                  {{ scope.row.isSuspicious === 1 ? '存疑(异常池)' : '正常' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="核身时间" width="160" align="center" prop="verifyTime" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script>
import { getFaceConfig, saveFaceConfig, listFaceRecords } from '@/api/system/fee/fundPro'

export default {
  name: 'FeeFaceConfig',
  data() {
    return {
      configLoading: true,
      recordLoading: false,
      config: {
        enabled: false,
        amountThreshold: 2000,
        suspiciousSimilarity: 0.6,
        sensitiveCategories: []
      },
      categoryInputVisible: false,
      categoryInputValue: '',
      suspiciousOnly: false,
      records: []
    }
  },
  created() {
    this.loadConfig()
    this.loadRecords()
  },
  methods: {
    loadConfig() {
      this.configLoading = true
      getFaceConfig().then(res => {
        this.config = res.data || this.config
        this.config.sensitiveCategories = this.config.sensitiveCategories || []
        this.configLoading = false
      }).catch(() => { this.configLoading = false })
    },
    handleSaveConfig() {
      saveFaceConfig(this.config).then(() => {
        this.$modal.msgSuccess('配置保存成功')
      })
    },
    showCategoryInput() {
      this.categoryInputVisible = true
      this.$nextTick(() => this.$refs.categoryInput && this.$refs.categoryInput.focus())
    },
    confirmCategory() {
      const val = (this.categoryInputValue || '').trim()
      if (val && this.config.sensitiveCategories.indexOf(val) === -1) {
        this.config.sensitiveCategories.push(val)
      }
      this.categoryInputVisible = false
      this.categoryInputValue = ''
    },
    removeCategory(cat) {
      this.config.sensitiveCategories = this.config.sensitiveCategories.filter(c => c !== cat)
    },
    loadRecords() {
      this.recordLoading = true
      listFaceRecords(this.suspiciousOnly).then(res => {
        this.records = res.data || []
        this.recordLoading = false
      }).catch(() => { this.recordLoading = false })
    }
  }
}
</script>
