/* __________              _____                                                *\
** \______   \____   _____/ ____\____   ____    Copyright (c) 2017-2023 Ponfee  **
**  |     ___/  _ \ /    \   __\/ __ \_/ __ \   http://www.ponfee.cn            **
**  |    |  (  <_> )   |  \  | \  ___/\  ___/   Apache License Version 2.0      **
**  |____|   \____/|___|  /__|  \___  >\___  >  http://www.apache.org/licenses/ **
**                      \/          \/     \/                                   **
\*                                                                              */

package cn.ponfee.scheduler.core.handle;

import cn.ponfee.scheduler.common.exception.Throwables.ThrowingSupplier;
import cn.ponfee.scheduler.common.spring.SpringContextHolder;
import cn.ponfee.scheduler.common.util.ClassUtils;
import cn.ponfee.scheduler.core.base.JobCodeMsg;
import cn.ponfee.scheduler.core.exception.JobException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.Assert;

import java.lang.reflect.Modifier;
import java.util.List;

import static cn.ponfee.scheduler.core.base.JobCodeMsg.SPLIT_JOB_FAILED;

/**
 * Job handler utility
 *
 * @author Ponfee
 */
public class JobHandlerUtils {

    public static boolean verify(String jobHandler, String jobParams) {
        if (StringUtils.isBlank(jobHandler)) {
            return false;
        }
        try {
            List<SplitTask> tasks = split(jobHandler, jobParams);
            return CollectionUtils.isNotEmpty(tasks);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Splits job to many sched task.
     *
     * @param jobHandler the job handler
     * @param jobParams  the job param
     * @return list of SplitTask
     * @throws JobException if split failed
     */
    public static List<SplitTask> split(String jobHandler, String jobParams) throws JobException {
        try {
            JobSplitter jobSplitter = JobHandlerUtils.load(jobHandler);
            List<SplitTask> splitTasks = jobSplitter.split(jobParams);
            if (CollectionUtils.isEmpty(splitTasks)) {
                throw new JobException(SPLIT_JOB_FAILED, "Job split none tasks.");
            }
            return splitTasks;
        } catch (JobException e) {
            throw e;
        } catch (Exception e) {
            throw new JobException(SPLIT_JOB_FAILED, "Split job occur error", e);
        }
    }

    /**
     * Load jobHandler instance, String parameter can be qualified class name or source code
     *
     * @param text qualified class name or source code
     * @return JobHandler instance object
     * @throws JobException if new instance failed
     */
    public static JobHandler<?> load(String text) throws JobException {
        if (SpringContextHolder.isInitialized()) {
            JobHandler<?> bean = ThrowingSupplier.ignored(() -> SpringContextHolder.getBean(text, JobHandler.class));
            if (bean != null) {
                Assert.isTrue(SpringContextHolder.isPrototype(text), () -> "Job handler spring bean name must be prototype: " + text);
                return bean;
            }
        }

        Class<JobHandler<?>> type = ClassUtils.getClass(text);
        if (type == null) {
            throw new JobException(JobCodeMsg.LOAD_HANDLER_ERROR, "Illegal class text: " + text);
        }

        // interface type: Modifier.isAbstract(type.getModifiers()) -> true
        if (!JobHandler.class.isAssignableFrom(type) || Modifier.isAbstract(type.getModifiers())) {
            throw new JobException(JobCodeMsg.LOAD_HANDLER_ERROR, "Invalid class type: " + ClassUtils.getName(type) + ", " + text);
        }

        return load(type);
    }

    // ------------------------------------------------------------private methods

    private static <T> T load(Class<T> type) {
        if (!SpringContextHolder.isInitialized()) {
            return ClassUtils.newInstance(type);
        }

        T bean = ThrowingSupplier.ignored(() -> SpringContextHolder.getBean(type));
        if (bean != null) {
            // must be annotated with @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
            Assert.isTrue(SpringContextHolder.isPrototype(type), () -> "Job handler spring bean type must be prototype: " + type);
            return bean;
        }

        return create(type);
    }

    private static <T> T create(Class<T> type) {
        T object = ClassUtils.newInstance(type);
        SpringContextHolder.autowire(object);
        return object;
    }

}
