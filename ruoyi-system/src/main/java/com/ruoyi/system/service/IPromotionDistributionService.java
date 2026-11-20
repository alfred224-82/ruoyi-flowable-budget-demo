package com.ruoyi.system.service;

import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.CouponBo;
import com.ruoyi.system.domain.bo.PromotionAttributionBo;
import com.ruoyi.system.domain.bo.PromotionCodeBo;
import com.ruoyi.system.domain.vo.*;

import java.util.List;
import java.util.Map;

/**
 * 推广分销服务接口（T16.5/T16.6/T16.10/T16.11，小微版 M16）
 * <p>推广码生成 / 扫码归因 / 老板后台推广管理 / 我的推广码 / 优惠券管理。
 * <p>★全部接口标注 @RequireFeature("FEAT_PROMOTION_DISTRIBUTION")；非小微版返回「当前版本不支持此功能」。
 *
 * @author ruoyi
 * @date 2026-09-12
 */
public interface IPromotionDistributionService {

    // ===== T16.5 推广码 =====

    /**
     * ★生成推广码（首次生成、之后复用；一人一码永久有效）
     * <p>门店码 scene=t=租户ID；个人码 scene=t=租户ID&u=用户名&r=角色（V3.7 微信二维码含 租户+人员+角色）；
     * 超 32 字符时 u 回退为用户ID；scene≤32 字符。
     *
     * @param codeType store/personal
     * @return 推广码VO（含 qr_code_url）
     */
    PromotionCodeVo generateCode(String codeType);

    /**
     * 推广码列表（老板 PC 管理）
     */
    TableDataInfo<PromotionCodeVo> queryCodePage(PromotionCodeBo bo, PageQuery pageQuery);

    /**
     * ★启用/禁用推广码（禁用后新扫码不再归因；禁用前消费照常算）
     */
    int toggleCode(Long id, String status);

    // ===== T16.6 扫码归因 =====

    /**
     * ★扫码归因（解析 scene_param → upsert promotion_attribution）
     * <p>谁最后扫码业绩归谁；推广人 status=禁用则不更新归因；从未扫码→promoter_user_id=NULL 自然流量。
     *
     * @param sceneParam 小程序码 scene 参数
     * @param customerUserId 扫码顾客用户ID
     * @param customerPhone 顾客手机号
     */
    void scanAttribution(String sceneParam, Long customerUserId, String customerPhone);

    /**
     * 消费归因列表（老板 PC 查看）
     */
    TableDataInfo<PromotionAttributionVo> queryAttributionPage(PromotionAttributionBo bo, PageQuery pageQuery);

    // ===== T16.10 老板后台推广统计 =====

    /**
     * 推广数据统计（按推广人汇总：带来人数/已消费/拉新成功数）
     */
    List<Map<String, Object>> queryPromotionStat(String period);

    /**
     * 推广业绩统计（按天/周/月）
     */
    List<Map<String, Object>> queryPerformanceStat(String period);

    // ===== T16.11 我的推广码（小程序） =====

    /**
     * ★我的推广码 + 推广数据 + 奖励/优惠券到账（小程序端）
     */
    Map<String, Object> myPromotion();

    /**
     * ★我的微信小程序码（V3.7 底部「我的推广码」Tab）：生成/复用个人推广码后，
     * 以 scene=t=租户ID&u=用户名&r=角色 调 wxacode.getUnlimited 返回 base64 小程序码图片。
     *
     * @return {scene, qrcodeUrl, userName, roleKey, tenantId}（微信 appid/secret 未配置时 qrcodeUrl 为空）
     */
    Map<String, Object> generateMyQrcode();

    // ===== T16.3 优惠券管理 =====

    /**
     * 优惠券列表
     */
    TableDataInfo<CouponVo> queryCouponPage(CouponBo bo, PageQuery pageQuery);

    /**
     * 优惠券详情
     */
    CouponVo queryCouponById(Long id);

    /**
     * 新增优惠券
     */
    Long insertCoupon(CouponBo bo);

    /**
     * 编辑优惠券
     */
    int updateCoupon(CouponBo bo);

    /**
     * 删除优惠券
     */
    int deleteCoupon(Long[] ids);

    /**
     * 我的优惠券列表（小程序端，按当前登录顾客）
     */
    List<CouponRecordVo> myCoupons();
}
