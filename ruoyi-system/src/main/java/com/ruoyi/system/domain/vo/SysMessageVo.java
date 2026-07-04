package com.ruoyi.system.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 系统消息通知视图对象 sys_message
 *
 * @author ruoyi
 * @date 2026-07-04
 */
@Data
@ExcelIgnoreUnannotated
public class SysMessageVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ExcelProperty("消息ID")
    private Long id;

    @ExcelProperty("接收用户ID")
    private Long userId;

    @ExcelProperty("消息标题")
    private String title;

    @ExcelProperty("消息内容")
    private String content;

    @ExcelProperty("消息类型")
    private String messageType;

    @ExcelProperty("业务类型")
    private String bizType;

    @ExcelProperty("业务ID")
    private Long bizId;

    @ExcelProperty("是否已读")
    private Integer isRead;

    @ExcelProperty("创建时间")
    private Date createTime;
}
