package com.heima.search.service.impl;

import com.alibaba.fastjson.JSON;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.search.dtos.UserSearchDto;
import com.heima.search.feign.BehaviorFeign;
import com.heima.search.service.ApUserSearchService;
import com.heima.search.service.ArticleSearchService;
import com.heima.utils.threadlocal.AppThreadLocalUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ArticleSearchServiceImpl implements ArticleSearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Autowired
    private ApUserSearchService apUserSearchService;

    /**
     * 文章搜索
     * @param dto
     * @return
     */
    @Override
    public ResponseResult search(UserSearchDto dto) throws IOException {

        //1.检查参数
        if(dto == null || StringUtils.isBlank(dto.getSearchWords())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //记录搜索关键字  异步请求   只有首页查询才会记录
        if(dto.getFromIndex() == 0){
            //行为实体
            //关键字
            apUserSearchService.insert(getEntry(dto),dto.getSearchWords());
        }

        //2.从es索引库中查询数据

        //创建搜索请求对象，指定索引库名称
        SearchRequest searchRequest = new SearchRequest("app_info_article");
        //创建搜索的构建条件对象
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();

        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();

        //根据关键字分词查询
        QueryStringQueryBuilder queryStringQueryBuilder = QueryBuilders.queryStringQuery(dto.getSearchWords()).field("title").defaultOperator(Operator.OR);
        boolQueryBuilder.must(queryStringQueryBuilder);
        //按照时间进行范围查询 发布时间小于mindate
        RangeQueryBuilder rangeQueryBuilder = QueryBuilders.rangeQuery("publishTime").lt(dto.getMinBehotTime());
        boolQueryBuilder.filter(rangeQueryBuilder);
        //按照发布时间进行倒序
        searchSourceBuilder.sort("publishTime", SortOrder.DESC);

        //分页的设置
        searchSourceBuilder.from(0);
        searchSourceBuilder.size(dto.getPageSize());

        searchSourceBuilder.query(boolQueryBuilder);
        searchRequest.source(searchSourceBuilder);
        SearchResponse searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);


        List<Map> articleList  = new ArrayList<>();

        //3.结果封装返回
        SearchHit[] hits = searchResponse.getHits().getHits();
        for (SearchHit hit : hits) {
            String sourceAsString = hit.getSourceAsString();
            Map map = JSON.parseObject(sourceAsString, Map.class);
            articleList.add(map);
        }

        return ResponseResult.okResult(articleList);
    }

    @Autowired
    private BehaviorFeign behaviorFeign;

    /**
     * 查询行为实体
     * @param dto
     * @return
     */
    public ApBehaviorEntry getEntry(UserSearchDto dto){
        return behaviorFeign.findByUserIdOrEquipmentId(AppThreadLocalUtils.getUser().getId(),dto.getEquipmentId());
    }
}
