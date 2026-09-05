<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="100px">
      <el-form-item label="场景编码" prop="sceneCode">
        <el-select v-model="queryParams.sceneCode" placeholder="请选择场景" clearable>
          <el-option label="基础报销引导" value="EXPENSE_BASIC" />
          <el-option label="固定资产报销引导" value="ASSET" />
          <el-option label="借款报销引导" value="LOAN" />
          <el-option label="外币报销引导" value="FOREIGN" />
        </el-select>
      </el-form-item>
      <el-form-item label="场景名称" prop="sceneName">
        <el-input
          v-model="queryParams.sceneName"
          placeholder="请输入场景名称"
          clearable
          @keyup.enter.native="handleQuery"
        />
      </el-form-item>
      <el-form-item label="行业编码" prop="industryCode">
        <el-input
          v-model="queryParams.industryCode"
          placeholder="行业编码（*=通用）"
          clearable
          @keyup.enter.native="handleQuery"
        />
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
          v-hasPermi="['fee:aiPrompt:add']"
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
          v-hasPermi="['fee:aiPrompt:edit']"
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
          v-hasPermi="['fee:aiPrompt:remove']"
        >删除</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="configList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="场景编码" align="center" prop="sceneCode" width="150">
        <template slot-scope="scope">
          <el-tag size="small">{{ scope.row.sceneCode }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="场景名称" align="center" prop="sceneName" min-width="150" />
      <el-table-column label="话术模板" align="center" prop="promptTemplate" min-width="250" show-overflow-tooltip />
      <el-table-column label="提问步骤数" align="center" width="100">
        <template slot-scope="scope">
          <el-tag type="info" size="small">{{ getStepCount(scope.row.questionSequence) }} 步</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="行业编码" align="center" prop="industryCode" width="100">
        <template slot-scope="scope">
          <span>{{ scope.row.industryCode === '*' ? '通用' : scope.row.industryCode }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 'ENABLED' ? 'success' : 'danger'" size="small">
            {{ scope.row.status === 'ENABLED' ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160" />
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200">
        <template slot-scope="scope">
          <el-button
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handlePreview(scope.row)"
          >预览</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-edit"
            @click="handleUpdate(scope.row)"
            v-hasPermi="['fee:aiPrompt:edit']"
          >修改</el-button>
          <el-button
            size="mini"
            type="text"
            icon="el-icon-delete"
            @click="handleDelete(scope.row)"
            v-hasPermi="['fee:aiPrompt:remove']"
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

    <!-- 添加或修改话术配置对话框 -->
    <el-dialog :title="title" :visible.sync="open" width="750px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="场景编码" prop="sceneCode">
          <el-select v-model="form.sceneCode" placeholder="请选择场景编码" style="width: 100%">
            <el-option label="基础报销引导(EXPENSE_BASIC)" value="EXPENSE_BASIC" />
            <el-option label="固定资产报销引导(ASSET)" value="ASSET" />
            <el-option label="借款报销引导(LOAN)" value="LOAN" />
            <el-option label="外币报销引导(FOREIGN)" value="FOREIGN" />
          </el-select>
        </el-form-item>
        <el-form-item label="场景名称" prop="sceneName">
          <el-input v-model="form.sceneName" placeholder="请输入场景名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="行业编码" prop="industryCode">
          <el-input v-model="form.industryCode" placeholder="行业编码（*=通用，所有行业适用）" maxlength="50" />
          <div class="el-form-item__tip" style="color: #909399; font-size: 12px;">留空或填 * 表示通用配置，所有行业适用</div>
        </el-form-item>
        <el-form-item label="话术模板" prop="promptTemplate">
          <el-input
            v-model="form.promptTemplate"
            type="textarea"
            :rows="4"
            placeholder="请输入AI引导话术模板，如：您好！我来帮您填写报销单..."
            maxlength="2000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="提问顺序" prop="questionSequence">
          <div style="width: 100%">
            <el-button type="primary" size="mini" icon="el-icon-plus" @click="addQuestion" style="margin-bottom: 10px">添加步骤</el-button>
            <el-table :data="questionList" border size="small" style="width: 100%">
              <el-table-column label="步骤" width="60" align="center" type="index" />
              <el-table-column label="引导问题" min-width="200">
                <template slot-scope="scope">
                  <el-input v-model="scope.row.question" placeholder="如：请选择费用类型" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="关联字段" width="150">
                <template slot-scope="scope">
                  <el-input v-model="scope.row.field" placeholder="如：itemCategory" size="small" />
                </template>
              </el-table-column>
              <el-table-column label="操作" width="80" align="center">
                <template slot-scope="scope">
                  <el-button size="mini" type="text" icon="el-icon-delete" @click="removeQuestion(scope.$index)" />
                </template>
              </el-table-column>
            </el-table>
          </div>
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

    <!-- 话术预览对话框 -->
    <el-dialog title="话术预览" :visible.sync="previewOpen" width="600px" append-to-body>
      <div v-if="previewData" class="preview-container">
        <el-card shadow="never" class="preview-card">
          <div slot="header">
            <span>{{ previewData.sceneName }}</span>
            <el-tag size="small" style="margin-left: 10px">{{ previewData.sceneCode }}</el-tag>
          </div>
          <div class="preview-prompt">
            <i class="el-icon-chat-dot-round" style="color: #409EFF; margin-right: 8px;"></i>
            <span>{{ previewData.promptTemplate }}</span>
          </div>
          <el-divider content-position="left">提问流程</el-divider>
          <el-steps direction="vertical" :active="previewSteps.length" finish-status="success">
            <el-step
              v-for="(step, index) in previewSteps"
              :key="index"
              :title="'步骤 ' + (index + 1)"
              :description="step.question + ' → ' + step.field"
            />
          </el-steps>
        </el-card>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="previewOpen = false">关 闭</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listAiPromptConfig, getAiPromptConfig, delAiPromptConfig, addAiPromptConfig, updateAiPromptConfig } from "@/api/system/fee/aiPrompt";

export default {
  name: "AiPromptConfig",
  data() {
    return {
      buttonLoading: false,
      loading: true,
      ids: [],
      single: true,
      multiple: true,
      showSearch: true,
      total: 0,
      configList: [],
      title: "",
      open: false,
      previewOpen: false,
      previewData: null,
      previewSteps: [],
      questionList: [],
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        sceneCode: undefined,
        sceneName: undefined,
        industryCode: undefined,
        status: undefined,
      },
      form: {},
      rules: {
        sceneCode: [
          { required: true, message: "场景编码不能为空", trigger: "change" }
        ],
        sceneName: [
          { required: true, message: "场景名称不能为空", trigger: "blur" }
        ],
        promptTemplate: [
          { required: true, message: "话术模板不能为空", trigger: "blur" }
        ],
      }
    };
  },
  created() {
    this.getList();
  },
  methods: {
    /** 查询列表 */
    getList() {
      this.loading = true;
      listAiPromptConfig(this.queryParams).then(response => {
        this.configList = response.rows;
        this.total = response.total;
        this.loading = false;
      });
    },
    /** 获取提问步骤数 */
    getStepCount(questionSequence) {
      if (!questionSequence) return 0;
      try {
        return JSON.parse(questionSequence).length;
      } catch (e) {
        return 0;
      }
    },
    /** 取消按钮 */
    cancel() {
      this.open = false;
      this.reset();
    },
    /** 表单重置 */
    reset() {
      this.form = {
        id: undefined,
        sceneCode: undefined,
        sceneName: undefined,
        promptTemplate: undefined,
        questionSequence: undefined,
        industryCode: "*",
        status: "ENABLED",
      };
      this.questionList = [];
      this.resetForm("form");
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1;
      this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    /** 多选框选中数据 */
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id);
      this.single = selection.length !== 1;
      this.multiple = !selection.length;
    },
    /** 新增按钮操作 */
    handleAdd() {
      this.reset();
      this.open = true;
      this.title = "新增AI话术配置";
    },
    /** 修改按钮操作 */
    handleUpdate(row) {
      this.loading = true;
      const id = row.id || this.ids[0];
      getAiPromptConfig(id).then(response => {
        this.loading = false;
        this.form = response.data;
        // 解析提问顺序JSON
        if (this.form.questionSequence) {
          try {
            this.questionList = JSON.parse(this.form.questionSequence);
          } catch (e) {
            this.questionList = [];
          }
        } else {
          this.questionList = [];
        }
        this.open = true;
        this.title = "修改AI话术配置";
      });
    },
    /** 预览按钮操作 */
    handlePreview(row) {
      this.previewData = row;
      if (row.questionSequence) {
        try {
          this.previewSteps = JSON.parse(row.questionSequence);
        } catch (e) {
          this.previewSteps = [];
        }
      } else {
        this.previewSteps = [];
      }
      this.previewOpen = true;
    },
    /** 添加提问步骤 */
    addQuestion() {
      this.questionList.push({ question: "", field: "" });
    },
    /** 删除提问步骤 */
    removeQuestion(index) {
      this.questionList.splice(index, 1);
    },
    /** 提交按钮 */
    submitForm() {
      this.$refs["form"].validate(valid => {
        if (valid) {
          this.buttonLoading = true;
          // 将提问顺序列表序列化为JSON
          this.form.questionSequence = this.questionList.length > 0
            ? JSON.stringify(this.questionList.map((item, index) => ({
                step: index + 1,
                question: item.question,
                field: item.field
              })))
            : null;
          if (this.form.id != undefined) {
            updateAiPromptConfig(this.form).then(response => {
              this.$modal.msgSuccess("修改成功");
              this.open = false;
              this.getList();
            }).finally(() => {
              this.buttonLoading = false;
            });
          } else {
            addAiPromptConfig(this.form).then(response => {
              this.$modal.msgSuccess("新增成功");
              this.open = false;
              this.getList();
            }).finally(() => {
              this.buttonLoading = false;
            });
          }
        }
      });
    },
    /** 删除按钮操作 */
    handleDelete(row) {
      const ids = row.id || this.ids;
      this.$modal.confirm('是否确认删除选中的AI话术配置？').then(() => {
        this.loading = true;
        return delAiPromptConfig(ids);
      }).then(() => {
        this.loading = false;
        this.getList();
        this.$modal.msgSuccess("删除成功");
      }).catch(() => {
      }).finally(() => {
        this.loading = false;
      });
    }
  }
};
</script>

<style scoped>
.preview-container {
  max-height: 500px;
  overflow-y: auto;
}
.preview-prompt {
  background: #f5f7fa;
  padding: 15px;
  border-radius: 8px;
  line-height: 1.8;
  font-size: 14px;
}
</style>
