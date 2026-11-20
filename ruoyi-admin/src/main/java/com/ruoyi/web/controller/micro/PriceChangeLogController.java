package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.bo.PriceChangeLogBo;
import com.ruoyi.system.domain.vo.PriceChangeLogVo;
import com.ruoyi.system.service.IPriceChangeLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 价格调整记录Controller（T15.4/T15.11 ④ 改价历史查询）
 * <p>只读查询：改价记录由服务项目改价（编辑/改价确认）时自动写入，本控制器不提供写入口。
 * <p>perms 对齐 M15_06 种子菜单：micro:priceLog:list。
 *
 * @author ruoyi
 * @date 2026-09-07
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/priceLog")
public class PriceChangeLogController extends BaseController {

    private final IPriceChangeLogService priceChangeLogService;

    /**
     * 改价记录列表（按服务项目 + 时间范围过滤，change_time 倒序）
     */
    @SaCheckPermission("micro:priceLog:list")
    @GetMapping("/list")
    public TableDataInfo<PriceChangeLogVo> list(PriceChangeLogBo bo, PageQuery pageQuery) {
        return priceChangeLogService.queryPageList(bo, pageQuery);
    }

    /**
     * 指定服务项目的改价历史
     */
    @SaCheckPermission("micro:priceLog:list")
    @GetMapping("/item/{itemId}")
    public R<List<PriceChangeLogVo>> listByItem(@PathVariable Long itemId) {
        return R.ok(priceChangeLogService.listByItem(itemId));
    }
}
