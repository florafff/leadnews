package com.heima.article.stream;

import com.alibaba.fastjson.JSON;
import com.heima.article.config.stream.KafkaStreamListener;
import com.heima.common.constants.HotArticleConstants;
import com.heima.model.mess.ArticleVisitStreamMess;
import com.heima.model.mess.UpdateArticleMess;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.kstream.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
public class HotArticleStreamHandler implements KafkaStreamListener<KStream<String, String>> {
    /**
     * 接收消息
     *
     * @return
     */
    @Override
    public String listenerTopic() {
        return HotArticleConstants.HOT_ARTICLE_SCORE_TOPIC;
    }

    /**
     * 处理完成后  发送消息
     *
     * @return
     */
    @Override
    public String sendTopic() {
        return HotArticleConstants.HOT_ARTICLE_INCR_HANDLE_TOPIC;
    }

    @Override
    public KStream<String, String> getService(KStream<String, String> stream) {

        KStream<String, String> kStream = stream.flatMapValues(new ValueMapper<String, Iterable<String>>() {
            @Override
            public Iterable<String> apply(String value) {
                UpdateArticleMess mess = JSON.parseObject(value, UpdateArticleMess.class);
                System.out.println(mess);
                //文章id 类型  ---》 1233333:views
                return Arrays.asList(mess.getArticleId() + ":" + mess.getType());
            }
        }).map(new KeyValueMapper<String, String, KeyValue<String, String>>() {
            @Override
            public KeyValue<String, String> apply(String key, String value) {
                return new KeyValue<>(value, value);
            }
        }).groupByKey().windowedBy(TimeWindows.of(10000)).count(Materialized.as("count-article-num-113"))
                //key-->mess.getArticleId()+":"+mess.getType()
                //value  long类型  count之后的数值
                .toStream().map((key, value) -> {
                    return new KeyValue<>(key.key().toString(), formatObj(key.key().toString(), value.toString()));

                });
       /* kStream.foreach((x,y)->{
            System.out.println(x+"---------------"+y);
        });*/

        return kStream;
    }

    //封装消息，发送
    private String formatObj(String key, String value) {
        ArticleVisitStreamMess mess = new ArticleVisitStreamMess();
        String[] split = key.split(":");
        mess.setArticleId(Long.valueOf(split[0]));
        if (split[1].equals(UpdateArticleMess.UpdateArticleType.LIKES.name())) {
            mess.setLike(Long.valueOf(value));
        }
        if (split[1].equals(UpdateArticleMess.UpdateArticleType.VIEWS.name())) {
            mess.setView(Long.valueOf(value));
        }
        if (split[1].equals(UpdateArticleMess.UpdateArticleType.COMMENT.name())) {
            mess.setComment(Long.valueOf(value));
        }
        if (split[1].equals(UpdateArticleMess.UpdateArticleType.COLLECTION.name())) {
            mess.setCollect(Long.valueOf(value));
        }

        System.out.println(JSON.toJSONString(mess) + "------------");
        return JSON.toJSONString(mess);
    }
}
