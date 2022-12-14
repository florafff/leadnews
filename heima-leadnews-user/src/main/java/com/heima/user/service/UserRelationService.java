package com.heima.user.service;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.dtos.UserRelationDto;

public interface UserRelationService  {

    /**
     * 关注或取消关注
     * @param dto
     * @return
     */
    public ResponseResult follow(UserRelationDto dto);
}
