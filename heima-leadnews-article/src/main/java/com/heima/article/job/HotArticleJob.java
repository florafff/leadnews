package com.heima.article.job;

import com.heima.article.service.HotArticleService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class HotArticleJob {

    @Autowired
    private HotArticleService hotArticleService;

    @XxlJob("computeHotArticleJob")
    public ReturnT handler(String param){
        log.info("定时计算文章任务开始");
        hotArticleService.computeHotArticle();
        log.info("定时计算文章任务结束");
        return ReturnT.SUCCESS;
    }
}
