package com.itheima.stock.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author : itheima
 * @date : 2023/1/14 16:14
 * @description  定义rabbitmq相关配置
 */
@Configuration
public class MqConfig {
    /**
     * 重新定义消息序列化的方式，改为基于json格式序列化和反序列化
     * @return
     */
    @Bean
    public MessageConverter messageConverter(){
        return new Jackson2JsonMessageConverter();
    }

    /**
     * 股票相关信息的主题交换机
     * @return
     */
    @Bean
    public TopicExchange stockTopicExchange(){
        return new TopicExchange("stockExchange",true,false);
    }

    /**
     * 定义队列
     * @return
     */
    @Bean
    public Queue innerMarketQueue(){
        return new Queue("innerMarketQueue",true);
    }

    /**
     * 绑定国内大盘队列到股票主题交换机
     * @return
     */
    @Bean
    public Binding bindingInnerMarketQueue(){
        return BindingBuilder.bind(innerMarketQueue()).to(stockTopicExchange()).with("inner.market");
    }

}
