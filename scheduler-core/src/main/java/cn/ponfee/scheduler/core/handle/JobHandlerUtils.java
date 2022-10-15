package cn.ponfee.scheduler.core.handle;

import cn.ponfee.scheduler.common.spring.SpringContextHolder;
import cn.ponfee.scheduler.common.util.ClassUtils;
import cn.ponfee.scheduler.core.base.JobCodeMsg;
import cn.ponfee.scheduler.core.exception.JobException;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

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
            JobSplitter jobSplitter = JobHandlerUtils.newInstance(jobHandler);
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

    private static <T> T newInstance(Class<T> type) {
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
