package com.itheima.stock.service.impl;

import com.alibaba.excel.EasyExcel;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.itheima.stock.mapper.StockMarketIndexInfoMapper;
import com.itheima.stock.mapper.StockRtInfoMapper;
import com.itheima.stock.pojo.domain.InnerMarketDomain;
import com.itheima.stock.pojo.domain.Stock4EvrDayDomain;
import com.itheima.stock.pojo.domain.Stock4MinuteDomain;
import com.itheima.stock.pojo.domain.StockUpdownDomain;
import com.itheima.stock.pojo.vo.StockInfoConfig;
import com.itheima.stock.service.StockService;
import com.itheima.stock.log.utils.DateTimeUtil;
import com.itheima.stock.vo.resp.PageResult;
import com.itheima.stock.vo.resp.R;
import com.itheima.stock.vo.resp.ResponseCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author : itheima
 * @date : 2023/1/11 10:10
 * @description  股票服务实现
 */
@Service("stockService")
@Slf4j
public class StockServiceImpl implements StockService {

    @Autowired
    private StockInfoConfig stockInfoConfig;

    @Autowired
    private StockMarketIndexInfoMapper stockMarketIndexInfoMapper;

    @Autowired
    private StockRtInfoMapper stockRtInfoMapper;

    /**
     * 注入本地缓存bean
     */
    @Autowired
    private Cache<String ,Object> caffeineCache;

    /**
     * 获取国内大盘最新的数据
     * @return
     */
    @Override
    public R<List<InnerMarketDomain>> getInnerMarketInfo() {
        //默认从本地缓存加载数据，如果不存在则从数据库加载并同步到本地缓存
        //在开盘周期内，本地缓存默认有效期1分钟
        R<List<InnerMarketDomain>>  result= (R<List<InnerMarketDomain>>) caffeineCache.get("innerMarketKey", key->{
            //1.获取股票最新的交易时间点（精确到分钟，秒和毫秒置为0）
            Date curDate = DateTimeUtil.getLastDate4Stock(DateTime.now()).toDate();
            //mock data 等后续完成股票采集job工程，再将代码删除即可
            curDate=DateTime.parse("2022-12-19 15:00:00", DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")).toDate();
            //2.获取大盘编码集合
            List<String> mCodes = stockInfoConfig.getInner();
            //3.调用mapper查询数据
            List<InnerMarketDomain> data=stockMarketIndexInfoMapper.getMarketInfo(curDate,mCodes);
            //4.封装并响应
            return R.ok(data);
        });
        return result;
    }

    /**
     * 分页查询最新的股票交易数据
     * @param page 当前页
     * @param pageSize 每页大小
     * @return
     */
    @Override
    public R<PageResult<StockUpdownDomain>> getStockInfoByPage(Integer page, Integer pageSize) {
        //1.获取股票最新的交易时间点（精确到分钟，秒和毫秒置为0）
        Date curDate = DateTimeUtil.getLastDate4Stock(DateTime.now()).toDate();
        //mock data 等后续完成股票采集job工程，再将代码删除即可
        curDate=DateTime.parse("2021-12-30 09:42:00", DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")).toDate();
        //2.设置PageHelper分页参数
        PageHelper.startPage(page,pageSize);
        //3.调用mapper查询
        List<StockUpdownDomain> pageData=stockRtInfoMapper.getStockInfoByTime(curDate);
        //4.组装PageResult对象
        PageInfo<StockUpdownDomain> pageInfo = new PageInfo<>(pageData);
        PageResult<StockUpdownDomain> pageResult = new PageResult<>(pageInfo);
        //5.响应数据
        return R.ok(pageResult);
    }

    /**
     * 统计最新股票交易日内每分钟的涨跌停的股票数量
     * @return
     */
    @Override
    public R<Map<String, List>> getStockUpDownCount() {
        //1.获取最新股票的交易时间点（截止时间）
        DateTime curDateTime = DateTimeUtil.getLastDate4Stock(DateTime.now());
        //假数据
        curDateTime=DateTime.parse("2022-01-06 14:25:00", DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
        Date endDate = curDateTime.toDate();
        //2.获取最新交易时间点对应的开盘时间点
        Date startDate = DateTimeUtil.getOpenDate(curDateTime).toDate();
        //3.统计涨停数据
        List<Map> upList=stockRtInfoMapper.getStockUpdownCount(startDate,endDate,1);
        //4.统计跌停
        List<Map> downList=stockRtInfoMapper.getStockUpdownCount(startDate,endDate,0);
        //5.组装数据
        HashMap<String, List> info = new HashMap<>();
        info.put("upList",upList);
        info.put("downList",downList);
        //6.响应数据
        return R.ok(info);
    }

    /**
     * 导出指定页码的最新股票信息
     * @param page 当前页
     * @param pageSize 每页大小
     * @param response
     */
    @Override
    public void exportStockUpDownInfo(Integer page, Integer pageSize, HttpServletResponse response) {
        //1.获取分页数据
        R<PageResult<StockUpdownDomain>> r = this.getStockInfoByPage(page, pageSize);
        List<StockUpdownDomain> rows = r.getData().getRows();
        //2.将数据导出到excel中
        // 这里注意 有同学反应使用swagger 会导致各种问题，请直接用浏览器或者用postman
        response.setContentType("application/vnd.ms-excel");
        response.setCharacterEncoding("utf-8");
        // 这里URLEncoder.encode可以防止中文乱码 当然和easyexcel没有关系（默认名称）
        try {
            String fileName = URLEncoder.encode("股票信息表", "UTF-8");
            response.setHeader("Content-disposition", "attachment;filename=" + fileName + ".xlsx");
            EasyExcel.write(response.getOutputStream(), StockUpdownDomain.class).sheet("股票涨幅信息").doWrite(rows);
        } catch (IOException e) {
            log.error("当前页码：{},每页大小：{},当前时间：{},异常信息：{}",page,pageSize,DateTime.now().toString("yyyy-MM-dd HH:mm:ss"),e.getMessage());
            //通知前端异常，稍后重试
            response.setContentType("application/json");
            response.setCharacterEncoding("utf-8");
            R<Object> error = R.error(ResponseCode.ERROR);
            try {
                String jsonData = new ObjectMapper().writeValueAsString(error);
                response.getWriter().write(jsonData);
            } catch (IOException ioException) {
                log.error("exportStockUpDownInfo:响应错误信息失败,时间：{}",DateTime.now().toString("yyyy-MM-dd HH:mm:ss"));
            }
        }
    }

    /**
     * 统计大盘T日和T-1日每分钟交易量的统计
     * @return
     */
    @Override
    public R<Map<String, List>> getComparedStockTradeAmt() {
        //1.获取T日（最新股票交易日的日期范围）
        DateTime tEndDateTime = DateTimeUtil.getLastDate4Stock(DateTime.now());
        //mock data
        tEndDateTime=DateTime.parse("2022-01-03 14:40:00", DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
        Date tEndDate = tEndDateTime.toDate();
        //开盘时间
        Date tStartDate = DateTimeUtil.getOpenDate(tEndDateTime).toDate();
        //2.获取T-1日的时间范围
        DateTime preTEndDateTime = DateTimeUtil.getPreviousTradingDay(tEndDateTime);
        //mock data
        preTEndDateTime=DateTime.parse("2022-01-02 14:40:00", DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
        Date preTEndDate = preTEndDateTime.toDate();
        //开盘时间
        Date tPreStartDate = DateTimeUtil.getOpenDate(preTEndDateTime).toDate();
        //3.调用mapper查询
        //3.1 统计T日
        List<Map> tData=stockMarketIndexInfoMapper.getSumAmtInfo(tStartDate,tEndDate,stockInfoConfig.getInner());
        //3.2 统计T-1日
        List<Map> preTData=stockMarketIndexInfoMapper.getSumAmtInfo(tPreStartDate,preTEndDate,stockInfoConfig.getInner());
        //4.组装数据
        HashMap<String, List> info = new HashMap<>();
        info.put("amtList",tData);
        info.put("yesAmtList",preTData);
        //5.响应数据
        return R.ok(info);
    }

    /**
     * 统计最新交易时间点下股票（A股）在各个涨幅区间的数量
     * @return
     */
    @Override
    public R<Map> getIncreaseRangeInfo() {
        //1.获取当前最新的股票交易时间点
        DateTime curDateTime = DateTimeUtil.getLastDate4Stock(DateTime.now());
        //mock data
        curDateTime=DateTime.parse("2022-01-06 09:55:00", DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
        Date curDate = curDateTime.toDate();
        //2.调用mapper获取数据
        List<Map> infos=stockRtInfoMapper.getIncreaseRangeInfoByDate(curDate);
        //获取有序的涨幅区间标题集合
        List<String> upDownRange = stockInfoConfig.getUpDownRange();
        //将顺序的涨幅区间内的每个元素转化成Map对象即可
        //方式1:普通循环
//        List<Map> allInfos=new ArrayList<>();
//        for (String title : upDownRange) {
//            Map tmp=null;
//            for (Map info : infos) {
//                if (info.containsValue(title)) {
//                    tmp=info;
//                    break;
//                }
//            }
//            if (tmp==null) {
//                //不存在，则补齐
//                tmp=new HashMap();
//                tmp.put("count",0);
//                tmp.put("title",title);
//            }
//            allInfos.add(tmp);
//        }
        //方式2：stream遍历获取
        List<Map> allInfos = upDownRange.stream().map(title -> {
            Optional<Map> result = infos.stream().filter(map -> map.containsValue(title)).findFirst();
            if (result.isPresent()) {
                return result.get();
            } else {
                HashMap<String, Object> tmp = new HashMap<>();
                tmp.put("count", 0);
                tmp.put("title", title);
                return tmp;
            }
        }).collect(Collectors.toList());

        //3.组装数据
        HashMap<String, Object> data = new HashMap<>();
        data.put("time",curDateTime.toString("yyyy-MM-dd HH:mm:ss"));
//        data.put("infos",infos);
        data.put("infos",allInfos);
        //4.响应
        return R.ok(data);
    }

    /**
     * 获取指定股票T日的分时数据
     * @param stockCode 股票编码
     * @return
     */
    @Override
    public R<List<Stock4MinuteDomain>> getStockScreenTimeSharing(String stockCode) {
        //1.获取T日最新股票交易时间点 endTime openTime
        DateTime endDateTime = DateTimeUtil.getLastDate4Stock(DateTime.now());
        endDateTime=DateTime.parse("2021-12-30 14:30:00", DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
        Date endDate = endDateTime.toDate();
        Date openDate = DateTimeUtil.getOpenDate(endDateTime).toDate();
        //2.查询
        List<Stock4MinuteDomain> data=stockRtInfoMapper.getStock4MinuteInfo(openDate,endDate,stockCode);
        //3.返回
        return R.ok(data);
    }

    /**
     * 统计指定股票的日K线数据
     * @param stockCode 股票编码
     * @return
     */
    @Override
    public R<List<Stock4EvrDayDomain>> getStockScreenDkLine(String stockCode) {
        //1.获取统计日k线的数据的时间范围
        //1.1 获取截止时间
        DateTime endDateTime = DateTimeUtil.getLastDate4Stock(DateTime.now());
        endDateTime=DateTime.parse("2022-06-06 14:25:00", DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
        Date endDate = endDateTime.toDate();
        //1.2 起始时间
        DateTime startdateTime = endDateTime.minusMonths(3);
        startdateTime=DateTime.parse("2022-01-01 09:30:00", DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
        Date startDate = startdateTime.toDate();
        //2.调用mapper获取指定日期范围内的日K线数据
        List<Stock4EvrDayDomain> dkLineData=stockRtInfoMapper.getStock4DkLine(startDate,endDate,stockCode);
        //3.返回
        return R.ok(dkLineData);
    }

    /**
     * 日k线功能实现
     * @param stockCode
     * @return
     */
    @Override
    public R<List<Stock4EvrDayDomain>> getStockDKLine(String stockCode) {
        if (StringUtils.isBlank(stockCode)) {
            return R.error(ResponseCode.DATA_ERROR);
        }
        //1.获取指定时间范围内的数据
        //1.1 获取截止时间
        DateTime endDateTime = DateTimeUtil.getLastDate4Stock(DateTime.now());
        endDateTime=DateTime.parse("2022-06-06 14:25:00", DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
        Date endDate = endDateTime.toDate();
        //1.2 起始时间
        DateTime startdateTime = endDateTime.minusMonths(3);
        startdateTime=DateTime.parse("2022-01-01 09:30:00", DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss"));
        Date startDate = startdateTime.toDate();
        //2.调用mapper接口查询数据
//        List<Stock4EvrDayDomain>  infos= stockRtInfoMapper.getStockScreenDkLine(stockCode,startTime,endTime);
        //方案2：分步实现
        List<Date> mxTimes=stockRtInfoMapper.getMxTime4EvryDay(stockCode,startDate,endDate);
        List<Stock4EvrDayDomain> infos=   stockRtInfoMapper.getStockScreenDkLine2(stockCode,mxTimes);

        if (CollectionUtils.isEmpty(infos)) {
            return R.error(ResponseCode.NO_RESPONSE_DATA);
        }
        //3.组装数据，并响应
        return R.ok(infos);
    }

}
