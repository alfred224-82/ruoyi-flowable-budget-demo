package com.ruoyi.system.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 复制布局业务对象（T26.5，小微版 M26）
 * <p>Boss 从已有房间复制布局到目标房间；复制后目标房间拥有独立 room_seat_layout + room_seat 记录，
 * 源房间修改不影响目标房间（V3.5 §16.4 脱钩原则）。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
@Data
public class CopyLayoutBo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 源房间ID（提供布局的房间） */
    @NotNull(message = "源房间ID不能为空", groups = {AddGroup.class})
    private Long sourceRoomId;

    /** 目标房间ID（接收布局的房间；若已有布局则先删后建） */
    @NotNull(message = "目标房间ID不能为空", groups = {AddGroup.class})
    private Long targetRoomId;
}
