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

import com.dangdang.config.service.easyzk.ConfigNode.KeyLoadingMode;
import com.dangdang.config.service.easyzk.support.localoverride.OverridedConfigNode;
import com.google.common.base.Preconditions;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

/**
 * 配置工厂
 *
 * @author <a href="mailto:wangyuxuan@dangdang.com">Yuxuan Wang</a>
 */
public final class ConfigFactory {

    private ConfigProfile configProfile;

    private ConfigLocalCache configLocalCache;

    private CuratorFramework client;

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigFactory.class);

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
     * @param node 节点名字
     * @return
     */
    public ConfigNode getConfigNode(String node) {
        return getConfigNode(node, KeyLoadingMode.NONE, null);
    }

    /**
     * 获取配置节点
     *
     * @param node           节点名字
     * @param keyLoadingMode 节点下属性的加载模式
     * @param keysSpecified  需要包含或排除的key,与{@code KeyLoadingMode}配合使用
     * @return
     */
    public ConfigNode getConfigNode(String node, KeyLoadingMode keyLoadingMode, Set<String> keysSpecified) {
        LOGGER.debug("Get node[{}] with mode[{}] and keys[{}]", node, keyLoadingMode, keysSpecified);

        final ConfigNode configNode = new OverridedConfigNode(configProfile, client, node);

        // Load configurations in remote zookeeper.
        configNode.defineKeyLoadingPattern(keyLoadingMode, keysSpecified);
        // config local cache
        configNode.setConfigLocalCache(configLocalCache);

        configNode.initConfigNode();
        return configNode;
    }

}
