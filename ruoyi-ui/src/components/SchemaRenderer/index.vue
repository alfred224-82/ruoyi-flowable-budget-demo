<template>
  <div class="schema-renderer" :class="renderTheme ? 'theme-' + renderTheme : ''">
    <div v-if="loading" class="schema-renderer__loading">
      <i class="el-icon-loading"></i> 正在加载页面配置...
    </div>
    <div v-else-if="!renderBlocks || renderBlocks.length === 0" class="schema-renderer__empty">
      <el-empty description="当前业态包未声明可渲染区块" />
    </div>
    <div v-else>
      <div v-for="block in renderBlocks" :key="block.code" class="schema-renderer__block">
        <!-- 表单区块 -->
        <el-card v-if="block.type === 'form'" shadow="never" class="schema-renderer__card">
          <div slot="header" class="schema-renderer__header">
            <span>{{ resolveTitle(block) }}</span>
          </div>
          <el-form :model="formData" :ref="'form_' + block.code" label-width="120px" size="small">
            <el-row :gutter="16">
              <el-col
                v-for="field in block.fields || []"
                :key="field.key"
                :span="field.span || 12"
              >
                <el-form-item
                  :label="field.label || field.key"
                  :prop="field.key"
                  :required="!!field.required"
                >
                  <!-- 数字 -->
                  <el-input-number
                    v-if="field.type === 'number'"
                    v-model="formData[field.key]"
                    :placeholder="field.placeholder || ''"
                    controls-position="right"
                    style="width: 100%"
                  />
                  <!-- 日期 -->
                  <el-date-picker
                    v-else-if="field.type === 'date'"
                    v-model="formData[field.key]"
                    type="date"
                    value-format="yyyy-MM-dd"
                    :placeholder="field.placeholder || '请选择日期'"
                    style="width: 100%"
                  />
                  <!-- 下拉 -->
                  <el-select
                    v-else-if="field.type === 'select'"
                    v-model="formData[field.key]"
                    :placeholder="field.placeholder || '请选择'"
                    style="width: 100%"
                  >
                    <el-option
                      v-for="opt in field.options || []"
                      :key="opt.value"
                      :label="opt.label"
                      :value="opt.value"
                    />
                  </el-select>
                  <!-- 开关 -->
                  <el-switch
                    v-else-if="field.type === 'switch'"
                    v-model="formData[field.key]"
                  />
                  <!-- 多行文本 -->
                  <el-input
                    v-else-if="field.type === 'textarea'"
                    v-model="formData[field.key]"
                    type="textarea"
                    :rows="field.rows || 3"
                    :placeholder="field.placeholder || ''"
                  />
                  <!-- 默认单行文本（input / user / 未知类型兜底） -->
                  <el-input
                    v-else
                    v-model="formData[field.key]"
                    :placeholder="field.placeholder || ''"
                  />
                </el-form-item>
              </el-col>
            </el-row>
          </el-form>
        </el-card>

        <!-- 列表区块 -->
        <el-card v-else-if="block.type === 'list'" shadow="never" class="schema-renderer__card">
          <div slot="header" class="schema-renderer__header">
            <span>{{ resolveTitle(block) }}</span>
          </div>
          <el-table :data="block.data || []" size="small" border>
            <el-table-column
              v-for="col in block.columns || []"
              :key="col.key"
              :prop="col.key"
              :label="col.label || col.key"
              :width="col.width"
              show-overflow-tooltip
            />
          </el-table>
        </el-card>

        <!-- 看板/自定义区块：交由父组件通过具名插槽接管 -->
        <el-card v-else shadow="never" class="schema-renderer__card">
          <div slot="header" class="schema-renderer__header">
            <span>{{ resolveTitle(block) }}</span>
          </div>
          <slot :name="block.code" :block="block">
            <div class="schema-renderer__fallback">
              区块类型 <code>{{ block.type || 'unknown' }}</code> 暂无内置渲染器，可通过插槽
              <code>#{{ block.code }}</code> 自定义。
            </div>
          </slot>
        </el-card>
      </div>

      <div v-if="showActions" class="schema-renderer__actions">
        <el-button type="primary" size="small" @click="handleSubmit">提交</el-button>
        <el-button size="small" @click="handleReset">重置</el-button>
      </div>
    </div>
  </div>
</template>

<script>
import { getRenderSchema } from '@/api/system/tenantBizConfig'

/**
 * Schema 页面引擎渲染组件（T10.6 v1.29）
 *
 * 消费后端 /system/tenantBizConfig/render 下发的声明式区块（schema_blocks ∪ 租户 module_overrides），
 * 支持 form / list / 自定义（board 等）三类区块，PC 与小程序共用同一份 Schema 契约。
 *
 * 用法一（自动加载当前租户 Schema）：
 *   <schema-renderer auto-load @submit="onSubmit" />
 * 用法二（父组件传入 blocks）：
 *   <schema-renderer :blocks="myBlocks" v-model="form" />
 * 用法三（自定义区块插槽）：
 *   <schema-renderer auto-load>
 *     <template #fullhouse_board="{ block }">...</template>
 *   </schema-renderer>
 */
export default {
  name: 'SchemaRenderer',
  props: {
    // 直接传入区块列表（优先级高于 autoLoad）
    blocks: {
      type: Array,
      default: null
    },
    // 主题预设（未传入时取 Schema 下发的 theme）
    theme: {
      type: String,
      default: ''
    },
    // 是否自动从后端加载当前租户渲染 Schema
    autoLoad: {
      type: Boolean,
      default: false
    },
    // 是否展示提交/重置按钮
    showActions: {
      type: Boolean,
      default: true
    },
    // 表单数据双向绑定
    value: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      loading: false,
      innerBlocks: [],
      innerTheme: '',
      formData: Object.assign({}, this.value)
    }
  },
  computed: {
    renderBlocks() {
      return this.blocks && this.blocks.length ? this.blocks : this.innerBlocks
    },
    renderTheme() {
      return this.theme || this.innerTheme || ''
    }
  },
  watch: {
    value: {
      handler(val) {
        this.formData = Object.assign({}, val)
      },
      deep: true
    },
    formData: {
      handler(val) {
        this.$emit('input', val)
      },
      deep: true
    }
  },
  created() {
    if (this.autoLoad && !(this.blocks && this.blocks.length)) {
      this.loadSchema()
    }
  },
  methods: {
    // 计算属性名与模板绑定名统一（模板用 blocks，此处做兼容映射）
    loadSchema() {
      this.loading = true
      getRenderSchema()
        .then(res => {
          const data = res.data || {}
          this.innerBlocks = data.blocks || []
          this.innerTheme = data.theme || ''
          this.$emit('loaded', data)
        })
        .catch(() => {
          this.innerBlocks = []
        })
        .finally(() => {
          this.loading = false
        })
    },
    resolveTitle(block) {
      // 标题优先取后端已解析的 title，缺失时回落 $bizT(code) 再回落 code
      if (block.title) return block.title
      if (typeof this.$bizT === 'function') return this.$bizT(block.code, block.code)
      return block.code
    },
    handleSubmit() {
      this.$emit('submit', Object.assign({}, this.formData))
    },
    handleReset() {
      this.formData = {}
      this.$emit('reset')
    }
  }
}
</script>

<style lang="scss" scoped>
.schema-renderer {
  &__loading,
  &__empty {
    padding: 40px 0;
    text-align: center;
    color: #909399;
  }
  &__block {
    margin-bottom: 16px;
  }
  &__card {
    border: 1px solid #ebeef5;
  }
  &__header {
    font-weight: 600;
    font-size: 14px;
  }
  &__fallback {
    color: #909399;
    font-size: 13px;
    code {
      background: #f4f4f5;
      padding: 0 4px;
      border-radius: 3px;
    }
  }
  &__actions {
    text-align: center;
    padding: 12px 0;
  }
}
</style>
