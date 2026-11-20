<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="80px">
      <el-form-item label="所属容器" prop="projectId">
        <el-select v-model="queryParams.projectId" placeholder="请选择容器" filterable clearable>
          <el-option v-for="p in containers" :key="p.id" :label="p.name" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="请选择状态" clearable>
          <el-option label="待办" value="PENDING" />
          <el-option label="进行中" value="DOING" />
          <el-option label="已完成" value="DONE" />
          <el-option label="已取消" value="CANCELLED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <el-row :gutter="10" class="mb8">
      <el-col :span="1.5">
        <el-button type="primary" plain icon="el-icon-plus" size="mini" @click="handleAdd" v-hasPermi="['biz:task:add']">新建任务</el-button>
      </el-col>
      <right-toolbar :showSearch.sync="showSearch" @queryTable="getList"></right-toolbar>
    </el-row>

    <el-table v-loading="loading" :data="taskList">
      <el-table-column label="任务名称" align="left" prop="taskName" min-width="160" show-overflow-tooltip />
      <el-table-column label="所属容器" align="center" min-width="140" show-overflow-tooltip>
        <template slot-scope="scope">{{ containerName(scope.row.projectId) }}</template>
      </el-table-column>
      <el-table-column label="指派人" align="center" width="100">
        <template slot-scope="scope">{{ userName(scope.row.assigneeUserId) }}</template>
      </el-table-column>
      <el-table-column label="进度" align="center" width="140">
        <template slot-scope="scope">
          <el-progress :percentage="scope.row.progress || 0" :stroke-width="10" text-inside />
        </template>
      </el-table-column>
      <el-table-column label="截止时间" align="center" prop="deadline" width="150">
        <template slot-scope="scope"><span>{{ parseTime(scope.row.deadline) }}</span></template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="90">
        <template slot-scope="scope">
          <el-tag :type="statusTag(scope.row.status)" size="small">{{ statusText(scope.row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="200">
        <template slot-scope="scope">
          <el-button size="mini" type="text" icon="el-icon-user" @click="handleAssign(scope.row)" v-hasPermi="['biz:task:assign']">指派</el-button>
          <el-button size="mini" type="text" icon="el-icon-edit" @click="handleUpdate(scope.row)" v-hasPermi="['biz:task:edit']">编辑</el-button>
          <el-button size="mini" type="text" icon="el-icon-delete" @click="handleDelete(scope.row)" v-hasPermi="['biz:task:remove']">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 新建/编辑任务 -->
    <el-dialog :title="title" :visible.sync="open" width="560px" append-to-body>
      <el-form ref="form" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="所属容器" prop="projectId">
          <el-select v-model="form.projectId" placeholder="请选择容器" filterable style="width: 100%;">
            <el-option v-for="p in containers" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="任务名称" prop="taskName">
          <el-input v-model="form.taskName" placeholder="请输入任务名称" />
        </el-form-item>
        <el-form-item label="指派人" prop="assigneeUserId">
          <el-select v-model="form.assigneeUserId" placeholder="请选择指派人" filterable clearable style="width: 100%;">
            <el-option v-for="u in userOptions" :key="u.userId" :label="u.nickName" :value="u.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="进度%" prop="progress">
          <el-input-number v-model="form.progress" :min="0" :max="100" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="截止时间" prop="deadline">
          <el-date-picker v-model="form.deadline" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="选择截止时间" style="width: 100%;" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-select v-model="form.status" placeholder="请选择状态" style="width: 100%;">
            <el-option label="待办" value="PENDING" />
            <el-option label="进行中" value="DOING" />
            <el-option label="已完成" value="DONE" />
            <el-option label="已取消" value="CANCELLED" />
          </el-select>
        </el-form-item>
        <el-form-item label="附件" prop="attachment">
          <el-input v-model="form.attachment" placeholder="附件URL列表（JSON数组）" />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitForm">确 定</el-button>
        <el-button @click="cancel">取 消</el-button>
      </div>
    </el-dialog>

    <!-- 指派/进度 -->
    <el-dialog title="指派与进度更新" :visible.sync="assignOpen" width="460px" append-to-body>
      <el-form ref="assignForm" :model="assignForm" label-width="90px">
        <el-form-item label="指派人">
          <el-select v-model="assignForm.assigneeUserId" placeholder="请选择指派人" filterable clearable style="width: 100%;">
            <el-option v-for="u in userOptions" :key="u.userId" :label="u.nickName" :value="u.userId" />
          </el-select>
        </el-form-item>
        <el-form-item label="进度%">
          <el-slider v-model="assignForm.progress" :max="100" show-input />
        </el-form-item>
      </el-form>
      <div slot="footer" class="dialog-footer">
        <el-button type="primary" @click="submitAssign">确 定</el-button>
        <el-button @click="assignOpen = false">取 消</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { listTask, addTask, updateTask, assignTask, delTask } from '@/api/biz/task'
import { listProject, listUserOptions } from '@/api/biz/project'

export default {
  name: 'BizProjectTask',
  data() {
    return {
      loading: true,
      showSearch: true,
      total: 0,
      taskList: [],
      containers: [],
      userOptions: [],
      open: false,
      assignOpen: false,
      title: '',
      queryParams: { pageNum: 1, pageSize: 10, projectId: null, status: null },
      form: {},
      assignForm: {},
      rules: {
        projectId: [{ required: true, message: '请选择所属容器', trigger: 'change' }],
        taskName: [{ required: true, message: '任务名称不能为空', trigger: 'blur' }]
      }
    }
  },
  created() {
    this.getList()
    listProject({ pageNum: 1, pageSize: 200, type: 'general' }).then(res => { this.containers = res.rows || [] })
    listUserOptions().then(res => { this.userOptions = res.data || [] }).catch(() => {})
  },
  methods: {
    getList() {
      this.loading = true
      listTask(this.queryParams).then(response => {
        this.taskList = response.rows
        this.total = response.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    containerName(id) {
      const p = this.containers.find(c => c.id === id)
      return p ? p.name : ('#' + id)
    },
    userName(id) {
      const u = this.userOptions.find(x => x.userId === id)
      return u ? u.nickName : (id || '-')
    },
    statusText(status) {
      const map = { PENDING: '待办', DOING: '进行中', DONE: '已完成', CANCELLED: '已取消' }
      return map[status] || status
    },
    statusTag(status) {
      const map = { PENDING: 'info', DOING: 'primary', DONE: 'success', CANCELLED: 'danger' }
      return map[status] || 'info'
    },
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    reset() {
      this.form = { id: null, projectId: null, taskName: null, assigneeUserId: null, progress: 0, deadline: null, attachment: null, status: 'PENDING' }
      this.resetForm('form')
    },
    cancel() {
      this.open = false
      this.reset()
    },
    handleAdd() {
      this.reset()
      this.open = true
      this.title = '新建任务'
    },
    handleUpdate(row) {
      this.reset()
      this.form = { ...row }
      this.open = true
      this.title = '编辑任务'
    },
    submitForm() {
      this.$refs['form'].validate(valid => {
        if (valid) {
          if (this.form.id != null) {
            updateTask(this.form).then(() => {
              this.$modal.msgSuccess('修改成功')
              this.open = false
              this.getList()
            })
          } else {
            addTask(this.form).then(() => {
              this.$modal.msgSuccess('新建成功')
              this.open = false
              this.getList()
            })
          }
        }
      })
    },
    handleAssign(row) {
      this.assignForm = { id: row.id, assigneeUserId: row.assigneeUserId, progress: row.progress || 0 }
      this.assignOpen = true
    },
    submitAssign() {
      assignTask(this.assignForm.id, this.assignForm.assigneeUserId, this.assignForm.progress).then(() => {
        this.$modal.msgSuccess('指派成功')
        this.assignOpen = false
        this.getList()
      })
    },
    handleDelete(row) {
      this.$modal.confirm('确认删除任务「' + row.taskName + '」？').then(() => {
        return delTask(row.id)
      }).then(() => {
        this.getList()
        this.$modal.msgSuccess('删除成功')
      }).catch(() => {})
    }
  }
}
</script>
