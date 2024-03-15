package com.itheima.stock.service;

import com.itheima.stock.pojo.domain.InnerMarketDomain;
import com.itheima.stock.pojo.domain.Stock4EvrDayDomain;
import com.itheima.stock.pojo.domain.Stock4MinuteDomain;
import com.itheima.stock.pojo.domain.StockUpdownDomain;
import com.itheima.stock.vo.resp.PageResult;
import com.itheima.stock.vo.resp.R;

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @author : itheima
 * @date : 2023/1/11 10:09
 * @description  股票服务接口
 */
public interface StockService {
    /**
     * 获取国内大盘最新的数据
     * @return
     */
    R<List<InnerMarketDomain>> getInnerMarketInfo();

    /**
     * 分页查询最新的股票交易数据
     * @param page 当前页
     * @param pageSize 每页大小
     * @return
     */
    R<PageResult<StockUpdownDomain>> getStockInfoByPage(Integer page, Integer pageSize);

    /**
     * 统计最新股票交易日内每分钟的涨跌停的股票数量
     * @return
     */
    R<Map<String, List>> getStockUpDownCount();

    /**
     * 导出指定页码的最新股票信息
     * @param page 当前页
     * @param pageSize 每页大小
     * @param response
     */
    void exportStockUpDownInfo(Integer page, Integer pageSize, HttpServletResponse response);

    /**
     * 统计大盘T日和T-1日每分钟交易量的统计
     * @return
     */
    R<Map<String, List>> getComparedStockTradeAmt();

    /**
     * 统计最新交易时间点下股票（A股）在各个涨幅区间的数量
     * @return
     */
    R<Map> getIncreaseRangeInfo();

    /**
     * 获取指定股票T日的分时数据
     * @param stockCode 股票编码
     * @return
     */
    R<List<Stock4MinuteDomain>> getStockScreenTimeSharing(String stockCode);

    /**
     * 统计指定股票的日K线数据 [备份]
     * @param stockCode 股票编码
     * @return
     */
    R<List<Stock4EvrDayDomain>> getStockScreenDkLine(String stockCode);
    /**
     * 统计指定股票的日K线数据
     * @param stockCode 股票编码
     * @return
     */
    public R<List<Stock4EvrDayDomain>> getStockDKLine(String stockCode);
}
