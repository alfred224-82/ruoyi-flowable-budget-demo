<template>
  <div class="app-container">
    <el-alert
      title="业态文案映射管理（T10.5 v1.29 文案映射 i18n）"
      type="info"
      description="biz_copy 为系统级共享表（无 tenant_id），仅平台超管维护。按 (biz_code, i18n_key, lang) 唯一键存储多语言文案。三维合并优先级：租户 module_overrides.copy > biz_copy > biz_bundle.copy > 前端硬编码兜底。"
      show-icon
      :closable="false"
      class="mb8"
    />

    <!-- 筛选栏 -->
    <el-form :model="query" ref="queryForm" size="small" :inline="true" label-width="80px">
      <el-form-item label="业态包" prop="bizCode">
        <el-select v-model="query.bizCode" placeholder="全部" clearable style="width: 180px">
          <el-option v-for="b in bizBundleOptions" :key="b.bizCode" :label="b.name + ' (' + b.bizCode + ')'" :value="b.bizCode" />
        </el-select>
      </el-form-item>
      <el-form-item label="语言" prop="lang">
        <el-select v-model="query.lang" placeholder="全部" clearable style="width: 140px">
          <el-option v-for="l in langOptions" :key="l" :label="l" :value="l" />
        </el-select>
      </el-form-item>
      <el-form-item label="模块" prop="module">
        <el-input v-model="query.module" placeholder="common/budget/micro" clearable style="width: 160px" />
      </el-form-item>
      <el-form-item label="Key" prop="i18nKey">
        <el-input v-model="query.i18nKey" placeholder="支持模糊" clearable style="width: 180px" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="query.status" placeholder="全部" clearable style="width: 100px">
          <el-option label="启用" value="0" />
          <el-option label="停用" value="1" />
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
        <el-button type="primary" icon="el-icon-plus" size="mini" @click="handleAdd">新增文案</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="warning" icon="el-icon-document-copy" size="mini" @click="handleCopyToLang">复制到其它语言</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" icon="el-icon-delete" size="mini" @click="handleEvictAll">清空全部缓存</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button icon="el-icon-refresh" size="mini" @click="loadList">刷新</el-button>
      </el-col>
    </el-row>

    <!-- 列表 -->
    <el-table v-loading="loading" :data="list" border size="small">
      <el-table-column label="业态包" prop="bizCode" align="left" width="140" />
      <el-table-column label="语言" prop="lang" align="center" width="90">
        <template slot-scope="scope">
          <el-tag size="mini" type="info">{{ scope.row.lang }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="模块" prop="module" align="center" width="100" />
      <el-table-column label="Key" prop="i18nKey" align="left" width="180" show-overflow-tooltip />
      <el-table-column label="文案值" prop="copyValue" align="left" min-width="200" show-overflow-tooltip />
      <el-table-column label="状态" prop="status" align="center" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="mini">
            {{ scope.row.status === '0' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" prop="remark" align="left" min-width="140" show-overflow-tooltip />
      <el-table-column label="操作" align="center" width="160" fixed="right">
        <template slot-scope="scope">
          <el-button type="text" icon="el-icon-edit" size="mini" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button type="text" icon="el-icon-delete" size="mini" @click="handleDelete(scope.row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 编辑弹窗 -->
    <el-dialog :title="editTitle" :visible.sync="editOpen" width="620px" append-to-body>
      <el-form ref="editForm" :model="editForm" :rules="editRules" label-width="100px">
        <el-form-item label="业态包" prop="bizCode">
          <el-select v-model="editForm.bizCode" placeholder="请选择业态包" style="width: 100%">
            <el-option v-for="b in bizBundleOptions" :key="b.bizCode" :label="b.name + ' (' + b.bizCode + ')'" :value="b.bizCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="语言" prop="lang">
          <el-select v-model="editForm.lang" filterable allow-create placeholder="zh-CN/en-US/zh-TW" style="width: 100%">
            <el-option v-for="l in langOptions" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item label="模块" prop="module">
          <el-input v-model="editForm.module" placeholder="common/budget/micro（可空）" />
        </el-form-item>
        <el-form-item label="Key" prop="i18nKey">
          <el-input v-model="editForm.i18nKey" placeholder="如 project / expense / income" />
        </el-form-item>
        <el-form-item label="文案值" prop="copyValue">
          <el-input v-model="editForm.copyValue" type="textarea" :rows="2" placeholder="当前语言的翻译文本" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="editForm.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="editForm.remark" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" @click="submitEdit">确 定</el-button>
        <el-button @click="editOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 复制到其它语言弹窗 -->
    <el-dialog title="复制文案到其它语言" :visible.sync="copyOpen" width="520px" append-to-body>
      <el-alert
        title="将当前筛选结果的文案批量复制到目标语言（已存在的 (key,lang) 会被覆盖）。"
        type="warning" :closable="false" show-icon style="margin-bottom: 15px"
      />
      <el-form label-width="100px">
        <el-form-item label="目标语言">
          <el-select v-model="copyTargetLang" filterable allow-create placeholder="zh-TW/en-US" style="width: 100%">
            <el-option v-for="l in langOptions" :key="l" :label="l" :value="l" />
          </el-select>
        </el-form-item>
        <el-form-item label="复制条数">
          <el-tag type="info">{{ list.length }} 条</el-tag>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button type="primary" :loading="copyLoading" @click="submitCopy">确 定</el-button>
        <el-button @click="copyOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listCopies, saveCopy, delCopy, evictCopyCache, batchSaveCopies, listLangs
} from '@/api/system/bizCopy'
import { listBizBundle } from '@/api/system/bizBundle'

export default {
  name: 'SystemBizCopy',
  data() {
    return {
      loading: false,
      list: [],
      query: { bizCode: null, lang: null, module: null, i18nKey: null, status: null },
      bizBundleOptions: [],
      langOptions: ['zh-CN', 'en-US', 'zh-TW', 'ja-JP'],
      // 编辑
      editOpen: false,
      editIsEdit: false,
      editTitle: '',
      editForm: {},
      editRules: {
        bizCode: [{ required: true, message: '业态包不能为空', trigger: 'change' }],
        lang: [{ required: true, message: '语言不能为空', trigger: 'change' }],
        i18nKey: [{ required: true, message: 'Key 不能为空', trigger: 'blur' }],
        copyValue: [{ required: true, message: '文案值不能为空', trigger: 'blur' }]
      },
      // 复制到其它语言
      copyOpen: false,
      copyTargetLang: '',
      copyLoading: false
    }
  },
  created() {
    this.loadBizBundles()
    this.loadLangs()
    this.loadList()
  },
  methods: {
    loadBizBundles() {
      listBizBundle().then(res => { this.bizBundleOptions = res.data || [] }).catch(() => {})
    },
    loadLangs() {
      listLangs().then(res => {
        const langs = res.data || []
        const merged = Array.from(new Set([...this.langOptions, ...langs]))
        this.langOptions = merged
      }).catch(() => {})
    },
    loadList() {
      this.loading = true
      listCopies(this.query).then(res => {
        this.list = res.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() { this.loadList() },
    resetQuery() {
      this.query = { bizCode: null, lang: null, module: null, i18nKey: null, status: null }
      this.loadList()
    },
    handleAdd() {
      this.editIsEdit = false
      this.editTitle = '新增文案'
      this.editForm = { bizCode: 'fund_control', lang: 'zh-CN', module: 'common', i18nKey: '', copyValue: '', status: '0', remark: '' }
      this.editOpen = true
    },
    handleEdit(row) {
      this.editIsEdit = true
      this.editTitle = '编辑文案'
      this.editForm = { ...row }
      this.editOpen = true
    },
    submitEdit() {
      this.$refs.editForm.validate(valid => {
        if (!valid) return
        saveCopy(this.editForm).then(() => {
          this.$modal.msgSuccess('保存成功')
          this.editOpen = false
          this.loadList()
          this.loadLangs()
        })
      })
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除 [' + row.bizCode + '/' + row.lang + '/' + row.i18nKey + '] ?').then(() => {
        return delCopy(row.id)
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.loadList()
      }).catch(() => {})
    },
    handleCopyToLang() {
      if (!this.list.length) {
        this.$modal.msgWarning('当前筛选结果为空，无法复制')
        return
      }
      this.copyTargetLang = ''
      this.copyOpen = true
    },
    submitCopy() {
      if (!this.copyTargetLang) {
        this.$modal.msgWarning('请选择目标语言')
        return
      }
      this.copyLoading = true
      const batch = this.list.map(row => ({
        bizCode: row.bizCode,
        i18nKey: row.i18nKey,
        lang: this.copyTargetLang,
        copyValue: row.copyValue,
        module: row.module,
        status: row.status || '0',
        remark: '从 ' + row.lang + ' 复制'
      }))
      batchSaveCopies(batch).then(() => {
        this.$modal.msgSuccess('复制成功，共 ' + batch.length + ' 条')
        this.copyOpen = false
        this.loadList()
        this.loadLangs()
      }).finally(() => { this.copyLoading = false })
    },
    handleEvictAll() {
      this.$modal.confirm('确认清空全部业态文案缓存？').then(() => {
        return evictCopyCache()
      }).then(() => {
        this.$modal.msgSuccess('缓存已清空')
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
</style>
