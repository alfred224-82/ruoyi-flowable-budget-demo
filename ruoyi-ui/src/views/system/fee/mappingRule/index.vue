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
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

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
      <el-table-column label="状态" align="center" prop="status" width="100">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 'ENABLED' ? 'success' : 'danger'">
            {{ scope.row.status === 'ENABLED' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="150">
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
  </div>
</template>

<script>
import { listMappingRule, getMappingRule, delMappingRule, addMappingRule, updateMappingRule } from "@/api/system/fee/mappingRule";
import { allEnabledPurposeTag } from "@/api/system/fee/purposeTag";
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
    }
  }
};
</script>
