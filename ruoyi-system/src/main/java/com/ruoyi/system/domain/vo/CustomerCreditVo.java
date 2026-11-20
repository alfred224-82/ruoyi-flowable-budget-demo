package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 顾客信誉视图对象 customer_credit（T15.31，老板 PC 顾客管理列表）
 * <p>展示累计爽约次数/黑名单状态/拉黑与移出时间；★no_show_count 移出后保留不清零（历史可追溯）。
 *
 * @author ruoyi
 * @date 2026-09-08
 */
@Data
public class CustomerCreditVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 顾客用户ID */
    private Long userId;

    /** 顾客昵称（服务层回填 sys_user.nick_name，非表字段） */
    private String customerName;

    /** 顾客手机号（服务层回填 sys_user.phonenumber，非表字段） */
    private String customerPhone;

    /** 累计爽约次数（保留历史，不清零） */
    private Integer noShowCount;

    /** 是否拉黑：0正常 1已拉黑 */
    private Integer isBlacklisted;

    /** 最近一次拉黑时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date blacklistedAt;

    /** 最近一次被老板移出黑名单时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date unblacklistedAt;

    /** 移出操作的老板用户ID */
    private Long operatorId;

    /** 备注 */
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;
}
