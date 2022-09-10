package com.heima.behavior.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.behavior.dtos.FollowBehaviorDto;
import com.heima.model.behavior.pojos.ApFollowBehavior;

public interface ApFollowBehaviorService extends IService<ApFollowBehavior> {

    /**
     * 保存用户的关注行为
     * @param dto
     */
    public void saveApFollowBehavior(FollowBehaviorDto dto);
}
