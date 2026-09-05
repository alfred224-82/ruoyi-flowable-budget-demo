<template>
  <div class="app-container">
    <el-form :inline="true" size="small" label-width="80px">
      <el-form-item label="预算期间">
        <el-date-picker
          v-model="period"
          type="month"
          placeholder="选择预算月份"
          value-format="yyyy-MM"
          :clearable="false"
          style="width: 200px"
          @change="loadQuotas"
        />
      </el-form-item>
      <!-- 年月+部门维度：编制部门选择器 -->
      <el-form-item label="编制部门">
        <!-- 高级版起：部门树选择器 -->
        <treeselect
          v-if="isAdvancedEdition && deptOptions.length > 0"
          v-model="departmentId"
          :options="deptOptions"
          :normalizer="deptNormalizer"
          placeholder="请选择编制部门"
          :clearable="false"
          style="width: 220px"
          @input="onDeptChange"
        />
        <!-- 基础版：只读标签 -->
        <el-tag v-else size="medium">{{ departmentName || '总公司' }}</el-tag>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="loadQuotas">查询</el-button>
      </el-form-item>
      <el-form-item v-if="sheetNo">
        <el-tag size="medium">预算单号：{{ sheetNo }}</el-tag>
        <el-tag size="medium" :type="getStatusType(status)" style="margin-left: 8px">
          {{ getStatusLabel(status) }}
        </el-tag>
      </el-form-item>
    </el-form>

    <el-alert
      v-if="loaded && !editable"
      :title="'当前期间预算单已进入「' + getStatusLabel(status) + '」状态，大类额度不可修改'"
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom: 12px"
    />

    <el-alert
      v-if="loaded && editable && hasUsageData"
      title="预算金额调整下限控制：当大类预算已使用 X% 时，调整后的新预算金额不得低于原金额的 X%（即不得低于已使用金额）"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 12px"
    />

    <el-table v-loading="loading" :data="quotas" border>
      <el-table-column label="序号" type="index" width="60" align="center" />
      <el-table-column label="大类编码" align="center" prop="categoryCode" width="140" />
      <el-table-column label="费用大类" align="center" prop="categoryName" min-width="140" />
      <el-table-column label="预算额度（元）" align="center" min-width="200">
        <template slot-scope="scope">
          <el-input-number
            v-model="scope.row.amount"
            :min="getMinAmount(scope.row)"
            :precision="2"
            :step="1000"
            :controls="false"
            :disabled="!editable"
            :style="getAmountStyle(scope.row)"
          />
        </template>
      </el-table-column>
      <el-table-column v-if="hasUsageData" label="已使用（元）" align="center" width="130">
        <template slot-scope="scope">
          <span :style="{ color: getUsageColor(scope.row) }">{{ formatAmount(getUsed(scope.row)) }}</span>
        </template>
      </el-table-column>
      <el-table-column v-if="hasUsageData" label="使用率" align="center" width="100">
        <template slot-scope="scope">
          <el-tag v-if="getUsageRate(scope.row) > 0" :type="getUsageTagType(scope.row)" size="small">
            {{ getUsageRate(scope.row) }}%
          </el-tag>
          <span v-else style="color: #909399">-</span>
        </template>
      </el-table-column>
      <el-table-column v-if="hasUsageData" label="调整下限（元）" align="center" width="130">
        <template slot-scope="scope">
          <span v-if="getMinAmount(scope.row) > 0" style="color: #E6A23C; font-weight: bold">
            {{ formatAmount(getMinAmount(scope.row)) }}
          </span>
          <span v-else style="color: #909399">不限</span>
        </template>
      </el-table-column>
    </el-table>

    <el-form label-width="80px" style="margin-top: 16px">
      <el-form-item label="备注" v-if="editable">
        <el-input
          v-model="remark"
          type="textarea"
          :rows="2"
          maxlength="500"
          show-word-limit
          placeholder="请输入备注"
          style="width: 600px"
        />
      </el-form-item>
      <el-form-item label="合计">
        <span class="total-amount">￥ {{ formatAmount(totalAmount) }}</span>
      </el-form-item>
    </el-form>

    <el-row v-if="editable">
      <el-button
        type="primary"
        icon="el-icon-check"
        :loading="saving"
        @click="handleSave"
        v-hasPermi="['system:preparation:categoryQuotaSave']"
      >保存大类额度</el-button>
    </el-row>
  </div>
</template>

<script>
import { getCategoryQuota, saveCategoryQuota } from "@/api/system/preparation";
import { listDept } from "@/api/system/dept";
import { hasEdition } from "@/utils/feature";
import Treeselect from '@riophae/vue-treeselect';
import '@riophae/vue-treeselect/dist/vue-treeselect.css';

export default {
  name: "CategoryQuota",
  components: { Treeselect },
  data() {
    const now = new Date();
    return {
      // 预算期间（yyyy-MM）
      period: now.getFullYear() + "-" + String(now.getMonth() + 1).padStart(2, "0"),
      // 遮罩层
      loading: false,
      // 保存中
      saving: false,
      // 是否已加载过
      loaded: false,
      // 关联编制单信息
      preparationId: undefined,
      sheetNo: undefined,
      status: undefined,
      // 是否可编辑（无单/草稿/驳回）
      editable: false,
      // 大类额度列表
      quotas: [],
      // 备注
      remark: undefined,
      // T5.13: 各大类使用率统计（categoryCode → {quota, used, usageRate, minAmount}）
      usageStats: {},
      // 年月+部门维度
      departmentId: this.$store.state.user.deptId || null,
      departmentName: this.$store.state.user.deptName || '总公司',
      deptOptions: [],
      isAdvancedEdition: hasEdition('ADVANCED')
    };
  },
  computed: {
    budgetYear() {
      return this.period ? parseInt(this.period.substring(0, 4), 10) : undefined;
    },
    budgetMonth() {
      return this.period ? parseInt(this.period.substring(5, 7), 10) : undefined;
    },
    totalAmount() {
      return this.quotas.reduce((sum, item) => sum + Number(item.amount || 0), 0);
    },
    hasUsageData() {
      return Object.keys(this.usageStats).length > 0;
    }
  },
  created() {
    this.loadQuotas();
    // 高级版起加载部门树
    if (this.isAdvancedEdition) {
      this.loadDeptTree();
    }
  },
  methods: {
    /** 加载部门树（高级版起） */
    loadDeptTree() {
      listDept({ status: '0' }).then(res => {
        this.deptOptions = this.buildDeptTree(res.data || [], 0);
      });
    },
    buildDeptTree(list, parentId) {
      return list.filter(d => d.parentId === parentId).map(d => ({
        id: d.deptId,
        label: d.deptName,
        children: this.buildDeptTree(list, d.deptId)
      }));
    },
    deptNormalizer(node) {
      if (node.children && !node.children.length) delete node.children;
      return { id: node.id, label: node.label, children: node.children };
    },
    /** 部门切换时重新加载额度 */
    onDeptChange() {
      // 从部门树解析部门名称
      const found = this.findDeptName(this.deptOptions, this.departmentId);
      this.departmentName = found || '总公司';
      this.loadQuotas();
    },
    findDeptName(tree, targetId) {
      for (const node of tree) {
        if (node.id === targetId) return node.label;
        if (node.children) {
          const found = this.findDeptName(node.children, targetId);
          if (found) return found;
        }
      }
      return null;
    },
    /** 查询当期大类额度 */
    loadQuotas() {
      if (!this.period) {
        this.$modal.msgWarning("请先选择预算期间");
        return;
      }
      this.loading = true;
      getCategoryQuota(this.budgetYear, this.budgetMonth, this.departmentId).then(response => {
        const data = response.data || {};
        this.preparationId = data.preparationId;
        this.sheetNo = data.sheetNo;
        this.status = data.status;
        this.editable = !!data.editable;
        this.quotas = (data.quotas || []).map(item => ({
          categoryCode: item.categoryCode,
          categoryName: item.categoryName,
          amount: item.amount
        }));
        // T5.13: 加载使用率统计
        this.usageStats = data.usageStats || {};
        // 同步后端返回的部门信息
        if (data.departmentName) {
          this.departmentName = data.departmentName;
        }
        this.loaded = true;
      }).finally(() => {
        this.loading = false;
      });
    },
    /** 保存大类额度 */
    handleSave() {
      if (!this.quotas || this.quotas.length === 0) {
        this.$modal.msgWarning("暂无可保存的大类额度");
        return;
      }
      const invalid = this.quotas.find(item => item.amount === undefined || item.amount === null || item.amount === "");
      if (invalid) {
        this.$modal.msgWarning("请为大类「" + invalid.categoryName + "」填写预算额度");
        return;
      }
      const data = {
        budgetYear: this.budgetYear,
        budgetMonth: this.budgetMonth,
        departmentId: this.departmentId,
        quotas: this.quotas.map(item => ({
          categoryCode: item.categoryCode,
          categoryName: item.categoryName,
          amount: item.amount
        })),
        remark: this.remark
      };
      this.$modal.confirm("确认保存 " + this.budgetYear + "年" + this.budgetMonth + "月「" + this.departmentName + "」各大类预算额度（合计 ￥" + this.formatAmount(this.totalAmount) + "）？保存后系统将自动生成预算科目拆分初稿。").then(() => {
        this.saving = true;
        return saveCategoryQuota(data);
      }).then((response) => {
        const result = response.data || {};
        let msg = '保存成功';
        // T5.14: 展示自动拆分结果
        if (result.splitResult) {
          const detailCount = result.splitResult.detailCount || 0;
          const skipped = result.splitResult.skippedCategories || [];
          msg += '，系统已自动生成 ' + detailCount + ' 条预算科目明细';
          if (skipped.length > 0) {
            msg += '（' + skipped.length + ' 个大类暂无科目映射，已跳过）';
          }
        }
        this.$modal.msgSuccess(msg);
        this.loadQuotas();
      }).catch(() => {}).finally(() => {
        this.saving = false;
      });
    },
    /** 状态标签 */
    getStatusLabel(status) {
      const map = {
        Draft: "草稿",
        Completed: "完成编制",
        Pending_Review: "待审核",
        Pending_Revision: "待修订",
        Approved: "已通过",
        Rejected: "已驳回"
      };
      return map[status] || status || "-";
    },
    getStatusType(status) {
      const map = {
        Draft: "info",
        Completed: "warning",
        Pending_Review: "warning",
        Pending_Revision: "warning",
        Approved: "success",
        Rejected: "danger"
      };
      return map[status] || "info";
    },
    /** 金额格式化 */
    formatAmount(amount) {
      if (amount === undefined || amount === null || amount === "") {
        return "0.00";
      }
      return Number(amount).toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    },
    // ===== T5.13 使用率辅助方法 =====
    /** 获取大类已使用金额 */
    getUsed(row) {
      const stats = this.usageStats[row.categoryCode];
      return stats ? Number(stats.used || 0) : 0;
    },
    /** 获取使用率 */
    getUsageRate(row) {
      const stats = this.usageStats[row.categoryCode];
      return stats ? Number(stats.usageRate || 0) : 0;
    },
    /** 获取调整下限 */
    getMinAmount(row) {
      const stats = this.usageStats[row.categoryCode];
      return stats ? Number(stats.minAmount || 0) : 0;
    },
    /** 输入框动态最小值（有使用率数据时取下限，否则取0） */
    getInputMin(row) {
      if (!this.hasUsageData) return 0;
      return this.getMinAmount(row);
    },
    /** 输入框样式（接近下限时变黄，低于或等于下限时变红） */
    getAmountStyle(row) {
      const min = this.getMinAmount(row);
      const amount = Number(row.amount || 0);
      if (min > 0 && amount <= min) return 'width: 200px';
      return 'width: 200px';
    },
    /** 使用率颜色 */
    getUsageColor(row) {
      const rate = this.getUsageRate(row);
      if (rate >= 100) return '#F56C6C';
      if (rate >= 95) return '#E6A23C';
      if (rate >= 85) return '#E6A23C';
      return '#67C23A';
    },
    /** 使用率标签类型 */
    getUsageTagType(row) {
      const rate = this.getUsageRate(row);
      if (rate >= 100) return 'danger';
      if (rate >= 85) return 'warning';
      return 'success';
    }
  }
};
</script>

<style scoped>
.total-amount {
  font-size: 18px;
  font-weight: bold;
  color: #f56c6c;
}
</style>
