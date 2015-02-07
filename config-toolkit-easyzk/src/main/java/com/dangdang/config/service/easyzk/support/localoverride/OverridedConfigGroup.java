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
package com.dangdang.config.service.easyzk.support.localoverride;

import com.dangdang.config.service.easyzk.ConfigGroup;
import com.dangdang.config.service.easyzk.ConfigProfile;
import com.google.common.collect.Maps;
import org.apache.curator.framework.CuratorFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * ConfigGroup with local configurations overrided
 * 
 * @author <a href="mailto:wangyuxuan@dangdang.com">Yuxuan Wang</a>
 *
 */
public class OverridedConfigGroup extends ConfigGroup {

	private Map<String, String> localProperties;

	private static final Logger LOGGER = LoggerFactory.getLogger(OverridedConfigGroup.class);

	/**
	 * @param configProfile
	 * @param group
	 */
	public OverridedConfigGroup(ConfigProfile configProfile, CuratorFramework client, String group) {
		super(configProfile, client, group);
		localProperties = LocalOverrideFileLoader.loadLocalProperties(configProfile.getRootNode(), group);
		LOGGER.info("Loading local override configs: {}", localProperties);
	}

	@Override
	public String getProperty(String key) {
		if (localProperties != null && localProperties.containsKey(key)) {
			return localProperties.get(key);
		}
		return super.getProperty(key);
	}

	@Override
	public Map<String, String> exportProperties() {
		Map<String, String> properties = super.exportProperties();
		if (localProperties != null) {
			properties.putAll(Maps.newHashMap(localProperties));
		}
		return properties;
	}

}
