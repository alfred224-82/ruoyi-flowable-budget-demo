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
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="approvalList">
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
  </div>
</template>

<script>
import { listPendingApproval, approveExpenseBill, rejectExpenseBill } from "@/api/system/fee/expense";

export default {
  name: "ExpenseApproval",
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      approvalList: [],
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
  },
  methods: {
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
