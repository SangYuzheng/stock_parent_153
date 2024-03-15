package com.itheima.stock.log.annotation;

import java.lang.annotation.*;

/**
 * @author : itheima
 * @date : 2023/2/4 11:45
 * @description  定义接口功能描述注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface StockLog {
    String value() default "";
}
