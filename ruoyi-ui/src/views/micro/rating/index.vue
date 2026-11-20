<template>
  <div class="app-container">
    <el-alert title="评价管理（★仅 micro_pro 版本，FEAT_RATING）" type="info"
      description="查看/筛选评价列表、回复评价、屏蔽不当评价、查看服务人员综合评分排行。" show-icon :closable="false" class="mb16" />

    <!-- 筛选 -->
    <el-form :model="queryParams" ref="queryForm" size="small" :inline="true" label-width="90px">
      <el-form-item label="被评价人" prop="targetUserId">
        <el-input v-model="queryParams.targetUserId" placeholder="用户ID" clearable :maxlength="20" />
      </el-form-item>
      <el-form-item label="评价场景" prop="scene">
        <el-select v-model="queryParams.scene" placeholder="全部" clearable>
          <el-option label="预约完成" value="booking_complete" />
          <el-option label="服务完成" value="service_done" />
          <el-option label="到店完成" value="visit_done" />
        </el-select>
      </el-form-item>
      <el-form-item label="状态" prop="status">
        <el-select v-model="queryParams.status" placeholder="全部" clearable>
          <el-option label="正常" value="NORMAL" />
          <el-option label="已屏蔽" value="SHIELDED" />
        </el-select>
      </el-form-item>
      <el-form-item>
        <el-button type="primary" icon="el-icon-search" size="mini" @click="handleQuery">搜索</el-button>
        <el-button icon="el-icon-refresh" size="mini" @click="resetQuery">重置</el-button>
      </el-form-item>
    </el-form>

    <!-- 评价列表 -->
    <el-table v-loading="loading" :data="ratingList">
      <el-table-column label="ID" align="center" prop="id" width="60" />
      <el-table-column label="被评价人" align="center" prop="targetUserName" width="100" />
      <el-table-column label="评价人" align="center" prop="authorUserName" width="100" />
      <el-table-column label="评分" align="center" width="120">
        <template slot-scope="scope">
          <span v-for="n in scope.row.rating" :key="n" style="color:#f7ba2a;">★</span>
          <span v-for="n in (5 - scope.row.rating)" :key="'e'+n" style="color:#ddd;">★</span>
        </template>
      </el-table-column>
      <el-table-column label="评价内容" align="left" prop="content" min-width="160" show-overflow-tooltip />
      <el-table-column label="场景" align="center" prop="scene" width="100">
        <template slot-scope="scope">
          {{ sceneMap[scope.row.scene] || scope.row.scene }}
        </template>
      </el-table-column>
      <el-table-column label="状态" align="center" prop="status" width="80">
        <template slot-scope="scope">
          <el-tag :type="scope.row.status === 'NORMAL' ? 'success' : 'danger'" size="mini">
            {{ scope.row.status === 'NORMAL' ? '正常' : '已屏蔽' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="回复" align="left" prop="replyContent" min-width="120" show-overflow-tooltip>
        <template slot-scope="scope">
          <span v-if="scope.row.replyContent">{{ scope.row.replyContent }}</span>
          <span v-else style="color:#c0c4cc;">未回复</span>
        </template>
      </el-table-column>
      <el-table-column label="创建时间" align="center" prop="createTime" width="150" />
      <el-table-column label="操作" align="center" width="180" fixed="right">
        <template slot-scope="scope">
          <el-button size="mini" type="text" @click="handleDetail(scope.row)">详情</el-button>
          <el-button v-if="!scope.row.replyContent" size="mini" type="text" @click="handleReply(scope.row)">回复</el-button>
          <el-button v-if="scope.row.status === 'NORMAL'" size="mini" type="text" style="color:#f56c6c;" @click="handleShield(scope.row)">屏蔽</el-button>
        </template>
      </el-table-column>
    </el-table>
    <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize" @pagination="getList" />

    <!-- 回复弹窗 -->
    <el-dialog title="回复评价" :visible.sync="replyOpen" width="500px">
      <el-form label-width="80px">
        <el-form-item label="评价内容">
          <span>{{ replyTarget && replyTarget.content }}</span>
        </el-form-item>
        <el-form-item label="回复内容">
          <el-input v-model="replyContent" type="textarea" :rows="3" :maxlength="500" show-word-limit placeholder="请输入回复内容" />
        </el-form-item>
      </el-form>
      <span slot="footer">
        <el-button @click="replyOpen = false">取消</el-button>
        <el-button type="primary" @click="submitReply">确认回复</el-button>
      </span>
    </el-dialog>

    <!-- 排行入口 -->
    <el-card shadow="hover" class="mt16">
      <div slot="header"><span style="font-weight:600;">综合评分排行</span></div>
      <el-button type="primary" size="small" @click="loadRanking">加载排行</el-button>
      <el-table v-if="ranking.length" :data="ranking" class="mt16" size="small">
        <el-table-column type="index" label="排名" width="60" />
        <el-table-column label="服务人员" align="center" prop="targetUserName" />
        <el-table-column label="平均评分" align="center" prop="avgRating" />
        <el-table-column label="评价总数" align="center" prop="totalCount" />
      </el-table>
    </el-card>
  </div>
</template>

<script>
import { listRating, replyRating, shieldRating, getRatingRanking } from '@/api/micro/rating'

export default {
  name: 'Rating',
  data() {
    return {
      loading: true,
      total: 0,
      ratingList: [],
      queryParams: { pageNum: 1, pageSize: 10, targetUserId: null, scene: null, status: null },
      sceneMap: { booking_complete: '预约完成', service_done: '服务完成', visit_done: '到店完成' },
      // 回复
      replyOpen: false,
      replyTarget: null,
      replyContent: '',
      // 排行
      ranking: []
    }
  },
  created() { this.getList() },
  methods: {
    getList() {
      this.loading = true
      listRating(this.queryParams).then(res => {
        this.ratingList = res.rows
        this.total = res.total
        this.loading = false
      }).catch(() => { this.loading = false })
    },
    handleQuery() { this.queryParams.pageNum = 1; this.getList() },
    resetQuery() { this.resetForm('queryForm'); this.handleQuery() },
    handleDetail(row) {
      this.$modal.msgSuccess('评价ID：' + row.id + '，内容：' + row.content)
    },
    handleReply(row) {
      this.replyTarget = row
      this.replyContent = ''
      this.replyOpen = true
    },
    submitReply() {
      if (!this.replyContent || !this.replyContent.trim()) { this.$modal.msgWarning('请输入回复内容'); return }
      if (this.replyContent.length > 500) { this.$modal.msgWarning('回复内容不能超过500字'); return }
      replyRating(this.replyTarget.id, this.replyContent).then(() => {
        this.$modal.msgSuccess('回复成功')
        this.replyOpen = false
        this.getList()
      })
    },
    handleShield(row) {
      this.$modal.confirm('确认屏蔽此条评价？屏蔽后不计入综合评分。')
        .then(() => shieldRating(row.id))
        .then(() => { this.$modal.msgSuccess('已屏蔽'); this.getList() })
    },
    loadRanking() {
      getRatingRanking({ topN: 20 }).then(res => {
        this.ranking = res.data || []
      })
    }
  }
}
</script>

<style scoped>
.mb16 { margin-bottom: 16px; }
.mt16 { margin-top: 16px; }
</style>
