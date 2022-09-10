package com.heima.apis.article;

import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.common.dtos.ResponseResult;

public interface ArticleHomeControllerApi {

    /**
     * 加载文章首页
     * @param dto
     * @return
     */
    public ResponseResult load(ArticleHomeDto dto);

    /**
     * 加载更多
     * @param dto
     * @return
     */
    public ResponseResult loadMore(ArticleHomeDto dto);

    /**
     * 加载最新
     * @param dto
     * @return
     */
    public ResponseResult loadNew(ArticleHomeDto dto);
}
