package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 首单识别业务对象 first_order_record（T16.4/T16.8，小微版 M16）
 * <p>★首单判定由引擎内部写入，不开放外部写入；BO 仅用于查询过滤。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
@Data
public class FirstOrderRecordBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 活动ID */
    @NotNull(message = "活动ID不能为空", groups = {AddGroup.class})
    private Long activityId;

    /** 顾客手机号（查询过滤用） */
    private String customerPhone;

    /** 顾客用户ID */
    private Long customerUserId;
}
