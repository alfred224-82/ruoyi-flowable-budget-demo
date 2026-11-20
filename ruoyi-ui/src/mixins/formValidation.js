/**
 * 表单校验 mixin
 * <p>
 * 对应开发规范"七、前端页面约束：防脏数据入库"。
 * 核心原则：能不让用户自由输入就不自由输入。
 * <p>
 * 使用方式：在 Vue 组件中引入 mixin，提交前调用 beforeSubmit 统一校验。
 * <pre>
 * import formValidation from '@/mixins/formValidation'
 *
 * export default {
 *   mixins: [formValidation],
 *   methods: {
 *     submitForm() {
 *       if (!this.beforeSubmit()) return
 *       // ... 提交逻辑
 *     }
 *   }
 * }
 * </pre>
 */
export default {
  methods: {
    /**
     * 提交前统一校验
     * 先执行 Element UI 的 form.validate()，再逐字段检查长度/精度
     * @returns {boolean} 校验是否通过
     */
    beforeSubmit() {
      // 1. Element UI 表单校验（必填、格式等）
      let formValid = true
      if (this.$refs.form) {
        this.$refs.form.validate(valid => {
          formValid = valid
        })
      }
      if (!formValid) {
        return false
      }
      // 2. 长度/精度校验（子类可覆盖 getLimits 提供字段限制表）
      if (typeof this.getLimits === 'function') {
        const limits = this.getLimits()
        for (const key of Object.keys(limits)) {
          const limit = limits[key]
          const value = this.form && this.form[key]
          if (value === undefined || value === null) continue

          // 字符串长度校验
          if (typeof value === 'string' && limit.maxLength) {
            if (value.length > limit.maxLength) {
              this.$message.error(`${limit.label || key} 长度不能超过 ${limit.maxLength} 个字符`)
              return false
            }
          }

          // 数字精度校验
          if (typeof value === 'number' || (typeof value === 'string' && !isNaN(Number(value)) && value !== '')) {
            const num = Number(value)
            if (limit.integerDigits !== undefined) {
              const intPart = Math.floor(Math.abs(num))
              const maxInt = Math.pow(10, limit.integerDigits) - 1
              if (intPart > maxInt) {
                this.$message.error(`${limit.label || key} 整数位不能超过 ${limit.integerDigits} 位`)
                return false
              }
            }
            if (limit.decimalDigits !== undefined) {
              const str = String(value)
              const dotIndex = str.indexOf('.')
              if (dotIndex !== -1) {
                const actualDecimals = str.length - dotIndex - 1
                if (actualDecimals > limit.decimalDigits) {
                  this.$message.error(`${limit.label || key} 小数位不能超过 ${limit.decimalDigits} 位`)
                  return false
                }
              }
            }
          }
        }
      }
      return true
    },

    /**
     * 校验单个字段长度（供模板中实时校验使用）
     * @param {string} value - 字段值
     * @param {number} maxLength - 最大长度
     * @param {string} label - 字段中文名
     * @returns {boolean} 是否通过
     */
    validateFieldLength(value, maxLength, label) {
      if (value && value.length > maxLength) {
        this.$message.error(`${label} 长度不能超过 ${maxLength} 个字符`)
        return false
      }
      return true
    },

    /**
     * 校验数字精度（供模板中实时校验使用）
     * @param {number|string} value - 数值
     * @param {number} integerDigits - 最大整数位数
     * @param {number} decimalDigits - 最大小数位数
     * @param {string} label - 字段中文名
     * @returns {boolean} 是否通过
     */
    validateNumberPrecision(value, integerDigits, decimalDigits, label) {
      if (value === '' || value === null || value === undefined) return true
      const num = Number(value)
      if (isNaN(num)) {
        this.$message.error(`${label} 必须是数字`)
        return false
      }
      const intPart = Math.floor(Math.abs(num))
      const maxInt = Math.pow(10, integerDigits) - 1
      if (intPart > maxInt) {
        this.$message.error(`${label} 整数位不能超过 ${integerDigits} 位`)
        return false
      }
      const str = String(value)
      const dotIndex = str.indexOf('.')
      if (dotIndex !== -1) {
        const actualDecimals = str.length - dotIndex - 1
        if (actualDecimals > decimalDigits) {
          this.$message.error(`${label} 小数位不能超过 ${decimalDigits} 位`)
          return false
        }
      }
      return true
    }
  }
}
