package com.heima.apis.article;

import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;

public interface ApArticleControllerApi {

    /**
     * 保存app端相关的文章
     * @param dto
     * @return
     */
    public ResponseResult saveAppArticle(ArticleDto dto);
}
