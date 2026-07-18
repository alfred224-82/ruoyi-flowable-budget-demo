<template>
  <div class="app-container">
    <el-card>
      <div slot="header"><span>预算调整申请</span></div>
      <el-alert v-if="expenseInfo" type="info" :closable="false" style="margin-bottom: 20px;">
        <template slot="title">
          报销单号：{{ expenseInfo.billNo }} | 目标科目：{{ expenseInfo.targetSubject }} | 缺口金额：¥{{ expenseInfo.shortageAmount }}
        </template>
      </el-alert>

      <el-form ref="form" :model="form" :rules="rules" label-width="120px" style="max-width: 600px;">
        <el-form-item label="报销单ID" prop="billId">
          <el-input v-model="form.billId" disabled />
        </el-form-item>
        <el-form-item label="目标预算科目" prop="targetSubject">
          <el-input v-model="form.targetSubject" disabled />
        </el-form-item>
        <el-form-item label="缺口金额" prop="shortageAmount">
          <el-input-number v-model="form.shortageAmount" :min="0" :precision="2" disabled />
        </el-form-item>
        <el-form-item label="申请调整金额" prop="requestAmount">
          <el-input-number v-model="form.requestAmount" :min="0.01" :precision="2" :max="form.shortageAmount" />
        </el-form-item>
        <el-form-item label="调整原因" prop="reason">
          <el-input v-model="form.reason" type="textarea" :rows="4" placeholder="请详细说明预算调整原因（5-500字符）"
            maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">提交申请</el-button>
          <el-button @click="$router.go(-1)">返回</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { applyBudgetAdjustment } from "@/api/system/fee/budgetAdjustment";

export default {
  name: "BudgetAdjustmentApply",
  data() {
    return {
      expenseInfo: null,
      submitting: false,
      form: {
        billId: undefined,
        targetSubject: '',
        shortageAmount: 0,
        requestAmount: 0,
        reason: ''
      },
      rules: {
        billId: [{ required: true, message: '缺少报销单ID', trigger: 'change' }],
        targetSubject: [{ required: true, message: '缺少目标科目', trigger: 'change' }],
        requestAmount: [{ required: true, message: '请输入申请调整金额', trigger: 'blur' }],
        shortageAmount: [{ required: true, message: '缺少缺口金额', trigger: 'change' }],
        reason: [
          { required: true, message: '请输入调整原因', trigger: 'blur' },
          { min: 5, max: 500, message: '调整原因长度需在5-500字符之间', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    const query = this.$route.query;
    if (query.billId) {
      this.form.billId = query.billId;
      this.form.targetSubject = query.targetSubject || '';
      const shortage = parseFloat(query.shortageAmount) || 0;
      this.form.shortageAmount = shortage;
      this.form.requestAmount = shortage;
      this.expenseInfo = {
        billNo: query.billNo || query.billId,
        targetSubject: this.form.targetSubject,
        shortageAmount: shortage.toFixed(2)
      };
    }
  },
  methods: {
    handleSubmit() {
      this.$refs.form.validate(async (valid) => {
        if (!valid) return;
        this.submitting = true;
        try {
          await applyBudgetAdjustment(this.form);
          this.$message.success('预算调整申请已提交');
          this.$router.go(-1);
        } catch (e) {
          console.error(e);
        } finally {
          this.submitting = false;
        }
      });
    }
  }
}
</script>
