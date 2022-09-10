package com.heima.apis.behavior;

import com.heima.model.behavior.dtos.LikesBehaviorDto;
import com.heima.model.behavior.pojos.ApLikesBehavior;
import com.heima.model.common.dtos.ResponseResult;

public interface ApLikesBehaviorControllerApi {

    /**
     * 文章的点赞或取消点赞
     * @param dto
     * @return
     */
    public ResponseResult like(LikesBehaviorDto dto);

    /**
     * 根据行为实体和文章id查询点赞
     * @param articleId
     * @param entryId
     * @param type
     * @return
     */
    public ApLikesBehavior findLikeByArticleIdAndEntryId(Long articleId,Integer entryId,Short type);
}
