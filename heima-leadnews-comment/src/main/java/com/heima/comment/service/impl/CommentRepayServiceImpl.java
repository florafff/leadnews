package com.heima.comment.service.impl;

import com.heima.comment.feign.UserFeign;
import com.heima.comment.service.CommentRepayService;
import com.heima.model.comment.dtos.CommentRepayDto;
import com.heima.model.comment.dtos.CommentRepayLikeDto;
import com.heima.model.comment.dtos.CommentRepaySaveDto;
import com.heima.model.comment.pojos.ApComment;
import com.heima.model.comment.pojos.ApCommentLike;
import com.heima.model.comment.pojos.ApCommentRepay;
import com.heima.model.comment.pojos.ApCommentRepayLike;
import com.heima.model.comment.vo.ApCommentRepayVo;
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
public class CommentRepayServiceImpl implements CommentRepayService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private UserFeign userFeign;

    /**
     * 查询评论回复列表
     * @param dto
     * @return
     */
    @Override
    public ResponseResult loadCommentRepay(CommentRepayDto dto) {
        //1.检查参数
        if(dto == null || dto.getCommentId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        int size = 10;

        //2.查询当前文章对应的评论
        Query query = Query.query(Criteria.where("commentId").is(dto.getCommentId()).and("createdTime").lt(dto.getMinDate())).limit(size).with(Sort.by(Sort.Direction.DESC, "createdTime"));
        List<ApCommentRepay> apCommentRepays = mongoTemplate.find(query, ApCommentRepay.class);


        List<ApCommentRepayVo> list = new ArrayList<>();

        //3.没有登录 直接返回数据
        ApUser user = AppThreadLocalUtils.getUser();
        if(user == null){
            return ResponseResult.okResult(apCommentRepays);
        }else{
            //4.已登录，需要判断当前用户点赞了哪些评论
            List<String> idList = apCommentRepays.stream().map(x -> x.getId()).collect(Collectors.toList());
            List<ApCommentRepayLike> apCommentRepayLikes = mongoTemplate.find(Query.query(Criteria.where("authorId").is(user.getId()).and("commentRepayId").in(idList)), ApCommentRepayLike.class);
            if(apCommentRepays!= null && apCommentRepayLikes!= null){
                apCommentRepays.forEach(comment->{
                    ApCommentRepayVo apCommentRepayVo = new ApCommentRepayVo();
                    BeanUtils.copyProperties(comment,apCommentRepayVo);

                    for (ApCommentRepayLike apCommentRepayLike : apCommentRepayLikes) {
                        if(comment.getId().equals(apCommentRepayLike.getCommentRepayId())){
                            apCommentRepayVo.setOperation((short)0);
                        }
                    }
                    list.add(apCommentRepayVo);
                });
                return ResponseResult.okResult(list);
            }else {
                return ResponseResult.okResult(apCommentRepays);
            }
        }
    }

    /**
     * 保存评论回复
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult saveCommentRepay(CommentRepaySaveDto dto) {
        //1.检查参数
        if(dto == null || dto.getCommentId()== null){
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

        ApCommentRepay apCommentRepay= new ApCommentRepay();
        apCommentRepay.setAuthorId(user.getId());
        apCommentRepay.setAuthorName(apUser.getName());
        apCommentRepay.setContent(dto.getContent());
        apCommentRepay.setCommentId(dto.getCommentId());
        apCommentRepay.setLikes(0);
        apCommentRepay.setCreatedTime(new Date());
        mongoTemplate.save(apCommentRepay);
        //更新评论的回复数量
        ApComment apComment = mongoTemplate.findById(dto.getCommentId(), ApComment.class);
        apComment.setReply(apComment.getReply()+1);
        mongoTemplate.save(apComment);

        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 评论回复点赞
     *
     * @param dto
     * @return
     */
    @Override
    public ResponseResult saveCommentRepayLike(CommentRepayLikeDto dto) {
        //1.检查参数
        if(dto == null || dto.getCommentRepayId() == null || dto.getOperation() > 1 || dto.getOperation() < 0){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.是否登录
        ApUser user = AppThreadLocalUtils.getUser();
        if(user == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        //3.点赞的操作
        ApCommentRepay apCommentRepay = mongoTemplate.findById(dto.getCommentRepayId(), ApCommentRepay.class);
        if(apCommentRepay!= null && dto.getOperation() == 0){
            //3.1 更新评论的点赞数  + 1
            apCommentRepay.setLikes(apCommentRepay.getLikes() + 1);
            mongoTemplate.save(apCommentRepay);

            //3.2 保存评论点赞文档
            ApCommentRepayLike apCommentRepayLike = new ApCommentRepayLike();
            apCommentRepayLike.setAuthorId(user.getId());
            apCommentRepayLike.setCommentRepayId(apCommentRepay.getId());
            mongoTemplate.save(apCommentRepayLike);
        }else if(apCommentRepay != null && dto.getOperation() == 1){
            //4.取消点赞的操作

            //4.1 更新评论的点赞数  -1
            apCommentRepay.setLikes(apCommentRepay.getLikes() <= 0 ? 0:apCommentRepay.getLikes()-1);
            mongoTemplate.save(apCommentRepay);
            //4.2 删除评论点赞文档
            mongoTemplate.remove(Query.query(Criteria.where("authorId").is(user.getId()).and("commentRepayId").is(apCommentRepay.getId())),ApCommentRepayLike.class);
        }
        //5.数据返回
        Map<String,Object> resultMap = new HashMap<>();
        resultMap.put("likes",apCommentRepay.getLikes());
        return ResponseResult.okResult(resultMap);
    }
}
