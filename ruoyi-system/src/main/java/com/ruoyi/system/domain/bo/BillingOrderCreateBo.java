package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 计费下单业务对象（T14.1）
 * <p>平台代下单/租户续费下单入参：指定租户 + 套餐；服务层据套餐换算金额/天数并回写 sys_tenant.expire_date。
 * <p>【严禁自动扣款】：payChannel=manual/offline 表示离线收款或平台代开通；试用无需支付直接开通。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
public class BillingOrderCreateBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 目标租户ID（平台代下单必填；租户自助续费取当前登录租户，忽略此值） */
    @NotNull(message = "租户ID不能为空", groups = {AddGroup.class})
    private Long tenantId;

    /** 套餐编码：trial/first_month/monthly/yearly */
    @NotBlank(message = "套餐编码不能为空", groups = {AddGroup.class})
    private String planCode;

    /** 支付渠道：wechat/alipay/offline/manual */
    private String payChannel;

    /** 第三方支付流水号（离线/手动可空） */
    private String transactionId;

    /** 备注 */
    private String remark;
}
