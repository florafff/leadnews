package com.heima.apis.wemedia;

import com.baomidou.mybatisplus.extension.api.R;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.pojos.WmUser;

public interface WmUserControllerApi {

    /**
     * 根据名称查询自媒体人
     * @param name
     * @return
     */
    public WmUser findByName(String name);

    /**
     * 保存自媒体人
     * @param wmUser
     * @return
     */
    public ResponseResult save(WmUser wmUser);

    /**
     * 查询自媒体用户
     * @param id
     * @return
     */
    public WmUser findUserById(Integer id);
}
