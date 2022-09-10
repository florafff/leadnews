package com.heima.search.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.behavior.pojos.ApBehaviorEntry;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.UserSearchDto;
import com.heima.model.search.pojos.ApUserSearch;

public interface ApUserSearchService  extends IService<ApUserSearch> {

    /**
     * 保存用户搜索记录
     * @param entry
     * @param searchWords
     */
    void insert(ApBehaviorEntry entry, String searchWords);

    /**
     * 加载用户搜索历史记录
     * @param dto
     * @return
     */
    public ResponseResult findUserSearch(UserSearchDto dto);

    /**
     * 删除搜索历史记录
     * @param dto
     * @return
     */
    public ResponseResult delUserSearch(UserSearchDto dto);
}
