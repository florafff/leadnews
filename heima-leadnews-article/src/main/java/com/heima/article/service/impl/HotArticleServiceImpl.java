package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.feign.AdminFeign;
import com.heima.article.mapper.ApArticleMapper;
import com.heima.article.service.HotArticleService;
import com.heima.common.constants.ArticleConstants;
import com.heima.model.admin.pojos.AdChannel;
import com.heima.model.article.pojos.ApArticle;
import com.heima.model.article.vo.HotArticleVo;
import com.heima.model.common.dtos.ResponseResult;
import org.joda.time.DateTime;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class HotArticleServiceImpl implements HotArticleService {

    @Autowired
    private ApArticleMapper apArticleMapper;

    /**
     * 定时 计算热点文章数据
     */
    @Override
    public void computeHotArticle() {
        //1.查询前5天的文章数据
        String dayParam = DateTime.now().minusDays(5).toString("yyyy-MM-dd 00:00:00");
        List<ApArticle> apArticles = apArticleMapper.selectList(Wrappers.<ApArticle>lambdaQuery().gt(ApArticle::getPublishTime, dayParam));

        //2.计算文章分值
        List<HotArticleVo> hotArticleVoList = computeHotArticle(apArticles);

        //3.为每一个频道缓存热点较高的30条文章
        cacheTagToRedis(hotArticleVoList);

    }

    @Autowired
    private AdminFeign adminFeign;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 给每一个频道缓存分值较高的30条数据
     * @param hotArticleVoList
     */
    private void cacheTagToRedis(List<HotArticleVo> hotArticleVoList) {
        //1.查询所有的频道
        ResponseResult responseResult = adminFeign.findAll();
        List<AdChannel> channels = JSON.parseArray(JSON.toJSONString(responseResult.getData()), AdChannel.class);

        //2.检索出频道对应的文章列表
        if(hotArticleVoList!= null && !hotArticleVoList.isEmpty()){
            for (AdChannel channel : channels) {
                List<HotArticleVo> hotArticleVos = hotArticleVoList.stream().filter(x -> x.getChannelId().equals(channel.getId())).collect(Collectors.toList());
                //3.给每个频道进行缓存
                sortAndCache(hotArticleVos, ArticleConstants.HOT_ARTICLE_FIRST_PAGE + channel.getId());

            }
        }

        //4.给推荐频道缓存30条数据  所有文章排序之后的前30条
        sortAndCache(hotArticleVoList, ArticleConstants.HOT_ARTICLE_FIRST_PAGE+ArticleConstants.DEFAULT_TAG);

    }

    /**
     * 排序并缓存
     * @param hotArticleVos
     * @param s
     */
    private void sortAndCache(List<HotArticleVo> hotArticleVos, String s) {
        hotArticleVos.sort(new Comparator<HotArticleVo>() {
            @Override
            public int compare(HotArticleVo o1, HotArticleVo o2) {
                return o2.getScore().compareTo(o1.getScore());
            }
        });
        if (hotArticleVos.size() > 30) {
            hotArticleVos = hotArticleVos.subList(0, 30);
        }
        redisTemplate.opsForValue().set(s, JSON.toJSONString(hotArticleVos));
    }

    /**
     * 计算文章的分值
     * @param apArticles
     * @return
     */
    private List<HotArticleVo> computeHotArticle(List<ApArticle> apArticles) {

        List<HotArticleVo> resultList = new ArrayList<>();

        HotArticleVo hotArticleVo = null;
        if(apArticles!= null && !apArticles.isEmpty()){
            for (ApArticle apArticle : apArticles) {
                hotArticleVo = new HotArticleVo();
                BeanUtils.copyProperties(apArticle,hotArticleVo);
                Integer score = computeScore(apArticle);
                hotArticleVo.setScore(score);
                resultList.add(hotArticleVo);
            }
        }
        return resultList;
    }

    /**
     * 计算某一个文章的分值
     * @param apArticle
     * @return
     */
    private Integer computeScore(ApArticle apArticle) {
        Integer score = 0;
        if(apArticle.getLikes()!=null){
            score+=apArticle.getLikes()* ArticleConstants.HOT_ARTICLE_LIKE_WEIGHT;
        }
        if(apArticle.getViews()!=null){
            score+=apArticle.getViews();
        }
        if(apArticle.getComment()!=null){
            score+=apArticle.getComment()* ArticleConstants.HOT_ARTICLE_COMMENT_WEIGHT;
        }
        if(apArticle.getCollection()!=null){
            score+=apArticle.getCollection()* ArticleConstants.HOT_ARTICLE_COLLECTION_WEIGHT;
        }
        return score;
    }
}
