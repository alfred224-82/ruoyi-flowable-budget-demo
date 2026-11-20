package com.ruoyi.system.annotation;

import com.ruoyi.system.domain.enums.SeatOperationType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 座位编排操作日志注解（T28.4，小微版 M28）
 * <p>标注在 Service 方法上，由 {@code SeatOperationLogAspect} 环绕拦截，
 * 自动记录操作前后座位快照到 {@code activity_seat_operation_log}，避免每个方法手写日志代码。
 * <p>★被标注方法须满足：入参中可解析出 activityId——
 * <ul>
 *   <li>入参含 {@code getActivityId()} 的 Bo（assign / move / swap / moveMine）；或</li>
 *   <li>首个 {@code Long} 入参即 activityId（lock / unlock）。</li>
 * </ul>
 * <p>★切面在事务边界之外执行（{@code @Order}），proceed() 成功返回（事务已提交）后再落日志；
 * 抛异常则不记录（失败操作不留痕）。
 *
 * @author ruoyi
 * @date 2026-10-13
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SeatOperation {

    /**
     * 操作类型
     */
    SeatOperationType value();

    /**
     * 备注（可选，写入 activity_seat_operation_log.remark）
     */
    String remark() default "";
}
