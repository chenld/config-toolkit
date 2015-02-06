package com.dangdang.config.service.easyzk.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 属性标注
 *
 * @author liangd.chen
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD})
public @interface ZooKeeper {

    String value() default "";

    /**
     * 节点
     */
    String node();

    /**
     * 节点下的key值
     */
    String key();

    /**
     * 默认的将配置，当在ZK获取值为空时
     */
    String defaultValue() default "";

}
