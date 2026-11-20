package com.ruoyi.system.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户下拉视图对象（项目成员选择用，仅返回报销人角色人员）
 *
 * @author ruoyi
 * @date 2026-09-06
 */
@Data
public class FeeUserOptionVo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户ID */
    private Long userId;
    /** 用户账号 */
    private String userName;
    /** 用户姓名（昵称） */
    private String nickName;
}
