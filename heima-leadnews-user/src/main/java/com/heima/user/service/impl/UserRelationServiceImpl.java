package com.heima.user.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.common.constants.FollowBehaviorConstants;
import com.heima.model.article.pojos.ApAuthor;
import com.heima.model.behavior.dtos.FollowBehaviorDto;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.UserRelationDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.model.user.pojos.ApUserFan;
import com.heima.model.user.pojos.ApUserFollow;
import com.heima.user.feign.ArticleFeign;
import com.heima.user.mapper.ApUserFanMapper;
import com.heima.user.mapper.ApUserFollowMapper;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.service.UserRelationService;
import com.heima.utils.threadlocal.AppThreadLocalUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@Transactional
public class UserRelationServiceImpl implements UserRelationService {

    @Autowired
    private ArticleFeign articleFeign;

    /**
     * 关注或取消关注
     * @param dto
     * @return
     */
    @Override
    public ResponseResult follow(UserRelationDto dto) {
        //1.检查参数
        if(dto == null || dto.getOperation() > 1 || dto.getOperation() < 0){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }
        //判断作者
        if(dto.getAuthorId() == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"当前作者不存在");
        }
        //获取登录人
        ApUser loginUser  = AppThreadLocalUtils.getUser();
        if(loginUser == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.NEED_LOGIN);
        }

        //2.获取作者的对应的用户id
        ApAuthor apAuthor = articleFeign.findById(dto.getAuthorId());
        //关注人的用户id
        Integer followId = apAuthor.getUserId();


        if(dto.getOperation() == 0){
            //3.关注 保存（用户关注ap_user_follow&& 用户粉丝ap_user_fan）  记录用户的关注行为
            return followByUserId(loginUser,followId,dto.getArticleId());

        }else {
            //4.取消关注 删除（用户关注ap_user_follow&& 用户粉丝ap_user_fan）
            return followCancelByUserId(loginUser,followId);
        }

    }

    /**
     * 取消关注  删除（用户关注ap_user_follow&& 用户粉丝ap_user_fan）
     * @param loginUser
     * @param followId
     * @return
     */
    private ResponseResult followCancelByUserId(ApUser loginUser, Integer followId) {
        ApUserFollow apUserFollow = apUserFollowMapper.selectOne(Wrappers.<ApUserFollow>lambdaQuery().eq(ApUserFollow::getUserId, loginUser.getId())
                .eq(ApUserFollow::getFollowId, followId));
        if(apUserFollow != null){
            ApUserFan apUserFan = apUserFanMapper.selectOne(Wrappers.<ApUserFan>lambdaQuery().eq(ApUserFan::getUserId, followId).eq(ApUserFan::getFansId, loginUser.getId()));
            //删除粉丝信息
            if(apUserFan != null){
                apUserFanMapper.delete(Wrappers.<ApUserFan>lambdaQuery().eq(ApUserFan::getUserId, followId).eq(ApUserFan::getFansId, loginUser.getId()));
            }
            //删除关注信息
            apUserFollowMapper.delete(Wrappers.<ApUserFollow>lambdaQuery().eq(ApUserFollow::getUserId, loginUser.getId())
                    .eq(ApUserFollow::getFollowId, followId));

            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);

        }else {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"未关注");
        }
    }

    @Autowired
    private ApUserMapper apUserMapper;

    @Autowired
    private ApUserFollowMapper apUserFollowMapper;

    @Autowired
    private ApUserFanMapper apUserFanMapper;

    @Autowired
    private KafkaTemplate kafkaTemplate;

    /**
     * 关注操作  保存用户关注ap_user_follow&& 用户粉丝ap_user_fan
     * @param loginUser  当前登录人的信息
     * @param followId   作者的用户id
     * @param articleId  文章id
     * @return
     */
    private ResponseResult followByUserId(ApUser loginUser, Integer followId, Long articleId) {
        //获取到作者对应的用户信息
        ApUser followUser = apUserMapper.selectById(followId);
        if(followUser == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"当前作者不存在");
        }
        ApUserFollow apUserFollow = apUserFollowMapper.selectOne(Wrappers.<ApUserFollow>lambdaQuery().eq(ApUserFollow::getUserId, loginUser.getId())
                .eq(ApUserFollow::getFollowId, followId));
        if(apUserFollow == null){
            //保存
            ApUserFan apUserFan = apUserFanMapper.selectOne(Wrappers.<ApUserFan>lambdaQuery().eq(ApUserFan::getUserId, followId).eq(ApUserFan::getFansId, loginUser.getId()));
            if(apUserFan == null){
                //保存粉丝
                apUserFan = new ApUserFan();
                apUserFan.setUserId(followId);
                apUserFan.setFansId(loginUser.getId().longValue());
                apUserFan.setIsDisplay(true);
                apUserFan.setIsShieldLetter(false);
                apUserFan.setIsShieldComment(false);
                apUserFan.setLevel((short)0);
//                apUserFan.setFansName();
                apUserFan.setCreatedTime(new Date());
                apUserFanMapper.insert(apUserFan);

            }
            //保存关注信息
            apUserFollow = new ApUserFollow();
            apUserFollow.setUserId(loginUser.getId());
            apUserFollow.setFollowId(followId);
            apUserFollow.setIsNotice(true);
            apUserFollow.setLevel((short)1);
            apUserFollow.setCreatedTime(new Date());
            apUserFollow.setFollowName(followUser.getName());
            apUserFollowMapper.insert(apUserFollow);

            //发消息  登录人  作者的用户id  文章id
            FollowBehaviorDto dto = new FollowBehaviorDto();
            dto.setArticleId(articleId);
            dto.setUserId(loginUser.getId());
            dto.setFollowId(followId);
            //异步发送消息
            kafkaTemplate.send(FollowBehaviorConstants.FOLLOW_BEHAVIOR_TOPIC, JSON.toJSONString(dto));


            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);

        }else {
            return ResponseResult.errorResult(AppHttpCodeEnum.DATA_EXIST,"已关注");
        }

    }
}
