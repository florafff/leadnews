package com.heima.apis.user;

import ch.qos.logback.core.pattern.util.RegularEscapeUtil;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.user.dtos.AuthDto;

public interface ApUserRealnameControllerApi {


    /**
     * 加载用户认证列表
     * @param dto
     * @return
     */
    public ResponseResult loadListByStatus(AuthDto dto);

    /**
     * 审核失败
     * @param dto
     * @return
     */
    public ResponseResult authFail(AuthDto dto);

    /**
     * 审核成功
     * @param dto
     * @return
     */
    public ResponseResult authPass(AuthDto dto);
}
