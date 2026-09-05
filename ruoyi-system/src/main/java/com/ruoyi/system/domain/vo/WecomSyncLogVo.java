package com.ruoyi.system.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 企微通讯录同步日志视图对象 wecom_sync_log（T13.2）
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class WecomSyncLogVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    private Long id;

    /** 租户ID */
    private Long tenantId;

    /** 同步类型：dept/user/all */
    private String syncType;

    /** 状态：SUCCESS/PARTIAL/FAILED */
    private String status;

    /** 总数 */
    private Integer totalCount;

    /** 成功数 */
    private Integer successCount;

    /** 失败数 */
    private Integer failCount;

    /** 错误详情 */
    private String errorMsg;

    /** 耗时(毫秒) */
    private Long costMs;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;
}
