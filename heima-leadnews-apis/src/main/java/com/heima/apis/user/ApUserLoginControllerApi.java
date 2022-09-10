package com.heima.apis.user;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.dtos.LoginDto;

public interface ApUserLoginControllerApi {



    /**
     * //手机号，密码 只要是app过来的请求，都会携带一个设备id号  设备id--》指的是手机上的一个唯一标识号
     * app登录功能
     * @param dto
     * @return
     */
    public ResponseResult login(LoginDto dto);
}
