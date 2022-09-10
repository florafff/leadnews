package com.heima.behavior.service.impl;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.heima.behavior.mapper.ApLikesBehaviorMapper;
import com.heima.behavior.service.ApBehaviorEntryService;
import com.heima.behavior.service.ApLikesBehaviorService;
import com.heima.common.constants.HotArticleConstants;
import com.heima.model.behavior.dtos.LikesBehaviorDto;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.behavior.pojos.ApLikesBehavior;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.mess.UpdateArticleMess;
import com.heima.utils.threadlocal.AppThreadLocalUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;


@Service
@Transactional
public class ApLikesBehaviorServiceImpl extends ServiceImpl<ApLikesBehaviorMapper, ApLikesBehavior> implements ApLikesBehaviorService {

    @Autowired
    private ApBehaviorEntryService apBehaviorEntryService;

    @Autowired
    private KafkaTemplate<String,String> kafkaTemplate;

    /**
     * 文章的点赞或取消点赞
     * @param dto
     * @return
     */
    @Override
    public ResponseResult like(LikesBehaviorDto dto) {
        //1.检查参数
        if(dto == null || dto.getArticleId() == null || (dto.getType() > 2 || dto.getType() < 0) || (dto.getOperation() < 0 || dto.getOperation() > 1)){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.查询行为实体
        ApBehaviorEntry apBehaviorEntry = apBehaviorEntryService.findByUserIdOrEquipmentId(AppThreadLocalUtils.getUser().getId(), dto.getEquipmentId());
        if(apBehaviorEntry == null){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //查询点赞行为对象
        ApLikesBehavior apLikesBehavior = getOne(Wrappers.<ApLikesBehavior>lambdaQuery().eq(ApLikesBehavior::getArticleId, dto.getArticleId())
                .eq(ApLikesBehavior::getEntryId, apBehaviorEntry.getId()));
        if(apLikesBehavior == null && dto.getOperation().equals(ApLikesBehavior.Operation.CANCEL.getCode())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"当前文章没有被点赞");
        }

        //发送消息
        if(dto.getOperation().equals(ApLikesBehavior.Operation.LIKE.getCode())){
            UpdateArticleMess mess = new UpdateArticleMess();
            mess.setArticleId(dto.getArticleId());
            mess.setType(UpdateArticleMess.UpdateArticleType.LIKES);
            mess.setAdd(1);
            kafkaTemplate.send(HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC, JSON.toJSONString(mess));
        }


        if(apLikesBehavior == null && dto.getOperation().equals(ApLikesBehavior.Operation.LIKE.getCode())){
            //3.点赞  保存数据
            apLikesBehavior = new ApLikesBehavior();
            apLikesBehavior.setOperation(dto.getOperation());
            apLikesBehavior.setEntryId(apBehaviorEntry.getId());
            apLikesBehavior.setArticleId(dto.getArticleId());
            apLikesBehavior.setCreatedTime(new Date());
            apLikesBehavior.setType(dto.getType());
            save(apLikesBehavior);
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);

        }else {
            //4.取消点赞 修改数据
            apLikesBehavior.setOperation(dto.getOperation());
            updateById(apLikesBehavior);
            return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);

        }
    }


}
