package com.dangdang.config.service.easyzk.demo.spring.notify;

import com.dangdang.config.service.observer.IObserver;
import com.dangdang.config.service.observer.Notify;

/**
 * @author liangd.chen
 */
@Notify(group = "property-group1", key = "string_property_key")
class GroupKeyObserver implements IObserver {
    @Override
    public void notify(String groupKey, String value) {
        System.out.println("notify group1: group1, key: key1" );
        System.out.println("print groupkey: " + groupKey + ", key: " + value);
    }
}