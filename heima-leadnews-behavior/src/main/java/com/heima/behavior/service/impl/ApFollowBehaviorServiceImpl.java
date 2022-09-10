package com.heima.behavior.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.behavior.mapper.ApFollowBehaviorMapper;
import com.heima.behavior.service.ApBehaviorEntryService;
import com.heima.behavior.service.ApFollowBehaviorService;
import com.heima.model.behavior.dtos.FollowBehaviorDto;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.behavior.pojos.ApFollowBehavior;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Transactional
public class ApFollowBehaviorServiceImpl  extends ServiceImpl<ApFollowBehaviorMapper, ApFollowBehavior> implements ApFollowBehaviorService {

    @Autowired
    private ApBehaviorEntryService apBehaviorEntryService;

    /**
     * 保存用户的关注行为
     * @param dto
     */
    @Override
    public void saveApFollowBehavior(FollowBehaviorDto dto) {

        //1.查询行为实体
        ApBehaviorEntry apBehaviorEntry = apBehaviorEntryService.findByUserIdOrEquipmentId(dto.getUserId(), null);
        if(apBehaviorEntry == null){
            throw new RuntimeException("行为实体没有找到，保存关注行为失败");
        }

        //2.保存关注行为
        ApFollowBehavior apFollowBehavior = new ApFollowBehavior();
        apFollowBehavior.setFollowId(dto.getFollowId());
        apFollowBehavior.setEntryId(apBehaviorEntry.getId());
        apFollowBehavior.setArticleId(dto.getArticleId());
        apFollowBehavior.setCreatedTime(new Date());
        save(apFollowBehavior);

    }
}
