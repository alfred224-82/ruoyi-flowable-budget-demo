package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.FeeWelfareConfigBo;
import com.ruoyi.system.domain.vo.FeeWelfareConfigVo;

import java.util.List;

/**
 * 公司福利政策配置Service接口
 * 设计文档 Phase 17 Task 17.2
 *
 * @author ruoyi
 * @date 2026-08-01
 */
public interface IFeeWelfareConfigService {

    /**
     * 分页查询福利配置列表
     *
     * @param bo        查询条件
     * @param pageQuery 分页参数
     * @return 分页列表
     */
    TableDataInfo<FeeWelfareConfigVo> queryPageList(FeeWelfareConfigBo bo, PageQuery pageQuery);

    /**
     * 根据ID查询福利配置详情
     *
     * @param id 主键
     * @return 配置详情
     */
    FeeWelfareConfigVo queryById(Long id);

    /**
     * 新增福利配置
     *
     * @param bo 配置信息
     * @return 影响行数
     */
    int insert(FeeWelfareConfigBo bo);

    /**
     * 更新福利配置
     *
     * @param bo 配置信息
     * @return 影响行数
     */
    int update(FeeWelfareConfigBo bo);

    /**
     * 批量删除福利配置
     *
     * @param ids 主键数组
     * @return 影响行数
     */
    int deleteByIds(Long[] ids);

    /**
     * Excel批量导入福利配置
     *
     * @param list 配置列表
     * @return 导入成功数量
     */
    int importWelfare(List<FeeWelfareConfigBo> list);

    /**
     * 导出福利配置
     *
     * @param bo 查询条件
     * @return 配置列表
     */
    List<FeeWelfareConfigVo> exportWelfare(FeeWelfareConfigBo bo);

    /**
     * 核心方法：根据出差信息自动匹配福利标准
     *
     * @param region       DOMESTIC/INTERNATIONAL
     * @param tripDays     出差天数
     * @param cityLevel    城市等级（国内）
     * @param countryGroup 国家分组（国外）
     * @return 匹配到的福利标准列表（短期+长期）
     */
    List<FeeWelfareConfigVo> matchWelfare(String region, Integer tripDays, String cityLevel, String countryGroup);
}
