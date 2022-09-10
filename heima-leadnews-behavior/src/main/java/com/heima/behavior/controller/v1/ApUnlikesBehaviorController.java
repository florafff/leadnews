package com.heima.behavior.controller.v1;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.apis.behavior.ApUnlikesBehaviorControllerApi;
import com.heima.behavior.service.ApUnlikesBehaviorService;
import com.heima.model.behavior.pojos.ApUnlikesBehavior;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/un_likes_behavior")
public class ApUnlikesBehaviorController implements ApUnlikesBehaviorControllerApi {

    @Autowired
    private ApUnlikesBehaviorService apUnlikesBehaviorService;

    /**
     * 根据文章id和行为实体id查询不喜欢
     * @param articleId
     * @param entryId
     * @return
     */
    @GetMapping("/one")
    @Override
    public ApUnlikesBehavior findUnLikesByArticleIdAndEntryId(@RequestParam("articleId") Long articleId,@RequestParam("entryId")  Integer entryId) {
        return apUnlikesBehaviorService.getOne(Wrappers.<ApUnlikesBehavior>lambdaQuery()
                .eq(ApUnlikesBehavior::getArticleId,articleId).eq(ApUnlikesBehavior::getEntryId,entryId));
    }
}
