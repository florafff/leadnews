package com.heima.admin.job;


import com.heima.admin.feign.WemediaFeign;
import com.heima.admin.service.WemediaNewsAutoScanService;
import com.xxl.job.core.biz.model.ReturnT;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.joda.time.chrono.IslamicChronology;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class WeMediaNewsAutoScanJob {

    @Autowired
    private WemediaFeign wemediaFeign;

    @Autowired
    private WemediaNewsAutoScanService wemediaNewsAutoScanService;


    @XxlJob("wemediaAutoScanJob")
    public ReturnT<String> autoScanJob(String param) throws Exception {
        //执行任务？

        //1.查询符合条件的内容
        List<Integer> idList = wemediaFeign.findRelease();
        if(idList != null && !idList.isEmpty()){
            for (Integer id : idList) {
                //2.审核
                wemediaNewsAutoScanService.autoScanByMediaNewsId(id);
            }
        }


        return ReturnT.SUCCESS;
    }
}
