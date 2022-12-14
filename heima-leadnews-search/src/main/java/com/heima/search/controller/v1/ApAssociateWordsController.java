package com.heima.search.controller.v1;

import com.heima.apis.search.ApAssociateWordsControllerApi;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.search.dtos.UserSearchDto;
import com.heima.search.service.ApAssociateWordsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/associate")
public class ApAssociateWordsController implements ApAssociateWordsControllerApi {

    @Autowired
    private ApAssociateWordsService apAssociateWordsService;

    /**
     * 查询关键字联想词
     * @param dto
     * @return
     */
    @PostMapping("/search")
    @Override
    public ResponseResult search(@RequestBody UserSearchDto dto) {
        return apAssociateWordsService.search(dto);
    }
}
