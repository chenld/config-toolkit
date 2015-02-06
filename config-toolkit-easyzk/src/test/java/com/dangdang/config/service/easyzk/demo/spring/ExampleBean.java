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
package com.dangdang.config.service.easyzk.demo.spring;

/**
 * Example bean for spring integration
 * 
 * @author <a href="mailto:wangyuxuan@dangdang.com">Yuxuan Wang</a>
 *
 */
public class ExampleBean {
	
	private String stringProperty;
	
	private int intProperty;

	public ExampleBean() {
		super();
	}

    public ExampleBean(String stringProperty, int intProperty) {
        this.stringProperty = stringProperty;
        this.intProperty = intProperty;
    }

    public String getStringProperty() {
		return stringProperty;
	}

	public int getIntProperty() {
		return intProperty;
	}

    public void setStringProperty(String stringProperty) {
        this.stringProperty = stringProperty;
    }

    public void setIntProperty(int intProperty) {
        this.intProperty = intProperty;
    }

    @Override
	public String toString() {
		return "ExampleBean [stringProperty=" + stringProperty + ", intProperty=" + intProperty + "]";
	}
	
}
