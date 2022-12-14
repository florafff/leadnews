package com.heima.behavior.controller.v1;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.apis.behavior.ApLikesBehaviorControllerApi;
import com.heima.behavior.service.ApLikesBehaviorService;
import com.heima.model.behavior.dtos.LikesBehaviorDto;
import com.heima.model.behavior.pojos.ApLikesBehavior;
import com.heima.model.common.dtos.ResponseResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/likes_behavior")
public class ApLikesBehaviorController implements ApLikesBehaviorControllerApi {

    @Autowired
    private ApLikesBehaviorService apLikesBehaviorService;

    /**
     * 文章的点赞或取消点赞
     * @param dto
     * @return
     */
    @PostMapping
    @Override
    public ResponseResult like(@RequestBody LikesBehaviorDto dto) {
        return apLikesBehaviorService.like(dto);
    }

    /**
     * 根据行为实体和文章id查询点赞
     * @param articleId
     * @param entryId
     * @param type
     * @return
     */
    @GetMapping("/one")
    @Override
    public ApLikesBehavior findLikeByArticleIdAndEntryId(@RequestParam("articleId") Long articleId,@RequestParam("entryId") Integer entryId,@RequestParam("type") Short type) {
        return apLikesBehaviorService.getOne(Wrappers.<ApLikesBehavior>lambdaQuery().eq(ApLikesBehavior::getArticleId,articleId)
                .eq(ApLikesBehavior::getEntryId,entryId).eq(ApLikesBehavior::getType,type));
    }
}
