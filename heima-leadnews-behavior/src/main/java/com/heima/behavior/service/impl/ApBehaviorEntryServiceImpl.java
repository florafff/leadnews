package com.heima.behavior.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.behavior.mapper.ApBehaviorEntryMapper;
import com.heima.behavior.service.ApBehaviorEntryService;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ApBehaviorEntryServiceImpl extends ServiceImpl<ApBehaviorEntryMapper, ApBehaviorEntry> implements ApBehaviorEntryService {
    /**
     * 根据用户id或设备id查询行为实体
     * @param userId
     * @param equipmentId
     * @return
     */
    @Override
    public ApBehaviorEntry findByUserIdOrEquipmentId(Integer userId, Integer equipmentId) {

        //根据用户查询行为实体
        if(userId != null){
            return getOne(Wrappers.<ApBehaviorEntry>lambdaQuery().eq(ApBehaviorEntry::getEntryId,userId).eq(ApBehaviorEntry::getType,1));
        }

        //根据设备id查询行为实体
        if(equipmentId != null){
            return getOne(Wrappers.<ApBehaviorEntry>lambdaQuery().eq(ApBehaviorEntry::getEntryId,equipmentId).eq(ApBehaviorEntry::getType,0));
        }
        return null;
    }
}
