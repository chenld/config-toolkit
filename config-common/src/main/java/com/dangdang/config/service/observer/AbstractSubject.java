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
package com.dangdang.config.service.observer;

/**
 * 主题通用实现
 *
 * @author <a href="mailto:wangyuxuan@dangdang.com">Yuxuan Wang</a>
 * @author liangd.chen
 */
public abstract class AbstractSubject implements ISubject {

    CompositeObserver compositeObserver = null;

    @Override
    public boolean register(final IObserver watcher, String groupKey) {
        if (compositeObserver != null){
            return compositeObserver.addWatchers(groupKey, watcher);
        }
        return false;
    }

    @Override
    public void notify(final String groupKey, final String value) {
        if (compositeObserver != null){
            compositeObserver.notify(groupKey, value);
        }
    }

    public void setCompositeObserver(CompositeObserver compositeObserver){
        this.compositeObserver = compositeObserver;
    }

}
