package com.heima.wemedia.controller.v1;

import com.heima.apis.wemedia.WmNewsControllerApi;
import com.heima.common.constants.WemediaContants;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import com.heima.model.wemedia.dtos.NewsAuthDto;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmNews;
import com.heima.wemedia.service.WmNewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/news")
public class WmNewsController implements WmNewsControllerApi {

    @Autowired
    private WmNewsService wmNewsService;

    /**
     * 查询自媒体文章列表
     * @param dto
     * @return
     */
    @PostMapping("/list")
    @Override
    public ResponseResult findAll(@RequestBody WmNewsPageReqDto dto) {
        return wmNewsService.findAll(dto);
    }

    /**
     * 文章提交  保存、修改、保存为草稿
     * @param dto
     * @return
     */
    @PostMapping("/submit")
    @Override
    public ResponseResult submitNews(@RequestBody WmNewsDto dto) {
        if(dto.getStatus().equals(WmNews.Status.SUBMIT.getCode())){
            return wmNewsService.submitNews(dto,WmNews.Status.SUBMIT.getCode());
        }else {
            return wmNewsService.submitNews(dto,WmNews.Status.NORMAL.getCode());
        }

    }

    /**
     * 根据id查询文章
     * @param id
     * @return
     */
    @GetMapping("/one/{id}")
    @Override
    public ResponseResult findWmNewsById(@PathVariable("id") Integer id) {
        return wmNewsService.findWmNewsById(id);
    }

    /**
     * 删除文章
     * @param id
     * @return
     */
    @GetMapping("/del_news/{id}")
    @Override
    public ResponseResult delNews(@PathVariable("id") Integer id) {
        return wmNewsService.delNews(id);
    }

    /**
     * 文章的上下架
     *
     * @param dto
     * @return
     */
    @PostMapping("/down_or_up")
    @Override
    public ResponseResult downOrUp(@RequestBody WmNewsDto dto) {
        return wmNewsService.downOrUp(dto);
    }

    /**
     * 查询自媒体文章
     * @param id
     * @return
     */
    @GetMapping("/findOne/{id}")
    @Override
    public WmNews findNewsById(@PathVariable("id") Integer id) {
        return wmNewsService.getById(id);
    }

    /**
     * 修改文章
     * @param wmNews
     * @return
     */
    @PostMapping("/update")
    @Override
    public ResponseResult updateWmNews(@RequestBody WmNews wmNews) {
        wmNewsService.updateById(wmNews);
        return ResponseResult.okResult(AppHttpCodeEnum.SUCCESS);
    }

    /**
     * 查询自媒体文章需要发布的文章id列表
     * @return
     */
    @GetMapping("/findRelease")
    @Override
    public List<Integer> findRelease() {
        return wmNewsService.findRelease();
    }

    /**
     * 查询自媒体文章列表 返回的vo对象
     * @param dto
     * @return
     */
    @PostMapping("/list_vo")
    @Override
    public ResponseResult findList(@RequestBody NewsAuthDto dto) {
        return wmNewsService.findList(dto);
    }

    /**
     * 查询文章详情  返回的vo对象
     * @param id
     * @return
     */
    @GetMapping("/one_vo/{id}")
    @Override
    public ResponseResult findWmNewsVo(@PathVariable("id") Integer id) {
        return wmNewsService.findWmNewsVo(id);
    }

    /**
     * 文章审核成功
     * @param dto
     * @return
     */
    @PostMapping("/auth_pass")
    @Override
    public ResponseResult authPass(@RequestBody NewsAuthDto dto) {
        return wmNewsService.updateStatus(WemediaContants.WM_NEWS_AUTH_PASS,dto);
    }

    /**
     * 文章审核失败
     * @param dto
     * @return
     */
    @PostMapping("/auth_fail")
    @Override
    public ResponseResult authFail(@RequestBody NewsAuthDto dto) {
        return wmNewsService.updateStatus(WemediaContants.WM_NEWS_AUTH_FAIL,dto);
    }
}
