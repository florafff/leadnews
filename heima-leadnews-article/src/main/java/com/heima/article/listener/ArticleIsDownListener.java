package com.heima.article.listener;

import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.heima.article.service.ApArticleConfigService;
import com.heima.common.constants.WmNewsMessageConstants;
import com.heima.model.article.pojos.ApArticleConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class ArticleIsDownListener {

    @Autowired
    private ApArticleConfigService apArticleConfigService;


    @KafkaListener(topics = WmNewsMessageConstants.WM_NEWS_UP_OR_DOWN_TOPIC)
    public void recevieMessage(ConsumerRecord<?,?> record){
        Optional<? extends ConsumerRecord<?, ?>> optional = Optional.ofNullable(record);
        if(optional.isPresent()){
            String value = (String) record.value();
            Map map = JSON.parseObject(value, Map.class);
            // 0 下架  1 上架
            Object enable = map.get("enable");
            boolean isDown = true;
            if(enable.equals(1)){
                isDown = false;
            }
            apArticleConfigService.update(Wrappers.<ApArticleConfig>lambdaUpdate()
                    .eq(ApArticleConfig::getArticleId,map.get("articleId"))
                    .set(ApArticleConfig::getIsDown,isDown));
        }
    }
}
