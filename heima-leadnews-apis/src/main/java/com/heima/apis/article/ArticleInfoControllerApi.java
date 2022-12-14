package com.heima.apis.article;

import com.heima.model.article.dtos.ArticleInfoDto;
import com.heima.model.common.dtos.ResponseResult;

public interface ArticleInfoControllerApi {

    /**
     * 加载文章详情
     * @param dto
     * @return
     */
    public ResponseResult loadArticleInfo(ArticleInfoDto dto);

    /**
     * 加载文章的行为数据
     * @param dto
     * @return
     */
    public ResponseResult loadArticleBehavior(ArticleInfoDto dto);
}
