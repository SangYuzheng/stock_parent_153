package com.itheima.stock;

import com.itheima.stock.pojo.Account;
import com.itheima.stock.service.StockTimerTaskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * @author : itheima
 * @date : 2023/1/14 9:11
 * @description :
 */
@SpringBootTest
public class TestRestTemplate {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private StockTimerTaskService stockTimerTaskService;

    /**
     * @desc 测试get
     */
    @Test
    public void test01() {
        String url="http://localhost:6766/account/getByUserNameAndAddress?userName=zhangsan&address=sh";
        ResponseEntity<String> resp = restTemplate.getForEntity(url, String.class);
        //获取响应头
        HttpHeaders headers = resp.getHeaders();
        System.out.println(headers);
        //获取响应的状态码 200
        int statusCode = resp.getStatusCodeValue();
        System.out.println(statusCode);
        //获取响应的数据
        String respData = resp.getBody();
        System.out.println(respData);
    }

    /**
     * @desc 测试get
     */
    @Test
    public void test02() {
        String url="http://localhost:6766/account/getByUserNameAndAddress?userName=zhangsan&address=sh";
//        ResponseEntity<Account> resp = restTemplate.getForEntity(url, Account.class);
        Account account = restTemplate.getForObject(url, Account.class);
        System.out.println(account);
    }

    /**
     * 请求头设置参数，访问指定接口
     */
    @Test
    public void test03(){
        String url="http://localhost:6766/account/getHeader";
        //设置请求头参数
        HttpHeaders headers = new HttpHeaders();
        headers.add("userName","zhangsan");
        //请求头填充到请求对象下
        HttpEntity<Map> entry = new HttpEntity<>(headers);
        //发送请求
        ResponseEntity<String> responseEntity = restTemplate.exchange(url, HttpMethod.GET, entry, String.class);
        String result = responseEntity.getBody();
        System.out.println(result);
    }

    /**
     * post模拟form表单提交数据
     */
    @Test
    public void test04(){
        String url="http://localhost:6766/account/addAccount";
        //设置请求头，指定请求数据方式
        HttpHeaders headers = new HttpHeaders();
        //告知被调用方，请求方式是form表单提交，这样对方解析数据时，就会按照form表单的方式解析处理
        headers.add("Content-type","application/x-www-form-urlencoded");
        //组装模拟form表单提交数据，内部元素相当于form表单的input框
        LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<>();
        map.add("id","10");
        map.add("userName","itheima");
        map.add("address","shanghai");
        HttpEntity<LinkedMultiValueMap<String, Object>> httpEntity = new HttpEntity<>(map, headers);
        /*
            参数1：请求url地址
            参数2：请求方式 POST
            参数3：请求体对象，携带了请求头和请求体相关的参数
            参数4：响应数据类型
         */
        ResponseEntity<Account> exchange = restTemplate.exchange(url, HttpMethod.POST, httpEntity, Account.class);
        Account body = exchange.getBody();
        System.out.println(body);
    }

    /**
     * post发送json数据
     */
    @Test
    public void test05(){
        String url="http://localhost:6766/account/updateAccount";
        //设置请求头的请求参数类型
        HttpHeaders headers = new HttpHeaders();
        //告知被调用方，发送的数据格式的json格式，对方要以json的方式解析处理
        headers.add("Content-type","application/json; charset=utf-8");
        //组装json格式数据
//        HashMap<String, String> reqMap = new HashMap<>();
//        reqMap.put("id","1");
//        reqMap.put("userName","zhangsan");
//        reqMap.put("address","上海");
//        String jsonReqData = new Gson().toJson(reqMap);
        String jsonReq="{\"address\":\"上海\",\"id\":\"1\",\"userName\":\"zhangsan\"}";
        //构建请求对象
        HttpEntity<String> httpEntity = new HttpEntity<>(jsonReq, headers);
          /*
            发送数据
            参数1：请求url地址
            参数2：请求方式
            参数3：请求体对象，携带了请求头和请求体相关的参数
            参数4：响应数据类型
         */
        ResponseEntity<Account> responseEntity = restTemplate.exchange(url, HttpMethod.POST, httpEntity, Account.class);
        //或者
        // Account account=restTemplate.postForObject(url,httpEntity,Account.class);
        Account body = responseEntity.getBody();
        System.out.println(body);
    }

    /**
     * 获取请求cookie值
     */
    @Test
    public void test06(){
        String url="http://localhost:6766/account/getCookie";
        ResponseEntity<String> result = restTemplate.getForEntity(url, String.class);
        //获取cookie
        List<String> cookies = result.getHeaders().get("Set-Cookie");
        //获取响应数据
        String resStr = result.getBody();
        System.out.println(resStr);
        System.out.println(cookies);
    }

    /**
     * @desc 测试采集国内大盘数据
     */
    @Test
    public void testInnerGetMarketInfo() throws InterruptedException {
//        stockTimerTaskService.getInnerMarketInfo();
        stockTimerTaskService.getStockRtIndex();
        //目的：让主线程休眠，等待子线程执行完任务
        Thread.sleep(5000);
    }




}
