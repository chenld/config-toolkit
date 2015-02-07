package com.dangdang.config.service.easyzk.demo.spring.observer;

import com.dangdang.config.service.observer.IObserver;
import com.dangdang.config.service.observer.Observer;

/**
 * @author liangd.chen
 */
@Observer(group = "property-group1")
class GroupObserver implements IObserver {
    @Override
    public void notify(String groupKey, String value) {
        System.out.println("notify group1: group1" );
        System.out.println("print groupkey: " + groupKey + ", key: " + value);
    }
}