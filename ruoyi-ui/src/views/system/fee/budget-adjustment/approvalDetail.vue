<template>
  <div class="app-container">
    <el-card v-if="detail">
      <div slot="header">
        <span>预算调整申请详情</span>
        <el-tag :type="statusTagType(detail.status)" style="margin-left: 10px;">{{ statusLabel(detail.status) }}</el-tag>
        <el-button style="float: right; padding: 3px 0" type="text" @click="handleBack">
          <i class="el-icon-back"></i> 返回列表
        </el-button>
      </div>

      <!-- 申请单内容 -->
      <el-descriptions :column="2" border>
        <el-descriptions-item label="申请单号">{{ detail.appNo }}</el-descriptions-item>
        <el-descriptions-item label="申请人">{{ detail.applicantName || detail.applicantId }}</el-descriptions-item>
        <el-descriptions-item label="物资小类">{{ itemCategoryLabel || detail.itemCategory || '-' }}</el-descriptions-item>
        <el-descriptions-item label="用途标签">{{ purposeTagLabel || detail.purposeTag || '-' }}</el-descriptions-item>
        <el-descriptions-item label="目标预算科目">{{ detail.targetSubjectName || detail.targetSubject }}</el-descriptions-item>
        <el-descriptions-item label="部门名称">{{ detail.deptName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="申请金额">
          <span style="font-weight: bold; color: #E6A23C;">¥{{ fmtAmount(detail.requestAmount) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="预算可用余额">
          <span style="font-weight: bold; color: #67C23A;">¥{{ fmtAmount(detail.currentBalance) }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="资金池金额">
          <span style="font-weight: bold; color: #409EFF;">¥{{ fmtAmount(fundPool ? fundPool.totalAmount : 0) }}</span>
          <span style="margin-left: 8px; color: #909399; font-size: 12px;">（{{ fundPoolLevelLabel }}级）</span>
        </el-descriptions-item>
        <el-descriptions-item label="当前审批层级">
          <el-tag size="small" type="warning">{{ levelMap[detail.currentLevel] || detail.currentLevel }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="申请时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="申请原因" :span="2">{{ detail.reason }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.rejectReason" label="驳回原因" :span="2">
          <span style="color: #F56C6C;">{{ detail.rejectReason }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 资金池信息 -->
      <el-card v-if="fundPool" shadow="never" style="margin-top: 20px;">
        <div slot="header">
          <span style="font-weight: bold;">
            <i class="el-icon-wallet"></i> {{ fundPoolLevelLabel }}资金池
          </span>
        </div>
        <el-row :gutter="20">
          <el-col :span="8">
            <el-statistic title="资金池总额度" :value="Number(fundPool.totalAmount || 0)">
              <template slot="prefix">¥</template>
            </el-statistic>
          </el-col>
          <el-col :span="8">
            <el-statistic title="已分摊金额" :value="Number(fundPool.allocatedAmount || 0)">
              <template slot="prefix">¥</template>
            </el-statistic>
          </el-col>
          <el-col :span="8">
            <el-statistic title="剩余可用额度" :value="Number(fundPoolAvailable) ">
              <template slot="prefix">¥</template>
            </el-statistic>
          </el-col>
        </el-row>
        <el-alert
          v-if="fundPoolAvailable < actualAllocateAmount"
          :title="'资金池剩余额度（¥' + fmtAmount(fundPoolAvailable) + '）不足以覆盖分摊金额（¥' + fmtAmount(actualAllocateAmount) + '），建议向上级转审'"
          type="warning"
          :closable="false"
          style="margin-top: 15px;"
        />
      </el-card>

      <!-- 操作按钮区 -->
      <div style="margin-top: 25px; text-align: center;">
        <!-- 审批人操作（审批中的单子） -->
        <template v-if="isPending">
          <el-button type="success" icon="el-icon-check" @click="handleApprove" :loading="approving">
            {{ isBranchOrHq ? '分摊到资金池' : '通过' }}
          </el-button>
          <el-button type="danger" icon="el-icon-close" @click="openReject" :loading="rejecting">驳回</el-button>
          <el-button
            v-if="detail.currentLevel < 3"
            type="warning"
            icon="el-icon-top"
            @click="openEscalate"
            :loading="escalating"
          >向上级转审</el-button>
        </template>
        <!-- 申请人操作（审批中的单子显示取消） -->
        <el-button v-if="isApplicant && isPending" type="info" @click="handleCancel" :loading="cancelling">取消申请</el-button>
      </div>
    </el-card>

    <!-- 分摊金额对话框（分公司/总公司审批时使用） -->
    <el-dialog :title="'分摊到' + allocateTargetLabel + '资金池'" :visible.sync="allocateDialogVisible" width="520px" append-to-body>
      <el-alert
        :title="allocateHintText"
        type="info"
        :closable="false"
        show-icon
        style="margin-bottom: 15px;"
      />
      <el-form ref="allocateForm" :model="allocateForm" :rules="allocateRules" label-width="100px">
        <el-form-item label="分摊金额" prop="allocateAmount">
          <el-input-number
            v-model="allocateForm.allocateAmount"
            :min="0.01"
            :max="Number(fundPoolAvailable)"
            :precision="2"
            :step="100"
            style="width: 100%;"
          />
          <div style="color: #909399; font-size: 12px; margin-top: 4px;">
            当前资金池可用额度：¥{{ fmtAmount(fundPoolAvailable) }}
          </div>
        </el-form-item>
        <el-form-item label="审批意见" prop="opinion">
          <el-input v-model="allocateForm.opinion" type="textarea" :rows="3" placeholder="请输入审批意见" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="allocateDialogVisible = false">取 消</el-button>
        <el-button type="success" @click="submitAllocate" :loading="approving">确认分摊</el-button>
      </div>
    </el-dialog>

    <!-- 驳回对话框 -->
    <el-dialog title="驳回申请" :visible.sync="rejectDialogVisible" width="480px" append-to-body>
      <el-form ref="rejectForm" :model="rejectForm" :rules="rejectRules" label-width="80px">
        <el-form-item label="驳回原因" prop="reason">
          <el-input v-model="rejectForm.reason" type="textarea" :rows="4" placeholder="请填写驳回原因（必填）" maxlength="500" show-word-limit />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="rejectDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitReject" :loading="rejecting">确认驳回</el-button>
      </div>
    </el-dialog>

    <!-- 转审对话框 -->
    <el-dialog title="向上级转审" :visible.sync="escalateDialogVisible" width="480px" append-to-body>
      <el-form ref="escalateForm" :model="escalateForm" :rules="escalateRules" label-width="80px">
        <el-form-item label="转审原因" prop="reason">
          <el-input v-model="escalateForm.reason" type="textarea" :rows="4" placeholder="请填写转审原因（如：资金池额度不足）" maxlength="500" show-word-limit />
        </el-form-item>
        <el-form-item>
          <span style="color: #909399;">
            转审后将流转至 <strong>{{ nextLevelLabel }}</strong> 审批
          </span>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="escalateDialogVisible = false">取 消</el-button>
        <el-button type="warning" @click="submitEscalate" :loading="escalating">确认转审</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import {
  getBudgetAdjustment,
  cancelBudgetAdjustment,
  approveBudgetAdjustment,
  rejectBudgetAdjustment,
  escalateBudgetAdjustment,
  getMyFundPool
} from "@/api/system/fee/budgetAdjustment";
import { allEnabledPurposeTag } from "@/api/system/fee/purposeTag";

export default {
  name: "BudgetAdjustmentApprovalDetail",
  dicts: ['fee_item_category'],
  data() {
    return {
      detail: null,
      fundPool: null,
      purposeTags: [],
      cancelling: false,
      approving: false,
      rejecting: false,
      escalating: false,
      rejectDialogVisible: false,
      escalateDialogVisible: false,
      rejectForm: { reason: '' },
      escalateForm: { reason: '' },
      rejectRules: {
        reason: [{ required: true, message: '请填写驳回原因', trigger: 'blur' }]
      },
      escalateRules: {
        reason: [{ required: true, message: '请填写转审原因', trigger: 'blur' }]
      },
      levelMap: { 1: '部门', 2: '分公司', 3: '总公司' },
      allocateDialogVisible: false,
      allocateForm: { allocateAmount: null, opinion: '' },
      allocateRules: {
        allocateAmount: [{ required: true, message: '请输入分摊金额', trigger: 'blur' }],
        opinion: [{ required: true, message: '请输入审批意见', trigger: 'blur' }]
      }
    }
  },
  computed: {
    isPending() {
      if (!this.detail) return false;
      return ['DEPT_APPROVING', 'BRANCH_APPROVING', 'HQ_APPROVING'].includes(this.detail.status);
    },
    isBranchOrHq() {
      if (!this.detail) return false;
      return this.detail.currentLevel === 2 || this.detail.currentLevel === 3;
    },
    allocateTargetLabel() {
      if (!this.detail) return '';
      return this.detail.currentLevel === 2 ? '部门' : '分公司';
    },
    allocateHintText() {
      if (!this.detail) return '';
      if (this.detail.currentLevel === 2) {
        return '分摊金额将加到申请部门（' + (this.detail.deptName || '') + '）的部门资金池中，部门经理可继续给科目增加预算。';
      }
      return '分摊金额将加到申请分公司的分公司资金池中。';
    },
    isApplicant() {
      // 如果当前用户是申请人，显示取消按钮
      if (!this.detail) return false;
      // 通过检查是否有applicantId来判断（简化处理：非申请人才能审批）
      return false; // 后端会校验权限，这里默认都显示审批按钮
    },
    fundPoolAvailable() {
      if (!this.fundPool) return 0;
      return Number((this.fundPool.totalAmount || 0) - (this.fundPool.allocatedAmount || 0)).toFixed(2);
    },
    actualAllocateAmount() {
      if (!this.detail) return 0;
      const req = Number(this.detail.requestAmount || 0);
      // 分公司/总公司审批：分摊金额 = 申请金额
      if (this.isBranchOrHq) return req;
      // 部门审批：实际分摊金额 = 申请金额 - 预算可用余额（差额）
      const bal = Number(this.detail.currentBalance || 0);
      return Math.max(req - bal, 0);
    },
    fundPoolLevelLabel() {
      if (this.fundPool && this.fundPool.levelType) {
        const map = { 'DEPT': '部门', 'BRANCH': '分公司', 'HQ': '总公司' };
        return map[this.fundPool.levelType] || this.fundPool.levelType;
      }
      if (!this.detail) return '';
      return this.levelMap[this.detail.currentLevel] || '';
    },
    nextLevelLabel() {
      if (!this.detail) return '';
      const next = (this.detail.currentLevel || 0) + 1;
      return this.levelMap[next] || '上级';
    },
    itemCategoryLabel() {
      if (!this.detail || !this.detail.itemCategory) return '';
      const dict = (this.dict && this.dict.type && this.dict.type.fee_item_category) || [];
      const found = dict.find(d => d.value === this.detail.itemCategory);
      return found ? found.label : '';
    },
    purposeTagLabel() {
      if (!this.detail || !this.detail.purposeTag) return '';
      const found = this.purposeTags.find(t => t.tagCode === this.detail.purposeTag);
      return found ? found.tagName : '';
    }
  },
  created() {
    const id = this.$route.params.id;
    if (id) {
      this.loadDetail(id);
      this.loadFundPool();
    }
    this.loadPurposeTags();
  },
  methods: {
    fmtAmount(val) {
      return Number(val || 0).toFixed(2);
    },
    async loadDetail(id) {
      try {
        const res = await getBudgetAdjustment(id);
        this.detail = res.data;
      } catch (e) { console.error(e) }
    },
    async loadFundPool() {
      try {
        const res = await getMyFundPool();
        this.fundPool = res.data;
      } catch (e) {
        console.error('加载资金池信息失败', e);
      }
    },
    loadPurposeTags() {
      allEnabledPurposeTag().then(response => {
        this.purposeTags = response.data || [];
      });
    },
    statusTagType(status) {
      const map = { 'DEPT_APPROVING': 'warning', 'BRANCH_APPROVING': 'warning', 'HQ_APPROVING': 'warning', 'APPROVED': 'success', 'REJECTED': 'danger', 'ESCALATED': 'info', 'CANCELLED': 'info' };
      return map[status] || 'info';
    },
    statusLabel(status) {
      const map = { 'DEPT_APPROVING': '部门审批中', 'BRANCH_APPROVING': '分公司审批中', 'HQ_APPROVING': '总公司审批中', 'APPROVED': '已通过', 'REJECTED': '已驳回', 'ESCALATED': '已转审', 'CANCELLED': '已撤回' };
      return map[status] || status;
    },
    handleBack() {
      this.$router.go(-1);
    },
    // === 通过 ===
    handleApprove() {
      if (this.isBranchOrHq) {
        // 分公司/总公司审批：打开分摊金额对话框
        this.allocateForm.allocateAmount = Number(this.detail.requestAmount || 0);
        this.allocateForm.opinion = '同意分摊';
        this.allocateDialogVisible = true;
        return;
      }
      // 部门级审批：按差额计算，直接确认
      const currentBalance = Number(this.detail.currentBalance || 0);
      const requestAmount = Number(this.detail.requestAmount || 0);
      const allocateAmount = Math.max(requestAmount - currentBalance, 0);
      const msg = `确认通过该预算调整申请？\n\n` +
        `• 申请金额：¥${this.fmtAmount(requestAmount)}\n` +
        `• 当前科目余额：¥${this.fmtAmount(currentBalance)}\n` +
        `• 实际从资金池分摊：¥${this.fmtAmount(allocateAmount)}（差额）\n` +
        `• 通过后科目余额将变为：¥${this.fmtAmount(requestAmount)}`;
      this.$confirm(msg, '确认通过', {
        confirmButtonText: '确认通过',
        cancelButtonText: '取消',
        type: 'info',
        dangerouslyUseHTMLString: false
      }).then(async () => {
        this.approving = true;
        try {
          await approveBudgetAdjustment(this.detail.id, '同意分摊');
          this.$message.success('审批通过！科目预算余额已调整为 ¥' + this.fmtAmount(requestAmount) + '，资金池分摊 ¥' + this.fmtAmount(allocateAmount));
          this.loadDetail(this.detail.id);
          this.loadFundPool();
        } catch (e) {
          console.error(e);
        } finally { this.approving = false }
      }).catch(() => {});
    },
    // === 分摊到资金池（分公司/总公司审批） ===
    submitAllocate() {
      this.$refs.allocateForm.validate(async valid => {
        if (valid) {
          this.approving = true;
          try {
            await approveBudgetAdjustment(this.detail.id, this.allocateForm.opinion, this.allocateForm.allocateAmount);
            this.$message.success('审批通过！已分摊 ¥' + this.fmtAmount(this.allocateForm.allocateAmount) + ' 到' + this.allocateTargetLabel + '资金池');
            this.allocateDialogVisible = false;
            this.loadDetail(this.detail.id);
            this.loadFundPool();
          } catch (e) {
            console.error(e);
          } finally { this.approving = false }
        }
      });
    },
    // === 驳回 ===
    openReject() {
      this.rejectForm.reason = '';
      this.rejectDialogVisible = true;
    },
    submitReject() {
      this.$refs.rejectForm.validate(async valid => {
        if (valid) {
          this.rejecting = true;
          try {
            await rejectBudgetAdjustment(this.detail.id, this.rejectForm.reason);
            this.$message.success('已驳回');
            this.rejectDialogVisible = false;
            this.loadDetail(this.detail.id);
          } catch (e) {
            console.error(e);
          } finally { this.rejecting = false }
        }
      });
    },
    // === 转审 ===
    openEscalate() {
      this.escalateForm.reason = '';
      this.escalateDialogVisible = true;
    },
    submitEscalate() {
      this.$refs.escalateForm.validate(async valid => {
        if (valid) {
          this.escalating = true;
          try {
            await escalateBudgetAdjustment(this.detail.id, this.escalateForm.reason);
            this.$message.success('已转审至' + this.nextLevelLabel);
            this.escalateDialogVisible = false;
            this.loadDetail(this.detail.id);
          } catch (e) {
            console.error(e);
          } finally { this.escalating = false }
        }
      });
    },
    // === 取消申请 ===
    handleCancel() {
      this.$confirm('确认撤回该预算调整申请？撤回后申请将作废。', '提示', {
        confirmButtonText: '确认撤回',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(async () => {
        this.cancelling = true;
        try {
          await cancelBudgetAdjustment(this.detail.id);
          this.$message.success('申请已撤回');
          this.loadDetail(this.detail.id);
        } catch (e) {
          console.error(e);
        } finally { this.cancelling = false }
      }).catch(() => {});
    }
  }
}
</script>

<style scoped>
.el-statistic {
  text-align: center;
}
</style>
