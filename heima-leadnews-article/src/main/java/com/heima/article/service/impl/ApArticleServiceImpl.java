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
     * ??????app??????????????????
     * @param dto
     * @return
     */
    @Override
    public ResponseResult saveAppArticle(ArticleDto dto) {
//        int a = 1/0;

        //1.????????????
        if(dto == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.????????????????????????id
        ApArticle apArticle = new ApArticle();
        BeanUtils.copyProperties(dto,apArticle);

        //2.1 ??????id,?????????ap_article,ap_article_config,ap_article_content???
        if(dto.getId() == null){

            //????????????id
            ApAuthor apAuthor = authorMapper.selectOne(Wrappers.<ApAuthor>lambdaQuery().eq(ApAuthor::getName, dto.getAuthorName()));
            if(apAuthor != null){
                apArticle.setAuthorId(apAuthor.getId().longValue());
            }
            //????????????
            save(apArticle);

            //?????????????????????
            ApArticleConfig apArticleConfig = new ApArticleConfig();
            apArticleConfig.setArticleId(apArticle.getId());
            apArticleConfig.setIsDelete(false);
            apArticleConfig.setIsDown(false);
            apArticleConfig.setIsComment(true);
            apArticleConfig.setIsForward(true);
            apArticleConfigMapper.insert(apArticleConfig);

            //?????????????????????
            ApArticleContent apArticleContent = new ApArticleContent();
            apArticleContent.setArticleId(apArticle.getId());
            apArticleContent.setContent(dto.getContent());
            apArticleContentMapper.insert(apArticleContent);



        }else{
            //2.2 ???id  ?????? ???ap_article,ap_article_content???
            ApArticle dbArticle = getById(dto.getId());
            if(dbArticle == null){
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"??????????????????");
            }
            //????????????
            updateById(apArticle);
            //??????????????????
            ApArticleContent apArticleContent = apArticleContentMapper.selectOne(Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId, dto.getId()));
            apArticleContent.setContent(dto.getContent());
            apArticleContentMapper.updateById(apArticleContent);
        }

        //???????????????es?????????
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


        //????????????????????????????????????id
        return ResponseResult.okResult(apArticle.getId());
    }

    private final static Short MAX_PAGE_SIZE = 50;

    @Autowired
    private ApArticleMapper apArticleMapper;

    @Value("${fdfs.url}")
    private String fileServerUrl;

    /**
     * ??????????????????
     * @param type 1 ????????????  2 ????????????
     * @param dto
     * @return
     */
    @Override
    public ResponseResult load(Short type, ArticleHomeDto dto) {
        //1.????????????
        Integer size = dto.getSize();
        if(size == null || size == 0){
            size = 10;
        }
        //??????????????????
        size = Math.min(size,MAX_PAGE_SIZE);
        dto.setSize(size);
        //??????type
        if(!type.equals(ArticleConstants.LOADTYPE_LOAD_MORE)&&!type.equals(ArticleConstants.LOADTYPE_LOAD_NEW)){
            type = ArticleConstants.LOADTYPE_LOAD_MORE;
        }
        //????????????
        if(StringUtils.isBlank(dto.getTag())){
            dto.setTag(ArticleConstants.DEFAULT_TAG);
        }
        //????????????
        if(dto.getMaxBehotTime()==null) dto.setMaxBehotTime(new Date());
        if(dto.getMinBehotTime()==null) dto.setMinBehotTime(new Date());

        //2.????????????
        List<ApArticle> articleList = apArticleMapper.loadArticleList(type, dto);

        //3.????????????
        ResponseResult responseResult = ResponseResult.okResult(articleList);
        responseResult.setHost(fileServerUrl);
        return responseResult;
    }

    /**
     * ??????????????????????????????
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
     * ??????????????????
     * @param dto
     * @return
     */
    @Override
    public ResponseResult loadArticleInfo(ArticleInfoDto dto) {

        //???????????????map
        Map<String,Object> resultMap = new HashMap<>();

        //1.????????????
        if(dto == null || dto.getArticleId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.?????????????????????
        ApArticleConfig apArticleConfig = apArticleConfigMapper.selectOne(Wrappers.<ApArticleConfig>lambdaQuery().eq(ApArticleConfig::getArticleId, dto.getArticleId()));
        if(apArticleConfig == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        if(!apArticleConfig.getIsDelete() && !apArticleConfig.getIsDown()){
            //3.?????????????????????
            ApArticleContent apArticleContent = apArticleContentMapper.selectOne(Wrappers.<ApArticleContent>lambdaQuery().eq(ApArticleContent::getArticleId, dto.getArticleId()));
            resultMap.put("content",apArticleContent);

        }
        //??????config
        resultMap.put("config",apArticleConfig);

        //4.??????????????????
        return ResponseResult.okResult(resultMap);
    }

    @Autowired
    private BehaviorFeign behaviorFeign;

    @Autowired
    private UserFeign userFeign;

    @Autowired
    private ApCollectionMapper apCollectionMapper;

    /**
     * ???????????????????????????
     * @param dto
     * @return
     */
    @Override
    public ResponseResult loadArticleBehavior(ArticleInfoDto dto) {
        //1.????????????
        if(dto == null || dto.getArticleId() == null || dto.getAuthorId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.??????????????????
        ApBehaviorEntry apBehaviorEntry = behaviorFeign.findByUserIdOrEquipmentId(AppThreadLocalUtils.getUser().getId(), dto.getEquipmentId());
        if(apBehaviorEntry == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        boolean islike = false,isunlike=false,iscollection=false,isfollow=false;

        //3.??????????????????
        ApLikesBehavior apLikesBehavior = behaviorFeign.findLikeByArticleIdAndEntryId(dto.getArticleId(), apBehaviorEntry.getId(), ApLikesBehavior.Type.ARTICLE.getCode());
        if(apLikesBehavior != null && apLikesBehavior.getOperation().equals(ApLikesBehavior.Operation.LIKE.getCode())){
            islike = true;
        }

        //4.?????????????????????
        ApUnlikesBehavior apUnlikesBehavior = behaviorFeign.findUnLikesByArticleIdAndEntryId(dto.getArticleId(), apBehaviorEntry.getId());
        if(apUnlikesBehavior!=null&& apUnlikesBehavior.getType().equals(ApUnlikesBehavior.Type.UNLIKE.getCode())){
            isunlike=true;
        }

        //5.????????????
        ApCollection apCollection = apCollectionMapper.selectOne(Wrappers.<ApCollection>lambdaQuery().eq(ApCollection::getArticleId, dto.getArticleId())
                .eq(ApCollection::getEntryId, apBehaviorEntry.getId()).eq(ApCollection::getType, ApCollection.Type.ARTICLE.getCode()));
        if(apCollection != null){
            iscollection=true;
        }

        //6.??????????????????
        ApAuthor apAuthor = authorMapper.selectById(dto.getAuthorId());
        if(apAuthor != null ){
            ApUserFollow apUserFollow = userFeign.findByUserIdAndFollowId(AppThreadLocalUtils.getUser().getId(), apAuthor.getUserId());
            if(apUserFollow != null){
                isfollow=true;
            }
        }


        //7.????????????
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
     * ????????????????????????
     * @param mess
     */
    @Override
    public void updateApArticle(ArticleVisitStreamMess mess) {

        //1.????????????
        ApArticle apArticle = getById(mess.getArticleId());

        //2.????????????
        apArticle.setViews((int)(apArticle.getViews()==null ? 0 : apArticle.getViews() +mess.getView()));
        apArticle.setCollection((int)(apArticle.getCollection()==null ? 0 : apArticle.getCollection() +mess.getCollect()));
        apArticle.setComment((int)(apArticle.getComment()==null ? 0 : apArticle.getComment() +mess.getComment()));
        apArticle.setLikes((int)(apArticle.getLikes()==null ? 0 : apArticle.getLikes() +mess.getLike()));
        updateById(apArticle);

        //3.?????????????????????
        Integer score = computeScore(apArticle);
        score = score*3;

        //4.??????redis??????
        String articleListStr = redisTemplate.opsForValue().get(ArticleConstants.HOT_ARTICLE_FIRST_PAGE + apArticle.getChannelId());
        cacheAndToRedis(apArticle, score, articleListStr, articleListStr, ArticleConstants.HOT_ARTICLE_FIRST_PAGE + apArticle.getChannelId());


        //5.??????????????????????????????
        String articleListAllStr = redisTemplate.opsForValue().get(ArticleConstants.HOT_ARTICLE_FIRST_PAGE + ArticleConstants.DEFAULT_TAG);
        cacheAndToRedis(apArticle, score, articleListStr, articleListAllStr, ArticleConstants.HOT_ARTICLE_FIRST_PAGE + ArticleConstants.DEFAULT_TAG);


    }

    /**
     * ????????????????????????????????????
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
            //5.???????????????????????????????????????????????????
            for (HotArticleVo hotArticleVo : hotArticleVoList) {
                if (hotArticleVo.getId().equals(apArticle.getId())) {
                    hotArticleVo.setScore(score);
                    flag = false;
                    break;
                }
            }

            //6.?????????????????????????????????????????????????????????????????????????????????-->??????
            if (flag && hotArticleVoList.size() >= 30) {
                hotArticleVoList.sort(new Comparator<HotArticleVo>() {
                    @Override
                    public int compare(HotArticleVo o1, HotArticleVo o2) {
                        return o2.getScore().compareTo(o1.getScore());
                    }
                });
                //???????????????????????????
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
            //????????????????????????
            redisTemplate.opsForValue().set(s, JSON.toJSONString(hotArticleVoList));
        }
    }

    /**
     * ??????????????????????????????
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