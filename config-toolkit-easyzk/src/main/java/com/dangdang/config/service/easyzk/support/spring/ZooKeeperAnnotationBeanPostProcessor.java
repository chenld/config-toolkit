package com.dangdang.config.service.easyzk.support.spring;

import com.dangdang.config.service.easyzk.ConfigProfile;
import com.dangdang.config.service.easyzk.annotation.ZooKeeper;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.GetDataBuilder;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.KeeperException.ConnectionLossException;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.SimpleTypeConverter;
import org.springframework.beans.TypeConverter;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.ReflectionUtils.*;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.springframework.util.ReflectionUtils.*;

/**
 * 配置的核心类
 * 1.初始时，标注的字段从ZK节点获取并赋值；
 * 2.ZK节点路径数值发生变化后，能自动获取值；
 *
 */
public class ZooKeeperAnnotationBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware, PriorityOrdered {

    private static final Logger LOG = LoggerFactory.getLogger(ZooKeeperAnnotationBeanPostProcessor.class);

    private static final FieldFilter FIELD_FILTER = new FieldFilter() {
        @Override
        public boolean matches(Field field) {
            return COPYABLE_FIELDS.matches(field) && field.isAnnotationPresent(ZooKeeper.class);
        }
    };

    private static final MethodFilter METHOD_FILTER = new MethodFilter() {
        @Override
        public boolean matches(Method method) {
            return USER_DECLARED_METHODS.matches(method) && method.isAnnotationPresent(ZooKeeper.class);
        }
    };

    private int order = Ordered.LOWEST_PRECEDENCE - 2;

    private CuratorFramework curatorFramework;

    private ConfigProfile configProfile;

    private TypeConverter typeConverter;

    public void setCuratorFramework(CuratorFramework curator) {
        this.curatorFramework = curator;
    }

    public void setConfigProfile(ConfigProfile configProfile) {
        this.configProfile = configProfile;
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
        if (beanFactory instanceof ConfigurableBeanFactory) {
            typeConverter = ((ConfigurableBeanFactory) beanFactory).getTypeConverter();
        }

        if (typeConverter == null) {
            typeConverter = new SimpleTypeConverter();
        }
    }

    @Override
    public Object postProcessBeforeInitialization(final Object bean, String beanName) throws BeansException {
        Class<?> targetClass = AopUtils.getTargetClass(bean);

        doWithFields(targetClass, new FieldCallback() {
            @Override
            public void doWith(final Field field) throws IllegalArgumentException, IllegalAccessException {
                makeAccessible(field);
                ZooKeeper zooKeeper = field.getAnnotation(ZooKeeper.class);
                String path = makePath(zooKeeper);
                final Class<?> requiredType = field.getType();
                lookup(path, new Setter<String>(bean, zooKeeper.defaultValue()) {
                    public void setValue(String value) {
                        setField(field, getTarget(), convert(value, requiredType, null));
                    }
                });
            }
        }, FIELD_FILTER);

        doWithMethods(targetClass, new MethodCallback() {
            @Override
            public void doWith(final Method method) throws IllegalArgumentException, IllegalAccessException {
                makeAccessible(method);
                ZooKeeper zooKeeper = method.getAnnotation(ZooKeeper.class);
                String path = makePath(zooKeeper);
                final MethodParameter param = MethodParameter.forMethodOrConstructor(method, 0);
                final Class<?> requiredType = param.getParameterType();
                lookup(path, new Setter<String>(bean, zooKeeper.defaultValue()) {
                    public void setValue(String value) {
                        invokeMethod(method, getTarget(), convert(getValue(value), requiredType, param));
                    }
                });
            }
        }, METHOD_FILTER);

        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    protected void lookup(final String path, final Setter setter) {
        lookup(path, setter, new Watcher() {
            @Override
            public void process(WatchedEvent event) {
                // check that path and event.getPath match?
                LOG.info("Watcher for '{}' received watched event: {}", path, event);
                if (event.getType() == EventType.NodeDataChanged) {
                    lookup(path, setter, this);
                }
            }
        });
    }

    protected void lookup(String path, Setter setter, Watcher watcher) {
        try {
            GetDataBuilder getDataBuilder = curatorFramework.getData();
            if (setter.isValid()) {
                getDataBuilder.usingWatcher(watcher);
            }
            byte[] data = getDataBuilder.forPath(path);
            setter.setValue(new String(data, "UTF-8"));
        } catch (NoNodeException ex) {
            // TODO
        } catch (ConnectionLossException ex) {
            // TODO
        } catch (Exception ex) {
            //rethrowRuntimeException(ex);
        }
    }

    protected Object convert(Object value, Class<?> requiredType, MethodParameter param) {
        if (typeConverter != null) {
            return typeConverter.convertIfNecessary(value, requiredType, param);
        } else {
            if (requiredType.isInstance(value)) {
                return requiredType.cast(value);
            } else {
                throw new TypeMismatchException(value, requiredType);
            }
        }
    }

    @Override
    public int getOrder() {
        return LOWEST_PRECEDENCE - 1;
    }

    private static abstract class Setter<T> {

        protected final WeakReference<Object> target;

        private  T defaultValue;

        public Setter(final Object target, T defaultValue) {
            this.target = new WeakReference<Object>(target);
            this.defaultValue = defaultValue;
        }

        public boolean isValid() {
            return this.target.get() != null;
        }

        public Object getTarget() {
            return target.get();
        }

        public T getValue(T value){
            if(value == null && defaultValue != null){
                return defaultValue;
            }

            return value;
        }

        abstract void setValue(T value);

    }

    /**
     * 构建@ZooKeeper 标注的ZK路径
     * @param zooKeeper
     * @return
     */
    private String makePath(ZooKeeper zooKeeper){
        String node = zooKeeper.node();
        String key = zooKeeper.key();
        String path = ZKPaths.makePath(node, key);
        return ZKPaths.makePath(configProfile.getRootNode(), path);
    }

}
