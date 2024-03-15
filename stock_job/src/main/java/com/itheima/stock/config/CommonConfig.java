package com.itheima.stock.config;

import com.itheima.stock.pojo.vo.StockInfoConfig;
import com.itheima.stock.log.utils.IdWorker;
import com.itheima.stock.log.utils.ParserStockInfoUtil;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author : itheima
 * @date : 2023/1/8 17:47
 * @description : 定义公共配置bean
 */
@Configuration
@EnableConfigurationProperties({StockInfoConfig.class})//开启对象相关配置对象的加载
public class CommonConfig {
    /**
     * 基于雪花算法保证生成的id唯一
     * @return
     */
    @Bean
    public IdWorker idWorker(){
        /*
          参数1：机器ID
          参数2：机房ID
          机房和机器编号一般由运维人员进行唯一性规划
         */
        return new IdWorker(1l,2l);
    }

    /**
     * 定义解析股票 大盘 外盘 个股 板块相关信息的工具类bean
     * @return
     */
    @Bean
    public ParserStockInfoUtil parserStockInfoUtil(){
        return new ParserStockInfoUtil(idWorker());
    }


}
