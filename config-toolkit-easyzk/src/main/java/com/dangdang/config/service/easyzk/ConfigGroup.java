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

import com.dangdang.config.service.observer.AbstractSubject;
import com.google.common.base.Charsets;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.GetChildrenBuilder;
import org.apache.curator.framework.api.GetDataBuilder;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 节点
 * 
 * @author <a href="mailto:wangyuxuan@dangdang.com">Yuxuan Wang</a>
 *
 */
public class ConfigGroup extends AbstractSubject {

	private ConfigProfile configProfile;

	/**
	 * 节点名字
	 */
	String group;

	private KeyLoadingMode keyLoadingMode;

	/**
	 * 需要包含或排除的key,由{@code KeyLoadingMode}决定
	 */
	private Set<String> keysSpecified;

	private CuratorFramework client;

	private ConfigLocalCache configLocalCache;

	public void setConfigLocalCache(ConfigLocalCache configLocalCache) {
		this.configLocalCache = configLocalCache;
	}

	/**
	 * 节点的下属性映射
	 */
	private final Map<String, String> properties = Maps.newConcurrentMap();

	static final Logger LOGGER = LoggerFactory.getLogger(ConfigGroup.class);

	protected ConfigGroup(ConfigProfile configProfile, CuratorFramework client, String group) {
		super();
		this.configProfile = configProfile;
		this.client = client;
		this.group = group;
	}

	public void defineKeyLoadingPattern(KeyLoadingMode keyLoadingMode, Set<String> keysSpecified) {
		this.keyLoadingMode = Preconditions.checkNotNull(keyLoadingMode);
		this.keysSpecified = keysSpecified != null ? Sets.newHashSet(keysSpecified) : keysSpecified;
	}

	/**
	 * 初始化节点
	 */
	protected void initConfigNode() {
		client.getCuratorListenable().addListener(new ConfigNodeEventListener(this));
		
		loadNode();

		// Update local cache
		if (configLocalCache != null) {
			configLocalCache.saveLocalCache(this, group);
		}

		// Consistency check
		if (configProfile.isConsistencyCheck()) {
			new Timer().scheduleAtFixedRate(new TimerTask() {

				@Override
				public void run() {
					LOGGER.debug("Do consistency check for group: {}", group);
					consistencyCheck();
				}
			}, 0L, configProfile.getConsistencyCheckRate());
		}
	}

	private ReadWriteLock lock = new ReentrantReadWriteLock();

    public ReadWriteLock getLock() {
        return lock;
    }

    /**
	 * 加载节点并监听节点变化
	 */
	void loadNode() {
		final String nodePath = ZKPaths.makePath(configProfile.getRootNode(), group);
		LOGGER.debug("Loading properties for node: {}, with loading mode: {} and keys specified: {}", nodePath, keyLoadingMode, keysSpecified);

		GetChildrenBuilder childrenBuilder = client.getChildren();

		try {
			List<String> children = childrenBuilder.watched().forPath(nodePath);
			if (children != null) {
				lock.writeLock().lock();
				try {
					properties.clear();
					for (String child : children) {
						loadKey(ZKPaths.makePath(nodePath, child));
					}
				} finally {
					lock.writeLock().unlock();
				}
			}
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	/**
	 * 一致性检查
	 */
	void consistencyCheck() {
		final String nodePath = ZKPaths.makePath(configProfile.getRootNode(), group);
		GetChildrenBuilder childrenBuilder = client.getChildren();
		try {
			List<String> children = childrenBuilder.watched().forPath(nodePath);
			if (children != null) {
				List<String> redundances = Lists.newArrayList(properties.keySet());
				redundances.removeAll(children);
				if (!redundances.isEmpty()) {
					for (String redundance : redundances) {
						properties.remove(redundance);
					}
				}
				// 由于一致性检查可能比较频繁,所以做与loadNode()的隔离即可,所以使用读锁
				lock.readLock().lock();
				try {
					for (String child : children) {
						loadKey(ZKPaths.makePath(nodePath, child));
					}
				} finally {
					lock.readLock().unlock();
				}
			}
		} catch (Exception e) {
			throw Throwables.propagate(e);
		}
	}

	/**
	 * 加载属性并监听属性变化
	 * 
	 * @param nodePath
	 *            属性的路径
	 * @throws Exception
	 */
	void loadKey(final String nodePath) throws Exception {
		String nodeName = ZKPaths.getNodeFromPath(nodePath);
		switch (keyLoadingMode) {
		case INCLUDE:
			if (!keysSpecified.contains(nodeName)) {
				return;
			}
			break;
		case EXCLUDE:
			if (keysSpecified.contains(nodeName)) {
				return;
			}
			break;
		case NONE:
			break;
		default:
			break;
		}

		GetDataBuilder data = client.getData();
		String childValue = new String(data.watched().forPath(nodePath), Charsets.UTF_8);

		if (Objects.equal(childValue, properties.get(nodeName))) {
			LOGGER.trace("Key data not change, ignore: key[{}]", nodeName);
		} else {
			LOGGER.debug("Loading data: key[{}] - value[{}]", nodeName, childValue);
            String groupKey = getGroupKey(nodeName);
			properties.put(groupKey, childValue);
			// 通知注册者
			notify(groupKey, childValue);
		}
	}

	/**
	 * 关闭连接
	 */
	@PreDestroy
	private void destroy() {
		if (client != null) {
			client.close();
		}
	}

	/**
	 * 获取属性值
	 * 
	 * @param key
	 * @return
	 */
	public String getProperty(String key) {
		lock.readLock().lock();
		try {
			return properties.get(key);
		} finally {
			lock.readLock().unlock();
		}
	}

	public String getGroup() {
		return group;
	}

	public ConfigLocalCache getConfigLocalCache() {
		return configLocalCache;
	}

	/**
	 * 导出属性列表
	 * 
	 * @return
	 */
	public Map<String, String> exportProperties() {
		return Maps.newHashMap(properties);
	}

	/**
	 * 节点下属性的加载模式
	 * 
	 * @author <a href="mailto:wangyuxuan@dangdang.com">Yuxuan Wang</a>
	 *
	 */
	public static enum KeyLoadingMode {
		/**
		 * 加载所有属性
		 */
		NONE,
		/**
		 * 包含某些属性
		 */
		INCLUDE,
		/**
		 * 排除某些属性
		 */
		EXCLUDE;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this).add("configProfile", configProfile).add("node", group).add("keyLoadingMode", keyLoadingMode)
				.add("keysSpecified", keysSpecified).add("properties", properties).toString();
	}

    /**
     * get key like group.key
     * @param nodeName
     * @return
     */
    private String getGroupKey(String nodeName){
        return group + "." + nodeName;
    }

}
