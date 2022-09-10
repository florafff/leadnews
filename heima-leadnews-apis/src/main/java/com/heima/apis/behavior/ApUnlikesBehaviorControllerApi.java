package com.heima.apis.behavior;

import com.heima.model.behavior.pojos.ApUnlikesBehavior;

public interface ApUnlikesBehaviorControllerApi {

    /**
     * 根据文章id和行为实体id查询不喜欢
     * @param articleId
     * @param entryId
     * @return
     */
    public ApUnlikesBehavior findUnLikesByArticleIdAndEntryId(Long articleId,Integer entryId);
}
