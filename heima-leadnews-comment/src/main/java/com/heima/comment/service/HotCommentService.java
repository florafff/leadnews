package com.heima.comment.service;

import com.heima.model.comment.pojos.ApComment;

public interface HotCommentService {

    /**
     * 计算热点评论数据
     * @param articleId
     * @param apComment
     */
    public void findHotComment(Long articleId, ApComment apComment);
}
