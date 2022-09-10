package com.heima.admin.controller.v1;

import com.heima.admin.service.AdChannelService;
import com.heima.apis.admin.AdChannelControllerApi;
import com.heima.common.exception.CostomException;
import com.heima.model.admin.dtos.ChannelDto;
import com.heima.model.admin.pojos.AdChannel;
import com.heima.model.common.dtos.ResponseResult;
import com.heima.model.common.enums.AppHttpCodeEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/channel")
public class AdChannelController implements AdChannelControllerApi {

    @Autowired
    private AdChannelService adChannelService;

    @PostMapping("/list")
    @Override
    public ResponseResult findByNameAndPage(@RequestBody ChannelDto dto) {
        return adChannelService.findByNameAndPage(dto);
    }

    @PostMapping("/save")
    @Override
    public ResponseResult insert(@RequestBody AdChannel adChannel) {
        return adChannelService.insert(adChannel);
    }

    @PostMapping("/update")
    @Override
    public ResponseResult update(@RequestBody AdChannel adChannel) {
        return adChannelService.update(adChannel);
    }

    @GetMapping("/del/{id}")
    @Override
    public ResponseResult deleteById(@PathVariable("id") Integer id) {
        if(true){
            throw new CostomException(ResponseResult.errorResult(AppHttpCodeEnum.PARAM_INVALID,"自定义异常"));
        }
        return adChannelService.deleteById(id);
    }

    /**
     * 查询所有频道
     * @return
     */
    @GetMapping("/channels")
    @Override
    public ResponseResult findAll() {
        List<AdChannel> list = adChannelService.list();
        return ResponseResult.okResult(list);
    }
}
