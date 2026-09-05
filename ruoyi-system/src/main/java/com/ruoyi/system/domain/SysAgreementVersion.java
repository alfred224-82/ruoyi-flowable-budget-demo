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
 * 协议版本对象 sys_agreement_version（T14.4 隐私政策/用户协议版本留痕）
 * <p>系统级共享表，不含 tenant_id（已加入 IGNORE_TENANT_TABLES）；表仅含 create_time/update_time，故不继承 BaseEntity。
 * <p>发布新版本时旧版转历史（status=1）；同类型同版本唯一。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Data
@TableName("sys_agreement_version")
public class SysAgreementVersion implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键ID */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /** 协议类型：privacy(隐私政策)/user(用户协议) */
    private String agreementType;

    /** 版本号(如 v1.0) */
    private String version;

    /** 协议标题 */
    private String title;

    /** 协议正文URL(OSS) */
    private String contentUrl;

    /** 正文哈希(防篡改) */
    private String contentHash;

    /** 发布时间 */
    private Date publishTime;

    /** 状态（0生效 1历史） */
    private String status;

    /** 创建者 */
    @TableField(fill = FieldFill.INSERT)
    private String createBy;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    /** 备注 */
    private String remark;
}
