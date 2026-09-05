<template>
  <div class="app-container ai-expense-container">
    <!-- 查看模式：传统只读展示 -->
    <el-card v-if="isViewMode" class="box-card">
      <div slot="header" class="clearfix">
        <span>查看报销单</span>
        <el-button style="float: right; padding: 3px 0" type="text" @click="$router.go(-1)">返回</el-button>
      </div>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="物资小类">{{ getDictLabel(itemCategoryOptions, form.itemCategory) }}</el-descriptions-item>
        <el-descriptions-item label="用途标签">{{ getPurposeTagName(form.purposeTag) }}</el-descriptions-item>
        <el-descriptions-item label="报销年月">{{ form.expensePeriod }}</el-descriptions-item>
        <el-descriptions-item label="币种">{{ form.currencyCode }}</el-descriptions-item>
        <el-descriptions-item label="金额(人民币)">¥{{ Number(form.amount || 0).toFixed(2) }}</el-descriptions-item>
        <el-descriptions-item label="出差天数">{{ form.travelDays || '-' }}</el-descriptions-item>
        <el-descriptions-item v-if="form.usefulLife" label="折旧年限">{{ form.usefulLife }} 年</el-descriptions-item>
        <el-descriptions-item label="备注" :span="2">{{ form.remark || '-' }}</el-descriptions-item>
      </el-descriptions>
      <div style="margin-top: 20px; text-align: center;">
        <el-button @click="$router.go(-1)">返 回</el-button>
      </div>
    </el-card>

    <!-- 填写模式：AI引导式对话 -->
    <template v-else>
      <div class="ai-guided-layout">
        <!-- 左侧：AI对话引导面板 -->
        <div class="ai-chat-panel">
          <div class="chat-header">
            <div class="header-left">
              <i class="el-icon-s-opportunity" style="font-size: 20px; color: #409EFF;"></i>
              <span class="header-title">AI 报销助手</span>
              <el-tag type="success" size="mini">企业专业版</el-tag>
            </div>
            <el-button type="text" icon="el-icon-refresh" @click="resetConversation">重新开始</el-button>
          </div>

          <!-- 对话消息区 -->
          <div class="chat-messages" ref="chatMessages">
            <div v-for="(msg, idx) in chatMessages" :key="idx" :class="['chat-msg', msg.role]">
              <div class="msg-avatar">
                <el-avatar v-if="msg.role === 'ai'" :size="32" icon="el-icon-s-opportunity" style="background: linear-gradient(135deg, #667eea, #764ba2);" />
                <el-avatar v-else :size="32" icon="el-icon-user-solid" style="background: #409EFF;" />
              </div>
              <div class="msg-body">
                <div class="msg-bubble" v-html="msg.content"></div>
                <!-- 快捷选项按钮 -->
                <div v-if="msg.options && msg.options.length" class="msg-options">
                  <el-button
                    v-for="opt in msg.options"
                    :key="opt.value"
                    size="small"
                    :type="msg.selected === opt.value ? 'primary' : 'default'"
                    @click="handleOptionClick(msg, opt)"
                    :disabled="msg.answered"
                  >{{ opt.label }}</el-button>
                </div>
                <!-- 输入型步骤 -->
                <div v-if="msg.inputType && !msg.answered" class="msg-input">
                  <template v-if="msg.inputType === 'amount'">
                    <el-input-number v-model="inputAmount" :min="0.01" :precision="2" placeholder="请输入金额" size="small" style="width: 180px;" />
                    <el-button type="primary" size="small" @click="submitInput(msg, 'amount')" style="margin-left: 8px;">确定</el-button>
                  </template>
                  <template v-else-if="msg.inputType === 'period'">
                    <el-date-picker v-model="inputPeriod" type="month" format="yyyy-MM" value-format="yyyy-MM" placeholder="选择月份" size="small" style="width: 150px;" />
                    <el-button type="primary" size="small" @click="submitInput(msg, 'period')" style="margin-left: 8px;">确定</el-button>
                  </template>
                  <template v-else-if="msg.inputType === 'travelDays'">
                    <el-input-number v-model="inputTravelDays" :min="0" :max="365" placeholder="出差天数" size="small" style="width: 140px;" />
                    <el-button type="primary" size="small" @click="submitInput(msg, 'travelDays')" style="margin-left: 8px;">确定</el-button>
                    <el-button size="small" @click="skipStep(msg)" style="margin-left: 4px;">跳过</el-button>
                  </template>
                  <template v-else-if="msg.inputType === 'remark'">
                    <el-input v-model="inputRemark" placeholder="输入备注（可选）" size="small" style="width: 220px;" />
                    <el-button type="primary" size="small" @click="submitInput(msg, 'remark')" style="margin-left: 8px;">确定</el-button>
                    <el-button size="small" @click="skipStep(msg)" style="margin-left: 4px;">跳过</el-button>
                  </template>
                  <template v-else-if="msg.inputType === 'usefulLife'">
                    <el-input-number v-model="inputUsefulLife" :min="1" :max="costMethod === 'ACCRUAL' ? 600 : 50" :placeholder="costMethod === 'ACCRUAL' ? '摊销期数' : '折旧年限'" size="small" style="width: 140px;" />
                    <span style="margin-left: 6px; font-size: 12px; color: #909399;">{{ costMethod === 'ACCRUAL' ? '月' : '年' }}</span>
                    <el-button type="primary" size="small" @click="submitInput(msg, 'usefulLife')" style="margin-left: 8px;">确定</el-button>
                  </template>
                </div>
              </div>
            </div>
            <!-- 打字指示器 -->
            <div v-if="aiTyping" class="chat-msg ai">
              <div class="msg-avatar">
                <el-avatar :size="32" icon="el-icon-s-opportunity" style="background: linear-gradient(135deg, #667eea, #764ba2);" />
              </div>
              <div class="msg-body">
                <div class="msg-bubble typing"><span></span><span></span><span></span></div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右侧：实时表单预览面板 -->
        <div class="form-preview-panel">
          <el-card shadow="never" class="preview-card">
            <div slot="header"><i class="el-icon-document"></i> 报销单信息</div>
            <el-form label-width="90px" size="small">
              <el-form-item label="消费大类">
                <el-tag v-if="form.expenseCategory" size="small">{{ getCategoryLabel(form.expenseCategory) }}</el-tag>
                <span v-else class="placeholder-text">待选择</span>
              </el-form-item>
              <el-form-item label="物资小类">
                <el-tag v-if="form.itemCategory" size="small" type="success">{{ getDictLabel(itemCategoryOptions, form.itemCategory) }}</el-tag>
                <span v-else class="placeholder-text">待选择</span>
              </el-form-item>
              <el-form-item label="用途标签">
                <el-tag v-if="form.purposeTag" size="small" type="warning">{{ getPurposeTagName(form.purposeTag) }}</el-tag>
                <span v-else class="placeholder-text">待选择</span>
              </el-form-item>
              <el-form-item label="报销年月">
                <span v-if="form.expensePeriod">{{ form.expensePeriod }}</span>
                <span v-else class="placeholder-text">待选择</span>
              </el-form-item>
              <el-form-item v-if="multiCurrency" label="币种">
                <el-tag v-if="form.currencyCode" size="small" type="info">{{ getCurrencyName(form.currencyCode) }}</el-tag>
                <span v-else class="placeholder-text">待选择</span>
              </el-form-item>
              <el-form-item label="金额">
                <span v-if="form.amount" style="font-weight: bold; color: #409EFF;">
                  {{ form.currencyCode !== 'CNY' ? getCurrencySymbol(form.currencyCode) : '¥' }}{{ Number(form.currencyCode !== 'CNY' && form.originalAmount ? form.originalAmount : form.amount).toFixed(2) }}
                  <span v-if="form.currencyCode !== 'CNY'" style="font-size:12px;color:#909399;">（{{ getCurrencyName(form.currencyCode) }}）</span>
                </span>
                <span v-else class="placeholder-text">待填写</span>
              </el-form-item>
              <el-form-item v-if="form.travelDays" label="出差天数">
                <span>{{ form.travelDays }} 天</span>
              </el-form-item>
              <el-form-item v-if="analysisDone && costMethod === 'DEPRECIATION'" label="支出类型">
                <el-tag size="small" type="danger">资本性支出（折旧）</el-tag>
              </el-form-item>
              <el-form-item v-else-if="analysisDone && costMethod === 'ACCRUAL'" label="支出类型">
                <el-tag size="small" type="warning">计提摊销类</el-tag>
              </el-form-item>
              <el-form-item v-else-if="analysisDone && costMethod === 'FEE_RATE'" label="支出类型">
                <el-tag size="small" type="info">财务费用（费率/利息）</el-tag>
              </el-form-item>
              <el-form-item v-else-if="analysisDone" label="支出类型">
                <el-tag size="small" type="success">收益性支出（直接费用化）</el-tag>
              </el-form-item>
              <el-form-item v-if="form.usefulLife && costMethod === 'DEPRECIATION'" label="折旧年限">
                <span>{{ form.usefulLife }} 年</span>
              </el-form-item>
              <el-form-item v-else-if="form.usefulLife && costMethod === 'ACCRUAL'" label="摊销期数">
                <span>{{ form.usefulLife }} 月</span>
              </el-form-item>
              <el-form-item v-if="subsidyResult" label="补助金额">
                <span style="color: #67C23A;">¥{{ subsidyResult.amount }}</span>
              </el-form-item>
              <el-form-item v-if="form.remark" label="备注">
                <span>{{ form.remark }}</span>
              </el-form-item>
            </el-form>

            <!-- 预算归属预览 -->
            <el-alert
              v-if="budgetPreview"
              :title="budgetPreview.passed ? '预算匹配成功' : '预算匹配失败'"
              :type="budgetPreview.passed ? 'success' : 'error'"
              :closable="false"
              show-icon
              style="margin-top: 12px;"
            >
              <template v-if="budgetPreview.passed">
                <p style="margin: 4px 0;">科目：{{ budgetPreview.budgetSubjectName || budgetPreview.budgetSubject }}</p>
                <p v-if="budgetPreview.remainingBudget != null" style="margin: 4px 0;">
                  可用余额：¥{{ Number(budgetPreview.remainingBudget).toFixed(2) }}
                </p>
              </template>
              <template v-else>
                <p style="margin: 4px 0;">{{ budgetPreview.message || '请联系财务配置映射规则' }}</p>
              </template>
            </el-alert>

            <!-- 盈利提示 -->
            <el-alert
              v-if="profitTip"
              :title="profitTip.tipText"
              :type="profitTip.tipType === 'INCREASE' ? 'success' : (profitTip.tipType === 'DECREASE' ? 'warning' : 'info')"
              :closable="false"
              show-icon
              style="margin-top: 12px;"
            />

            <!-- 提交按钮 -->
            <div v-if="canSubmit" style="margin-top: 20px; text-align: center;">
              <el-button type="primary" :loading="submitLoading" icon="el-icon-check" @click="handleSubmit">
                确认提交报销单
              </el-button>
            </div>
          </el-card>
        </div>
      </div>
    </template>
  </div>
</template>

<script>
import { addExpenseBill, getExpenseBill, calculateSubsidy, getProfitTip, listCurrencies } from "@/api/system/fee/expense";
import { allEnabledPurposeTag } from "@/api/system/fee/purposeTag";
import { previewMapping } from "@/api/system/fee/mappingRule";
import { getPrompt } from "@/api/system/fee/aiPrompt";
import { getUserProfile } from "@/api/system/user";

export default {
  name: "ExpenseForm",
  dicts: ['fee_item_category'],
  data() {
    return {
      submitLoading: false,
      aiTyping: false,
      purposeTags: [],
      currencyOptions: [],
      budgetPreview: null,
      profitTip: null,
      subsidyResult: null,
      currentUser: {},
      itemCategoryOptions: [],
      chatMessages: [],
      currentStep: 0,
      // 输入暂存
      inputAmount: undefined,
      inputPeriod: new Date().toISOString().slice(0, 7),
      inputTravelDays: undefined,
      inputRemark: '',
      inputUsefulLife: undefined,
      // 映射规则分析结果
      isFixedAsset: false,
      accountingClass: '',
      costMethod: 'DIRECT',
      analysisDone: false,
      financialTipShown: false,
      remarkPhaseDone: false,
      periodConfirmed: false,
      // 表单数据
      form: {
        expenseCategory: '',
        itemCategory: '',
        purposeTag: '',
        amount: undefined,
        currencyCode: 'CNY',
        originalAmount: undefined,
        travelDays: undefined,
        usefulLife: undefined,
        expenseDate: new Date().toISOString().slice(0, 10),
        expensePeriod: new Date().toISOString().slice(0, 7),
        remark: ''
      },
      // AI话术配置
      promptConfig: null
    };
  },
  computed: {
    isViewMode() {
      return this.$route.query.view === '1';
    },
    /** 多币种是否开启（高级版/企业版返回多个币种，基础版仅CNY） */
    multiCurrency() {
      return this.currencyOptions.length > 1;
    },
    /** 是否财务费用类（利息/费率），会计大类为 FINANCIAL 或科目编码以3003开头 */
    isFinancialExpense() {
      return this.accountingClass === 'FINANCIAL' ||
        (this.budgetPreview && this.budgetPreview.budgetSubject && this.budgetPreview.budgetSubject.startsWith('3003'));
    },
    canSubmit() {
      return this.form.itemCategory && this.form.purposeTag && this.form.amount && this.form.expensePeriod && this.currentStep >= 99;
    }
  },
  created() {
    this.loadPurposeTags();
    this.loadCurrencyOptions();
    this.loadCurrentUser().then(() => {
      this.itemCategoryOptions = this.dict.type.fee_item_category || [];
      if (this.isViewMode && this.$route.query.id) {
        this.loadExpenseBill(this.$route.query.id);
      } else if (!this.isViewMode) {
        this.loadPromptConfig();
      }
    });
  },
  watch: {
    'dict.type.fee_item_category': {
      handler(val) {
        if (val && val.length) {
          this.itemCategoryOptions = val;
        }
      },
      immediate: true
    }
  },
  methods: {
    // ========== 初始化 ==========
    async loadPromptConfig() {
      try {
        const res = await getPrompt('EXPENSE_BASIC', '*');
        this.promptConfig = res.data;
      } catch (e) {
        this.promptConfig = null;
      }
      this.startConversation();
    },
    loadPurposeTags() {
      allEnabledPurposeTag().then(response => {
        this.purposeTags = response.data || [];
      });
    },
    loadCurrencyOptions() {
      listCurrencies().then(response => {
        this.currencyOptions = response.data || [{ code: 'CNY', name: '人民币' }];
      }).catch(() => {
        this.currencyOptions = [{ code: 'CNY', name: '人民币' }];
      });
    },
    async loadCurrentUser() {
      const response = await getUserProfile();
      this.currentUser = (response.data && response.data.user) || {};
    },
    loadExpenseBill(id) {
      getExpenseBill(id).then(response => {
        const bill = response.data;
        this.form = {
          itemCategory: bill.itemCategory || '',
          purposeTag: bill.purposeTag || '',
          amount: bill.amount,
          currencyCode: bill.currencyCode || 'CNY',
          originalAmount: bill.originalAmount,
          travelDays: bill.travelDays,
          usefulLife: bill.usefulLife,
          expenseDate: bill.expenseDate,
          expensePeriod: bill.expensePeriod || '',
          remark: bill.remark || ''
        };
      });
    },

    // ========== AI对话引导流程 ==========
    startConversation() {
      this.chatMessages = [];
      this.currentStep = 0;
      const deptName = (this.currentUser.dept && this.currentUser.dept.deptName) || '当前部门';
      this.addAiMessage(
        `您好！我是AI报销助手 🤖<br>已自动识别您的部门：<b>${deptName}</b><br><br>接下来我将引导您完成报销单填写，请根据提示逐步操作。`
      );
      this.$nextTick(() => this.nextStep());
    },
    resetConversation() {
      this.form = {
        expenseCategory: '',
        itemCategory: '',
        purposeTag: '',
        amount: undefined,
        currencyCode: 'CNY',
        originalAmount: undefined,
        travelDays: undefined,
        usefulLife: undefined,
        expenseDate: new Date().toISOString().slice(0, 10),
        expensePeriod: new Date().toISOString().slice(0, 7),
        remark: ''
      };
      this.budgetPreview = null;
      this.profitTip = null;
      this.subsidyResult = null;
      // 重置映射规则分析状态
      this.isFixedAsset = false;
      this.accountingClass = '';
      this.analysisDone = false;
      this.financialTipShown = false;
      this.remarkPhaseDone = false;
      this.periodConfirmed = false;
      this.inputUsefulLife = undefined;
      this.startConversation();
    },
    nextStep() {
      this.currentStep++;
      const step = this.currentStep;
      // 基础版：多币种未开启时跳过币种选择步骤
      if (step === 4 && !this.multiCurrency) {
        this.nextStep();
        return;
      }
      setTimeout(() => {
        if (step === 1) {
          // 步骤1：选择消费大类
          this.addAiMessage('请问本次报销属于哪个<b>消费大类</b>？', [
            { label: '🛒 采购', value: 'PURCHASE' },
            { label: '✈️ 差旅', value: 'TRAVEL' },
            { label: '🍽️ 招待', value: 'ENTERTAIN' },
            { label: '📦 其他', value: 'OTHER' }
          ]);
        } else if (step === 2) {
          // 步骤2：选择物资小类
          const options = (this.itemCategoryOptions || []).map(d => ({ label: d.label, value: d.value }));
          this.addAiMessage('请选择<b>物资小类</b>（报销物品的具体分类）：', options);
        } else if (step === 3) {
          // 步骤3：选择用途标签
          const options = this.purposeTags.map(t => ({ label: t.tagName + ' ' + (t.tagIcon || ''), value: t.tagCode }));
          this.addAiMessage('请选择<b>用途标签</b>（该笔费用的业务用途）：', options);
        } else if (step === 4) {
          // 步骤4：选择币种（仅多币种开启时显示）
          const options = (this.currencyOptions || []).map(c => ({ label: c.name + '（' + c.code + '）', value: c.code }));
          this.addAiMessage('请选择<b>报销币种</b>：', options);
        } else {
          // 步骤5+：动态阶段（根据映射规则分析结果决定流程）
          this.renderDynamicPhase();
        }
        this.aiTyping = false;
        this.scrollToBottom();
      }, 600);
      this.aiTyping = true;
    },
    /**
     * 动态阶段渲染：根据映射规则分析结果（costMethod）决定后续步骤
     * 阶段顺序：金额 → [折旧:折旧年限] → [计提:摊销期数] → [费率:提示] → 报销年月 → 出差天数 → 备注 → 确认
     */
    renderDynamicPhase() {
      if (!this.form.amount) {
        // 阶段：输入金额
        if (this.multiCurrency) {
          const cur = this.currencyOptions.find(c => c.code === this.form.currencyCode);
          const curName = cur ? cur.name : '人民币';
          const tip = this.form.currencyCode !== 'CNY' ? '（' + curName + '，系统将按当月汇率折算为人民币）' : '（人民币，单位：元）';
          this.addAiMessageInput('请输入<b>报销金额</b>' + tip + '：', 'amount');
        } else {
          this.addAiMessageInput('请输入<b>报销金额</b>（人民币，单位：元）：', 'amount');
        }
      } else if (this.costMethod === 'DEPRECIATION' && !this.form.usefulLife) {
        // 阶段：折旧类 → 折旧年限
        this.addAiMessageInput(
          '📊 智能分析：该笔支出匹配<b>固定资产折旧</b>规则（资本性支出）。<br>请输入<b>折旧年限</b>（系统将按月自动计提折旧）：',
          'usefulLife'
        );
      } else if (this.costMethod === 'ACCRUAL' && !this.form.usefulLife) {
        // 阶段：计提类 → 摊销期数
        this.addAiMessageInput(
          '📊 智能分析：该笔支出匹配<b>计提摊销</b>规则（长期待摊/无形资产）。<br>请输入<b>摊销期数（月）</b>（系统将按月自动计提摊销）：',
          'usefulLife'
        );
      } else if (this.costMethod === 'FEE_RATE' && !this.financialTipShown) {
        // 阶段：费率类 → 提示利息/费率由系统自动计提（企业版功能）
        this.financialTipShown = true;
        this.addAiMessage(
          '📊 智能分析：该笔支出归属<b>财务费用</b>类（利息/费率）。<br>' +
          '<el-tag type="warning" size="mini">企业版</el-tag> 借款利息由系统按月自动计提，无需手动填写费率。<br>' +
          '当前仅记录报销金额，利息计提请在「借款台账」中管理。'
        );
        this.$nextTick(() => this.nextStep());
      } else if (!this.periodConfirmed) {
        // 阶段：报销年月
        this.addAiMessageInput('请选择<b>报销年月</b>（对应预算编制期间，系统按此月查询预算余额）：', 'period');
      } else if (this.form.travelDays === undefined) {
        // 阶段：出差天数
        this.addAiMessageInput('如有出差，请输入<b>出差天数</b>（系统将自动核算补助金额）：', 'travelDays');
      } else if (!this.remarkPhaseDone) {
        // 阶段：备注
        this.remarkPhaseDone = true;
        this.addAiMessageInput('是否需要添加<b>备注</b>说明？', 'remark');
      } else {
        // 阶段：完成确认
        this.refreshBudgetPreview();
        this.addAiMessage(
          '✅ 报销信息已采集完毕！<br>请在右侧面板确认信息无误后，点击<b>「确认提交报销单」</b>按钮完成提交。'
        );
        this.currentStep = 99; // 标记完成
      }
    },

    // ========== 消息处理 ==========
    addAiMessage(content, options) {
      this.chatMessages.push({ role: 'ai', content, options: options || [], answered: false, selected: null });
      this.scrollToBottom();
    },
    addAiMessageInput(content, inputType) {
      this.chatMessages.push({ role: 'ai', content, inputType, answered: false });
      this.scrollToBottom();
    },
    addUserMessage(content) {
      this.chatMessages.push({ role: 'user', content });
      this.scrollToBottom();
    },
    handleOptionClick(msg, opt) {
      if (msg.answered) return;
      msg.answered = true;
      msg.selected = opt.value;
      this.addUserMessage(opt.label);

      // 根据当前步骤记录数据
      const step = this.currentStep;
      if (step === 1) {
        this.form.expenseCategory = opt.value;
      } else if (step === 2) {
        this.form.itemCategory = opt.value;
      } else if (step === 3) {
        this.form.purposeTag = opt.value;
        // 用途标签确定后，提前分析映射规则（判断后续是折旧年限还是费率/利息）
        this.analyzeMappingResult();
      } else if (step === 4) {
        this.form.currencyCode = opt.value;
      }
      this.$nextTick(() => this.nextStep());
    },
    submitInput(msg, type) {
      if (type === 'amount') {
        if (!this.inputAmount || this.inputAmount <= 0) {
          this.$message.warning('请输入有效金额');
          return;
        }
        if (this.form.currencyCode !== 'CNY') {
          // 外币：记录原币金额，人民币金额由后端折算
          this.form.originalAmount = this.inputAmount;
          this.form.amount = this.inputAmount; // 暂存，后端会覆盖为折算后金额
          const cur = this.currencyOptions.find(c => c.code === this.form.currencyCode);
          this.addUserMessage((cur ? cur.code + ' ' : '') + Number(this.inputAmount).toFixed(2) + '（' + (cur ? cur.name : '') + '）');
        } else {
          this.form.amount = this.inputAmount;
          this.form.originalAmount = undefined;
          this.addUserMessage('¥' + Number(this.inputAmount).toFixed(2));
        }
      } else if (type === 'usefulLife') {
        if (!this.inputUsefulLife || this.inputUsefulLife <= 0) {
          const label = this.costMethod === 'ACCRUAL' ? '摊销期数（1-600月）' : '折旧年限（1-50年）';
          this.$message.warning('请输入有效的' + label);
          return;
        }
        this.form.usefulLife = this.inputUsefulLife;
        this.addUserMessage(this.inputUsefulLife + (this.costMethod === 'ACCRUAL' ? ' 月' : ' 年'));
      } else if (type === 'period') {
        if (!this.inputPeriod) {
          this.$message.warning('请选择报销年月');
          return;
        }
        this.form.expensePeriod = this.inputPeriod;
        this.periodConfirmed = true;
        this.addUserMessage(this.inputPeriod);
      } else if (type === 'travelDays') {
        this.form.travelDays = this.inputTravelDays || 0;
        if (this.form.travelDays > 0) {
          this.addUserMessage(this.form.travelDays + ' 天');
          this.calculateSubsidyAmount();
        } else {
          this.addUserMessage('无出差');
        }
      } else if (type === 'remark') {
        this.form.remark = this.inputRemark || '';
        this.addUserMessage(this.inputRemark ? this.inputRemark : '无备注');
      }
      msg.answered = true;
      this.$nextTick(() => this.nextStep());
    },
    skipStep(msg) {
      msg.answered = true;
      if (msg.inputType === 'travelDays') {
        this.form.travelDays = 0;
        this.addUserMessage('跳过');
      } else if (msg.inputType === 'remark') {
        this.form.remark = '';
        this.addUserMessage('跳过');
      } else if (msg.inputType === 'usefulLife') {
        // 折旧/计提类必须填写年限/期数，不可跳过
        const label = this.costMethod === 'ACCRUAL' ? '摊销期数' : '折旧年限';
        this.$message.warning(this.costMethod === 'ACCRUAL' ? '计提摊销类必须填写摊销期数' : '固定资产必须填写折旧年限');
        msg.answered = false;
        return;
      }
      this.$nextTick(() => this.nextStep());
    },

    // ========== 业务逻辑 ==========
    async calculateSubsidyAmount() {
      if (!this.form.travelDays || this.form.travelDays <= 0) {
        this.subsidyResult = null;
        return;
      }
      try {
        const res = await calculateSubsidy('TRAVEL', this.form.travelDays);
        this.subsidyResult = res.data;
      } catch (e) {
        this.subsidyResult = null;
      }
    },
    /**
     * 提前分析映射规则：根据物资小类+用途标签判断后续流程分支
     * costMethod=DEPRECIATION → 后续询问折旧年限
     * costMethod=ACCRUAL → 后续询问摊销期数
     * costMethod=FEE_RATE → 提示利息/费率自动计提
     * costMethod=DIRECT → 普通费用，无额外步骤
     */
    async analyzeMappingResult() {
      if (!this.form.itemCategory || !this.form.purposeTag || !this.currentUser.deptId) {
        return;
      }
      try {
        const res = await previewMapping({
          itemCategory: this.form.itemCategory,
          purposeTag: this.form.purposeTag,
          deptId: this.currentUser.deptId
        });
        if (res.data && res.data.passed) {
          this.isFixedAsset = res.data.isFixedAsset === 1;
          this.accountingClass = res.data.accountingClass || '';
          this.costMethod = res.data.costMethod || 'DIRECT';
        } else {
          this.isFixedAsset = false;
          this.accountingClass = '';
          this.costMethod = 'DIRECT';
        }
      } catch (e) {
        this.isFixedAsset = false;
        this.accountingClass = '';
        this.costMethod = 'DIRECT';
      }
      this.analysisDone = true;
    },
    async refreshBudgetPreview() {
      if (!this.form.itemCategory || !this.form.purposeTag || !this.currentUser.deptId) {
        this.budgetPreview = null;
        return;
      }
      try {
        const res = await previewMapping({
          itemCategory: this.form.itemCategory,
          purposeTag: this.form.purposeTag,
          deptId: this.currentUser.deptId,
          budgetPeriod: this.form.expensePeriod
        });
        this.budgetPreview = res.data;
        // 同步更新分析标志（以最终完整预览结果为准）
        if (this.budgetPreview && this.budgetPreview.passed) {
          this.isFixedAsset = this.budgetPreview.isFixedAsset === 1;
          this.accountingClass = this.budgetPreview.accountingClass || '';
          this.costMethod = this.budgetPreview.costMethod || 'DIRECT';
        }
        if (this.budgetPreview && this.budgetPreview.budgetSubject) {
          this.fetchProfitTip(this.budgetPreview.budgetSubject);
        }
      } catch (e) {
        this.budgetPreview = null;
      }
    },
    async fetchProfitTip(budgetSubjectCode) {
      try {
        const res = await getProfitTip(budgetSubjectCode, this.form.amount);
        this.profitTip = res.data;
      } catch (e) {
        this.profitTip = null;
      }
    },
    handleSubmit() {
      if (this.budgetPreview && !this.budgetPreview.passed) {
        this.$message.error(this.budgetPreview.message || '预算科目匹配失败，请联系财务配置规则');
        return;
      }
      this.submitLoading = true;
      addExpenseBill(this.form).then(() => {
        this.$message.success('报销单提交成功！');
        this.$router.push('/fee/expense');
      }).catch(error => {
        console.error('提交失败:', error);
      }).finally(() => {
        this.submitLoading = false;
      });
    },

    // ========== 工具方法 ==========
    getDictLabel(dictOptions, value) {
      if (!value || !dictOptions) return value;
      const dict = dictOptions.find(d => d.value === value);
      return dict ? dict.label : value;
    },
    getPurposeTagName(code) {
      if (!code) return '';
      const tag = this.purposeTags.find(t => t.tagCode === code);
      return tag ? tag.tagName : code;
    },
    getCategoryLabel(code) {
      const map = { PURCHASE: '采购', TRAVEL: '差旅', ENTERTAIN: '招待', OTHER: '其他' };
      return map[code] || code;
    },
    getCurrencyName(code) {
      if (!code) return '';
      const cur = (this.currencyOptions || []).find(c => c.code === code);
      return cur ? cur.name : code;
    },
    getCurrencySymbol(code) {
      const map = { CNY: '¥', USD: '$', EUR: '€', GBP: '£', JPY: '¥' };
      return map[code] || code + ' ';
    },
    scrollToBottom() {
      this.$nextTick(() => {
        const el = this.$refs.chatMessages;
        if (el) el.scrollTop = el.scrollHeight;
      });
    }
  }
};
</script>

<style lang="scss" scoped>
.ai-expense-container {
  padding: 10px;
}
.ai-guided-layout {
  display: flex;
  gap: 16px;
  height: calc(100vh - 120px);
}
.ai-chat-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e8e8e8;
  overflow: hidden;

  .chat-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 16px;
    border-bottom: 1px solid #f0f0f0;
    background: #fafafa;

    .header-left {
      display: flex;
      align-items: center;
      gap: 8px;
      .header-title { font-size: 15px; font-weight: 600; color: #303133; }
    }
  }

  .chat-messages {
    flex: 1;
    overflow-y: auto;
    padding: 16px;

    .chat-msg {
      display: flex;
      margin-bottom: 16px;
      gap: 10px;

      &.user {
        flex-direction: row-reverse;
        .msg-bubble {
          background: #409EFF;
          color: #fff;
          border-radius: 12px 2px 12px 12px;
        }
      }
      &.ai {
        .msg-bubble {
          background: #f4f4f5;
          color: #303133;
          border-radius: 2px 12px 12px 12px;
          border: 1px solid #e8e8e8;
        }
      }

      .msg-body {
        max-width: 75%;

        .msg-bubble {
          padding: 10px 14px;
          font-size: 14px;
          line-height: 1.6;
          word-break: break-word;
        }
        .msg-bubble.typing {
          display: flex; gap: 4px; padding: 14px 18px;
          span {
            width: 7px; height: 7px; background: #909399; border-radius: 50%;
            animation: blink 1.4s infinite both;
            &:nth-child(2) { animation-delay: 0.2s; }
            &:nth-child(3) { animation-delay: 0.4s; }
          }
        }
        .msg-options {
          margin-top: 8px;
          display: flex;
          flex-wrap: wrap;
          gap: 6px;
        }
        .msg-input {
          margin-top: 8px;
          display: flex;
          align-items: center;
          flex-wrap: wrap;
        }
      }
    }
  }
}
.form-preview-panel {
  width: 360px;
  flex-shrink: 0;
  overflow-y: auto;

  .preview-card {
    border-radius: 8px;
    ::v-deep .el-card__header { font-weight: 600; font-size: 15px; }
  }
  .placeholder-text { color: #c0c4cc; font-size: 13px; }
}
@keyframes blink {
  0%, 80%, 100% { opacity: 0; }
  40% { opacity: 1; }
}
</style>
