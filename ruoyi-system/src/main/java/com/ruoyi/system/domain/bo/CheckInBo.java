package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 员工打卡提交业务对象（T24.1，小微版 M24）
 * <p>员工小程序打卡提交：活动ID + 拍照 + 位置 + 水印。
 *
 * @author ruoyi
 * @date 2026-10-12
 */
@Data
public class CheckInBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 活动ID */
    @NotNull(message = "活动ID不能为空", groups = {AddGroup.class})
    private Long activityId;

    /** 打卡照片URL（OSS） */
    private String photoUrl;

    /** 打卡地点名称 */
    private String locationName;

    /** 纬度 */
    private BigDecimal locationLat;

    /** 经度 */
    private BigDecimal locationLng;

    /** 水印文字（前端生成：时间/位置/用户名） */
    private String watermarkText;

    /** 备注 */
    private String remark;
}
