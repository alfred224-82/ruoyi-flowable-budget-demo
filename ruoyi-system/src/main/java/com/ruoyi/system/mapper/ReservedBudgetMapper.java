package com.ruoyi.system.mapper;

import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.ruoyi.system.domain.ReservedBudget;
import com.ruoyi.system.domain.vo.ReservedBudgetVo;
import org.apache.ibatis.annotations.Param;

/**
 * 预留预算Mapper接口
 *
 * @author ruoyi
 * @date 2026-07-15
 */
public interface ReservedBudgetMapper extends BaseMapperPlus<ReservedBudgetMapper, ReservedBudget, ReservedBudgetVo> {

    /**
     * 悲观锁查询预留预算行（SELECT FOR UPDATE）
     *
     * @param id 预留预算ID
     * @return 预留预算实体
     */
    ReservedBudget selectForUpdate(@Param("id") Long id);
}
