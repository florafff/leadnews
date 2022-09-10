package com.heima.apis.user;

import com.heima.model.user.pojos.ApUserFollow;

public interface ApUserFollowControllerApi {

    /**
     * 查询关注信息
     * @param userId
     * @param followId
     * @return
     */
    public ApUserFollow findByUserIdAndFollowId(Integer userId,Integer followId);
}
