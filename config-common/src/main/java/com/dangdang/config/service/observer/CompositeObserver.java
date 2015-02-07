package com.dangdang.config.service.observer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

/**
 * Composite 模式
 *
 * @author liangd.chen
 */
public class CompositeObserver implements IObserver {

    Map<String, List<IObserver>> observers = Maps.newHashMap();

    String group;

    ExecutorService executorService;

    public CompositeObserver(String group, ExecutorService executorService) {
        this.group = group;
        this.executorService = executorService;
    }

    /**
     * add Watchers
     *
     * @param groupKey  like group.key
     * @param iObserver
     * @return
     */
    public boolean addWatchers(String groupKey, IObserver iObserver) {
        if (!matchGroup(groupKey)) {
            return false;
        }

        List<IObserver> lst = observers.get(groupKey);
        if (lst == null) {
            observers.put(groupKey, new ArrayList<IObserver>());
            lst = observers.get(groupKey);
        }

        lst.add(iObserver);
        return true;
    }

    @Override
    public void notify(final String groupKey, final String value) {
        List<IObserver> lst = Lists.newArrayList();
        int idx = groupKey.indexOf(".");
        if (idx > -1) {
            List<IObserver> grouplst = observers.get(groupKey.substring(0, idx));
            if (grouplst != null && grouplst.size() > 0) {
                lst.addAll(grouplst);
            }
        }

        List<IObserver> groupKeyLst = observers.get(groupKey);
        if (groupKeyLst != null && groupKeyLst.size() > 0) {
            lst.addAll(observers.get(groupKey));
        }

        if (lst != null && !lst.isEmpty()) {
            for (final IObserver iObserver : lst) {
                executorService.submit(new Callable<String>() {
                    @Override
                    public String call() {
                        iObserver.notify(groupKey, value);
                        return "OK";
                    }
                });
            }
        }
    }

    public String getGroup() {
        return group;
    }

    public boolean matchGroup(String groupKey) {
        String group = groupKey;
        int idx = groupKey.indexOf(".");
        if (idx > -1) {
            group = groupKey.substring(0, idx);
        }
        return this.group.equals(group);
    }
}
