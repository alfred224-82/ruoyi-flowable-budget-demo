<template>
  <div class="app-container">
    <el-alert
      title="业态包管理（T10.2 多业态平台底座）"
      type="info"
      description="biz_bundle 为系统级共享表（无 tenant_id），仅平台超管维护。新增业态包只需插数据 + 补文案，不动核心表结构。feature_flags / copy / schema_blocks / engine_strategy 均为 JSON 字段。"
      show-icon
      :closable="false"
      class="mb8"
    />

    <!-- 工具栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" icon="el-icon-plus" size="mini" @click="handleAdd">新增业态包</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" icon="el-icon-delete" size="mini" @click="handleEvictAll">清空全部缓存</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button icon="el-icon-refresh" size="mini" @click="loadList">刷新</el-button>
      </el-col>
    </el-row>

    <!-- 列表 -->
    <el-table v-loading="loading" :data="list" border>
      <el-table-column label="业态编码" prop="bizCode" align="left" width="150" />
      <el-table-column label="业态名称" prop="name" align="left" width="150" />
      <el-table-column label="引擎策略" prop="engineStrategy" align="left" width="160" show-overflow-tooltip />
      <el-table-column label="状态" prop="status" align="center" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="mini">
            {{ scope.row.status === '0' ? '启用' : '停用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="开关数" align="center" width="80">
        <template slot-scope="scope">
          <span>{{ parseJsonArray(scope.row.featureFlags).length }}</span>
        </template>
      </el-table-column>
      <el-table-column label="文案键数" align="center" width="80">
        <template slot-scope="scope">
          <span>{{ parseJsonMap(scope.row.copy).length }}</span>
        </template>
      </el-table-column>
      <el-table-column label="备注" prop="remark" align="left" min-width="160" show-overflow-tooltip />
      <el-table-column label="操作" align="center" width="220">
        <template slot-scope="scope">
          <el-button type="text" icon="el-icon-view" size="mini" @click="handleDetail(scope.row)">详情</el-button>
          <el-button type="text" icon="el-icon-edit" size="mini" @click="handleEdit(scope.row)">编辑</el-button>
          <el-button type="text" icon="el-icon-delete" size="mini" @click="handleEvict(scope.row)">清缓存</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 详情弹窗 -->
    <el-dialog title="业态包详情" :visible.sync="detailOpen" width="780px" append-to-body>
      <el-descriptions :column="2" border size="small">
        <el-descriptions-item label="业态编码">{{ detail.bizCode }}</el-descriptions-item>
        <el-descriptions-item label="业态名称">{{ detail.name }}</el-descriptions-item>
        <el-descriptions-item label="引擎策略" :span="2">{{ detail.engineStrategy || '—' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === '0' ? 'success' : 'info'" size="mini">
            {{ detail.status === '0' ? '启用' : '停用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="备注">{{ detail.remark || '—' }}</el-descriptions-item>
      </el-descriptions>

      <div class="detail-section">
        <div class="detail-title">功能开关（feature_flags）— {{ detailFlags.length }} 项</div>
        <div v-if="detailFlags.length" class="tag-box">
          <el-tag v-for="f in detailFlags" :key="f" size="mini" class="feature-tag">{{ f }}</el-tag>
        </div>
        <span v-else style="color:#909399;font-size:12px;">未声明（三维能力计算时跳过业态过滤）</span>
      </div>

      <div class="detail-section">
        <div class="detail-title">文案映射（copy）— {{ detailCopyKeys.length }} 键</div>
        <el-table v-if="detailCopyKeys.length" :data="detailCopyEntries" size="mini" max-height="200" border>
          <el-table-column label="键" prop="key" width="200" />
          <el-table-column label="值" prop="value" show-overflow-tooltip />
        </el-table>
        <span v-else style="color:#909399;font-size:12px;">无文案覆盖</span>
      </div>

      <div class="detail-section">
        <div class="detail-title">页面区块（schema_blocks）</div>
        <el-input v-if="detail.schemaBlocks" type="textarea" :value="detail.schemaBlocks" :rows="4" readonly />
        <span v-else style="color:#909399;font-size:12px;">未配置</span>
      </div>

      <div class="detail-section">
        <div class="detail-title">菜单范围（menus）</div>
        <el-input v-if="detail.menus" type="textarea" :value="detail.menus" :rows="3" readonly />
        <span v-else style="color:#909399;font-size:12px;">未配置</span>
      </div>

      <div slot="footer">
        <el-button @click="detailOpen = false">关 闭</el-button>
      </div>
    </el-dialog>

    <!-- 编辑弹窗 -->
    <el-dialog :title="editTitle" :visible.sync="editOpen" width="700px" append-to-body>
      <el-form ref="editForm" :model="editForm" :rules="editRules" label-width="110px">
        <el-form-item label="业态编码" prop="bizCode">
          <el-input v-model="editForm.bizCode" :disabled="editIsEdit" placeholder="如 fund_control" />
        </el-form-item>
        <el-form-item label="业态名称" prop="name">
          <el-input v-model="editForm.name" placeholder="如 资金管控" />
        </el-form-item>
        <el-form-item label="引擎策略" prop="engineStrategy">
          <el-input v-model="editForm.engineStrategy" placeholder="组队引擎策略（可空）" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="editForm.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="功能开关JSON" prop="featureFlags">
          <el-input v-model="editForm.featureFlags" type="textarea" :rows="3" placeholder='["FEAT_BUDGET_PREPARE","FEAT_SUBJECT_3LAYER"]' />
        </el-form-item>
        <el-form-item label="文案映射JSON" prop="copy">
          <el-input v-model="editForm.copy" type="textarea" :rows="3" placeholder='{"app.title":"资金管控系统"}' />
        </el-form-item>
        <el-form-item label="页面区块JSON" prop="schemaBlocks">
          <el-input v-model="editForm.schemaBlocks" type="textarea" :rows="3" placeholder="Schema 区块定义（可空）" />
        </el-form-item>
        <el-form-item label="菜单范围JSON" prop="menus">
          <el-input v-model="editForm.menus" type="textarea" :rows="2" placeholder="菜单ID列表（可空）" />
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
  </div>
</template>

<script>
import { listBizBundle, getBizBundle, saveBizBundle, evictBizBundleCache } from '@/api/system/bizBundle'

export default {
  name: 'SystemBizBundle',
  data() {
    return {
      loading: false,
      list: [],
      // 详情
      detailOpen: false,
      detail: {},
      detailFlags: [],
      detailCopyKeys: [],
      detailCopyEntries: [],
      // 编辑
      editOpen: false,
      editIsEdit: false,
      editTitle: '',
      editForm: {},
      editRules: {
        bizCode: [{ required: true, message: '业态编码不能为空', trigger: 'blur' }],
        name: [{ required: true, message: '业态名称不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.loadList()
  },
  methods: {
    loadList() {
      this.loading = true
      listBizBundle().then(response => {
        this.list = response.data || []
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    parseJsonArray(json) {
      if (!json) return []
      try {
        const arr = JSON.parse(json)
        return Array.isArray(arr) ? arr : []
      } catch (e) {
        return []
      }
    },
    parseJsonMap(json) {
      if (!json) return []
      try {
        const obj = JSON.parse(json)
        return typeof obj === 'object' && obj !== null ? Object.keys(obj) : []
      } catch (e) {
        return []
      }
    },
    handleDetail(row) {
      getBizBundle(row.bizCode).then(response => {
        const data = response.data || {}
        this.detail = data.bundle || row
        const flags = data.featureFlags || []
        this.detailFlags = Array.isArray(flags) ? flags : []
        const copy = data.copy || {}
        this.detailCopyKeys = Object.keys(copy)
        this.detailCopyEntries = this.detailCopyKeys.map(k => ({ key: k, value: copy[k] }))
        this.detailOpen = true
      })
    },
    handleAdd() {
      this.editIsEdit = false
      this.editTitle = '新增业态包'
      this.editForm = {
        bizCode: '', name: '', engineStrategy: '', status: '0',
        featureFlags: '[]', copy: '{}', schemaBlocks: '', menus: '', remark: ''
      }
      this.editOpen = true
    },
    handleEdit(row) {
      this.editIsEdit = true
      this.editTitle = '编辑业态包 - ' + row.bizCode
      this.editForm = {
        bizCode: row.bizCode,
        name: row.name,
        engineStrategy: row.engineStrategy || '',
        status: row.status || '0',
        featureFlags: row.featureFlags || '[]',
        copy: row.copy || '{}',
        schemaBlocks: row.schemaBlocks || '',
        menus: row.menus || '',
        remark: row.remark || ''
      }
      this.editOpen = true
    },
    submitEdit() {
      this.$refs.editForm.validate(valid => {
        if (!valid) return
        saveBizBundle(this.editForm).then(() => {
          this.$modal.msgSuccess('保存成功')
          this.editOpen = false
          this.loadList()
        })
      })
    },
    handleEvict(row) {
      evictBizBundleCache(row.bizCode).then(() => {
        this.$modal.msgSuccess('缓存已清除')
      })
    },
    handleEvictAll() {
      this.$confirm('确认清空全部业态包缓存？', '提示', { type: 'warning' }).then(() => {
        evictBizBundleCache('').then(() => {
          this.$modal.msgSuccess('全部缓存已清除')
        })
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.mb8 { margin-bottom: 8px; }
.detail-section { margin-top: 16px; }
.detail-title { font-weight: 600; font-size: 13px; color: #303133; margin-bottom: 8px; }
.tag-box { line-height: 2; }
.feature-tag { margin: 0 6px 6px 0; }
</style>
