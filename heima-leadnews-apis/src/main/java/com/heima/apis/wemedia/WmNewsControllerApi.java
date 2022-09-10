package com.heima.apis.wemedia;

import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.NewsAuthDto;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmNews;

import java.util.List;

public interface WmNewsControllerApi {

    /**
     * 查询自媒体文章列表
     * @param dto
     * @return
     */
    public ResponseResult findAll(WmNewsPageReqDto dto);

    /**
     * 文章提交  保存、修改、保存为草稿
     * @param dto
     * @return
     */
    public ResponseResult submitNews(WmNewsDto dto);

    /**
     * 根据id查询文章
     * @param id
     * @return
     */
    public ResponseResult findWmNewsById(Integer id);

    /**
     * 删除文章
     * @param id
     * @return
     */
    public ResponseResult delNews(Integer id);

    /**
     * 文章的上下架
     * @param dto
     * @return
     */
    public ResponseResult downOrUp(WmNewsDto dto);

    /**
     * 查询自媒体文章
     * @param id
     * @return
     */
    public WmNews findNewsById(Integer id);

    /**
     * 修改文章
     * @param wmNews
     * @return
     */
    public ResponseResult updateWmNews(WmNews wmNews);

    /**
     * 查询自媒体文章需要发布的文章id列表
     * @return
     */
    public List<Integer> findRelease();

    /**
     * 查询自媒体文章列表 返回的vo对象
     * @param dto
     * @return
     */
    public ResponseResult findList(NewsAuthDto dto);

    /**
     * 查询文章详情  返回的vo对象
     * @param id
     * @return
     */
    public ResponseResult findWmNewsVo(Integer id);

    /**
     * 文章审核成功
     * @param dto
     * @return
     */
    public ResponseResult authPass(NewsAuthDto dto);

    /**
     * 文章审核失败
     * @param dto
     * @return
     */
    public ResponseResult authFail(NewsAuthDto dto);
}
