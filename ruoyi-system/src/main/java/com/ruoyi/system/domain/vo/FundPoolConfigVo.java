package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 资金池配置视图对象
 *
 * @author ruoyi
 * @date 2026-07-24
 */
@Data
public class FundPoolConfigVo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    private String levelType;

    private String levelName;

    private Long roleId;

    private String roleKey;

    private String roleName;

    private BigDecimal initialAmount;

    private BigDecimal warningThreshold;

    private String status;

    private Date createTime;

    private Date updateTime;
}
