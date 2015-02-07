/**
 * Copyright 1999-2014 dangdang.com.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dangdang.config.service.easyzk;

import com.dangdang.config.service.easyzk.ConfigGroup.KeyLoadingMode;
import com.dangdang.config.service.easyzk.support.localoverride.OverridedConfigGroup;
import com.dangdang.config.service.observer.CompositeObserver;
import com.dangdang.config.service.observer.IObserver;
import com.dangdang.config.service.observer.Observer;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 配置工厂
 *
 * @author <a href="mailto:wangyuxuan@dangdang.com">Yuxuan Wang</a>
 */
public final class ConfigFactory implements ApplicationContextAware, InitializingBean {

    private ConfigProfile configProfile;

    private ConfigLocalCache configLocalCache;

    private CuratorFramework client;

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigFactory.class);

    private Map<String, CompositeObserver> compositeObserverMap = Maps.newConcurrentMap();

    private ApplicationContext context;

    private ExecutorService executorService = Executors.newCachedThreadPool();

    public ConfigFactory(String rootNode, CuratorFramework client) {
        this(rootNode, false, client);
    }

    public ConfigFactory(String rootNode, boolean openLocalCache, CuratorFramework client) {
        this(new ConfigProfile(rootNode, openLocalCache), client);
    }

    public ConfigFactory(ConfigProfile configProfile, CuratorFramework client) {
        super();
        this.configProfile = Preconditions.checkNotNull(configProfile);
        if (configProfile.isOpenLocalCache()) {
            this.configLocalCache = new ConfigLocalCache(configProfile.getRootNode());
        }
        this.client = client;
    }

    public CuratorFramework getClient() {
        return client;
    }

    /**
     * 获取配置节点
     *
     * @param group 节点名字
     * @return
     */
    public ConfigGroup getConfigNode(String group) {
        return getConfigGroup(group, KeyLoadingMode.NONE, null);
    }

    /**
     * 获取配置节点
     *
     * @param group           节点名字
     * @param keyLoadingMode 节点下属性的加载模式
     * @param keysSpecified  需要包含或排除的key,与{@code KeyLoadingMode}配合使用
     * @return
     */
    public ConfigGroup getConfigGroup(String group, KeyLoadingMode keyLoadingMode, Set<String> keysSpecified) {
        LOGGER.debug("Get group[{}] with mode[{}] and keys[{}]", group, keyLoadingMode, keysSpecified);

        ConfigGroup configGroup = new OverridedConfigGroup(configProfile, client, group);
        configGroup.setCompositeObserver(compositeObserverMap.get(group));
        // Load configurations in remote zookeeper.
        configGroup.defineKeyLoadingPattern(keyLoadingMode, keysSpecified);
        // config local cache
        configGroup.setConfigLocalCache(configLocalCache);

        configGroup.initConfigNode();
        return configGroup;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        context = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Map<String, IObserver> watchers = context.getBeansOfType(IObserver.class, false, true);
        for (Map.Entry<String, IObserver> entry : watchers.entrySet()) {
            IObserver watcher = entry.getValue();
            Observer observer = AnnotationUtils.findAnnotation(watcher.getClass(), Observer.class);
            if (observer != null) {
                String group = observer.group();
                String key = observer.key();
                if (!Strings.isNullOrEmpty(group)) {
                    CompositeObserver compositeObserver = compositeObserverMap.get(group);
                    if (compositeObserver == null) {
                        compositeObserverMap.put(group, new CompositeObserver(group, executorService));
                        compositeObserver = compositeObserverMap.get(group);
                    }

                    String groupKey = group;
                    if (!Strings.isNullOrEmpty(key)) {
                        groupKey += "." + key;
                    }
                    compositeObserver.addWatchers(groupKey, watcher);
                }
            }
        }
    }
}
