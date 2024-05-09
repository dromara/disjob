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
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
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
            if (SpringContextHolder.applicationContext != null) {
                throw new IllegalStateException("Spring context holder already initialized.");
            }
            SpringContextHolder.applicationContext = Objects.requireNonNull(ctx);
        }
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        synchronized (SpringContextHolder.class) {
            if (SpringContextHolder.beanFactory != null) {
                throw new IllegalStateException("Spring context holder already initialized.");
            }
            SpringContextHolder.beanFactory = Objects.requireNonNull(beanFactory);
        }
    }

    public static ApplicationContext applicationContext() {
        return applicationContext;
    }

    // -----------------------------------------------------------------------getBean

    /**
     * Gets spring bean by bean name.
     *
     * @param beanName then bean name
     * @return spring bean
     */
    public static <T> T getBean(String beanName) {
        return (T) applicationContext.getBean(beanName);
    }

    /**
     * Gets spring bean by bean type.
     *
     * @param beanType the bean type
     * @return spring bean
     */
    public static <T> T getBean(Class<T> beanType) {
        return applicationContext.getBean(beanType);
    }

    /**
     * Gets spring bean by bean name and type.
     *
     * @param beanName the bean name
     * @param beanType the bean type
     * @return spring bean
     */
    public static <T> T getBean(String beanName, Class<T> beanType) {
        return applicationContext.getBean(beanName, beanType);
    }

    // -----------------------------------------------------------------------getPrototypeBean

    public static <T> T getPrototypeBean(String beanName) throws IllegalStateException {
        T bean;
        try {
            bean = (T) applicationContext.getBean(beanName);
        } catch (NoSuchBeanDefinitionException ignored) {
            return null;
        }

        if (!applicationContext.isPrototype(beanName)) {
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
        T bean;
        try {
            bean = applicationContext.getBean(beanType);
        } catch (NoSuchBeanDefinitionException ignored) {
            return null;
        }

        for (String beanName : applicationContext.getBeanNamesForType(beanType)) {
            if (!applicationContext.isPrototype(beanName)) {
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
        T bean;
        try {
            bean = applicationContext.getBean(beanName, beanType);
        } catch (NoSuchBeanDefinitionException ignored) {
            return null;
        }

        if (!applicationContext.isPrototype(beanName)) {
            throw new IllegalStateException("Bean name is not a prototype bean: " + beanName + ", " + beanType);
        }

        return bean;
    }

    // -----------------------------------------------------------------------getSingletonBean

    public static <T> T getSingletonBean(String beanName) throws IllegalStateException {
        T bean;
        try {
            bean = (T) applicationContext.getBean(beanName);
        } catch (NoSuchBeanDefinitionException ignored) {
            return null;
        }

        if (!applicationContext.isSingleton(beanName)) {
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
        T bean;
        try {
            bean = applicationContext.getBean(beanType);
        } catch (NoSuchBeanDefinitionException ignored) {
            return null;
        }

        for (String beanName : applicationContext.getBeanNamesForType(beanType)) {
            if (!applicationContext.isSingleton(beanName)) {
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
        T bean;
        try {
            bean = applicationContext.getBean(beanName, beanType);
        } catch (NoSuchBeanDefinitionException ignored) {
            return null;
        }

        if (!applicationContext.isSingleton(beanName)) {
            throw new IllegalStateException("Bean name is not a singleton bean: " + beanName + ", " + beanType);
        }

        return bean;
    }

    // -----------------------------------------------------------------------other methods

    public static Environment getEnvironment() {
        return applicationContext.getEnvironment();
    }

    /**
     * Returns spring container contains specified bean name.
     *
     * @param beanName the bean name
     * @return {@code true} if contains bean
     */
    public static boolean containsBean(String beanName) {
        return applicationContext.containsBean(beanName);
    }

    /**
     * Returns spring bean name type.
     *
     * @param beanName then bean name
     * @return bean name type
     */
    public static Class<?> getType(String beanName) {
        return applicationContext.getType(beanName);
    }

    /**
     * Returns spring bean name other alias name.
     *
     * @param beanName then bean name
     * @return other alias name
     */
    public static String[] getAliases(String beanName) {
        return applicationContext.getAliases(beanName);
    }

    /**
     * Removes bean from spring container.
     *
     * @param beanName the bean name
     */
    public static void removeBean(String beanName) {
        if (!(applicationContext instanceof ConfigurableApplicationContext)) {
            throw new UnsupportedOperationException("Remove bean failed: " + getObjectClassName(applicationContext));
        }

        ConfigurableApplicationContext cac = (ConfigurableApplicationContext) applicationContext;
        BeanDefinitionRegistry beanFactory = (BeanDefinitionRegistry) cac.getBeanFactory();
        beanFactory.removeBeanDefinition(beanName);
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
    public static <T> T registerBean(String beanName, Class<T> beanType, Object... args) {
        if (!(applicationContext instanceof ConfigurableApplicationContext)) {
            throw new BeanDefinitionValidationException("Register bean failed: " + getObjectClassName(applicationContext));
        }

        BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(beanType);
        for (Object arg : args) {
            beanDefinitionBuilder.addConstructorArgValue(arg);
        }
        BeanDefinition beanDefinition = beanDefinitionBuilder.getRawBeanDefinition();

        ConfigurableApplicationContext cac = (ConfigurableApplicationContext) applicationContext;
        BeanDefinitionRegistry beanFactory = (BeanDefinitionRegistry) cac.getBeanFactory();
        beanFactory.registerBeanDefinition(beanName, beanDefinition);
        return cac.getBean(beanName, beanType);
    }

    /**
     * Autowire annotated from spring container for object
     *
     * @param bean the spring bean
     */
    public static void autowireBean(Object bean) {
        applicationContext.getAutowireCapableBeanFactory().autowireBean(bean);
    }

}
