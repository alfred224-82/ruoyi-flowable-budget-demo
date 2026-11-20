<template>
  <div class="app-container">
    <el-alert
      title="三层控制模型：角色模板定「权限天花板」∩ 页面装修定「排版」= 小程序最终渲染（T15.17 / T15.30）"
      type="info"
      description="本页只做个性化排版（勾选 / 拖拽排序 / 显隐 / 参数），不涉及菜单、perms 与区块权限配置。勾选了但天花板未授权或当前用户无权限的区块，小程序端一律不渲染 —— 配置永不突破权限。"
      show-icon
      :closable="false"
      class="mb8"
    />

    <el-form :inline="true" size="small" class="mb8">
      <el-form-item label="装修页面">
        <el-radio-group v-model="pageKey" @change="loadBlocks">
          <el-radio-button v-for="p in pageOptions" :key="p.value" :label="p.value">{{ p.label }}</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="isSuperAdmin" label="目标租户">
        <el-select v-model="tenantId" filterable style="width:260px;" @change="loadBlocks">
          <el-option label="系统默认模板（tenant_id=0）" :value="0" />
          <el-option
            v-for="t in tenantOptions"
            :key="t.id"
            :label="t.companyName + '（' + t.tenantCode + '）'"
            :value="t.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-check" size="mini" :loading="saving" @click="submitDecorate" v-hasPermi="['micro:page:edit']">保存装修</el-button>
        <el-button
          type="warning"
          plain
          icon="el-icon-refresh-left"
          size="mini"
          :disabled="tenantId === 0"
          @click="handleResetDefault"
          v-hasPermi="['micro:page:edit']"
        >恢复默认</el-button>
        <el-button icon="el-icon-view" size="mini" @click="handlePreview">预览我的渲染态</el-button>
      </el-form-item>
    </el-form>

    <el-tabs v-model="roleCode" type="card" @tab-click="loadBlocks">
      <el-tab-pane v-for="r in roleOptions" :key="r.value" :label="r.label" :name="r.value" />
    </el-tabs>

    <el-row :gutter="16">
      <!-- 组件库 -->
      <el-col :span="9">
        <el-card shadow="never">
          <div slot="header">
            <span style="font-weight:600;">区块组件库</span>
            <span style="color:#909399;font-size:12px;margin-left:8px;">共 {{ blockCatalog.length }} 个，只增不改</span>
          </div>
          <div v-for="item in blockCatalog" :key="item.key" class="catalog-item">
            <div class="catalog-main">
              <div class="catalog-name">{{ item.name }}</div>
              <div class="catalog-key">{{ item.key }}</div>
              <el-tag v-if="!isRoleMatch(item)" type="warning" size="mini">非本角色默认区块</el-tag>
            </div>
            <el-button
              type="text"
              size="mini"
              icon="el-icon-plus"
              :disabled="isAdded(item.key)"
              @click="addBlock(item)"
              v-hasPermi="['micro:page:edit']"
            >{{ isAdded(item.key) ? '已添加' : '添加' }}</el-button>
          </div>
        </el-card>
      </el-col>

      <!-- 当前排版 -->
      <el-col :span="15">
        <el-card shadow="never">
          <div slot="header">
            <span style="font-weight:600;">{{ roleLabel }} · {{ pageLabel }} 排版</span>
            <span style="color:#909399;font-size:12px;margin-left:8px;">拖拽手柄可排序，保存后小程序实时生效</span>
          </div>
          <div v-loading="loading">
            <el-empty v-if="layoutBlocks.length === 0" description="该角色该页面尚未配置区块，可从左侧组件库添加，或点「恢复默认」克隆系统模板" />
            <draggable v-else v-model="layoutBlocks" handle=".drag-handle" animation="200">
              <div v-for="(block, index) in layoutBlocks" :key="block.blockKey" class="layout-item">
                <i class="el-icon-rank drag-handle" title="拖拽排序" />
                <span class="layout-index">{{ index + 1 }}</span>
                <div class="layout-main">
                  <div class="layout-name">{{ blockName(block.blockKey) }}</div>
                  <div class="layout-key">{{ block.blockKey }}</div>
                </div>
                <el-tag v-if="block.blockParams" size="mini" type="info">有参数</el-tag>
                <el-switch
                  v-model="block.showFlag"
                  :active-value="1"
                  :inactive-value="0"
                  active-text="显示"
                  inactive-text="隐藏"
                />
                <el-button type="text" size="mini" icon="el-icon-top" :disabled="index === 0" @click="moveBlock(index, -1)">上移</el-button>
                <el-button type="text" size="mini" icon="el-icon-bottom" :disabled="index === layoutBlocks.length - 1" @click="moveBlock(index, 1)">下移</el-button>
                <el-button type="text" size="mini" icon="el-icon-setting" @click="openParams(block)">参数</el-button>
                <el-button type="text" size="mini" icon="el-icon-delete" style="color:#f56c6c;" @click="removeBlock(index)">移除</el-button>
              </div>
            </draggable>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 区块参数（JSON） -->
    <el-dialog :title="'区块参数 - ' + blockName(paramsBlock.blockKey)" :visible.sync="paramsOpen" width="560px" append-to-body>
      <el-form label-width="90px">
        <el-form-item label="区块标识">
          <span>{{ paramsBlock.blockKey }}</span>
        </el-form-item>
        <el-form-item label="参数 JSON">
          <el-input v-model="paramsText" type="textarea" :rows="8" placeholder='如 {"period":"week"}，留空表示无参数' />
          <div style="color:#909399;font-size:12px;line-height:1.6;">
            必须是合法 JSON 对象。可选 <code>perm</code> 键声明该区块所需权限标识（如 <code>{"perm":"micro:income:list"}</code>），
            无该权限的用户不会渲染此区块（第③层过滤）；未声明 perm 的区块对拥有本页面角色的用户一律可见。
          </div>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitParams">确 定</el-button>
        <el-button @click="paramsOpen = false">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 渲染态预览 -->
    <el-dialog title="渲染态预览（当前登录身份）" :visible.sync="previewOpen" width="620px" append-to-body>
      <el-alert
        title="该接口按当前登录人的主角色 + 本租户版本档位天花板 + perms 三重过滤，与上方所选角色无关"
        type="warning"
        :closable="false"
        show-icon
        class="mb8"
      />
      <el-table v-loading="previewLoading" :data="previewBlocks" max-height="360">
        <el-table-column label="顺序" align="center" type="index" width="70" />
        <el-table-column label="区块名称" align="left" min-width="140">
          <template slot-scope="scope">
            <span>{{ blockName(scope.row.blockKey) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="区块标识" align="center" prop="blockKey" width="160" />
        <el-table-column label="参数" align="left" prop="blockParams" min-width="140" show-overflow-tooltip>
          <template slot-scope="scope">
            <span>{{ scope.row.blockParams || '—' }}</span>
          </template>
        </el-table-column>
      </el-table>
      <div slot="footer" class="dialog-footer">
        <el-button @click="previewOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import draggable from 'vuedraggable'
import { listPageBlock, decoratePage, resetDefault, getPageConfig } from '@/api/micro/pageBlock'
import { listTenant } from '@/api/system/tenant'

export default {
  name: 'MicroPageDecorate',
  components: { draggable },
  data() {
    return {
      loading: false,
      saving: false,
      pageKey: 'home',
      roleCode: 'boss',
      // 仅超管可跨租户装修（0=系统默认模板）；非超管由后端强制本租户
      tenantId: 0,
      tenantOptions: [],
      layoutBlocks: [],
      pageOptions: [
        { value: 'home', label: '首页' },
        { value: 'mine', label: '我的' },
        { value: 'booking', label: '预约' },
        { value: 'report', label: '报表' }
      ],
      roleOptions: [
        { value: 'boss', label: '老板 boss' },
        { value: 'salesman', label: '业务员 salesman' },
        { value: 'customer', label: '顾客 customer' }
      ],
      /**
       * 区块组件库（T15.16 ②）：与小程序区块组件一一对应，只增不改。
       * roles 为该区块的默认适用角色（源自 M15_03 桌游店系统模板），
       * 仅作 UI 提示 —— 允许跨角色添加，最终是否渲染由天花板 ∩ perms 决定。
       */
      blockCatalog: [
        { key: 'revenue_week', name: '本周收入概览', roles: ['boss'], params: '{"period":"week"}' },
        { key: 'revenue_received', name: '已结算收入', roles: ['boss', 'salesman'], params: null },
        { key: 'revenue_pending', name: '待结算收入', roles: ['boss', 'salesman'], params: null },
        { key: 'staff_photo_list', name: '员工照片墙', roles: ['customer'], params: '{"role":"salesman"}' },
        { key: 'activity_banner', name: '活动 Banner', roles: ['customer'], params: null },
        { key: 'quick_booking', name: '快速预约入口', roles: ['customer'], params: null },
        { key: 'my_schedule', name: '我的排班', roles: ['salesman'], params: null },
        { key: 'checkin_entry', name: '打卡入口', roles: ['salesman'], params: null },
        { key: 'shop_info', name: '店铺信息', roles: ['boss', 'customer'], params: null },
        { key: 'notice_bar', name: '通知栏', roles: ['boss'], params: null }
      ],
      // 参数编辑
      paramsOpen: false,
      paramsText: '',
      paramsBlock: {},
      // 渲染态预览
      previewOpen: false,
      previewLoading: false,
      previewBlocks: []
    }
  },
  computed: {
    isSuperAdmin() {
      const perms = this.$store.getters.permissions || []
      return perms.indexOf('*:*:*') !== -1
    },
    roleLabel() {
      const hit = this.roleOptions.filter(item => item.value === this.roleCode)
      return hit.length ? hit[0].label : this.roleCode
    },
    pageLabel() {
      const hit = this.pageOptions.filter(item => item.value === this.pageKey)
      return hit.length ? hit[0].label : this.pageKey
    }
  },
  created() {
    if (this.isSuperAdmin) {
      this.getTenantOptions()
    }
    this.loadBlocks()
  },
  methods: {
    getTenantOptions() {
      listTenant({ pageNum: 1, pageSize: 200 }).then(response => {
        this.tenantOptions = response.rows || []
      }).catch(() => {})
    },
    blockName(key) {
      const hit = this.blockCatalog.filter(item => item.key === key)
      return hit.length ? hit[0].name : key
    },
    isRoleMatch(item) {
      return item.roles.indexOf(this.roleCode) !== -1
    },
    isAdded(key) {
      return this.layoutBlocks.some(block => block.blockKey === key)
    },
    loadBlocks() {
      this.loading = true
      // 非超管不传 tenantId：后端忽略并强制本租户（租户隔离由插件完成）
      const targetTenant = this.isSuperAdmin ? this.tenantId : null
      listPageBlock(this.roleCode, this.pageKey, targetTenant).then(response => {
        this.layoutBlocks = (response.data || []).map(item => ({
          blockKey: item.blockKey,
          showFlag: item.showFlag == null ? 1 : item.showFlag,
          blockParams: item.blockParams,
          sortOrder: item.sortOrder
        }))
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    addBlock(item) {
      if (this.isAdded(item.key)) return
      if (!this.isRoleMatch(item)) {
        this.$modal.msgWarning('「' + item.name + '」非本角色默认区块，若角色模板天花板未授权，小程序端不会渲染')
      }
      this.layoutBlocks.push({
        blockKey: item.key,
        showFlag: 1,
        blockParams: item.params,
        sortOrder: this.layoutBlocks.length + 1
      })
    },
    removeBlock(index) {
      this.layoutBlocks.splice(index, 1)
    },
    moveBlock(index, step) {
      const target = index + step
      if (target < 0 || target >= this.layoutBlocks.length) return
      const rows = this.layoutBlocks.slice()
      const tmp = rows[index]
      rows[index] = rows[target]
      rows[target] = tmp
      this.layoutBlocks = rows
    },
    openParams(block) {
      this.paramsBlock = block
      this.paramsText = block.blockParams || ''
      this.paramsOpen = true
    },
    submitParams() {
      const text = (this.paramsText || '').trim()
      if (text) {
        try {
          const parsed = JSON.parse(text)
          if (parsed === null || typeof parsed !== 'object' || Array.isArray(parsed)) {
            this.$modal.msgError('区块参数必须是 JSON 对象，如 {"period":"week"}')
            return
          }
        } catch (err) {
          this.$modal.msgError('区块参数不是合法 JSON：' + err.message)
          return
        }
      }
      this.paramsBlock.blockParams = text || null
      this.paramsOpen = false
      this.$modal.msgSuccess('参数已更新，记得点「保存装修」持久化')
    },
    /**
     * 保存装修（整页全量覆盖：后端先删该 (租户,角色,页面,来源) 旧区块再按提交顺序落 sort_order）。
     * ★sortOrder 一律按当前数组下标重排，使拖拽结果成为权威顺序。
     * ★blocks 为空清单 = 清空该页所有区块，故二次确认。
     */
    submitDecorate() {
      const tip = this.layoutBlocks.length === 0
        ? '当前排版为空，保存将【清空】「' + this.roleLabel + ' · ' + this.pageLabel + '」的全部区块，确认继续？'
        : '确认保存「' + this.roleLabel + ' · ' + this.pageLabel + '」的排版（共 ' + this.layoutBlocks.length + ' 个区块）？保存后小程序实时生效。'
      this.$modal.confirm(tip).then(() => {
        this.saving = true
        const payload = {
          roleCode: this.roleCode,
          pageKey: this.pageKey,
          moduleType: 'micro',
          blocks: this.layoutBlocks.map((block, index) => ({
            blockKey: block.blockKey,
            showFlag: block.showFlag == null ? 1 : block.showFlag,
            blockParams: block.blockParams,
            sortOrder: index + 1
          }))
        }
        if (this.isSuperAdmin) {
          payload.tenantId = this.tenantId
        }
        return decoratePage(payload)
      }).then(() => {
        this.saving = false
        this.$modal.msgSuccess('保存成功')
        this.loadBlocks()
      }).catch(() => { this.saving = false })
    },
    handleResetDefault() {
      this.$modal.confirm('确认将「' + this.roleLabel + ' · ' + this.pageLabel + '」恢复为系统默认模板（tenant_id=0）？当前租户该页的装修配置将被覆盖。')
        .then(() => resetDefault(this.roleCode, this.pageKey, this.isSuperAdmin ? this.tenantId : null))
        .then(() => {
          this.$modal.msgSuccess('已恢复默认')
          this.loadBlocks()
        })
        .catch(() => {})
    },
    handlePreview() {
      this.previewOpen = true
      this.previewLoading = true
      getPageConfig(this.pageKey).then(response => {
        this.previewBlocks = response.data || []
        this.previewLoading = false
        if (!this.previewBlocks.length) {
          this.$modal.msgWarning('渲染态为空：当前登录身份在本租户无主角色，或天花板/perms 过滤后无可渲染区块')
        }
      }).catch(() => { this.previewLoading = false })
    }
  }
}
</script>

<style scoped>
.catalog-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 10px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-bottom: 8px;
}
.catalog-main {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
}
.catalog-name {
  font-size: 13px;
  color: #303133;
  margin-right: 8px;
}
.catalog-key {
  font-size: 12px;
  color: #909399;
  margin-right: 8px;
  font-family: Consolas, Monaco, monospace;
}
.layout-item {
  display: flex;
  align-items: center;
  padding: 8px 10px;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-bottom: 8px;
  background: #fff;
}
.drag-handle {
  cursor: move;
  color: #909399;
  margin-right: 10px;
  font-size: 16px;
}
.layout-index {
  width: 22px;
  height: 22px;
  line-height: 22px;
  text-align: center;
  border-radius: 50%;
  background: #f0f2f5;
  color: #606266;
  font-size: 12px;
  margin-right: 10px;
}
.layout-main {
  flex: 1;
  min-width: 0;
}
.layout-name {
  font-size: 13px;
  color: #303133;
}
.layout-key {
  font-size: 12px;
  color: #909399;
  font-family: Consolas, Monaco, monospace;
}
.layout-item .el-button {
  margin-left: 6px;
}
.layout-item .el-tag {
  margin-right: 10px;
}
.layout-item .el-switch {
  margin: 0 10px;
}
</style>
