package com.ruoyi.web.controller.fee;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.PurposeTagBo;
import com.ruoyi.system.domain.vo.PurposeTagVo;
import com.ruoyi.system.service.IPurposeTagService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 用途标签管理Controller
 *
 * @author ruoyi
 * @date 2026-07-13
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/fee/purposeTag")
public class PurposeTagController extends BaseController {

    private final IPurposeTagService purposeTagService;

    /**
     * 查询用途标签列表
     */
    @SaCheckPermission("fee:purposeTag:list")
    @GetMapping("/list")
    public TableDataInfo<PurposeTagVo> list(PurposeTagBo bo, PageQuery pageQuery) {
        Page<PurposeTagVo> page = purposeTagService.selectPageList(bo, pageQuery);
        return TableDataInfo.build(page);
    }

    /**
     * 获取全部启用标签（下拉用，无需权限）
     */
    @GetMapping("/allEnabled")
    public R<List<PurposeTagVo>> allEnabled() {
        return R.ok(purposeTagService.queryAllEnabled());
    }

    /**
     * 获取用途标签详细信息
     */
    @SaCheckPermission("fee:purposeTag:query")
    @GetMapping("/{id}")
    public R<PurposeTagVo> getInfo(@PathVariable Long id) {
        return R.ok(purposeTagService.queryById(id));
    }

    /**
     * 新增用途标签
     */
    @SaCheckPermission("fee:purposeTag:add")
    @Log(title = "用途标签", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Void> add(@Validated(AddGroup.class) @RequestBody PurposeTagBo bo) {
        return toAjax(purposeTagService.insertByBo(bo));
    }

    /**
     * 修改用途标签
     */
    @SaCheckPermission("fee:purposeTag:edit")
    @Log(title = "用途标签", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody PurposeTagBo bo) {
        return toAjax(purposeTagService.updateByBo(bo));
    }

    /**
     * 删除用途标签
     */
    @SaCheckPermission("fee:purposeTag:remove")
    @Log(title = "用途标签", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(purposeTagService.deleteWithValidByIds(Arrays.asList(ids)));
    }
}
