package com.heima.comment.service.impl;

import com.heima.comment.service.HotCommentService;
import com.heima.model.comment.pojos.ApComment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class HotCommentServiceImpl implements HotCommentService {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 计算热点评论数据
     * @param articleId
     * @param apComment  点赞数超过5  当前评论是普通评论
     */
    @Override
    @Async("taskExecutor")//使用异步线程池
    public void findHotComment(Long articleId, ApComment apComment) {
        int a = 1/0;
        //查询当前文章热点评论的所有数据
        Query query = Query.query(Criteria.where("entryId").is(articleId).and("flag").is(1)).with(Sort.by(Sort.Direction.DESC, "likes"));
        List<ApComment> apComments = mongoTemplate.find(query, ApComment.class);

        if(apComments != null && apComments.size() > 0){
            //如果当前有数据，则判断当前评论的点赞数是否大于热点评论中的最小点赞数
            ApComment lastComment = apComments.get(apComments.size() - 1);
            //如果当前评论数据大于5条，则替换热点评论中的最后一条数据
            if(lastComment.getLikes() < apComment.getLikes()){
                apComment.setFlag((short)1);
                mongoTemplate.save(apComment);
                //如果当前热点评论不满足5条，则直//增
                if(apComments.size()>=5){
                    lastComment.setFlag((short)0);
                    mongoTemplate.save(lastComment);
                }
            }

        }else {
            //如果当前没有热点数据并且，直接新增
            apComment.setFlag((short)1);
            mongoTemplate.save(apComment);
        }





    }
}
