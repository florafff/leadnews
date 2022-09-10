package com.heima.search.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.UserSearchDto;
import com.heima.model.search.pojos.ApAssociateWords;

public interface ApAssociateWordsService extends IService<ApAssociateWords> {

    /**
     * 查询关键字联想词
     * @param dto
     * @return
     */
    public ResponseResult search(UserSearchDto dto);

    /**
     * 查询关键字联想词
     * @param dto
     * @return
     */
    public ResponseResult searchV2(UserSearchDto dto);
}
