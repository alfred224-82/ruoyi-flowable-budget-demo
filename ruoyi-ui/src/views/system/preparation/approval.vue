<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="年度" prop="budgetYear">
        <el-date-picker
          v-model="queryParams.budgetYear"
          type="year"
          placeholder="选择年度"
          value-format="yyyy"
          clearable
          style="width: 200px"
        />
      </el-form-item>
      <el-form-item label="月份" prop="budgetMonth">
        <el-select v-model="queryParams.budgetMonth" placeholder="请选择月份" clearable style="width: 150px">
          <el-option label="1月" value="1" />
          <el-option label="2月" value="2" />
          <el-option label="3月" value="3" />
          <el-option label="4月" value="4" />
          <el-option label="5月" value="5" />
          <el-option label="6月" value="6" />
          <el-option label="7月" value="7" />
          <el-option label="8月" value="8" />
          <el-option label="9月" value="9" />
          <el-option label="10月" value="10" />
          <el-option label="11月" value="11" />
          <el-option label="12月" value="12" />
        </el-select>
      </el-form-item>
      <el-form-item label="部门" prop="orgId" v-if="showDeptFilter">
        <el-tree-select
          v-model="queryParams.orgId"
          :data="deptOptions"
          :props="{ value: 'id', label: 'label', children: 'children' }"
          value-key="id"
          placeholder="请选择部门"
          check-strictly
          clearable
          style="width: 250px"
        />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-check"
          size="mini"
          @click="handleBatchApprove"
          v-hasPermi="['system:preparation:approve']"
        >批量审批</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="preparationList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" :selectable="isSelectable" />
      <el-table-column label="预算单号" align="center" prop="sheetNo" width="180" />
      <el-table-column label="年月" align="center" prop="budgetPeriod" width="120" />
      <el-table-column label="组织名称" align="center" prop="orgName" width="150" />
      <el-table-column label="预算总额" align="center" prop="totalBudget" width="150">
        <template slot-scope="scope">
          {{ formatAmount(scope.row.totalBudget) }}
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="120">
        <template slot-scope="scope">
          <el-tag :type="getStatusType(scope.row.status)">
            {{ getStatusLabel(scope.row.status) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="审批阶段" align="center" prop="approvalStage" width="130">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.approvalStage && scope.row.approvalStage !== 'None'" :type="getStageType(scope.row.approvalStage)" size="small">
            {{ getStageLabel(scope.row.approvalStage) }}
          </el-tag>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column label="当前处理人" align="center" prop="currentHandler" width="120" />
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}:{s}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="120">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleView(scope.row)"
            v-hasPermi="['system:preparation:query']"
          >{{ scope.row.status === 'Approved' || scope.row.status === 'Rejected' ? '查看' : '审核' }}</el-button>
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

    <!-- 批量审批对话框 -->
    <el-dialog title="批量审批" :visible.sync="batchApproveOpen" width="500px" append-to-body>
      <el-form ref="batchApproveForm" :model="batchApproveForm" label-width="100px">
        <el-form-item label="审批意见">
          <el-input
            v-model="batchApproveForm.remark"
            type="textarea"
            :rows="4"
            placeholder="请输入审批意见（可选）"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitBatchApprove">确 定</el-button>
        <el-button @click="batchApproveOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listPreparation, batchApprove } from "@/api/system/preparation";
import { treeselect } from "@/api/system/dept";

export default {
  name: "PreparationApproval",
  data() {
    return {
      // 遮罩层
      loading: true,
      // 选中数组
      ids: [],
      // 非多个禁用
      multiple: true,
      // 显示搜索条件
      showSearch: true,
      // 总条数
      total: 0,
      // 预算编制表格数据
      preparationList: [],
      // 部门树选项
      deptOptions: [],
      // 是否显示部门过滤
      showDeptFilter: false,
      // 用户角色
      userRoles: [],
      // 用户部门ID
      userDeptId: null,
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        budgetYear: undefined,
        budgetMonth: undefined,
        orgId: undefined,
        status: undefined // 默认查询所有待审核状态
      },
      // 批量审批对话框
      batchApproveOpen: false,
      batchApproveForm: {
        remark: ''
      },
      // 选中的行数据（用于校验）
      selectedRows: []
    };
  },
  created() {
    this.getUserInfo();
    this.getList();
  },
  methods: {
    /** 获取用户信息 */
    getUserInfo() {
      this.userRoles = this.$store.state.user.roles || [];
      this.userDeptId = this.$store.state.user.deptId;
      
      // 根据角色判断是否显示部门过滤
      // 总公司角色：有所有部门权限
      // 分公司角色：只有分公司下所有部门
      // 部门领导：只有自己部门
      if (this.userRoles.includes('admin') || this.userRoles.includes('hq_manager')) {
        // 总公司管理员或总部管理者，显示所有部门
        this.showDeptFilter = true;
        this.loadDeptTree();
      } else if (this.userRoles.includes('branch_manager')) {
        // 分公司管理者，显示分公司下所有部门
        this.showDeptFilter = true;
        this.loadDeptTree(this.userDeptId);
      } else {
        // 部门领导，只显示自己部门
        this.showDeptFilter = true;
        this.queryParams.orgId = this.userDeptId;
      }
    },
    /** 加载部门树 */
    loadDeptTree(parentDeptId) {
      const params = parentDeptId ? { parentId: parentDeptId } : {};
      treeselect(params).then(response => {
        this.deptOptions = response.data;
      });
    },
    /** 查询预算编制列表 */
    getList() {
      this.loading = true;
      listPreparation(this.queryParams).then(response => {
        this.preparationList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      // 重新设置默认状态
      this.queryParams.status = undefined;
      // 如果是部门领导，恢复部门限制
      if (!this.userRoles.includes('admin') && 
          !this.userRoles.includes('hq_manager') && 
          !this.userRoles.includes('branch_manager')) {
        this.queryParams.orgId = this.userDeptId;
      }
      this.handleQuery();
    },
    // 多选框选中数据
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id);
      this.multiple = !selection.length;
      
      // 保存选中的完整数据用于校验
      this.selectedRows = selection;
    },
    /** 判断行是否可勾选（仅待审核状态可勾选） */
    isSelectable(row) {
      return row.status === 'Pending_Review';
    },
    /** 查看/审核按钮操作 */
    handleView(row) {
      this.$router.push({ path: '/system/preparation/approvalDetail', query: { id: row.id } });
    },
    /** 批量审批按钮操作 */
    handleBatchApprove() {
      if (this.ids.length === 0) {
        this.$modal.msgWarning("请选择要审批的编制");
        return;
      }
      
      // 校验选中的记录是否都是待审核状态
      const nonPendingItems = this.selectedRows.filter(row => row.status !== 'Pending_Review');
      if (nonPendingItems.length > 0) {
        const invalidStatuses = nonPendingItems.map(row => this.getStatusLabel(row.status)).join('、');
        this.$modal.msgError(`只能审批“待审核”状态的记录，当前选中包含 ${invalidStatuses} 状态的记录`);
        return;
      }
      
      this.batchApproveForm.remark = '';
      this.batchApproveOpen = true;
    },
    /** 提交批量审批 */
    submitBatchApprove() {
      this.$modal.confirm(`确认批量审批选中的 ${this.ids.length} 条编制？`).then(() => {
        return batchApprove({
          ids: this.ids,
          remark: this.batchApproveForm.remark
        });
      }).then(() => {
        this.batchApproveOpen = false;
        this.getList();
        this.$modal.msgSuccess("批量审批成功");
      }).catch(() => {});
    },
    /** 获取状态标签类型 */
    getStatusType(status) {
      const statusMap = {
        'Draft': 'info',
        'Pending_Review': 'warning',
        'Approved': 'success',
        'Rejected': 'danger',
        'Pending_Revision': 'warning'
      };
      return statusMap[status] || 'info';
    },
    /** 获取状态标签文本 */
    getStatusLabel(status) {
      const statusMap = {
        'Draft': '草稿',
        'Pending_Review': '待审核',
        'Approved': '已通过',
        'Rejected': '已驳回',
        'Pending_Revision': '待修订'
      };
      return statusMap[status] || status;
    },
    /** 获取审批阶段标签 */
    getStageLabel(stage) {
      const stageMap = {
        'Dept': '部门领导',
        'Branch': '分公司领导',
        'HQ': '总公司领导'
      };
      return stageMap[stage] || stage;
    },
    /** 获取审批阶段标签类型 */
    getStageType(stage) {
      return 'warning';
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
