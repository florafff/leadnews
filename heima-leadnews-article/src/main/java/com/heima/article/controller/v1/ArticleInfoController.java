package com.heima.article.controller.v1;

import com.heima.apis.article.ArticleInfoControllerApi;
import com.heima.article.service.ApArticleService;
import com.heima.model.article.dtos.ArticleInfoDto;
import com.heima.model.common.dtos.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/article")
public class ArticleInfoController implements ArticleInfoControllerApi {

    @Autowired
    private ApArticleService articleService;


    /**
     * 加载文章详情
     * @param dto
     * @return
     */
    @PostMapping("/load_article_info")
    @Override
    public ResponseResult loadArticleInfo(@RequestBody ArticleInfoDto dto) {
        return articleService.loadArticleInfo(dto);
    }


    /**
     * 加载文章的行为数据
     *
     * @param dto
     * @return
     */
    @PostMapping("/load_article_behavior")
    @Override
    public ResponseResult loadArticleBehavior(@RequestBody ArticleInfoDto dto) {
        return articleService.loadArticleBehavior(dto);
    }
}
