package com.ruoyi.system.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.*;
import com.ruoyi.system.domain.vo.ExceptionCenterVo;
import com.ruoyi.system.mapper.*;
import com.ruoyi.system.service.IExceptionCenterService;
import com.ruoyi.system.service.IMonthlyFixedCostService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 业务异常处理中心服务实现（T18.8，小微版 M18）
 * <p>★fail-fast：入口判空 + DB 返回判空。
 * <p>聚合四类异常：预约冲突 / 预约失败 / 固定支出异常 / 合规命中。
 * <p>v1.68 修复：固定支出异常为派生异常（无物理来源记录），「已处理」需持久化确认标记
 * （micro_exception_ack，按 租户+类型+当月 唯一），否则列表刷新重新推导导致异常「复活」。
 *
 * @author ruoyi
 * @date 2026-09-17
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExceptionCenterServiceImpl implements IExceptionCenterService {

    private final BookingOrderMapper bookingOrderMapper;
    private final IMonthlyFixedCostService fixedCostService;
    private final VoiceComplianceHitMapper complianceHitMapper;
    private final MicroExceptionAckMapper exceptionAckMapper;
    private final PromotionActivityMapper activityMapper;

    /** 异常类型中文映射 */
    private static final Map<String, String> TYPE_LABEL_MAP = new LinkedHashMap<>();
    static {
        TYPE_LABEL_MAP.put("BOOKING_CONFLICT", "预约冲突");
        TYPE_LABEL_MAP.put("BOOKING_FAILED", "预约失败");
        TYPE_LABEL_MAP.put("FIXED_COST_ANOMALY", "固定支出异常");
        TYPE_LABEL_MAP.put("COMPLIANCE_HIT", "合规命中");
    }

    /** 合规命中类型中文映射 */
    private static final Map<String, String> HIT_TYPE_CN_MAP = new LinkedHashMap<>();
    static {
        HIT_TYPE_CN_MAP.put("NO_TRANSCRIPTION", "语音未转写");
        HIT_TYPE_CN_MAP.put("SENSITIVE_WORD", "敏感词命中");
        HIT_TYPE_CN_MAP.put("POLICY_VIOLATION", "策略违规");
        HIT_TYPE_CN_MAP.put("PASS", "合规通过");
        HIT_TYPE_CN_MAP.put("UNKNOWN", "未知类型");
    }

    @Override
    public ExceptionCenterVo getExceptions() {
        ExceptionCenterVo vo = new ExceptionCenterVo();
        List<ExceptionCenterVo.ExceptionItem> items = new ArrayList<>();

        // ① 预约冲突（orderStatus=CONFLICT）
        List<BookingOrder> conflicts = bookingOrderMapper.selectList(
            Wrappers.<BookingOrder>lambdaQuery()
                .eq(BookingOrder::getOrderStatus, "CONFLICT")
                .orderByDesc(BookingOrder::getCreateTime));
        if (conflicts != null) {
            for (BookingOrder order : conflicts) {
                ExceptionCenterVo.ExceptionItem item = new ExceptionCenterVo.ExceptionItem();
                item.setId(order.getId());
                item.setExceptionType("BOOKING_CONFLICT");
                item.setExceptionTypeLabel("预约冲突");
                item.setSeverity("HIGH");
                item.setTitle("预约时间冲突：" + (order.getServiceName() != null ? order.getServiceName() : "未知服务"));
                item.setDescription("预约订单 " + order.getOrderNo() + " 存在时间冲突，请调整时间或手动处理。");
                item.setOrderNo(order.getOrderNo());
                item.setOccurTime(order.getCreateTime());
                item.setStatus("PENDING");
                item.setSuggestedAction("调整预约时间或联系顾客协商");
                item.setCanRetryBooking(true);
                items.add(item);
            }
        }

        // ② 预约失败（orderStatus=FAILED）
        List<BookingOrder> failed = bookingOrderMapper.selectList(
            Wrappers.<BookingOrder>lambdaQuery()
                .eq(BookingOrder::getOrderStatus, "FAILED")
                .orderByDesc(BookingOrder::getCreateTime));
        if (failed != null) {
            for (BookingOrder order : failed) {
                ExceptionCenterVo.ExceptionItem item = new ExceptionCenterVo.ExceptionItem();
                item.setId(order.getId());
                item.setExceptionType("BOOKING_FAILED");
                item.setExceptionTypeLabel("预约失败");
                item.setSeverity("MEDIUM");
                item.setTitle("预约失败：" + (order.getServiceName() != null ? order.getServiceName() : "未知服务"));
                item.setDescription("预约订单 " + order.getOrderNo() + " 已失败，可一键重新预约（自动带入原参数）。");
                item.setOrderNo(order.getOrderNo());
                item.setOccurTime(order.getCreateTime());
                item.setStatus("PENDING");
                item.setSuggestedAction("一键重新预约");
                item.setCanRetryBooking(true);
                items.add(item);
            }
        }

        // ③ 固定支出异常（本月无活跃支出项，T30.6 分摊覆盖口径；老板当月已点「已处理」则不再推导，避免异常「复活」）
        SimpleDateFormat monthFmt = new SimpleDateFormat("yyyy-MM");
        String currentMonth = monthFmt.format(new Date());
        if (!isFixedCostAnomalyAcked(currentMonth)) {
            long monthCostCount = fixedCostService.countActiveInMonth(currentMonth);
            if (monthCostCount == 0) {
                ExceptionCenterVo.ExceptionItem costItem = new ExceptionCenterVo.ExceptionItem();
                costItem.setId(0L);
                costItem.setExceptionType("FIXED_COST_ANOMALY");
                costItem.setExceptionTypeLabel("固定支出异常");
                costItem.setSeverity("MEDIUM");
                costItem.setTitle("本月未配置固定支出项");
                costItem.setDescription("当前月份 " + currentMonth + " 没有启用的固定支出记录，可能影响利润计算准确性。");
                costItem.setOccurTime(new Date());
                costItem.setStatus("PENDING");
                costItem.setSuggestedAction("前往固定支出管理添加本月支出项");
                costItem.setCanRetryBooking(false);
                items.add(costItem);
            }
        }

        // ④ 合规命中（PENDING 状态 + result=FAIL，近 7 天；PASS 记录不视为异常）
        Calendar cal7 = Calendar.getInstance();
        cal7.add(Calendar.DAY_OF_MONTH, -7);
        List<VoiceComplianceHit> hits = complianceHitMapper.selectList(
            Wrappers.<VoiceComplianceHit>lambdaQuery()
                .ge(VoiceComplianceHit::getHitTime, cal7.getTime())
                .eq(VoiceComplianceHit::getStatus, "PENDING")
                .eq(VoiceComplianceHit::getResult, "FAIL")
                .orderByDesc(VoiceComplianceHit::getHitTime));
        if (hits != null) {
            for (VoiceComplianceHit hit : hits) {
                ExceptionCenterVo.ExceptionItem item = new ExceptionCenterVo.ExceptionItem();
                item.setId(hit.getId());
                item.setExceptionType("COMPLIANCE_HIT");
                item.setExceptionTypeLabel("合规命中");
                item.setSeverity("LOW");
                String hitType = hit.getHitType() != null ? hit.getHitType() : "UNKNOWN";
                String hitTypeCn = HIT_TYPE_CN_MAP.getOrDefault(hitType, hitType);
                // 关联活动名称
                String activityName = null;
                if (hit.getActivityId() != null) {
                    PromotionActivity activity = activityMapper.selectById(hit.getActivityId());
                    if (activity != null) {
                        activityName = activity.getTitle();
                    }
                }
                String titlePrefix = activityName != null ? activityName : "未知活动";
                item.setTitle(titlePrefix + " - " + hitTypeCn);
                item.setDescription(hit.getContentSummary() != null ? hit.getContentSummary() : "语音合规检测命中，请查看详情。");
                item.setOccurTime(hit.getHitTime());
                item.setStatus("PENDING");
                item.setSuggestedAction("查看详情并确认处理");
                item.setCanRetryBooking(false);
                items.add(item);
            }
        }

        // 按严重程度排序：HIGH > MEDIUM > LOW
        Map<String, Integer> severityOrder = new HashMap<>();
        severityOrder.put("HIGH", 0);
        severityOrder.put("MEDIUM", 1);
        severityOrder.put("LOW", 2);
        items.sort(Comparator.comparingInt(a -> severityOrder.getOrDefault(a.getSeverity(), 3)));

        vo.setItems(items);
        vo.setTotalCount(items.size());

        log.info("[ExceptionCenter] totalExceptions={}", items.size());
        return vo;
    }

    /**
     * 固定支出异常当月是否已被老板标记「已处理」
     * <p>★fail-fast：DB 返回 null 视为未确认。
     *
     * @param monthKey 月份键（yyyy-MM）
     * @return true=当月已确认，列表/计数不再推导该异常
     */
    private boolean isFixedCostAnomalyAcked(String monthKey) {
        Long ackCount = exceptionAckMapper.selectCount(
            Wrappers.<MicroExceptionAck>lambdaQuery()
                .eq(MicroExceptionAck::getExceptionType, "FIXED_COST_ANOMALY")
                .eq(MicroExceptionAck::getExceptionKey, monthKey));
        return ackCount != null && ackCount > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolveException(String exceptionType, Long sourceId) {
        if (exceptionType == null || sourceId == null) {
            throw new ServiceException("异常类型和来源ID不能为空");
        }

        switch (exceptionType) {
            case "BOOKING_CONFLICT":
            case "BOOKING_FAILED":
                BookingOrder order = bookingOrderMapper.selectById(sourceId);
                if (order == null) {
                    throw new ServiceException("预约订单不存在");
                }
                // 标记为已处理（更新为 CANCELLED 或由业务逻辑决定）
                BookingOrder update = new BookingOrder();
                update.setId(sourceId);
                update.setOrderStatus("RESOLVED");
                bookingOrderMapper.updateById(update);
                log.info("[ExceptionCenter] resolved booking exception: type={}, orderId={}", exceptionType, sourceId);
                break;

            case "COMPLIANCE_HIT":
                VoiceComplianceHit hit = complianceHitMapper.selectById(sourceId);
                if (hit == null) {
                    throw new ServiceException("合规命中记录不存在");
                }
                VoiceComplianceHit hitUpdate = new VoiceComplianceHit();
                hitUpdate.setId(sourceId);
                hitUpdate.setStatus("RESOLVED");
                complianceHitMapper.updateById(hitUpdate);
                log.info("[ExceptionCenter] resolved compliance hit: id={}", sourceId);
                break;

            case "FIXED_COST_ANOMALY":
                // 派生异常无物理来源记录：按「租户+类型+当月」持久化确认标记（micro_exception_ack），
                // 当月不再推导；配置了固定支出或进入新月后自然失效（防止确认标记永久隐藏异常）
                String ackMonth = new SimpleDateFormat("yyyy-MM").format(new Date());
                if (!isFixedCostAnomalyAcked(ackMonth)) {
                    MicroExceptionAck ack = new MicroExceptionAck();
                    ack.setExceptionType("FIXED_COST_ANOMALY");
                    ack.setExceptionKey(ackMonth);
                    ack.setRemark("老板在异常处理中心标记固定支出异常已处理");
                    exceptionAckMapper.insert(ack);
                }
                log.info("[ExceptionCenter] fixed cost anomaly acknowledged: month={}", ackMonth);
                break;

            default:
                throw new ServiceException("不支持的异常类型：" + exceptionType);
        }
    }
}
