<template>
  <div class="app-container">
    <el-card class="box-card" v-loading="loading">
      <div slot="header" class="clearfix">
        <span>超预算审批详情</span>
        <el-button style="float: right; padding: 3px 0" type="text" @click="$router.go(-1)">返回列表</el-button>
      </div>

      <!-- 审批单信息 -->
      <el-descriptions title="审批单信息" :column="2" border>
        <el-descriptions-item label="预算科目">{{ approval.budgetSubjectName }}</el-descriptions-item>
        <el-descriptions-item label="原预算金额">
          ¥{{ Number(approval.originalBudget || 0).toFixed(2) }}
        </el-descriptions-item>
        <el-descriptions-item label="已使用金额">
          ¥{{ Number(approval.usedAmount || 0).toFixed(2) }}
        </el-descriptions-item>
        <el-descriptions-item label="申请金额">
          ¥{{ Number(approval.requestAmount || 0).toFixed(2) }}
        </el-descriptions-item>
        <el-descriptions-item label="缺口金额">
          <span style="color: #f56c6c; font-weight: bold;">
            ¥{{ Number(approval.shortageAmount || 0).toFixed(2) }}
          </span>
        </el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="approval.status === 'PENDING' ? 'warning' : 'success'">
            {{ approval.status === 'PENDING' ? '待处理' : '已处理' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="审批动作" v-if="approval.approvalAction">
          <el-tag v-if="approval.approvalAction === 'APPROVE'" type="success">通过</el-tag>
          <el-tag v-else-if="approval.approvalAction === 'REJECT'" type="danger">驳回</el-tag>
          <el-tag v-else-if="approval.approvalAction === 'ADJUST'" type="warning">调剂</el-tag>
          <span v-else>-</span>
        </el-descriptions-item>
      </el-descriptions>

      <!-- 关联报销单信息 -->
      <el-divider content-position="left">关联报销单</el-divider>
      <el-descriptions v-if="expenseBill.id" :column="2" border>
        <el-descriptions-item label="单据编号">{{ expenseBill.billNo }}</el-descriptions-item>
        <el-descriptions-item label="物资小类">
          {{ getDictLabel(itemCategoryOptions, expenseBill.itemCategory) }}
        </el-descriptions-item>
        <el-descriptions-item label="用途标签">{{ expenseBill.purposeTag }}</el-descriptions-item>
        <el-descriptions-item label="金额">
          ¥{{ Number(expenseBill.amount || 0).toFixed(2) }}
        </el-descriptions-item>
        <el-descriptions-item label="费用日期">{{ expenseBill.expenseDate }}</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ expenseBill.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <el-empty v-else description="无关联报销单"></el-empty>

      <!-- 审批操作区域（仅待处理状态显示） -->
      <template v-if="approval.status === 'PENDING'">
        <el-divider content-position="left">审批操作</el-divider>
        <el-form ref="approvalForm" :model="approvalForm" :rules="approvalRules" label-width="120px" style="max-width: 800px;">
          <el-form-item label="审批动作" prop="action">
            <el-radio-group v-model="approvalForm.action" @change="onActionChange">
              <el-radio label="APPROVE">通过（追加预算）</el-radio>
              <el-radio label="REJECT">驳回</el-radio>
              <el-radio label="ADJUST">调整科目预算</el-radio>
            </el-radio-group>
          </el-form-item>

          <!-- 调剂表单 -->
          <template v-if="approvalForm.action === 'ADJUST'">
            <el-form-item label="调出科目" prop="adjustFromSubject">
              <el-select v-model="approvalForm.adjustFromSubject" placeholder="请选择调出科目" style="width: 100%" filterable>
                <el-option
                  v-for="subject in availableSubjects"
                  :key="subject.subjectCode"
                  :label="subject.subjectName + ' (余额: ¥' + Number(subject.remainingBudget).toFixed(2) + ')'"
                  :value="subject.subjectCode"
                />
              </el-select>
            </el-form-item>
            <el-form-item label="调剂金额" prop="adjustAmount">
              <el-input-number
                v-model="approvalForm.adjustAmount"
                :min="0"
                :max="Number(approval.shortageAmount)"
                :precision="2"
                :step="100"
                style="width: 100%"
              />
              <div style="color: #999; font-size: 12px;">
                最大可调剂金额：¥{{ Number(approval.shortageAmount).toFixed(2) }}
              </div>
            </el-form-item>
          </template>

          <el-form-item label="审批意见" prop="opinion">
            <el-input
              v-model="approvalForm.opinion"
              type="textarea"
              :rows="4"
              placeholder="请输入审批意见（必填）"
              maxlength="500"
              show-word-limit
            />
          </el-form-item>

          <el-form-item>
            <el-button type="primary" :loading="submitLoading" @click="handleSubmit">提交审批</el-button>
            <el-button @click="$router.go(-1)">取消</el-button>
          </el-form-item>
        </el-form>
      </template>

      <!-- 已处理信息显示 -->
      <template v-else>
        <el-divider content-position="left">审批结果</el-divider>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="审批人">{{ approval.approverName }}</el-descriptions-item>
          <el-descriptions-item label="审批时间">{{ approval.approvalTime }}</el-descriptions-item>
          <el-descriptions-item label="审批意见" :span="2">{{ approval.approvalOpinion || '-' }}</el-descriptions-item>
          <el-descriptions-item label="调出科目" v-if="approval.approvalAction === 'ADJUST'">
            {{ approval.adjustFromSubjectName }}
          </el-descriptions-item>
          <el-descriptions-item label="调剂金额" v-if="approval.approvalAction === 'ADJUST'">
            ¥{{ Number(approval.adjustAmount || 0).toFixed(2) }}
          </el-descriptions-item>
        </el-descriptions>

        <!-- 调剂流水 -->
        <template v-if="adjustmentLogs.length > 0">
          <el-divider content-position="left">调剂流水记录</el-divider>
          <el-table :data="adjustmentLogs" border>
            <el-table-column label="调出科目" align="center" prop="fromSubject" />
            <el-table-column label="调入科目" align="center" prop="toSubject" />
            <el-table-column label="调剂金额" align="center" prop="adjustAmount">
              <template slot-scope="scope">
                ¥{{ Number(scope.row.adjustAmount).toFixed(2) }}
              </template>
            </el-table-column>
            <el-table-column label="创建时间" align="center" prop="createTime" width="180" />
          </el-table>
        </template>
      </template>
    </el-card>
  </div>
</template>

<script>
import { getApproval, processApproval, queryAdjustmentLog } from "@/api/system/fee/approval";

export default {
  name: "ApprovalDetail",
  dicts: ['fee_item_category'],
  data() {
    return {
      loading: false,
      submitLoading: false,
      approvalId: null,
      approval: {},
      expenseBill: {},
      availableSubjects: [],
      adjustmentLogs: [],
      // 物资小类字典选项
      itemCategoryOptions: [],
      approvalForm: {
        action: 'APPROVE',
        adjustFromSubject: '',
        adjustAmount: null,
        opinion: ''
      },
      approvalRules: {
        action: [
          { required: true, message: "请选择审批动作", trigger: "change" }
        ],
        opinion: [
          { required: true, message: "审批意见不能为空", trigger: "blur" }
        ]
      }
    };
  },
  created() {
    this.approvalId = this.$route.query.id;
    // 加载字典数据
    this.itemCategoryOptions = this.dict.type.fee_item_category || [];
    if (this.approvalId) {
      this.loadDetail();
    } else {
      this.$message.error('缺少审批单ID');
      this.$router.go(-1);
    }
  },
  methods: {
    // 获取字典标签
    getDictLabel(dictOptions, value) {
      if (!value || !dictOptions) return value;
      const dict = dictOptions.find(d => d.value === value);
      return dict ? dict.label : value;
    },
    loadDetail() {
      this.loading = true;
      getApproval(this.approvalId).then(response => {
        this.approval = response.data || {};
        this.expenseBill = response.data.expenseBill || {};
        this.loading = false;

        // 如果是待处理状态，加载可调出科目
        if (this.approval.status === 'PENDING') {
          this.loadAvailableSubjects();
        } else {
          // 已处理状态，加载调剂流水
          this.loadAdjustmentLogs();
        }
      }).catch(() => {
        this.loading = false;
      });
    },
    loadAvailableSubjects() {
      // TODO: 调用后端接口获取可调出科目列表
      // 这里暂时使用模拟数据，实际应调用API
      // const res = await getAvailableSubjects({ deptId: this.approval.deptId });
      // this.availableSubjects = res.data;
      console.log('加载可调出科目');
    },
    loadAdjustmentLogs() {
      queryAdjustmentLog(this.approvalId).then(response => {
        this.adjustmentLogs = response.data || [];
      });
    },
    onActionChange(action) {
      if (action === 'ADJUST') {
        this.loadAvailableSubjects();
      }
    },
    handleSubmit() {
      this.$refs["approvalForm"].validate(valid => {
        if (valid) {
          // 校验调剂参数
          if (this.approvalForm.action === 'ADJUST') {
            if (!this.approvalForm.adjustFromSubject) {
              this.$message.error('请选择调出科目');
              return;
            }
            if (!this.approvalForm.adjustAmount || this.approvalForm.adjustAmount <= 0) {
              this.$message.error('请输入有效的调剂金额');
              return;
            }
          }

          this.submitLoading = true;
          const params = {
            id: this.approvalId,
            action: this.approvalForm.action,
            opinion: this.approvalForm.opinion
          };

          if (this.approvalForm.action === 'ADJUST') {
            params.adjustFromSubject = this.approvalForm.adjustFromSubject;
            params.adjustAmount = this.approvalForm.adjustAmount;
          }

          processApproval(params).then(() => {
            this.$message.success('审批处理成功');
            this.$router.push('/system/fee/approval');
          }).catch(() => {
            this.submitLoading = false;
          });
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
.el-descriptions {
  margin-bottom: 20px;
}
</style>
