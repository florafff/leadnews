package com.heima.common.exception;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import lombok.extern.log4j.Log4j2;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice  //控制器增强
@Log4j2
public class ExceptionCatch {

    @ExceptionHandler(Exception.class)
    @ResponseBody
    public ResponseResult exception(Exception e){
        e.printStackTrace();
        log.error("catch exception:{}",e.getMessage());
        //发邮件，发短信
        return ResponseResult.errorResult(AppHttpCodeEnum.SERVER_ERROR);
    }

    @ExceptionHandler(CostomException.class)
    @ResponseBody
    public ResponseResult exception(CostomException e){
        e.printStackTrace();
        log.error("catch exception:{}",e.getMessage());
        return e.getResponseResult();
    }
}
