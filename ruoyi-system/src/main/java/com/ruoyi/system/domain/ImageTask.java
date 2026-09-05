package com.ruoyi.system.domain;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 异步衍生图任务对象 biz_image_task（T12.7 上传即返回，后台生成完成后回写 URL）
 * <p>Logo/海报成品生成队列；失败重试与告警。表仅含 create_time/update_time，故不继承 BaseEntity。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@TableName("biz_image_task")
public class ImageTask implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 租户ID
     */
    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;

    /**
     * 关联 tenant_theme.id（可空）
     */
    private Long themeId;

    /**
     * 任务类型：logo_render(80x80)/poster_card(750x400)/poster_detail(750x1000)
     */
    private String taskType;

    /**
     * 原图URL
     */
    private String sourceUrl;

    /**
     * 目标规格：80x80/750x400/750x1000
     */
    private String spec;

    /**
     * 状态：PENDING/PROCESSING/SUCCESS/FAILED
     */
    private String status;

    /**
     * 成品URL（成功后回写 tenant_theme）
     */
    private String resultUrl;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 失败原因
     */
    private String errorMsg;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;
}
