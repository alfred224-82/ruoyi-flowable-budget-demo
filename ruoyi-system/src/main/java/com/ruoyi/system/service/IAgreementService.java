package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.AgreementVersionBo;
import com.ruoyi.system.domain.vo.SysAgreementVersionVo;
import com.ruoyi.system.domain.vo.SysUserAgreementLogVo;

import java.util.List;

/**
 * 隐私政策/用户协议服务接口（T14.4 版本留痕 + 用户同意留痕）
 * <p>sys_agreement_version 为系统级共享表；发布新版本时同类型旧版本自动转历史（status=1）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
public interface IAgreementService {

    /**
     * 发布协议新版本：同类型旧版转历史，新版置生效（status=0）并计算正文哈希
     *
     * @return 影响行数
     */
    int publish(AgreementVersionBo bo);

    /**
     * 当前各类型生效版本列表（status=0）
     */
    List<SysAgreementVersionVo> listActive();

    /**
     * 指定类型的全部版本（按发布时间倒序）
     *
     * @param agreementType 协议类型（可空=全部）
     */
    List<SysAgreementVersionVo> listVersions(String agreementType);

    /**
     * 协议版本详情
     */
    SysAgreementVersionVo queryById(Long id);

    /**
     * 记录用户同意某版本协议（留痕：用户/openid/类型/版本/IP/时间）
     *
     * @param agreementType 协议类型 privacy/user
     * @param version       同意的版本号
     * @param openid        微信 openid（小程序端，可空）
     * @param clientIp      来源IP
     * @return 影响行数
     */
    int recordAgree(String agreementType, String version, String openid, String clientIp);

    /**
     * 用户同意留痕分页查询
     *
     * @param agreementType 协议类型（可空）
     * @param userId        用户ID（可空）
     */
    TableDataInfo<SysUserAgreementLogVo> queryAgreeLogPageList(String agreementType, Long userId, PageQuery pageQuery);
}
