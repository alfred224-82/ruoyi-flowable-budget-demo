<template>
  <div class="app-container">
    <el-card v-if="detail">
      <div slot="header"><span>超预算审批详情</span></div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="报销单号">{{ detail.billNo }}</el-descriptions-item>
        <el-descriptions-item label="责任中心">{{ detail.costCenterGroup }}</el-descriptions-item>
        <el-descriptions-item label="预算科目">{{ detail.budgetSubject }}</el-descriptions-item>
        <el-descriptions-item label="原预算">¥{{ detail.originalBudget }}</el-descriptions-item>
        <el-descriptions-item label="已使用">¥{{ detail.usedAmount }}</el-descriptions-item>
        <el-descriptions-item label="申请金额">¥{{ detail.requestAmount }}</el-descriptions-item>
        <el-descriptions-item label="缺口金额">¥{{ detail.shortageAmount }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.status === 'PENDING' ? 'warning' : detail.status === 'APPROVED' ? 'success' : 'danger'">{{ detail.status }}</el-tag>
        </el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card style="margin-top: 20px;" v-if="detail && detail.status === 'PENDING'">
      <div slot="header"><span>审批操作</span></div>
      <el-form label-width="100px">
        <el-form-item label="审批意见"><el-input v-model="opinion" type="textarea" :rows="2" /></el-form-item>
        <el-form-item>
          <el-button type="success" @click="handleAction('APPROVE')">通过</el-button>
          <el-button type="danger" @click="handleAction('REJECT')">驳回</el-button>
          <el-button type="warning" @click="adjustDialogVisible = true">调整科目预算</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-dialog title="调整科目预算" :visible.sync="adjustDialogVisible" width="500px">
      <el-form :model="adjustForm" label-width="100px">
        <el-form-item label="调出科目"><el-input v-model="adjustForm.adjustFromSubject" placeholder="科目编码" /></el-form-item>
        <el-form-item label="调剂金额"><el-input-number v-model="adjustForm.adjustAmount" :min="0" :precision="2" /></el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="adjustDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleAction('ADJUST')">确认调剂</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getOverBudget, processOverBudget } from "@/api/system/fee/overBudget";
export default {
  name: "OverBudgetDetail",
  data() {
    return { detail: null, opinion: '', adjustDialogVisible: false, adjustForm: { adjustFromSubject: '', adjustAmount: 0 } }
  },
  created() {
    const id = this.$route.params.id;
    if (id) this.loadDetail(id);
  },
  methods: {
    async loadDetail(id) {
      const res = await getOverBudget(id);
      this.detail = res.data;
    },
    async handleAction(action) {
      try {
        await processOverBudget({
          id: this.detail.id, action,
          adjustFromSubject: this.adjustForm.adjustFromSubject,
          adjustAmount: this.adjustForm.adjustAmount,
          opinion: this.opinion
        });
        this.$message.success('操作成功');
        this.adjustDialogVisible = false;
        this.loadDetail(this.detail.id);
      } catch (e) { console.error(e) }
    }
  }
}
</script>
