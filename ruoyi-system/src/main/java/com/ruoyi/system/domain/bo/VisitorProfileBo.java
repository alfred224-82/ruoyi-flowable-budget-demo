package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * 游客信息补全业务对象 visitor_profile（M31 T31.6）
 *
 * @author ruoyi
 * @date 2026-10-24
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class VisitorProfileBo extends BaseEntity {

    /** 用户ID（PK=FK→sys_user.user_id） */
    @NotNull(message = "用户ID不能为空", groups = {EditGroup.class})
    private Long userId;

    /** 真实姓名 */
    @NotBlank(message = "真实姓名不能为空", groups = {EditGroup.class})
    private String realName;

    /** 身份证号 */
    private String idCard;

    /** 生日 */
    private Date birthday;

    /** 性别：1=男 2=女 */
    private Integer gender;

    /** 地址 */
    private String address;

    /** 微信 openid */
    private String wechatOpenid;

    /** 支付宝 user_id */
    private String alipayUserId;
}
