package com.itheima.stock.mapper;

import com.itheima.stock.pojo.entity.StockBusiness;

import java.util.List;

/**
* @author 46035
* @description 针对表【stock_business(主营业务表)】的数据库操作Mapper
* @createDate 2023-01-08 15:14:39
* @Entity com.itheima.stock.pojo.entity.StockBusiness
*/
public interface StockBusinessMapper {

    int deleteByPrimaryKey(String id);

    int insert(StockBusiness record);

    int insertSelective(StockBusiness record);

    StockBusiness selectByPrimaryKey(String id);

    int updateByPrimaryKeySelective(StockBusiness record);

    int updateByPrimaryKey(StockBusiness record);

    /**
     * 获取所有A股个股的编码集合
     * @return
     */
    List<String> getAllStockCodes();

}
