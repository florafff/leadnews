package com.heima.comment.feign;

import com.heima.model.user.pojos.ApUser;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("leadnews-app-user")
public interface UserFeign {

    @GetMapping("/api/v1/user/{id}")
    public ApUser findById(@PathVariable("id") Integer id);
}
