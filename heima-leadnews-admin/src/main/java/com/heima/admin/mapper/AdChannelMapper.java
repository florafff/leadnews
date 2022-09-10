package com.heima.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.heima.model.admin.pojos.AdChannel;
import org.apache.ibatis.annotations.Mapper;

/**
 * 频道的持久层
 */
@Mapper
public interface AdChannelMapper extends BaseMapper<AdChannel> {
}
