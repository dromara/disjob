/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.disjob.common.spring;

import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * <pre>
 * ContextLoaderListener的bean factory是DispatcherServlet的parent
 * spring上下文无法访问spring mvc上下文，但spring mvc上下文却能访问spring上下文，使用List<ApplicationContext>解决
 * </pre>
 *
 * spring上下文持有类
 *
 * @author Ponfee
 */
public class SpringContextHolder implements ApplicationContextAware, DisposableBean {

    private static final List<ApplicationContext> HOLDER = new ArrayList<>();

    private static final AtomicBoolean INITIALIZED = new AtomicBoolean(false);

    @Override
    public void setApplicationContext(ApplicationContext cxt) throws BeansException {
        synchronized (SpringContextHolder.class) {
            if (!HOLDER.contains(cxt)) {
                HOLDER.add(cxt);
            }
            INITIALIZED.compareAndSet(false, true);
        }
    }

    public static boolean isInitialized() {
        return INITIALIZED.get();
    }

    /**
     * 通过名称获取bean
     *
     * @param name
     * @return
     */
    public static Object getBean(String name) {
        return get(c -> c.getBean(name));
    }

    /**
     * 通过类获取bean
     *
     * @param clazz
     * @return
     */
    public static <T> T getBean(Class<T> clazz) {
        return get(c -> c.getBean(clazz));
    }

    /**
     * @param name
     * @param clazz
     * @return
     */
    public static <T> T getBean(String name, Class<T> clazz) {
        return get(c -> c.getBean(name, clazz));
    }

    /**
     * 判断是否含有该名称的Bean
     *
     * @param name
     * @return
     */
    public static boolean containsBean(String name) {
        for (ApplicationContext c : HOLDER) {
            if (c.containsBean(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断Bean是否单例
     *
     * @param name
     * @return
     */
    public static boolean isSingleton(String name) {
        BeansException ex = null;
        for (ApplicationContext c : HOLDER) {
            try {
                if (c.isSingleton(name)) {
                    return true;
                }
            } catch (BeansException e) {
                if (ex == null) {
                    ex = e;
                }
            }
        }
        if (ex == null) {
            return false;
        } else {
            throw ex;
        }
    }
    public static boolean isPrototype(String name) {
        BeansException ex = null;
        for (ApplicationContext c : HOLDER) {
            try {
                if (c.isPrototype(name)) {
                    return true;
                }
            } catch (BeansException e) {
                if (ex == null) {
                    ex = e;
                }
            }
        }
        if (ex == null) {
            return false;
        } else {
            throw ex;
        }
    }

    public static boolean isPrototype(Class<?> type) {
        for (ApplicationContext c : HOLDER) {
            String[] beanNames = c.getBeanNamesForType(type);
            if (ArrayUtils.isNotEmpty(beanNames)) {
                return Arrays.stream(beanNames).allMatch(c::isPrototype);
            }
        }
        return false;
    }

    /**
     * 获取Bean的类型
     *
     * @param name
     * @return
     */
    public static Class<?> getType(String name) {
        return get(c -> c.getType(name));
    }

    /**
     * 获取bean的别名
     *
     * @param name
     * @return
     */
    public static String[] getAliases(String name) {
        return get(c -> c.getAliases(name)); // 不抛异常
    }

    /**
     * Returns a map that conatain spec annotation beans
     *
     * @param annotationType the Annotation type
     * @return a map
     */
    public static Map<String, Object> getBeansWithAnnotation(Class<? extends Annotation> annotationType) {
        return get(c -> c.getBeansWithAnnotation(annotationType));
    }

    /**
     * Autowire annotated from spring container for object
     *
     * @param object the object
     */
    public static void autowire(Object object) {
        Assert.state(HOLDER.size() > 0, "Must be defined SpringContextHolder within spring config file.");

        for (ApplicationContext context : HOLDER) {
            context.getAutowireCapableBeanFactory().autowireBean(object);
        }
    }

    @Override
    public void destroy() {
        // no-op
    }

    // -----------------------------------------------------------------------

    private static <T> T get(Function<ApplicationContext, T> finder) throws BeansException {
        BeansException ex = null;
        T result;
        for (ApplicationContext c : HOLDER) {
            try {
                if ((result = finder.apply(c)) != null) {
                    return result;
                }
            } catch (BeansException e) {
                if (ex == null) {
                    ex = e;
                }
            }
        }

        if (ex == null) {
            return null;
        } else {
            throw ex;
        }
    }

}
