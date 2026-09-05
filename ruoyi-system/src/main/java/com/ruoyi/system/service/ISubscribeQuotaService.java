package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.vo.SysSubscribeQuotaVo;

/**
 * 订阅消息授权次数服务接口（T14.4 微信一次性订阅消息合规）
 * <p>微信一次性订阅：用户每授权一次仅可下发一条。发送前必须校验剩余授权次数，
 * 次数不足则【不发送、不报错】，由业务侧引导用户重新授权。
 * <p>次数变更采用原子条件 UPDATE，防并发超发。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface ISubscribeQuotaService {

    /**
     * 记录一次用户订阅授权：剩余次数 +1、累计授权 +1（无记录则初始化）
     *
     * @param openid     微信 openid
     * @param templateId 订阅消息模板ID
     * @return 影响行数
     */
    int grant(String openid, String templateId);

    /**
     * 发送前校验并消费一次授权：剩余次数 &gt;0 则 -1、累计发送 +1 并返回 true；
     * 否则返回 false（调用方据此跳过发送，不报错）。
     *
     * @return 是否允许发送
     */
    boolean checkAndConsume(String openid, String templateId);

    /**
     * 查询某 openid 某模板的剩余授权次数（无记录返回 0）
     */
    int remaining(String openid, String templateId);

    /**
     * 授权次数分页查询
     *
     * @param openid 微信 openid（可空）
     */
    TableDataInfo<SysSubscribeQuotaVo> queryPageList(String openid, PageQuery pageQuery);
}
