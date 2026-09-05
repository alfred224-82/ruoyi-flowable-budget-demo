<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="规则编码" prop="ruleCode">
        <el-input
          v-model="queryParams.ruleCode"
          placeholder="请输入规则编码"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="物资小类" prop="itemCategory">
        <el-select v-model="queryParams.itemCategory" placeholder="请选择物资小类" clearable>
          <el-option
            v-for="dict in itemCategoryOptions"
            :key="dict.value"
            :label="dict.label"
            :value="dict.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="用途标签" prop="purposeTag">
        <el-select v-model="queryParams.purposeTag" placeholder="请选择用途标签" clearable>
          <el-option
            v-for="tag in purposeTags"
            :key="tag.tagCode"
            :label="tag.tagName"
            :value="tag.tagCode"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="启用" value="ENABLED" />
          <el-option label="禁用" value="DISABLED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button
          type="primary"
          plain
          icon="el-icon-plus"
          size="mini"
          @click="handleAdd"
          v-hasPermi="['fee:mappingRule:add']"
        >新增</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="success"
          plain
          icon="el-icon-edit"
          size="mini"
          :disabled="single"
          @click="handleUpdate"
          v-hasPermi="['fee:mappingRule:edit']"
        >修改</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="danger"
          plain
          icon="el-icon-delete"
          size="mini"
          :disabled="multiple"
          @click="handleDelete"
          v-hasPermi="['fee:mappingRule:remove']"
        >删除</el-button>
      </el-col>
      <el-col :span="1.5">
        <el-button
          type="info"
          plain
          icon="el-icon-upload2"
          size="mini"
          @click="handleImport"
          v-hasPermi="['fee:mappingRule:import']"
        >导入</el-button>
      </el-col>
      <el-col :span="1.5" v-if="canRuleCustom">
        <el-button
          type="warning"
          plain
          icon="el-icon-document"
          size="mini"
          @click="handleAuditOpen"
          v-hasPermi="['fee:mappingRule:list']"
        >变更审计</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <!-- 规则版本灰度面板（专业版 FEAT_RULE_GRAYSCALE） -->
    <el-card v-if="canGrayscale" shadow="never" style="margin-bottom: 12px;">
      <div slot="header">
        <span>规则版本与灰度发布（新规则默认10%灰度，比对命中率后再全量）</span>
        <el-button style="float: right; margin-left: 10px;" type="primary" size="mini" icon="el-icon-s-promotion" @click="publishOpen = true" v-hasPermi="['fee:mappingRule:publish']">发布新版本</el-button>
        <el-button style="float: right;" size="mini" icon="el-icon-magic-stick" @click="recommendOpen = true">AI语义推荐</el-button>
      </div>
      <el-table :data="ruleVersions" size="mini" v-loading="versionLoading" max-height="280">
        <el-table-column label="版本" width="80" align="center">
          <template slot-scope="scope">V{{ scope.row.versionNo }}</template>
        </el-table-column>
        <el-table-column label="规则数" width="80" align="center" prop="ruleCount" />
        <el-table-column label="灰度比例" width="100" align="center">
          <template slot-scope="scope">{{ scope.row.grayscalePct }}%</template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template slot-scope="scope">
            <el-tag size="mini" :type="versionStatusType(scope.row.grayscaleStatus)">{{ versionStatusText(scope.row.grayscaleStatus) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="命中率" width="100" align="center">
          <template slot-scope="scope">{{ scope.row.hitRate == null ? '-' : (scope.row.hitRate * 100).toFixed(2) + '%' }}</template>
        </el-table-column>
        <el-table-column label="发布时间" min-width="150" align="center" prop="publishAt" />
        <el-table-column label="操作" width="230" align="center">
          <template slot-scope="scope">
            <el-button size="mini" type="text" @click="handleCompare(scope.row)">比对命中率</el-button>
            <el-button v-if="scope.row.grayscaleStatus === 'GRAYSCALE'" size="mini" type="text" @click="handleFullRelease(scope.row)" v-hasPermi="['fee:mappingRule:publish']">全量发布</el-button>
            <el-button v-if="scope.row.grayscaleStatus !== 'ROLLBACK'" size="mini" type="text" style="color:#F56C6C;" @click="handleRollback(scope.row)" v-hasPermi="['fee:mappingRule:publish']">回滚</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-table v-loading="loading" :data="mappingRuleList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="规则编码" align="center" prop="ruleCode" width="120" />
      <el-table-column label="物资小类" align="center" prop="itemCategory">
        <template slot-scope="scope">
          {{ getDictLabel(itemCategoryOptions, scope.row.itemCategory) }}
        </template>
      </el-table-column>
      <el-table-column label="用途标签" align="center" prop="purposeTag" />
      <el-table-column label="目标预算科目" align="center" prop="targetBudgetSubject" />
      <el-table-column label="会计大类" align="center" prop="accountingClass">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.accountingClass === 'EXPENSE'" type="warning">费用类</el-tag>
          <el-tag v-else-if="scope.row.accountingClass === 'COST'" type="success">成本类</el-tag>
          <el-tag v-else-if="scope.row.accountingClass === 'ASSET'" type="primary">资产类</el-tag>
          <el-tag v-else>{{ scope.row.accountingClass }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="是否固定资产" align="center" prop="isFixedAsset" width="120">
        <template slot-scope="scope">
          <el-tag :type="scope.row.isFixedAsset === 1 ? 'danger' : 'info'">
            {{ scope.row.isFixedAsset === 1 ? '是' : '否' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="费用确认方式" align="center" prop="costMethod" width="120">
        <template slot-scope="scope">
          <el-tag v-if="scope.row.costMethod === 'DEPRECIATION'" type="danger">折旧</el-tag>
          <el-tag v-else-if="scope.row.costMethod === 'ACCRUAL'" type="warning">计提</el-tag>
          <el-tag v-else-if="scope.row.costMethod === 'FEE_RATE'" type="info">费率</el-tag>
          <el-tag v-else type="success">直支</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 'ENABLED' ? 'success' : 'danger'">
            {{ scope.row.status === 'ENABLED' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column v-if="canRuleCustom" label="发布状态" align="center" prop="publishStatus" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.publishStatus === 'PUBLISHED' ? 'success' : 'info'" size="small">
            {{ publishStatusText(scope.row.publishStatus) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" :width="canRuleCustom ? 260 : 150">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['fee:mappingRule:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['fee:mappingRule:remove']"
          >删除</el-button>
          <el-button
            v-if="canRuleCustom"
            size="mini"
            type="text"
            icon="el-icon-data-analysis"
            @click="handleImpact(scope.row)"
            v-hasPermi="['fee:mappingRule:impact']"
          >影响分析</el-button>
          <el-button
            v-if="canRuleCustom && scope.row.publishStatus !== 'PUBLISHED'"
            size="mini"
            type="text"
            icon="el-icon-s-promotion"
            @click="handlePublishRule(scope.row)"
            v-hasPermi="['fee:mappingRule:publish']"
          >发布</el-button>
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

    <!-- 添加或修改映射规则对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="700px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="规则编码" prop="ruleCode">
          <el-input v-model="form.ruleCode" placeholder="请输入规则编码" maxlength="50" />
        </el-form-item>
        <el-form-item label="物资小类" prop="itemCategory">
          <el-select v-model="form.itemCategory" placeholder="请选择物资小类" style="width: 100%">
            <el-option
              v-for="dict in itemCategoryOptions"
              :key="dict.value"
              :label="dict.label"
              :value="dict.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="用途标签" prop="purposeTag">
          <el-select v-model="form.purposeTag" placeholder="请选择用途标签" style="width: 100%">
            <el-option
              v-for="tag in purposeTags"
              :key="tag.tagCode"
              :label="tag.tagName + ' ' + tag.tagIcon"
              :value="tag.tagCode"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="目标预算科目" prop="targetBudgetSubject">
          <el-input v-model="form.targetBudgetSubject" placeholder="请输入目标预算科目编码" maxlength="50" />
        </el-form-item>
        <el-form-item label="会计大类" prop="accountingClass">
          <el-select v-model="form.accountingClass" placeholder="请选择会计大类" style="width: 100%">
            <el-option label="费用类(EXPENSE)" value="EXPENSE" />
            <el-option label="成本类(COST)" value="COST" />
            <el-option label="资产类(ASSET)" value="ASSET" />
            <el-option label="负债类(LIABILITY)" value="LIABILITY" />
            <el-option label="所有者权益类(EQUITY)" value="EQUITY" />
            <el-option label="收入类(INCOME)" value="INCOME" />
          </el-select>
        </el-form-item>
        <el-form-item label="是否固定资产" prop="isFixedAsset">
          <el-radio-group v-model="form.isFixedAsset">
            <el-radio :label="1">是</el-radio>
            <el-radio :label="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="费用确认方式" prop="costMethod">
          <el-select v-model="form.costMethod" placeholder="请选择费用确认方式" style="width: 100%">
            <el-option label="直接费用化(DIRECT)" value="DIRECT" />
            <el-option label="折旧(DEPRECIATION)" value="DEPRECIATION" />
            <el-option label="计提摊销(ACCRUAL)" value="ACCRUAL" />
            <el-option label="费率利息(FEE_RATE)" value="FEE_RATE" />
          </el-select>
          <div class="el-form-item__tip" style="color:#909399;font-size:12px;">
            基础版仅支持直支；高级版支持折旧；企业版支持全部
          </div>
        </el-form-item>
        <el-form-item label="优先级" prop="priority">
          <el-input-number v-model="form.priority" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio label="ENABLED">启用</el-radio>
            <el-radio label="DISABLED">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button :loading="buttonLoading" type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 导入对话框 -->
    <el-dialog title="导入映射规则" :visible.sync="importOpen" width="400px" append-to-body>
      <el-upload
        ref="upload"
        :limit="1"
        accept=".xlsx, .xls"
        :headers="uploadHeaders"
        :action="uploadUrl"
        :auto-upload="false"
        :on-success="handleUploadSuccess"
        :on-error="handleUploadError"
      >
        <el-button size="small" type="primary">选择文件</el-button>
        <div slot="tip" class="el-upload__tip">仅允许导入xls、xlsx格式文件</div>
      </el-upload>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitUpload">确 定</el-button>
        <el-button @click="importOpen = false">取 消</el-button>
      </div>
    </el-dialog>
    <!-- 发布新版本对话框 -->
    <el-dialog title="发布规则版本" :visible.sync="publishOpen" width="420px" append-to-body>
      <el-form label-width="110px">
        <el-form-item label="灰度百分比">
          <el-input-number v-model="publishPct" :min="1" :max="100" style="width: 200px;" />
          <div style="color:#909399;font-size:12px;">默认10%灰度；100%=直接全量发布</div>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="publishOpen = false">取 消</el-button>
        <el-button type="primary" @click="handlePublish">发 布</el-button>
      </div>
    </el-dialog>

    <!-- 灰度比对结果对话框 -->
    <el-dialog title="灰度命中率比对" :visible.sync="compareOpen" width="460px" append-to-body>
      <el-descriptions :column="1" border size="small" v-if="compareResult">
        <el-descriptions-item label="版本">V{{ compareResult.versionNo }}</el-descriptions-item>
        <el-descriptions-item label="样本单据数">{{ compareResult.sampleCount }}</el-descriptions-item>
        <el-descriptions-item label="灰度分组样本数">{{ compareResult.grayscaleSampleCount }}</el-descriptions-item>
        <el-descriptions-item label="新版本命中率">{{ pctText(compareResult.newHitRate) }}</el-descriptions-item>
        <el-descriptions-item label="上一版本命中率">{{ pctText(compareResult.oldHitRate) }}</el-descriptions-item>
        <el-descriptions-item label="命中率变化">
          <span :style="{ color: (compareResult.hitRateDiff || 0) >= 0 ? '#67C23A' : '#F56C6C' }">
            {{ compareResult.hitRateDiff == null ? '-' : (compareResult.hitRateDiff >= 0 ? '+' : '') + (compareResult.hitRateDiff * 100).toFixed(2) + '%' }}
          </span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>

    <!-- AI 语义推荐对话框 -->
    <el-dialog title="AI 语义推荐科目" :visible.sync="recommendOpen" width="640px" append-to-body>
      <el-input v-model="recommendText" placeholder="输入报销摘要/用途描述，如：项目组出差北京机票与酒店" @keyup.enter.native="handleRecommend">
        <el-button slot="append" icon="el-icon-search" @click="handleRecommend">推荐</el-button>
      </el-input>
      <el-table :data="recommendList" size="small" style="margin-top: 12px;">
        <el-table-column label="规则编码" prop="ruleCode" width="120" />
        <el-table-column label="物资小类" prop="itemCategory" width="120" />
        <el-table-column label="用途标签" prop="purposeTag" width="120" />
        <el-table-column label="目标预算科目" prop="budgetSubject" min-width="140" />
        <el-table-column label="会计大类" prop="accountingClass" width="110" />
      </el-table>
    </el-dialog>
    <!-- 影响分析对话框（T6.4 / FEAT_RULE_ENGINE_CUSTOM） -->
    <el-dialog title="发布前影响分析" :visible.sync="impactOpen" width="520px" append-to-body>
      <el-descriptions :column="1" border size="small" v-if="impactResult">
        <el-descriptions-item v-for="(value, key) in impactResult" :key="key" :label="impactLabel(key)">{{ value }}</el-descriptions-item>
      </el-descriptions>
      <div style="color:#909399;font-size:12px;margin-top:10px;">建议先评估影响范围再发布；发布全程留痕可追溯。</div>
    </el-dialog>

    <!-- 规则变更审计日志（T6.4） -->
    <el-dialog title="规则变更审计日志" :visible.sync="auditOpen" width="860px" append-to-body>
      <el-table :data="auditList" size="small" border v-loading="auditLoading">
        <el-table-column label="规则ID" width="90" align="center" prop="ruleId" />
        <el-table-column label="操作类型" width="110" align="center">
          <template slot-scope="scope">
            <el-tag size="mini" :type="auditActionType(scope.row.action)">{{ auditActionText(scope.row.action) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作人" width="110" align="center" prop="operUserName" />
        <el-table-column label="操作时间" width="150" align="center">
          <template slot-scope="scope">{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}</template>
        </el-table-column>
        <el-table-column label="变更后内容" min-width="220" prop="publishContent" :show-overflow-tooltip="true" />
      </el-table>
      <pagination v-show="auditTotal > 0" :total="auditTotal" :page.sync="auditQuery.pageNum" :limit.sync="auditQuery.pageSize" @pagination="loadAuditList" />
    </el-dialog>
  </div>
</template>

<script>
import { listMappingRule, getMappingRule, delMappingRule, addMappingRule, updateMappingRule, publishMappingRule, impactMappingRule, listMappingAudit } from "@/api/system/fee/mappingRule";
import { allEnabledPurposeTag } from "@/api/system/fee/purposeTag";
import { listRuleVersions, publishRuleVersion, compareRuleVersion, fullReleaseRuleVersion, rollbackRuleVersion, recommendSubject } from "@/api/system/fee/fundPro";
import { loadFeatures, featureVisible } from "@/utils/feature";
import { getToken } from "@/utils/auth";

export default {
  name: "MappingRule",
  dicts: ['fee_item_category'],
  data() {
    return {
      buttonLoading: false,
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      mappingRuleList: [],
      purposeTags: [],
      // 物资小类字典选项
      itemCategoryOptions: [],
      title: "",
      open: false,
      importOpen: false,
      // 专业版灰度面板（FEAT_RULE_GRAYSCALE）
      canGrayscale: false,
      versionLoading: false,
      ruleVersions: [],
      publishOpen: false,
      publishPct: 10,
      compareOpen: false,
      compareResult: null,
      recommendOpen: false,
      recommendText: '',
      recommendList: [],
      // 高级版规则引擎自定义（FEAT_RULE_ENGINE_CUSTOM，T6.4）
      canRuleCustom: false,
      impactOpen: false,
      impactResult: null,
      auditOpen: false,
      auditLoading: false,
      auditList: [],
      auditTotal: 0,
      auditQuery: { pageNum: 1, pageSize: 10, ruleId: undefined },
      uploadUrl: process.env.VUE_APP_BASE_API + "/fee/mappingRule/importData",
      uploadHeaders: { Authorization: "Bearer " + getToken() },
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        ruleCode: undefined,
        itemCategory: undefined,
        purposeTag: undefined,
        status: undefined,
      },
      form: {},
      rules: {
        ruleCode: [
          { required: true, message: "规则编码不能为空", trigger: "blur" }
        ],
        itemCategory: [
          { required: true, message: "物资小类不能为空", trigger: "blur" }
        ],
        purposeTag: [
          { required: true, message: "请选择用途标签", trigger: "change" }
        ],
        targetBudgetSubject: [
          { required: true, message: "目标预算科目不能为空", trigger: "blur" }
        ],
        accountingClass: [
          { required: true, message: "请选择会计大类", trigger: "change" }
        ]
      }
    };
  },
  created() {
    this.getList();
    this.loadPurposeTags();
    // 加载字典数据
    this.itemCategoryOptions = this.dict.type.fee_item_category || [];
    // 专业版开关：灰度面板按 License 下发
    loadFeatures().then(() => {
      this.canGrayscale = featureVisible('FEAT_RULE_GRAYSCALE');
      this.canRuleCustom = featureVisible('FEAT_RULE_ENGINE_CUSTOM');
      if (this.canGrayscale) {
        this.loadVersions();
      }
    });
  },
  methods: {
    // 获取字典标签
    getDictLabel(dictOptions, value) {
      if (!value || !dictOptions) return value;
      const dict = dictOptions.find(d => d.value === value);
      return dict ? dict.label : value;
    },
    getList() {
      this.loading = true;
      listMappingRule(this.queryParams).then(response => {
        this.mappingRuleList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    loadPurposeTags() {
      allEnabledPurposeTag().then(response => {
        this.purposeTags = response.data || [];
      });
    },
    cancel() {
      this.open = false;
      this.reset();
    },
    reset() {
      this.form = {
        id: undefined,
        ruleCode: undefined,
        itemCategory: undefined,
        purposeTag: undefined,
        targetBudgetSubject: undefined,
        accountingClass: 'EXPENSE',
        isFixedAsset: 0,
        costMethod: 'DIRECT',
        priority: 0,
        status: "ENABLED"
      };
      this.resetForm("form");
    },
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "添加映射规则";
    },
    handleUpdate(row) {
      this.reset();
      const id = row.id || this.ids[0];
      getMappingRule(id).then(response => {
        this.form = response.data;
        this.open = true;
        this.title = "修改映射规则";
      });
    },
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.buttonLoading = true;
          if (this.form.id != null) {
            updateMappingRule(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
              this.buttonLoading = false;
            });
          } else {
            addMappingRule(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
              this.buttonLoading = false;
            });
          }
        }
      });
    },
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$modal.confirm('是否确认删除映射规则编号为"' + ids + '"的数据项？').then(function() {
        return delMappingRule(ids);
      }).then(() => {
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {});
    },
    handleImport() {
      this.importOpen = true;
    },
    submitUpload() {
      this.$refs.upload.submit();
    },
    handleUploadSuccess(response) {
      this.$modal.msgSuccess(response.msg || "导入成功");
      this.importOpen = false;
      this.getList();
    },
    handleUploadError(err) {
      this.$modal.msgError("导入失败：" + err);
    },
    // ========== 规则版本灰度（专业版） ==========
    loadVersions() {
      this.versionLoading = true;
      listRuleVersions().then(res => {
        this.ruleVersions = res.data || [];
        this.versionLoading = false;
      }).catch(() => { this.versionLoading = false; });
    },
    versionStatusText(status) {
      const map = { GRAYSCALE: '灰度中', FULL: '全量生效', SUPERSEDED: '已替换', ROLLBACK: '已回滚' };
      return map[status] || status;
    },
    versionStatusType(status) {
      const map = { GRAYSCALE: 'warning', FULL: 'success', SUPERSEDED: 'info', ROLLBACK: 'danger' };
      return map[status] || 'info';
    },
    pctText(rate) {
      return rate == null ? '-' : (rate * 100).toFixed(2) + '%';
    },
    handlePublish() {
      publishRuleVersion(this.publishPct).then(() => {
        this.$modal.msgSuccess('版本发布成功');
        this.publishOpen = false;
        this.loadVersions();
      });
    },
    handleCompare(row) {
      compareRuleVersion(row.id).then(res => {
        this.compareResult = res.data;
        this.compareOpen = true;
        this.loadVersions();
      });
    },
    handleFullRelease(row) {
      this.$modal.confirm('确认将版本 V' + row.versionNo + ' 全量发布？旧全量版本将被替换。').then(() => {
        return fullReleaseRuleVersion(row.id);
      }).then(() => {
        this.$modal.msgSuccess('全量发布成功');
        this.loadVersions();
      }).catch(() => {});
    },
    handleRollback(row) {
      this.$modal.confirm('确认回滚版本 V' + row.versionNo + '？').then(() => {
        return rollbackRuleVersion(row.id);
      }).then(() => {
        this.$modal.msgSuccess('回滚成功');
        this.loadVersions();
      }).catch(() => {});
    },
    handleRecommend() {
      if (!this.recommendText) return;
      recommendSubject(this.recommendText).then(res => {
        this.recommendList = res.data || [];
      });
    },
    // ========== 规则草稿→发布（高级版 / FEAT_RULE_ENGINE_CUSTOM，T6.4） ==========
    publishStatusText(status) {
      const map = { DRAFT: '草稿', PUBLISHED: '已发布' };
      return map[status] || status || '草稿';
    },
    handlePublishRule(row) {
      this.$modal.confirm('确认发布规则「' + row.ruleCode + '」？发布后正式参与匹配并写入审计日志。').then(() => {
        return publishMappingRule(row.id);
      }).then(() => {
        this.$modal.msgSuccess('发布成功');
        this.getList();
      }).catch(() => {});
    },
    impactLabel(key) {
      const map = { affectedBillCount: '影响历史报销单数', affectedBudgetCount: '影响预算条目数', budgetSubjects: '涉及预算科目', lastHitTime: '最近命中时间' };
      return map[key] || key;
    },
    handleImpact(row) {
      impactMappingRule(row.id).then(res => {
        this.impactResult = res.data || {};
        this.impactOpen = true;
      });
    },
    auditActionText(action) {
      const map = { CREATE: '新增', UPDATE: '修改', PUBLISH: '发布', ROLLBACK: '回滚', DELETE: '删除' };
      return map[action] || action;
    },
    auditActionType(action) {
      const map = { CREATE: 'success', UPDATE: 'warning', PUBLISH: '', ROLLBACK: 'danger', DELETE: 'danger' };
      return map[action] || 'info';
    },
    handleAuditOpen() {
      this.auditQuery = { pageNum: 1, pageSize: 10, ruleId: undefined };
      this.auditOpen = true;
      this.loadAuditList();
    },
    loadAuditList() {
      this.auditLoading = true;
      listMappingAudit(this.auditQuery).then(res => {
        this.auditList = res.rows || [];
        this.auditTotal = res.total || 0;
        this.auditLoading = false;
      }).catch(() => { this.auditLoading = false; });
    }
  }
};
</script>
