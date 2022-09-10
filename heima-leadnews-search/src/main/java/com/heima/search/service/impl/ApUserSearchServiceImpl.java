package com.heima.search.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.search.dtos.UserSearchDto;
import com.heima.model.search.pojos.ApUserSearch;
import com.heima.search.feign.BehaviorFeign;
import com.heima.search.mapper.ApUserSearchMapper;
import com.heima.search.service.ApUserSearchService;
import com.heima.utils.threadlocal.AppThreadLocalUtils;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Transactional
@Log4j2
public class ApUserSearchServiceImpl extends ServiceImpl<ApUserSearchMapper, ApUserSearch> implements ApUserSearchService {

    /**
     * 保存用户搜索记录
     *
     * @param entry
     * @param searchWords
     */
    @Override
    @Async("taskExecutor")
    public void insert(ApBehaviorEntry entry, String searchWords) {
        //1.检查参数
        if(entry == null){
            throw new RuntimeException("记录搜索历史失败");
        }

        //2.查询关键字
        ApUserSearch apUserSearch = getOne(Wrappers.<ApUserSearch>lambdaQuery().eq(ApUserSearch::getEntryId, entry.getId()).eq(ApUserSearch::getKeyword, searchWords));

        //3.如果存在 status=1  什么都不做   status=0 修改为1
        if(apUserSearch != null && apUserSearch.getStatus() == 1){
            log.info("当前关键字已存在");
            return;
        }else if(apUserSearch != null && apUserSearch.getStatus() == 0){
            apUserSearch.setStatus(1);
            updateById(apUserSearch);
            return;
        }

        //4.如果不存在，新增
        apUserSearch = new ApUserSearch();
        apUserSearch.setStatus(1);
        apUserSearch.setKeyword(searchWords);
        apUserSearch.setEntryId(entry.getId());
        apUserSearch.setCreatedTime(new Date());
        save(apUserSearch);
    }

    /**
     * 加载用户搜索历史记录
     * @param dto
     * @return
     */
    @Override
    public ResponseResult findUserSearch(UserSearchDto dto) {
        //1.检查参数
        if(dto== null ){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        if(dto.getPageSize() > 50 ){
            dto.setPageSize(50);
        }

        //2.获取行为实体
        ApBehaviorEntry apBehaviorEntry = getEntry(dto);
        if(apBehaviorEntry == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //3.分页查询
        IPage pageParam = new Page(0,dto.getPageSize());
        IPage page = page(pageParam, Wrappers.<ApUserSearch>lambdaQuery().eq(ApUserSearch::getEntryId, apBehaviorEntry.getId())
                .eq(ApUserSearch::getStatus, 1));

        //4.结果返回
        return ResponseResult.okResult(page.getRecords());
    }

    @Autowired
    private BehaviorFeign behaviorFeign;

    /**
     * 查询行为实体
     * @param dto
     * @return
     */
    public ApBehaviorEntry getEntry(UserSearchDto dto){
        return behaviorFeign.findByUserIdOrEquipmentId(AppThreadLocalUtils.getUser().getId(),dto.getEquipmentId());
    }

    /**
     * 删除搜索历史记录
     * @param dto
     * @return
     */
    @Override
    public ResponseResult delUserSearch(UserSearchDto dto) {
        //1.检查参数
        if(dto == null || dto.getId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.获取行为实体
        ApBehaviorEntry apBehaviorEntry = getEntry(dto);
        if(apBehaviorEntry == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //3.删除，即修改status为0
        update(Wrappers.<ApUserSearch>lambdaUpdate().eq(ApUserSearch::getEntryId,apBehaviorEntry.getId())
                .eq(ApUserSearch::getId,dto.getId()).set(ApUserSearch::getStatus,0));

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }
}
