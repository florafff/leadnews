package com.heima.admin.listener;


import com.heima.admin.service.WemediaNewsAutoScanService;
import com.heima.common.constants.NewsAutoScanConstants;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class WemediaNewsAutoListener {

    @Autowired
    private WemediaNewsAutoScanService wemediaNewsAutoScanService;

    @KafkaListener(topics = NewsAutoScanConstants.WM_NEWS_AUTO_SCAN_TOPIC)
    public void receiveMessage(ConsumerRecord<?,?> record){
        Optional<? extends ConsumerRecord<?, ?>> optional = Optional.ofNullable(record);
        if(optional.isPresent()){
            String value = (String) record.value();
            wemediaNewsAutoScanService.autoScanByMediaNewsId(Integer.valueOf(value));
        }
    }
}
