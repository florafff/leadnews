package com.heima.article.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.dtos.ArticleInfoDto;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.mess.ArticleVisitStreamMess;

public interface ApArticleService extends IService<ApArticle> {

    /**
     * 保存app端相关的文章
     * @param dto
     * @return
     */
    public ResponseResult saveAppArticle(ArticleDto dto);

    /**
     * 加载文章列表
     * @param type  1 加载更多  2 加载最新
     * @param dto
     * @return
     */
    public ResponseResult load(Short type ,ArticleHomeDto dto);

    /**
     * 优先从缓存中获取数据
     * @param type
     * @param dto
     * @param isFirstPage
     * @return
     */
    public ResponseResult loadV2(Short type ,ArticleHomeDto dto,boolean isFirstPage);

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

    /**
     * 重新计算文章分值
     * @param mess
     */
    public void updateApArticle(ArticleVisitStreamMess mess);
}