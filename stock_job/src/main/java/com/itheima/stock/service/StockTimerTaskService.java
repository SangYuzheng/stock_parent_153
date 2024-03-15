package com.itheima.stock.service;

/**
 * @author : itheima
 * @date : 2023/1/14 11:04
 * @description  定义股票采集数据服务接口
 */
public interface StockTimerTaskService {
    /**
     * 获取国内大盘的实时数据信息
     */
    void getInnerMarketInfo();

    /**
     * 定义获取分钟级股票数据
     */
    void getStockRtIndex();
}
