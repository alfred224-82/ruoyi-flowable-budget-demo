<template>
  <div class="app-container">
    <!-- 搜索栏 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="申请单号" prop="appNo">
        <el-input
          v-model="queryParams.appNo"
          placeholder="请输入申请单号"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="部门审批中" value="DEPT_APPROVING" />
          <el-option label="分公司审批中" value="BRANCH_APPROVING" />
          <el-option label="总公司审批中" value="HQ_APPROVING" />
          <el-option label="已通过" value="APPROVED" />
          <el-option label="已驳回" value="REJECTED" />
          <el-option label="已撤回" value="CANCELLED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 操作按钮栏 -->
    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['fee:budgetAdjustment:apply']"
        >新增申请</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 列表 -->
    <el-table v-loading="loading" :data="list" border>
      <el-table-column label="申请单号" prop="appNo" width="160" :show-overflow-tooltip="true" />
      <el-table-column label="申请人" prop="applicantName" width="100" />
      <el-table-column label="目标科目" prop="targetSubjectName" min-width="130" :show-overflow-tooltip="true">
        <template slot-scope="scope">
          {{ scope.row.targetSubjectName || scope.row.targetSubject }}
        </template>
      </el-table-column>
      <el-table-column label="申请金额" prop="requestAmount" width="110" align="right">
        <template slot-scope="scope">
          <span v-if="scope.row.requestAmount != null">¥{{ Number(scope.row.requestAmount).toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="预算可用余额" prop="currentBalance" width="120" align="right">
        <template slot-scope="scope">
          <span v-if="scope.row.currentBalance != null">¥{{ Number(scope.row.currentBalance).toFixed(2) }}</span>
        </template>
      </el-table-column>
      <el-table-column label="当前层级" prop="currentLevel" width="90" align="center">
        <template slot-scope="scope">
          {{ levelMap[scope.row.currentLevel] || scope.row.currentLevel }}
        </template>
      </el-table-column>
      <el-table-column label="部门名称" prop="deptName" width="130" />
      <el-table-column label="状态" width="120" align="center">
        <template slot-scope="scope">
          <el-tag :type="statusTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="申请时间" prop="createTime" width="150" align="center">
        <template slot-scope="scope">
          {{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="80" fixed="right" align="center">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleView(scope.row)"
          >查看</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      :page.sync="queryParams.pageNum"
      :limit.sync="queryParams.pageSize"
      @pagination="getList"
    />

    <!-- 新增/查看申请弹窗 -->
    <el-dialog :title="viewMode ? '查看预算调整申请' : '新增预算调整申请'" :visible.sync="addDialogVisible" width="560px" append-to-body>
      <el-form ref="addForm" :model="addForm" :rules="addRules" label-width="120px">
        <el-form-item label="物资小类" prop="itemCategory">
          <el-select
            v-model="addForm.itemCategory"
            :disabled="viewMode"
            placeholder="请选择物资小类"
            style="width: 100%"
            @change="onMappingFieldChange"
          >
            <el-option
              v-for="dict in itemCategoryOptions"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="用途标签" prop="purposeTag">
          <el-select
            v-model="addForm.purposeTag"
            :disabled="viewMode"
            placeholder="请选择用途标签"
            style="width: 100%"
            @change="onMappingFieldChange"
          >
            <el-option
              v-for="tag in purposeTags"
              :key="tag.tagCode"
              :label="tag.tagName + ' ' + tag.tagIcon"
              :value="tag.tagCode"
            />
          </el-select>
        </el-form-item>
        <!-- 预算归属预览 -->
        <el-form-item v-if="budgetPreview" label="预算归属预览">
          <el-alert
            :title="budgetPreview.passed ? '预算科目匹配成功' : '预算科目匹配失败'"
            :type="budgetPreview.passed ? 'success' : 'error'"
            :closable="false"
            show-icon
          >
            <template v-if="budgetPreview.passed">
              <p>预算科目：{{ budgetPreview.budgetSubjectName || budgetPreview.budgetSubject }}</p>
              <p v-if="budgetPreview.remainingBudget !== null && budgetPreview.remainingBudget !== undefined">
                可用余额：¥{{ Number(budgetPreview.remainingBudget).toFixed(2) }}
              </p>
            </template>
            <template v-else>
              <p>{{ budgetPreview.message }}</p>
            </template>
          </el-alert>
        </el-form-item>
        <el-form-item label="申请调整金额" prop="requestAmount">
          <el-input-number v-model="addForm.requestAmount" :min="0.01" :precision="2" :disabled="viewMode" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="调整原因" prop="reason">
          <el-input v-model="addForm.reason" type="textarea" :rows="3" :disabled="viewMode" placeholder="请详细说明预算调整原因（5-500字符）"
            :maxlength="viewMode ? undefined : 500" :show-word-limit="!viewMode" />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button v-if="!viewMode" type="primary" @click="submitAdd" :loading="submitting">提交申请</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listBudgetAdjustment, applyBudgetAdjustment, getBudgetAdjustment } from "@/api/system/fee/budgetAdjustment";
import { allEnabledPurposeTag } from "@/api/system/fee/purposeTag";
import { previewMapping } from "@/api/system/fee/mappingRule";
import { getUserProfile } from "@/api/system/user";

export default {
  name: "BudgetAdjustmentApproval",
  dicts: ['fee_item_category'],
  data() {
    return {
      list: [],
      total: 0,
      loading: false,
      showSearch: true,
      queryParams: { pageNum: 1, pageSize: 10, appNo: undefined, status: undefined },
      levelMap: { 1: '部门', 2: '分公司', 3: '总公司' },
      // 用途标签选项
      purposeTags: [],
      // 物资小类字典选项
      itemCategoryOptions: [],
      // 当前用户信息
      currentUser: {},
      // 预算归属预览结果
      budgetPreview: null,
      // 新增/查看弹窗
      addDialogVisible: false,
      viewMode: false,
      submitting: false,
      addForm: {
        itemCategory: '',
        purposeTag: '',
        requestAmount: 0,
        reason: ''
      },
      addRules: {
        itemCategory: [{ required: true, message: '请选择物资小类', trigger: 'change' }],
        purposeTag: [{ required: true, message: '请选择用途标签', trigger: 'change' }],
        requestAmount: [{ required: true, message: '请输入申请调整金额', trigger: 'blur' }],
        reason: [
          { required: true, message: '请输入调整原因', trigger: 'blur' },
          { min: 5, max: 500, message: '调整原因长度需在5-500字符之间', trigger: 'blur' }
        ]
      }
    }
  },
  created() {
    this.getList();
    this.loadPurposeTags();
    this.loadCurrentUser();
    this.itemCategoryOptions = this.dict.type.fee_item_category || [];
  },
  methods: {
    async getList() {
      this.loading = true;
      try {
        const res = await listBudgetAdjustment(this.queryParams);
        this.list = res.rows || [];
        this.total = res.total || 0;
      } finally { this.loading = false }
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    // 加载用途标签
    loadPurposeTags() {
      allEnabledPurposeTag().then(response => {
        this.purposeTags = response.data || [];
      });
    },
    // 加载当前用户信息
    loadCurrentUser() {
      getUserProfile().then(response => {
        // getUserProfile 返回 { user, roleGroup, postGroup }，取 user 对象
        this.currentUser = (response.data && response.data.user) || {};
      });
    },
    // 物资小类/用途标签变化时触发预览
    async onMappingFieldChange() {
      if (!this.addForm.itemCategory || !this.addForm.purposeTag) {
        this.budgetPreview = null;
        return;
      }
      try {
        const res = await previewMapping({
          itemCategory: this.addForm.itemCategory,
          purposeTag: this.addForm.purposeTag,
          deptId: this.currentUser.deptId
        });
        this.budgetPreview = res.data;
      } catch (error) {
        console.error('预算归属预览失败:', error);
        this.budgetPreview = null;
      }
    },
    // 新增申请
    handleAdd() {
      this.viewMode = false;
      this.budgetPreview = null;
      this.addForm = {
        itemCategory: '',
        purposeTag: '',
        requestAmount: 0,
        reason: ''
      };
      this.addDialogVisible = true;
      this.$nextTick(() => {
        this.$refs.addForm && this.$refs.addForm.clearValidate();
      });
    },
    submitAdd() {
      this.$refs.addForm.validate(async (valid) => {
        if (!valid) return;
        // 前端校验映射规则是否匹配
        if (!this.budgetPreview || !this.budgetPreview.passed) {
          this.$message.error(this.budgetPreview ? this.budgetPreview.message : '请先选择物资小类和用途标签');
          return;
        }
        this.submitting = true;
        try {
          await applyBudgetAdjustment(this.addForm);
          this.$message.success('预算调整申请已提交');
          this.addDialogVisible = false;
          this.getList();
        } catch (e) {
          console.error(e);
        } finally {
          this.submitting = false;
        }
      });
    },
    // 查看申请（只读弹窗）
    async handleView(row) {
      this.viewMode = true;
      this.budgetPreview = null;
      try {
        const res = await getBudgetAdjustment(row.id);
        const data = res.data || {};
        this.addForm = {
          itemCategory: data.itemCategory || '',
          purposeTag: data.purposeTag || '',
          requestAmount: data.requestAmount || 0,
          reason: data.reason || ''
        };
        // 加载预览信息（只读模式也显示归属结果）
        if (data.itemCategory && data.purposeTag) {
          this.onMappingFieldChange();
        }
        this.addDialogVisible = true;
      } catch (e) {
        console.error('加载申请详情失败', e);
      }
    },
    statusTagType(status) {
      const map = {
        'DEPT_APPROVING': 'warning', 'BRANCH_APPROVING': 'warning', 'HQ_APPROVING': 'warning',
        'APPROVED': 'success', 'REJECTED': 'danger', 'ESCALATED': 'info', 'CANCELLED': 'info'
      };
      return map[status] || 'info';
    },
    statusLabel(status) {
      const map = {
        'DEPT_APPROVING': '部门审批中', 'BRANCH_APPROVING': '分公司审批中', 'HQ_APPROVING': '总公司审批中',
        'APPROVED': '已通过', 'REJECTED': '已驳回', 'ESCALATED': '已转审', 'CANCELLED': '已撤回'
      };
      return map[status] || status;
    }
  }
}
</script>
