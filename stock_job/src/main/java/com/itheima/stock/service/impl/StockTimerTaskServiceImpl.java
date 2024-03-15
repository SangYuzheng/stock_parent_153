package com.itheima.stock.service.impl;

import com.google.common.collect.Lists;
import com.itheima.stock.face.StockCacheFace;
import com.itheima.stock.mapper.StockBusinessMapper;
import com.itheima.stock.mapper.StockMarketIndexInfoMapper;
import com.itheima.stock.mapper.StockRtInfoMapper;
import com.itheima.stock.pojo.entity.StockMarketIndexInfo;
import com.itheima.stock.pojo.entity.StockRtInfo;
import com.itheima.stock.pojo.vo.StockInfoConfig;
import com.itheima.stock.service.StockTimerTaskService;
import com.itheima.stock.log.utils.DateTimeUtil;
import com.itheima.stock.log.utils.IdWorker;
import com.itheima.stock.log.utils.ParseType;
import com.itheima.stock.log.utils.ParserStockInfoUtil;
import lombok.extern.slf4j.Slf4j;
import org.joda.time.DateTime;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author : itheima
 * @date : 2023/1/14 11:06
 * @description :
 */
@Service
@Slf4j
public class StockTimerTaskServiceImpl implements StockTimerTaskService {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private StockInfoConfig stockInfoConfig;

    @Autowired
    private IdWorker idWorker;

    @Autowired
    private StockMarketIndexInfoMapper stockMarketIndexInfoMapper;

    @Autowired
    private StockBusinessMapper stockBusinessMapper;

    @Autowired
    private ParserStockInfoUtil parserStockInfoUtil;

    @Autowired
    private StockRtInfoMapper stockRtInfoMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;

    /**
     * 必须保证该对象无状态
     */
    private HttpEntity<Object> httpEntity;

    @Autowired
    private StockCacheFace stockCacheFace;

    @Override
    public void getInnerMarketInfo() {
        //1.阶段1：采集原始数据
        //1.1 组装url地址
//        String url="http://hq.sinajs.cn/list=sh000001,sz399001";
        String  url=stockInfoConfig.getMarketUrl()+String.join(",",stockInfoConfig.getInner());
        //1.2 维护请求头，添加防盗链和用户标识
//        HttpHeaders headers = new HttpHeaders();
//        //防盗链
//        headers.add("Referer","https://finance.sina.com.cn/stock/");
//        //用户客户端标识
//        headers.add("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36 Edg/108.0.1462.76");
//        //维护http请求实体对象
//        HttpEntity<Object> httpEntity = new HttpEntity<>(headers);
        //发起请求
        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
        int statusCodeValue = responseEntity.getStatusCodeValue();
        if (statusCodeValue!=200) {
            //当前请求失败
            log.error("当前时间点：{},采集数据失败，http状态码：{}", DateTime.now().toString("yyyy-MM-dd HH:mm:ss"),statusCodeValue);
            //其它：发送邮件 企业微信 钉钉等给相关运营人员提醒
            return;
        }
        //获取js格式数据
        String jsData = responseEntity.getBody();
        log.info("当前时间点：{},采集原始数据内容：{}",DateTime.now().toString("yyyy-MM-dd HH:mm:ss"),jsData);
        //2.阶段2：java正则解析原始数据
        //2.1 定义正则表达式
        String reg="var hq_str_(.+)=\"(.+)\";";
        //2.2 表达式编译
        Pattern pattern = Pattern.compile(reg);
        //2.3 匹配字符串
        Matcher matcher = pattern.matcher(jsData);
        List<StockMarketIndexInfo> entities=new ArrayList<>();
        while (matcher.find()){
            //1.获取大盘的编码
            String marketCode = matcher.group(1);
            //2.获取其它信息
            String otherInfo = matcher.group(2);
            //将other字符串以逗号切割，获取大片的详情信息
            String[] splitArr = otherInfo.split(",");
            //大盘名称
            String marketName=splitArr[0];
            //获取当前大盘的开盘点数
            BigDecimal openPoint=new BigDecimal(splitArr[1]);
            //前收盘点
            BigDecimal preClosePoint=new BigDecimal(splitArr[2]);
            //获取大盘的当前点数
            BigDecimal curPoint=new BigDecimal(splitArr[3]);
            //获取大盘最高点
            BigDecimal maxPoint=new BigDecimal(splitArr[4]);
            //获取大盘的最低点
            BigDecimal minPoint=new BigDecimal(splitArr[5]);
            //获取成交量
            Long tradeAmt=Long.valueOf(splitArr[8]);
            //获取成交金额
            BigDecimal tradeVol=new BigDecimal(splitArr[9]);
            //时间
            Date curTime = DateTimeUtil.getDateTimeWithoutSecond(splitArr[30] + " " + splitArr[31]).toDate();
            //3.阶段3：解析数据封装entity
            StockMarketIndexInfo entity = StockMarketIndexInfo.builder()
                    .id(idWorker.nextId())
                    .marketName(marketName)
                    .openPoint(openPoint)
                    .preClosePoint(preClosePoint)
                    .curPoint(curPoint)
                    .maxPoint(maxPoint)
                    .minPoint(minPoint)
                    .tradeAmount(tradeAmt)
                    .tradeVolume(tradeVol)
                    .marketCode(marketCode)
                    .curTime(curTime)
                    .build();
            entities.add(entity);
        }
        log.info("解析大盘数据完毕！");
        //4.阶段4：调用mybatis批量入库
        int count=stockMarketIndexInfoMapper.insertBatch(entities);
        if (count>0) {
            //大盘数据采集完毕后，通知backend工程刷新缓存
            //发送日期对象，接收方通过接收的日期与当前日期比对，能判断出数据延迟的时长，用于运维通知处理
            rabbitTemplate.convertAndSend("stockExchange","inner.market",new Date());
            log.info("当前时间：{},插入大盘数据：{}成功",DateTime.now().toString("yyyy-MM-dd HH:mm:ss"),entities);
        }else{
            log.error("当前时间：{},插入大盘数据：{}失败",DateTime.now().toString("yyyy-MM-dd HH:mm:ss"),entities);
        }
    }

    /**
     * 定义获取分钟级股票数据
     */
    @Override
    public void getStockRtIndex() {
        //1.获取所有个股的集合 3000+
        List<String> allCodes=stockCacheFace.getAllStockCodeWithPrefix();
//        List<String> allCodes = stockBusinessMapper.getAllStockCodes();
//        //添加大盘业务前缀 sh sz
//        allCodes=allCodes.stream().map(code->code.startsWith("6")?"sh"+code:"sz"+code).collect(Collectors.toList());
        //一次性将所有集合拼接到url地址中，导致地址过长，参数过多
        // String url=  stockInfoConfig.getMarketUrl()+ String.join(",",allCodes);
        long startTime = System.currentTimeMillis();
        //核心思路：将大的集合切分成若干小集合，分批次拉取数据
        Lists.partition(allCodes, 15).forEach(codes->{
            //原始方案
           //分批次采集
//            String url=stockInfoConfig.getMarketUrl()+String.join(",",codes);
//            //维护请求头，添加防盗链和用户标识
////            HttpHeaders headers = new HttpHeaders();
////            //防盗链
////            headers.add("Referer","https://finance.sina.com.cn/stock/");
////            //用户客户端标识
////            headers.add("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36 Edg/108.0.1462.76");
////            //维护http请求实体对象
////            HttpEntity<Object> httpEntity = new HttpEntity<>(headers);
//            //发起请求
//            ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
//            int statusCodeValue = responseEntity.getStatusCodeValue();
//            if (statusCodeValue!=200) {
//                //当前请求失败
//                log.error("当前时间点：{},采集数据失败，http状态码：{}", DateTime.now().toString("yyyy-MM-dd HH:mm:ss"),statusCodeValue);
//                //其它：发送邮件 企业微信 钉钉等给相关运营人员提醒
//                return;
//            }
//            //获取原始js格式数据
//            String jsData = responseEntity.getBody();
//            //调用工具类解析获取各个数据
//            List<StockRtInfo> list = parserStockInfoUtil.parser4StockOrMarketInfo(jsData, ParseType.ASHARE);
//            log.info("采集个股数据：{}",list);
//            //批量保存采集的个股数据
//            int count=stockRtInfoMapper.insertBatch(list);
//            if (count>0) {
//                log.info("当前时间：{},插入个股数据：{}成功",DateTime.now().toString("yyyy-MM-dd HH:mm:ss"),list);
//            }else{
//                log.error("当前时间：{},插入个股数据：{}失败",DateTime.now().toString("yyyy-MM-dd HH:mm:ss"),list);
//            }
            //方案1：原始方案采集个股数据时将集合分片，然后分批次串行采集数据，效率不高，存在较高的采集延迟：引入多线程
            //代码的问题：1.每次来任务，就创建换一个线程，复用性差 2.如果多线程使用不当，造成CPU竞争激烈，导致频繁的上下文切换，导致程序性能降低
//            new Thread(()->{
//                //分批次采集
//                String url=stockInfoConfig.getMarketUrl()+String.join(",",codes);
//                //维护请求头，添加防盗链和用户标识
////            HttpHeaders headers = new HttpHeaders();
////            //防盗链
////            headers.add("Referer","https://finance.sina.com.cn/stock/");
////            //用户客户端标识
////            headers.add("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36 Edg/108.0.1462.76");
////            //维护http请求实体对象
////            HttpEntity<Object> httpEntity = new HttpEntity<>(headers);
//                //发起请求
//                ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
//                int statusCodeValue = responseEntity.getStatusCodeValue();
//                if (statusCodeValue!=200) {
//                    //当前请求失败
//                    log.error("当前时间点：{},采集数据失败，http状态码：{}", DateTime.now().toString("yyyy-MM-dd HH:mm:ss"),statusCodeValue);
//                    //其它：发送邮件 企业微信 钉钉等给相关运营人员提醒
//                    return;
//                }
//                //获取原始js格式数据
//                String jsData = responseEntity.getBody();
//                //调用工具类解析获取各个数据
//                List<StockRtInfo> list = parserStockInfoUtil.parser4StockOrMarketInfo(jsData, ParseType.ASHARE);
//                log.info("采集个股数据：{}",list);
//                //批量保存采集的个股数据
//                int count=stockRtInfoMapper.insertBatch(list);
//                if (count>0) {
//                    log.info("当前时间：{},插入个股数据：{}成功",DateTime.now().toString("yyyy-MM-dd HH:mm:ss"),list);
//                }else{
//                    log.error("当前时间：{},插入个股数据：{}失败",DateTime.now().toString("yyyy-MM-dd HH:mm:ss"),list);
//                }
//            }).start();
            //方案2：引入线程池
            threadPoolTaskExecutor.execute(()->{
                //分批次采集
                String url=stockInfoConfig.getMarketUrl()+String.join(",",codes);
                //维护请求头，添加防盗链和用户标识
//            HttpHeaders headers = new HttpHeaders();
//            //防盗链
//            headers.add("Referer","https://finance.sina.com.cn/stock/");
//            //用户客户端标识
//            headers.add("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36 Edg/108.0.1462.76");
//            //维护http请求实体对象
//            HttpEntity<Object> httpEntity = new HttpEntity<>(headers);
                //发起请求
                ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class);
                int statusCodeValue = responseEntity.getStatusCodeValue();
                if (statusCodeValue!=200) {
                    //当前请求失败
                    log.error("当前时间点：{},采集数据失败，http状态码：{}", DateTime.now().toString("yyyy-MM-dd HH:mm:ss"),statusCodeValue);
                    //其它：发送邮件 企业微信 钉钉等给相关运营人员提醒
                    return;
                }
                //获取原始js格式数据
                String jsData = responseEntity.getBody();
                //调用工具类解析获取各个数据
                List<StockRtInfo> list = parserStockInfoUtil.parser4StockOrMarketInfo(jsData, ParseType.ASHARE);
                log.info("采集个股数据：{}",list);
                //批量保存采集的个股数据
                int count=stockRtInfoMapper.insertBatch(list);
                if (count>0) {
                    log.info("当前时间：{},插入个股数据：{}成功",DateTime.now().toString("yyyy-MM-dd HH:mm:ss"),list);
                }else{
                    log.error("当前时间：{},插入个股数据：{}失败",DateTime.now().toString("yyyy-MM-dd HH:mm:ss"),list);
                }
            });
        });
        long takeTime=System.currentTimeMillis()- startTime;
        log.info("本地采集花费时间：{}ms",takeTime);
    }

    /**
     * bean生命周期的初始化回调方法
     */
    @PostConstruct
    //@PreDestroy
    public void initData(){
        //维护请求头，添加防盗链和用户标识
        HttpHeaders headers = new HttpHeaders();
        //防盗链
        headers.add("Referer","https://finance.sina.com.cn/stock/");
        //用户客户端标识
        headers.add("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36 Edg/108.0.1462.76");
        //维护http请求实体对象
        httpEntity = new HttpEntity<>(headers);
    }
}
