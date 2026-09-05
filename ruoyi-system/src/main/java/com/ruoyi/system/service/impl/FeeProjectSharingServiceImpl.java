package com.ruoyi.system.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.hutool.core.util.StrUtil;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.FeeProject;
import com.ruoyi.system.domain.FeeProjectSharing;
import com.ruoyi.system.domain.vo.FeeProjectSharingVo;
import com.ruoyi.system.mapper.FeeProjectMapper;
import com.ruoyi.system.mapper.FeeProjectSharingMapper;
import com.ruoyi.system.service.IFeeNotificationService;
import com.ruoyi.system.service.IFeeProjectSharingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 项目分成Service实现（T5.9 / ADR-025）
 * 老板定比例：审批时设定分成比例与基数，计算分成金额；基数缺省按基数类型从项目数据推导
 *
 * @author ruoyi
 * @date 2026-08-29
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeProjectSharingServiceImpl implements IFeeProjectSharingService {

    private final FeeProjectSharingMapper sharingMapper;
    private final FeeProjectMapper projectMapper;
    private final IFeeNotificationService notificationService;

    @Override
    public TableDataInfo<FeeProjectSharingVo> queryPageList(Long projectId, String status, PageQuery pageQuery) {
        Page<FeeProjectSharingVo> page = sharingMapper.selectVoPage(pageQuery.build(),
            Wrappers.<FeeProjectSharing>lambdaQuery()
                .eq(projectId != null, FeeProjectSharing::getProjectId, projectId)
                .eq(StrUtil.isNotBlank(status), FeeProjectSharing::getStatus, status)
                .orderByDesc(FeeProjectSharing::getCreateTime));
        fillProjectName(page.getRecords());
        return TableDataInfo.build(page);
    }

    @Override
    public FeeProjectSharingVo queryById(Long id) {
        FeeProjectSharingVo vo = sharingMapper.selectVoById(id);
        if (vo == null) {
            throw new ServiceException("分成记录不存在");
        }
        fillProjectName(List.of(vo));
        return vo;
    }

    @Override
    public Long insertSharing(FeeProjectSharing sharing) {
        if (sharing.getProjectId() == null || sharing.getShareToUserId() == null) {
            throw new ServiceException("分成必须关联项目与分成对象");
        }
        FeeProject project = projectMapper.selectById(sharing.getProjectId());
        if (project == null) {
            throw new ServiceException("关联项目不存在");
        }
        sharing.setStatus("PENDING");
        if (StrUtil.isBlank(sharing.getRole())) {
            sharing.setRole("INTRODUCER");
        }
        if (StrUtil.isBlank(sharing.getBaseType())) {
            sharing.setBaseType("CONTRACT");
        }
        sharingMapper.insert(sharing);

        // 通知老板审批（角色广播，T5.12）
        try {
            notificationService.notifyRole("boss", "新分成申请待审批",
                "项目【" + project.getProjectName() + "】提交分成申请，对象："
                    + StrUtil.nullToEmpty(sharing.getShareToUserName()) + "，请及时审批",
                "APPROVAL", "PROJECT_SHARING", sharing.getId());
        } catch (Exception e) {
            log.warn("分成申请通知发送失败（不阻断主流程）: {}", e.getMessage());
        }
        return sharing.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int approveSharing(Long id, BigDecimal ratio, BigDecimal baseAmount) {
        FeeProjectSharing sharing = sharingMapper.selectById(id);
        if (sharing == null) {
            throw new ServiceException("分成记录不存在");
        }
        if (!"PENDING".equals(sharing.getStatus())) {
            throw new ServiceException("仅待审批状态的分成可申请审批");
        }
        if (ratio == null || ratio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("分成比例由老板设定，必须大于0");
        }
        // 基数缺省时按基数类型从项目数据推导
        BigDecimal base = baseAmount != null ? baseAmount : resolveBaseAmount(sharing);
        BigDecimal shareAmount = base.multiply(ratio).setScale(2, RoundingMode.HALF_UP);

        FeeProjectSharing update = new FeeProjectSharing();
        update.setId(id);
        update.setRatio(ratio);
        update.setBaseAmount(base);
        update.setShareAmount(shareAmount);
        update.setStatus("ACTIVE");
        int rows = sharingMapper.updateById(update);

        try {
            notificationService.notifyRole("finance", "分成申请已审批",
                "分成申请已审批通过，分成金额 " + shareAmount + " 元，已计入项目分成",
                "APPROVAL", "PROJECT_SHARING", id);
        } catch (Exception e) {
            log.warn("分成审批通知发送失败（不阻断主流程）: {}", e.getMessage());
        }
        return rows;
    }

    @Override
    public int rejectSharing(Long id) {
        FeeProjectSharing sharing = sharingMapper.selectById(id);
        if (sharing == null) {
            throw new ServiceException("分成记录不存在");
        }
        if (!"PENDING".equals(sharing.getStatus())) {
            throw new ServiceException("仅待审批状态的分成可驳回");
        }
        // 基础版简化：驳回直接删除待审批记录，申请人可重新提交
        return sharingMapper.deleteById(id);
    }

    @Override
    public int paySharing(Long id) {
        FeeProjectSharing sharing = sharingMapper.selectById(id);
        if (sharing == null) {
            throw new ServiceException("分成记录不存在");
        }
        if (!"ACTIVE".equals(sharing.getStatus())) {
            throw new ServiceException("仅生效状态的分成可标记支付");
        }
        FeeProjectSharing update = new FeeProjectSharing();
        update.setId(id);
        update.setStatus("PAID");
        return sharingMapper.updateById(update);
    }

    /**
     * 按基数类型推导分成基数：合同额/已核验收入/实际利润
     */
    private BigDecimal resolveBaseAmount(FeeProjectSharing sharing) {
        FeeProject project = projectMapper.selectById(sharing.getProjectId());
        if (project == null) {
            throw new ServiceException("关联项目不存在");
        }
        if (sharing.getBaseAmount() != null) {
            return sharing.getBaseAmount();
        }
        switch (StrUtil.nullToEmpty(sharing.getBaseType())) {
            case "CONTRACT":
                return zero(project.getCurrentContractAmount());
            case "INCOME":
                return zero(project.getVerifiedIncome());
            case "PROFIT":
                // 纸面利润简化：已核验收入 - 总支出
                return zero(project.getVerifiedIncome()).subtract(zero(project.getTotalExpense()));
            default:
                throw new ServiceException("固定基数类型必须填写分成基数");
        }
    }

    /**
     * 批量填充项目名称
     */
    private void fillProjectName(List<FeeProjectSharingVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> projectIds = list.stream().map(FeeProjectSharingVo::getProjectId)
            .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (CollUtil.isEmpty(projectIds)) {
            return;
        }
        Map<Long, FeeProject> projectMap = projectMapper.selectBatchIds(projectIds).stream()
            .collect(Collectors.toMap(FeeProject::getId, Function.identity()));
        list.forEach(vo -> {
            FeeProject project = projectMap.get(vo.getProjectId());
            vo.setProjectName(project == null ? null : project.getProjectName());
        });
    }

    private BigDecimal zero(BigDecimal value) {
        return Objects.requireNonNullElse(value, BigDecimal.ZERO);
    }
}
