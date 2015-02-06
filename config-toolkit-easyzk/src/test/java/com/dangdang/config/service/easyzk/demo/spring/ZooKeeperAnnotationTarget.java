package com.dangdang.config.service.easyzk.demo.spring;

import com.dangdang.config.service.easyzk.annotation.ZooKeeper;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

public class ZooKeeperAnnotationTarget implements InitializingBean, DisposableBean {

    public static final String FIELD_PATH = "/field";
    public static final String METHOD_PATH = "/method";
    public static final String NONEXISTENT_PATH = "/nonexistent";
    @ZooKeeper(node = "property-group1", key = FIELD_PATH)
    private String field;
    private String method;
    private String nonexistent;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getMethod() {
        return method;
    }

    @ZooKeeper(node = "property-group2", key = METHOD_PATH)
    public void setMethod(final String method) {
        this.method = method;
        print("setter");
    }

    public String getNonexistent() {
        return nonexistent;
    }

    @ZooKeeper(node = "property-group2", key = NONEXISTENT_PATH)
    public void setNonexistent(final String nonexistent) {
        this.nonexistent = nonexistent;
        print("nonexistent");
    }

    public ZooKeeperAnnotationTarget() {
        print("ctor");
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        print("init");
    }

    @Override
    public void destroy() throws Exception {
        print("destroy");
    }

    public void print(String phase) {
        System.out.printf("phase '%s': field=%s, method=%s, nonexistent=%s\n", phase, field, method, nonexistent);
    }

    @Override
    public String toString() {
        return "ZooKeeperAnnotationTarget{" +
                "field='" + field + '\'' +
                ", method='" + method + '\'' +
                ", nonexistent='" + nonexistent + '\'' +
                '}';
    }
}
