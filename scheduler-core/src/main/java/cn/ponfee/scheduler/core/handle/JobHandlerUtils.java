package cn.ponfee.scheduler.core.handle;

import cn.ponfee.scheduler.common.spring.SpringContextHolder;
import cn.ponfee.scheduler.common.util.ClassUtils;
import cn.ponfee.scheduler.core.base.JobCodeMsg;
import cn.ponfee.scheduler.core.exception.JobException;

import java.lang.reflect.Modifier;

/**
 * New job handler instance utility
 *
 * @author Ponfee
 */
public class JobHandlerUtils {

    /**
     * New jobHandler instance, <br/>
     * String parameter can be qualified class name or source code
     *
     * @param text qualified class name or source code
     * @return JobHandler instance object
     */
    public static JobHandler<?> newInstance(String text) throws JobException {
        Class<JobHandler<?>> type = ClassUtils.getClass(text);
        if (type == null) {
            throw new JobException(JobCodeMsg.LOAD_HANDLER_ERROR, "Illegal class text: " + text);
        }

        // interface type: Modifier.isAbstract(type.getModifiers())==true
        if (!JobHandler.class.isAssignableFrom(type) || Modifier.isAbstract(type.getModifiers())) {
            throw new JobException(JobCodeMsg.LOAD_HANDLER_ERROR, "Invalid class type: " + ClassUtils.getName(type) + ", " + text);
        }
        return newInstance(type);
    }

    public static <T> T newInstance(Class<T> type) {
        if (!SpringContextHolder.isInitialized()) {
            return ClassUtils.newInstance(type);
        }
        try {
            // must be annotated with @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
            T object = SpringContextHolder.getBean(type);
            return object != null ? object : create(type);
        } catch (Exception e) {
            return create(type);
        }
    }

    private static <T> T create(Class<T> type) {
        T object = ClassUtils.newInstance(type);
        SpringContextHolder.autowire(object);
        return object;
    }
}
