package com.ruoyi.web.controller.biz;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.bo.LiveAnnouncementBo;
import com.ruoyi.system.domain.vo.LiveAnnouncementVo;
import com.ruoyi.system.domain.vo.LiveBoardVo;
import com.ruoyi.system.service.ILiveAnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;

/**
 * 直播公告Controller（T12.6 状态机 / T12.8 视频号闭环）
 * <p>业务状态由运营维护，展示态由服务端派生；board 接口供 PC/小程序首页直播位。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/biz/announcement")
public class LiveAnnouncementController extends BaseController {

    private final ILiveAnnouncementService announcementService;

    /**
     * 公告列表（后台运营，含派生展示态）
     */
    @SaCheckPermission("biz:announcement:list")
    @GetMapping("/list")
    public TableDataInfo<LiveAnnouncementVo> list(LiveAnnouncementBo bo, PageQuery pageQuery) {
        return announcementService.queryPageList(bo, pageQuery);
    }

    /**
     * 首页直播看板（仅可见公告，LIVING 优先；无则 hasAnnouncement=false）
     */
    @GetMapping("/board")
    public R<LiveBoardVo> board(@RequestParam(required = false) Long projectId) {
        return R.ok(announcementService.currentBoard(projectId));
    }

    /**
     * 公告详情
     */
    @SaCheckPermission("biz:announcement:query")
    @GetMapping("/{id}")
    public R<LiveAnnouncementVo> getInfo(@PathVariable Long id) {
        return R.ok(announcementService.queryById(id));
    }

    /**
     * 新建公告
     */
    @SaCheckPermission("biz:announcement:add")
    @Log(title = "直播公告", businessType = BusinessType.INSERT)
    @PostMapping
    public R<Long> add(@Validated(AddGroup.class) @RequestBody LiveAnnouncementBo bo) {
        return R.ok(announcementService.insert(bo));
    }

    /**
     * 编辑公告
     */
    @SaCheckPermission("biz:announcement:edit")
    @Log(title = "直播公告", businessType = BusinessType.UPDATE)
    @PutMapping
    public R<Void> edit(@Validated(EditGroup.class) @RequestBody LiveAnnouncementBo bo) {
        return toAjax(announcementService.update(bo));
    }

    /**
     * 开播（PENDING→LIVING）
     */
    @SaCheckPermission("biz:announcement:edit")
    @Log(title = "直播公告", businessType = BusinessType.UPDATE)
    @PutMapping("/startLiving/{id}")
    public R<Void> startLiving(@PathVariable Long id) {
        return toAjax(announcementService.startLiving(id));
    }

    /**
     * 下播（LIVING→OFFLINE）
     */
    @SaCheckPermission("biz:announcement:edit")
    @Log(title = "直播公告", businessType = BusinessType.UPDATE)
    @PutMapping("/offline/{id}")
    public R<Void> offline(@PathVariable Long id) {
        return toAjax(announcementService.offline(id));
    }

    /**
     * 删除公告
     */
    @SaCheckPermission("biz:announcement:remove")
    @Log(title = "直播公告", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(announcementService.deleteByIds(Arrays.asList(ids)));
    }
}
