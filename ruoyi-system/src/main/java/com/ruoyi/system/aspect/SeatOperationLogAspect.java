package com.ruoyi.system.aspect;

import com.ruoyi.system.annotation.SeatOperation;
import com.ruoyi.system.service.IActivitySeatOperationLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

/**
 * 座位编排操作日志切面（T28.4，小微版 M28）
 * <p>环绕拦截所有标注 {@link SeatOperation} 的 Service 方法，自动记录操作前后的座位快照到
 * {@code activity_seat_operation_log}，避免每个 Service 方法手写日志代码。
 * <p>★执行顺序：{@code @Order(1)} 使本切面位于事务边界之外——proceed() 返回时事务已提交，
 * after 快照读取的是已落库的最新座位状态；proceed() 抛异常则不记录日志（失败操作不留痕）。
 * <p>★activityId 解析：优先取入参 Bo 的 {@code getActivityId()}；否则取首个 {@code Long} 入参（lock/unlock）。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
@Slf4j
@Aspect
@Component
@Order(1)
@RequiredArgsConstructor
public class SeatOperationLogAspect {

    private final IActivitySeatOperationLogService operationLogService;

    @Around("@annotation(seatOp)")
    public Object around(ProceedingJoinPoint pjp, SeatOperation seatOp) throws Throwable {
        Long activityId = extractActivityId(pjp.getArgs());
        // activityId 无法解析时不阻断主流程，仅跳过日志（降级）
        String before = activityId != null ? operationLogService.snapshotSeats(activityId) : null;
        Object result = pjp.proceed();
        if (activityId != null) {
            String after = operationLogService.snapshotSeats(activityId);
            String remark = seatOp.remark().isEmpty() ? seatOp.value().getLabel() : seatOp.remark();
            operationLogService.record(activityId, seatOp.value().getCode(), before, after, remark);
        } else {
            log.warn("[SeatOpLogAspect] 未能从入参解析 activityId，跳过操作日志：method={}", resolveMethodName(pjp));
        }
        return result;
    }

    /**
     * 从方法入参解析 activityId：
     * <ol>
     *   <li>入参对象含 {@code getActivityId()} 方法 → 反射调用取值（assign/move/swap/moveMine 的 Bo）</li>
     *   <li>入参为 {@code Long} → 直接作为 activityId（lock/unlock 的 activityId 参数）</li>
     * </ol>
     */
    private Long extractActivityId(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        // ① 优先从 Bo 的 getActivityId() 解析
        for (Object arg : args) {
            if (arg == null) {
                continue;
            }
            try {
                Method getter = arg.getClass().getMethod("getActivityId");
                Object val = getter.invoke(arg);
                if (val instanceof Long) {
                    return (Long) val;
                }
            } catch (NoSuchMethodException ignored) {
                // 该入参无 getActivityId()，继续尝试下一个
            } catch (Exception e) {
                log.debug("[SeatOpLogAspect] 反射解析 getActivityId 失败：{}", arg.getClass().getName(), e);
            }
        }
        // ② 退化：取首个 Long 入参（lock/unlock 场景 activityId 为唯一 Long 参数）
        for (Object arg : args) {
            if (arg instanceof Long) {
                return (Long) arg;
            }
        }
        return null;
    }

    private String resolveMethodName(ProceedingJoinPoint pjp) {
        try {
            return pjp.getSignature().getDeclaringType().getSimpleName() + "#" + pjp.getSignature().getName();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
