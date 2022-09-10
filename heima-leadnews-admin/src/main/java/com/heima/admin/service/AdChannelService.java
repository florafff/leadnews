package com.heima.admin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.heima.model.admin.dtos.ChannelDto;
import com.heima.model.admin.pojos.AdChannel;
import com.heima.model.common.dtos.ResponseResult;

public interface AdChannelService extends IService<AdChannel> {

    /**
     * 获取频道的列表
     * @param dto
     * @return
     */
    public ResponseResult findByNameAndPage(ChannelDto dto);

    /**
     * 保存频道
     * @param adChannel
     * @return
     */
    public ResponseResult insert(AdChannel adChannel);

    /**
     * 修改频道
     * @param adChannel
     * @return
     */
    public ResponseResult update(AdChannel adChannel);

    /**
     * 根据id删除频道
     * @param id
     * @return
     */
    public ResponseResult deleteById(Integer id);
}
