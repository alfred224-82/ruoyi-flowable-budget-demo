<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="单据编号" prop="billNo">
        <el-input
          v-model="queryParams.billNo"
          placeholder="请输入单据编号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item v-if="canEvidenceChain" label="证据等级">
        <el-select v-model="queryParams.evidenceLevel" placeholder="全部" clearable style="width: 130px;">
          <el-option label="强（strong）" value="strong" />
          <el-option label="中（medium）" value="medium" />
          <el-option label="弱（weak）" value="weak" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row v-if="canEvidenceChain" :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="success" plain icon="el-icon-check" size="mini" :disabled="!selectedIds.length"
                   @click="handleBatchApprove" v-hasPermi="['fee:expense:batchApprove']">批量通过</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button type="danger" plain icon="el-icon-close" size="mini" :disabled="!selectedIds.length"
                   @click="handleBatchReject" v-hasPermi="['fee:expense:batchReject']">批量驳回</el-button>
      </el-col>
      <el-col :span="1.5">
        <span style="color:#909399;font-size:12px;line-height:28px;">已选 {{ selectedIds.length }} 条</span>
      </el-col>
    </el-row>

    <el-table v-loading="loading" :data="approvalList" @selection-change="handleSelectionChange">
      <el-table-column v-if="canEvidenceChain" type="selection" width="55" align="center" />
      <el-table-column label="单据编号" align="center" prop="billNo" width="260" :show-overflow-tooltip="true" />
      <el-table-column label="申请人" align="center" prop="applicantName" width="100" />
      <el-table-column label="部门名称" align="center" prop="deptName" width="130" />
      <el-table-column label="物资小类" align="center" prop="itemCategory" :show-overflow-tooltip="true" />
      <el-table-column label="用途标签" align="center" prop="purposeTagName" :show-overflow-tooltip="true" />
      <el-table-column label="报销金额" align="center" prop="amount" width="120">
        <template slot-scope="scope">
          ¥{{ Number(scope.row.amount).toFixed(2) }}
        </template>
      </el-table-column>
      <el-table-column label="费用日期" align="center" prop="expenseDate" width="120">
        <template slot-scope="scope">
          {{ parseTime(scope.row.expenseDate, '{y}-{m}-{d}') }}
        </template>
      </el-table-column>
      <el-table-column label="预算科目" align="center" prop="calcBudgetSubjectName" :show-overflow-tooltip="true" />
      <el-table-column v-if="canEvidenceChain" label="证据等级" align="center" width="90">
        <template slot-scope="scope">
          <el-tag :type="evidenceTagType(scope.row.evidenceLevel)" size="small">{{ scope.row.evidenceLevel || '-' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="提交时间" align="center" prop="createTime" width="150">
        <template slot-scope="scope">
          {{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleApproval(scope.row)"
            style="color: #E6A23C; font-weight: bold;"
          >审批</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleView(scope.row)"
          >详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total>0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 审批弹窗 -->
    <el-dialog :title="approvalDialog.title" :visible.sync="approvalDialog.visible" width="600px" append-to-body>
      <el-descriptions :column="2" border size="small" style="margin-bottom: 15px;">
        <el-descriptions-item label="单据编号">{{ approvalDialog.bill.billNo }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ approvalDialog.bill.applicantName }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ approvalDialog.bill.deptName }}</el-descriptions-item>
        <el-descriptions-item label="报销金额">
          <span style="color: #f56c6c; font-weight: bold;">¥{{ Number(approvalDialog.bill.amount).toFixed(2) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="用途标签">{{ approvalDialog.bill.purposeTagName }}</el-descriptions-item>
        <el-descriptions-item label="预算科目">{{ approvalDialog.bill.calcBudgetSubjectName }}</el-descriptions-item>
        <el-descriptions-item label="费用日期">{{ parseTime(approvalDialog.bill.expenseDate, '{y}-{m}-{d}') }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ approvalDialog.bill.remark || '-' }}</el-descriptions-item>
      </el-descriptions>

      <el-form :model="approvalForm" label-width="100px">
        <el-form-item label="审批意见">
          <el-input v-model="approvalForm.opinion" type="textarea" :rows="2" placeholder="选填" />
        </el-form-item>
      </el-form>

      <div slot="footer" class="dialog-footer">
        <el-button type="success" @click="handleApprove" :loading="submitLoading">通过</el-button>
        <el-button type="danger" @click="showRejectDialog" :loading="submitLoading">驳回</el-button>
        <el-button @click="approvalDialog.visible = false">取消</el-button>
      </div>
    </el-dialog>

    <!-- 驳回对话框 -->
    <el-dialog title="驳回原因" :visible.sync="rejectDialogVisible" width="500px" append-to-body>
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
    <!-- 批量驳回对话框 -->
    <el-dialog title="批量驳回原因" :visible.sync="batchRejectVisible" width="500px" append-to-body>
      <el-form :model="rejectForm" :rules="rejectRules" ref="batchRejectForm" label-width="100px">
        <el-form-item label="驳回原因" prop="reason">
          <el-input v-model="rejectForm.reason" type="textarea" :rows="4" placeholder="请输入驳回原因（5-500字符）" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="batchRejectVisible = false">取消</el-button>
        <el-button type="primary" @click="doBatchReject" :loading="submitLoading">确认驳回</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listPendingApproval, approveExpenseBill, rejectExpenseBill, batchApproveExpenseBill, batchRejectExpenseBill } from "@/api/system/fee/expense";
import { loadFeatures, featureVisible } from "@/utils/feature";

export default {
  name: "ExpenseApproval",
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      approvalList: [],
      // T6.7 证据链分级：开关开启后启用多选批量审核与证据筛选（FEAT_EVIDENCE_CHAIN）
      canEvidenceChain: false,
      selectedIds: [],
      batchRejectVisible: false,
      queryParams: {
        pageNum: 1,
        pageSize: 10
      },
      submitLoading: false,
      approvalDialog: {
        visible: false,
        title: '审批报销单',
        bill: {}
      },
      approvalForm: {
        opinion: ''
      },
      rejectDialogVisible: false,
      rejectForm: {
        reason: '',
        opinion: ''
      },
      rejectRules: {
        reason: [
          { required: true, message: '驳回原因不能为空', trigger: 'blur' },
          { min: 5, max: 500, message: '驳回原因需5-500字符', trigger: 'blur' }
        ]
      }
    };
  },
  created() {
    this.getList();
    loadFeatures().then(() => {
      this.canEvidenceChain = featureVisible('FEAT_EVIDENCE_CHAIN');
    });
  },
  methods: {
    evidenceTagType(level) {
      const map = { strong: 'success', medium: 'warning', weak: 'danger' };
      return map[level] || 'info';
    },
    handleSelectionChange(selection) {
      this.selectedIds = selection.map(item => item.id);
    },
    showBatchResult(result, actionText) {
      const data = result.data || {};
      const successNum = data.successNum || 0;
      const failures = data.failures || [];
      if (failures.length === 0) {
        this.$message.success('批量' + actionText + '成功，共 ' + successNum + ' 条');
      } else {
        this.$alert('成功 ' + successNum + ' 条，失败 ' + failures.length + ' 条：<br/>' + failures.join('<br/>'), '批量' + actionText + '结果', { dangerouslyUseHTMLString: true });
      }
      this.getList();
    },
    handleBatchApprove() {
      if (!this.selectedIds.length) return;
      this.$modal.confirm('确认批量通过选中的 ' + this.selectedIds.length + ' 张报销单？').then(() => {
        this.submitLoading = true;
        return batchApproveExpenseBill(this.selectedIds, null);
      }).then(res => {
        this.showBatchResult(res, '审批');
      }).catch(() => {}).finally(() => { this.submitLoading = false; });
    },
    handleBatchReject() {
      if (!this.selectedIds.length) return;
      this.rejectForm = { reason: '', opinion: '' };
      this.batchRejectVisible = true;
    },
    doBatchReject() {
      this.$refs.batchRejectForm.validate(valid => {
        if (!valid) return;
        this.submitLoading = true;
        batchRejectExpenseBill(this.selectedIds, this.rejectForm.reason, null).then(res => {
          this.batchRejectVisible = false;
          this.showBatchResult(res, '驳回');
        }).finally(() => { this.submitLoading = false; });
      });
    },
    getList() {
      this.loading = true;
      listPendingApproval(this.queryParams).then(response => {
        this.approvalList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    handleApproval(row) {
      this.approvalDialog.bill = row;
      this.approvalForm.opinion = '';
      this.approvalDialog.visible = true;
    },
    handleView(row) {
      this.$router.push({ path: '/system/fee/expense/form', query: { view: '1', id: row.id } });
    },
    handleApprove() {
      this.submitLoading = true;
      approveExpenseBill(this.approvalDialog.bill.id, this.approvalForm.opinion).then(() => {
        this.$message.success('审批通过');
        this.approvalDialog.visible = false;
        this.getList();
      }).finally(() => {
        this.submitLoading = false;
      });
    },
    showRejectDialog() {
      this.approvalDialog.visible = false;
      this.rejectForm = { reason: '', opinion: '' };
      this.rejectDialogVisible = true;
    },
    handleReject() {
      this.$refs.rejectForm.validate(valid => {
        if (!valid) return;
        this.submitLoading = true;
        rejectExpenseBill(this.approvalDialog.bill.id, this.rejectForm.reason, this.rejectForm.opinion).then(() => {
          this.$message.success('已驳回');
          this.rejectDialogVisible = false;
          this.getList();
        }).finally(() => {
          this.submitLoading = false;
        });
      });
    }
  }
};
</script>
