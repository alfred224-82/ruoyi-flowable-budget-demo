<template>
  <div class="app-container">
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" v-show="showSearch" label-width="68px">
      <el-form-item label="消息类型" prop="messageType">
        <el-select v-model="queryParams.messageType" placeholder="请选择消息类型" clearable>
          <el-option label="审批通知" value="APPROVAL" />
          <el-option label="驳回通知" value="REJECT" />
          <el-option label="系统通知" value="SYSTEM" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="isRead">
        <el-select v-model="queryParams.isRead" placeholder="请选择状态" clearable>
          <el-option label="未读" :value="0" />
          <el-option label="已读" :value="1" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
        <el-button type="success" plain icon="el-icon-check" size="mini" @click="handleMarkAllRead">全部已读</el-button>
      </el-form-item>
    </el-form>

    <el-table v-loading="loading" :data="messageList" @selection-change="handleSelectionChange">
      <el-table-column type="selection" width="55" align="center" />
      <el-table-column label="序号" align="center" prop="id" width="80" />
      <el-table-column label="消息类型" align="center" prop="messageType" width="120">
        <template slot-scope="scope">
          <el-tag :type="getMessageTypeTag(scope.row.messageType)" size="small">
            {{ getMessageTypeLabel(scope.row.messageType) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="标题" align="left" prop="title" :show-overflow-tooltip="true" min-width="300">
        <template slot-scope="scope">
          <span :class="{ 'unread-title': scope.row.isRead === 0 }">{{ scope.row.title }}</span>
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="isRead" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.isRead === 0 ? 'warning' : 'info'" size="small">
            {{ scope.row.isRead === 0 ? '未读' : '已读' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="160">
        <template slot-scope="scope">
          <span>{{ parseTime(scope.row.createTime, '{y}-{m}-{d} {h}:{i}') }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" align="center" class-name="small-padding fixed-width" width="120">
        <template slot-scope="scope">
          <el-button
            v-if="scope.row.isRead === 0"
            size="mini"
            type="text"
            icon="el-icon-check"
            @click="handleMarkAsRead(scope.row)"
          >标记已读</el-button>
          <el-button
            v-if="scope.row.bizId"
            size="mini"
            type="text"
            icon="el-icon-view"
            @click="handleViewBiz(scope.row)"
          >查看详情</el-button>
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
  </div>
</template>

<script>
import { listMessage, markAsRead, markAllAsRead } from '@/api/system/message'

export default {
  name: 'Message',
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
      // 消息表格数据
      messageList: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        messageType: null,
        isRead: null
      }
    }
  },
  created() {
    this.getList()
  },
  methods: {
    /** 查询消息列表 */
    getList() {
      this.loading = true
      listMessage(this.queryParams).then(response => {
        this.messageList = response.rows
        this.total = response.total
        this.loading = false
      })
    },
    /** 搜索按钮操作 */
    handleQuery() {
      this.queryParams.pageNum = 1
      this.getList()
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm('queryForm')
      this.handleQuery()
    },
    /** 多选框选中数据 */
    handleSelectionChange(selection) {
      this.ids = selection.map(item => item.id)
      this.multiple = !selection.length
    },
    /** 标记单条已读 */
    handleMarkAsRead(row) {
      markAsRead(row.id).then(() => {
        this.$modal.msgSuccess('标记成功')
        row.isRead = 1
        // 更新父组件铃铛的未读数（通过事件总线）
        this.$bus && this.$bus.$emit('refresh-unread-count')
      })
    },
    /** 全部标记已读 */
    handleMarkAllRead() {
      this.$confirm('确定将所有消息标记为已读吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        markAllAsRead().then(() => {
          this.$modal.msgSuccess('全部标记成功')
          this.messageList.forEach(msg => { msg.isRead = 1 })
          // 更新父组件铃铛的未读数（通过事件总线）
          this.$bus && this.$bus.$emit('refresh-unread-count')
        })
      }).catch(() => {})
    },
    /** 查看业务详情 */
    handleViewBiz(row) {
      if (row.bizType === 'budget_preparation') {
        this.$router.push('/system/preparation/wizard?id=' + row.bizId + '&viewOnly=true')
      } else {
        this.$modal.msgWarning('暂不支持该业务类型的跳转')
      }
    },
    /** 获取消息类型标签 */
    getMessageTypeLabel(type) {
      const map = { APPROVAL: '审批', REJECT: '驳回', SYSTEM: '系统' }
      return map[type] || type
    },
    /** 获取消息类型颜色 */
    getMessageTypeTag(type) {
      const map = { APPROVAL: 'warning', REJECT: 'danger', SYSTEM: 'info' }
      return map[type] || 'info'
    }
  }
}
</script>

<style scoped>
.unread-title {
  font-weight: bold;
  color: #409eff;
}
</style>
