package com.dangdang.config.service.observer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 定义节点触发事件
 * 1.当key为空时，则定义组级别事件
 * 2.当key不为空时，则定义属性级别事件
 *
 * @author liangd.chen
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Notify {

    /**
     * 组
     */
    String group();

    /**
     * 属性
     */
    String key() default "";

}
