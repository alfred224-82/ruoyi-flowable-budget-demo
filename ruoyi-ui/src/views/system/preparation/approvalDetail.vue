<template>
  <div class="app-container">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>预算审核详情</span>
        <el-button style="float: right; padding: 3px 0" type="text" @click="handleReturn">
          <i class="el-icon-back"></i> 返回查询页面
        </el-button>
      </div>

      <!-- 基础信息 -->
      <el-descriptions :column="3" border class="mb20">
        <el-descriptions-item label="预算单号">{{ preparationData.sheetNo }}</el-descriptions-item>
        <el-descriptions-item label="预算期间">{{ preparationData.budgetPeriod }}</el-descriptions-item>
        <el-descriptions-item label="组织名称">{{ preparationData.orgName }}</el-descriptions-item>
        <el-descriptions-item label="编制人">{{ preparationData.createBy }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ parseTime(preparationData.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}
        </el-descriptions-item>
        <el-descriptions-item label="预算总额">
          <span style="color: #409EFF; font-weight: bold;">{{ preparationData.totalBudget ? preparationData.totalBudget.toFixed(2) : '0.00' }} 元</span>
        </el-descriptions-item>
        <el-descriptions-item label="备注" :span="3">{{ preparationData.remark || '无' }}</el-descriptions-item>
        <el-descriptions-item label="截止时间" v-if="preparationData.deadlineTime">
          <budget-countdown :deadline="preparationData.deadlineTime" :status="preparationData.status" />
        </el-descriptions-item>
      </el-descriptions>

      <!-- 驳回历史 -->
      <el-card v-if="preparationData.id" shadow="never" class="mb20">
        <div slot="header">
          <span style="font-weight: bold;">驳回历史</span>
        </div>
        <budget-reject-timeline :sheet-id="preparationData.id" />
      </el-card>

      <!-- 校验结果 -->
      <el-alert
        v-if="validationResults.length > 0"
        title="校验警告"
        type="warning"
        :closable="false"
        style="margin-bottom: 20px;"
      >
        <div style="max-height: 200px; overflow-y: auto;">
          <el-table :data="validationResults" border size="small">
            <el-table-column label="规则名称" prop="ruleName" width="150" />
            <el-table-column label="风险等级" width="100">
              <template slot-scope="scope">
                <el-tag :type="getRiskLevelType(scope.row.riskLevel)">
                  {{ getRiskLevelLabel(scope.row.riskLevel) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="科目编码" prop="subjectCode" width="120" />
            <el-table-column label="科目名称" prop="subjectName" width="150" />
            <el-table-column label="预算金额" width="120">
              <template slot-scope="scope">
                {{ scope.row.budgetAmount ? scope.row.budgetAmount.toFixed(2) : '0.00' }}
              </template>
            </el-table-column>
            <el-table-column label="问题描述" prop="errorMessage" />
          </el-table>
        </div>
      </el-alert>

      <!-- 科目预算明细 -->
      <el-tabs v-model="activeTab" type="border-card">
        <el-tab-pane
          v-for="(type, index) in budgetTypes"
          :key="type.value"
          :label="type.label"
          :name="type.value"
        >
          <el-table
            :data="subjectsByType[type.value] || []"
            border
            style="width: 100%"
            max-height="500"
          >
            <el-table-column label="科目编码" prop="subjectCode" width="150" align="center" />
            <el-table-column label="科目名称" prop="subjectName" width="200" align="center" />
            <el-table-column label="预算金额" prop="budgetAmount" align="center">
              <template slot-scope="scope">
                {{ scope.row.budgetAmount ? scope.row.budgetAmount.toFixed(2) : '0.00' }}
              </template>
            </el-table-column>
            <el-table-column label="说明" prop="remark" align="center" />
          </el-table>
        </el-tab-pane>
      </el-tabs>

      <!-- 底部按钮 -->
      <div style="margin-top: 30px; text-align: center;">
        <el-button @click="handleReturn">返 回</el-button>
        <el-button type="danger" @click="handleReject">
          <i class="el-icon-close"></i> 审核驳回
        </el-button>
        <el-button type="success" @click="handleApprove">
          <i class="el-icon-check"></i> 审核通过
        </el-button>
      </div>
    </el-card>

    <!-- 驳回对话框 -->
    <el-dialog title="审核驳回" :visible.sync="rejectOpen" width="500px" append-to-body>
      <el-form ref="rejectForm" :model="rejectForm" :rules="rejectRules" label-width="100px">
        <el-form-item label="驳回理由" prop="reason">
          <el-input
            v-model="rejectForm.reason"
            type="textarea"
            :rows="6"
            placeholder="请填写驳回理由（必填）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitReject">确 定</el-button>
        <el-button @click="rejectOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getPreparation, listPreparationDetail, approvePreparation, rejectPreparation, getValidationResults } from "@/api/system/preparation";
import { listFirstLevelSubject, listSecondLevelSubject } from "@/api/system/preparation";
import BudgetCountdown from '@/components/BudgetCountdown';
import BudgetRejectTimeline from '@/components/BudgetRejectTimeline';

export default {
  name: "PreparationApprovalDetail",
  components: { BudgetCountdown, BudgetRejectTimeline },
  data() {
    return {
      // 当前激活的tab
      activeTab: '',
      // 预算类型列表
      budgetTypes: [
        { value: 'INCOME', label: '收入类' },
        { value: 'COST', label: '成本类' },
        { value: 'EXPENSE', label: '费用类' },
        { value: 'ASSET', label: '资产类' },
        { value: 'LIABILITY', label: '负债类' },
        { value: 'EQUITY', label: '权益类' }
      ],
      // 按类型分组的科目数据
      subjectsByType: {},
      // 预算编制数据
      preparationData: {},
      // 校验结果
      validationResults: [],
      // 驳回对话框
      rejectOpen: false,
      rejectForm: {
        reason: ''
      },
      // 驳回表单校验
      rejectRules: {
        reason: [
          { required: true, message: "驳回理由不能为空", trigger: "blur" },
          { min: 5, message: "驳回理由至少5个字符", trigger: "blur" }
        ]
      }
    };
  },
  created() {
    this.loadData();
  },
  methods: {
    /** 加载数据 */
    async loadData() {
      const id = this.$route.query.id;
      if (!id) {
        this.$modal.msgError("缺少预算单ID");
        this.handleReturn();
        return;
      }

      try {
        // 加载主表数据
        const prepResponse = await getPreparation(id);
        this.preparationData = prepResponse.data;

        // 加载校验结果
        await this.loadValidationResults(id);

        // 加载科目数据
        await this.loadSubjectsByType();

        // 加载明细数据
        await this.loadDetailData(id);
      } catch (error) {
        this.$modal.msgError("加载数据失败");
        console.error(error);
      }
    },
    /** 加载校验结果 */
    async loadValidationResults(sheetId) {
      try {
        const response = await getValidationResults(sheetId);
        this.validationResults = response.data || [];
      } catch (error) {
        console.error('加载校验结果失败', error);
      }
    },
    /** 按类型加载科目 */
    async loadSubjectsByType() {
      try {
        // 初始化每个类型的数组
        this.budgetTypes.forEach(type => {
          this.$set(this.subjectsByType, type.value, []);
        });
        
        // 获取所有一级科目（level=1）
        const { listFirstLevelSubject } = await import('@/api/system/preparation');
        const firstLevelResponse = await listFirstLevelSubject();
        const firstLevelSubjects = firstLevelResponse.data || [];
        
        // 对每个一级科目，获取其下的所有子科目
        for (const firstLevel of firstLevelSubjects) {
          const children = await this.getAllChildren(firstLevel.id);
          
          // 将科目按类型分组
          children.forEach(subject => {
            const subjectType = subject.subjectType || firstLevel.subjectType;
            if (this.subjectsByType[subjectType]) {
              this.subjectsByType[subjectType].push({
                ...subject,
                budgetAmount: 0,
                remark: '',
                parentSubjectName: firstLevel.subjectName
              });
            }
          });
        }
        
        // 设置默认激活第一个有数据的tab
        const firstTypeWithSubjects = this.budgetTypes.find(type => 
          this.subjectsByType[type.value] && this.subjectsByType[type.value].length > 0
        );
        if (firstTypeWithSubjects) {
          this.activeTab = firstTypeWithSubjects.value;
        }
      } catch (error) {
        this.$modal.msgError("加载科目数据失败");
        console.error(error);
      }
    },
    /** 递归获取所有子科目 */
    async getAllChildren(parentId) {
      const result = [];
      try {
        const { listSecondLevelSubject } = await import('@/api/system/preparation');
        const response = await listSecondLevelSubject(parentId);
        const children = response.data || [];
        
        result.push(...children);
        
        // 递归获取子科目的子科目
        for (const child of children) {
          const subChildren = await this.getAllChildren(child.id);
          result.push(...subChildren);
        }
      } catch (error) {
        console.error(`获取科目 ${parentId} 的子科目失败`, error);
      }
      return result;
    },
    /** 加载明细数据 */
    async loadDetailData(sheetId) {
      try {
        const response = await listPreparationDetail(sheetId);
        const details = response.data || [];

        // 将明细数据映射到对应的科目
        details.forEach(detail => {
          // 在所有类型中查找匹配的科目
          for (const type of this.budgetTypes) {
            const subjects = this.subjectsByType[type.value];
            if (subjects) {
              const subject = subjects.find(s => s.subjectCode === detail.subjectCode);
              if (subject) {
                subject.budgetAmount = detail.budgetAmount || 0;
                subject.remark = detail.remark || '';
                break;
              }
            }
          }
        });
      } catch (error) {
        console.error('加载明细数据失败', error);
      }
    },
    /** Tab切换事件 */
    handleTabClick(tab) {
      // Tab已经预加载所有数据，无需额外操作
      console.log('切换到Tab:', tab.name);
    },
    /** 审核通过 */
    handleApprove() {
      this.$modal.confirm('确认审核通过该预算编制？').then(() => {
        return approvePreparation(this.preparationData.id, '');
      }).then(() => {
        this.$modal.msgSuccess("审核通过");
        setTimeout(() => {
          this.handleReturn();
        }, 1500);
      }).catch(() => {});
    },
    /** 审核驳回 */
    handleReject() {
      this.rejectForm.reason = '';
      this.rejectOpen = true;
    },
    /** 提交驳回 */
    submitReject() {
      this.$refs.rejectForm.validate(valid => {
        if (valid) {
          this.$modal.confirm('确认驳回该预算编制？').then(() => {
            return rejectPreparation(this.preparationData.id, this.rejectForm.reason);
          }).then(() => {
            this.rejectOpen = false;
            this.$modal.msgSuccess("已驳回");
            setTimeout(() => {
              this.handleReturn();
            }, 1500);
          }).catch(() => {});
        }
      });
    },
    /** 返回查询页面 */
    handleReturn() {
      this.$router.push('/system/preparation/approval');
    },
    /** 获取风险等级标签类型 */
    getRiskLevelType(level) {
      const levelMap = {
        'HIGH': 'danger',
        'MEDIUM': 'warning',
        'LOW': 'info'
      };
      return levelMap[level] || 'info';
    },
    /** 获取风险等级标签文本 */
    getRiskLevelLabel(level) {
      const levelMap = {
        'HIGH': '高',
        'MEDIUM': '中',
        'LOW': '低'
      };
      return levelMap[level] || level;
    }
  }
};
</script>

<style scoped>
.mb20 {
  margin-bottom: 20px;
}

.box-card {
  margin-bottom: 20px;
}
</style>
