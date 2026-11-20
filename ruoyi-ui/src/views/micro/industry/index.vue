<template>
  <div class="app-container">
    <el-alert
      title="新增行业只需配模板，小程序零改动（T15.10）"
      type="info"
      description="行业模板为系统级共享数据，仅平台管理员维护。① 维护行业基础信息；② 配置「业务角色名 → 标准角色编码」映射；④ 新建小微版租户时按行业一键初始化角色映射（幂等，已存在的角色自动跳过）。"
      show-icon
      :closable="false"
      class="mb8"
    />

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="行业名称" prop="industryName">
        <el-input v-model="queryParams.industryName" placeholder="请输入行业名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="行业编码" prop="industryCode">
        <el-input v-model="queryParams.industryCode" placeholder="如 billiards / pet" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable style="width:120px;">
          <el-option label="启用" value="0" />
          <el-option label="停用" value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['system:industry:add']">新增行业模板</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:industry:edit']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="industryList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="图标" align="center" prop="icon" width="70">
        <template slot-scope="scope">
          <i v-if="isIconClass(scope.row.icon)" :class="scope.row.icon" style="font-size:18px;" />
          <span v-else style="font-size:18px;">{{ scope.row.icon || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="行业名称" align="left" prop="industryName" min-width="120" show-overflow-tooltip />
      <el-table-column label="行业编码" align="center" prop="industryCode" width="130" />
      <el-table-column label="排序" align="center" prop="sortOrder" width="70" />
      <el-table-column label="预置数据" align="center" width="90">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.presetConfig" type="success" size="mini">已配置</el-tag>
          <span v-else style="color:#c0c4cc;">—</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="mini">{{ scope.row.status === '0' ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="是否可语音" align="center" width="100">
        <template slot-scope="scope">
          <el-switch v-model="scope.row.voiceEnabled" :active-value="1" :inactive-value="0" disabled />
        </template>
      </el-table-column>
      <el-table-column label="备注" align="left" prop="remark" min-width="120" show-overflow-tooltip />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="290">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:industry:edit']">编辑</el-button>
          <el-button size="mini" type="text" icon="el-icon-s-operation" @click="handleRoleMap(scope.row)" v-hasPermi="['system:industry:roleMap']">角色映射</el-button>
          <el-button size="mini" type="text" icon="el-icon-magic-stick" @click="handleInit(scope.row)" v-hasPermi="['system:industry:edit']">初始化租户</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:industry:edit']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 新增 / 编辑行业模板 -->
    <el-dialog :title="title" :visible.sync="open" width="640px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="100px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="行业名称" prop="industryName">
              <el-input v-model="form.industryName" placeholder="如：台球厅 / 宠物店" maxlength="50" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="行业编码" prop="industryCode">
              <el-input v-model="form.industryCode" :disabled="form.id != null" placeholder="小写字母+下划线，如 billiards" maxlength="50" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-row>
          <el-col :span="12">
            <el-form-item label="图标" prop="icon">
              <el-input v-model="form.icon" placeholder="Element 图标类名或 emoji" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="排序" prop="sortOrder">
              <el-input-number v-model="form.sortOrder" :min="0" :max="9999" controls-position="right" style="width:100%;" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
          <span style="color:#909399;font-size:12px;margin-left:12px;">停用后不再出现在新建租户的行业下拉中</span>
        </el-form-item>
        <el-form-item label="是否可语音" prop="voiceEnabled">
          <el-switch v-model="form.voiceEnabled" :active-value="1" :inactive-value="0" />
          <span style="color:#909399;font-size:12px;margin-left:12px;">行业语音能力总开关（三级控制：admin→platform_admin→boss）</span>
        </el-form-item>
        <el-form-item label="预置数据" prop="presetConfig">
          <el-input
            v-model="form.presetConfig"
            type="textarea"
            :rows="6"
            placeholder='预留字段：行业预置数据（JSON），如 {"categories":[...],"items":[...]}'
          />
          <div style="color:#909399;font-size:12px;line-height:1.6;">
            必须为合法 JSON。当前后端仅存储不解析，供后续「按行业预置服务分类 / 服务项目」扩展使用；不确定时请留空。
          </div>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" placeholder="备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- ② 角色映射：业务角色名 → 标准角色编码（整表替换保存） -->
    <el-dialog :title="'角色映射 - ' + currentIndustry.industryName" :visible.sync="mapOpen" width="820px" append-to-body>
      <el-alert
        title="同一行业内业务角色名不可重复；标准角色编码只能从平台标准角色集中选择"
        type="warning"
        :closable="false"
        show-icon
        class="mb8"
      />
      <el-row :gutter="10" class="mb8">
        <el-col :span="1.5">
          <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="addMapRow">添加映射</el-button>
        </el-col>
      </el-row>
      <el-table v-loading="mapLoading" :data="roleMaps" max-height="380">
        <el-table-column label="业务角色名" align="left" min-width="160">
          <template slot-scope="scope">
            <el-input v-model="scope.row.bizRoleName" size="mini" placeholder="如：助教 / 教练" maxlength="50" />
          </template>
        </el-table-column>
        <el-table-column label="标准角色编码" align="left" min-width="180">
          <template slot-scope="scope">
            <el-select v-model="scope.row.roleCode" size="mini" placeholder="请选择标准角色" filterable style="width:100%;">
              <el-option
                v-for="r in standardRoles"
                :key="r.roleCode"
                :label="r.roleName + '（' + r.roleCode + '）'"
                :value="r.roleCode"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="默认启用" align="center" width="100">
          <template slot-scope="scope">
            <el-switch v-model="scope.row.defaultFlag" :active-value="1" :inactive-value="0" />
          </template>
        </el-table-column>
        <el-table-column label="排序" align="center" width="120">
          <template slot-scope="scope">
            <el-input-number v-model="scope.row.sortOrder" size="mini" :min="0" :max="9999" controls-position="right" style="width:100%;" />
          </template>
        </el-table-column>
        <el-table-column label="操作" align="center" width="80">
          <template slot-scope="scope">
            <el-button size="mini" type="text" icon="el-icon-delete" @click="removeMapRow(scope.$index)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="mapSaving" @click="submitRoleMap">保存映射</el-button>
        <el-button @click="mapOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- ④ 按行业初始化目标租户角色映射 -->
    <el-dialog :title="'初始化租户 - ' + currentIndustry.industryName" :visible.sync="initOpen" width="520px" append-to-body>
      <el-form ref="initForm" :model="initForm" :rules="initRules" label-width="100px">
        <el-form-item label="行业模板">
          <span>{{ currentIndustry.industryName }}（{{ currentIndustry.industryCode }}）</span>
        </el-form-item>
        <el-form-item label="目标租户" prop="tenantId">
          <el-select
            v-model="initForm.tenantId"
            placeholder="输入企业名称搜索"
            filterable
            remote
            reserve-keyword
            :remote-method="remoteTenant"
            :loading="tenantLoading"
            style="width:100%;"
          >
            <el-option v-for="t in tenantOptions" :key="t.id" :label="t.companyName + '（' + t.tenantCode + '）'" :value="t.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <div style="color:#909399;font-size:12px;line-height:1.6;padding:0 20px;">
        按本行业的角色映射写入目标租户：业务角色名作为显示名，「默认启用」的角色置为启用、其余置为禁用。操作幂等，目标租户已存在的角色编码自动跳过。
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" :loading="initSaving" @click="submitInit">确认初始化</el-button>
        <el-button @click="initOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listIndustry, getIndustry, addIndustry, updateIndustry, delIndustry, listRoleMap, saveRoleMap, initTenantByIndustry } from '@/api/micro/industry'
import { listStandardRoles } from '@/api/micro/tenantRole'
import { listTenant } from '@/api/system/tenant'

export default {
  name: 'MicroIndustry',
  data() {
    // 预置数据为预留 JSON 字段，前端做格式校验避免写入非法内容
    const validateJson = (rule, value, callback) => {
      if (!value) return callback()
      try {
        JSON.parse(value)
        callback()
      } catch (err) {
        callback(new Error('预置数据必须是合法 JSON：' + err.message))
      }
    }
    return {
      loading: true,
      showSearch: true,
      total: 0,
      industryList: [],
      ids: [],
      multiple: true,
      // 新增 / 编辑
      open: false,
      title: '',
      // 角色映射
      mapOpen: false,
      mapLoading: false,
      mapSaving: false,
      roleMaps: [],
      standardRoles: [],
      currentIndustry: {},
      // 初始化租户
      initOpen: false,
      initSaving: false,
      tenantLoading: false,
      tenantOptions: [],
      initForm: {},
      queryParams: { pageNum: 1, pageSize: 10, industryName: null, industryCode: null, status: null },
      form: {},
      initRules: {
        tenantId: [{ required: true, message: '请选择目标租户', trigger: 'change' }]
      },
      rules: {
        industryName: [{ required: true, message: '行业名称不能为空', trigger: 'blur' }],
        industryCode: [
          { required: true, message: '行业编码不能为空', trigger: 'blur' },
          { pattern: /^[a-z][a-z0-9_]*$/, message: '行业编码须以小写字母开头，仅含小写字母、数字与下划线', trigger: 'blur' }
        ],
        presetConfig: [{ validator: validateJson, trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listIndustry(this.queryParams).then(response => {
        this.industryList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    isIconClass(val) {
      return typeof val === 'string' && val.indexOf('el-icon-') === 0
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.multiple = !selection.length
    },
    reset() {
      this.form = { id: null, industryCode: null, industryName: null, icon: null, presetConfig: null, sortOrder: 0, status: '0', voiceEnabled: 1, remark: null }
      this.resetForm('form')
    },
    cancel() {
      this.open = false
      this.reset()
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增行业模板'
    },
    handleUpdate(row) {
      this.reset()
      getIndustry(row.id).then(response => {
        this.form = response.data
        this.open = true
        this.title = '编辑行业模板'
      })
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) return
        if (this.form.id != null) {
          updateIndustry(this.form).then(() => {
            this.$modal.msgSuccess('修改成功')
            this.open = false
            this.getList()
          })
        } else {
          addIndustry(this.form).then(() => {
            this.$modal.msgSuccess('新增成功')
            this.open = false
            this.getList()
          })
        }
      })
    },
    handleDelete(row) {
      const delIds = row.id ? [row.id] : this.ids
      if (!delIds.length) return
      this.$modal.confirm('确认删除选中的 ' + delIds.length + ' 个行业模板吗？其角色映射将不再用于新租户初始化。')
        .then(() => delIndustry(delIds.join(',')))
        .then(() => {
          this.$modal.msgSuccess('删除成功')
          this.getList()
        })
        .catch(() => {})
    },
    /** ② 打开角色映射：加载标准角色集（admin 上下文返回全量）+ 该行业既有映射 */
    handleRoleMap(row) {
      this.currentIndustry = row
      this.mapOpen = true
      this.mapLoading = true
      Promise.all([listStandardRoles(), listRoleMap(row.industryCode)]).then(results => {
        this.standardRoles = results[0].data || []
        this.roleMaps = (results[1].data || []).map(item => ({
          bizRoleName: item.bizRoleName,
          roleCode: item.roleCode,
          defaultFlag: item.defaultFlag == null ? 0 : item.defaultFlag,
          sortOrder: item.sortOrder == null ? 0 : item.sortOrder
        }))
        this.mapLoading = false
      }).catch(() => { this.mapLoading = false })
    },
    addMapRow() {
      this.roleMaps.push({ bizRoleName: null, roleCode: null, defaultFlag: 1, sortOrder: this.roleMaps.length + 1 })
    },
    removeMapRow(index) {
      this.roleMaps.splice(index, 1)
    },
    /**
     * 保存角色映射（后端整表替换：先物理删除该行业全部映射再批量插入）。
     * ★前端先拦截空行与业务角色名重复，避免被后端静默丢弃造成「保存成功但数据丢失」。
     */
    submitRoleMap() {
      const rows = this.roleMaps.filter(item => item.bizRoleName && item.roleCode)
      if (rows.length !== this.roleMaps.length) {
        this.$modal.msgWarning('存在业务角色名或标准角色编码为空的行，请补全或移除后再保存')
        return
      }
      const names = rows.map(item => item.bizRoleName)
      const dup = names.filter((name, index) => names.indexOf(name) !== index)
      if (dup.length) {
        this.$modal.msgWarning('业务角色名重复：' + Array.from(new Set(dup)).join('、'))
        return
      }
      this.$modal.confirm('保存将整表替换「' + this.currentIndustry.industryName + '」的角色映射（共 ' + rows.length + ' 条），确认继续？')
        .then(() => {
          this.mapSaving = true
          return saveRoleMap(this.currentIndustry.industryCode, rows)
        })
        .then(() => {
          this.$modal.msgSuccess('保存成功')
          this.mapSaving = false
          this.mapOpen = false
        })
        .catch(() => { this.mapSaving = false })
    },
    /** ④ 初始化租户 */
    handleInit(row) {
      this.currentIndustry = row
      this.initForm = { tenantId: null }
      this.initOpen = true
      this.remoteTenant('')
    },
    remoteTenant(keyword) {
      this.tenantLoading = true
      // ★sys_tenant.status 为字符串枚举（PENDING/NORMAL/DISABLED/EXPIRED），非 sys_user 的 0/1
      const query = { pageNum: 1, pageSize: 20, status: 'NORMAL' }
      const kw = (keyword || '').trim()
      if (kw) {
        query.companyName = kw
      }
      listTenant(query).then(response => {
        this.tenantOptions = response.rows || []
        this.tenantLoading = false
      }).catch(() => { this.tenantLoading = false })
    },
    submitInit() {
      this.$refs['initForm'].validate(valid => {
        if (!valid) return
        this.initSaving = true
        initTenantByIndustry(this.initForm.tenantId, this.currentIndustry.industryCode).then(response => {
          this.initSaving = false
          const count = response.data || 0
          if (count > 0) {
            this.$modal.msgSuccess('初始化成功，新增 ' + count + ' 条角色映射')
          } else {
            this.$modal.msgWarning('未新增角色映射：该行业尚未配置映射，或目标租户已全部存在')
          }
          this.initOpen = false
        }).catch(() => { this.initSaving = false })
      })
    }
  }
}
</script>
