package com.heima.article.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.article.feign.BehaviorFeign;
import com.heima.article.feign.UserFeign;
import com.heima.article.mapper.*;
import com.heima.article.service.ApArticleService;
import com.heima.common.constants.ArticleConstants;
import com.heima.model.article.dtos.ArticleDto;
import com.heima.model.article.dtos.ArticleHomeDto;
import com.heima.model.article.dtos.ArticleInfoDto;
import com.heima.model.article.pojos.*;
import com.heima.model.article.vo.HotArticleVo;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.behavior.pojos.ApLikesBehavior;
import com.heima.model.behavior.pojos.ApUnlikesBehavior;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mess.ArticleVisitStreamMess;
import com.heima.model.user.pojos.ApUserFollow;
import com.heima.utils.threadlocal.AppThreadLocalUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.*;


@Service
@Transactional
public class ApArticleServiceImpl extends ServiceImpl<ApArticleMapper, ApArticle> implements ApArticleService {


    @Autowired
    private AuthorMapper authorMapper;

    @Autowired
    private ApArticleConfigMapper apArticleConfigMapper;

    @Autowired
    private ApArticleContentMapper apArticleContentMapper;

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    /**
     * 保存app端相关的文章
     * @param dto
     * @return
     */
    @Override
    public ResponseResult saveAppArticle(ArticleDto dto) {
//        int a = 1/0;

        //1.检查参数
        if(dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.判断是否存在文章id
        ApArticle apArticle = new ApArticle();
        BeanUtils.copyProperties(dto,apArticle);

        //2.1 没有id,保存（ap_article,ap_article_config,ap_article_content）
        if(dto.getId() == null){

            //补全作者id
            ApAuthor apAuthor = authorMapper.selectOne(Wrappers.<ApAuthor>lambdaQuery().eq(ApAuthor::getName, dto.getAuthorName()));
            if(apAuthor != null){
                apArticle.setAuthorId(apAuthor.getId().longValue());
            }
            //保存文章
            save(apArticle);

            //保存文章的配置
            ApArticleConfig apArticleConfig = new ApArticleConfig();
            apArticleConfig.setArticleId(apArticle.getId());
            apArticleConfig.setIsDelete(false);
            apArticleConfig.setIsDown(false);
            apArticleConfig.setIsComment(true);
            apArticleConfig.setIsForward(true);
            apArticleConfigMapper.insert(apArticleConfig);

            //保存文章的内容
            ApArticleContent apArticleContent = new ApArticleContent();
            apArticleContent.setArticleId(apArticle.getId());
            apArticleContent.setContent(dto.getContent());
            apArticleContentMapper.insert(apArticleContent);



        }else{
            //2.2 有id  修改 （ap_article,ap_article_content）
            ApArticle dbArticle = getById(dto.getId());
            if(dbArticle == null){
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"文章没有找到");
            }
            //修改文章
            updateById(apArticle);
            //修改文章内容
            ApArticleContent apArticleContent = apArticleContentMapper.selectOne(Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId, dto.getId()));
            apArticleContent.setContent(dto.getContent());
            apArticleContentMapper.updateById(apArticleContent);
        }

        //导入数据到es索引库
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("id", apArticle.getId().toString());
        map.put("publishTime", apArticle.getPublishTime());
        map.put("layout", apArticle.getLayout());
        map.put("images", apArticle.getImages());
        map.put("authorId", apArticle.getAuthorId());
        map.put("title", apArticle.getTitle());
        IndexRequest indexRequest = new IndexRequest("app_info_article").id(apArticle.getId().toString()).source(map);
        try {
            restHighLevelClient.index(indexRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }


        //返回的时候，必须携带文章id
        return ResponseResult.okResult(apArticle.getId());
    }

    private final static Short MAX_PAGE_SIZE = 50;

    @Autowired
    private ApArticleMapper apArticleMapper;

    @Value("${fdfs.url}")
    private String fileServerUrl;

    /**
     * 加载文章列表
     * @param type 1 加载更多  2 加载最新
     * @param dto
     * @return
     */
    @Override
    public ResponseResult load(Short type, ArticleHomeDto dto) {
        //1.参数检查
        Integer size = dto.getSize();
        if(size == null || size == 0){
            size = 10;
        }
        //每天显示条数
        size = Math.min(size,MAX_PAGE_SIZE);
        dto.setSize(size);
        //检查type
        if(!type.equals(ArticleConstants.LOADTYPE_LOAD_MORE)&&!type.equals(ArticleConstants.LOADTYPE_LOAD_NEW)){
            type = ArticleConstants.LOADTYPE_LOAD_MORE;
        }
        //频道判断
        if(StringUtils.isBlank(dto.getTag())){
            dto.setTag(ArticleConstants.DEFAULT_TAG);
        }
        //时间控制
        if(dto.getMaxBehotTime()==null) dto.setMaxBehotTime(new Date());
        if(dto.getMinBehotTime()==null) dto.setMinBehotTime(new Date());

        //2.查询数据
        List<ApArticle> articleList = apArticleMapper.loadArticleList(type, dto);

        //3.结果封装
        ResponseResult responseResult = ResponseResult.okResult(articleList);
        responseResult.setHost(fileServerUrl);
        return responseResult;
    }

    /**
     * 优先从缓存中获取数据
     * @param type
     * @param dto
     * @param isFirstPage
     * @return
     */
    @Override
    public ResponseResult loadV2(Short type, ArticleHomeDto dto, boolean isFirstPage) {
        if(isFirstPage){
            String articleListStr = redisTemplate.opsForValue().get(ArticleConstants.HOT_ARTICLE_FIRST_PAGE + dto.getTag());
            if(StringUtils.isNotBlank(articleListStr)){
                List<HotArticleVo> hotArticleVoList = JSON.parseArray(articleListStr, HotArticleVo.class);
                ResponseResult responseResult = ResponseResult.okResult(hotArticleVoList);
                responseResult.setHost(fileServerUrl);
                return responseResult;
            }
        }

        return load(type,dto);
    }

    /**
     * 加载文章详情
     * @param dto
     * @return
     */
    @Override
    public ResponseResult loadArticleInfo(ArticleInfoDto dto) {

        //结果返回的map
        Map<String,Object> resultMap = new HashMap<>();

        //1.检查参数
        if(dto == null || dto.getArticleId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.查询文章的配置
        ApArticleConfig apArticleConfig = apArticleConfigMapper.selectOne(Wrappers.<ApArticleConfig>lambdaQuery().eq(ApArticleConfig::getArticleId, dto.getArticleId()));
        if(apArticleConfig == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        if(!apArticleConfig.getIsDelete() && !apArticleConfig.getIsDown()){
            //3.查询文章的内容
            ApArticleContent apArticleContent = apArticleContentMapper.selectOne(Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId, dto.getArticleId()));
            resultMap.put("content",apArticleContent);

        }
        //封装config
        resultMap.put("config",apArticleConfig);

        //4.结果封装返回
        return ResponseResult.okResult(resultMap);
    }

    @Autowired
    private BehaviorFeign behaviorFeign;

    @Autowired
    private UserFeign userFeign;

    @Autowired
    private ApCollectionMapper apCollectionMapper;

    /**
     * 加载文章的行为数据
     * @param dto
     * @return
     */
    @Override
    public ResponseResult loadArticleBehavior(ArticleInfoDto dto) {
        //1.检查参数
        if(dto == null || dto.getArticleId() == null || dto.getAuthorId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.查询行为实体
        ApBehaviorEntry apBehaviorEntry = behaviorFeign.findByUserIdOrEquipmentId(AppThreadLocalUtils.getUser().getId(), dto.getEquipmentId());
        if(apBehaviorEntry == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        boolean islike = false,isunlike=false,iscollection=false,isfollow=false;

        //3.查询点赞对象
        ApLikesBehavior apLikesBehavior = behaviorFeign.findLikeByArticleIdAndEntryId(dto.getArticleId(), apBehaviorEntry.getId(), ApLikesBehavior.Type.ARTICLE.getCode());
        if(apLikesBehavior != null && apLikesBehavior.getOperation().equals(ApLikesBehavior.Operation.LIKE.getCode())){
            islike = true;
        }

        //4.查询不喜欢对象
        ApUnlikesBehavior apUnlikesBehavior = behaviorFeign.findUnLikesByArticleIdAndEntryId(dto.getArticleId(), apBehaviorEntry.getId());
        if(apUnlikesBehavior!=null&& apUnlikesBehavior.getType().equals(ApUnlikesBehavior.Type.UNLIKE.getCode())){
            isunlike=true;
        }

        //5.查询收藏
        ApCollection apCollection = apCollectionMapper.selectOne(Wrappers.<ApCollection>lambdaQuery().eq(ApCollection::getArticleId, dto.getArticleId())
                .eq(ApCollection::getEntryId, apBehaviorEntry.getId()).eq(ApCollection::getType, ApCollection.Type.ARTICLE.getCode()));
        if(apCollection != null){
            iscollection=true;
        }

        //6.查询是否关注
        ApAuthor apAuthor = authorMapper.selectById(dto.getAuthorId());
        if(apAuthor != null ){
            ApUserFollow apUserFollow = userFeign.findByUserIdAndFollowId(AppThreadLocalUtils.getUser().getId(), apAuthor.getUserId());
            if(apUserFollow != null){
                isfollow=true;
            }
        }


        //7.结果封装
        //{"isfollow":true,"islike":true,"isunlike":false,"iscollection":true}
        Map<String,Object> resultMap = new HashMap<>();
        resultMap.put("isfollow",isfollow);
        resultMap.put("islike",islike);
        resultMap.put("isunlike",isunlike);
        resultMap.put("iscollection",iscollection);
        return ResponseResult.okResult(resultMap);
    }

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 重新计算文章分值
     * @param mess
     */
    @Override
    public void updateApArticle(ArticleVisitStreamMess mess) {

        //1.查询文章
        ApArticle apArticle = getById(mess.getArticleId());

        //2.修改文章
        apArticle.setViews((int)(apArticle.getViews()==null ? 0 : apArticle.getViews() +mess.getView()));
        apArticle.setCollection((int)(apArticle.getCollection()==null ? 0 : apArticle.getCollection() +mess.getCollect()));
        apArticle.setComment((int)(apArticle.getComment()==null ? 0 : apArticle.getComment() +mess.getComment()));
        apArticle.setLikes((int)(apArticle.getLikes()==null ? 0 : apArticle.getLikes() +mess.getLike()));
        updateById(apArticle);

        //3.计算文章的分值
        Integer score = computeScore(apArticle);
        score = score*3;

        //4.查询redis缓存
        String articleListStr = redisTemplate.opsForValue().get(ArticleConstants.HOT_ARTICLE_FIRST_PAGE + apArticle.getChannelId());
        cacheAndToRedis(apArticle, score, articleListStr, articleListStr, ArticleConstants.HOT_ARTICLE_FIRST_PAGE + apArticle.getChannelId());


        //5.更新推荐缓存中的数据
        String articleListAllStr = redisTemplate.opsForValue().get(ArticleConstants.HOT_ARTICLE_FIRST_PAGE + ArticleConstants.DEFAULT_TAG);
        cacheAndToRedis(apArticle, score, articleListStr, articleListAllStr, ArticleConstants.HOT_ARTICLE_FIRST_PAGE + ArticleConstants.DEFAULT_TAG);


    }

    /**
     * 重新计算文章分析，并替换
     * @param apArticle
     * @param score
     * @param articleListStr
     * @param articleListAllStr
     * @param s
     */
    private void cacheAndToRedis(ApArticle apArticle, Integer score, String articleListStr, String articleListAllStr, String s) {
        if (StringUtils.isNotBlank(articleListAllStr)) {
            List<HotArticleVo> hotArticleVoList = JSON.parseArray(articleListStr, HotArticleVo.class);
            boolean flag = true;
            //5.如果当前缓存中有当前文章，更新分值
            for (HotArticleVo hotArticleVo : hotArticleVoList) {
                if (hotArticleVo.getId().equals(apArticle.getId())) {
                    hotArticleVo.setScore(score);
                    flag = false;
                    break;
                }
            }

            //6.缓存中没有当前文章，获取缓存中的最小分值的文章作对比，-->替换
            if (flag && hotArticleVoList.size() >= 30) {
                hotArticleVoList.sort(new Comparator<HotArticleVo>() {
                    @Override
                    public int compare(HotArticleVo o1, HotArticleVo o2) {
                        return o2.getScore().compareTo(o1.getScore());
                    }
                });
                //找到分值最小的文章
                HotArticleVo hotArticleVo = hotArticleVoList.get(hotArticleVoList.size() - 1);
                if (hotArticleVo.getScore() < score) {
                    hotArticleVoList.remove(hotArticleVo);
                    HotArticleVo hot = new HotArticleVo();
                    BeanUtils.copyProperties(apArticle, hot);
                    hot.setScore(score);
                    hotArticleVoList.add(hot);
                }
            } else if(flag) {
                HotArticleVo hot = new HotArticleVo();
                BeanUtils.copyProperties(apArticle, hot);
                hot.setScore(score);
                hotArticleVoList.add(hot);
            }
            //更新缓存中的数据
            redisTemplate.opsForValue().set(s, JSON.toJSONString(hotArticleVoList));
        }
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