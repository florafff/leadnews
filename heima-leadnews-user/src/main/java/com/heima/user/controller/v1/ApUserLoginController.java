package com.heima.user.controller.v1;

import com.heima.apis.user.ApUserLoginControllerApi;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.dtos.LoginDto;
import com.heima.user.service.ApUserLoginService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/login")
public class ApUserLoginController implements ApUserLoginControllerApi {

    @Autowired
    private ApUserLoginService apUserLoginService;
    /**
     * //手机号，密码 只要是app过来的请求，都会携带一个设备id号  设备id--》指的是手机上的一个唯一标识号
     * app登录功能
     *
     * @param dto
     * @return
     */
    @PostMapping("/login_auth")
    @Override
    public ResponseResult login(@RequestBody LoginDto dto) {
        return apUserLoginService.login(dto);
    }
}
