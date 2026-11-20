<template>
  <div class="app-container">
    <el-alert
      title="房间 / 空间管理（原子资源）"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom:12px;">
      <template slot="title">
        房间/空间支持 4 种计价模式（计时 / 套餐 / 计量 / 一口价），可直接被顾客预约选用。首次进入若列表为空，系统会按您店铺所属行业，自动从平台行业模板初始化一套参考房间（幂等仅空表补录，不覆盖您已有的数据）。您可自由增删改。
      </template>
    </el-alert>

    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="房间名称" prop="name">
        <el-input v-model="queryParams.name" placeholder="请输入房间/空间名称" clearable @keyup.enter.native="handleQuery" />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
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
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['micro:platform:room:edit']">新增房间</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="roomList">
      <el-table-column label="房间名称" align="left" prop="name" min-width="160" show-overflow-tooltip />
      <el-table-column label="可容纳" align="center" prop="capacity" width="100">
        <template slot-scope="scope">
          <span v-if="scope.row.capacity != null">{{ scope.row.capacity }}人</span>
          <span v-else>{{ scope.row.description || '—' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="价格" align="center" prop="price" width="160">
        <template slot-scope="scope">
          <span v-if="scope.row.pricingType === 'duration'" style="color:#f56c6c;font-weight:600;">￥{{ scope.row.price }}/{{ scope.row.unit || '小时' }}</span>
          <span v-else-if="scope.row.pricingType === 'package'" style="color:#f56c6c;font-weight:600;">￥{{ scope.row.price }}(套餐)</span>
          <span v-else-if="scope.row.pricingType === 'times'" style="color:#f56c6c;font-weight:600;">￥{{ scope.row.price }}/{{ scope.row.unit || '次' }}</span>
          <span v-else-if="scope.row.pricingType === 'fixed'" style="color:#f56c6c;font-weight:600;">￥{{ scope.row.price }}(一口价)</span>
          <span v-else style="color:#f56c6c;font-weight:600;">￥{{ scope.row.price }}/{{ scope.row.unit || '' }}</span>
        </template>
      </el-table-column>
      <el-table-column label="排序" align="center" prop="sortOrder" width="70" />
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === '0' ? 'success' : 'info'" size="small">{{ scope.row.status === '0' ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="220">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['micro:platform:room:edit']">编辑</el-button>
          <el-button size="mini" type="text" :icon="scope.row.status === '0' ? 'el-icon-bottom' : 'el-icon-top'" @click="handleToggle(scope.row)" v-hasPermi="['micro:platform:room:edit']">{{ scope.row.status === '0' ? '停用' : '启用' }}</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['micro:platform:room:edit']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 新增/编辑房间/空间 -->
    <el-dialog :title="title" :visible.sync="open" width="780px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="110px">
        <el-row>
          <el-col :span="12">
            <el-form-item label="房间名称" prop="name">
              <el-input v-model="form.name" placeholder="如：小房间 / 大包厢 / 团操房" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属分类" prop="categoryId">
              <el-select v-model="form.categoryId" placeholder="默认「房间/空间」" clearable style="width:100%;">
                <el-option v-for="item in categoryOptions" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- ★ 计价模式（4种） -->
        <el-form-item label="计价模式" prop="pricingType">
          <el-select v-model="form.pricingType" placeholder="请选择计价模式" style="width:100%;" @change="onPricingTypeChange">
            <el-option label="计时 — 单价 × 时长" value="duration" />
            <el-option label="套餐 — 基础价 + 超时费 + 超量费" value="package" />
            <el-option label="计量 — 单价 × 数量" value="times" />
            <el-option label="一口价 — 固定值（含约束上限）" value="fixed" />
          </el-select>
        </el-form-item>

        <!-- ① 计时 / 计量：单价 + 单位 -->
        <el-row v-if="form.pricingType === 'duration' || form.pricingType === 'times'">
          <el-col :span="12">
            <el-form-item :label="form.pricingType === 'duration' ? '单价(元)' : '单价(元)'" prop="price">
              <el-input-number v-model="form.price" :min="0" :precision="2" :step="10" controls-position="right" style="width:100%;" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item :label="form.pricingType === 'duration' ? '单位' : '计量单位'" prop="unit">
              <el-select v-model="form.unit" placeholder="请选择" style="width:100%;">
                <el-option v-for="u in unitOptions" :key="u" :label="u" :value="u" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <!-- ② 套餐：基础价 + 含时长/超时 + 含数量/超量 + 附加费说明 -->
        <template v-if="form.pricingType === 'package'">
          <el-row>
            <el-col :span="12">
              <el-form-item label="基础价(元)" prop="price">
                <el-input-number v-model="form.price" :min="0" :precision="2" :step="10" controls-position="right" style="width:100%;" />
                <div class="field-hint">套餐固定基础价，如 500</div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="含时长(小时)" prop="extraConfig.includedDuration">
                <el-input-number v-model="extraConfig.includedDuration" :min="0" :precision="1" :step="0.5" controls-position="right" style="width:100%;" />
                <div class="field-hint">基础价包含的使用时长，超出另计超时费</div>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="12">
              <el-form-item label="超时费(元/时)" prop="extraConfig.overtimePrice">
                <el-input-number v-model="extraConfig.overtimePrice" :min="0" :precision="2" :step="5" controls-position="right" style="width:100%;" />
                <div class="field-hint">超出含时长后，每小时加收的费用</div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="含数量" prop="extraConfig.includedQuantity">
                <el-input-number v-model="extraConfig.includedQuantity" :min="0" :precision="0" :step="1" controls-position="right" style="width:100%;" />
                <div class="field-hint">基础价包含的数量上限（如公里数/份数），超出另计</div>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="12">
              <el-form-item label="超量费(元/单位)" prop="extraConfig.overQuantityPrice">
                <el-input-number v-model="extraConfig.overQuantityPrice" :min="0" :precision="2" :step="5" controls-position="right" style="width:100%;" />
                <div class="field-hint">超出含数量后，每单位加收的费用</div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="附加费说明" prop="extraConfig.surchargeNote">
                <el-input v-model="extraConfig.surchargeNote" placeholder="如：含高速费/停车费，或其他实报实销说明" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <!-- ③ 一口价：固定价 + 含时长/超时 + 含里程/超里程 + 附加费说明 -->
        <template v-if="form.pricingType === 'fixed'">
          <el-row>
            <el-col :span="12">
              <el-form-item label="固定价格(元)" prop="price">
                <el-input-number v-model="form.price" :min="0" :precision="2" :step="10" controls-position="right" style="width:100%;" />
                <div class="field-hint">一口价固定金额，不随时长/里程计费</div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="含时长(小时)" prop="extraConfig.includedDuration">
                <el-input-number v-model="extraConfig.includedDuration" :min="0" :precision="1" :step="0.5" controls-position="right" style="width:100%;" />
                <div class="field-hint">一口价包含的使用时长上限，超出另计超时费</div>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="12">
              <el-form-item label="超时费(元/时)" prop="extraConfig.overtimePrice">
                <el-input-number v-model="extraConfig.overtimePrice" :min="0" :precision="2" :step="5" controls-position="right" style="width:100%;" />
                <div class="field-hint">超出含时长后，每小时加收的费用（留空=不限时）</div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="含里程(km)" prop="extraConfig.includedMileage">
                <el-input-number v-model="extraConfig.includedMileage" :min="0" :precision="0" :step="1" controls-position="right" style="width:100%;" />
                <div class="field-hint">一口价包含的里程上限，超出另计超里程费</div>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row>
            <el-col :span="12">
              <el-form-item label="超里程费(元/km)" prop="extraConfig.overMileagePrice">
                <el-input-number v-model="extraConfig.overMileagePrice" :min="0" :precision="2" :step="1" controls-position="right" style="width:100%;" />
                <div class="field-hint">超出含里程后，每公里加收的费用（留空=不限里程）</div>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="附加费说明" prop="extraConfig.surchargeNote">
                <el-input v-model="extraConfig.surchargeNote" placeholder="如：含高速费/停车费，或其他实报实销说明" />
              </el-form-item>
            </el-col>
          </el-row>
        </template>

        <el-row>
          <el-col :span="12">
            <el-form-item label="可容纳人数" prop="capacity">
              <el-input-number v-model="form.capacity" :min="1" :max="999" controls-position="right" placeholder="如：4" style="width:100%;" />
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
        </el-form-item>
        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="2" placeholder="备注" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  listPlatformRoom, getPlatformRoom, addPlatformRoom, updatePlatformRoom,
  togglePlatformRoom, delPlatformRoom, listPlatformRoomCategories
} from '@/api/micro/platformRoom'

export default {
  name: 'MicroPlatformRoom',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      roomList: [],
      categoryOptions: [],
      open: false,
      title: '',
      queryParams: { pageNum: 1, pageSize: 10, name: null, status: null },
      form: {},
      // ★ 套餐/一口价的额外参数（提交时序列化为 extraConfig JSON）
      extraConfig: {
        includedDuration: null,
        overtimePrice: null,
        includedQuantity: null,
        overQuantityPrice: null,
        includedMileage: null,
        overMileagePrice: null,
        surchargeNote: null
      },
      rules: {
        name: [{ required: true, message: '房间名称不能为空', trigger: 'blur' }],
        price: [{ required: true, message: '价格不能为空', trigger: 'blur' }],
        pricingType: [{ required: true, message: '请选择计价模式', trigger: 'change' }]
      }
    }
  },
  computed: {
    /** 根据计价模式动态生成单位选项 */
    unitOptions() {
      if (this.form.pricingType === 'duration') {
        return ['小时', '天']
      }
      if (this.form.pricingType === 'times') {
        return ['公里', '吨', '份', '次']
      }
      return []
    }
  },
  created() {
    this.getList()
    this.getCategoryOptions()
  },
  methods: {
    getList() {
      this.loading = true
      listPlatformRoom(this.queryParams).then(response => {
        this.roomList = response.rows
        this.total = response.total
        this.loading = false
        // 自愈初始化可能新建了空间分类，刷新分类下拉
        this.getCategoryOptions()
      }).catch(() => { this.loading = false })
    },
    getCategoryOptions() {
      listPlatformRoomCategories().then(response => {
        this.categoryOptions = response.data || []
      })
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    reset() {
      // 房间默认：计时模式，元/小时
      this.form = { id: null, categoryId: null, name: null, price: null, unit: '小时', pricingType: 'duration', capacity: null, description: null, sortOrder: 0, status: '0', remark: null, extraConfig: null }
      this.extraConfig = {
        includedDuration: null,
        overtimePrice: null,
        includedQuantity: null,
        overQuantityPrice: null,
        includedMileage: null,
        overMileagePrice: null,
        surchargeNote: null
      }
      this.resetForm('form')
    },
    /** 计价模式切换时重置相关字段 */
    onPricingTypeChange() {
      this.form.price = null
      this.form.unit = null
      this.extraConfig = {
        includedDuration: null,
        overtimePrice: null,
        includedQuantity: null,
        overQuantityPrice: null,
        includedMileage: null,
        overMileagePrice: null,
        surchargeNote: null
      }
      // 计时默认单位=小时，计量默认单位=公里
      if (this.form.pricingType === 'duration') this.form.unit = '小时'
      if (this.form.pricingType === 'times') this.form.unit = '公里'
    },
    cancel() {
      this.open = false
      this.reset()
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新增房间/空间'
    },
    handleUpdate(row) {
      this.reset()
      getPlatformRoom(row.id).then(response => {
        this.form = response.data
        // ★ 反序列化 extraConfig → extraConfig 对象
        if (this.form.extraConfig) {
          try {
            const cfg = typeof this.form.extraConfig === 'string' ? JSON.parse(this.form.extraConfig) : this.form.extraConfig
            Object.assign(this.extraConfig, cfg)
          } catch (e) {
            console.warn('[Room] extraConfig 解析失败:', e)
          }
        }
        this.open = true
        this.title = '编辑房间/空间'
      })
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          // ★ 序列化 extraConfig → JSON 字符串
          const hasExtra = this.form.pricingType === 'package' || this.form.pricingType === 'fixed'
          if (hasExtra) {
            const cfg = {}
            if (this.extraConfig.includedDuration != null) cfg.includedDuration = this.extraConfig.includedDuration
            if (this.extraConfig.overtimePrice != null) cfg.overtimePrice = this.extraConfig.overtimePrice
            if (this.extraConfig.includedQuantity != null) cfg.includedQuantity = this.extraConfig.includedQuantity
            if (this.extraConfig.overQuantityPrice != null) cfg.overQuantityPrice = this.extraConfig.overQuantityPrice
            if (this.extraConfig.includedMileage != null) cfg.includedMileage = this.extraConfig.includedMileage
            if (this.extraConfig.overMileagePrice != null) cfg.overMileagePrice = this.extraConfig.overMileagePrice
            if (this.extraConfig.surchargeNote) cfg.surchargeNote = this.extraConfig.surchargeNote
            this.form.extraConfig = Object.keys(cfg).length > 0 ? JSON.stringify(cfg) : null
          } else {
            this.form.extraConfig = null
          }

          if (this.form.id != null) {
            updatePlatformRoom(this.form).then(() => {
              this.$modal.msgSuccess('修改成功')
              this.open = false
              this.getList()
            })
          } else {
            addPlatformRoom(this.form).then(() => {
              this.$modal.msgSuccess('新增成功')
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleToggle(row) {
      const target = row.status === '0' ? '1' : '0'
      const text = row.status === '0' ? '停用' : '启用'
      this.$modal.confirm('确认' + text + '房间「' + row.name + '」？').then(() => {
        return togglePlatformRoom(row.id, target)
      }).then(() => {
        this.$modal.msgSuccess(text + '成功')
        this.getList()
      }).catch(() => {})
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除房间「' + row.name + '」？').then(() => {
        return delPlatformRoom(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.field-hint {
  font-size: 12px;
  color: #909399;
  line-height: 1.4;
  margin-top: 2px;
}
</style>
