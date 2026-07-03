<template>
  <div class="app-container">
    <el-card class="box-card">
      <div slot="header" class="clearfix">
        <span>预算编制向导</span>
        <div style="float: right; display: flex; align-items: center; gap: 15px;">
          <span style="font-size: 15px; font-weight: bold; color: #303133;">
            预算总额：<span style="color: #E6A23C; font-size: 18px;">{{ totalBudgetDisplay }}</span> 元
          </span>
        </div>
      </div>

      <!-- 步骤条 -->
      <el-steps :active="currentStep" finish-status="success" align-center style="margin-bottom: 30px;">
        <el-step title="基础信息" description="填写预算年度、月份等基本信息"></el-step>
        <el-step title="科目预算编制" description="按科目录入预算金额"></el-step>
        <el-step title="完成编制" description="规则校验并提交预算编制"></el-step>
      </el-steps>

      <!-- 第一步：基础信息 -->
      <div v-show="currentStep === 0" class="step-content">
        <el-form ref="basicForm" :model="basicForm" :rules="basicRules" label-width="120px" class="form-container">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="预算年度" prop="budgetYear">
                <el-date-picker
                  v-model="basicForm.budgetYear"
                  type="year"
                  placeholder="选择年度"
                  value-format="yyyy"
                  style="width: 100%"
                  disabled
                />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="预算月份" prop="budgetMonth">
                <el-select v-model="basicForm.budgetMonth" placeholder="请选择月份" style="width: 100%">
                  <el-option label="1月" :value="1" />
                  <el-option label="2月" :value="2" />
                  <el-option label="3月" :value="3" />
                  <el-option label="4月" :value="4" />
                  <el-option label="5月" :value="5" />
                  <el-option label="6月" :value="6" />
                  <el-option label="7月" :value="7" />
                  <el-option label="8月" :value="8" />
                  <el-option label="9月" :value="9" />
                  <el-option label="10月" :value="10" />
                  <el-option label="11月" :value="11" />
                  <el-option label="12月" :value="12" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="编制人" prop="createBy">
                <el-input v-model="basicForm.createBy" disabled />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="组织名称" prop="orgName">
                <el-input v-model="basicForm.orgName" disabled />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="24">
              <el-form-item label="备注" prop="remark">
                <el-input
                  v-model="basicForm.remark"
                  type="textarea"
                  :rows="4"
                  placeholder="请输入备注信息（可选）"
                  maxlength="500"
                  show-word-limit
                />
              </el-form-item>
            </el-col>
          </el-row>
        </el-form>
      </div>

      <!-- 第二步：科目预算编制（树形表格） -->
      <div v-show="currentStep === 1" class="step-content">
        <el-tabs v-model="activeTab" type="border-card" @tab-click="handleTabClick">
          <el-tab-pane
            v-for="type in budgetTypes"
            :key="type.value"
            :label="type.label"
            :name="type.value"
          >
            <el-table
              :data="subjectsByType[type.value] || []"
              border
              row-key="id"
              default-expand-all
              :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
              style="width: 100%"
              max-height="500"
            >
              <el-table-column label="科目编码" prop="subjectCode" width="180" align="center" />
              <el-table-column label="科目名称" prop="subjectName" min-width="220" />
              <el-table-column label="预算金额" width="200" align="center">
                <template slot-scope="scope">
                  <el-input-number
                    v-if="scope.row.isLeaf === 1"
                    v-model="scope.row.budgetAmount"
                    :precision="2"
                    :min="0"
                    :step="100"
                    controls-position="right"
                    style="width: 100%"
                    @change="handleAmountChange(scope.row)"
                  />
                  <span v-else>-</span>
                </template>
              </el-table-column>
              <el-table-column label="说明" width="200" align="center">
                <template slot-scope="scope">
                  <el-input
                    v-if="scope.row.isLeaf === 1"
                    v-model="scope.row.remark"
                    placeholder="可选"
                    size="small"
                  />
                  <span v-else>-</span>
                </template>
              </el-table-column>
            </el-table>
          </el-tab-pane>
        </el-tabs>

      </div>

      <!-- 第三步：完成编制（含规则校验） -->
      <div v-show="currentStep === 2" class="step-content">
        <el-card shadow="never">
          <div slot="header">
            <span style="font-weight: bold;">预算编制汇总</span>
          </div>

          <el-descriptions :column="2" border>
            <el-descriptions-item label="预算单号">{{ preparationData.sheetNo || '自动生成' }}</el-descriptions-item>
            <el-descriptions-item label="预算期间">{{ basicForm.budgetYear }}-{{ String(basicForm.budgetMonth).padStart(2, '0') }}</el-descriptions-item>
            <el-descriptions-item label="组织名称">{{ basicForm.orgName }}</el-descriptions-item>
            <el-descriptions-item label="编制人">{{ basicForm.createBy }}</el-descriptions-item>
            <el-descriptions-item label="预算总额" :span="2">
              <span style="color: #E6A23C; font-size: 18px; font-weight: bold;">{{ totalBudgetDisplay }} 元</span>
            </el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">
              {{ basicForm.remark || '无' }}
            </el-descriptions-item>
          </el-descriptions>

          <div style="margin-top: 20px;">
            <h4 style="margin-bottom: 10px;">科目预算明细</h4>
            <el-table
              :data="allDetailData"
              border
              max-height="400"
            >
              <el-table-column label="科目类型" prop="budgetType" width="100" align="center" />
              <el-table-column label="科目编码" prop="subjectCode" width="150" align="center" />
              <el-table-column label="科目名称" prop="subjectName" width="200" align="center" />
              <el-table-column label="预算金额" prop="budgetAmount" align="center">
                <template slot-scope="scope">
                  {{ formatAmount(scope.row.budgetAmount) }}
                </template>
              </el-table-column>
              <el-table-column label="说明" prop="remark" align="center" />
            </el-table>
          </div>
        </el-card>

        <!-- 规则校验区域 -->
        <el-card shadow="never" style="margin-top: 20px;">
          <div slot="header" style="display: flex; justify-content: space-between; align-items: center;">
            <span style="font-weight: bold;">
              <i class="el-icon-check"></i> 规则校验结果
            </span>
            <el-button type="primary" size="small" :loading="validationLoading" @click="runValidation">
              <i class="el-icon-refresh"></i> 重新校验
            </el-button>
          </div>

          <!-- 校验统计 -->
          <div style="margin-bottom: 15px;" v-if="validationResults.length > 0">
            <el-tag type="success" size="medium" style="margin-right: 10px;">
              <i class="el-icon-circle-check"></i> 通过 {{ passCount }}
            </el-tag>
            <el-tag type="danger" size="medium" style="margin-right: 10px;" v-if="errorCount > 0">
              <i class="el-icon-circle-close"></i> 错误 {{ errorCount }}
            </el-tag>
            <el-tag type="warning" size="medium" style="margin-right: 10px;" v-if="warningCount > 0">
              <i class="el-icon-warning"></i> 警告 {{ warningCount }}
            </el-tag>
            <el-tag type="info" size="medium" style="margin-right: 10px;" v-if="infoCount > 0">
              <i class="el-icon-info"></i> 建议 {{ infoCount }}
            </el-tag>
          </div>

          <!-- 阻断提示 -->
          <el-alert
            v-if="hasErrors"
            title="存在严重错误，请返回第二步修正后再完成编制"
            type="error"
            :closable="false"
            show-icon
            style="margin-bottom: 15px;"
          />

          <!-- 校验结果列表 -->
          <div v-loading="validationLoading">
            <div
              v-for="(result, index) in validationResults"
              :key="index"
              class="validation-item"
              :class="'validation-' + result.severityLevel.toLowerCase()"
            >
              <div class="validation-icon">
                <!-- 通过：绿色勾 -->
                <i v-if="result.passed" class="el-icon-circle-check validation-pass"></i>
                <!-- ERROR：红色叉 -->
                <i v-else-if="result.severityLevel === 'ERROR'" class="el-icon-circle-close validation-error"></i>
                <!-- WARNING：黄色警告 -->
                <i v-else-if="result.severityLevel === 'WARNING'" class="el-icon-warning validation-warning"></i>
                <!-- INFO：蓝色信息 -->
                <i v-else class="el-icon-info validation-info"></i>
              </div>
              <div class="validation-content">
                <div class="validation-title">
                  <el-tag :type="getTagType(result)" size="mini" style="margin-right: 8px;">
                    {{ getSeverityLabel(result.severityLevel) }}
                  </el-tag>
                  {{ result.ruleName }}
                </div>
                <div class="validation-message" :class="'text-' + getSeverityClass(result)">
                  <span v-if="result.subjectCode">[{{ result.subjectCode }} {{ result.subjectName || '' }}] </span>
                  {{ result.message }}
                  <span v-if="result.budgetAmount !== null && result.budgetAmount !== undefined" style="color: #909399; margin-left: 8px;">
                    (金额: {{ formatAmount(result.budgetAmount) }})
                  </span>
                </div>
              </div>
            </div>

            <el-empty v-if="!validationLoading && validationResults.length === 0" description="暂无校验结果" />
          </div>
        </el-card>
      </div>

      <!-- 底部按钮 -->
      <div style="margin-top: 30px; text-align: center;">
        <el-button @click="handleReturn">取 消</el-button>
        <el-button v-if="currentStep > 1 && !viewOnly" @click="handlePrevStep">上一步</el-button>
        <el-button v-if="currentStep < 2 && !viewOnly" type="primary" @click="handleNextStep">
          {{ currentStep === 0 ? '下一步' : '下一步' }}
        </el-button>
        <el-button
          v-if="currentStep === 2 && !viewOnly"
          type="success"
          :disabled="hasErrors"
          :loading="submitLoading"
          @click="handleFinish"
        >
          {{ hasErrors ? '存在错误，请返回修正' : '完成编制' }}
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script>
import { addPreparation, updatePreparation, getPreparation, batchSavePreparationDetail, listPreparationDetail, listAllSubjects, executeValidation, getPreviousMonthDetails } from "@/api/system/preparation";
import { getInfo } from "@/api/login";

export default {
  name: "PreparationWizard",
  data() {
    return {
      currentStep: 0,
      viewOnly: false,
      activeTab: '',
      budgetTypes: [
        { value: 'INCOME', label: '收入类' },
        { value: 'COST', label: '成本类' },
        { value: 'EXPENSE', label: '费用类' },
        { value: 'ASSET', label: '资产类' },
        { value: 'LIABILITY', label: '负债类' },
        { value: 'EQUITY', label: '权益类' }
      ],
      subjectsByType: {},
      basicForm: {
        budgetYear: new Date().getFullYear().toString(),
        budgetMonth: new Date().getMonth() + 1,
        orgId: null,
        createBy: '',
        orgName: '',
        remark: ''
      },
      basicRules: {
        budgetYear: [{ required: true, message: "预算年度不能为空", trigger: "blur" }],
        budgetMonth: [{ required: true, message: "预算月份不能为空", trigger: "change" }]
      },
      allDetailData: [],
      preparationData: {},
      totalBudget: 0,
      // 规则校验相关
      validationResults: [],
      validationLoading: false,
      submitLoading: false
    };
  },
  computed: {
    /** 是否有ERROR级别的不通过项 */
    hasErrors() {
      return this.validationResults.some(r => !r.passed && r.severityLevel === 'ERROR');
    },
    passCount() {
      return this.validationResults.filter(r => r.passed).length;
    },
    errorCount() {
      return this.validationResults.filter(r => !r.passed && r.severityLevel === 'ERROR').length;
    },
    warningCount() {
      return this.validationResults.filter(r => !r.passed && r.severityLevel === 'WARNING').length;
    },
    infoCount() {
      return this.validationResults.filter(r => !r.passed && r.severityLevel === 'INFO').length;
    },
    /** 安全格式化预算总额，避免 NaN，带千位符 */
    totalBudgetDisplay() {
      return this.formatAmount(this.totalBudget);
    }
  },
  created() {
    this.initPage();
  },
  methods: {
    async initPage() {
      try {
        const res = await getInfo();
        const user = res.data.user;
        this.basicForm.createBy = user.nickName || user.userName || '';
        this.basicForm.orgId = (user.dept && user.dept.deptId) || null;
        this.basicForm.orgName = (user.dept && user.dept.deptName) || '';
      } catch (error) {
        console.error('获取用户信息失败', error);
      }

      const id = this.$route.query.id;
      this.viewOnly = this.$route.query.viewOnly === 'true';

      if (id) {
        await this.loadExistingData(id);
      } else {
        await this.loadSubjectsByType();
        // 新建模式：尝试加载上月已审批数据初始化金额
        await this.loadPreviousMonthData();
      }
    },

    async loadExistingData(id) {
      try {
        const response = await getPreparation(id);
        const data = response.data;
        this.basicForm.budgetYear = data.budgetYear.toString();
        this.basicForm.budgetMonth = data.budgetMonth;
        this.basicForm.orgId = data.orgId;
        this.basicForm.createBy = data.createBy;
        this.basicForm.orgName = data.orgName;
        this.basicForm.remark = data.remark;
        this.preparationData = data;

        await this.loadSubjectsByType();
        await this.loadDetailData(id);

        // 修改模式：直接进入第2步，不可返回第1步
        if (this.viewOnly) {
          this.currentStep = 2;
        } else {
          this.currentStep = 1;
        }
      } catch (error) {
        this.$modal.msgError("加载数据失败");
        console.error(error);
      }
    },

    async loadSubjectsByType() {
      try {
        const response = await listAllSubjects();
        const allSubjects = response.data || [];

        const grouped = {};
        this.budgetTypes.forEach(t => { grouped[t.value] = []; });

        allSubjects.forEach(s => {
          const type = s.subjectType;
          if (grouped[type]) {
            grouped[type].push({ ...s, budgetAmount: 0, remark: '' });
          }
        });

        this.budgetTypes.forEach(t => {
          this.$set(this.subjectsByType, t.value, this.buildTree(grouped[t.value]));
        });

        const firstTypeWithSubjects = this.budgetTypes.find(t =>
          this.subjectsByType[t.value] && this.subjectsByType[t.value].length > 0
        );
        if (firstTypeWithSubjects) {
          this.activeTab = firstTypeWithSubjects.value;
        }
      } catch (error) {
        this.$modal.msgError("加载科目数据失败");
        console.error(error);
      }
    },

    /** 加载上月已审批通过的预算数据，初始化本月金额 */
    async loadPreviousMonthData() {
      if (!this.basicForm.orgId || !this.basicForm.budgetYear || !this.basicForm.budgetMonth) {
        return;
      }
      try {
        const response = await getPreviousMonthDetails(
          this.basicForm.orgId,
          parseInt(this.basicForm.budgetYear),
          this.basicForm.budgetMonth
        );
        const prevDetails = response.data || [];
        if (prevDetails.length === 0) {
          return;
        }

        // 构建科目编码到叶子节点的映射
        const leafMap = {};
        this.budgetTypes.forEach(t => {
          this.collectLeaves(this.subjectsByType[t.value] || []).forEach(leaf => {
            leafMap[leaf.subjectCode] = leaf;
          });
        });

        // 将上月数据填充到当前表单
        prevDetails.forEach(detail => {
          const leaf = leafMap[detail.subjectCode];
          if (leaf) {
            leaf.budgetAmount = Number(detail.budgetAmount) || 0;
            leaf.remark = detail.remark || '';
          }
        });

        // 重新计算总额
        this.calculateTotalBudget();
        this.$modal.msgSuccess(`已加载上月预算数据（${prevDetails.length}个科目），请根据实际情况调整`);
      } catch (error) {
        console.log('加载上月数据失败或上月无已审批数据', error);
      }
    },

    buildTree(items) {
      const map = {};
      const roots = [];
      items.forEach(item => {
        item.children = [];
        map[item.id] = item;
      });
      items.forEach(item => {
        if (item.parentId && item.parentId !== 0 && map[item.parentId]) {
          map[item.parentId].children.push(item);
        } else {
          roots.push(item);
        }
      });
      return roots;
    },

    collectLeaves(nodes) {
      const leaves = [];
      const walk = (list) => {
        list.forEach(node => {
          if (node.isLeaf === 1) {
            leaves.push(node);
          }
          if (node.children && node.children.length > 0) {
            walk(node.children);
          }
        });
      };
      walk(nodes);
      return leaves;
    },

    async loadDetailData(sheetId) {
      try {
        const response = await listPreparationDetail(sheetId);
        const details = response.data || [];

        const leafMap = {};
        this.budgetTypes.forEach(t => {
          this.collectLeaves(this.subjectsByType[t.value] || []).forEach(leaf => {
            leafMap[leaf.subjectCode] = leaf;
          });
        });

        details.forEach(detail => {
          const leaf = leafMap[detail.subjectCode];
          if (leaf) {
            leaf.budgetAmount = Number(detail.budgetAmount) || 0;
            leaf.remark = detail.remark || '';
          }
        });

        this.calculateTotalBudget();
      } catch (error) {
        console.error('加载明细数据失败', error);
      }
    },

    handleTabClick(tab) {
      console.log('切换到Tab:', tab.name);
    },

    handleAmountChange(row) {
      // 确保行级金额是数值类型
      row.budgetAmount = Number(row.budgetAmount) || 0;
      this.calculateTotalBudget();
    },

    calculateTotalBudget() {
      let total = 0;
      this.budgetTypes.forEach(t => {
        const tree = this.subjectsByType[t.value];
        if (tree) {
          this.collectLeaves(tree).forEach(leaf => {
            const amt = parseFloat(leaf.budgetAmount);
            if (!isNaN(amt)) {
              total += amt;
            }
          });
        }
      });
      this.totalBudget = parseFloat(total.toFixed(2));
    },

    async handlePrevStep() {
      if (this.currentStep > 0) {
        // 修改模式下，禁止从第二步返回第一步（基础信息不可修改）
        if (this.preparationData.id && this.currentStep === 1) {
          return;
        }
        // 从第三步返回第二步时，从数据库加载明细数据回填表单
        if (this.currentStep === 2 && this.preparationData.id) {
          await this.reloadDetailDataFromDb();
        }
        this.currentStep--;
      }
    },

    async handleNextStep() {
      if (this.currentStep === 0) {
        this.$refs.basicForm.validate(async valid => {
          if (valid) {
            // 校验部门+月份是否已存在
            const exists = await this.checkDuplicate();
            if (exists) {
              this.$modal.msgWarning(`该部门 ${this.basicForm.orgName} 在 ${this.basicForm.budgetYear}年${String(this.basicForm.budgetMonth).padStart(2, '0')}月 已有预算编制记录，请勿重复创建`);
              return;
            }
            
            await this.saveBasicInfo();
            this.currentStep++;
          }
        });
      } else if (this.currentStep === 1) {
        if (this.totalBudget === 0) {
          this.$modal.msgWarning("请至少填写一个科目的预算金额");
          return;
        }
        this.prepareSummaryData();
        // 保存明细数据（后端会自动判断：无数据则新增，有数据则先删后插；同时自动更新编制总额）
        await this.saveDetailDataToDb();
        this.currentStep++;
        // 进入第三步时自动执行规则校验
        await this.runValidation();
      }
    },

    async saveBasicInfo() {
      try {
        const data = {
          budgetYear: parseInt(this.basicForm.budgetYear),
          budgetMonth: this.basicForm.budgetMonth,
          orgId: this.basicForm.orgId,
          orgName: this.basicForm.orgName,
          createBy: this.basicForm.createBy,
          remark: this.basicForm.remark,
          status: 'Draft',
          totalBudget: 0
        };

        let response;
        if (this.preparationData.id) {
          data.id = this.preparationData.id;
          response = await updatePreparation(data);
        } else {
          response = await addPreparation(data);
          // 后端返回新建记录的 ID
          const newId = response.data;
          this.preparationData = { id: newId };
        }
        this.$modal.msgSuccess("基础信息保存成功");
      } catch (error) {
        this.$modal.msgError("保存失败：" + (error.message || '未知错误'));
        throw error;
      }
    },

    /** 保存明细数据到数据库 */
    async saveDetailDataToDb() {
      try {
        const details = this.allDetailData.map(item => ({
          sheetId: this.preparationData.id,
          subjectCode: item.subjectCode,
          subjectName: item.subjectName,
          subjectType: item.subjectType,
          budgetAmount: Number(item.budgetAmount) || 0,
          remark: item.remark
        }));
        await batchSavePreparationDetail(details);
      } catch (error) {
        console.error('保存明细数据失败', error);
        this.$modal.msgError("保存明细数据失败：" + (error.message || '未知错误'));
        throw error;
      }
    },

    /** 从数据库加载明细数据回填到第二步表单 */
    async reloadDetailDataFromDb() {
      try {
        // 先清空当前表单中所有叶子节点的金额
        this.budgetTypes.forEach(t => {
          this.collectLeaves(this.subjectsByType[t.value] || []).forEach(leaf => {
            leaf.budgetAmount = 0;
            leaf.remark = '';
          });
        });
        // 从数据库重新加载明细数据
        await this.loadDetailData(this.preparationData.id);
      } catch (error) {
        console.error('重新加载明细数据失败', error);
      }
    },

    /** 汇总所有科目金额并更新到编制主表的预算总额字段 */
    async updateTotalBudgetToPreparation() {
      try {
        await updatePreparation({
          id: this.preparationData.id,
          totalBudget: Number(this.totalBudget) || 0,
          orgId: this.basicForm.orgId,
          budgetYear: parseInt(this.basicForm.budgetYear),
          budgetMonth: this.basicForm.budgetMonth
        });
      } catch (error) {
        console.error('更新预算总额失败', error);
        this.$modal.msgError("更新预算总额失败：" + (error.message || '未知错误'));
        throw error;
      }
    },

    prepareSummaryData() {
      this.allDetailData = [];
      this.budgetTypes.forEach(t => {
        const typeLabel = t.label;
        this.collectLeaves(this.subjectsByType[t.value] || []).forEach(leaf => {
          if (leaf.budgetAmount > 0) {
            this.allDetailData.push({
              budgetType: typeLabel,
              subjectType: t.value,
              subjectCode: leaf.subjectCode,
              subjectName: leaf.subjectName,
              budgetAmount: leaf.budgetAmount,
              remark: leaf.remark
            });
          }
        });
      });
    },

    /** 检查部门+月份是否已存在 */
    async checkDuplicate() {
      try {
        const { listPreparation } = await import('@/api/system/preparation');
        const response = await listPreparation({
          budgetYear: parseInt(this.basicForm.budgetYear),
          budgetMonth: this.basicForm.budgetMonth,
          orgId: this.basicForm.orgId
        });
        const rows = response.rows || [];
        // 排除当前正在编辑的记录
        const filtered = rows.filter(row => row.id !== this.preparationData.id);
        return filtered.length > 0;
      } catch (error) {
        console.error('检查重复失败', error);
        return false;
      }
    },

    /** 执行规则校验 */
    async runValidation() {
      this.validationLoading = true;
      this.validationResults = [];
      try {
        // 构造校验数据：包含所有有金额的明细 + 零金额的叶子节点
        const details = [];
        this.budgetTypes.forEach(t => {
          this.collectLeaves(this.subjectsByType[t.value] || []).forEach(leaf => {
            details.push({
              subjectCode: leaf.subjectCode,
              subjectName: leaf.subjectName,
              subjectType: t.value,
              budgetAmount: leaf.budgetAmount || 0
            });
          });
        });

        const response = await executeValidation(details);
        this.validationResults = response.data || [];
      } catch (error) {
        console.error('规则校验失败', error);
        this.$modal.msgError("规则校验执行失败：" + (error.message || ''));
      } finally {
        this.validationLoading = false;
      }
    },

    /** 获取严重级别标签类型 */
    getTagType(result) {
      if (result.passed) return 'success';
      switch (result.severityLevel) {
        case 'ERROR': return 'danger';
        case 'WARNING': return 'warning';
        case 'INFO': return 'info';
        default: return 'info';
      }
    },

    /** 获取严重级别中文 */
    getSeverityLabel(level) {
      switch (level) {
        case 'ERROR': return '错误';
        case 'WARNING': return '警告';
        case 'INFO': return '建议';
        default: return level;
      }
    },

    /** 获取严重级别CSS类 */
    getSeverityClass(result) {
      if (result.passed) return 'pass';
      return result.severityLevel.toLowerCase();
    },

    /** 完成编制 */
    async handleFinish() {
      if (this.hasErrors) {
        this.$modal.msgError("存在严重错误，请返回修正后再完成编制");
        return;
      }

      try {
        this.submitLoading = true;

        const details = this.allDetailData.map(item => ({
          sheetId: this.preparationData.id,
          subjectCode: item.subjectCode,
          subjectName: item.subjectName,
          subjectType: item.subjectType,
          budgetAmount: item.budgetAmount,
          remark: item.remark
        }));

        // 保存明细数据（后端自动汇总更新编制总额）
        await batchSavePreparationDetail(details);

        this.$modal.msgSuccess("预算编制完成！");
        this.$router.push('/system/preparation');
      } catch (error) {
        this.$modal.msgError("保存失败：" + (error.message || '未知错误'));
      } finally {
        this.submitLoading = false;
      }
    },

    handleReturn() {
      this.$router.push('/system/preparation');
    },
    /** 格式化金额（千位符，2位小数） */
    formatAmount(val) {
      const num = parseFloat(val);
      if (isNaN(num)) return '0.00';
      const fixed = num.toFixed(2);
      const parts = fixed.split('.');
      parts[0] = parts[0].replace(/\B(?=(\d{3})+(?!\d))/g, ',');
      return parts.join('.');
    }
  }
};
</script>

<style scoped>
.step-content {
  min-height: 400px;
  padding: 20px;
}
.form-container {
  max-width: 800px;
  margin: 0 auto;
}
.box-card {
  margin-bottom: 20px;
}

/* 规则校验样式 */
.validation-item {
  display: flex;
  align-items: flex-start;
  padding: 12px 15px;
  margin-bottom: 8px;
  border-radius: 6px;
  border-left: 4px solid #dcdfe6;
  background-color: #fafafa;
  transition: all 0.3s;
}
.validation-item:hover {
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}
.validation-item.validation-error {
  border-left-color: #f56c6c;
  background-color: #fef0f0;
}
.validation-item.validation-warning {
  border-left-color: #e6a23c;
  background-color: #fdf6ec;
}
.validation-item.validation-info {
  border-left-color: #409eff;
  background-color: #ecf5ff;
}
.validation-item.validation-pass {
  border-left-color: #67c23a;
  background-color: #f0f9eb;
}
.validation-icon {
  margin-right: 12px;
  font-size: 20px;
  line-height: 1;
  flex-shrink: 0;
  margin-top: 2px;
}
.validation-pass {
  color: #67c23a;
  font-size: 22px;
}
.validation-error {
  color: #f56c6c;
  font-size: 22px;
}
.validation-warning {
  color: #e6a23c;
  font-size: 22px;
}
.validation-info {
  color: #409eff;
  font-size: 22px;
}
.validation-content {
  flex: 1;
}
.validation-title {
  font-weight: 600;
  font-size: 14px;
  margin-bottom: 4px;
  color: #303133;
}
.validation-message {
  font-size: 13px;
  line-height: 1.5;
}
.text-pass {
  color: #67c23a;
}
.text-error {
  color: #f56c6c;
}
.text-warning {
  color: #e6a23c;
}
.text-info {
  color: #409eff;
}
</style>
