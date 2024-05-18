/*
 * Copyright 2022-2024 Ponfee (http://www.ponfee.cn/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cn.ponfee.disjob.common.spring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.DefaultSingletonBeanRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;

import java.util.Objects;

import static cn.ponfee.disjob.common.util.ClassUtils.getObjectClassName;

/**
 * Spring container context holder
 *
 * @author Ponfee
 */
public class SpringContextHolder implements ApplicationContextAware, BeanFactoryPostProcessor {

    private static ApplicationContext applicationContext;
    private static ConfigurableListableBeanFactory beanFactory;

    @Override
    public void setApplicationContext(ApplicationContext ctx) {
        synchronized (SpringContextHolder.class) {
            if (applicationContext != null) {
                throw new IllegalStateException("Spring context holder already initialized.");
            }
            applicationContext = Objects.requireNonNull(ctx);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory bf) throws BeansException {
        synchronized (SpringContextHolder.class) {
            if (beanFactory != null) {
                throw new IllegalStateException("Spring context holder already initialized.");
            }
            beanFactory = Objects.requireNonNull(bf);
        }
    }

    // -----------------------------------------------------------------------get bean factory

    public static ApplicationContext getApplicationContext() {
        return Objects.requireNonNull(applicationContext, "ApplicationContext is null.");
    }

    public static ListableBeanFactory getListableBeanFactory() {
        if (beanFactory != null) {
            return beanFactory;
        }
        return Objects.requireNonNull(applicationContext, "ListableBeanFactory is null.");
    }

    public static ConfigurableListableBeanFactory getConfigurableListableBeanFactory() {
        if (beanFactory != null) {
            return beanFactory;
        }
        if (applicationContext instanceof ConfigurableApplicationContext) {
            return ((ConfigurableApplicationContext) applicationContext).getBeanFactory();
        }
        throw (applicationContext == null)
            ? new NullPointerException("ConfigurableListableBeanFactory is null.")
            : new UnsupportedOperationException("ApplicationContext is not ConfigurableListableBeanFactory.");
    }

    public static boolean isNotNull() {
        return beanFactory != null || applicationContext != null;
    }

    public static Environment getEnvironment() {
        return getApplicationContext().getEnvironment();
    }

    public static void publishEvent(Object event) {
        getApplicationContext().publishEvent(event);
    }

    // -----------------------------------------------------------------------getBean

    /**
     * Gets spring bean by bean name.
     *
     * @param beanName then bean name
     * @return spring bean
     */
    public static <T> T getBean(String beanName) {
        return (T) getListableBeanFactory().getBean(beanName);
    }

    /**
     * Gets spring bean by bean type.
     *
     * @param beanType the bean type
     * @return spring bean
     */
    public static <T> T getBean(Class<T> beanType) {
        return getListableBeanFactory().getBean(beanType);
    }

    /**
     * Gets spring bean by bean name and type.
     *
     * @param beanName the bean name
     * @param beanType the bean type
     * @return spring bean
     */
    public static <T> T getBean(String beanName, Class<T> beanType) {
        return getListableBeanFactory().getBean(beanName, beanType);
    }

    // -----------------------------------------------------------------------getPrototypeBean

    public static <T> T getPrototypeBean(String beanName) throws IllegalStateException {
        ListableBeanFactory factory = getListableBeanFactory();
        T bean;
        try {
            bean = (T) factory.getBean(beanName);
        } catch (NoSuchBeanDefinitionException ignored) {
            return null;
        }

        if (!factory.isPrototype(beanName)) {
            throw new IllegalStateException("Bean name is not a prototype bean: " + beanName);
        }

        return bean;
    }

    /**
     * Gets spring bean by bean type, if not defined bean then return null
     *
     * @param beanType the bean type
     * @return spring bean
     * @throws IllegalStateException if not prototype bean
     */
    public static <T> T getPrototypeBean(Class<T> beanType) throws IllegalStateException {
        ListableBeanFactory factory = getListableBeanFactory();
        T bean;
        try {
            bean = factory.getBean(beanType);
        } catch (NoSuchBeanDefinitionException ignored) {
            return null;
        }

        for (String beanName : factory.getBeanNamesForType(beanType)) {
            if (!factory.isPrototype(beanName)) {
                throw new IllegalStateException("Bean type is not a prototype bean: " + beanType);
            }
        }

        return bean;
    }

    /**
     * Gets spring bean by bean name and type, if not defined bean then return null
     *
     * @param beanName the bean name
     * @param beanType the bean type
     * @return spring bean
     * @throws IllegalStateException if not prototype bean
     */
    public static <T> T getPrototypeBean(String beanName, Class<T> beanType) throws IllegalStateException {
        ListableBeanFactory factory = getListableBeanFactory();
        T bean;
        try {
            bean = factory.getBean(beanName, beanType);
        } catch (NoSuchBeanDefinitionException ignored) {
            return null;
        }

        if (!factory.isPrototype(beanName)) {
            throw new IllegalStateException("Bean name is not a prototype bean: " + beanName + ", " + beanType);
        }

        return bean;
    }

    // -----------------------------------------------------------------------getSingletonBean

    public static <T> T getSingletonBean(String beanName) throws IllegalStateException {
        ListableBeanFactory factory = getListableBeanFactory();
        T bean;
        try {
            bean = (T) factory.getBean(beanName);
        } catch (NoSuchBeanDefinitionException ignored) {
            return null;
        }

        if (!factory.isSingleton(beanName)) {
            throw new IllegalStateException("Bean name is not a prototype bean: " + beanName);
        }

        return bean;
    }

    /**
     * Gets spring bean by bean type, if not defined bean then return null
     *
     * @param beanType the bean type
     * @return spring bean
     * @throws IllegalStateException if not singleton bean
     */
    public static <T> T getSingletonBean(Class<T> beanType) throws IllegalStateException {
        ListableBeanFactory factory = getListableBeanFactory();
        T bean;
        try {
            bean = factory.getBean(beanType);
        } catch (NoSuchBeanDefinitionException ignored) {
            return null;
        }

        for (String beanName : factory.getBeanNamesForType(beanType)) {
            if (!factory.isSingleton(beanName)) {
                throw new IllegalStateException("Bean type is not a singleton bean: " + beanType);
            }
        }
        return bean;
    }

    /**
     * Gets spring bean by bean name and type, if not defined bean then return null
     *
     * @param beanName the bean name
     * @param beanType the bean type
     * @return spring bean
     * @throws IllegalStateException if not singleton bean
     */
    public static <T> T getSingletonBean(String beanName, Class<T> beanType) throws IllegalStateException {
        ListableBeanFactory factory = getListableBeanFactory();
        T bean;
        try {
            bean = factory.getBean(beanName, beanType);
        } catch (NoSuchBeanDefinitionException ignored) {
            return null;
        }

        if (!factory.isSingleton(beanName)) {
            throw new IllegalStateException("Bean name is not a singleton bean: " + beanName + ", " + beanType);
        }

        return bean;
    }

    // -----------------------------------------------------------------------other methods

    /**
     * Returns spring container contains specified bean name.
     *
     * @param beanName the bean name
     * @return {@code true} if contains bean
     */
    public static boolean containsBean(String beanName) {
        return getListableBeanFactory().containsBean(beanName);
    }

    /**
     * Returns spring bean name type.
     *
     * @param beanName then bean name
     * @return bean name type
     */
    public static Class<?> getType(String beanName) {
        return getListableBeanFactory().getType(beanName);
    }

    /**
     * Returns spring bean name other alias name.
     *
     * @param beanName then bean name
     * @return other alias name
     */
    public static String[] getAliases(String beanName) {
        return getApplicationContext().getAliases(beanName);
    }

    public static void registerSingleton(String beanName, Object bean) {
        ConfigurableListableBeanFactory factory = getConfigurableListableBeanFactory();
        factory.autowireBean(bean);
        factory.registerSingleton(beanName, bean);
    }

    public static void destroySingleton(String beanName) {
        ConfigurableListableBeanFactory factory = getConfigurableListableBeanFactory();
        if (factory instanceof DefaultSingletonBeanRegistry) {
            ((DefaultSingletonBeanRegistry) factory).destroySingleton(beanName);
        }
        throw new UnsupportedOperationException("Unsupported destroy Singleton: " + getObjectClassName(factory));
    }

    /**
     * Registers bean definition to spring container.
     *
     * @param beanName the bean name
     * @param beanType the bean type
     * @param args     the bean type constructor arguments
     * @param <T>      bean type
     * @return spring container bean instance
     */
    public static <T> T registerBeanDefinition(String beanName, Class<T> beanType, Object... args) {
        ApplicationContext ac = getApplicationContext();
        if (!(ac instanceof ConfigurableApplicationContext)) {
            throw new UnsupportedOperationException("Unsupported register bean definition: " + getObjectClassName(ac));
        }

        BeanDefinitionBuilder bdb = BeanDefinitionBuilder.genericBeanDefinition(beanType);
        for (Object arg : args) {
            bdb.addConstructorArgValue(arg);
        }
        ConfigurableApplicationContext cac = (ConfigurableApplicationContext) ac;
        BeanDefinitionRegistry bdr = (BeanDefinitionRegistry) cac.getBeanFactory();
        bdr.registerBeanDefinition(beanName, bdb.getRawBeanDefinition());

        return cac.getBean(beanName, beanType);
    }

    /**
     * Removes bean definition from spring container.
     *
     * @param beanName the bean name
     */
    public static void removeBeanDefinition(String beanName) {
        ApplicationContext ac = getApplicationContext();
        if (!(ac instanceof ConfigurableApplicationContext)) {
            throw new UnsupportedOperationException("Unsupported remove bean definition: " + getObjectClassName(ac));
        }

        ConfigurableApplicationContext cac = (ConfigurableApplicationContext) ac;
        ((BeanDefinitionRegistry) cac.getBeanFactory()).removeBeanDefinition(beanName);
    }

    /**
     * Autowire annotated from spring container for object
     *
     * @param bean the spring bean
     */
    public static void autowireBean(Object bean) {
        getApplicationContext().getAutowireCapableBeanFactory().autowireBean(bean);
    }

}
