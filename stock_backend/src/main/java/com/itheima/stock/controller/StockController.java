package com.itheima.stock.controller;

import com.itheima.stock.pojo.domain.InnerMarketDomain;
import com.itheima.stock.pojo.domain.Stock4EvrDayDomain;
import com.itheima.stock.pojo.domain.Stock4MinuteDomain;
import com.itheima.stock.pojo.domain.StockUpdownDomain;
import com.itheima.stock.service.StockService;
import com.itheima.stock.vo.resp.PageResult;
import com.itheima.stock.vo.resp.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

/**
 * @author : itheima
 * @date : 2023/1/11 10:05
 * @description  定义股票相关接口控制器
 */
@Api(value = "/api/quot", tags = {"定义股票相关接口控制器"})
@RestController
@RequestMapping("/api/quot")
public class StockController {

//    @Autowired
    @Resource
    private StockService stockService;


    /**
     * 获取国内大盘最新的数据
     * @return
     */
    @ApiOperation(value = "获取国内大盘最新的数据", notes = "获取国内大盘最新的数据", httpMethod = "GET")
    @GetMapping("/index/all")
    public R<List<InnerMarketDomain>> getInnerMarketInfo(){
        return stockService.getInnerMarketInfo();
    }

    /**
     * 分页查询最新的股票交易数据
     * @param page 当前页
     * @param pageSize 每页大小
     * @return
     */
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", dataType = "int", name = "page", value = "当前页"),
            @ApiImplicitParam(paramType = "query", dataType = "int", name = "pageSize", value = "每页大小")
    })
    @ApiOperation(value = "分页查询最新的股票交易数据", notes = "分页查询最新的股票交易数据", httpMethod = "GET")
    @GetMapping("/stock/all")
    public R<PageResult<StockUpdownDomain>> getStockInfoByPage(@RequestParam(value = "page",required = false,defaultValue = "1") Integer page,
                                                               @RequestParam(value = "pageSize",required = false,defaultValue = "20") Integer pageSize){
        return stockService.getStockInfoByPage(page,pageSize);
    }

    /**
     * 统计最新股票交易日内每分钟的涨跌停的股票数量
     * @return
     */
    @ApiOperation(value = "统计最新股票交易日内每分钟的涨跌停的股票数量", notes = "统计最新股票交易日内每分钟的涨跌停的股票数量", httpMethod = "GET")
    @GetMapping("/stock/updown/count")
    public R<Map<String,List>> getStockUpDownCount(){
        return stockService.getStockUpDownCount();
    }

    /**
     * 导出指定页码的最新股票信息
     * @param page 当前页
     * @param pageSize 每页大小
     * @param response
     */
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", dataType = "int", name = "page", value = "当前页"),
            @ApiImplicitParam(paramType = "query", dataType = "int", name = "pageSize", value = "每页大小")
    })
    @ApiOperation(value = "导出指定页码的最新股票信息", notes = "导出指定页码的最新股票信息", httpMethod = "GET")
    @GetMapping("/stock/export")
    public void exportStockUpDownInfo(@RequestParam(value = "page",required = false,defaultValue = "1") Integer page,
                                      @RequestParam(value = "pageSize",required = false,defaultValue = "20") Integer pageSize,
                                      HttpServletResponse response){
         stockService.exportStockUpDownInfo(page,pageSize,response);
    }


    /**
     * 统计大盘T日和T-1日每分钟交易量的统计
     * @return
     */
    @ApiOperation(value = "统计大盘T日和T-1日每分钟交易量的统计", notes = "统计大盘T日和T-1日每分钟交易量的统计", httpMethod = "GET")
    @GetMapping("/stock/tradeAmt")
    public R<Map<String,List>> getComparedStockTradeAmt(){
        return stockService.getComparedStockTradeAmt();
    }

    /**
     * 统计最新交易时间点下股票（A股）在各个涨幅区间的数量
     * @return
     */
    @ApiOperation(value = "统计最新交易时间点下股票（A股）在各个涨幅区间的数量", notes = "统计最新交易时间点下股票（A股）在各个涨幅区间的数量", httpMethod = "GET")
    @GetMapping("/stock/updown")
    public R<Map> getIncreaseRangeInfo(){
        return stockService.getIncreaseRangeInfo();
    }

    /**
     * 获取指定股票T日的分时数据
     * @param stockCode 股票编码
     * @return
     */
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", dataType = "string", name = "stockCode", value = "股票编码", required = true)
    })
    @ApiOperation(value = "获取指定股票T日的分时数据", notes = "获取指定股票T日的分时数据", httpMethod = "GET")
    @GetMapping("/stock/screen/time-sharing")
    public R<List<Stock4MinuteDomain>> getStockScreenTimeSharing(@RequestParam(value = "code",required = true) String stockCode){
        return stockService.getStockScreenTimeSharing(stockCode);
    }


    /**
     * 统计指定股票的日K线数据
     * @param stockCode 股票编码
     * @return
     */
    @ApiImplicitParams({
            @ApiImplicitParam(paramType = "query", dataType = "string", name = "stockCode", value = "股票编码", required = true)
    })
    @ApiOperation(value = "统计指定股票的日K线数据", notes = "统计指定股票的日K线数据", httpMethod = "GET")
    @GetMapping("/stock/screen/dkline")
    public R<List<Stock4EvrDayDomain>> getStockScreenDkLine(@RequestParam(value = "code",required = true) String stockCode){
//        return stockService.getStockScreenDkLine(stockCode);
        return stockService.getStockDKLine(stockCode);
    }

}
