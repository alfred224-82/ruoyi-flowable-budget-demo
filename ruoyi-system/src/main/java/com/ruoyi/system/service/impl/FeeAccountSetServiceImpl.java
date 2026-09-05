package com.ruoyi.system.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.FeeAccountSet;
import com.ruoyi.system.domain.FeeOrg;
import com.ruoyi.system.domain.bo.FeeAccountSetBo;
import com.ruoyi.system.domain.vo.FeeAccountSetReportVo;
import com.ruoyi.system.domain.vo.FeeAccountSetVo;
import com.ruoyi.system.mapper.FeeAccountSetMapper;
import com.ruoyi.system.mapper.FeeOrgMapper;
import com.ruoyi.system.service.IFeeAccountSetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 多账套Service实现（T7.7 / FEAT_MULTI_ACCOUNT_SET）
 * <p>账套 CRUD + 默认账套唯一 + 切换上下文 + 合并报表（各账套已审批单据汇总）。
 * 租户隔离由多租户拦截器自动完成（fee_account_set / fee_expense_bill 均含 tenant_id）。
 *
 * @author ruoyi
 * @date 2026-09-05
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeeAccountSetServiceImpl implements IFeeAccountSetService {

    /** 当前操作账套上下文键（token 会话级） */
    private static final String CURRENT_ACCOUNT_SET_KEY = "currentAccountSetId";

    /** 默认账套标记 */
    private static final Integer DEFAULT_YES = 1;

    private final FeeAccountSetMapper accountSetMapper;
    private final FeeOrgMapper orgMapper;

    @Override
    public Page<FeeAccountSetVo> queryPageList(FeeAccountSetBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<FeeAccountSet> lqw = buildQueryWrapper(bo);
        Page<FeeAccountSetVo> page = accountSetMapper.selectVoPage(pageQuery.build(), lqw);
        fillOrgName(page.getRecords());
        return page;
    }

    @Override
    public List<FeeAccountSetVo> listAll() {
        List<FeeAccountSetVo> list = accountSetMapper.selectVoList(Wrappers.<FeeAccountSet>lambdaQuery()
            .orderByDesc(FeeAccountSet::getIsDefault)
            .orderByAsc(FeeAccountSet::getId));
        fillOrgName(list);
        return list;
    }

    @Override
    public FeeAccountSetVo queryById(Long id) {
        FeeAccountSetVo vo = accountSetMapper.selectVoById(id);
        if (vo != null) {
            fillOrgName(Collections.singletonList(vo));
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long insertByBo(FeeAccountSetBo bo) {
        checkCodeUnique(bo.getCode(), null);
        FeeAccountSet entity = BeanUtil.toBean(bo, FeeAccountSet.class);
        if (StrUtil.isBlank(entity.getStatus())) {
            entity.setStatus("ACTIVE");
        }
        if (StrUtil.isBlank(entity.getFiscalYearStart())) {
            entity.setFiscalYearStart("01-01");
        }
        // 未显式指定默认标记时：若当前无任何默认账套，则首个账套自动设为默认
        if (entity.getIsDefault() == null) {
            boolean noneDefault = accountSetMapper.selectCount(
                Wrappers.<FeeAccountSet>lambdaQuery().eq(FeeAccountSet::getIsDefault, DEFAULT_YES)) == 0;
            entity.setIsDefault(noneDefault ? DEFAULT_YES : 0);
        }
        accountSetMapper.insert(entity);
        if (DEFAULT_YES.equals(entity.getIsDefault())) {
            clearOtherDefault(entity.getId());
        }
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateByBo(FeeAccountSetBo bo) {
        FeeAccountSet exist = accountSetMapper.selectById(bo.getId());
        if (exist == null) {
            throw new ServiceException("账套不存在");
        }
        if (StrUtil.isNotBlank(bo.getCode())) {
            checkCodeUnique(bo.getCode(), bo.getId());
        }
        FeeAccountSet entity = BeanUtil.toBean(bo, FeeAccountSet.class);
        boolean updated = accountSetMapper.updateById(entity) > 0;
        if (updated && DEFAULT_YES.equals(bo.getIsDefault())) {
            clearOtherDefault(bo.getId());
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return false;
        }
        for (Long id : ids) {
            if (accountSetMapper.countBillsByAccountSet(id) > 0) {
                throw new ServiceException("账套下存在报销单据，禁止删除");
            }
        }
        List<FeeAccountSet> targets = accountSetMapper.selectBatchIds(ids);
        boolean deletingDefault = targets.stream().anyMatch(a -> DEFAULT_YES.equals(a.getIsDefault()));
        long remaining = accountSetMapper.selectCount(
            Wrappers.<FeeAccountSet>lambdaQuery().notIn(FeeAccountSet::getId, ids));
        if (deletingDefault && remaining > 0) {
            throw new ServiceException("不能删除默认账套，请先将其它账套设为默认");
        }
        return accountSetMapper.deleteBatchIds(ids) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean setDefault(Long id) {
        FeeAccountSet exist = accountSetMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("账套不存在");
        }
        clearOtherDefault(id);
        FeeAccountSet entity = new FeeAccountSet();
        entity.setId(id);
        entity.setIsDefault(DEFAULT_YES);
        return accountSetMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean switchAccountSet(Long id) {
        FeeAccountSet exist = accountSetMapper.selectById(id);
        if (exist == null) {
            throw new ServiceException("账套不存在");
        }
        if (!"ACTIVE".equals(exist.getStatus())) {
            throw new ServiceException("账套未启用，无法切换");
        }
        StpUtil.getTokenSession().set(CURRENT_ACCOUNT_SET_KEY, id);
        return true;
    }

    @Override
    public Long currentOrDefaultAccountSetId() {
        try {
            Object v = StpUtil.getTokenSession().get(CURRENT_ACCOUNT_SET_KEY);
            if (v != null) {
                return Long.valueOf(v.toString());
            }
        } catch (Exception ignored) {
            // 未登录或会话不可用时回退默认账套
        }
        FeeAccountSet def = accountSetMapper.selectOne(Wrappers.<FeeAccountSet>lambdaQuery()
            .eq(FeeAccountSet::getIsDefault, DEFAULT_YES)
            .last("LIMIT 1"));
        return def == null ? null : def.getId();
    }

    @Override
    public List<FeeAccountSetReportVo> consolidatedReport(String period) {
        List<FeeAccountSetVo> accountSets = accountSetMapper.selectVoList(Wrappers.<FeeAccountSet>lambdaQuery());
        // 默认账套承接 account_set_id=NULL 的存量单据
        Long defaultId = accountSets.stream()
            .filter(v -> DEFAULT_YES.equals(v.getIsDefault()))
            .map(FeeAccountSetVo::getId)
            .findFirst().orElse(null);

        Map<Long, Long> countMap = new HashMap<>();
        Map<Long, BigDecimal> amountMap = new HashMap<>();
        for (Map<String, Object> row : accountSetMapper.sumBillGroupByAccountSet(period)) {
            Object asidObj = row.get("accountSetId");
            Long asid = asidObj == null ? defaultId : Long.valueOf(asidObj.toString());
            if (asid == null) {
                // 无默认账套且单据未归属账套：无法归集，跳过
                continue;
            }
            long cnt = row.get("billCount") == null ? 0L : Long.parseLong(row.get("billCount").toString());
            BigDecimal amt = row.get("totalAmount") == null
                ? BigDecimal.ZERO : new BigDecimal(row.get("totalAmount").toString());
            countMap.merge(asid, cnt, Long::sum);
            amountMap.merge(asid, amt, BigDecimal::add);
        }

        List<FeeAccountSetReportVo> result = new ArrayList<>();
        for (FeeAccountSetVo vo : accountSets) {
            FeeAccountSetReportVo r = new FeeAccountSetReportVo();
            r.setAccountSetId(vo.getId());
            r.setAccountSetCode(vo.getCode());
            r.setAccountSetName(vo.getName());
            r.setBillCount(countMap.getOrDefault(vo.getId(), 0L));
            r.setTotalAmount(amountMap.getOrDefault(vo.getId(), BigDecimal.ZERO));
            result.add(r);
        }
        return result;
    }

    /**
     * 清除除 keepId 外的所有默认账套标记（保证每租户至多一个默认）
     */
    private void clearOtherDefault(Long keepId) {
        FeeAccountSet reset = new FeeAccountSet();
        reset.setIsDefault(0);
        accountSetMapper.update(reset, Wrappers.<FeeAccountSet>lambdaQuery()
            .eq(FeeAccountSet::getIsDefault, DEFAULT_YES)
            .ne(FeeAccountSet::getId, keepId));
    }

    /**
     * 校验账套编码租户内唯一
     */
    private void checkCodeUnique(String code, Long excludeId) {
        if (StrUtil.isBlank(code)) {
            return;
        }
        long count = accountSetMapper.selectCount(Wrappers.<FeeAccountSet>lambdaQuery()
            .eq(FeeAccountSet::getCode, code)
            .ne(excludeId != null, FeeAccountSet::getId, excludeId));
        if (count > 0) {
            throw new ServiceException("账套编码已存在：" + code);
        }
    }

    private LambdaQueryWrapper<FeeAccountSet> buildQueryWrapper(FeeAccountSetBo bo) {
        LambdaQueryWrapper<FeeAccountSet> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getOrgId() != null, FeeAccountSet::getOrgId, bo.getOrgId());
        lqw.like(StrUtil.isNotBlank(bo.getName()), FeeAccountSet::getName, bo.getName());
        lqw.eq(StrUtil.isNotBlank(bo.getCode()), FeeAccountSet::getCode, bo.getCode());
        lqw.eq(StrUtil.isNotBlank(bo.getStatus()), FeeAccountSet::getStatus, bo.getStatus());
        lqw.orderByDesc(FeeAccountSet::getIsDefault).orderByAsc(FeeAccountSet::getId);
        return lqw;
    }

    /**
     * 填充所属组织名称（关联 fee_org）
     */
    private void fillOrgName(List<FeeAccountSetVo> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        List<Long> orgIds = list.stream().map(FeeAccountSetVo::getOrgId)
            .filter(Objects::nonNull).distinct().collect(Collectors.toList());
        if (orgIds.isEmpty()) {
            return;
        }
        Map<Long, String> nameMap = orgMapper.selectBatchIds(orgIds).stream()
            .collect(Collectors.toMap(FeeOrg::getId, FeeOrg::getOrgName, (a, b) -> a));
        list.forEach(vo -> vo.setOrgName(nameMap.get(vo.getOrgId())));
    }
}
