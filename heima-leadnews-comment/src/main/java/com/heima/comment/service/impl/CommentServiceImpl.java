package com.heima.comment.service.impl;

import com.heima.comment.feign.UserFeign;
import com.heima.comment.service.CommentService;
import com.heima.comment.service.HotCommentService;
import com.heima.model.comment.dtos.CommentDto;
import com.heima.model.comment.dtos.CommentLikeDto;
import com.heima.model.comment.dtos.CommentSaveDto;
import com.heima.model.comment.pojos.ApComment;
import com.heima.model.comment.pojos.ApCommentLike;
import com.heima.model.comment.vo.ApCommentVo;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.pojos.ApUser;
import com.heima.utils.threadlocal.AppThreadLocalUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;


@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private UserFeign userFeign;

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 保存评论
     * @param dto
     * @return
     */
    @Override
    public ResponseResult saveComment(CommentSaveDto dto) {

        //1.检查参数
        if(dto == null || dto.getArticleId()== null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //判断当前内容是否超过了140字
        if(StringUtils.isBlank(dto.getContent()) || dto.getContent().length() > 140){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"评论字数不能超过140字");
        }

        //2.判断用户是否登录
        ApUser user = AppThreadLocalUtils.getUser();
        if(user == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        //3.安全检查-->内容检查  自己实现

        //4.保存评论

        //查询用户
        ApUser apUser = userFeign.findById(user.getId());
        if(apUser == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"当前登录信息有误");
        }

        ApComment apComment = new ApComment();
        apComment.setAuthorId(user.getId());
        apComment.setAuthorName(apUser.getName());
        apComment.setImage(apUser.getImage());
        apComment.setContent(dto.getContent());
        apComment.setEntryId(dto.getArticleId());
        apComment.setLikes(0);
        apComment.setReply(0);
        apComment.setFlag((short)0);
        apComment.setType((short)0);
        apComment.setCreatedTime(new Date());
        mongoTemplate.save(apComment);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    @Autowired
    private HotCommentService hotCommentService;

    /**
     * 点赞或取消点赞
     * @param dto
     * @return
     */
    @Override
    public ResponseResult like(CommentLikeDto dto) {
        //1.检查参数
        if(dto == null || dto.getCommentId() == null || dto.getOperation() > 1 || dto.getOperation() < 0){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.是否登录
        ApUser user = AppThreadLocalUtils.getUser();
        if(user == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        //3.点赞的操作
        ApComment apComment = mongoTemplate.findById(dto.getCommentId(), ApComment.class);
        if(apComment!= null && dto.getOperation() == 0){
            //3.1 更新评论的点赞数  + 1
            apComment.setLikes(apComment.getLikes() + 1);
            mongoTemplate.save(apComment);

            //计算评论是否是热点评论  1，当前评论是普通评论，2，点赞数大于5
            if(apComment.getFlag().shortValue()==0 && apComment.getLikes() >= 5){
                hotCommentService.findHotComment(apComment.getEntryId(),apComment);
            }


            //3.2 保存评论点赞文档
            ApCommentLike apCommentLike = new ApCommentLike();
            apCommentLike.setAuthorId(user.getId());
            apCommentLike.setCommentId(apComment.getId());
            mongoTemplate.save(apCommentLike);
        }else if(apComment != null && dto.getOperation() == 1){
            //4.取消点赞的操作

            //4.1 更新评论的点赞数  -1
            apComment.setLikes(apComment.getLikes() <= 0 ? 0:apComment.getLikes()-1);
            mongoTemplate.save(apComment);
            //4.2 删除评论点赞文档
            mongoTemplate.remove(Query.query(Criteria.where("authorId").is(user.getId()).and("commentId").is(apComment.getId())),ApCommentLike.class);
        }
        //5.数据返回
        Map<String,Object> resultMap = new HashMap<>();
        resultMap.put("likes",apComment.getLikes());
        return ResponseResult.okResult(resultMap);
    }

    /**
     * 查询评论列表
     * @param dto
     * @return
     */
    @Override
    public ResponseResult findAritlcId(CommentDto dto) {
        //1.检查参数
        if(dto == null || dto.getArticleId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        int size = 10;

        //2.查询当前文章对应的评论

        List<ApComment> apComments = null;

        //是首页
        if(dto.getIndex() == 1){
            //查询热点评论  5
            Query query = Query.query(Criteria.where("entryId").is(dto.getArticleId()).and("flag").is(1)).with(Sort.by(Sort.Direction.DESC, "likes"));
            apComments = mongoTemplate.find(query, ApComment.class);
            //查询普通评论
            if(apComments != null && apComments.size() > 0){
                size = size - apComments.size();
                //是首页
                Query query1 = Query.query(Criteria.where("entryId").is(dto.getArticleId()).and("flag").is(0).and("createdTime").lt(dto.getMinDate())).limit(size).with(Sort.by(Sort.Direction.DESC, "createdTime"));
                List<ApComment> apComments1 = mongoTemplate.find(query1, ApComment.class);
                apComments.addAll(apComments1);
            }else {

                Query query2 = Query.query(Criteria.where("entryId").is(dto.getArticleId()).and("flag").is(0).and("createdTime").lt(dto.getMinDate())).limit(size).with(Sort.by(Sort.Direction.DESC, "createdTime"));
                apComments = mongoTemplate.find(query2, ApComment.class);
            }

        }else {
            //不是首页
            Query query = Query.query(Criteria.where("entryId").is(dto.getArticleId()).and("flag").is(0).and("createdTime").lt(dto.getMinDate())).limit(size).with(Sort.by(Sort.Direction.DESC, "createdTime"));
            apComments = mongoTemplate.find(query, ApComment.class);

        }




        List<ApCommentVo> list = new ArrayList<>();

        //3.没有登录 直接返回数据
        ApUser user = AppThreadLocalUtils.getUser();
        if(user == null){
            return ResponseResult.okResult(apComments);
        }else{
            //4.已登录，需要判断当前用户点赞了哪些评论
            List<String> idList = apComments.stream().map(x -> x.getId()).collect(Collectors.toList());
            List<ApCommentLike> apCommentLikes = mongoTemplate.find(Query.query(Criteria.where("authorId").is(user.getId()).and("commentId").in(idList)), ApCommentLike.class);
            if(apComments!= null && apCommentLikes!= null){
                apComments.forEach(comment->{
                    ApCommentVo apCommentVo = new ApCommentVo();
                    BeanUtils.copyProperties(comment,apCommentVo);
                    for (ApCommentLike apCommentLike : apCommentLikes) {
                        if(comment.getId().equals(apCommentLike.getCommentId())){
                            apCommentVo.setOperation((short)0);
                        }
                    }
                    list.add(apCommentVo);
                });
                return ResponseResult.okResult(list);
            }else {
                return ResponseResult.okResult(apComments);
            }
        }

    }
}
