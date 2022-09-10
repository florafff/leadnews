package com.heima.wemedia.controller.v1;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.apis.wemedia.WmUserControllerApi;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.pojos.WmUser;
import com.heima.wemedia.service.WmUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.sql.Wrapper;

@RestController
@RequestMapping("/api/v1/user")
public class WmUserController implements WmUserControllerApi {

    @Autowired
    private WmUserService wmUserService;


    @GetMapping("/findByName/{name}")
    @Override
    public WmUser findByName(@PathVariable("name") String name) {
        return wmUserService.getOne(Wrappers.<WmUser>lambdaQuery().eq(WmUser::getName,name));
    }

    @PostMapping("/save")
    @Override
    public ResponseResult save(@RequestBody WmUser wmUser) {
//        int a = 1/0;
        wmUserService.save(wmUser);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 查询自媒体用户
     * @param id
     * @return
     */
    @GetMapping("/findOne/{id}")
    @Override
    public WmUser findUserById(@PathVariable("id") Integer id) {
        return wmUserService.getById(id);
    }
}
