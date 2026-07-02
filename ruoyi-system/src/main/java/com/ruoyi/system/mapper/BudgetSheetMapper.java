package com.ruoyi.system.mapper;

import com.ruoyi.system.domain.BudgetSheet;
import com.ruoyi.system.domain.vo.BudgetSheetVo;
import com.ruoyi.common.core.mapper.BaseMapperPlus;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;

@Mapper
@Component
public interface BudgetSheetMapper extends BaseMapperPlus<BudgetSheetMapper, BudgetSheet, BudgetSheetVo> {

}

