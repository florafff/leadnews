package com.heima.user.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(value = {"com.heima.common.knife4j","com.heima.common.exception"})
public class InitConfig {
}
