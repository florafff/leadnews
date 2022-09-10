package com.heima.apis.search;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.UserSearchDto;

public interface ApAssociateWordsControllerApi {

    /**
     * 查询关键字联想词
     * @param dto
     * @return
     */
    public ResponseResult search(UserSearchDto dto);
}
