package com.dangdang.config.service.easyzk.support.spring;

import com.dangdang.config.service.easyzk.ConfigProfile;
import com.dangdang.config.service.easyzk.annotation.ZooKeeper;
import com.google.common.base.Charsets;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.CuratorEvent;
import org.apache.curator.framework.api.CuratorListener;
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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.core.MethodParameter;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.util.ReflectionUtils.*;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

import static org.springframework.util.ReflectionUtils.*;

/**
 * 配置的核心类
 * 1.初始时，标注的字段从ZK节点获取并赋值；
 * 2.ZK节点路径数值发生变化后，能自动获取值；
 *
 * @author liangd.chen
 */
public class ZooKeeperAnnotationBeanPostProcessor implements BeanPostProcessor, BeanFactoryAware, PriorityOrdered, CuratorListener, InitializingBean {

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

    private CuratorFramework curator;

    private ConfigProfile configProfile;

    private TypeConverter typeConverter;

    private ConcurrentHashMap<String, Setter<String>> setters = new ConcurrentHashMap<String, Setter<String>>();

    public void setCurator(CuratorFramework curator) {
        this.curator = curator;
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
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;
    }

    protected void lookup(String path, Setter setter) {
        try {
            GetDataBuilder getDataBuilder = curator.getData();
            if (setter.isValid()) {
                //getDataBuilder.usingWatcher(watcher);
                if (!setters.containsKey(path)) {
                    setters.put(path, setter);
                }
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

    /**
     * Invoked by a BeanFactory after it has set all bean properties supplied
     * (and satisfied BeanFactoryAware and ApplicationContextAware).
     * <p>This method allows the bean instance to perform initialization only
     * possible when all bean properties have been set and to throw an
     * exception in the event of misconfiguration.
     *
     * @throws Exception in the event of misconfiguration (such
     *                   as failure to set an essential property) or if initialization fails.
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        curator.getCuratorListenable().addListener(this);
    }

    private static abstract class Setter<T> {

        protected final WeakReference<Object> target;

        private T defaultValue;

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

        public T getValue(T value) {
            if (value == null && defaultValue != null) {
                return defaultValue;
            }

            return value;
        }

        abstract void setValue(T value);

    }

    /**
     * 构建@ZooKeeper 标注的ZK路径
     *
     * @param zooKeeper
     * @return ZK key节点全路径
     */
    private String makePath(ZooKeeper zooKeeper) {
        String node = zooKeeper.node();
        String key = zooKeeper.key();
        String path = ZKPaths.makePath(node, key);
        return ZKPaths.makePath(configProfile.getRootNode(), path);
    }

    /**
     * Called when a background task has completed or a watch has triggered
     *
     * @param client client
     * @param event  the event
     * @throws Exception any errors
     */
    @Override
    public void eventReceived(CuratorFramework client, CuratorEvent event) throws Exception {
        final WatchedEvent watchedEvent = event.getWatchedEvent();
        if (watchedEvent != null
                && watchedEvent.getState() == Watcher.Event.KeeperState.SyncConnected
                && watchedEvent.getType() == EventType.NodeDataChanged) {
            String nodePath = watchedEvent.getPath();
            Setter<String> setter = setters.get(nodePath);

            if (setter != null) {
                GetDataBuilder data = client.getData();
                String value = new String(data.watched().forPath(nodePath), Charsets.UTF_8);
                setter.setValue(value);
            }

        }


    }
}
