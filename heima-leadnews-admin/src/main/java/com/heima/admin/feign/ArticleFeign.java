package com.heima.admin.feign;

import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.common.dtos.ResponseResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient("leadnews-article")
public interface ArticleFeign {


    @PostMapping("/api/v1/article/save")
    public ResponseResult saveAppArticle(ArticleDto dto);
}
