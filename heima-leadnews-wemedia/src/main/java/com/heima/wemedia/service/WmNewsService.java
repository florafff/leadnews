package com.heima.wemedia.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.wemedia.dtos.NewsAuthDto;
import com.heima.model.wemedia.dtos.WmNewsDto;
import com.heima.model.wemedia.dtos.WmNewsPageReqDto;
import com.heima.model.wemedia.pojos.WmNews;

import java.util.List;

public interface WmNewsService extends IService<WmNews> {

    /**
     * 查询自媒体文章列表
     * @param dto
     * @return
     */
    public ResponseResult findAll(WmNewsPageReqDto dto);


    /**
     *
     * @param dto
     * @param isSubmit  0 为草稿  1:待审核
     * @return
     */
    public ResponseResult submitNews(WmNewsDto dto,Short isSubmit);

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
     * 修改文章的状态
     * @param status  2 失败  4 人工审核成功
     * @param dto
     * @return
     */
    public ResponseResult updateStatus(Short status ,NewsAuthDto dto);
}
