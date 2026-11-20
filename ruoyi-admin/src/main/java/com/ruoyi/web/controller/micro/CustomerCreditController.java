package com.ruoyi.web.controller.micro;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.vo.CustomerCreditVo;
import com.ruoyi.system.domain.vo.NoShowRecordVo;
import com.ruoyi.system.service.ICustomerCreditService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 顾客管理Controller（T15.32，老板 PC 防爽约/黑名单，★仅 micro_pro 生效）
 * <p>perms 对齐 M15_13 种子菜单：列表 micro:customer:list；移出黑名单 micro:customer:unblacklist（唯一解除出口）；
 * 爽约明细 micro:customer:creditDetail。
 * <p>★黑名单唯一解除出口为老板手动移出：is_blacklisted=0，no_show_count 保留不清零（历史可追溯）；跨租户隔离。
 *
 * @author ruoyi
 * @date 2026-09-08
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/micro/customer")
public class CustomerCreditController extends BaseController {

    private final ICustomerCreditService customerCreditService;

    /**
     * 顾客信誉/黑名单列表（老板 PC 顾客管理）
     *
     * @param userId        可选：按顾客用户ID精确过滤
     * @param isBlacklisted 可选：0正常/1已拉黑
     */
    @SaCheckPermission("micro:customer:list")
    @GetMapping("/list")
    public TableDataInfo<CustomerCreditVo> list(@RequestParam(required = false) Long userId,
                                                @RequestParam(required = false) Integer isBlacklisted,
                                                PageQuery pageQuery) {
        return customerCreditService.queryPageList(userId, isBlacklisted, pageQuery);
    }

    /**
     * ★移出黑名单（micro:customer:unblacklist，唯一解除出口）：is_blacklisted=0，no_show_count 保留不清零。
     */
    @SaCheckPermission("micro:customer:unblacklist")
    @Log(title = "顾客管理", businessType = BusinessType.UPDATE)
    @PutMapping("/unblacklist/{userId}")
    public R<Void> unblacklist(@PathVariable Long userId) {
        return toAjax(customerCreditService.unblacklist(userId));
    }

    /**
     * 顾客爽约明细（micro:customer:creditDetail）：完整爽约历史（no_show_record）。
     */
    @SaCheckPermission("micro:customer:creditDetail")
    @GetMapping("/creditDetail/{userId}")
    public R<List<NoShowRecordVo>> creditDetail(@PathVariable Long userId) {
        return R.ok(customerCreditService.listNoShowRecords(userId));
    }
}
