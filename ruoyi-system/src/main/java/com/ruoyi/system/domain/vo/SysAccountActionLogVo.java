package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 账户数据操作留痕视图对象 sys_account_action_log（T14.4）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class SysAccountActionLogVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 用户ID */
    private Long userId;

    /** 操作类型：DATA_EXPORT/ACCOUNT_DELETE */
    private String actionType;

    /** 状态：PENDING/PROCESSING/DONE/REJECTED */
    private String status;

    /** 结果引用 */
    private String resultRef;

    /** 完成截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date deadline;

    /** 处理人 */
    private String operator;

    /** 申请时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date applyTime;

    /** 完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date finishTime;

    /** 备注 */
    private String remark;
}
