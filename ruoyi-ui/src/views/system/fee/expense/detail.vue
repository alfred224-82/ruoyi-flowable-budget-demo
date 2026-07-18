<template>
  <div class="app-container">
    <!-- 报销单详情 -->
    <el-card class="box-card" v-if="bill">
      <div slot="header"><span>报销单详情</span></div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="报销单号">{{ bill.billNo }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType(bill.status)">{{ statusLabel(bill.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="报销金额">¥{{ bill.amount }}</el-descriptions-item>
        <el-descriptions-item label="费用发生日期">{{ bill.expenseDate }}</el-descriptions-item>
        <el-descriptions-item label="预算科目">{{ bill.calcBudgetSubject }}</el-descriptions-item>
        <el-descriptions-item label="责任中心">{{ bill.costCenterCode }}</el-descriptions-item>
        <el-descriptions-item label="用途标签">{{ bill.purposeTag }}</el-descriptions-item>
        <el-descriptions-item label="项目编号">{{ bill.projectCode || '-' }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ bill.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 审批操作区 -->
    <el-card class="box-card" style="margin-top: 20px;" v-if="bill && bill.status === 'SUBMITTED'">
      <div slot="header"><span>审批操作</span></div>
      <el-form :model="approvalForm" label-width="100px">
        <el-form-item label="审批意见">
          <el-input v-model="approvalForm.opinion" type="textarea" :rows="3" placeholder="选填" />
        </el-form-item>
        <el-form-item>
          <el-button type="success" @click="handleApprove" :loading="submitLoading">通过</el-button>
          <el-button type="danger" @click="showRejectDialog">驳回</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 驳回对话框 -->
    <el-dialog title="驳回原因" :visible.sync="rejectDialogVisible" width="500px">
      <el-form :model="rejectForm" :rules="rejectRules" ref="rejectForm" label-width="100px">
        <el-form-item label="驳回原因" prop="reason">
          <el-input v-model="rejectForm.reason" type="textarea" :rows="4" placeholder="请输入驳回原因（5-500字符）" />
        </el-form-item>
        <el-form-item label="审批意见">
          <el-input v-model="rejectForm.opinion" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="rejectDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleReject" :loading="submitLoading">确认驳回</el-button>
      </div>
    </el-dialog>

    <!-- 操作时间线 -->
    <el-card class="box-card" style="margin-top: 20px;">
      <div slot="header"><span>操作时间线</span></div>
      <el-timeline v-if="timeline.length > 0">
        <el-timeline-item v-for="item in timeline" :key="item.logId"
          :timestamp="item.operationTime" placement="top"
          :type="timelineType(item.operationType)">
          <el-card shadow="hover">
            <p><strong>{{ item.operationType }}</strong> - {{ item.operatorName }}（{{ item.operatorRole }}）</p>
            <p v-if="item.beforeStatus">状态变更：{{ item.beforeStatus }} → {{ item.afterStatus }}</p>
            <p v-if="item.operationDetail">{{ item.operationDetail }}</p>
          </el-card>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-else description="暂无操作记录" />
    </el-card>
  </div>
</template>

<script>
import { getExpenseBill, approveExpenseBill, rejectExpenseBill } from "@/api/system/fee/expense";
import { getOperationTimeline } from "@/api/system/fee/operationLog";

export default {
  name: "ExpenseDetail",
  data() {
    return {
      bill: null,
      timeline: [],
      approvalForm: { opinion: '' },
      rejectForm: { reason: '', opinion: '' },
      rejectDialogVisible: false,
      submitLoading: false,
      rejectRules: {
        reason: [
          { required: true, message: '驳回原因不能为空', trigger: 'blur' },
          { min: 5, max: 500, message: '驳回原因需5-500字符', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    const id = this.$route.params.id;
    if (id) {
      this.loadBill(id);
      this.loadTimeline();
    }
  },
  methods: {
    async loadBill(id) {
      const res = await getExpenseBill(id);
      this.bill = res.data;
    },
    async loadTimeline() {
      if (!this.bill) return;
      const res = await getOperationTimeline(this.bill.billNo);
      this.timeline = res.data || [];
    },
    async handleApprove() {
      this.submitLoading = true;
      try {
        await approveExpenseBill(this.bill.id, this.approvalForm.opinion);
        this.$message.success('审批通过');
        this.loadBill(this.bill.id);
        this.loadTimeline();
      } catch (e) {
        console.error(e);
      } finally {
        this.submitLoading = false;
      }
    },
    showRejectDialog() {
      this.rejectForm = { reason: '', opinion: '' };
      this.rejectDialogVisible = true;
    },
    handleReject() {
      this.$refs.rejectForm.validate(async valid => {
        if (!valid) return;
        this.submitLoading = true;
        try {
          await rejectExpenseBill(this.bill.id, this.rejectForm.reason, this.rejectForm.opinion);
          this.$message.success('已驳回');
          this.rejectDialogVisible = false;
          this.loadBill(this.bill.id);
          this.loadTimeline();
        } catch (e) {
          console.error(e);
        } finally {
          this.submitLoading = false;
        }
      });
    },
    statusLabel(status) {
      const map = { DRAFT: '草稿', SUBMITTED: '已提交', APPROVED: '已通过', REJECTED: '已驳回', OVER_BUDGET_PENDING: '超预算待审' };
      return map[status] || status;
    },
    statusTagType(status) {
      const map = { DRAFT: 'info', SUBMITTED: 'warning', APPROVED: 'success', REJECTED: 'danger', OVER_BUDGET_PENDING: 'warning' };
      return map[status] || 'info';
    },
    timelineType(type) {
      if (type.includes('APPROVE') || type === 'SUBMIT') return 'success';
      if (type.includes('REJECT')) return 'danger';
      return 'primary';
    }
  }
}
</script>
