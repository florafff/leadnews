package com.heima.apis.search;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.UserSearchDto;

import java.io.IOException;

public interface ArticleSearchControllerApi {

    /**
     * 文章搜索
     * @param dto
     * @return
     */
    public ResponseResult search(UserSearchDto dto) throws IOException;
}
