package com.ruoyi.system.domain.bo;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * Boss/平台 Admin 投诉处理业务对象（T25.4，小微版 M25）
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Data
public class ComplaintReplyBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 处理回复内容 */
    @NotNull(message = "处理回复不能为空")
    private String handlerReply;

    /** 处理后状态：RESOLVED=已解决 / CLOSED=已关闭 */
    private String status;
}
