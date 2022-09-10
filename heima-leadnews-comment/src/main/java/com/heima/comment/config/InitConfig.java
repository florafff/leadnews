package com.heima.comment.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(value = {"com.heima.common.knife4j","com.heima.common.exception",
        "com.heima.common.jackson","com.heima.common.mongo","com.heima.common.threadpool"})
public class InitConfig {
}
