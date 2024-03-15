package com.itheima.stock.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * @author : itheima
 * @date : 2023/1/14 9:01
 * @description : 定义http客户端工具bean
 */
@Configuration
public class HttpClientConfig {

    /**
     * 定义http客户端bean
     * @return
     */
    @Bean
    public RestTemplate  restTemplate(){
        return new RestTemplate();
    }

}
