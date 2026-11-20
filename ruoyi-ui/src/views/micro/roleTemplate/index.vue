<template>
  <div class="app-container">
    <el-alert
      title="角色模板管理（T15.7/T15.26/T15.29）：三层控制模型第①层「权限天花板」"
      type="info"
      description="role_template 按 version_type × role_code 绑定菜单(menu_keys)与小程序区块(block_keys)配方。第①层角色模板定天花板，第②层页面装修只能在天花板内调排版，第③层 perms 做接口级兜底。lite→pro 升级 = 换配方（菜单扩围），角色 code / 用户 / 业务数据零迁移。"
      show-icon
      :closable="false"
      class="mb8"
    />
    <el-alert
      title="系统级共享表，仅 admin 可维护"
      type="warning"
      description="role_template 无 tenant_id、无 del_flag（已在 IGNORE_TENANT_TABLES），不做租户隔离；删除为物理删除。平台管理员无 system:roleTemplate:* 权限，看不到本页。编辑时 version_type / role_code 为身份字段，后端强制忽略改动以保证 uk_version_role 唯一键稳定。"
      show-icon
      :closable="false"
      class="mb8"
    />

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="90px">
      <el-form-item label="版本档位" prop="versionType">
        <el-select v-model="queryParams.versionType" placeholder="请选择版本档位" clearable style="width:220px;">
          <el-option v-for="item in versionOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="标准角色" prop="roleCode">
        <el-select v-model="queryParams.roleCode" placeholder="请选择标准角色" clearable filterable style="width:200px;">
          <el-option v-for="item in roleOptions" :key="item.roleCode" :label="item.roleName + '（' + item.roleCode + '）'" :value="item.roleCode" />
        </el-select>
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['system:roleTemplate:add']">新增配方</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-delete" size="mini" :disabled="multiple" @click="handleDelete" v-hasPermi="['system:roleTemplate:add']">删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="templateList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="50" align="center" />
      <el-table-column label="版本档位" align="left" min-width="190" show-overflow-tooltip>
        <template slot-scope="scope">
          <span>{{ versionLabel(scope.row.versionType) }}</span>
          <el-tag size="mini" type="info" class="code-tag">{{ scope.row.versionType }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标准角色" align="left" min-width="150" show-overflow-tooltip>
        <template slot-scope="scope">
          <span>{{ roleLabel(scope.row.roleCode) }}</span>
          <el-tag size="mini" class="code-tag">{{ scope.row.roleCode }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="菜单数" align="center" width="90">
        <template slot-scope="scope">
          <el-button type="text" size="mini" @click="handleDetail(scope.row)">{{ parseKeys(scope.row.menuKeys).length }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="区块数" align="center" width="90">
        <template slot-scope="scope">
          <el-button type="text" size="mini" @click="handleDetail(scope.row)">{{ parseKeys(scope.row.blockKeys).length }}</el-button>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" width="90">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="mini">{{ scope.row.status === '0' ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="备注" align="left" prop="remark" min-width="220" show-overflow-tooltip />
      <el-table-column label="更新时间" align="center" prop="updateTime" width="160" />
      <el-table-column label="操作" align="center" width="230" class-name="small-padding fixed-width">
        <template slot-scope="scope">
          <el-button type="text" size="mini" icon="el-icon-view" @click="handleDetail(scope.row)" v-hasPermi="['system:roleTemplate:list']">配方</el-button>
          <el-button type="text" size="mini" icon="el-icon-menu" @click="handleBind(scope.row)" v-hasPermi="['system:roleTemplate:bindMenu']">绑定菜单</el-button>
          <el-button type="text" size="mini" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['system:roleTemplate:add']">编辑</el-button>
          <el-button type="text" size="mini" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['system:roleTemplate:add']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 新增 / 编辑（身份字段 + 状态 + 备注 + 配方） -->
    <el-dialog :title="title" :visible.sync="open" width="820px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="版本档位" prop="versionType">
          <el-select v-model="form.versionType" placeholder="请选择版本档位" :disabled="form.id != null" style="width:100%;">
            <el-option v-for="item in versionOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
          <div v-if="form.id != null" class="tip">身份字段不可改（后端 update 强制置空 version_type / role_code），需变更请删除后重建。</div>
        </el-form-item>
        <el-form-item label="标准角色" prop="roleCode">
          <el-select v-model="form.roleCode" placeholder="请选择标准角色" :disabled="form.id != null" filterable allow-create default-first-option style="width:100%;">
            <el-option v-for="item in roleOptions" :key="item.roleCode" :label="item.roleName + '（' + item.roleCode + '）'" :value="item.roleCode" />
          </el-select>
          <div class="tip">取自 sys_role.role_key 标准角色集，全版本共用；小微版 tenant_type_scope 硬隔离为 boss/salesman/customer。</div>
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="0">启用</el-radio>
            <el-radio label="1">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="菜单范围" prop="menuKeys">
          <el-select
            v-model="form.menuList"
            multiple
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入 perms（如 micro:booking:list）"
            style="width:100%;"
          >
            <el-option-group v-for="group in permsGroups" :key="group.group" :label="group.group">
              <el-option v-for="opt in group.options" :key="opt.value" :label="opt.label ? opt.value + ' | ' + opt.label : opt.value" :value="opt.value" />
            </el-option-group>
          </el-select>
          <div class="tip">menu_keys = 该档位该角色解锁的 perms 数组（JSON）。选项动态取自 sys_menu.perms，取不到时回退内置清单；支持直接输入自定义 perms。</div>
        </el-form-item>
        <el-form-item label="区块天花板" prop="blockKeys">
          <el-select v-model="form.blockList" multiple filterable allow-create default-first-option placeholder="选择或输入 block_key" style="width:100%;">
            <el-option v-for="item in blockCatalog" :key="item.key" :label="item.key + ' | ' + item.name" :value="item.key" />
          </el-select>
          <div class="tip">block_keys = 页面装修可选区块的上限集合。留空数组表示不设天花板（filterByCeiling 向后兼容：天花板为空则不过滤）。</div>
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" maxlength="500" show-word-limit placeholder="请输入备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 绑定菜单（仅 menu_keys / block_keys，perms system:roleTemplate:bindMenu） -->
    <el-dialog :title="'绑定菜单范围 - ' + bindTitle" :visible.sync="bindOpen" width="760px" append-to-body>
      <el-form ref="bindForm" :model="bindForm" label-width="110px">
        <el-form-item label="菜单范围">
          <el-select v-model="bindForm.menuList" multiple filterable allow-create default-first-option placeholder="选择或输入 perms" style="width:100%;">
            <el-option-group v-for="group in permsGroups" :key="group.group" :label="group.group">
              <el-option v-for="opt in group.options" :key="opt.value" :label="opt.label ? opt.value + ' | ' + opt.label : opt.value" :value="opt.value" />
            </el-option-group>
          </el-select>
          <div class="tip">已选 {{ bindForm.menuList.length }} 项。升级档位即在此扩围，用户下次登录（刷新菜单缓存）后生效。</div>
        </el-form-item>
        <el-form-item label="区块天花板">
          <el-select v-model="bindForm.blockList" multiple filterable allow-create default-first-option placeholder="选择或输入 block_key" style="width:100%;">
            <el-option v-for="item in blockCatalog" :key="item.key" :label="item.key + ' | ' + item.name" :value="item.key" />
          </el-select>
          <div class="tip">已选 {{ bindForm.blockList.length }} 项。</div>
        </el-form-item>
      </el-form>
      <el-alert
        title="清空说明"
        type="info"
        description="后端 bindMenu 走 MyBatis-Plus updateById（默认跳过 null 字段），因此本页把空集合统一提交为 [] 而非 null，确保「取消天花板 / 收回全部菜单」能真正落库。"
        show-icon
        :closable="false"
      />
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitBind">确 定</el-button>
        <el-button @click="bindOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 配方详情（只读） -->
    <el-dialog :title="'配方详情 - ' + detailTitle" :visible.sync="detailOpen" width="760px" append-to-body>
      <el-descriptions :column="2" border size="small" class="mb8">
        <el-descriptions-item label="版本档位">{{ versionLabel(detail.versionType) }}</el-descriptions-item>
        <el-descriptions-item label="标准角色">{{ roleLabel(detail.roleCode) }}（{{ detail.roleCode }}）</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === '0' ? 'success' : 'info'" size="mini">{{ detail.status === '0' ? '启用' : '停用' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ detail.updateTime || '—' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ detail.remark || '—' }}</el-descriptions-item>
      </el-descriptions>
      <el-form label-width="110px" size="small">
        <el-form-item :label="'菜单范围(' + detailMenuKeys.length + ')'">
          <div class="tag-box">
            <el-tag v-for="k in detailMenuKeys" :key="k" size="mini" class="key-tag">{{ k }}</el-tag>
            <span v-if="!detailMenuKeys.length" class="empty-text">未配置菜单范围</span>
          </div>
        </el-form-item>
        <el-form-item :label="'区块天花板(' + detailBlockKeys.length + ')'">
          <div class="tag-box">
            <el-tag v-for="k in detailBlockKeys" :key="k" size="mini" type="success" class="key-tag">{{ k }}</el-tag>
            <span v-if="!detailBlockKeys.length" class="empty-text">未配置（表示不设天花板限制，页面装修可自由排版）</span>
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="gotoPageDecorate">前往页面装修</el-button>
        <el-button @click="detailOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listRoleTemplate, getRoleTemplate, addRoleTemplate, updateRoleTemplate, bindTemplateMenu, delRoleTemplate } from '@/api/micro/roleTemplate'
import { listStandardRoles } from '@/api/micro/tenantRole'
import { listVersionOptions } from '@/api/system/edition'
import { listMenu } from '@/api/system/menu'

/** 版本档位标签兜底（正常由 /system/edition/version-options 动态下发） */
const FALLBACK_VERSIONS = [
  { value: 'micro_lite', label: '小微版·推广档(免费)' },
  { value: 'micro_pro', label: '小微版·经营档(收费)' },
  { value: 'basic', label: '企业版·基础版' },
  { value: 'advanced', label: '企业版·高级版' },
  { value: 'pro', label: '企业版·专业版' }
]

/** 标准角色兜底（取不到 /micro/tenantRole/standardRoles 时使用，如当前身份无 micro:tenantRole:list） */
const FALLBACK_ROLES = [
  { roleCode: 'boss', roleName: '老板' },
  { roleCode: 'salesman', roleName: '业务员' },
  { roleCode: 'customer', roleName: '顾客' },
  { roleCode: 'project_manager', roleName: '项目经理' },
  { roleCode: 'expense_staff', roleName: '报销人' },
  { roleCode: 'finance', roleName: '财务' }
]

/** perms 兜底清单（取自 M15_06 菜单种子 + M15_10 分档配方） */
const FALLBACK_PERMS = [
  'micro:dashboard:view', 'micro:dashboard:today',
  'micro:booking:list', 'micro:booking:add', 'micro:booking:edit', 'micro:booking:cancel', 'micro:booking:complete', 'micro:order:detail',
  'micro:settle:list', 'micro:settle:create', 'micro:settle:generate', 'micro:settle:markPaid', 'micro:settle:detail', 'micro:settle:history',
  'micro:income:list', 'micro:income:add', 'micro:expense:list', 'micro:expense:add', 'micro:budgetAlert:edit',
  'micro:report:weekly', 'micro:report:monthly', 'micro:report:profit', 'micro:report:export', 'micro:report:cashFlow',
  'micro:promo:list', 'micro:promo:add', 'micro:promo:edit', 'micro:promo:remove', 'micro:promo:poster',
  'micro:category:list', 'micro:category:add', 'micro:category:edit', 'micro:category:remove',
  'micro:service:list', 'micro:service:add', 'micro:service:edit', 'micro:service:editConfirm', 'micro:service:toggle', 'micro:service:remove',
  'micro:priceLog:list', 'micro:shop:edit', 'micro:shop:hours', 'micro:shop:payInfo', 'micro:notify:list', 'micro:notify:config',
  'micro:staff:schedule', 'micro:staff:scheduleDetail', 'micro:staff:income', 'micro:staff:incomeWeek', 'micro:staff:incomeMonth',
  'micro:staff:incomeDetail', 'micro:staff:checkin', 'micro:staff:checkinSubmit',
  'micro:customer:shop', 'micro:customer:book', 'micro:customer:selectService', 'micro:customer:selectTime', 'micro:customer:submitBook',
  'micro:customer:myBook', 'micro:customer:cancelBook', 'micro:customer:payInfo',
  'micro:customer:list', 'micro:customer:unblacklist', 'micro:customer:creditDetail',
  'micro:activity:browse'
]

export default {
  name: 'MicroRoleTemplate',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      templateList: [],
      ids: [],
      multiple: true,
      versionOptions: [],
      roleOptions: [],
      permsGroups: [],
      // T15.16 区块组件库（与「页面装修」页保持同一份目录）
      blockCatalog: [
        { key: 'revenue_week', name: '本周收入概览' },
        { key: 'revenue_received', name: '已结算收入' },
        { key: 'revenue_pending', name: '待结算收入' },
        { key: 'staff_photo_list', name: '员工照片墙' },
        { key: 'activity_banner', name: '活动 Banner' },
        { key: 'quick_booking', name: '快速预约入口' },
        { key: 'my_schedule', name: '我的排班' },
        { key: 'checkin_entry', name: '打卡入口' },
        { key: 'shop_info', name: '店铺信息' },
        { key: 'notice_bar', name: '通知栏' }
      ],
      queryParams: { pageNum: 1, pageSize: 10, versionType: null, roleCode: null, status: null },
      // 新增/编辑
      open: false,
      title: '',
      form: {},
      rules: {
        versionType: [{ required: true, message: '版本档位不能为空', trigger: 'change' }],
        roleCode: [{ required: true, message: '标准角色编码不能为空', trigger: 'change' }]
      },
      // 绑定菜单
      bindOpen: false,
      bindTitle: '',
      bindForm: { id: null, menuList: [], blockList: [] },
      // 配方详情
      detailOpen: false,
      detailTitle: '',
      detail: {},
      detailMenuKeys: [],
      detailBlockKeys: []
    }
  },
  created() {
    this.loadVersionOptions()
    this.loadRoleOptions()
    this.loadPermsOptions()
    // 从「版本档位管理」页跳转过来时按档位预过滤
    const versionType = this.$route.query.versionType
    if (versionType) {
      this.queryParams.versionType = versionType
    }
    this.getList()
  },
  methods: {
    getList() {
      this.loading = true
      listRoleTemplate(this.queryParams).then(response => {
        this.templateList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    /** 版本档位选项：小微版两档 + 企业版三档合并为五档 */
    loadVersionOptions() {
      Promise.all([listVersionOptions('micro'), listVersionOptions('enterprise')]).then(results => {
        const merged = (results[0].data || []).concat(results[1].data || [])
        this.versionOptions = merged.length ? merged : FALLBACK_VERSIONS
      }).catch(() => { this.versionOptions = FALLBACK_VERSIONS })
    },
    /** 标准角色选项：/micro/tenantRole/standardRoles（admin 无租户上下文时返回全量标准角色集） */
    loadRoleOptions() {
      listStandardRoles().then(response => {
        const rows = (response.data || []).filter(item => item.roleCode)
        this.roleOptions = rows.length ? rows : FALLBACK_ROLES
      }).catch(() => { this.roleOptions = FALLBACK_ROLES })
    },
    /** perms 选项：动态取自 sys_menu.perms，按前两段分组；取不到时回退内置清单 */
    loadPermsOptions() {
      listMenu({}).then(response => {
        const rows = response.data || []
        const nameMap = {}
        rows.forEach(item => {
          const perms = item.perms
          if (perms && perms.indexOf(':') > 0 && !nameMap[perms]) {
            nameMap[perms] = item.menuName
          }
        })
        const keys = Object.keys(nameMap).sort()
        if (!keys.length) {
          this.permsGroups = this.buildGroups(FALLBACK_PERMS, null)
          return
        }
        this.permsGroups = this.buildGroups(keys, nameMap)
      }).catch(() => { this.permsGroups = this.buildGroups(FALLBACK_PERMS, null) })
    },
    buildGroups(keys, nameMap) {
      const groups = {}
      keys.forEach(key => {
        const parts = key.split(':')
        const groupName = parts.length > 1 ? parts[0] + ':' + parts[1] : parts[0]
        if (!groups[groupName]) {
          groups[groupName] = []
        }
        groups[groupName].push({ value: key, label: nameMap ? nameMap[key] : null })
      })
      return Object.keys(groups).map(name => ({ group: name, options: groups[name] }))
    },
    versionLabel(versionType) {
      const hit = this.versionOptions.filter(item => item.value === versionType)[0]
      return hit ? hit.label : (versionType || '—')
    },
    roleLabel(roleCode) {
      const hit = this.roleOptions.filter(item => item.roleCode === roleCode)[0]
      return hit ? hit.roleName : (roleCode || '—')
    },
    /** menu_keys / block_keys 为 JSON 数组字符串，解析失败按空数组处理 */
    parseKeys(json) {
      if (!json) {
        return []
      }
      try {
        const arr = JSON.parse(json)
        return Array.isArray(arr) ? arr : []
      } catch (err) {
        return []
      }
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
      this.form = { id: null, versionType: null, roleCode: null, status: '0', remark: null, menuList: [], blockList: [] }
      this.resetForm('form')
    },
    cancel() {
      this.open = false
      this.reset()
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增角色模板配方'
    },
    handleUpdate(row) {
      this.reset()
      getRoleTemplate(row.id).then(response => {
        const data = response.data || {}
        this.form = {
          id: data.id,
          versionType: data.versionType,
          roleCode: data.roleCode,
          status: data.status || '0',
          remark: data.remark,
          menuList: this.parseKeys(data.menuKeys),
          blockList: this.parseKeys(data.blockKeys)
        }
        this.open = true
        this.title = '编辑角色模板配方'
      })
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (!valid) {
          return
        }
        // 空集合提交为 [] 而非 null：updateById 默认跳过 null 字段，null 无法真正清空
        const payload = {
          id: this.form.id,
          versionType: this.form.versionType,
          roleCode: this.form.roleCode,
          status: this.form.status,
          remark: this.form.remark,
          menuKeys: JSON.stringify(this.form.menuList || []),
          blockKeys: JSON.stringify(this.form.blockList || [])
        }
        const isEdit = payload.id != null
        const request = isEdit ? updateRoleTemplate(payload) : addRoleTemplate(payload)
        request.then(() => {
          this.$modal.msgSuccess(isEdit ? '修改成功' : '新增成功')
          this.open = false
          this.getList()
        })
      })
    },
    handleBind(row) {
      this.bindTitle = this.versionLabel(row.versionType) + ' / ' + row.roleCode
      this.bindForm = { id: row.id, menuList: this.parseKeys(row.menuKeys), blockList: this.parseKeys(row.blockKeys) }
      this.bindOpen = true
    },
    submitBind() {
      if (!this.bindForm.id) {
        this.$modal.msgError('角色模板ID不能为空')
        return
      }
      const payload = {
        id: this.bindForm.id,
        menuKeys: JSON.stringify(this.bindForm.menuList || []),
        blockKeys: JSON.stringify(this.bindForm.blockList || [])
      }
      bindTemplateMenu(payload).then(() => {
        this.$modal.msgSuccess('绑定成功')
        this.bindOpen = false
        this.getList()
      })
    },
    handleDetail(row) {
      this.detailTitle = this.versionLabel(row.versionType) + ' / ' + row.roleCode
      this.detail = row
      this.detailMenuKeys = this.parseKeys(row.menuKeys)
      this.detailBlockKeys = this.parseKeys(row.blockKeys)
      this.detailOpen = true
    },
    handleDelete(row) {
      const targetIds = row.id ? [row.id] : this.ids
      if (!targetIds.length) {
        this.$modal.msgWarning('请先选择要删除的配方')
        return
      }
      const names = targetIds.map(id => {
        const hit = this.templateList.filter(item => item.id === id)[0]
        return hit ? hit.versionType + '/' + hit.roleCode : id
      })
      this.$modal.confirm('角色模板为物理删除（系统级表无 del_flag），确认删除配方「' + names.join('、') + '」吗？').then(() => {
        return delRoleTemplate(targetIds.join(','))
      }).then(() => {
        this.$modal.msgSuccess('删除成功')
        this.getList()
      }).catch(() => {})
    },
    /** 三层控制模型第②层：带着角色编码跳到页面装修，验证天花板是否按预期收敛排版可选区块 */
    gotoPageDecorate() {
      const roleCode = this.detail.roleCode
      this.detailOpen = false
      this.$router.push({ path: '/microConfig/pageDecorate', query: { roleCode: roleCode }})
    }
  }
}
</script>

<style scoped>
.code-tag {
  margin-left: 6px;
}
.tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}
.tag-box {
  line-height: 2;
}
.key-tag {
  margin: 0 6px 6px 0;
  font-family: Consolas, Monaco, monospace;
}
.empty-text {
  color: #909399;
  font-size: 12px;
}
</style>
