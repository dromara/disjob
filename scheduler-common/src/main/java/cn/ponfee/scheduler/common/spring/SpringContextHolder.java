package cn.ponfee.scheduler.common.spring;

import cn.ponfee.scheduler.common.util.ClassUtils;
import cn.ponfee.scheduler.common.util.Fields;
import cn.ponfee.scheduler.common.util.GenericUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.Assert;

import javax.annotation.Resource;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
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
            INITIALIZED.set(true);
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

    // -----------------------------------------------------------------------
    /**
     * Injects the field from spring container for object
     *
     * @param object the object
     *
     * @see #autowire(Object)
     */
    public static void inject(Object object) {
        Assert.state(HOLDER.size() > 0, "Must be defined SpringContextHolder within spring config file.");

        for (Field field : ClassUtils.listFields(object.getClass())) {
            Object fieldValue = null;
            Class<?> fieldType = GenericUtils.getFieldActualType(object.getClass(), field);
            Resource resource = AnnotationUtils.getAnnotation(field, Resource.class);
            if (resource != null) {
                fieldValue = getBean(StringUtils.isNotBlank(resource.name()) ? resource.name() : field.getName(), fieldType);
                if (fieldValue == null) {
                    fieldValue = getBean(fieldType);
                }
            } else if (field.isAnnotationPresent(Autowired.class)) {
                Qualifier qualifier = AnnotationUtils.getAnnotation(field, Qualifier.class);
                if (qualifier != null && StringUtils.isNotBlank(qualifier.value())) {
                    fieldValue = getBean(qualifier.value(), fieldType);
                } else {
                    fieldValue = getBean(fieldType);
                }
            }

            if (fieldType.isInstance(fieldValue)) {
                Fields.put(object, field, fieldValue);
            }
        }
    }

    /**
     * Autowire annotated field from spring container for object
     *
     * @param object the object
     *
     * @see #inject(Object)
     */
    public static void autowire(Object object) {
        Assert.state(HOLDER.size() > 0, "Must be defined SpringContextHolder within spring config file.");

        for (ApplicationContext context : HOLDER) {
            context.getAutowireCapableBeanFactory().autowireBean(object);
        }
    }

    @Override
    public void destroy() {
        /*
        synchronized (SpringContextHolder.class) {
            HOLDER.clear();
        }
        */
    }

    private static <T> T get(Function<ApplicationContext, T> finder) throws BeansException {
        //Assert.state(HOLDER.size() > 0, "must be defined SpringContextHolder within spring config file.");
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
