package com.heima.apis.user;

import com.heima.model.user.pojos.ApUser;

public interface ApUserControllerApi {

    /**
     * 根据id查询用户
     * @param id
     * @return
     */
    public ApUser findById(Integer id);
}
