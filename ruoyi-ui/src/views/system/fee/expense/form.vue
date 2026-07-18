<template>
  <div class="app-container">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>{{ isViewMode ? '查看报销单' : '填写报销单' }}</span>
        <el-button style="float: right; padding: 3px 0" type="text" @click="$router.go(-1)">返回</el-button>
      </div>

      <el-form ref="form" :model="form" :rules="rules" label-width="120px" style="max-width: 800px; margin: 0 auto;">
        <el-form-item label="物资小类" prop="itemCategory">
          <el-select v-model="form.itemCategory" placeholder="请选择物资小类" style="width: 100%" :disabled="isViewMode" @change="refreshBudgetPreview">
            <el-option
              v-for="dict in itemCategoryOptions"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="用途标签" prop="purposeTag">
          <el-select v-model="form.purposeTag" placeholder="请选择用途标签" style="width: 100%" :disabled="isViewMode" @change="onPurposeTagChange">
            <el-option
              v-for="tag in purposeTags"
              :key="tag.tagCode"
              :label="tag.tagName + ' ' + tag.tagIcon"
              :value="tag.tagCode"
            />
          </el-select>
        </el-form-item>

        <el-form-item label="金额" prop="amount">
          <el-input-number
            v-model="form.amount"
            :min="0.01"
            :precision="2"
            placeholder="请输入报销金额"
            :disabled="isViewMode"
            style="width: 100%"
          />
          <div v-if="budgetPreview && budgetPreview.passed && budgetPreview.remainingBudget != null" style="color: #909399; font-size: 12px; margin-top: 4px;">
            可用余额：¥{{ Number(budgetPreview.remainingBudget).toFixed(2) }}
          </div>
        </el-form-item>


        <el-form-item label="备注" prop="remark">
          <el-input v-model="form.remark" type="textarea" :rows="3" placeholder="请输入备注" :disabled="isViewMode" />
        </el-form-item>

        <!-- 预算归属预览 -->
        <el-form-item v-if="budgetPreview" label="预算归属预览">
          <el-alert
            :title="budgetPreview.passed ? '预算归属匹配成功' : '预算归属匹配失败'"
            :type="budgetPreview.passed ? 'success' : 'error'"
            :closable="false"
            show-icon
          >
            <template v-if="budgetPreview.passed">
              <p>预算科目：{{ budgetPreview.budgetSubjectName || budgetPreview.budgetSubject }}</p>
              <p v-if="budgetPreview.remainingBudget !== null && budgetPreview.remainingBudget !== undefined">
                可用余额：¥{{ Number(budgetPreview.remainingBudget).toFixed(2) }}
              </p>
            </template>
            <template v-else>
              <p>{{ budgetPreview.message }}</p>
            </template>
          </el-alert>
        </el-form-item>

        <el-form-item>
          <el-button v-if="!isViewMode" type="primary" :loading="submitLoading" @click="handleSubmit">提交报销单</el-button>
          <el-button @click="$router.go(-1)">取消</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { addExpenseBill, submitExpense, getExpenseBill } from "@/api/system/fee/expense";
import { allEnabledPurposeTag } from "@/api/system/fee/purposeTag";
import { previewMapping } from "@/api/system/fee/mappingRule";
import { getUserProfile } from "@/api/system/user";

export default {
  name: "ExpenseForm",
  dicts: ['fee_item_category'],
  data() {
    return {
      submitLoading: false,
      purposeTags: [],
      budgetPreview: null,
      currentUser: {},
      // 物资小类字典选项
      itemCategoryOptions: [],
      form: {
        itemCategory: '',
        purposeTag: '',
        amount: undefined,
        expenseDate: new Date().toISOString().slice(0, 10),
        remark: ''
      },
      rules: {
        itemCategory: [
          { required: true, message: "物资小类不能为空", trigger: "blur" }
        ],
        purposeTag: [
          { required: true, message: "请选择用途标签", trigger: "change" }
        ],
        amount: [
          { required: true, message: "金额不能为空", trigger: "blur" },
          { type: 'number', min: 0.01, message: "金额必须大于0", trigger: "blur" }
        ],
        expenseDate: [
          { required: true, message: "请选择费用日期", trigger: "change" }
        ]
      }
    };
  },
  computed: {
    isViewMode() {
      return this.$route.query.view === '1';
    }
  },
  created() {
    this.loadPurposeTags();
    this.loadCurrentUser().then(() => {
      // 用户信息加载完成后再初始化字典
      this.itemCategoryOptions = this.dict.type.fee_item_category || [];
      // 查看模式：加载已有报销单数据
      if (this.isViewMode && this.$route.query.id) {
        this.loadExpenseBill(this.$route.query.id);
      }
    });
  },
  methods: {
    loadExpenseBill(id) {
      getExpenseBill(id).then(response => {
        const bill = response.data;
        this.form = {
          itemCategory: bill.itemCategory || '',
          purposeTag: bill.purposeTag || '',
          amount: bill.amount,
          expenseDate: bill.expenseDate,
          remark: bill.remark || ''
        };
        // 查看模式下自动刷新预算归属预览
        this.refreshBudgetPreview();
      });
    },
    loadPurposeTags() {
      allEnabledPurposeTag().then(response => {
        this.purposeTags = response.data || [];
      });
    },
    async loadCurrentUser() {
      const response = await getUserProfile();
      // getUserProfile 返回 { user, roleGroup, postGroup }，取 user 对象
      this.currentUser = (response.data && response.data.user) || {};
    },
    onPurposeTagChange() {
      this.refreshBudgetPreview();
    },
    async refreshBudgetPreview() {
      if (!this.form.itemCategory || !this.form.purposeTag) {
        this.budgetPreview = null;
        return;
      }
      // 确保用户信息已加载
      if (!this.currentUser.deptId) {
        return;
      }
      try {
        const res = await previewMapping({
          itemCategory: this.form.itemCategory,
          purposeTag: this.form.purposeTag,
          deptId: this.currentUser.deptId
        });
        this.budgetPreview = res.data;
      } catch (error) {
        console.error('预算归属预览失败:', error);
        this.budgetPreview = null;
      }
    },
    handleSubmit() {
      this.$refs["form"].validate(async valid => {
        if (valid) {
          // 前端校验
          if (this.budgetPreview && !this.budgetPreview.passed) {
            this.$message.error(this.budgetPreview.message);
            return;
          }

          this.submitLoading = true;
          try {
            // 第一步：创建报销单（草稿）
            const addRes = await addExpenseBill(this.form);
            const billId = addRes.data.id;
            // 第二步：提交报销单
            await submitExpense(billId);
            this.$message.success('提交成功');
            this.$router.push('/fee/expense');
          } catch (error) {
            console.error('提交失败:', error);
          } finally {
            this.submitLoading = false;
          }
        }
      });
    }
  }
};
</script>

<style scoped>
.box-card {
  margin: 20px;
}
</style>
