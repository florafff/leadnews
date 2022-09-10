package com.heima.user.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.user.dtos.LoginDto;
import com.heima.model.user.pojos.ApUser;
import com.heima.user.mapper.ApUserMapper;
import com.heima.user.service.ApUserLoginService;
import com.heima.utils.common.AppJwtUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;

@Service
@Transactional
public class ApUserLoginServiceImpl implements ApUserLoginService {

    @Autowired
    private ApUserMapper apUserMapper;

    /**
     * //手机号，密码 只要是app过来的请求，都会携带一个设备id号  设备id--》指的是手机上的一个唯一标识号
     * app登录功能
     * @param dto
     * @return
     */
    @Override
    public ResponseResult login(LoginDto dto) {
        //1.检查参数
        if(dto.getEquipmentId() == null  && StringUtils.isBlank(dto.getPhone())&& StringUtils.isBlank(dto.getPassword())){
            return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
        }

        //2.用户登录 （手机号+密码）
        if(StringUtils.isNotBlank(dto.getPhone()) && StringUtils.isNotBlank(dto.getPassword())){
            ApUser apUser = apUserMapper.selectOne(Wrappers.<ApUser>lambdaQuery().eq(ApUser::getPhone, dto.getPhone()));
            if(apUser != null){
                //比对密码
                String pswd = DigestUtils.md5DigestAsHex((dto.getPassword() + apUser.getSalt()).getBytes());
                if(pswd.equals(apUser.getPassword())){

                    Map<String,Object> map = new HashMap<>();
                    map.put("token", AppJwtUtil.getToken(apUser.getId().longValue()));
                    apUser.setPassword("");
                    apUser.setSalt("");
                    map.put("user",apUser);
                    return ResponseResult.okResult(map);


                }else {
                    return ResponseResult.errorResult(AppHttpCodeEnum.LOGIN_PASSWORD_ERROR);
                }

            }else {
                return ResponseResult.errorResult(AppHttpCodeEnum.DATA_NOT_EXIST,"当前用户不存在");
            }

        }else{
            //3.设备登录
            if(dto.getEquipmentId()== null){
                return ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID);
            }
            Map<String,Object> map = new HashMap<>();
            map.put("token",AppJwtUtil.getToken(0l));
            return ResponseResult.okResult(map);
        }

    }
}
