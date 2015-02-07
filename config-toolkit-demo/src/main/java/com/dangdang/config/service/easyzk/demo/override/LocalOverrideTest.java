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
package com.dangdang.config.service.easyzk.demo.override;

import com.dangdang.config.service.easyzk.ConfigFactory;
import com.dangdang.config.service.easyzk.ConfigGroup;
import com.dangdang.config.service.observer.IObserver;
import com.google.common.base.Preconditions;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;

/**
 * @author <a href="mailto:wangyuxuan@dangdang.com">Yuxuan Wang</a>
 *
 */
public class LocalOverrideTest {

	public static void main(String[] args) {
        CuratorFramework cli = CuratorFrameworkFactory.newClient("zoo.host1:8181", new RetryOneTime(2000));

        ConfigFactory configFactory = new ConfigFactory("/projectx/modulex", cli);

		ConfigGroup propertyGroup1 = configFactory.getConfigNode("property-group1");
		System.out.println(propertyGroup1);
		
		// Listen changes
		propertyGroup1.register(new IObserver() {
			@Override
			public void notifiy(String data, String value) {
				// Some initialization
			}
		}, "group1");

		String stringProperty = propertyGroup1.getProperty("string_property_key");
		System.out.println(stringProperty);
		Preconditions.checkState("Welcome here.".equals(stringProperty));
		
		String intProperty = propertyGroup1.getProperty("int_property_key");
		System.out.println(intProperty);
		Preconditions.checkState(1123 == Integer.parseInt(intProperty));
	}

}
