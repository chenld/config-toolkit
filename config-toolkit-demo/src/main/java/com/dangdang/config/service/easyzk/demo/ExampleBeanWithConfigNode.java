/**
 * 
 */
package com.dangdang.config.service.easyzk.demo;

import com.dangdang.config.service.easyzk.ConfigGroup;

/**
 * @author <a href="mailto:wangyuxuan@dangdang.com">Yuxuan Wang</a>
 *
 */
public class ExampleBeanWithConfigNode {
	
	private ConfigGroup propertyGroup1;
	
	public void someMethod(){
		System.out.println(propertyGroup1.getProperty("someKey"));
	}

	public void setPropertyGroup1(ConfigGroup propertyGroup1) {
		this.propertyGroup1 = propertyGroup1;
	}

}
