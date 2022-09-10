package com.heima.wemedia.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(value = {"com.heima.common.knife4j","com.heima.common.exception","com.heima.common.fastdfs"})
public class InitConfig {
}
