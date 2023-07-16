/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.spring;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.util.Arrays;
import java.util.Objects;

/**
 * Spring container context holder
 *
 * @author Ponfee
 */
public class SpringContextHolder implements ApplicationContextAware {

    private static ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext cxt) {
        synchronized (SpringContextHolder.class) {
            if (applicationContext != null) {
                throw new IllegalStateException("Spring context holder already initialized.");
            }
            applicationContext = Objects.requireNonNull(cxt);
        }
    }

    /**
     * Gets spring bean by bean name.
     *
     * @param beanName then bean name
     * @return spring bean
     */
    public static Object getBean(String beanName) throws BeansException {
        if (applicationContext == null) {
            return null;
        }
        return applicationContext.getBean(beanName);
    }

    /**
     * Gets spring bean by bean type.
     *
     * @param beanType the bean type
     * @return spring bean
     */
    public static <T> T getBean(Class<T> beanType) {
        if (applicationContext == null) {
            return null;
        }
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
        if (applicationContext == null) {
            return null;
        }
        return applicationContext.getBean(beanName, beanType);
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
        if (applicationContext == null) {
            return null;
        }

        T bean;
        try {
            bean = applicationContext.getBean(beanName, beanType);
        } catch (BeansException ignored) {
            return null;
        }

        if (applicationContext.isPrototype(beanName)) {
            return bean;
        }
        throw new IllegalStateException("Bean name is not a prototype bean: " + beanName + ", " + beanType);
    }

    /**
     * Gets spring bean by bean type, if not defined bean then return null
     *
     * @param beanType the bean type
     * @return spring bean
     * @throws IllegalStateException if not prototype bean
     */
    public static <T> T getPrototypeBean(Class<T> beanType) throws IllegalStateException {
        if (applicationContext == null) {
            return null;
        }

        T bean;
        try {
            bean = applicationContext.getBean(beanType);
        } catch (BeansException ignored) {
            return null;
        }

        String[] beanNames = applicationContext.getBeanNamesForType(beanType);
        if (Arrays.stream(beanNames).allMatch(applicationContext::isPrototype)) {
            return bean;
        }
        throw new IllegalStateException("Bean type is not a prototype bean: " + beanType);
    }

    /**
     * Returns spring container contains specified bean name.
     *
     * @param beanName the bean name
     * @return {@code true} if contains bean
     */
    public static boolean containsBean(String beanName) {
        if (applicationContext == null) {
            return false;
        }
        return applicationContext.containsBean(beanName);
    }

    /**
     * Returns spring bean name type.
     *
     * @param beanName then bean name
     * @return bean name type
     */
    public static Class<?> getType(String beanName) {
        if (applicationContext == null) {
            return null;
        }
        return applicationContext.getType(beanName);
    }

    /**
     * Returns spring bean name other alias name.
     *
     * @param beanName then bean name
     * @return other alias name
     */
    public static String[] getAliases(String beanName) {
        if (applicationContext == null) {
            return null;
        }
        return applicationContext.getAliases(beanName);
    }

    /**
     * Autowire annotated from spring container for object
     *
     * @param bean the spring bean
     */
    public static void autowire(Object bean) {
        if (applicationContext == null) {
            return;
        }
        applicationContext.getAutowireCapableBeanFactory().autowireBean(bean);
    }

}
