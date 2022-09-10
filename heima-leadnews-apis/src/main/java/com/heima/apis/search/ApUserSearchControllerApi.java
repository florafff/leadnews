package com.heima.apis.search;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.UserSearchDto;

public interface ApUserSearchControllerApi {

    /**
     * 加载用户搜索历史记录
     * @param dto
     * @return
     */
    public ResponseResult findUserSearch(UserSearchDto dto);

    /**
     * 删除搜索历史记录
     * @param dto
     * @return
     */
    public ResponseResult delUserSearch(UserSearchDto dto);
}
