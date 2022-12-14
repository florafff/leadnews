package com.heima.search.controller.v1;

import com.heima.apis.search.ApUserSearchControllerApi;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.UserSearchDto;
import com.heima.search.service.ApUserSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/history")
public class ApUserSearchController implements ApUserSearchControllerApi {

    @Autowired
    private ApUserSearchService apUserSearchService;

    /**
     * 加载用户搜索历史记录
     * @param dto
     * @return
     */
    @PostMapping("/load")
    @Override
    public ResponseResult findUserSearch(@RequestBody UserSearchDto dto) {
        return apUserSearchService.findUserSearch(dto);
    }

    /**
     * 删除搜索历史记录
     * @param dto
     * @return
     */
    @PostMapping("/del")
    @Override
    public ResponseResult delUserSearch(@RequestBody UserSearchDto dto) {
        return apUserSearchService.delUserSearch(dto);
    }
}
