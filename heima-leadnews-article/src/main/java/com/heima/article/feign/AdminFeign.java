package com.heima.article.feign;

import com.heima.model.common.dtos.ResponseResult;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("leadnews-admin")
public interface AdminFeign {

    /**
     * 查询所有频道
     * @return
     */
    @GetMapping("/api/v1/channel/channels")
    public ResponseResult findAll();
}
