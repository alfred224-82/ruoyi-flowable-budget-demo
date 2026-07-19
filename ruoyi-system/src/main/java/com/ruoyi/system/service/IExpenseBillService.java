package com.ruoyi.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.system.domain.ExpenseBill;
import com.ruoyi.system.domain.bo.ExpenseBillBo;
import com.ruoyi.system.domain.vo.ExpenseBillVo;

/**
 * 报销单Service接口
 *
 * @author ruoyi
 * @date 2026-07-13
 */
public interface IExpenseBillService {

    /**
     * 查询报销单详情
     *
     * @param id 主键ID
     * @return 报销单VO
     */
    ExpenseBillVo queryById(Long id);

    /**
     * 分页查询报销单列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页结果
     */
    Page<ExpenseBillVo> selectPageList(ExpenseBillBo bo, PageQuery pageQuery);

    /**
     * 新增报销单（创建即提交，无草稿状态）
     *
     * @param bo 业务对象
     * @return 报销单实体
     */
    ExpenseBill insertByBo(ExpenseBillBo bo);
}
