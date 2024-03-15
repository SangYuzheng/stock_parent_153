package com.itheima.stock.job;

import com.itheima.stock.service.StockTimerTaskService;
import com.xxl.job.core.handler.annotation.XxlJob;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author : itheima
 * @date : 2023/2/1 11:37
 * @description  定义xxljob任务执行器bean
 */
@Component
public class StockJob {


    @Autowired
    private StockTimerTaskService stockTimerTaskService;


    /**
     * 1、简单任务示例（Bean模式）
     */
    @XxlJob("myJobHandler")
    public void demoJobHandler() throws Exception {
        System.out.println("当前时间点为："+ DateTime.now().toString("yyyy-MM-dd HH:mm:ss"));
    }

    /**
     * 定时采集A股大盘数据
     * 建议：针对不同的股票数据，定义不同的采集任务，解耦，方便维护
     */
    @XxlJob("getInnerMarketInfo")
    public void getStockInfos(){
        stockTimerTaskService.getInnerMarketInfo();
    }

    /**
     * 定时采集A股大个股数据
     */
    @XxlJob("getStockRtIndex")
    public void getStockRtIndex(){
        stockTimerTaskService.getStockRtIndex();
    }




}
