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
package com.dangdang.config.service.easyzk.demo.spring.annotation;

import com.dangdang.config.service.easyzk.ConfigFactory;
import com.dangdang.config.service.easyzk.ConfigGroup;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryOneTime;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Example with spring annotation
 *
 * @author <a href="mailto:wangyuxuan@dangdang.com">Yuxuan Wang</a>
 */
@Configuration
public class ConfigCenter {

    @Bean
    public ConfigFactory getConfigFactory() {
        CuratorFramework cli = CuratorFrameworkFactory.newClient("zoo.host1:8181,zoo.host2:8181,zoo.host3:8181", new RetryOneTime(2000));

        return new ConfigFactory("/projectx/modulex", cli);
    }

    @Bean(name = "propertyGroup1")
    public ConfigGroup getPropertyGroup1(ConfigFactory configFactory) {
        return configFactory.getConfigNode("property-group1");
    }
}
