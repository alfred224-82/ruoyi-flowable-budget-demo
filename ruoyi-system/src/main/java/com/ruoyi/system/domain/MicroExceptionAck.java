package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.TenantEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务异常已处理确认对象 micro_exception_ack（T18.8 增强，v1.68 新增）
 * <p>异常处理中心「固定支出异常」为派生异常（无物理来源记录，id=0），老板点击「已处理」后
 * 需按「租户 + 异常类型 + 业务键(月份)」持久化确认标记，否则列表刷新时重新推导导致异常「复活」。
 * <p>租户级表（含 tenant_id），多租户插件自动隔离。
 *
 * @author ruoyi
 * @date 2026-10-02
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("micro_exception_ack")
public class MicroExceptionAck extends TenantEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 异常类型：FIXED_COST_ANOMALY(固定支出异常) 等
     */
    private String exceptionType;

    /**
     * 异常业务键（固定支出异常=月份 yyyy-MM；确认当月有效，新月重新推导）
     */
    private String exceptionKey;

    /**
     * 备注
     */
    private String remark;
}
