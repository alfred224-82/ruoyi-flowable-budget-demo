package com.ruoyi.framework.handler;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpStatus;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.ruoyi.common.core.domain.BaseEntity;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;

import java.util.Date;

/**
 * MP注入处理器
 *
 * @author Lion Li
 * @date 2021/4/25
 */
@Slf4j
public class CreateAndUpdateMetaObjectHandler implements MetaObjectHandler {

    @Override
    public void insertFill(MetaObject metaObject) {
        try {
            if (ObjectUtil.isNull(metaObject)) {
                return;
            }
            // 审计字段填充：仅 BaseEntity 及其子类（plain 实体的时间列依赖 DB DEFAULT / ON UPDATE 兜底）
            if (metaObject.getOriginalObject() instanceof BaseEntity) {
                BaseEntity baseEntity = (BaseEntity) metaObject.getOriginalObject();
                Date current = ObjectUtil.isNotNull(baseEntity.getCreateTime())
                    ? baseEntity.getCreateTime() : new Date();
                baseEntity.setCreateTime(current);
                baseEntity.setUpdateTime(current);
                String username = StringUtils.isNotBlank(baseEntity.getCreateBy())
                    ? baseEntity.getCreateBy() : getLoginUsername();
                // 当前已登录 且 创建人为空 则填充
                baseEntity.setCreateBy(username);
                // 当前已登录 且 更新人为空 则填充
                baseEntity.setUpdateBy(username);
            }
            // 租户ID自动填充：对任意含 tenantId 属性的实体生效，兼容不继承 BaseEntity 的 plain 实体
            // （如 biz_image_task / biz_message_log / biz_content_security_log / open_api_call_log 等）。
            // 这些实体的 tenantId 标注了 @TableField(fill = INSERT)，MP 会把该列强制写入 INSERT，
            // 若此处不填充则落 null，触发 "Column 'tenant_id' cannot be null"。
            // 注意：网关鉴权 / 定时任务 / 超管跨租户等无登录租户上下文时 getTenantId() 为 null，
            // 此处不填充，须由 Service 层显式 setTenantId(目标租户)。
            if (metaObject.hasSetter("tenantId") && ObjectUtil.isNull(metaObject.getValue("tenantId"))) {
                Long tenantId = LoginHelper.getTenantId();
                if (ObjectUtil.isNotNull(tenantId)) {
                    metaObject.setValue("tenantId", tenantId);
                }
            }
        } catch (Exception e) {
            throw new ServiceException("自动注入异常 => " + e.getMessage(), HttpStatus.HTTP_UNAUTHORIZED);
        }
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        try {
            if (ObjectUtil.isNotNull(metaObject) && metaObject.getOriginalObject() instanceof BaseEntity) {
                BaseEntity baseEntity = (BaseEntity) metaObject.getOriginalObject();
                Date current = new Date();
                // 更新时间填充(不管为不为空)
                baseEntity.setUpdateTime(current);
                String username = getLoginUsername();
                // 当前已登录 更新人填充(不管为不为空)
                if (StringUtils.isNotBlank(username)) {
                    baseEntity.setUpdateBy(username);
                }
            }
        } catch (Exception e) {
            throw new ServiceException("自动注入异常 => " + e.getMessage(), HttpStatus.HTTP_UNAUTHORIZED);
        }
    }

    /**
     * 获取登录用户名
     */
    private String getLoginUsername() {
        LoginUser loginUser;
        try {
            loginUser = LoginHelper.getLoginUser();
        } catch (Exception e) {
            log.warn("自动注入警告 => 用户未登录");
            return null;
        }
        return ObjectUtil.isNotNull(loginUser) ? loginUser.getUsername() : null;
    }

}
