package com.heima.apis.article;

import com.heima.model.article.pojos.ApAuthor;
import com.heima.model.common.dtos.ResponseResult;

public interface AuthorControllerApi {

    /**
     * 根据用户id查询作者
     * @param id
     * @return
     */
    public ApAuthor findByUserId(Integer id);

    /**
     * 保存
     * @param apAuthor
     * @return
     */
    public ResponseResult save(ApAuthor apAuthor);

    /**
     * 根据id查询作者
     * @param id
     * @return
     */
    public ApAuthor findById(Integer id);
}
